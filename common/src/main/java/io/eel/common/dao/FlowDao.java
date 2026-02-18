package io.eel.common.dao;

import io.eel.common.model.Flow;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface FlowDao {

    Set<UUID> getFlowsByUser(String userName);

    Optional<Flow> getFlowByCanonicalId(String canonicalId);

    Flow updateFlow(Flow flow);

    Flow createNewFlow(String author);

    Flow incrementFlow(Flow flow);

    String generateTransformationStagingPresignedUrl(UUID flowId);

}
