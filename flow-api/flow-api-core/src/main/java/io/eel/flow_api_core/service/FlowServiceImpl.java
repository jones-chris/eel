package io.eel.flow_api_core.service;

import io.eel.common.model.Flow;
import io.eel.common.dao.FlowDao;
import io.eel.flow_api_core.stacks.EelBatchProcessorStackOrchestrator;
import io.eel.flow_api_core.exception.ImmutableFlowException;
import io.eel.flow_api_core.exception.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class FlowServiceImpl implements FlowService {

    private static final Logger log = LogManager.getLogger(FlowServiceImpl.class);

    private FlowDao flowDao;

    private EelBatchProcessorStackOrchestrator eelBatchProcessorStackOrchestrator;

    private FlowServiceImpl() {}

    public FlowServiceImpl(
            FlowDao flowDao,
            EelBatchProcessorStackOrchestrator eelBatchProcessorStackOrchestrator
    ) {
        this.flowDao = flowDao;
        this.eelBatchProcessorStackOrchestrator = eelBatchProcessorStackOrchestrator;
    }

    @Override
    public Flow createNewFlow() {
        return this.flowDao.createNewFlow("chris.jones"); // todo:  change the author parameter
    }

    @Override
    public Flow updateFlow(String canonicalId, Flow newFlow) throws ImmutableFlowException, ResourceNotFoundException {
        Optional<Flow> originalFlow = this.getFlowByCanonicalId(canonicalId);

        if (originalFlow.isEmpty()) {
            throw new ResourceNotFoundException(canonicalId);
        } else {
            if (originalFlow.get().isFinalized()) {
                String message = "Flow with canonical id of " + canonicalId + " is already finalized";

                log.error(message);

                throw new ImmutableFlowException(canonicalId);
            }

            // Performs a complete overwrite of the existing flow.
            return this.flowDao.updateFlow(newFlow);

            // todo:  Set persisted flow isFinalized to true so it is immutable now.
        }
    }

    @Override
    public Flow incrementFlow(Flow flow) {
        final Flow newFlow = flow.increment();

        return this.flowDao.incrementFlow(newFlow);
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

    @Override
    public Flow finalizeFlow(Flow flow) {
        // Mark isFinalized as true.
        flow.setFinalized(true);
        this.flowDao.updateFlow(flow);

        // Deploy infra.
        try {
            Set<String> sheetNames = flow.getScheduledBatchConfiguration().sheetQueries().keySet();
            this.eelBatchProcessorStackOrchestrator.deploy(flow);

            return flow;
        } catch (Throwable t) {
            // If there is an error, then mark isFinalized as false.  The orchestrator's deploy method already handles
            // rolling back the resources.
            log.error(t);

            flow.setFinalized(false);
            this.flowDao.updateFlow(flow);

            throw t;
        }

    }
}
