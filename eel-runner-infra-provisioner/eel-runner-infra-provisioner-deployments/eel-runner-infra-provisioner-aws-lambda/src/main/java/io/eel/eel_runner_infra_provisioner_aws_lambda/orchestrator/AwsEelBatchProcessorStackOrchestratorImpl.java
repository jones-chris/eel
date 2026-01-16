package io.eel.eel_runner_infra_provisioner_aws_lambda.orchestrator;

import io.eel.common.model.Flow;
import io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AwsEelBatchProcessorStackOrchestratorImpl implements EelBatchProcessorStackOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AwsEelBatchProcessorStackOrchestratorImpl.class);

    private List<FlowComponentStack> flowComponentStacks = new ArrayList<>();

    private int currentFlowComponentStackIndex = 0;

    public AwsEelBatchProcessorStackOrchestratorImpl() {
        // Create AWS service clients that are shared amongst the flow component stacks below.  It would be a waste of
        // memory for each flow component stack to have their own duplicated AWS service client.
        final S3Client s3Client = S3Client.create();
        final SqsClient sqsClient = SqsClient.create();
        final IamClient iamClient = IamClient.builder().build();
        final LambdaClient lambdaClient = LambdaClient.create();
        final SfnClient stepFunctionsClient = SfnClient.create();

        // Create the flow component stacks and their dependent flow component stacks.
        FlowComponentStack landingBucketStack = new AwsS3LandingBucketFlowComponentStackImpl(s3Client);
        FlowComponentStack sqsInputQueueStack = new AwsSqsInputQueueFlowComponentStackImpl(
                sqsClient,
                List.of(landingBucketStack)
        );
        FlowComponentStack sqsDeadLetterQueueStack = new AwsSqsDeadLetterQueueFlowComponentStackImpl(sqsClient);
        FlowComponentStack lambdaFunctionStack = new AwsLambdaFlowComponentStackImpl(
                s3Client,
                iamClient,
                lambdaClient,
                List.of(landingBucketStack, sqsDeadLetterQueueStack, sqsInputQueueStack)
        );
        FlowComponentStack queriesStepFunction = new AwsQueriesStepFunctionFlowComponentStackImpl(
                stepFunctionsClient,
                iamClient,
                List.of(sqsInputQueueStack, landingBucketStack)
        );
        FlowComponentStack schedulerStack = new AwsSchedulerFlowComponentStackImpl(
                iamClient,
                SchedulerClient.create(),
                List.of(queriesStepFunction)
        );

        // Define the order of the flow component stacks' deployment and assign them to the flowComponentStacks field,
        // so that this class' deploy() and rollback() methods will iterate on them and deploy them and, if necessary,
        // roll them back.
        this.flowComponentStacks = List.of(
                landingBucketStack,
                sqsInputQueueStack,
                sqsDeadLetterQueueStack,
                lambdaFunctionStack,
                queriesStepFunction,
                schedulerStack
        );
    }

    /**
     * This constructor is intended to be used if you have custom {@link FlowComponentStack}s for a custom EEL deployment.
     * It is rare that you would need to use this constructor.  It is recommended you use the default constructor
     * and its internal default {@link FlowComponentStack}s to handle the EEL deployment.
     * <p>
     * Note that the flow component stacks will be deployed sequentially in the order that you put them in the {@link List}.  Similarly,
     * they will be rolled back sequentially in the reverse order of the {@link List} starting with the stack that encountered the
     * deployment error.
     *
     * @param flowComponentStacks The {@link FlowComponentStack}s to deploy.
     */
    public AwsEelBatchProcessorStackOrchestratorImpl(List<FlowComponentStack> flowComponentStacks) {
        this.flowComponentStacks = flowComponentStacks;
    }

    @Override
    public void deploy(Flow flow) {
        for (int i = 0; i < this.flowComponentStacks.size() - 1; i++) {
            currentFlowComponentStackIndex = i;

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

    /**
     * Deletes a previously successfully deleted {@link Flow}.
     *
     * @param flow The {@link Flow} to delete.
     */
    @Override
    public void delete(Flow flow) {
        // Set the current stack index to the last stack in the flow component stacks, so that it will delete all resources starting
        // with the last resource and working backwards sequentially.
        this.currentFlowComponentStackIndex = this.flowComponentStacks.size() - 1;
        this.rollback(flow);
    }

    /**
     * Rolls back a {@link Flow} that has already been successfully provisioned/deployed.
     *
     * @param flow The {@link Flow} to roll back.
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
