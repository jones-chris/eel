package io.eel.flow_api_core.service;

import io.eel.common.model.Flow;
import io.eel.common.dao.FlowDao;
import io.eel.common.model.TransformationExtractionType;
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
    public Flow updateFlow(Flow newFlow) throws ImmutableFlowException, ResourceNotFoundException {
        String canonicalId = newFlow.getCanonicalId();

        log.info("Attempting to update flow with canonical id of " + canonicalId);
        Optional<Flow> originalFlow = this.getFlowByIdAndVersion(newFlow.getId().toString(), newFlow.getVersion());

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
    public Optional<Flow> getFlowByIdAndVersion(String flowId, int version) {
        return this.flowDao.getFlowByIdAndVersion(flowId, version);
    }

    @Override
    public Set<UUID> getFlowsByUser(String userName) {
        return this.flowDao.getFlowsByUser(userName);
    }

    @Override
    public Set<Integer> getFlowVersionsByFlowId(UUID flowId) {
        return this.flowDao.getFlowVersions(flowId);
    }

    @Override
    public String generateTransformationStagingPresignedUrl(UUID flowId, TransformationExtractionType extractionType) {
        return this.flowDao.generateTransformationStagingPresignedUrl(flowId, extractionType);
    }

    @Override
    public Flow finalizeFlow(Flow flow) {
        // Mark isFinalized as true, so that it is locked/immutable while the deployment is attempted.
        flow.setFinalized(true);
        this.flowDao.updateFlow(flow);

        // Send message to infra provisioner queue for the infra to be deployed.
        this.flowInfrastructureActionQueueDao.sendDeploymentMessage(
                FlowInfrastructureActionRequestDto.newDeploymentRequest(flow)
        );

        return flow;
    }

    @Override
    public Flow unfinalizeFlow(Flow flow) {
        // Mark isFinalized as false, so that it can be re-deployed in the future, if desired.
        flow.setFinalized(false);
        this.flowDao.updateFlow(flow);

        // Send a message to infra provisioner queue for the infra to be deleted.
        this.flowInfrastructureActionQueueDao.sendDeleteMessage(
                FlowInfrastructureActionRequestDto.newDeletionRequest(flow)
        );

        return flow;
    }
}
