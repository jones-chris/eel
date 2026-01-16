package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import io.eel.common.EelPackager;
import io.eel.common.model.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import static io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.Constants.ENGINE_TIMEOUT_IN_SECONDS;
import static io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.Constants.TEN_SECONDS;
import static io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.FlowComponentStack.ResourceType.*;
import static io.eel.eel_runner_infra_provisioner_aws_lambda.util.Utils.sleep;

public class AwsLambdaFlowComponentStackImpl extends FlowComponentStack {

    private static final Logger log = LoggerFactory.getLogger(AwsLambdaFlowComponentStackImpl.class);

    private static final String ORIGINAL_EEL_JAR_BUCKET = System.getenv("ORIGINAL_EEL_ARTIFACTS_BUCKET_NAME");

    private static final String ORIGINAL_EEL_JAR_KEY = System.getenv("ORIGINAL_EEL_ARTIFACTS_BUCKET_KEY");

    private static final String EEL_TRANSFORMATIONS_BUCKET_NAME = System.getenv("EEL_TRANSFORMATIONS_BUCKET_NAME");

    /**
     *  The ARN of the AWS managed policy that grants Lambda read/delete access to SQS.
     */
    private static final String SQS_EXECUTION_POLICY_ARN = "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole";

    private static final String ASSUME_ROLE_POLICY_DOCUMENT_FOR_LAMBDA = """
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

    private final Collection<Tag> iamResourceTags;

    private final S3Client s3Client;

    private final IamClient iamClient;

    private final LambdaClient lambdaClient;

    public AwsLambdaFlowComponentStackImpl(
            S3Client s3Client,
            IamClient iamClient,
            LambdaClient lambdaClient,
            List<FlowComponentStack> dependentStacks
    ) {
        super(dependentStacks);

        this.iamResourceTags = this.buildTags();
        this.s3Client = s3Client;
        this.iamClient = iamClient;
        this.lambdaClient = lambdaClient;

        super.addRollbackAction(RollbackActions.deleteRole(this.iamClient))
                .addRollbackAction(RollbackActions.deleteS3Bucket(this.s3Client))
                .addRollbackAction(RollbackActions.deletePolicy(this.iamClient))
                .addRollbackAction(RollbackActions.deleteLambdaFunction(this.lambdaClient));
    }

    @Override
    public boolean deploy(Flow flow) {
        try {
            // Build the Lambda EEL jar artifact.
            InputStream originalJarInputStream = this.getS3ObjectAsInputStream(ORIGINAL_EEL_JAR_BUCKET, ORIGINAL_EEL_JAR_KEY);
            InputStream excelInputStream = this.getS3ObjectAsInputStream(EEL_TRANSFORMATIONS_BUCKET_NAME, flow.getCanonicalId());

            final File eelJar = EelPackager.build(originalJarInputStream, excelInputStream);

            // Build the Lambda role.
            Role lambdaRole = this.provisionLambdaRole(flow);

            // Build the Lambda role policy.
            this.provisionLambdaRolePolicy(lambdaRole, flow);

            // Build the Lambda Function.
            String lambdaFunctionName = this.provisionLambdaFunction(lambdaRole, eelJar, flow);

            // Add SQS policy to role.
            this.addSqsRolePolicy(lambdaRole);

            // Create event source mapping between input queue and Lambda Function.
            this.provisionEventSourceMapping(lambdaFunctionName);

            return true;
        } catch (Throwable t) {
            log.error("Encountered error when trying to create Lambda Function {}", flow.getId().toString());
            log.error("", t);

            this.rollback(flow);

            return false;
        }
    }

    @Override
    public boolean delete(String flowId, int version) {
        return false;
    }

    private InputStream getS3ObjectAsInputStream(String bucket, String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> s3Object = this.s3Client.getObject(request, ResponseTransformer.toBytes());

        return s3Object.asInputStream();
    }

    private void provisionEventSourceMapping(String lambdaFunctionName) {
        String inputQueueArn = this.getDependentResource(AWS_SQS_INPUT_QUEUE_ARN).orElseThrow();

        CreateEventSourceMappingRequest mappingRequest = CreateEventSourceMappingRequest.builder()
                .eventSourceArn(inputQueueArn) // The ARN of the SQS queue
                .functionName(lambdaFunctionName) // The ARN/Name of the Lambda function
                .batchSize(1) // Number of messages to process in a single batch
                .enabled(true)
                .build();

        String eventSourceMappingArn = this.lambdaClient.createEventSourceMapping(mappingRequest)
                .eventSourceMappingArn();

        this.provisionedResources.put(AWS_LAMBDA_EVENT_SOURCE_MAPPING_ARN, eventSourceMappingArn);
    }

    private Role provisionLambdaRole(Flow flow) {
        CreateRoleRequest lambdaCreateRoleRequest = CreateRoleRequest.builder()
                .roleName("eel-engine-" + flow.getId().toString())
                .tags(this.iamResourceTags)
                .assumeRolePolicyDocument(ASSUME_ROLE_POLICY_DOCUMENT_FOR_LAMBDA)
                .build();

        Role lambdaRole = this.iamClient.createRole(lambdaCreateRoleRequest).role();

        this.provisionedResources.put(AWS_IAM_ROLE_NAME, lambdaRole.roleName());

        return lambdaRole;
    }

    private void provisionLambdaRolePolicy(Role lambdaRole, Flow flow) {
        String landingBucketId = this.getDependentResource(AWS_S3_BUCKET_NAME).orElseThrow();
        String deadLetterQueueArn = this.getDependentResource(AWS_SQS_DEAD_LETTER_QUEUE_URL).orElseThrow();
        String inputQueueArn = this.getDependentResource(AWS_SQS_INPUT_QUEUE_ARN).orElseThrow();

        CreatePolicyRequest createPolicyRequest = CreatePolicyRequest.builder()
                .policyName("eel-engine-" + flow.getId().toString())
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
                                landingBucketId,
                                landingBucketId,
                                deadLetterQueueArn,
                                inputQueueArn
                        )
                ).tags(iamResourceTags)
                .build();

        CreatePolicyResponse createPolicyResponse = this.iamClient.createPolicy(createPolicyRequest);

        AttachRolePolicyRequest attachRolePolicyRequest = AttachRolePolicyRequest.builder()
                .roleName(lambdaRole.roleName())
                .policyArn(createPolicyResponse.policy().arn())
                .build();

        this.iamClient.attachRolePolicy(attachRolePolicyRequest);

        this.provisionedResources.put(AWS_IAM_POLICY_ARN, createPolicyResponse.policy().arn());

        // Let the current thread sleep so that IAM role and policy are fully registered with IAM before creating the Lambda function.
        sleep(TEN_SECONDS);
    }

    private String provisionLambdaFunction(Role lambdaRole, File eelJar, Flow flow) throws FileNotFoundException {
        String deadLetterQueueArn = this.getDependentResource(AWS_SQS_DEAD_LETTER_QUEUE_ARN).orElseThrow();

        final CreateFunctionRequest request = CreateFunctionRequest.builder()
                .functionName(flow.getId().toString())
                .role(lambdaRole.arn())
                .timeout(ENGINE_TIMEOUT_IN_SECONDS)
                .runtime(Runtime.JAVA21)
                .architectures(Architecture.X86_64)
                .deadLetterConfig(
                        DeadLetterConfig.builder()
                                .targetArn(deadLetterQueueArn)
                                .build()
                ).code(
                        FunctionCode.builder()
                                .zipFile(SdkBytes.fromInputStream(new FileInputStream(eelJar)))
                                .build()
                ).handler("io.eel.engine_deployments_aws_lambda.S3PutObjectHandler")
                .tags(this.tags)
                .build();

        String lambdaFunctionName = this.lambdaClient.createFunction(request).functionName();

        this.provisionedResources.put(AWS_LAMBDA_FUNCTION_NAME, lambdaFunctionName);

        return lambdaFunctionName;
    }

    // todo: is this really needed?  Can this be added to the previous create policy call?
    private void addSqsRolePolicy(Role lambdaRole) {
        AttachRolePolicyRequest attachRequest = AttachRolePolicyRequest.builder()
                .roleName(lambdaRole.roleName())
                .policyArn(SQS_EXECUTION_POLICY_ARN)
                .build();

        iamClient.attachRolePolicy(attachRequest);
    }

    private Collection<Tag> buildTags() {
        return this.tags.entrySet().stream()
                .map(entry -> Tag.builder()
                        .key(entry.getKey())
                        .value(entry.getValue())
                        .build()
                ).toList();
    }

}
