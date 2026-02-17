package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import io.eel.common.model.Flow;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.logging.Logger;

import static io.eel.eel_runner_infra_provisioner_core.stacks.model.ResourceType.AWS_SQS_DEAD_LETTER_QUEUE_ARN;
import static io.eel.eel_runner_infra_provisioner_core.stacks.model.ResourceType.AWS_SQS_DEAD_LETTER_QUEUE_URL;

public class AwsSqsDeadLetterQueueFlowComponentStackImpl extends FlowComponentStack {

    private static Logger log = Logger.getLogger(AwsSqsDeadLetterQueueFlowComponentStackImpl.class.getName());

    private SqsClient sqsClient;

    private AwsSqsDeadLetterQueueFlowComponentStackImpl() {}

    public AwsSqsDeadLetterQueueFlowComponentStackImpl(SqsClient sqsClient) {
        super();

        this.sqsClient = sqsClient;

        this.addExpectedProvisionedResources(AWS_SQS_DEAD_LETTER_QUEUE_URL, AWS_SQS_DEAD_LETTER_QUEUE_ARN);

        this.addRollbackAction(RollbackActions.deleteDeadLetterSqsQueue(sqsClient));
    }

    @Override
    public boolean deploy(Flow flow) {
        try {
            // Create the queue.
            final CreateQueueRequest request = CreateQueueRequest.builder()
                    .queueName("dlq-" + flow.getId().toString())
                    .tags(this.tags)
                    .build();

            final String queueUrl = this.sqsClient.createQueue(request)
                    .queueUrl();

            this.provisionedResources.put(AWS_SQS_DEAD_LETTER_QUEUE_URL, queueUrl);

            // Get the queue's ARN.
            GetQueueAttributesRequest getQueueAttributesRequest = GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.QUEUE_ARN)
                    .build();

            String deadLetterQueueArn = this.sqsClient.getQueueAttributes(getQueueAttributesRequest)
                    .attributes()
                    .get(QueueAttributeName.QUEUE_ARN);

            this.provisionedResources.put(AWS_SQS_DEAD_LETTER_QUEUE_ARN, deadLetterQueueArn);

            return true;
        } catch (Throwable t) {
            log.severe(t.getMessage());
            log.severe("Initiating rollback");

            this.rollback(flow);

            return false;
        }
    }

    @Override
    public boolean delete(String flowId, int version) {
        return false;
    }
}
