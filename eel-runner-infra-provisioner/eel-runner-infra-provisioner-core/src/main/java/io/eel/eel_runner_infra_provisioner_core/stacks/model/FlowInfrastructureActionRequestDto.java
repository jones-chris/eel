package io.eel.eel_runner_infra_provisioner_core.stacks.model;

import io.eel.common.model.Flow;

import java.util.UUID;

public record FlowInfrastructureActionRequestDto(
        UUID flowId,
        int version,
        InfrastructureAction infrastructureAction
) {

    public static FlowInfrastructureActionRequestDto newDeploymentRequest(Flow flow) {
        return new FlowInfrastructureActionRequestDto(flow.getId(), flow.getVersion(), InfrastructureAction.DEPLOY);
    }

    public static FlowInfrastructureActionRequestDto newDeletionRequest(Flow flow) {
        return new FlowInfrastructureActionRequestDto(flow.getId(), flow.getVersion(), InfrastructureAction.DELETE);
    }

    public enum InfrastructureAction {

        DEPLOY,
        DELETE

    }

}
