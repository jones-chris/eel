package io.eel.flow_api_core.service;

import io.eel.common.model.Flow;
import io.eel.flow_api_core.dao.FlowDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class FlowServiceImpl implements FlowService {

    private static final Logger log = LogManager.getLogger(FlowServiceImpl.class);
    private FlowDao flowDao;

    private FlowServiceImpl() {}

    public FlowServiceImpl(FlowDao flowDao) {
        this.flowDao = flowDao;
    }

    @Override
    public Flow createNewFlow() {
        return this.flowDao.createNewFlow("chris.jones"); // todo:  change the author parameter
    }

    @Override
    public Flow updateFlow(Flow flow) {
        final Flow newFlowVersion = flow.increment();

        return this.flowDao.updateFlow(newFlowVersion);
    }

    @Override
    public Optional<Flow> getFlowByCanonicalId(String canonicalId) {
        return this.flowDao.getFlowByCanonicalId(canonicalId);
    }

    @Override
    public Set<UUID> getFlowsByUser(String userName) {
        return this.flowDao.getFlowsByUser(userName);
    }

    @Override
    public String generateTransformationStagingPresignedUrl(UUID flowId) {
        return this.flowDao.generateTransformationStagingPresignedUrl(flowId);
    }
}
