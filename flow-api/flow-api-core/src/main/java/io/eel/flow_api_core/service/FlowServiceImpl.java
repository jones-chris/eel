package io.eel.flow_api_core.service;

import io.eel.common.model.Flow;
import io.eel.common.dao.FlowDao;
import io.eel.eel_runner_infra_provisioner_core.stacks.model.FlowInfrastructureActionRequestDto;
import io.eel.flow_api_core.dao.FlowInfrastructureActionQueueDao;
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

    private FlowInfrastructureActionQueueDao flowInfrastructureActionQueueDao;

    private FlowServiceImpl() {}

    public FlowServiceImpl(
            FlowDao flowDao,
            FlowInfrastructureActionQueueDao flowInfrastructureActionQueueDao
    ) {
        this.flowDao = flowDao;
        this.flowInfrastructureActionQueueDao = flowInfrastructureActionQueueDao;
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
        // Mark isFinalized as true, so that it is locked/immutable while the deployment is attempted.
        flow.setFinalized(true);
        this.flowDao.updateFlow(flow);

        // Send message to infra provisioner queue for the infra to be deployed or deleted.
        this.flowInfrastructureActionQueueDao.sendDeploymentMessage(
                FlowInfrastructureActionRequestDto.newDeploymentRequest(flow)
        );

        return flow;
    }
}
