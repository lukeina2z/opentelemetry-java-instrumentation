/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.awslambdacore.v1_0;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
// import static java.util.Collections.singletonList;
import static net.bytebuddy.matcher.ElementMatchers.not;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.Arrays;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

// import java.util.logging.Logger;

@AutoService(InstrumentationModule.class)
public class AwsLambdaInstrumentationModule extends InstrumentationModule {

  // private static final Logger logger =
  // Logger.getLogger(AwsLambdaInstrumentationModule.class.getName());

  public AwsLambdaInstrumentationModule() {
    super("aws-lambda-core", "aws-lambda-core-1.0", "aws-lambda");

    // logger.info("xxxlog:xx300-d in the constructor of AwsLambdaInstrumentationModule");
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    // aws-lambda-events-2.2 is used when SQSEvent is present
    return not(hasClassesNamed("com.amazonaws.services.lambda.runtime.events.SQSEvent"));
  }

  @Override
  public boolean isHelperClass(String className) {
    return className.startsWith("io.opentelemetry.contrib.awsxray.");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    // return singletonList(new AwsLambdaRequestHandlerInstrumentation());
    // return singletonList(new AwsLambdaRequestStreamHandlerInstrumentation());
    return Arrays.asList(
        new AwsLambdaRequestHandlerInstrumentation(),
        new AwsLambdaRequestStreamHandlerInstrumentation());
  }
}
