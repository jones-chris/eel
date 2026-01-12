package io.eel.eel_runner_infra_provisioner_aws_lambda.orchestrator;

import io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.FlowComponentStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AwsEelBatchProcessorStackOrchestratorImpl implements EelBatchProcessorStackOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AwsEelBatchProcessorStackOrchestratorImpl.class);

    private List<FlowComponentStack> flowComponentStacks = new ArrayList<>();

    private int currentFlowComponentStackIndex = 0;

    private AwsEelBatchProcessorStackOrchestratorImpl() {}

    public AwsEelBatchProcessorStackOrchestratorImpl(List<FlowComponentStack> flowComponentStacks) {
        this.flowComponentStacks = flowComponentStacks;
    }

    @Override
    public void deploy(String flowId, int version) {
        for (int i = 0; i < this.flowComponentStacks.size() - 1; i++) {
            currentFlowComponentStackIndex = 0;

            FlowComponentStack flowComponentStack = this.flowComponentStacks.get(i);

            boolean wasSuccessful = flowComponentStack.deploy();
            if (! wasSuccessful) {
                log.error("Flow component stack {} did not deploy successfully for flow ID {} and version {}",
                        flowComponentStack.getClass().getName(),
                        flowComponentStack.getFlowId(),
                        flowComponentStack.getFlowVersion()
                );
                this.rollback();
            }

        }
    }

    @Override
    public void delete(String flowId, int version) {

    }

    /**
     * Rolls back the stacks that have already been successfully provisioned/deployed.
     */
    private void rollback() {
        for (int i = this.currentFlowComponentStackIndex - 1; i >= 0; i--) {
            FlowComponentStack flowComponentStack = this.flowComponentStacks.get(i);

            boolean wasSuccessful = flowComponentStack.rollback();
            if (! wasSuccessful) {
                log.error("Flow component stack {} did not rollback successfully for flow ID {}", flowComponentStack.getClass().getName(), flowComponentStack.getFlowId());
            }
        }
    }

}
