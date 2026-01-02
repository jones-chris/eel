package io.eel.flow_api_core.service;

import io.eel.common.model.Flow;
import io.eel.flow_api_core.exception.ImmutableFlowException;
import io.eel.flow_api_core.exception.ResourceNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface FlowService {

    Flow createNewFlow();

    Flow updateFlow(String canonicalId, Flow newFlow) throws ImmutableFlowException, ResourceNotFoundException;

    Flow incrementFlow(Flow flow);

    Optional<Flow> getFlowByCanonicalId(String canonicalId);

    Set<UUID> getFlowsByUser(String userName);

    String generateTransformationStagingPresignedUrl(UUID flowId);

    Flow finalizeFlow(String canonicalId) throws ResourceNotFoundException;

}
