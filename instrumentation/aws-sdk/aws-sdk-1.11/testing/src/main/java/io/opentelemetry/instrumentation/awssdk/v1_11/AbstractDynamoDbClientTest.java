/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.incubating.AwsIncubatingAttributes.AWS_DYNAMODB_TABLE_NAMES;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_SYSTEM;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DbSystemIncubatingValues.DYNAMODB;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import io.opentelemetry.sdk.testing.assertj.AttributeAssertion;
import io.opentelemetry.testing.internal.armeria.common.HttpResponse;
import io.opentelemetry.testing.internal.armeria.common.HttpStatus;
import io.opentelemetry.testing.internal.armeria.common.MediaType;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public abstract class AbstractDynamoDbClientTest extends AbstractBaseAwsClientTest {

  public abstract AmazonDynamoDBClientBuilder configureClient(AmazonDynamoDBClientBuilder client);

  @Override
  protected boolean hasRequestId() {
    return false;
  }

  @Test
  public void sendRequestWithMockedResponse() throws Exception {
    AmazonDynamoDBClientBuilder clientBuilder = AmazonDynamoDBClientBuilder.standard();
    AmazonDynamoDB client =
        configureClient(clientBuilder)
            .withEndpointConfiguration(endpoint)
            .withCredentials(credentialsProvider)
            .build();

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.PLAIN_TEXT_UTF_8, ""));

    List<AttributeAssertion> additionalAttributes =
        Arrays.asList(
            equalTo(stringKey("aws.table.name"), "sometable"),
            equalTo(DB_SYSTEM, DYNAMODB),
            equalTo(AWS_DYNAMODB_TABLE_NAMES, singletonList("sometable")));

    Object response = client.createTable(new CreateTableRequest("sometable", null));
    assertRequestWithMockedResponse(
        response, client, "DynamoDBv2", "CreateTable", "POST", additionalAttributes);
  }

  @Test
  public void testGetTableArnWithMockedResponse() {
    AmazonDynamoDBClientBuilder clientBuilder = AmazonDynamoDBClientBuilder.standard();
    AmazonDynamoDB client =
        configureClient(clientBuilder)
            .withEndpointConfiguration(endpoint)
            .withCredentials(credentialsProvider)
            .build();

    String tableName = "MockTable";
    String expectedArn = "arn:aws:dynamodb:us-west-2:123456789012:table/" + tableName;

    String body =
        "{\n"
            + "\"Table\": {\n"
            + "\"TableName\": \""
            + tableName
            + "\",\n"
            + "\"TableArn\": \""
            + expectedArn
            + "\"\n"
            + "}\n"
            + "}";

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.JSON_UTF_8, body));

    String actualArn =
        client
            .describeTable(new DescribeTableRequest().withTableName(tableName))
            .getTable()
            .getTableArn();

    assertEquals("Table ARN should match expected value", expectedArn, actualArn);
  }
}
