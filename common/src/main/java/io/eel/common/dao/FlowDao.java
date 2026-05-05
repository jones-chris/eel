package io.eel.common.dao;

import io.eel.common.model.Flow;
import io.eel.common.model.TransformationExtractionType;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface FlowDao {

    Set<UUID> getFlowsByUser(String userName);

    Optional<Flow> getFlowByIdAndVersion(String flowId, int version);

    Set<Integer> getFlowVersions(UUID flowId);

    Flow updateFlow(Flow flow);

    Flow createNewFlow(String author);

    Flow incrementFlow(Flow flow);

    String generateTransformationStagingPresignedUrl(UUID flowId, TransformationExtractionType extractionType);

}
