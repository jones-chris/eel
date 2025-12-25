package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import com.amazonaws.services.stepfunctions.builder.StateMachine;
import com.amazonaws.services.stepfunctions.builder.states.Branch;
import com.amazonaws.services.stepfunctions.builder.states.ParallelState;
import com.amazonaws.services.stepfunctions.builder.states.State;
import com.amazonaws.services.stepfunctions.builder.states.TaskState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.eel.common.EelPackager;
import io.eel.common.model.Query;
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
import software.amazon.awssdk.services.lambda.model.DeadLetterConfig;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.scheduler.SchedulerAsyncClient;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.*;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.CreateStateMachineRequest;
import software.amazon.awssdk.services.sfn.model.CreateStateMachineResponse;
import software.amazon.awssdk.services.sfn.model.StateMachineType;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

import static com.amazonaws.services.stepfunctions.builder.StepFunctionBuilder.*;
import static java.util.Map.*;

public class AwsLambdaEelBatchProcessorStack implements EelBatchProcessorStack {

    private final static Logger log = Logger.getLogger(AwsLambdaEelBatchProcessorStack.class.getName());

    // The ARN of the AWS managed policy that grants Lambda read/delete access to SQS
    private static final String SQS_EXECUTION_POLICY_ARN = "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole";

    private static final String EEL_TRANSFORMATIONS_BUCKET_NAME = System.getenv("EEL_TRANSFORMATIONS_BUCKET_NAME");

    private static final String EEL_QUERY_RUNNER_ARN = System.getenv("EEL_QUERY_RUNNER_ID");

    private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private String inputQueueUrl;

    private SchedulerClient schedulerClient;

    private LambdaClient lambdaClient;

    private S3Client s3Client;

    private SqsClient sqsClient;

    private IamClient iamClient;

    private SfnClient stepFunctionsClient;

    private final Map<String, String> tags = new HashMap<>();

    private final EelBatchProcessorStackResources resources = new EelBatchProcessorStackResources();

    private AwsLambdaEelBatchProcessorStack() {}

    public AwsLambdaEelBatchProcessorStack(
            SchedulerClient schedulerClient,
            LambdaClient lambdaClient,
            S3Client s3Client,
            SqsClient sqsClient,
            IamClient iamClient,
            SfnClient stepFunctionsClient
    ) {
        this.schedulerClient = schedulerClient;
        this.lambdaClient = lambdaClient;
        this.s3Client = s3Client;
        this.sqsClient = sqsClient;
        this.iamClient = iamClient;
        this.stepFunctionsClient = stepFunctionsClient;

//        this.resources.setRuntimePlatformId("arn:aws:lambda:us-east-1:some_account:function:8817065c-0e13-43ca-978f-544e899365e1v0");
        this.inputQueueUrl = "https://sqs.us-east-1.amazonaws.com/526661363425/eel-input-b6ec0a10-ef89-4c0f-9ce9-4e516b942a17";
//        this.resources.setLambdaRolePolicyArn("arn:aws:iam::526661363425:policy/eel-engine-8817065c-0e13-43ca-978f-544e899365e1");
        this.resources.setDeadLetterQueueId("arn:aws:sqs:us-east-1:526661363425:eel-input-b6ec0a10-ef89-4c0f-9ce9-4e516b942a17");
        this.resources.setLandingBucketId("eel-input-b6ec0a10-ef89-4c0f-9ce9-4e516b942a17");
        this.resources.setInputQueueArn("arn:aws:sqs:us-east-1:526661363425:eel-input-b6ec0a10-ef89-4c0f-9ce9-4e516b942a17");
//        this.resources.setRuntimePlatformId("arn:aws:lambda:us-east-1:526661363425:function:8817065c-0e13-43ca-978f-544e899365e1");
//        this.resources.setStepFunctionStateMachineArn("arn:aws:states:us-east-1:526661363425:stateMachine:8817065c-0e13-43ca-978f-544e899365e1");
//        this.resources.setLambdaRoleArn("arn:aws:iam::526661363425:role/eel-engine-8817065c-0e13-43ca-978f-544e899365e1");
//        this.resources.setEventSourceMappingArn("arn:aws:lambda:us-east-1:526661363425:event-source-mapping:c5d8d25b-57a2-49e6-a7b8-5312f033c5fe");
    }

    @Override
    public void deploy(String canonicalId, String cronExpression, String flowId, int version, Set<String> sheetNames) {
        this.buildLandingBucket(flowId);
        this.buildInputQueue(flowId);
        this.buildDeadLetterQueue(flowId);
        this.buildEelRuntimePlatform(flowId, canonicalId);
//
        // todo: may be able to remove this.
//        this.buildLandingBucketTrigger(flowId);

//        this.buildInputQueueLambdaEventSourceMapping();
        this.buildQueryStepFunction(flowId, version, sheetNames);
//        this.buildCronSchedule(canonicalId, cronExpression, flowId);
    }

    // https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/javav2/example_code/scheduler/src/main/java/com/example/eventbrideschedule/scenario/EventbridgeSchedulerActions.java#L104
    @Override
    public void buildCronSchedule(String canonicalId, String cronExpression, String flowId) {
        try {
            // Create the Scheduler role and policy so that Scheduler can assume the role and invoke the SFN.
            String trustPolicy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {
                        "Service": "scheduler.amazonaws.com"
                      },
                      "Action": "sts:AssumeRole"
                    }
                  ]
                }
                """;

            Role role = this.iamClient.createRole(
                    CreateRoleRequest.builder()
                            .roleName("eel-scheduler-" + flowId)
                            .assumeRolePolicyDocument(trustPolicy)
                            .build()
            ).role();

            Thread.sleep(Duration.ofSeconds(10));

            String permissionsPolicy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Action": "states:StartExecution",
                            "Resource": "%s"
                        }
                    ]
                }
                """.formatted(this.resources.getStepFunctionStateMachineArn());

            this.iamClient.putRolePolicy(PutRolePolicyRequest.builder()
                    .roleName(role.roleName())
                    .policyName(role.roleName())
                    .policyDocument(permissionsPolicy)
                    .build());

            Thread.sleep(Duration.ofSeconds(10));

            // Create the Scheduler instance.
//            final String input = gson.toJson(
//                    Map.of("canonicalId", canonicalId)
//            );

            Target target = Target.builder()
                    .arn(this.resources.getStepFunctionStateMachineArn())
                    .roleArn(role.arn())
//                    .input(input) // todo: is this required?  If so, does it need to be null or an empty string?
                    .build();

            CreateScheduleRequest request = CreateScheduleRequest.builder()
                    .name(flowId)
                    .scheduleExpression(cronExpression)
                    .target(target)
                    .startDate(Instant.now())
                    .flexibleTimeWindow(FlexibleTimeWindow.builder().mode(FlexibleTimeWindowMode.OFF).build())
                    .build();

            CreateScheduleResponse response = this.schedulerClient.createSchedule(request);
            String schedulerArn = response.scheduleArn();
            this.resources.setCronScheduleId(schedulerArn);
            log.info("Successfully created schedule {} " + flowId + ", The ARN is " + schedulerArn);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException("Error creating schedule: " + t.getMessage());
        }
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

    // todo:  may not need this.
//    @Override
//    public void buildLandingBucketTrigger(String flowId) {
//        try {
//            PutBucketNotificationConfigurationRequest triggerRequest = PutBucketNotificationConfigurationRequest.builder()
//                    .bucket(this.resources.getLandingBucketId())
//                    .notificationConfiguration(
//                            NotificationConfiguration.builder()
//                                    .queueConfigurations(
//                                            QueueConfiguration.builder()
//                                                    .events(Event.S3_OBJECT_CREATED_PUT)
//                                                    .queueArn(this.resources.getInputQueueArn())
//                                                    .build()
//                                    ).build()
//                    ).build();
//
//            PutBucketNotificationConfigurationResponse response = this.s3Client.putBucketNotificationConfiguration(triggerRequest);
//        } catch (Throwable t) {
//            log.severe("Encountered error when trying to create a S3 PUT object bucket trigger for " + flowId + ", error message: " + t.getMessage());
//            throw t;
//        }
//    }

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
                .roleName("eel-engine-" + flowId)
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
                    .policyName("eel-engine-" + flowId)
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

            // Let the current thread sleep so that IAM role and policy are fully registered with IAM before creating the Lambda function.
            Thread.sleep(Duration.ofSeconds(10));
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

    private State.Builder buildQueryRunnerState(String flowId, int version, String sheetName) {
        return TaskState.builder()
                .resource("arn:aws:states:::lambda:invoke") // Use the optimized resource
                .parameters(
                        Map.of(
                                "FunctionName", EEL_QUERY_RUNNER_ARN,
                                "Payload", Map.of(
                                        "flowId", flowId,
                                        "version", version,
                                        "inputSheet", sheetName,
                                        "destinationBucket", this.resources.getLandingBucketId()
                                )
                        )
                ).transition(end());
    }

    public void buildQueryStepFunction(String flowId, int version, Set<String> sheetNames) {
        try {
            // 2. Create the Parallel State with two identical branches
            ParallelState.Builder parallelProcessing = ParallelState.builder()
                    .comment("Run the sheet queries in parallel");

            for (String sheetName : sheetNames) {
                State.Builder queryRunnerState = this.buildQueryRunnerState(flowId, version, sheetName);
                parallelProcessing.branch(Branch.builder().startAt(sheetName).state(sheetName, queryRunnerState));
            }

            parallelProcessing.transition(next("NotifySQS"));

            // 3. Define the SQS Task
            // We use Parameters to format the message using the output from the parallel branches
            State.Builder sendToSqs = TaskState.builder()
                    .resource("arn:aws:states:::sqs:sendMessage")
                    .parameters(
                            Map.of(
                                    "QueueUrl", this.inputQueueUrl,
                                    "MessageBody.$", "$"
                            )
                    ).transition(end());

            // 4. Assemble the State Machine
            StateMachine stateMachine = StateMachine.builder()
                    .startAt("ParallelProcessing")
                    .state("ParallelProcessing", parallelProcessing)
                    .state("NotifySQS", sendToSqs)
                    .build();

            // 5. Create the IAM policy
            String eelFlowsDynamoDbTableArn = System.getenv("EEL_FLOWS_TABLE_ARN");

            String trustPolicy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [{
                    "Effect": "Allow",
                    "Principal": { "Service": "states.amazonaws.com" },
                    "Action": "sts:AssumeRole"
                  }]
                }
                """;

            // 2. Create the Role
            Role role = this.iamClient.createRole(
                    CreateRoleRequest.builder()
                            .roleName("eel-sfn-" + flowId)
                            .assumeRolePolicyDocument(trustPolicy)
                            .build()
            ).role();

            // 3. Define the Permissions Policy using Text Blocks and formatted variables
            String permissionsPolicy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Action": "s3:PutObject",
                      "Resource": "arn:aws:s3:::%s/*"
                    },
                    {
                      "Effect": "Allow",
                      "Action": "dynamodb:GetItem",
                      "Resource": "%s"
                    },
                    {
                      "Effect": "Allow",
                      "Action": "lambda:InvokeFunction",
                      "Resource": "%s"
                    },
                    {
                      "Effect": "Allow",
                      "Action": "sqs:SendMessage",
                      "Resource": "%s"
                    }
                  ]
                }
                """.formatted(
                        this.resources.getLandingBucketId(),
                        eelFlowsDynamoDbTableArn,
                        System.getenv("EEL_QUERY_RUNNER_ARN"),
                        this.resources.getInputQueueArn()
                );

            // 4. Attach the policy as an Inline Policy
            this.iamClient.putRolePolicy(PutRolePolicyRequest.builder()
                    .roleName(role.roleName())
                    .policyName(role.roleName())
                    .policyDocument(permissionsPolicy)
                    .build());

            CreateStateMachineRequest machineRequest = CreateStateMachineRequest.builder()
                    .definition(stateMachine.toPrettyJson())
                    .name(flowId)
                    .roleArn(role.arn())
                    .type(StateMachineType.STANDARD)
                    .build();

            CreateStateMachineResponse response = this.stepFunctionsClient.createStateMachine(machineRequest);

            final String stateMachineArn = response.stateMachineArn();
            log.info("Successfully created SFN state machine: " + stateMachineArn);
            this.resources.setStepFunctionStateMachineArn(stateMachineArn);
        } catch (Throwable t) {
            log.severe("Encountered error when trying to create Step Function State Machine " + flowId + ", error message: " + t.getMessage());
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
            this.inputQueueUrl = queueUrl;

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
                            of(QueueAttributeName.POLICY.toString(), policyJson)
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
