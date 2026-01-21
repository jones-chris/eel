package io.eel.eel_runner_infra_provisioner_aws_lambda.orchestrator;

import io.eel.common.model.Flow;
import io.eel.eel_runner_infra_provisioner_aws_lambda.dao.AwsDynamoDbFlowResourcesDaoImpl;
import io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.*;
import io.eel.eel_runner_infra_provisioner_core.stacks.model.FlowResources;
import io.eel.eel_runner_infra_provisioner_core.stacks.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AwsEelBatchProcessorStackOrchestratorImpl implements EelBatchProcessorStackOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AwsEelBatchProcessorStackOrchestratorImpl.class);

    private List<FlowComponentStack> flowComponentStacks = new ArrayList<>();

    private int currentFlowComponentStackIndex = 0;

    private AwsDynamoDbFlowResourcesDaoImpl flowResourcesDao;

    public AwsEelBatchProcessorStackOrchestratorImpl() {
        this(DynamoDbClient.create());
    }

    public AwsEelBatchProcessorStackOrchestratorImpl(DynamoDbClient dynamoDbClient) {
        this.flowResourcesDao = new AwsDynamoDbFlowResourcesDaoImpl(dynamoDbClient);

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
        for (int i = 0; i <= this.flowComponentStacks.size() - 1; i++) {
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

                return;
            }
        }

        // Persist all provisioned resources' key-value pairs to DDB to retrieve when needing to delete the stack.
        Map<ResourceType, String> resources = flowComponentStacks.stream()
                .flatMap(stack -> stack.getProvisionedResources().entrySet().stream())
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        )
                );

        FlowResources flowResources = FlowResources.from(flow, resources);

        this.flowResourcesDao.save(flowResources);
    }

    /**
     * Deletes a previously successfully deleted {@link Flow}.
     *
     * @param flow The {@link Flow} to delete.
     */
    @Override
    public void delete(Flow flow) {
        // Get flow resources.
        FlowResources flowResources = this.flowResourcesDao.getById(flow.getId().toString())
                .orElseThrow(() -> new RuntimeException("Could not find existing flow resource to delete for canonical id " + flow.getCanonicalId()));

        // Hydrate the stacks with their required provisioned resources.  Each stack should be instantiated with the resource
        // keys that it expects to provision.  The values should be null/empty until hydrated.
        for (FlowComponentStack stack : this.flowComponentStacks) {
            for (ResourceType resourceType : stack.getProvisionedResources().keySet()) {
                String resourceId = Optional.ofNullable(flowResources.resources().get(resourceType))
                        .orElseThrow(() -> new RuntimeException("Could not find resource for flow canonical id " + flow.getCanonicalId() + " resource type " + resourceType + " and stack " + stack.getClass().getName()));

                stack.getProvisionedResources().put(resourceType, resourceId);
            }
        }

        // Set the current stack index to the last stack in the flow component stacks, so that it will delete all resources starting
        // with the last resource and working backwards sequentially.
        this.currentFlowComponentStackIndex = this.flowComponentStacks.size();
        this.rollback(flow);

        // todo:  if rollback is successful, then delete flow resources from DDB.
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
