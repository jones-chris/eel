package io.eel.flow_api_core.service;

import io.eel.common.model.FlowExecution;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlowExecutionService {

    Optional<FlowExecution> getById(UUID flowId, long executionTimestamp);

    List<FlowExecution> getPageByFlowId(UUID flowId);
}
