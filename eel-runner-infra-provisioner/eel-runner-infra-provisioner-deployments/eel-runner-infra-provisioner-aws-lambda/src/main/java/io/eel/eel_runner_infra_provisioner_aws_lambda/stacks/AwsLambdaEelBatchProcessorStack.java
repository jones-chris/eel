package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import io.eel.eel_runner_infra_provisioner_aws_lambda.StreamLambdaHandler;
import io.eel.eel_runner_infra_provisioner_core.stacks.EelBatchProcessorStack;
import io.eel.eel_runner_infra_provisioner_core.stacks.EelBatchProcessorStackResources;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.scheduler.SchedulerAsyncClient;
import software.amazon.awssdk.services.scheduler.model.ConflictException;
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindow;
import software.amazon.awssdk.services.scheduler.model.Target;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

public class AwsLambdaEelBatchProcessorStack implements EelBatchProcessorStack {

    private final static Logger log = Logger.getLogger(AwsLambdaEelBatchProcessorStack.class.getName());

    private SchedulerAsyncClient schedulerAsyncClient;

    private LambdaClient lambdaClient;

    private S3Client s3Client;

    private SqsClient sqsClient;

    private final Map<String, String> tags = new HashMap<>();

    private final EelBatchProcessorStackResources resources = new EelBatchProcessorStackResources();

    private static final String EEL_TRANSFORMATIONS_BUCKET_NAME = System.getenv("EEL_TRANSFORMATIONS_BUCKET_NAME");

    private AwsLambdaEelBatchProcessorStack() {}

    public AwsLambdaEelBatchProcessorStack(
            SchedulerAsyncClient schedulerAsyncClient,
            LambdaClient lambdaClient,
            S3Client s3Client,
            SqsClient sqsClient
    ) {
        this.schedulerAsyncClient = schedulerAsyncClient;
        this.lambdaClient = lambdaClient;
        this.s3Client = s3Client;
        this.sqsClient = sqsClient;
    }

    @Override
    public void deploy(String canonicalId, String cronExpression) {
        this.buildDeadLetterQueue(canonicalId);
        this.buildEelRuntimePlatform(canonicalId);
        this.buildLandingBucket(canonicalId);
        this.buildLandingBucketTrigger(canonicalId);
        this.buildCronSchedule(canonicalId, cronExpression);
    }

    // https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/javav2/example_code/scheduler/src/main/java/com/example/eventbrideschedule/scenario/EventbridgeSchedulerActions.java#L104
    @Override
    public void buildCronSchedule(String canonicalId, String cronExpression) {
        Target target = Target.builder()
                .arn(this.resources.getRuntimePlatformId())
                .roleArn(roleArn)
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
    public void buildLandingBucket(String canonicalId) {
        try {
            final CreateBucketRequest request = CreateBucketRequest.builder()
                    .bucket(canonicalId)
                    .build();

            CreateBucketResponse response = this.s3Client.createBucket(request);

            this.resources.setLandingBucketId(response.bucketArn());
        } catch (Throwable t) {
            log.severe("Encountered error when trying to create bucket " + canonicalId + ", error message: " + t.getMessage());
            throw t;
        }
    }

    @Override
    public void buildLandingBucketTrigger(String canonicalId) {
        try {
            PutBucketNotificationConfigurationRequest triggerRequest = PutBucketNotificationConfigurationRequest.builder()
                    .bucket(this.resources.getLandingBucketId())
                    .notificationConfiguration(
                            NotificationConfiguration.builder()
                                    .lambdaFunctionConfigurations(
                                            LambdaFunctionConfiguration.builder()
                                                    .lambdaFunctionArn(this.resources.getRuntimePlatformId())
                                                    .events(Event.S3_OBJECT_CREATED_PUT)
                                                    .build()
                                    ).build()
                    ).build();

            this.s3Client.putBucketNotificationConfiguration(triggerRequest);
        } catch (Throwable t) {
            log.severe("Encountered error when trying to create a S3 PUT object bucket trigger for " + canonicalId + ", error message: " + t.getMessage());
            throw t;
        }
    }

    @Override
    public void buildEelRuntimePlatform(String canonicalId) {
        try {
            final CreateFunctionRequest request = CreateFunctionRequest.builder()
                    .runtime(Runtime.JAVA21)
                    .architectures(Architecture.X86_64)
                    .deadLetterConfig(
                            DeadLetterConfig.builder()
                                    .targetArn(this.resources.getDeadLetterQueueId())
                                    .build()
                    ).code(
                            FunctionCode.builder()
                                    .s3Bucket(EEL_TRANSFORMATIONS_BUCKET_NAME) // todo:  add bucket where transformation jar is.
                                    .s3Key(canonicalId) // todo:  add key where transformation jar is.
                                    .build()
                    ).handler() // todo: add handler
                    .tags(this.tags)
                    .build();

            CreateFunctionResponse response = this.lambdaClient.createFunction(request);

            this.resources.setRuntimePlatformId(response.functionArn());
        } catch (Throwable t) {
            log.severe("Encountered error when trying to create bucket " + canonicalId + ", error message: " + t.getMessage());
            throw t;
        }
    }

    @Override
    public void buildDeadLetterQueue(String canonicalId) {
        try {
            final CreateQueueRequest request = CreateQueueRequest.builder()
                    .queueName(canonicalId)
                    .tags(this.tags)
                    .build();

            CreateQueueResponse response = this.sqsClient.createQueue(request);

            this.resources.setDeadLetterQueueId(response.queueUrl());
        } catch (Throwable t) {
            log.severe("Encountered error when trying to create SQS dead letter queue " + canonicalId + ", error message: " + t.getMessage());
            throw t;
        }
    }

}
