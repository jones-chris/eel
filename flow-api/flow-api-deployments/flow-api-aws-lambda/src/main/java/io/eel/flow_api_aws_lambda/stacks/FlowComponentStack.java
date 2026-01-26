package io.eel.flow_api_aws_lambda.stacks;

import io.eel.common.model.Flow;
import io.eel.flow_api_core.stacks.model.ResourceType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.eel.flow_api_aws_lambda.stacks.RollbackActions.ResourceDeletionAttempt.tryToDeleteResource;

@Slf4j
public abstract class FlowComponentStack {

    protected Map<String, String> tags = new HashMap<>();

    protected List<FlowComponentStack> dependentStacks = new ArrayList<>();

    /**
     * An {@link LinkedHashMap} that maintains the order the resources were provisioned.  The key is the unique identifier
     * (most likely something like an ARN) that allows users to quickly access a resource.  An {@link LinkedHashMap} is
     * used here so resources can be rolled back/deleted in the reverse order they were provisioned, if needed.
     */
    @Getter
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
     * Adds a {@link ResourceType} that is expected to be provisioned when this stack is deployed.  Care should be taken
     * to order the {@param resourceTypes} in the same order that the stack will provision them, because insertion/creation
     * order is vital for rollback/deletion to succeed.
     *
     * @param resourceTypes Variable number of {@link ResourceType}s that are expected to be provisioned when deploying
     *                      this stack.
     */
    protected void addExpectedProvisionedResources(ResourceType... resourceTypes) {
        for (ResourceType resourceType : resourceTypes) {
            this.provisionedResources.put(resourceType, null);
        }
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
                                    boolean wasSuccessful = resourceDeletionAttempt.runRollbackAction(resourceId);
//                                    boolean wasSuccessful = tryToDeleteResource(resourceId, resourceDeletionAttempt.getRollbackAction());
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
     * Finds the first resource id of the given {@link ResourceType} that it finds among the dependent stacks.
     *
     * @param resourceType {@link ResourceType}
     * @return The resource ID {@link String}
     */
    protected Optional<String> getDependentResource(ResourceType resourceType) {
        for (FlowComponentStack dependentStack : this.dependentStacks) {
            Optional<String> resourceIdOptional = Optional.ofNullable(dependentStack.provisionedResources.get(resourceType));

            if (resourceIdOptional.isPresent()) {
                return resourceIdOptional;
            }
        }

        return Optional.empty();
    }

}
