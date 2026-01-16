package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import io.eel.common.model.Flow;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import static io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.FlowComponentStack.ResourceType.AWS_SQS_DEAD_LETTER_QUEUE_URL;

public class AwsSqsDeadLetterQueueFlowComponentStackImpl extends FlowComponentStack {

    private SqsClient sqsClient;

    private AwsSqsDeadLetterQueueFlowComponentStackImpl() {}

    public AwsSqsDeadLetterQueueFlowComponentStackImpl(SqsClient sqsClient) {
        super();

        this.sqsClient = sqsClient;

        this.addRollbackAction(RollbackActions.deleteSqsQueue(sqsClient));
    }

    @Override
    public boolean deploy(Flow flow) {
        try {
            final CreateQueueRequest request = CreateQueueRequest.builder()
                    .queueName("dlq-" + flow.getId().toString())
                    .tags(this.tags)
                    .build();

            final String queueUrl = this.sqsClient.createQueue(request)
                    .queueUrl();

            this.provisionedResources.put(AWS_SQS_DEAD_LETTER_QUEUE_URL, queueUrl);

            return true;
        } catch (Throwable t) {
            log.error("", t);
            log.error("Initiating rollback");

            this.rollback(flow);

            return false;
        }
    }

    @Override
    public boolean delete(String flowId, int version) {
        return false;
    }
}
