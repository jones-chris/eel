package io.eel.common_aws;

import io.eel.common.dao.FlowExecutionDao;
import io.eel.common.model.FlowExecution;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class AwsDynamoDbFlowExecutionDaoImpl extends BaseAwsDynamoDbDao<FlowExecution, String> implements FlowExecutionDao {

    private static final String TABLE_NAME = "eel-flow-executions";

    private static final String PARTITION_KEY = "flowId";

    private static final String SORT_KEY = "executionTimeStamp";

    private static final Function<Map<String, AttributeValue>, FlowExecution> DYNAMO_DB_ITEM_MAPPER = (item) ->
            gson.fromJson(
                    item.get("object").s(),
                    FlowExecution.class
            );

    public AwsDynamoDbFlowExecutionDaoImpl(DynamoDbClient dynamoDbClient) {
        super(dynamoDbClient, TABLE_NAME, PARTITION_KEY, SORT_KEY);
    }

    public Optional<FlowExecution> getById(String flowId, long executionTimestamp) {
        AttributeValue executionTimestampAttributeValue = AttributeValue.fromN(String.valueOf(executionTimestamp));
        return super.getOneById(flowId, executionTimestampAttributeValue, DYNAMO_DB_ITEM_MAPPER);
    }

    public FlowExecution save(FlowExecution flowExecution) {
        return super.saveWithSortKey(
                flowExecution,
                f -> AttributeValue.fromS(flowExecution.flowId().toString()),
                f -> AttributeValue.fromN(String.valueOf(flowExecution.executionTimeStamp().toEpochSecond()))
        );
    }

    public List<FlowExecution> getPageByFlowId(String flowId) {
        return super.getPageById(flowId, DYNAMO_DB_ITEM_MAPPER);
    }

}
