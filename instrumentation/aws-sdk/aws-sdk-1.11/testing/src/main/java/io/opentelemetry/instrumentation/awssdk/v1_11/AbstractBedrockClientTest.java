/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import com.amazonaws.services.bedrock.AmazonBedrock;
import com.amazonaws.services.bedrock.AmazonBedrockClientBuilder;
import com.amazonaws.services.bedrock.model.GetGuardrailRequest;
import com.google.common.collect.ImmutableMap;
import io.opentelemetry.testing.internal.armeria.common.HttpResponse;
import io.opentelemetry.testing.internal.armeria.common.HttpStatus;
import io.opentelemetry.testing.internal.armeria.common.MediaType;
import org.junit.jupiter.api.Test;

public abstract class AbstractBedrockClientTest extends AbstractBaseAwsClientTest {

  public abstract AmazonBedrockClientBuilder configureClient(AmazonBedrockClientBuilder client);

  @Override
  protected boolean hasRequestId() {
    return true;
  }

  @Test
  public void sendRequestWithMockedResponse() throws Exception {
    AmazonBedrockClientBuilder clientBuilder = AmazonBedrockClientBuilder.standard();
    AmazonBedrock client =
        configureClient(clientBuilder)
            .withEndpointConfiguration(endpoint)
            .withCredentials(credentialsProvider)
            .build();

    String body =
        "{"
            + "  \"blockedInputMessaging\": \"string\","
            + "  \"blockedOutputsMessaging\": \"string\","
            + "  \"contentPolicy\": {},"
            + "  \"createdAt\": \"2024-06-12T18:31:45Z\","
            + "  \"description\": \"string\","
            + "  \"guardrailArn\": \"guardrailArn\","
            + "  \"guardrailId\": \"guardrailId\","
            + "  \"kmsKeyArn\": \"string\","
            + "  \"name\": \"string\","
            + "  \"sensitiveInformationPolicy\": {},"
            + "  \"status\": \"READY\","
            + "  \"topicPolicy\": {"
            + "    \"topics\": ["
            + "      {"
            + "        \"definition\": \"string\","
            + "        \"examples\": [ \"string\" ],"
            + "        \"name\": \"string\","
            + "        \"type\": \"string\""
            + "      }"
            + "    ]"
            + "  },"
            + "  \"updatedAt\": \"2024-06-12T18:31:48Z\","
            + "  \"version\": \"DRAFT\","
            + "  \"wordPolicy\": {}"
            + "}";

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8, body));

    Object response =
        client.getGuardrail(new GetGuardrailRequest().withGuardrailIdentifier("guardrailId"));

    assertRequestWithMockedResponse(
        response,
        client,
        "Bedrock",
        "GetGuardrail",
        "GET",
        ImmutableMap.of("aws.bedrock.guardrail.id", "guardrailId"));
  }
}
