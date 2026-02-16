package io.eel.eel_runner_infra_provisioner_aws_lambda;

import io.eel.common.dao.FlowDao;
import io.eel.common.model.Flow;
import io.eel.common_aws.AwsDynamoDbFlowDaoImpl;
import io.eel.eel_runner_infra_provisioner_aws_lambda.orchestrator.AwsEelBatchProcessorStackOrchestratorImpl;
import io.eel.eel_runner_infra_provisioner_core.stacks.orchestrator.EelBatchProcessorStackOrchestrator;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.UUID;

public class LocalTest {

    public static void main(String[] args) {

        EelBatchProcessorStackOrchestrator stackOrchestrator = new AwsEelBatchProcessorStackOrchestratorImpl();

        final String flowId = "b6ec0a10-ef89-4c0f-9ce9-4e516b942a18";
        final int version = 0;
        final FlowDao flowDao = new AwsDynamoDbFlowDaoImpl(DynamoDbClient.create());
        Flow flow = flowDao.getFlowByCanonicalId(Flow.Utils.getCanonicalId(UUID.fromString(flowId), version))
                .orElseThrow();

//        stackOrchestrator.deploy(flow);
        stackOrchestrator.delete(flow);

    }

}
