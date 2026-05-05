package io.eel.common.dao;

import io.eel.common.model.FlowExecution;

import java.util.List;
import java.util.Optional;

public interface FlowExecutionDao {

    FlowExecution save(FlowExecution flowExecution);

    Optional<FlowExecution> getById(String flowId, long executionTimestamp);

    List<FlowExecution> getPageByFlowId(String flowId);

}
