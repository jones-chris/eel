package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import io.eel.common.model.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.RollbackActions.ResourceDeletionAttempt.tryToDeleteResource;

public abstract class FlowComponentStack {

    protected static final Logger log = LoggerFactory.getLogger(FlowComponentStack.class);

    protected Map<String, String> tags = new HashMap<>();

    protected List<FlowComponentStack> dependentStacks = new ArrayList<>();

    /**
     * An {@link LinkedHashMap} that maintains the order the resources were provisioned.  The key is the unique identifier
     * (most likely something like an ARN) that allows users to quickly access a resource.  An {@link LinkedHashMap} is
     * used here so resources can be rolled back/deleted in the reverse order they were provisioned, if needed.
     */
    protected final LinkedHashMap<ResourceType, String> provisionedResources = new LinkedHashMap<>();

    private final Map<ResourceType, RollbackActions.ResourceDeletionAttempt> rollbackActions = new HashMap<>();

    protected FlowComponentStack() {}

    protected FlowComponentStack(List<FlowComponentStack> dependentStacks) {
        this.dependentStacks = dependentStacks;
    }

    protected FlowComponentStack addRollbackAction(Map.Entry<ResourceType, RollbackActions.ResourceDeletionAttempt> rollbackAction) {
        this.rollbackActions.put(rollbackAction.getKey(), rollbackAction.getValue());
        return this;
    }

    protected FlowComponentStack addRollbackAction(ResourceType resourceType, RollbackActions.ResourceDeletionAttempt rollbackAction) {
        this.rollbackActions.put(resourceType, rollbackAction);
        return this;
    }

    /**
     * Deploys/provisions the necessary resources in the target platform.
     *
     * @return true if deployment was successful.  Otherwise, false.
     */
    public abstract boolean deploy(Flow flow);

    /**
     * Deletes/rolls back the necessary resources in the target platform.
     *
     * @return true if the rollback was successful.  Otherwise, false.
     */
    public abstract boolean delete(String flowId, int version);

    public final boolean rollback(Flow flow) {
        // If the stack did not provision any resources (ex:  it failed on it's first resource), then just return true.
        if (this.provisionedResources.isEmpty()) {
            log.debug("No provisioned resources in stack {} with flow id {} to rollback", this.getClass().getName(), flow.getCanonicalId());
            return true;
        }

        // Otherwise, rollback the resources in the reverse order that they were created in.
        AtomicBoolean allResourcesWereDeleted = new AtomicBoolean(true);

        try {
            for (Map.Entry<ResourceType, String> entry : this.provisionedResources.reversed().entrySet()) {
                final ResourceType resourceType = entry.getKey();
                final String resourceId = entry.getValue();

                Optional.ofNullable(rollbackActions.get(resourceType))
                        .ifPresentOrElse(
                                resourceDeletionAttempt -> {
                                    boolean wasSuccessful = tryToDeleteResource(resourceId, resourceDeletionAttempt.getRollbackAction());
                                    if (! wasSuccessful) allResourcesWereDeleted.set(false);
                                },
                                () -> log.error("Did not find rollback action for resource type of {} for flow with canonical id {}", resourceType, flow.getCanonicalId())
                        );
            }

            return allResourcesWereDeleted.get();
        } catch (Throwable t) {
            log.error("", t);

            return false;
        }
    }

    /**
     * Finds the first resource id of the given {@link ResourceType} that it finds among the {@link this#dependentStacks}.
     *
     * @param resourceType {@link ResourceType}
     * @return The resource ID {@link String}
     */
    protected Optional<String> getDependentResource(ResourceType resourceType) {
        return this.dependentStacks.stream()
                .map(stack -> stack.provisionedResources.get(resourceType))
                .findFirst();
    }

    public enum ResourceType {

        // AWS resource types
        AWS_IAM_ROLE,
        AWS_IAM_POLICY_ARN,
        AWS_SCHEDULER_NAME,
        AWS_S3_BUCKET_NAME,
        AWS_LAMBDA_FUNCTION_NAME,
        AWS_SQS_INPUT_QUEUE_URL,
        AWS_SQS_INPUT_QUEUE_ARN,
        AWS_SQS_DEAD_LETTER_QUEUE_URL,
        AWS_SQS_DEAD_LETTER_QUEUE_ARN,
        AWS_STEP_FUNCTION_ARN,

        // GCP resource types
        GCP_IAM_ROLE,
        GCP_IAM_POLICY,
        GCP_SCHEDULER,
        GCP_BUCKET,
        GCP_CLOUD_FUNCTION,
        GCP_PUB_SUB_INPUT_QUEUE_URL,
        GCP_PUB_SUB_DEAD_LETTER_QUEUE_URL

    }

}
