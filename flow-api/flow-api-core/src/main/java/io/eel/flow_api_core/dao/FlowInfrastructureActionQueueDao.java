package io.eel.flow_api_core.dao;

import io.eel.eel_runner_infra_provisioner_core.stacks.model.FlowInfrastructureActionRequestDto;

public interface FlowInfrastructureActionQueueDao {

    void sendDeploymentMessage(FlowInfrastructureActionRequestDto flowInfrastructureActionRequestDto);

    void sendDeleteMessage(FlowInfrastructureActionRequestDto flowInfrastructureActionRequestDto);

}
