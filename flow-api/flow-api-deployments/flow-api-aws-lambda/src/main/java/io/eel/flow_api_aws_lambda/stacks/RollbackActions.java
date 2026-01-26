package io.eel.flow_api_aws_lambda.stacks;

import io.eel.flow_api_core.stacks.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.DeleteEventSourceMappingRequest;
import software.amazon.awssdk.services.lambda.model.DeleteFunctionRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleRequest;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.DeleteStateMachineRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.eel.flow_api_aws_lambda.stacks.Constants.TEN_SECONDS;
import static io.eel.flow_api_core.stacks.model.ResourceType.*;
import static io.eel.flow_api_aws_lambda.stacks.util.Utils.sleep;

public class RollbackActions {

    protected static final Logger log = LoggerFactory.getLogger(RollbackActions.class);

    public static Map.Entry<ResourceType, ResourceDeletionAttempt> deleteLambdaEventSourceMapping(LambdaClient lambdaClient) {
        return new AbstractMap.SimpleEntry<>(
                AWS_LAMBDA_EVENT_SOURCE_MAPPING_UUID,
                ResourceDeletionAttempt.of(
                        resourceId -> lambdaClient.deleteEventSourceMapping(
                                DeleteEventSourceMappingRequest.builder()
                                        .uuid(resourceId)
                                        .build()
                        )
                )
        );
    }

    public static Map.Entry<ResourceType, ResourceDeletionAttempt> deleteRole(ResourceType iamRoleNameResourceType, IamClient iamClient) {
        return new AbstractMap.SimpleEntry<>(
                iamRoleNameResourceType,
                iamRoleDeletionAttempt.apply(iamClient)
        );
    }

    public static Map.Entry<ResourceType, ResourceDeletionAttempt> deleteS3Bucket(S3Client s3Client) {
        return new AbstractMap.SimpleEntry<>(
                AWS_S3_BUCKET_NAME,
                ResourceDeletionAttempt.of(
                        resourceId -> {
                            // Empty bucket before deleting it.
                            deleteAllBucketObjects(resourceId, s3Client);

                            // Delete the empty bucket.
                            s3Client.deleteBucket(
                                    DeleteBucketRequest.builder()
                                            .bucket(resourceId)
                                            .build()
                            );
                        }
                )
        );
    }

    public static Map.Entry<ResourceType, ResourceDeletionAttempt> deleteScheduler(SchedulerClient schedulerClient) {
        return new AbstractMap.SimpleEntry<>(
                AWS_SCHEDULER_NAME,
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
                AWS_LAMBDA_FUNCTION_NAME,
                ResourceDeletionAttempt.of(
                        resourceId -> lambdaClient.deleteFunction(
                                DeleteFunctionRequest.builder()
                                        .functionName(resourceId)
                                        .build())
                )
        );
    }

    public static Map.Entry<ResourceType, ResourceDeletionAttempt> deleteInputSqsQueue(SqsClient sqsClient) {
        return new AbstractMap.SimpleEntry<>(
                AWS_SQS_INPUT_QUEUE_URL,
                sqsQueueDeletionAttempt.apply(sqsClient)
        );
    }

    public static Map.Entry<ResourceType, ResourceDeletionAttempt> deleteDeadLetterSqsQueue(SqsClient sqsClient) {
        return new AbstractMap.SimpleEntry<>(
                AWS_SQS_DEAD_LETTER_QUEUE_URL,
                sqsQueueDeletionAttempt.apply(sqsClient)
        );
    }

    public static Map.Entry<ResourceType, ResourceDeletionAttempt> deleteStepFunction(SfnClient stepFunctionClient) {
        return new AbstractMap.SimpleEntry<>(
                AWS_STEP_FUNCTION_ARN,
                ResourceDeletionAttempt.of(
                        resourceId -> stepFunctionClient.deleteStateMachine(
                                DeleteStateMachineRequest.builder()
                                        .stateMachineArn(resourceId)
                                        .build())
                )
        );
    }

    private static final Function<IamClient, ResourceDeletionAttempt> iamRoleDeletionAttempt = iamClient -> ResourceDeletionAttempt.of(
            resourceId  -> {
                // 1. Detach Managed Policies
                ListAttachedRolePoliciesResponse attachedPolicies = iamClient.listAttachedRolePolicies(
                        ListAttachedRolePoliciesRequest.builder()
                                .roleName(resourceId)
                                .build()
                );

                for (AttachedPolicy policy : attachedPolicies.attachedPolicies()) {
                    iamClient.detachRolePolicy(
                            DetachRolePolicyRequest.builder()
                                    .roleName(resourceId)
                                    .policyArn(policy.policyArn())
                                    .build()
                    );
                    log.debug("Detached managed policy {} from role {}", policy.policyName(), resourceId);
                }

                // 2. Delete Inline Policies
                ListRolePoliciesResponse inlinePolicies = iamClient.listRolePolicies(
                        ListRolePoliciesRequest.builder()
                                .roleName(resourceId)
                                .build()
                );

                for (String policyName : inlinePolicies.policyNames()) {
                    iamClient.deleteRolePolicy(
                            DeleteRolePolicyRequest.builder()
                                    .roleName(resourceId)
                                    .policyName(policyName)
                                    .build()
                    );
                    log.debug("Deleted inline policy {} from role {}", policyName, resourceId);
                }

                // 3. Delete the Role
                iamClient.deleteRole(
                        DeleteRoleRequest.builder()
                                .roleName(resourceId)
                                .build()
                );
                log.debug("Role {} deleted successfully", resourceId);

                sleep(TEN_SECONDS);
            }
    );

    private static final Function<SqsClient, ResourceDeletionAttempt> sqsQueueDeletionAttempt = sqsClient -> ResourceDeletionAttempt.of(
            resourceId -> {
                sqsClient.deleteQueue(
                        DeleteQueueRequest.builder()
                                .queueUrl(resourceId)
                                .build()
                );

                // SQS requires a minimum of 60 seconds to expire before a queue can be created with the same
                // name as the one we just deleted.
                sleep(Duration.ofSeconds(60));
            }
    );

    private static void deleteAllBucketObjects(String bucketName, S3Client s3Client) {
        // 1. List all objects in the bucket
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        ListObjectsV2Response listResponse;

        do {
            listResponse = s3Client.listObjectsV2(listRequest);

            if (listResponse.contents().isEmpty()) {
                log.info("Bucket {} is already empty", bucketName);
                return;
            }

            // 2. Extract keys of objects to delete
            List<ObjectIdentifier> keysToDelete = listResponse.contents().stream()
                    .map(s3Object -> ObjectIdentifier.builder().key(s3Object.key()).build())
                    .collect(Collectors.toList());

            // 3. Execute the batch delete
            DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder().objects(keysToDelete).build())
                    .build();

            s3Client.deleteObjects(deleteRequest);
            log.debug("Deleted {} objects", keysToDelete.size());

            // 4. Handle pagination if there are > 1000 objects
            listRequest = listRequest.toBuilder()
                    .continuationToken(listResponse.nextContinuationToken())
                    .build();

        } while (listResponse.isTruncated());
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
