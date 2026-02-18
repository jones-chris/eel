package io.eel.eel_runner_infra_provisioner_aws_lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.Gson;
import io.eel.common.dao.FlowDao;
import io.eel.common.model.Flow;
import io.eel.common_aws.AwsDynamoDbFlowDaoImpl;
import io.eel.eel_runner_infra_provisioner_aws_lambda.orchestrator.AwsEelBatchProcessorStackOrchestratorImpl;
import io.eel.eel_runner_infra_provisioner_core.stacks.orchestrator.EelBatchProcessorStackOrchestrator;
import io.eel.eel_runner_infra_provisioner_core.stacks.model.FlowInfrastructureActionRequestDto;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.logging.Logger;

import static io.eel.eel_runner_infra_provisioner_core.stacks.model.FlowInfrastructureActionRequestDto.InfrastructureAction.DELETE;
import static io.eel.eel_runner_infra_provisioner_core.stacks.model.FlowInfrastructureActionRequestDto.InfrastructureAction.DEPLOY;

public class StreamLambdaHandler implements RequestHandler<SQSEvent, String> {

    private final static Logger log = Logger.getLogger(StreamLambdaHandler.class.getName());

    private static final Gson gson = new Gson();

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();

    private final FlowDao flowDao = new AwsDynamoDbFlowDaoImpl(this.dynamoDbClient);

    private final EelBatchProcessorStackOrchestrator eelBatchProcessorStackOrchestrator = new AwsEelBatchProcessorStackOrchestratorImpl(this.dynamoDbClient);;

    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {
        log.info("sqsEvent is: " + sqsEvent);
        log.info("context is: " + context);

        sqsEvent.getRecords()
                .forEach(
                        record -> {
                            log.info("record body is: " + record.getBody());

                            FlowInfrastructureActionRequestDto flowInfraActionRequest = gson.fromJson(record.getBody(), FlowInfrastructureActionRequestDto.class);

                            String flowCanonicalId = Flow.Utils.getCanonicalId(flowInfraActionRequest.flowId(), flowInfraActionRequest.version());
                            Flow flow = flowDao.getFlowByCanonicalId(flowCanonicalId).orElseThrow();

                            if (DEPLOY.equals(flowInfraActionRequest.infrastructureAction())) {
                                this.eelBatchProcessorStackOrchestrator.deploy(flow);
                            } else if (DELETE.equals(flowInfraActionRequest.infrastructureAction())) {
                                this.eelBatchProcessorStackOrchestrator.delete(flow);
                            } else {
                                String message = "Did not expect flow infrastructure action of " + flowInfraActionRequest.infrastructureAction();

                                log.severe(message);

                                throw new RuntimeException(message);
                            }
                        }
                );

        return "Success";
    }

}
