/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import com.amazonaws.services.bedrockagent.AWSBedrockAgent;
import com.amazonaws.services.bedrockagent.AWSBedrockAgentClientBuilder;
import com.amazonaws.services.bedrockagent.model.GetAgentRequest;
import com.amazonaws.services.bedrockagent.model.GetDataSourceRequest;
import com.amazonaws.services.bedrockagent.model.GetKnowledgeBaseRequest;
import com.google.common.collect.ImmutableMap;
import io.opentelemetry.testing.internal.armeria.common.HttpResponse;
import io.opentelemetry.testing.internal.armeria.common.HttpStatus;
import io.opentelemetry.testing.internal.armeria.common.MediaType;
import java.util.Map;
import org.junit.jupiter.api.Test;

public abstract class AbstractBedrockAgentClientTest extends AbstractBaseAwsClientTest {

  public abstract AWSBedrockAgentClientBuilder configureClient(AWSBedrockAgentClientBuilder client);

  @Override
  protected boolean hasRequestId() {
    return true;
  }

  @Test
  public void sendGetAgentRequest() throws Exception {
    AWSBedrockAgent client = createClient();

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8, "{}"));

    Object response = client.getAgent(new GetAgentRequest().withAgentId("agentId"));

    assertRequestWithMockedResponse(
        response,
        client,
        "AWSBedrockAgent",
        "GetAgent",
        "GET",
        ImmutableMap.of("aws.bedrock.agent.id", "agentId"));
  }

  @Test
  public void sendGetKnowledgeBaseRequest() throws Exception {
    AWSBedrockAgent client = createClient();

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8, "{}"));

    Object response =
        client.getKnowledgeBase(
            new GetKnowledgeBaseRequest().withKnowledgeBaseId("knowledgeBaseId"));

    assertRequestWithMockedResponse(
        response,
        client,
        "AWSBedrockAgent",
        "GetKnowledgeBase",
        "GET",
        ImmutableMap.of("aws.bedrock.knowledge_base.id", "knowledgeBaseId"));
  }

  @Test
  public void sendGetDataSourceRequest() throws Exception {
    AWSBedrockAgent client = createClient();

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8, "{}"));

    Object response =
        client.getDataSource(
            new GetDataSourceRequest()
                .withDataSourceId("datasourceId")
                .withKnowledgeBaseId("knowledgeBaseId"));

    Map<String, String> additionalAttributes =
        ImmutableMap.of("aws.bedrock.data_source.id", "datasourceId");

    assertRequestWithMockedResponse(
        response, client, "AWSBedrockAgent", "GetDataSource", "GET", additionalAttributes);
  }

  private AWSBedrockAgent createClient() {
    AWSBedrockAgentClientBuilder clientBuilder = AWSBedrockAgentClientBuilder.standard();
    return configureClient(clientBuilder)
        .withEndpointConfiguration(endpoint)
        .withCredentials(credentialsProvider)
        .build();
  }
}
