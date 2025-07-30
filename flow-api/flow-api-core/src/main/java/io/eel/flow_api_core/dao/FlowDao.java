package io.eel.flow_api_core.dao;

import io.eel.common.model.Flow;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface FlowDao {

    Set<UUID> getFlowsByUser(String userName);

    Optional<Flow> getFlowById(UUID id);

}
