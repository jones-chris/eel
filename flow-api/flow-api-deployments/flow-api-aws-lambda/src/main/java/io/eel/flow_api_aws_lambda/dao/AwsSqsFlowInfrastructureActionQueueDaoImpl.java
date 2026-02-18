package io.eel.flow_api_aws_lambda.dao;

import com.google.gson.Gson;
import io.eel.eel_runner_infra_provisioner_core.stacks.model.FlowInfrastructureActionRequestDto;
import io.eel.flow_api_core.dao.FlowInfrastructureActionQueueDao;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class AwsSqsFlowInfrastructureActionQueueDaoImpl implements FlowInfrastructureActionQueueDao {

    private static final Gson gson = new Gson();

    private String queueUrl;

    private SqsClient sqsClient;

    private AwsSqsFlowInfrastructureActionQueueDaoImpl() {}

    public AwsSqsFlowInfrastructureActionQueueDaoImpl(final String queueUrl) {
        this(queueUrl, SqsClient.create());
    }

    public AwsSqsFlowInfrastructureActionQueueDaoImpl(final String queueUrl, SqsClient sqsClient) {
        this.queueUrl = queueUrl;
        this.sqsClient = sqsClient;
    }

    @Override
    public void sendDeploymentMessage(FlowInfrastructureActionRequestDto flowInfrastructureActionRequestDto) {
        this.sendMessage(flowInfrastructureActionRequestDto);
    }

    @Override
    public void sendDeleteMessage(FlowInfrastructureActionRequestDto flowInfrastructureActionRequestDto) {
        this.sendMessage(flowInfrastructureActionRequestDto);
    }

    /**
     * This method consolidates the logic to send a message to the SQS queue, because the AWS EEL architecture only uses
     * one SQS queue for both deploying and deleting resources.  Whereas other cloud providers may require two queues.
     *
     * @param flowInfrastructureActionRequestDto {@link FlowInfrastructureActionRequestDto}
     */
    private void sendMessage(FlowInfrastructureActionRequestDto flowInfrastructureActionRequestDto) {
        this.sqsClient.sendMessage(
                SendMessageRequest.builder()
                        .queueUrl(this.queueUrl)
                        .messageBody(
                                gson.toJson(flowInfrastructureActionRequestDto)
                        )
                        .build()
        );
    }

}
