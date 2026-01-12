package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import io.eel.common.model.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.UUID;

public abstract class FlowComponentStack {

    private static final Logger log = LoggerFactory.getLogger(FlowComponentStack.class);

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

    protected FlowComponentStack(Flow flow) {
        this.flow = flow;
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

    public abstract boolean rollback();

    protected static boolean tryToDeleteResource(String resourceId, Runnable runnable) {
        try {
            runnable.run();

            log.info("Successfully deleted resource with id {}", resourceId);

            return true;
        } catch (Throwable t) {
            log.error("Failed to delete role {}.  Moving onto next resource.", resourceId);
            log.error("", t);

            return false;
        }
    }

    protected enum ResourceType {

        AWS_IAM_ROLE,
        AWS_IAM_POLICY,
        AWS_SCHEDULER

    }

}
