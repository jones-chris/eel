package io.eel.eel_runner_infra_provisioner_aws_lambda.stacks;

import io.eel.common.model.Flow;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.Constants.ENGINE_TIMEOUT_IN_SECONDS;
import static io.eel.eel_runner_infra_provisioner_core.stacks.model.ResourceType.*;
import static java.util.Map.of;

public class AwsSqsInputQueueFlowComponentStackImpl extends FlowComponentStack {

    private static final Logger log = Logger.getLogger(AwsSqsInputQueueFlowComponentStackImpl.class.getName());

    private SqsClient sqsClient;

    private AwsSqsInputQueueFlowComponentStackImpl() {}

    public AwsSqsInputQueueFlowComponentStackImpl(
            SqsClient sqsClient,
            List<FlowComponentStack> dependentStacks
    ) {
        super(dependentStacks);

        this.sqsClient = sqsClient;

        this.addExpectedProvisionedResources(AWS_SQS_INPUT_QUEUE_URL, AWS_SQS_INPUT_QUEUE_ARN);

        super.addRollbackAction(RollbackActions.deleteInputSqsQueue(sqsClient));
    }

    @Override
    public boolean deploy(Flow flow) {
        try {
            // Create the queue.
            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                    .queueName("eel-input-" + flow.getId().toString())
                    .attributes(
                            Map.of(
                                    // The queue's visibility timeout must match the Engine Lambda Function's timeout.
                                    QueueAttributeName.VISIBILITY_TIMEOUT, String.valueOf(ENGINE_TIMEOUT_IN_SECONDS)
                            )
                    ).tags(this.tags)
                    .build();

            final String queueUrl = this.sqsClient
                    .createQueue(createQueueRequest)
                    .queueUrl();

            this.provisionedResources.put(AWS_SQS_INPUT_QUEUE_URL, queueUrl);

            // Get the queue's ARN so we can use it in the queue policy below.
            GetQueueAttributesRequest getQueueAttributesRequest = GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.QUEUE_ARN)
                    .build();

            String inputQueueArn = this.sqsClient.getQueueAttributes(getQueueAttributesRequest)
                    .attributes()
                    .get(QueueAttributeName.QUEUE_ARN);

            this.provisionedResources.put(AWS_SQS_INPUT_QUEUE_ARN, inputQueueArn);

            // Add a policy to the queue.  The policy grants S3 permission to send messages to the queue.
            SetQueueAttributesRequest setAttributesRequest = SetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl) // SQS requires the Queue URL
                    .attributesWithStrings(
                            // Use the POLICY attribute name to pass the JSON string
                            of(QueueAttributeName.POLICY.toString(), this.buildSqsQueuePolicy(inputQueueArn))
                    ).build();

            // 3. Execute the request
            sqsClient.setQueueAttributes(setAttributesRequest);

            log.info("Successfully created queue and set SQS policy for S3 notifications on queue: " + queueUrl);

            return true;
        } catch (Throwable t) {
            log.severe(t.getMessage());
            log.severe("Initiating rollback");

            this.rollback(flow);

            return false;
        }
    }

    private String buildSqsQueuePolicy(String inputQueueArn) {
        return String.format("""
                {
                  "Version": "2012-10-17",
                  "Id": "SQS-Policy-For-S3-Notification",
                  "Statement": [
                    {
                      "Sid": "AllowS3ToSendMessage",
                      "Effect": "Allow",
                      "Principal": {
                        "Service": "s3.amazonaws.com"
                      },
                      "Action": "sqs:SendMessage",
                      "Resource": "%s",
                      "Condition": {
                        "ArnEquals": {
                          "aws:SourceArn": "arn:aws:s3:::%s"
                        }
                      }
                    }
                  ]
                }
                """,
                inputQueueArn,
                this.getDependentResource(AWS_S3_BUCKET_NAME)
        );
    }
}
