package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.eel.common.EelPackager;
import io.eel.common.model.ScheduledBatchDto;
import io.eel.common.model.ScheduledBatchType;
import io.eel.common.model.StorageLocation;
import io.eel.eel_runner_infra_provisioner_core.stacks.EelBatchProcessorStack;
import io.eel.eel_runner_infra_provisioner_core.stacks.EelBatchProcessorStackResources;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.services.iam.model.Tag;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.scheduler.SchedulerAsyncClient;
import software.amazon.awssdk.services.scheduler.model.ConflictException;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.Target;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

public class AwsLambdaEelBatchProcessorStack implements EelBatchProcessorStack {

    private final static Logger log = Logger.getLogger(AwsLambdaEelBatchProcessorStack.class.getName());

    // The ARN of the AWS managed policy that grants Lambda read/delete access to SQS
    private static final String SQS_EXECUTION_POLICY_ARN = "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole";

    private static final String EEL_TRANSFORMATIONS_BUCKET_NAME = System.getenv("EEL_TRANSFORMATIONS_BUCKET_NAME");

    private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private SchedulerAsyncClient schedulerAsyncClient;

    private LambdaClient lambdaClient;

    private S3Client s3Client;

    private SqsClient sqsClient;

    private IamClient iamClient;

    private final Map<String, String> tags = new HashMap<>();

    private final EelBatchProcessorStackResources resources = new EelBatchProcessorStackResources();

    private AwsLambdaEelBatchProcessorStack() {}

    public AwsLambdaEelBatchProcessorStack(
            SchedulerAsyncClient schedulerAsyncClient,
            LambdaClient lambdaClient,
            S3Client s3Client,
            SqsClient sqsClient,
            IamClient iamClient
    ) {
        this.schedulerAsyncClient = schedulerAsyncClient;
        this.lambdaClient = lambdaClient;
        this.s3Client = s3Client;
        this.sqsClient = sqsClient;
        this.iamClient = iamClient;

        this.resources.setRuntimePlatformId("arn:aws:lambda:us-east-1:some_account:function:8817065c-0e13-43ca-978f-544e899365e1v0");
        this.resources.setDeadLetterQueueId("arn:aws:sqs:us-east-1:some_account:dlq-8817065c-0e13-43ca-978f-544e899365e1v0");
    }

    @Override
    public void deploy(String canonicalId, String cronExpression, String flowId) {
        this.buildLandingBucket(flowId);
        this.buildInputQueue(flowId);
        this.buildDeadLetterQueue(flowId);
        this.buildEelRuntimePlatform(flowId, canonicalId);
        this.buildLandingBucketTrigger(flowId);
        this.buildInputQueueLambdaEventSourceMapping();
//        this.buildCronSchedule(flowId, cronExpression);
    }

    // https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/javav2/example_code/scheduler/src/main/java/com/example/eventbrideschedule/scenario/EventbridgeSchedulerActions.java#L104
    @Override
    public void buildCronSchedule(String canonicalId, String cronExpression) {
        final String input = gson.toJson(
                new ScheduledBatchDto(
                        ScheduledBatchType.ZIP_OF_CSV_FILES,
                        new StorageLocation("eel-input-8817065c-0e13-43ca-978f-544e899365e1", "eel_data.zip")
                )
        );

        Target target = Target.builder()
                .arn(this.resources.getRuntimePlatformId())
                .roleArn(this.resources.getLambdaRoleArn())
                .input(input)
                .build();

        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .name(canonicalId)
                .scheduleExpression(cronExpression)
                .target(target)
                .startDate(Instant.now())
                .build();

        this.schedulerAsyncClient.createSchedule(request)
                .thenApply(response -> {
                    String schedulerArn = response.scheduleArn();
                    this.resources.setCronScheduleId(schedulerArn);

                    log.info("Successfully created schedule {} " + canonicalId + ", The ARN is " + schedulerArn);

                    return true;
                })
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        if (ex instanceof ConflictException) {
                            log.severe("A conflict exception occurred while creating the schedule: " + ex.getMessage());

                            throw new CompletionException("A conflict exception occurred while creating the schedule: " + ex.getMessage(), ex);
                        } else {
                            throw new CompletionException("Error creating schedule: " + ex.getMessage(), ex);
                        }
                    }
                });
    }

    @Override
    public void buildLandingBucket(String flowId) {
        try {
            final String bucketName = "eel-input-" + flowId;

            final CreateBucketRequest request = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            this.s3Client.createBucket(request);

            this.resources.setLandingBucketId(bucketName);
        } catch (Throwable t) {
            log.severe("Encountered error when trying to create bucket " + flowId + ", error message: " + t.getMessage());
            throw t;
        }
    }

    @Override
    public void buildLandingBucketTrigger(String flowId) {
        try {
            PutBucketNotificationConfigurationRequest triggerRequest = PutBucketNotificationConfigurationRequest.builder()
                    .bucket(this.resources.getLandingBucketId())
                    .notificationConfiguration(
                            NotificationConfiguration.builder()
                                    .queueConfigurations(
                                            QueueConfiguration.builder()
                                                    .events(Event.S3_OBJECT_CREATED_PUT)
                                                    .queueArn(this.resources.getInputQueueArn())
                                                    .build()
                                    ).build()
                    ).build();

            PutBucketNotificationConfigurationResponse response = this.s3Client.putBucketNotificationConfiguration(triggerRequest);
        } catch (Throwable t) {
            log.severe("Encountered error when trying to create a S3 PUT object bucket trigger for " + flowId + ", error message: " + t.getMessage());
            throw t;
        }
    }

    @Override
    public void buildEelRuntimePlatform(String flowId, String canonicalId) {
        final String originalEelJarBucket = System.getenv("ORIGINAL_EEL_ARTIFACTS_BUCKET_NAME");
        final String originalEelJarKey = System.getenv("ORIGINAL_EEL_ARTIFACTS_BUCKET_KEY");

        InputStream originalJarInputStream = this.getS3ObjectAsInputStream(originalEelJarBucket, originalEelJarKey);
        InputStream excelInputStream = this.getS3ObjectAsInputStream(EEL_TRANSFORMATIONS_BUCKET_NAME, canonicalId);

        final File eelJar = EelPackager.build(originalJarInputStream, excelInputStream);

        // todo:  build role.
        final String ASSUME_ROLE_POLICY_DOCUMENT_FOR_LAMBDA = """
        {
          "Version": "2012-10-17",
          "Statement": [
            {
              "Effect": "Allow",
              "Principal": {
                "Service": "lambda.amazonaws.com"
              },
              "Action": "sts:AssumeRole"
            }
          ]
        }
        """;

        Collection<Tag> lambdaRoleTags = new ArrayList<>();
        this.tags.forEach((key, value) -> {
            lambdaRoleTags.add(
                    Tag.builder().key(key).value(value).build()
            );
        });
        CreateRoleRequest lambdaCreateRoleRequest = CreateRoleRequest.builder()
                .roleName(flowId)
                .tags(lambdaRoleTags)
                .assumeRolePolicyDocument(ASSUME_ROLE_POLICY_DOCUMENT_FOR_LAMBDA)
                .build();

        Role lambdaRole;
        try {
            CreateRoleResponse response = this.iamClient.createRole(lambdaCreateRoleRequest);
            lambdaRole = response.role();
        } catch (Throwable t) {
            log.severe("Encountered error when creating the lambda role " + flowId + ", error message: " + t.getMessage());
            throw new RuntimeException(t);
        }

        try {
            CreatePolicyRequest createPolicyRequest = CreatePolicyRequest.builder()
                    .policyName(flowId)
                    .policyDocument(
                            """
                                    {
                                      "Version": "2012-10-17",
                                      "Statement": [
                                        {
                                            "Effect": "Allow",
                                            "Action": [
                                                "s3:Get*"
                                            ],
                                            "Resource": [
                                                "arn:aws:s3:::%s",
                                                "arn:aws:s3:::%s/*"
                                            ]
                                        },
                                        {
                                            "Effect": "Allow",
                                            "Action": [
                                                "sqs:*",
                                                "sqs:SendMessage"
                                            ],
                                            "Resource": [
                                                "%s",
                                                "%s"
                                            ]
                                        }
                                      ]
                                    }
                                    """.formatted(
                                        this.resources.getLandingBucketId(),
                                        this.resources.getLandingBucketId(),
                                        this.resources.getDeadLetterQueueId(),
                                        this.resources.getInputQueueArn()
                                    )
                    ).tags(lambdaRoleTags)
                    .build();

            CreatePolicyResponse createPolicyResponse = this.iamClient.createPolicy(createPolicyRequest);

            AttachRolePolicyRequest attachRolePolicyRequest = AttachRolePolicyRequest.builder()
                    .roleName(lambdaRole.roleName())
                    .policyArn(createPolicyResponse.policy().arn())
                    .build();

            this.iamClient.attachRolePolicy(attachRolePolicyRequest);
            this.resources.setLambdaRolePolicyArn(createPolicyResponse.policy().arn());
        } catch (Throwable t) {
            log.severe("Encountered error when creating the lambda role policy " + flowId + ", error message: " + t.getMessage());
            throw new RuntimeException(t);
        }

        try {
            final CreateFunctionRequest request = CreateFunctionRequest.builder()
                    .functionName(flowId)
                    .role(lambdaRole.arn())
                    .runtime(Runtime.JAVA21)
                    .architectures(Architecture.X86_64)
                    .deadLetterConfig(
                            DeadLetterConfig.builder()
                                    .targetArn(this.resources.getDeadLetterQueueId())
                                    .build()
                    ).code(
                            FunctionCode.builder()
                                .zipFile(SdkBytes.fromInputStream(new FileInputStream(eelJar)))
                                .build()
                    ).handler("io.eel.engine_deployments_aws_lambda.S3PutObjectHandler")
                    .tags(this.tags)
                    .build();

            CreateFunctionResponse response = this.lambdaClient.createFunction(request);

            this.resources.setLambdaRoleArn(response.role());
            this.resources.setRuntimePlatformId(response.functionArn());

            log.info("Attaching SQS execution policy to role: " + lambdaRole);
            try {
                AttachRolePolicyRequest attachRequest = AttachRolePolicyRequest.builder()
                        .roleName(lambdaRole.roleName())
                        .policyArn(SQS_EXECUTION_POLICY_ARN)
                        .build();

                iamClient.attachRolePolicy(attachRequest);
                log.info("Successfully attached SQS execution policy.");
            } catch (Exception e) {
                log.severe("Failed to attach SQS policy to IAM role: " + e.getMessage());
                // It's possible the policy is already attached, but we handle other errors.
                throw new RuntimeException("IAM Role configuration failed.", e);
            }
        } catch (Throwable t) {
            log.severe("Encountered error when trying to create bucket " + canonicalId + ", error message: " + t.getMessage());
            throw new RuntimeException(t);
        }
    }

    @Override
    public void buildDeadLetterQueue(String flowId) {
        try {
            final CreateQueueRequest request = CreateQueueRequest.builder()
                    .queueName("dlq-" + flowId)
                    .tags(this.tags)
                    .build();

            CreateQueueResponse response = this.sqsClient.createQueue(request);

            GetQueueAttributesRequest getQueueAttributesRequest = GetQueueAttributesRequest.builder()
                    .queueUrl(response.queueUrl())
                    .attributeNames(QueueAttributeName.QUEUE_ARN)
                    .build();

            GetQueueAttributesResponse getQueueAttributesResponse = this.sqsClient.getQueueAttributes(getQueueAttributesRequest);

            this.resources.setDeadLetterQueueId(getQueueAttributesResponse.attributes().get(QueueAttributeName.QUEUE_ARN));
        } catch (Throwable t) {
            log.severe("Encountered error when trying to create SQS dead letter queue " + flowId + ", error message: " + t.getMessage());
            throw t;
        }
    }

    private void buildInputQueue(String flowId) {
        try {
            final CreateQueueRequest request = CreateQueueRequest.builder()
                    .queueName("eel-input-" + flowId)
                    .tags(this.tags)
                    .build();

            CreateQueueResponse response = this.sqsClient.createQueue(request);
            final String queueUrl = response.queueUrl();

            GetQueueAttributesRequest getQueueAttributesRequest = GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.QUEUE_ARN)
                    .build();

            GetQueueAttributesResponse getQueueAttributesResponse = this.sqsClient.getQueueAttributes(getQueueAttributesRequest);

            String inputQueueArn = getQueueAttributesResponse.attributes().get(QueueAttributeName.QUEUE_ARN);
            this.resources.setInputQueueArn(inputQueueArn);

            // 1. Construct the policy JSON string.  The policy grants S3 permission to send messages to the queue.
            String policyJson = String.format("""
                {
                  "Version": "2012-10-17",
                  "Id": "SQS-Policy-For-S3-Notification",
                  "Statement": [
                    {
                      "Sid": "AllowS3ToSendMessage",
                      "Effect": "Allow",
                      "Principal": {
                        "Service": "s3.amazonaws.com"
                      },
                      "Action": "sqs:SendMessage",
                      "Resource": "%s",
                      "Condition": {
                        "ArnEquals": {
                          "aws:SourceArn": "arn:aws:s3:::%s"
                        }
                      }
                    }
                  ]
                }
                """,
                    this.resources.getInputQueueArn(),
                    this.resources.getLandingBucketId()
            );

            SetQueueAttributesRequest setAttributesRequest = SetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl) // SQS requires the Queue URL
                    .attributesWithStrings(
                            // Use the POLICY attribute name to pass the JSON string
                            Map.of(QueueAttributeName.POLICY.toString(), policyJson)
                    )
                    .build();

            // 3. Execute the request
            sqsClient.setQueueAttributes(setAttributesRequest);

            log.info("Successfully set SQS policy for S3 notifications on queue: " + queueUrl);
        } catch (Throwable t) {
            log.severe("Encountered error when trying to create SQS dead letter queue " + flowId + ", error message: " + t.getMessage());
            throw t;
        }
    }

    private void buildInputQueueLambdaEventSourceMapping() {
        try {
            log.info("Creating Event Source Mapping between SQS and Lambda...");
            CreateEventSourceMappingRequest mappingRequest = CreateEventSourceMappingRequest.builder()
                    .eventSourceArn(this.resources.getInputQueueArn()) // The ARN of the SQS queue
                    .functionName(this.resources.getRuntimePlatformId()) // The ARN/Name of the Lambda function
                    .batchSize(1) // Number of messages to process in a single batch
                    .enabled(true)
                    .build();

            CreateEventSourceMappingResponse mappingResponse = this.lambdaClient.createEventSourceMapping(mappingRequest);

            this.resources.setEventSourceMappingArn(mappingResponse.eventSourceMappingArn());
            log.info("Event Source Mapping created successfully. UUID: " + mappingResponse.eventSourceMappingArn());
        } catch (Exception e) {
            log.severe("Failed to create Event Source Mapping. The Lambda role may still lack permissions: " + e.getMessage());
            throw new RuntimeException("Event Source Mapping failed.", e);
        }
    }

    private InputStream getS3ObjectAsInputStream(String bucket, String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> s3Object = this.s3Client.getObject(request, ResponseTransformer.toBytes());

        return s3Object.asInputStream();
    }

}
