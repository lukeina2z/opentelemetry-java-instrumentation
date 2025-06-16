/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2.internal;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

final class AwsExperimentalAttributes {
  static final AttributeKey<String> AWS_LAMBDA_ARN = stringKey("aws.lambda.function.arn");
  static final AttributeKey<String> AWS_LAMBDA_NAME = stringKey("aws.lambda.function.name");

  private AwsExperimentalAttributes() {}
}
