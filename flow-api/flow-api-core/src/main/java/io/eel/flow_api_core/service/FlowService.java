package io.eel.flow_api_core.service;

import io.eel.common.model.Flow;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface FlowService {

    Flow createNewFlow();

    Flow updateFlow(Flow flow);

    Optional<Flow> getFlowById(UUID id);

    Set<UUID> getFlowsByUser(String userName);

    String generateTransformationStagingPresignedUrl(UUID flowId);

}
