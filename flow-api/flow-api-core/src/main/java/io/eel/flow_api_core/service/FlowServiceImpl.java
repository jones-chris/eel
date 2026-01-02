package io.eel.flow_api_core.service;

import io.eel.common.model.Flow;
import io.eel.common.dao.FlowDao;
import io.eel.eel_runner_infra_provisioner_core.stacks.EelBatchProcessorStack;
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

    private EelBatchProcessorStack eelBatchProcessorStack;

    private FlowServiceImpl() {}

    public FlowServiceImpl(
            FlowDao flowDao,
            EelBatchProcessorStack eelBatchProcessorStack
    ) {
        this.flowDao = flowDao;
        this.eelBatchProcessorStack = eelBatchProcessorStack;
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
        final Flow newFlowVersion = flow.increment();
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
    public Flow finalizeFlow(String canonicalId) throws ResourceNotFoundException {
        Optional<Flow> flowOptional = this.getFlowByCanonicalId(canonicalId);

        if (flowOptional.isEmpty()) {
            throw new ResourceNotFoundException(canonicalId);
        } else {
            Flow flow = flowOptional.get();

            // mark isFinalized as true.
            flow.setFinalized(true);
            this.flowDao.updateFlow(flow);

            // deploy infra.
            try {
                Set<String> sheetNames = flow.getScheduledBatchConfiguration().sheetQueries().keySet();
                this.eelBatchProcessorStack.deploy(canonicalId, "", "", 0, sheetNames);

                return flow;
            } catch (EelStackDeploymentException e) {
                // todo: if error, then rollback and mark isFinalized as false.
            }
        }

    }
}
