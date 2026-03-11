package io.eel.engine_deployments_aws_lambda.dao;

import io.eel.common.model.FlowExecution;
import io.eel.common_aws.BaseAwsDynamoDbDao;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class AwsDynamoDbFlowExecutionDaoImpl extends BaseAwsDynamoDbDao<FlowExecution, String> {

    private static final String TABLE_NAME = "eel-flow-executions";

    private static final String PARTITION_KEY = "flowIdAndExecutionId";

    private static final Function<
            Map<String, AttributeValue>,
            FlowExecution
            > DYNAMO_DB_ITEM_MAPPER = (item) ->
            gson.fromJson(
                    item.get("object").s(),
                    FlowExecution.class
            );

    public AwsDynamoDbFlowExecutionDaoImpl(DynamoDbClient dynamoDbClient) {
        super(dynamoDbClient, TABLE_NAME, PARTITION_KEY);
    }

    public Optional<FlowExecution> getById(String flowIdAndExecutionId) {
        return super.getById(flowIdAndExecutionId, DYNAMO_DB_ITEM_MAPPER);
    }

    public FlowExecution save(FlowExecution flowExecution) {
        String flowIdAndExecutionId = getFlowIdAndExecutionId(flowExecution);

        return super.save(
                flowExecution,
                f -> AttributeValue.fromS(flowIdAndExecutionId)
        );
    }

    private static String getFlowIdAndExecutionId(FlowExecution flowExecution) {
        return flowExecution.flowId().toString() + "#" + flowExecution.version() + "#" + flowExecution.executionId();
    }

}
