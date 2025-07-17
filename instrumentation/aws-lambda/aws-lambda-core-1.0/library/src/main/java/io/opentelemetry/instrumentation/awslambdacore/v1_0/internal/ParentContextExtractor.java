/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awslambdacore.v1_0.internal;

import static io.opentelemetry.instrumentation.awslambdacore.v1_0.internal.MapUtils.lowercaseMap;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.javaagent.tooling.muzzle.NoMuzzle;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class ParentContextExtractor {

  private static final Logger logger = Logger.getLogger(ParentContextExtractor.class.getName());
  private static final String AWS_TRACE_HEADER_ENV_KEY = "_X_AMZN_TRACE_ID";
  private static final String AWS_TRACE_HEADER_PROP = "com.amazonaws.xray.traceHeader";
  // lower-case map getter used for extraction
  static final String AWS_TRACE_HEADER_PROPAGATOR_KEY = "x-amzn-trace-id";
  static boolean getXrayTraceIdMethodExists = true;

  static Context extract(
      Map<String, String> headers,
      AwsLambdaFunctionInstrumenter instrumenter,
      com.amazonaws.services.lambda.runtime.Context lambdaContext) {
    Map<String, String> mergedHeaders = lowercaseMap(headers);
    String parentTraceHeader = getTraceHeader(lambdaContext);
    if (parentTraceHeader != null) {
      mergedHeaders.put(AWS_TRACE_HEADER_PROPAGATOR_KEY, parentTraceHeader);
    }
    return instrumenter.extract(mergedHeaders, MapGetter.INSTANCE);
  }

  @NoMuzzle
  private static String getTraceHeader(
      com.amazonaws.services.lambda.runtime.Context lambdaContext) {
    String traceHeader = null;

    // Lambda Core dependency that is actually used by Lambda Runtime may be on an older version
    // that does not have the `getXrayTraceId` method. If `NoSuchMethodError` occurs, we do not
    // attempt invoking `getXrayTraceId` again.
    if (getXrayTraceIdMethodExists) {
      try {
        traceHeader = lambdaContext.getXrayTraceId();
      } catch (NoSuchMethodError e) {
        logger.fine("Failed to get X-Ray trace ID from lambdaContext: " + e);
        getXrayTraceIdMethodExists = false;
      }
    }
    if (traceHeader != null && !traceHeader.isEmpty()) {
      return traceHeader;
    }

    // Lambda propagates trace header by system property instead of environment variable from java17
    traceHeader = System.getProperty(AWS_TRACE_HEADER_PROP);
    if (traceHeader == null || traceHeader.isEmpty()) {
      return System.getenv(AWS_TRACE_HEADER_ENV_KEY);
    }
    return traceHeader;
  }

  private enum MapGetter implements TextMapGetter<Map<String, String>> {
    INSTANCE;

    @Override
    public Iterable<String> keys(Map<String, String> map) {
      return map.keySet();
    }

    @Override
    public String get(Map<String, String> map, String s) {
      return map.get(s.toLowerCase(Locale.ROOT));
    }
  }

  private ParentContextExtractor() {}
}
