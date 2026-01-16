package io.eel.eel_runner_infra_provisioner_aws_lambda.orchestrator;

import io.eel.common.model.Flow;
import io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.AwsLambdaFlowComponentStackImpl;
import io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.AwsS3LandingBucketFlowComponentStackImpl;
import io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.AwsSchedulerFlowComponentStackImpl;
import io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.FlowComponentStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.scheduler.SchedulerClient;

import java.util.List;
import java.util.Map;

public class AwsEelBatchProcessorStackOrchestratorImpl implements EelBatchProcessorStackOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AwsEelBatchProcessorStackOrchestratorImpl.class);

    private List<FlowComponentStack> flowComponentStacks = List.of(
            new AwsS3LandingBucketFlowComponentStackImpl(S3Client.create()),
            // todo:  input queue
            // todo:  DLQ
            new AwsLambdaFlowComponentStackImpl(
                    Map.of(), // todo: tags
                    S3Client.create(),
                    IamClient.builder().build(),
                    LambdaClient.create(),
                    "", // todo: landing bucket id
                    "", // todo: DLQ id
                    "" // todo: input queue arn
            ),
            // todo: buildInputQueueLambdaEventSourceMapping
            // todo: this.buildQueryStepFunction(flowId, version, sheetNames);
            new AwsSchedulerFlowComponentStackImpl(
                    IamClient.builder().build(),
                    SchedulerClient.create(),
                    "" // todo: step function arn
            )
    );

    private int currentFlowComponentStackIndex = 0;

    private AwsEelBatchProcessorStackOrchestratorImpl() {}

    public AwsEelBatchProcessorStackOrchestratorImpl(List<FlowComponentStack> flowComponentStacks) {
        this.flowComponentStacks = flowComponentStacks;
    }

    @Override
    public void deploy(Flow flow) {
        for (int i = 0; i < this.flowComponentStacks.size() - 1; i++) {
            currentFlowComponentStackIndex = 0;

            FlowComponentStack flowComponentStack = this.flowComponentStacks.get(i);

            boolean wasSuccessful = flowComponentStack.deploy(flow);
            if (! wasSuccessful) {
                log.error(
                        "Flow component stack {} did not deploy successfully for flow ID {} and version {}",
                        flowComponentStack.getClass().getName(),
                        flow.getId().toString(),
                        flow.getVersion()
                );
                this.rollback(flow);
            }

        }
    }

    @Override
    public void delete(Flow flow) {

    }

    /**
     * Rolls back the stacks that have already been successfully provisioned/deployed.
     */
    private void rollback(Flow flow) {
        for (int i = this.currentFlowComponentStackIndex - 1; i >= 0; i--) {
            FlowComponentStack flowComponentStack = this.flowComponentStacks.get(i);

            boolean wasSuccessful = flowComponentStack.rollback(flow);
            if (! wasSuccessful) {
                log.error("Flow component stack {} did not rollback successfully for flow ID {}", flowComponentStack.getClass().getName(), flow.getCanonicalId());
            }
        }
    }

}
