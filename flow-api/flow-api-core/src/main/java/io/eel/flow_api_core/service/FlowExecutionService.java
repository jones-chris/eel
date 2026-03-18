package io.eel.flow_api_core.service;

import io.eel.common.model.FlowExecution;

import java.util.Optional;
import java.util.UUID;

public interface FlowExecutionService {

    Optional<FlowExecution> getById(UUID flowId, long executionTimestamp);

}
