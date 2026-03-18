package io.eel.flow_api_core.service;

import io.eel.common.dao.FlowExecutionDao;
import io.eel.common.model.FlowExecution;

import java.util.Optional;
import java.util.UUID;

public class FlowExecutionServiceImpl implements FlowExecutionService {

    private FlowExecutionDao flowExecutionDao;

    public FlowExecutionServiceImpl(FlowExecutionDao flowExecutionDao) {
        this.flowExecutionDao = flowExecutionDao;
    }

    @Override
    public Optional<FlowExecution> getById(UUID flowId, long executionTimestamp) {
        return this.flowExecutionDao.getById(flowId.toString(), executionTimestamp);
    }

}
