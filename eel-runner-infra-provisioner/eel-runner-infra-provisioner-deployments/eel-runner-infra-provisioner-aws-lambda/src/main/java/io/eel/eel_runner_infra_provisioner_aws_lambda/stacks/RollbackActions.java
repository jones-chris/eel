package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.FlowComponentStack.ResourceType;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.DeletePolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Consumer;

import static io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.FlowComponentStack.ResourceType.*;
import static io.eel.eel_runner_infra_provisioner_aws_lambda.util.Utils.TEN_SECONDS;
import static io.eel.eel_runner_infra_provisioner_aws_lambda.util.Utils.sleep;

public class RollbackActions {

    public static Map.Entry<ResourceType, Consumer<String>> deleteRole(IamClient iamClient) {
        return new AbstractMap.SimpleEntry<>(
                AWS_IAM_ROLE,
                (resourceId) -> {
                    iamClient.deleteRole(
                            DeleteRoleRequest.builder()
                                    .roleName(resourceId)
                                    .build()
                    );

                    sleep(TEN_SECONDS);
                }
        );
    }

    public static Map.Entry<ResourceType, Consumer<String>> deleteS3Bucket(S3Client s3Client) {
        return new AbstractMap.SimpleEntry<>(
                AWS_S3_BUCKET,
                (resourceId) -> {
                    s3Client.deleteBucket(
                            DeleteBucketRequest.builder()
                                    .bucket(resourceId)
                                    .build()
                    );
                }
        );
    }

    public static Map.Entry<ResourceType, Consumer<String>> deletePolicy(IamClient iamClient) {
        return new AbstractMap.SimpleEntry<>(
                AWS_IAM_POLICY,
                (resourceId) -> {
                    iamClient.deletePolicy(
                            DeletePolicyRequest.builder()
                                    .policyArn(resourceId)
                                    .build()
                    );
                }
        );
    }

}
