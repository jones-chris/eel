package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.FlowComponentStack.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.DeletePolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.DeleteFunctionRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleRequest;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Consumer;

import static io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.FlowComponentStack.ResourceType.*;
import static io.eel.eel_runner_infra_provisioner_aws_lambda.util.Utils.TEN_SECONDS;
import static io.eel.eel_runner_infra_provisioner_aws_lambda.util.Utils.sleep;

public class RollbackActions {

    protected static final Logger log = LoggerFactory.getLogger(RollbackActions.class);

    public static Map.Entry<ResourceType, ResourceDeletionAttempt> deleteRole(IamClient iamClient) {
        return new AbstractMap.SimpleEntry<>(
                AWS_IAM_ROLE,
                ResourceDeletionAttempt.of(
                        resourceId  -> {
                            iamClient.deleteRole(
                                    DeleteRoleRequest.builder()
                                            .roleName(resourceId)
                                            .build()
                            );

                            sleep(TEN_SECONDS);
                        }
                )
        );
    }

    public static Map.Entry<ResourceType, ResourceDeletionAttempt> deleteS3Bucket(S3Client s3Client) {
        return new AbstractMap.SimpleEntry<>(
                AWS_S3_BUCKET,
                ResourceDeletionAttempt.of(
                        resourceId -> s3Client.deleteBucket(
                                DeleteBucketRequest.builder()
                                        .bucket(resourceId)
                                        .build())
                )
        );
    }

    public static Map.Entry<ResourceType, ResourceDeletionAttempt> deletePolicy(IamClient iamClient) {
        return new AbstractMap.SimpleEntry<>(
                AWS_IAM_POLICY,
                ResourceDeletionAttempt.of(
                        resourceId -> iamClient.deletePolicy(
                                DeletePolicyRequest.builder()
                                        .policyArn(resourceId)
                                        .build())
                )
        );
    }

    public static Map.Entry<ResourceType, ResourceDeletionAttempt> deleteScheduler(SchedulerClient schedulerClient) {
        return new AbstractMap.SimpleEntry<>(
                AWS_SCHEDULER,
                ResourceDeletionAttempt.of(
                        resourceId -> schedulerClient.deleteSchedule(
                                DeleteScheduleRequest.builder()
                                        .name(resourceId)
                                        .build())
                )
        );
    }

    public static Map.Entry<ResourceType, ResourceDeletionAttempt> deleteLambdaFunction(LambdaClient lambdaClient) {
        return new AbstractMap.SimpleEntry<>(
                AWS_LAMBDA_FUNCTION,
                ResourceDeletionAttempt.of(
                        resourceId -> lambdaClient.deleteFunction(
                                DeleteFunctionRequest.builder()
                                        .functionName(resourceId)
                                        .build())
                )
        );
    }

    public static class ResourceDeletionAttempt {

        private Consumer<String> rollbackAction;

        private ResourceDeletionAttempt() {}

        private ResourceDeletionAttempt(Consumer<String> rollbackAction) {
            this.rollbackAction = rollbackAction;
        }

        public static ResourceDeletionAttempt of(Consumer<String> rollbackAction) {
            return new ResourceDeletionAttempt(rollbackAction);
        }

        public Consumer<String> getRollbackAction() {
            return this.rollbackAction;
        }

        public boolean runRollbackAction(String resourceId) {
            return tryToDeleteResource(resourceId, this.rollbackAction);
        }

        /**
         * This static method is intended to be used for custom/override rollback actions outside of the {@link ResourceDeletionAttempt}
         * class.  While a {@link ResourceDeletionAttempt} instance could be used, sometimes it is more readable to use
         * this method.
         * <p>
         * The user is free to use an instance of {@link ResourceDeletionAttempt} and call {@link ResourceDeletionAttempt#runRollbackAction(String)}
         * or use this method based on which they think the is needed in their situation.
         *
         * @param resourceId The unique id of the resource to attempt to delete.
         * @param rollbackAction The action to take to rollback/delete the resource.
         * @return {@link boolean} True if the resource was deleted/rolled back successfully.  Otherwise, false.  This
         * method **never** throws an exception.
         */
        public static boolean tryToDeleteResource(String resourceId, Consumer<String> rollbackAction) {
            try {
                rollbackAction.accept(resourceId);

                log.debug("Successfully deleted resource with id {}", resourceId);

                return true;
            } catch (Throwable t) {
                log.error("Failed to delete resource {}.  Moving onto next resource", resourceId);
                log.error("", t);

                return false;
            }
        }

    }

}
