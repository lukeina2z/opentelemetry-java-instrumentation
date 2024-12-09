/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v1_11;

import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.google.common.collect.ImmutableMap;
import io.opentelemetry.testing.internal.armeria.common.HttpResponse;
import io.opentelemetry.testing.internal.armeria.common.HttpStatus;
import io.opentelemetry.testing.internal.armeria.common.MediaType;
import java.util.Map;
import org.junit.jupiter.api.Test;

public abstract class AbstractSnsClientTest extends AbstractBaseAwsClientTest {

  public abstract AmazonSNSClientBuilder configureClient(AmazonSNSClientBuilder client);

  @Override
  protected boolean hasRequestId() {
    return true;
  }

  @Test
  public void testSendRequestWithwithTopicArnMockedResponse() throws Exception {
    AmazonSNSClientBuilder clientBuilder = AmazonSNSClientBuilder.standard();
    AmazonSNS client =
        configureClient(clientBuilder)
            .withEndpointConfiguration(endpoint)
            .withCredentials(credentialsProvider)
            .build();

    String body =
        "<PublishResponse xmlns=\"https://sns.amazonaws.com/doc/2010-03-31/\">"
            + "    <PublishResult>"
            + "        <MessageId>567910cd-659e-55d4-8ccb-5aaf14679dc0</MessageId>"
            + "    </PublishResult>"
            + "    <ResponseMetadata>"
            + "        <RequestId>d74b8436-ae13-5ab4-a9ff-ce54dfea72a0</RequestId>"
            + "    </ResponseMetadata>"
            + "</PublishResponse>";

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.PLAIN_TEXT_UTF_8, body));

    Map<String, String> additionalAttributes =
        ImmutableMap.of(
            MESSAGING_DESTINATION_NAME.toString(), "somearn", "aws.sns.topic.arn", "somearn");

    Object response =
        client.publish(new PublishRequest().withMessage("somemessage").withTopicArn("somearn"));

    assertRequestWithMockedResponse(
        response, client, "SNS", "Publish", "POST", additionalAttributes);
  }

  @Test
  public void testSendRequestWithwithTargetArnMockedResponse() throws Exception {
    AmazonSNSClientBuilder clientBuilder = AmazonSNSClientBuilder.standard();
    AmazonSNS client =
        configureClient(clientBuilder)
            .withEndpointConfiguration(endpoint)
            .withCredentials(credentialsProvider)
            .build();

    String body =
        "<PublishResponse xmlns=\"https://sns.amazonaws.com/doc/2010-03-31/\">"
            + "    <PublishResult>"
            + "        <MessageId>567910cd-659e-55d4-8ccb-5aaf14679dc0</MessageId>"
            + "    </PublishResult>"
            + "    <ResponseMetadata>"
            + "        <RequestId>d74b8436-ae13-5ab4-a9ff-ce54dfea72a0</RequestId>"
            + "    </ResponseMetadata>"
            + "</PublishResponse>";

    server.enqueue(HttpResponse.of(HttpStatus.OK, MediaType.PLAIN_TEXT_UTF_8, body));

    Map<String, String> additionalAttributes =
        ImmutableMap.of(MESSAGING_DESTINATION_NAME.toString(), "somearn");

    Object response =
        client.publish(new PublishRequest().withMessage("somemessage").withTargetArn("somearn"));
    assertRequestWithMockedResponse(
        response, client, "SNS", "Publish", "POST", additionalAttributes);
  }
}
