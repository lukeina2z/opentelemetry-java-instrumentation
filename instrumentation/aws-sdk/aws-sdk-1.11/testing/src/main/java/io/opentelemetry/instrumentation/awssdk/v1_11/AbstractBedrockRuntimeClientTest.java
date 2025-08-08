/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static java.util.Arrays.asList;

import com.amazonaws.services.bedrockruntime.AmazonBedrockRuntime;
import com.amazonaws.services.bedrockruntime.AmazonBedrockRuntimeClientBuilder;
import com.amazonaws.services.bedrockruntime.model.InvokeModelRequest;
import io.opentelemetry.sdk.testing.assertj.AttributeAssertion;
import io.opentelemetry.testing.internal.armeria.common.HttpResponse;
import io.opentelemetry.testing.internal.armeria.common.HttpStatus;
import io.opentelemetry.testing.internal.armeria.common.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class AbstractBedrockRuntimeClientTest extends AbstractBaseAwsClientTest {

  public abstract AmazonBedrockRuntimeClientBuilder configureClient(
      AmazonBedrockRuntimeClientBuilder client);

  @Override
  protected boolean hasRequestId() {
    return true;
  }

  @ParameterizedTest
  @MethodSource("testData")
  public void sendRequestWithMockedResponse(
      String modelId,
      String requestBody,
      String expectedResponse,
      List<AttributeAssertion> expectedAttributes)
      throws Exception {
    AmazonBedrockRuntimeClientBuilder clientBuilder = AmazonBedrockRuntimeClientBuilder.standard();
    AmazonBedrockRuntime client =
        configureClient(clientBuilder)
            .withEndpointConfiguration(endpoint)
            .withCredentials(credentialsProvider)
            .build();

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8, expectedResponse));

    client.invokeModel(
        new InvokeModelRequest()
            .withModelId(modelId)
            .withBody(StandardCharsets.UTF_8.encode(requestBody)));

    assertRequestWithMockedResponse(
        expectedResponse, client, "BedrockRuntime", "InvokeModel", "POST", expectedAttributes);
  }

  private static Stream<Object[]> testData() {
    return Stream.of(
        new Object[] {
          "ai21.jamba-1-5-mini-v1:0",
          "{\"messages\":[{\"role\":\"user\",\"message\":\"Which LLM are you?\"}],\"max_tokens\":1000,\"top_p\":0.8,\"temperature\":0.7}",
          "{\"choices\":[{\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":42}}",
          asList(
              equalTo(stringKey("gen_ai.request.model"), "ai21.jamba-1-5-mini-v1:0"),
              equalTo(stringKey("gen_ai.system"), "aws.bedrock"),
              equalTo(stringKey("gen_ai.request.max_tokens"), "1000"),
              equalTo(stringKey("gen_ai.request.temperature"), "0.7"),
              equalTo(stringKey("gen_ai.request.top_p"), "0.8"),
              equalTo(stringKey("gen_ai.response.finish_reasons"), "[stop]"),
              equalTo(stringKey("gen_ai.usage.input_tokens"), "5"),
              equalTo(stringKey("gen_ai.usage.output_tokens"), "42"))
        },
        new Object[] {
          "amazon.titan-text-premier-v1:0",
          "{\"inputText\":\"Hello, world!\",\"textGenerationConfig\":{\"temperature\":0.7,\"topP\":0.9,\"maxTokenCount\":100,\"stopSequences\":[\"END\"]}}",
          "{\"inputTextTokenCount\":5,\"results\":[{\"tokenCount\":42,\"outputText\":\"Hi! I'm Titan, an AI assistant.\",\"completionReason\":\"stop\"}]}",
          asList(
              equalTo(stringKey("gen_ai.request.model"), "amazon.titan-text-premier-v1:0"),
              equalTo(stringKey("gen_ai.system"), "aws.bedrock"),
              equalTo(stringKey("gen_ai.request.max_tokens"), "100"),
              equalTo(stringKey("gen_ai.request.temperature"), "0.7"),
              equalTo(stringKey("gen_ai.request.top_p"), "0.9"),
              equalTo(stringKey("gen_ai.response.finish_reasons"), "[stop]"),
              equalTo(stringKey("gen_ai.usage.input_tokens"), "5"),
              equalTo(stringKey("gen_ai.usage.output_tokens"), "42"))
        },
        new Object[] {
          "anthropic.claude-3-5-sonnet-20241022-v2:0",
          "{\"anthropic_version\":\"bedrock-2023-05-31\",\"messages\":[{\"role\":\"user\",\"content\":\"Hello, world\"}],\"max_tokens\":100,\"temperature\":0.7,\"top_p\":0.9}",
          "{\"stop_reason\":\"end_turn\",\"usage\":{\"input_tokens\":2095,\"output_tokens\":503}}",
          asList(
              equalTo(
                  stringKey("gen_ai.request.model"), "anthropic.claude-3-5-sonnet-20241022-v2:0"),
              equalTo(stringKey("gen_ai.system"), "aws.bedrock"),
              equalTo(stringKey("gen_ai.request.max_tokens"), "100"),
              equalTo(stringKey("gen_ai.request.temperature"), "0.7"),
              equalTo(stringKey("gen_ai.request.top_p"), "0.9"),
              equalTo(stringKey("gen_ai.response.finish_reasons"), "[end_turn]"),
              equalTo(stringKey("gen_ai.usage.input_tokens"), "2095"),
              equalTo(stringKey("gen_ai.usage.output_tokens"), "503"))
        },
        new Object[] {
          "meta.llama3-70b-instruct-v1:0",
          "{\"prompt\":\"<|begin_of_text|><|start_header_id|>user<|end_header_id|>\\\\nDescribe the purpose of a 'hello world' program in one line. <|eot_id|>\\\\n<|start_header_id|>assistant<|end_header_id|>\\\\n\",\"max_gen_len\":128,\"temperature\":0.1,\"top_p\":0.9}",
          "{\"prompt_token_count\":2095,\"generation_token_count\":503,\"stop_reason\":\"stop\"}",
          asList(
              equalTo(stringKey("gen_ai.request.model"), "meta.llama3-70b-instruct-v1:0"),
              equalTo(stringKey("gen_ai.system"), "aws.bedrock"),
              equalTo(stringKey("gen_ai.request.max_tokens"), "128"),
              equalTo(stringKey("gen_ai.request.temperature"), "0.1"),
              equalTo(stringKey("gen_ai.request.top_p"), "0.9"),
              equalTo(stringKey("gen_ai.response.finish_reasons"), "[stop]"),
              equalTo(stringKey("gen_ai.usage.input_tokens"), "2095"),
              equalTo(stringKey("gen_ai.usage.output_tokens"), "503"))
        },
        new Object[] {
          "cohere.command-r-v1:0",
          "{\"message\":\"Convince me to write a LISP interpreter in one line.\",\"temperature\":0.8,\"max_tokens\":4096,\"p\":0.45}",
          "{\"text\":\"test-output\",\"finish_reason\":\"COMPLETE\"}",
          asList(
              equalTo(stringKey("gen_ai.request.model"), "cohere.command-r-v1:0"),
              equalTo(stringKey("gen_ai.system"), "aws.bedrock"),
              equalTo(stringKey("gen_ai.request.max_tokens"), "4096"),
              equalTo(stringKey("gen_ai.request.temperature"), "0.8"),
              equalTo(stringKey("gen_ai.request.top_p"), "0.45"),
              equalTo(stringKey("gen_ai.response.finish_reasons"), "[COMPLETE]"),
              equalTo(stringKey("gen_ai.usage.input_tokens"), "9"),
              equalTo(stringKey("gen_ai.usage.output_tokens"), "2"))
        });
  }
}
