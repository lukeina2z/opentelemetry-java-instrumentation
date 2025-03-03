/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.awslambdaevents.v2_2;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
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
        @Advice.Local("otelFunctionContext") io.opentelemetry.context.Context functionContext,
        @Advice.Local("otelFunctionScope") Scope functionScope,
        @Advice.Local("otelMessageContext") io.opentelemetry.context.Context messageContext,
        @Advice.Local("otelMessageScope") Scope messageScope) {
      otelInput = AwsLambdaRequest.create(context, input, Collections.emptyMap());
      io.opentelemetry.context.Context parentContext =
          AwsLambdaInstrumentationHelper.functionInstrumenter().extract(otelInput);

      if (!AwsLambdaInstrumentationHelper.functionInstrumenter()
          .shouldStart(parentContext, otelInput)) {
        return;
      }

      functionContext =
          AwsLambdaInstrumentationHelper.functionInstrumenter().start(parentContext, otelInput);
      functionScope = functionContext.makeCurrent();

      //  if (input instanceof SQSEvent) {
      //    if (AwsLambdaInstrumentationHelper.messageInstrumenter()
      //        .shouldStart(functionContext, (SQSEvent) input)) {
      //      messageContext =
      //          AwsLambdaInstrumentationHelper.messageInstrumenter()
      //              .start(functionContext, (SQSEvent) input);
      //      messageScope = messageContext.makeCurrent();
      //    }
      //  }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Argument(value = 0, typing = Typing.DYNAMIC) Object arg,
        @Advice.Thrown Throwable throwable,
        @Advice.Local("otelInput") AwsLambdaRequest input,
        @Advice.Local("otelFunctionContext") io.opentelemetry.context.Context functionContext,
        @Advice.Local("otelFunctionScope") Scope functionScope,
        @Advice.Local("otelMessageContext") io.opentelemetry.context.Context messageContext,
        @Advice.Local("otelMessageScope") Scope messageScope) {

      // logger.warning("xxxlog: event 2.2: in stream handler stopSpan() call");

      if (messageScope != null) {
        messageScope.close();
        AwsLambdaInstrumentationHelper.messageInstrumenter()
            .end(messageContext, (SQSEvent) arg, null, throwable);
      }

      if (functionScope != null) {
        functionScope.close();
        AwsLambdaInstrumentationHelper.functionInstrumenter()
            .end(functionContext, input, null, throwable);
      }

      OpenTelemetrySdkAccess.forceFlush(1, TimeUnit.SECONDS);
    }
  }
}
