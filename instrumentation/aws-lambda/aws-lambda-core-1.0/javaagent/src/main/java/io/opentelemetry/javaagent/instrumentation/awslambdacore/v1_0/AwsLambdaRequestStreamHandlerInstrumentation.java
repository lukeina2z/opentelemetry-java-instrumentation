/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.awslambdacore.v1_0;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static io.opentelemetry.javaagent.instrumentation.awslambdacore.v1_0.AwsLambdaInstrumentationHelper.functionInstrumenter;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.amazonaws.services.lambda.runtime.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.awslambdacore.v1_0.AwsLambdaRequest;
import io.opentelemetry.javaagent.bootstrap.OpenTelemetrySdkAccess;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;
import net.bytebuddy.matcher.ElementMatcher;

// import java.util.logging.Logger;

public class AwsLambdaRequestStreamHandlerInstrumentation implements TypeInstrumentation {

  // private static final Logger logger =
  // Logger.getLogger(AwsLambdaRequestStreamHandlerInstrumentation.class.getName());

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("com.amazonaws.services.lambda.runtime.RequestStreamHandler");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("com.amazonaws.services.lambda.runtime.RequestStreamHandler"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(named("handleRequest"))
            // .and(takesArgument(0, named("java.io.InputStream")))
            // .and(takesArgument(1, named("java.io.OutputStream")))
            .and(takesArgument(2, named("com.amazonaws.services.lambda.runtime.Context"))),
        AwsLambdaRequestStreamHandlerInstrumentation.class.getName() + "$HandleRequestAdvice");
  }

  @SuppressWarnings("unused")
  public static class HandleRequestAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Argument(0) InputStream input,
        @Advice.Argument(2) Context context,
        @Advice.Local("otelInput") AwsLambdaRequest otelInput,
        @Advice.Local("otelContext") io.opentelemetry.context.Context otelContext,
        @Advice.Local("otelScope") Scope otelScope) {

      otelInput = AwsLambdaRequest.create(context, input, Collections.emptyMap());
      io.opentelemetry.context.Context parentContext = functionInstrumenter().extract(otelInput);

      // logger.warning("xxxlog: in the constructor of AwsLambdaInstrumentationModule");

      if (!functionInstrumenter().shouldStart(parentContext, otelInput)) {
        return;
      }

      // logger.warning("xxxlog: right before stream handler start instrumenter start()");
      otelContext = functionInstrumenter().start(parentContext, otelInput);
      otelScope = otelContext.makeCurrent();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Argument(value = 0, typing = Typing.DYNAMIC) Object arg,
        @Advice.Thrown Throwable throwable,
        @Advice.Local("otelInput") AwsLambdaRequest input,
        @Advice.Local("otelContext") io.opentelemetry.context.Context functionContext,
        @Advice.Local("otelScope") Scope functionScope) {

      // logger.warning("xxxlog: in stream handler stopSpan() call");
      if (functionScope != null) {
        functionScope.close();
        functionInstrumenter().end(functionContext, input, null, throwable);
      }

      OpenTelemetrySdkAccess.forceFlush(1, TimeUnit.SECONDS);
    }
  }
}
