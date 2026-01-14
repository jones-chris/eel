package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import io.eel.common.EelPackager;
import io.eel.common.model.Flow;
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
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import static io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.FlowComponentStack.ResourceType.*;
import static io.eel.eel_runner_infra_provisioner_aws_lambda.util.Utils.TEN_SECONDS;
import static io.eel.eel_runner_infra_provisioner_aws_lambda.util.Utils.sleep;

public class AwsLambdaFlowComponentStackImpl extends FlowComponentStack {

    private static final String ORIGINAL_EEL_JAR_BUCKET = System.getenv("ORIGINAL_EEL_ARTIFACTS_BUCKET_NAME");

    private static final String ORIGINAL_EEL_JAR_KEY = System.getenv("ORIGINAL_EEL_ARTIFACTS_BUCKET_KEY");

    private static final String EEL_TRANSFORMATIONS_BUCKET_NAME = System.getenv("EEL_TRANSFORMATIONS_BUCKET_NAME");

    /**
     *  The ARN of the AWS managed policy that grants Lambda read/delete access to SQS.
     */
    private static final String SQS_EXECUTION_POLICY_ARN = "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole";

    private final static int ENGINE_TIMEOUT_IN_SECONDS = 120; // 2 minutes

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

    private final String landingBucketId;

    private final String deadLetterQueueId;

    private final String inputQueueArn;

    protected AwsLambdaFlowComponentStackImpl(
            Flow flow,
            Map<String, String> tags,
            S3Client s3Client,
            IamClient iamClient,
            LambdaClient lambdaClient,
            final String landingBucketId,
            final String deadLetterQueueId,
            final String inputQueueArn
    ) {
        super(flow, tags);

        this.iamResourceTags = this.buildTags();
        this.s3Client = s3Client;
        this.iamClient = iamClient;
        this.lambdaClient = lambdaClient;
        this.landingBucketId = landingBucketId;
        this.deadLetterQueueId = deadLetterQueueId;
        this.inputQueueArn = inputQueueArn;

        super.addRollbackAction(RollbackActions.deleteRole(this.iamClient))
                .addRollbackAction(RollbackActions.deleteS3Bucket(this.s3Client))
                .addRollbackAction(RollbackActions.deletePolicy(this.iamClient))
                .addRollbackAction(
                        AWS_LAMBDA_FUNCTION,
                        (resourceId) -> {
                            this.lambdaClient.deleteFunction(
                                    DeleteFunctionRequest.builder()
                                            .functionName(resourceId)
                                            .build()
                            );
                        }
                );
    }

    @Override
    public boolean deploy() {
        try {
            // Build the Lambda EEL jar artifact.
            InputStream originalJarInputStream = this.getS3ObjectAsInputStream(ORIGINAL_EEL_JAR_BUCKET, ORIGINAL_EEL_JAR_KEY);
            InputStream excelInputStream = this.getS3ObjectAsInputStream(EEL_TRANSFORMATIONS_BUCKET_NAME, this.flow.getCanonicalId());

            final File eelJar = EelPackager.build(originalJarInputStream, excelInputStream);

            // Build the Lambda role.
            CreateRoleRequest lambdaCreateRoleRequest = CreateRoleRequest.builder()
                    .roleName("eel-engine-" + this.getFlowId().toString())
                    .tags(this.iamResourceTags)
                    .assumeRolePolicyDocument(ASSUME_ROLE_POLICY_DOCUMENT_FOR_LAMBDA)
                    .build();

            Role lambdaRole = this.iamClient.createRole(lambdaCreateRoleRequest).role();
            this.provisionedResources.put(AWS_IAM_ROLE, lambdaRole.roleName());

            // Build the Lambda role policy.
            CreatePolicyRequest createPolicyRequest = CreatePolicyRequest.builder()
                    .policyName("eel-engine-" + this.getFlowId().toString())
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
                                    this.landingBucketId,
                                    this.landingBucketId,
                                    this.deadLetterQueueId,
                                    this.inputQueueArn
                            )
                    ).tags(iamResourceTags)
                    .build();

            CreatePolicyResponse createPolicyResponse = this.iamClient.createPolicy(createPolicyRequest);

            AttachRolePolicyRequest attachRolePolicyRequest = AttachRolePolicyRequest.builder()
                    .roleName(lambdaRole.roleName())
                    .policyArn(createPolicyResponse.policy().arn())
                    .build();

            this.iamClient.attachRolePolicy(attachRolePolicyRequest);
            this.provisionedResources.put(AWS_IAM_POLICY, createPolicyResponse.policy().arn());

            // Let the current thread sleep so that IAM role and policy are fully registered with IAM before creating the Lambda function.
            sleep(TEN_SECONDS);

            final CreateFunctionRequest request = CreateFunctionRequest.builder()
                    .functionName(this.getFlowId().toString())
                    .role(lambdaRole.arn())
                    .timeout(ENGINE_TIMEOUT_IN_SECONDS)
                    .runtime(Runtime.JAVA21)
                    .architectures(Architecture.X86_64)
                    .deadLetterConfig(
                            DeadLetterConfig.builder()
                                    .targetArn(this.deadLetterQueueId)
                                    .build()
                    ).code(
                            FunctionCode.builder()
                                    .zipFile(SdkBytes.fromInputStream(new FileInputStream(eelJar)))
                                    .build()
                    ).handler("io.eel.engine_deployments_aws_lambda.S3PutObjectHandler")
                    .tags(this.tags)
                    .build();

            CreateFunctionResponse response = this.lambdaClient.createFunction(request);
            this.provisionedResources.put(AWS_LAMBDA_FUNCTION, response.functionName());

//            this.resources.setLambdaRoleArn(response.role());
//            this.resources.setRuntimePlatformId(response.functionArn());

            AttachRolePolicyRequest attachRequest = AttachRolePolicyRequest.builder()
                    .roleName(lambdaRole.roleName())
                    .policyArn(SQS_EXECUTION_POLICY_ARN)
                    .build();

            iamClient.attachRolePolicy(attachRequest);

            return true;
        } catch (Throwable t) {
            log.error("Encountered error when trying to create Lambda Function {}", flow.getId().toString());
            log.error("", t);

            this.rollback();

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

    private Collection<Tag> buildTags() {
        return this.tags.entrySet().stream()
                .map(entry -> Tag.builder().key(entry.getKey()).value(entry.getValue()).build())
                .toList();
    }

}
