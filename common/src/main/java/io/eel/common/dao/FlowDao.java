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

    String generateTransformationStagingPresignedUrl(UUID flowId);

//    void saveFileToS3(Path path);

}
