package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import io.eel.common.model.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.RollbackActions.ResourceDeletionAttempt.tryToDeleteResource;

public abstract class FlowComponentStack {

    protected static final Logger log = LoggerFactory.getLogger(FlowComponentStack.class);

    protected Map<String, String> tags = new HashMap<>();

    /**
     * An {@link LinkedHashMap} that maintains the order the resources were provisioned.  The key is the unique identifier
     * (most likely something like an ARN) that allows users to quickly access a resource.  An {@link LinkedHashMap} is
     * used here so resources can be rolled back/deleted in the reverse order they were provisioned, if needed.
     */
    protected final LinkedHashMap<ResourceType, String> provisionedResources = new LinkedHashMap<>();

    /**
     * The {@link Flow} encapsulating the data to provision resources for.
     */
    protected Flow flow;

    private Map<ResourceType, RollbackActions.ResourceDeletionAttempt> rollbackActions;

    protected FlowComponentStack(Flow flow) {
        this.flow = flow;
    }

    protected FlowComponentStack(Flow flow, Map<String, String> tags) {
        this.flow = flow;
        this.tags = tags;
    }

//    protected void setRollbackActions(Map<ResourceType, Consumer<String>> rollbackActions) {
//        this.rollbackActions = rollbackActions;
//    }

    protected FlowComponentStack addRollbackAction(Map.Entry<ResourceType, RollbackActions.ResourceDeletionAttempt> rollbackAction) {
        this.rollbackActions.put(rollbackAction.getKey(), rollbackAction.getValue());
        return this;
    }

    protected FlowComponentStack addRollbackAction(ResourceType resourceType, RollbackActions.ResourceDeletionAttempt rollbackAction) {
        this.rollbackActions.put(resourceType, rollbackAction);
        return this;
    }

    public UUID getFlowId() {
        return this.flow.getId();
    }

    public int getFlowVersion() {
        return this.flow.getVersion();
    }

    /**
     * Deploys/provisions the necessary resources in the target platform.
     *
     * @return true if deployment was successful.  Otherwise, false.
     */
    public abstract boolean deploy();

    /**
     * Deletes/rolls back the necessary resources in the target platform.
     *
     * @return true if the rollback was successful.  Otherwise, false.
     */
    public abstract boolean delete(String flowId, int version);

    public final boolean rollback() {
        // If the stack did not provision any resources (ex:  it failed on it's first resource), then just return true.
        if (this.provisionedResources.isEmpty()) {
            log.debug("No provisioned resources in stack {} with flow id {} to rollback", this.getClass().getName(), this.getFlowId());
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
                                    if (!wasSuccessful) allResourcesWereDeleted.set(false);
                                },
                                () -> log.error("Encountered unexpected resource type of {} for flow with id {}", resourceType, this.getFlowId())
                        );
            }

            return allResourcesWereDeleted.get();
        } catch (Throwable t) {
            log.error("", t);

            return false;
        }
    };

    public enum ResourceType {

        AWS_IAM_ROLE,
        AWS_IAM_POLICY,
        AWS_SCHEDULER,
        AWS_S3_BUCKET,
        AWS_LAMBDA_FUNCTION
//        AWS_LAMBDA_ROLE

    }

}
