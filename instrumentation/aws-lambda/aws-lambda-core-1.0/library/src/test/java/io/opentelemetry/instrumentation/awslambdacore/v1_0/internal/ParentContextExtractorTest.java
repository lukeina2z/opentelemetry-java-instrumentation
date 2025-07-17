/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awslambdacore.v1_0.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
@ExtendWith(SystemStubsExtension.class)
class ParentContextExtractorTest {

  @SystemStub final EnvironmentVariables environmentVariables = new EnvironmentVariables();
  @SystemStub final SystemProperties systemProperties = new SystemProperties();

  private static final OpenTelemetry OTEL_WITH_B3_PROPAGATOR =
      OpenTelemetry.propagating(ContextPropagators.create(B3Propagator.injectingSingleHeader()));

  private static final AwsLambdaFunctionInstrumenter INSTRUMENTER_WITH_B3_PROPAGATOR =
      AwsLambdaFunctionInstrumenterFactory.createInstrumenter(OTEL_WITH_B3_PROPAGATOR);

  // Only for new lambda context tests
  private static final OpenTelemetry OTEL_WITH_B3_XRAY_PROPAGATORS =
      OpenTelemetry.propagating(
          ContextPropagators.create(
              TextMapPropagator.composite(
                  B3Propagator.injectingSingleHeader(), AwsXrayPropagator.getInstance())));
  private static final OpenTelemetry OTEL_WITH_XRAY_B3_PROPAGATORS =
      OpenTelemetry.propagating(
          ContextPropagators.create(
              TextMapPropagator.composite(
                  AwsXrayPropagator.getInstance(), B3Propagator.injectingSingleHeader())));

  private static final AwsLambdaFunctionInstrumenter INSTRUMENTER_WITH_B3_XRAY_PROPAGATORS =
      AwsLambdaFunctionInstrumenterFactory.createInstrumenter(OTEL_WITH_B3_XRAY_PROPAGATORS);

  private static final AwsLambdaFunctionInstrumenter INSTRUMENTER_WITH_XRAY_B3_PROPAGATORS =
      AwsLambdaFunctionInstrumenterFactory.createInstrumenter(OTEL_WITH_XRAY_B3_PROPAGATORS);

  private static final Context mockLambdaContext = mock(Context.class);

  @Test
  void shouldUseHttpIfAwsParentNotSampled() {
    // given
    Map<String, String> headers =
        ImmutableMap.of(
            "X-b3-traceId",
            "4fd0b6131f19f39af59518d127b0cafe",
            "x-b3-spanid",
            "0000000000000123",
            "X-B3-Sampled",
            "true");
    environmentVariables.set(
        "_X_AMZN_TRACE_ID",
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=0000000000000456;Sampled=0");

    // when
    io.opentelemetry.context.Context context =
        ParentContextExtractor.extract(headers, INSTRUMENTER_WITH_B3_PROPAGATOR, mockLambdaContext);
    // then
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.getSpanId()).isEqualTo("0000000000000123");
    assertThat(spanContext.getTraceId()).isEqualTo("4fd0b6131f19f39af59518d127b0cafe");
  }

  @Test
  void shouldPreferAwsParentHeaderIfValidAndSampled() {
    // given
    Map<String, String> headers =
        ImmutableMap.of(
            "X-b3-traceId",
            "4fd0b6131f19f39af59518d127b0cafe",
            "x-b3-spanid",
            "0000000000000456",
            "X-B3-Sampled",
            "true");
    environmentVariables.set(
        "_X_AMZN_TRACE_ID",
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=0000000000000456;Sampled=1");

    // when
    io.opentelemetry.context.Context context =
        ParentContextExtractor.extract(
            headers, INSTRUMENTER_WITH_B3_XRAY_PROPAGATORS, mockLambdaContext);
    // then
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.getSpanId()).isEqualTo("0000000000000456");
    assertThat(spanContext.getTraceId()).isEqualTo("8a3c60f7d188f8fa79d48a391a778fa6");
  }

  @Test
  void shouldExtractCaseInsensitiveHeaders() {
    // given
    Map<String, String> headers =
        ImmutableMap.of(
            "X-b3-traceId",
            "4fd0b6131f19f39af59518d127b0cafe",
            "x-b3-spanid",
            "0000000000000456",
            "X-B3-Sampled",
            "true");

    // when
    io.opentelemetry.context.Context context =
        ParentContextExtractor.extract(headers, INSTRUMENTER_WITH_B3_PROPAGATOR, mockLambdaContext);
    // then
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.getSpanId()).isEqualTo("0000000000000456");
    assertThat(spanContext.getTraceId()).isEqualTo("4fd0b6131f19f39af59518d127b0cafe");
  }

  @Test
  void shouldPreferSystemPropertyOverEnvVariable() {
    // given
    systemProperties.set(
        "com.amazonaws.xray.traceHeader",
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa7;Parent=0000000000000789;Sampled=1");
    environmentVariables.set(
        "_X_AMZN_TRACE_ID",
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=0000000000000456;Sampled=1");

    // when
    io.opentelemetry.context.Context context =
        ParentContextExtractor.extract(
            ImmutableMap.of(), INSTRUMENTER_WITH_B3_XRAY_PROPAGATORS, mockLambdaContext);
    // then
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.getSpanId()).isEqualTo("0000000000000789");
    assertThat(spanContext.getTraceId()).isEqualTo("8a3c60f7d188f8fa79d48a391a778fa7");
  }

  @Test
  void shouldUseLambdaContextToExtractXrayTraceId() {
    // given
    Map<String, String> headers = ImmutableMap.of();
    Context mockLambdaContextWithXrayTraceId = mock(Context.class);
    when(mockLambdaContextWithXrayTraceId.getXrayTraceId())
        .thenReturn("Root=1-4fd0b613-1f19f39af59518d127b0cafe;Parent=0000000000000123;Sampled=1");

    // when
    io.opentelemetry.context.Context context =
        ParentContextExtractor.extract(
            headers, INSTRUMENTER_WITH_B3_XRAY_PROPAGATORS, mockLambdaContextWithXrayTraceId);
    // then
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.getSpanId()).isEqualTo("0000000000000123");
    assertThat(spanContext.getTraceId()).isEqualTo("4fd0b6131f19f39af59518d127b0cafe");
  }

  @Test
  void shouldPreferLambdaContextOverSystemProperty() {
    // given
    Map<String, String> headers = ImmutableMap.of();
    Context mockLambdaContextWithXrayTraceId = mock(Context.class);
    when(mockLambdaContextWithXrayTraceId.getXrayTraceId())
        .thenReturn("Root=1-4fd0b613-1f19f39af59518d127b0cafe;Parent=0000000000000123;Sampled=1");
    systemProperties.set(
        "com.amazonaws.xray.traceHeader",
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa7;Parent=0000000000000789;Sampled=1");

    // when
    io.opentelemetry.context.Context context =
        ParentContextExtractor.extract(
            headers, INSTRUMENTER_WITH_B3_XRAY_PROPAGATORS, mockLambdaContextWithXrayTraceId);
    // then
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.getSpanId()).isEqualTo("0000000000000123");
    assertThat(spanContext.getTraceId()).isEqualTo("4fd0b6131f19f39af59518d127b0cafe");
  }

  @Test
  void shouldPreferLambdaContextOverEnvVariable() {
    // given
    Map<String, String> headers = ImmutableMap.of();
    Context mockLambdaContextWithXrayTraceId = mock(Context.class);
    when(mockLambdaContextWithXrayTraceId.getXrayTraceId())
        .thenReturn("Root=1-4fd0b613-1f19f39af59518d127b0cafe;Parent=0000000000000123;Sampled=1");
    environmentVariables.set(
        "_X_AMZN_TRACE_ID",
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=0000000000000456;Sampled=1");

    // when
    io.opentelemetry.context.Context context =
        ParentContextExtractor.extract(
            headers, INSTRUMENTER_WITH_B3_XRAY_PROPAGATORS, mockLambdaContextWithXrayTraceId);
    // then
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.getSpanId()).isEqualTo("0000000000000123");
    assertThat(spanContext.getTraceId()).isEqualTo("4fd0b6131f19f39af59518d127b0cafe");
  }

  @Test
  void shouldPreferLambdaContextOverHttp() {
    // given
    Map<String, String> headers =
        ImmutableMap.of(
            "X-b3-traceId",
            "4fd0b6131f19f39af59518d127b0cafe",
            "x-b3-spanid",
            "0000000000000123",
            "X-B3-Sampled",
            "true");
    Context mockLambdaContextWithXrayTraceId = mock(Context.class);
    when(mockLambdaContextWithXrayTraceId.getXrayTraceId())
        .thenReturn("Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=0000000000000456;Sampled=1");

    // when
    io.opentelemetry.context.Context context =
        ParentContextExtractor.extract(
            headers, INSTRUMENTER_WITH_B3_XRAY_PROPAGATORS, mockLambdaContextWithXrayTraceId);
    // then
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.getSpanId()).isEqualTo("0000000000000456");
    assertThat(spanContext.getTraceId()).isEqualTo("8a3c60f7d188f8fa79d48a391a778fa6");
  }

  @Test
  void shouldPreferHttpOverXrayIdSetByLambdaContext() {
    // given
    Map<String, String> headers =
        ImmutableMap.of(
            "X-b3-traceId",
            "4fd0b6131f19f39af59518d127b0cafe",
            "x-b3-spanid",
            "0000000000000123",
            "X-B3-Sampled",
            "true");
    environmentVariables.set(
        "_X_AMZN_TRACE_ID",
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=0000000000000456;Sampled=1");
    systemProperties.set(
        "com.amazonaws.xray.traceHeader",
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=0000000000000456;Sampled=1");
    Context mockLambdaContextWithXrayTraceId = mock(Context.class);
    when(mockLambdaContextWithXrayTraceId.getXrayTraceId())
        .thenReturn("Root=1-8a3c60f7-d188f8fa79d48a391a778fa6;Parent=0000000000000456;Sampled=1");

    // when
    io.opentelemetry.context.Context context =
        ParentContextExtractor.extract(
            headers, INSTRUMENTER_WITH_XRAY_B3_PROPAGATORS, mockLambdaContextWithXrayTraceId);
    // then
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.getSpanId()).isEqualTo("0000000000000123");
    assertThat(spanContext.getTraceId()).isEqualTo("4fd0b6131f19f39af59518d127b0cafe");
  }

  @Test
  void shouldFallbackToSystemPropertyIfContextTraceIdIsNull() {
    // given
    Map<String, String> headers = ImmutableMap.of();
    Context mockLambdaContextWithXrayTraceId = mock(Context.class);
    when(mockLambdaContextWithXrayTraceId.getXrayTraceId()).thenReturn(null);
    systemProperties.set(
        "com.amazonaws.xray.traceHeader",
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa7;Parent=0000000000000789;Sampled=1");

    // when
    io.opentelemetry.context.Context context =
        ParentContextExtractor.extract(
            headers, INSTRUMENTER_WITH_B3_XRAY_PROPAGATORS, mockLambdaContextWithXrayTraceId);
    // then
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.getSpanId()).isEqualTo("0000000000000789");
    assertThat(spanContext.getTraceId()).isEqualTo("8a3c60f7d188f8fa79d48a391a778fa7");
  }

  @Test
  void shouldFallbackToSystemPropertyIfContextTraceIdIsEmptyString() {
    // given
    Map<String, String> headers = ImmutableMap.of();
    Context mockLambdaContextWithXrayTraceId = mock(Context.class);
    when(mockLambdaContextWithXrayTraceId.getXrayTraceId()).thenReturn("");
    systemProperties.set(
        "com.amazonaws.xray.traceHeader",
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa7;Parent=0000000000000789;Sampled=1");

    // when
    io.opentelemetry.context.Context context =
        ParentContextExtractor.extract(
            headers, INSTRUMENTER_WITH_B3_XRAY_PROPAGATORS, mockLambdaContextWithXrayTraceId);
    // then
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.getSpanId()).isEqualTo("0000000000000789");
    assertThat(spanContext.getTraceId()).isEqualTo("8a3c60f7d188f8fa79d48a391a778fa7");
  }

  @Test
  void shouldFallbackToSystemPropertyWhenNoSuchMethodErrorThrown() {
    // given
    Map<String, String> headers = ImmutableMap.of();
    Context mockLambdaContextWithNoSuchMethodError = mock(Context.class);
    when(mockLambdaContextWithNoSuchMethodError.getXrayTraceId())
        .thenThrow(new NoSuchMethodError("getXrayTraceId method not found"));
    systemProperties.set(
        "com.amazonaws.xray.traceHeader",
        "Root=1-8a3c60f7-d188f8fa79d48a391a778fa7;Parent=0000000000000789;Sampled=1");

    // Reset the static flag to ensure the method is attempted
    ParentContextExtractor.getXrayTraceIdMethodExists = true;

    // when - call extract
    io.opentelemetry.context.Context context =
        ParentContextExtractor.extract(
            headers, INSTRUMENTER_WITH_B3_XRAY_PROPAGATORS, mockLambdaContextWithNoSuchMethodError);

    // then
    Span span = Span.fromContext(context);
    SpanContext spanContext = span.getSpanContext();
    assertThat(spanContext.isValid()).isTrue();
    assertThat(spanContext.getSpanId()).isEqualTo("0000000000000789");
    assertThat(spanContext.getTraceId()).isEqualTo("8a3c60f7d188f8fa79d48a391a778fa7");
    // Verify getXrayTraceId was called only once
    assertThat(ParentContextExtractor.getXrayTraceIdMethodExists).isFalse();
    verify(mockLambdaContextWithNoSuchMethodError, times(1)).getXrayTraceId();

    // when - call extract again
    ParentContextExtractor.extract(
        headers, INSTRUMENTER_WITH_B3_XRAY_PROPAGATORS, mockLambdaContextWithNoSuchMethodError);
    // Verify the call count of getXrayTraceId is still 1
    verify(mockLambdaContextWithNoSuchMethodError, times(1)).getXrayTraceId();
  }
}
