package io.eel.eel_runner_infra_provisioner_aws_lambda.dao;

import io.eel.common_aws.BaseAwsDynamoDbDao;
import io.eel.eel_runner_infra_provisioner_core.stacks.model.FlowResources;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class AwsDynamoDbFlowResourcesDaoImpl extends BaseAwsDynamoDbDao<FlowResources, String> {

    private static final String TABLE_NAME = "eel-flow-resources";

    private static final String PARTITION_KEY = "id";

    private static final Function<
            Map<String, AttributeValue>,
            FlowResources
    > DYNAMO_DB_ITEM_MAPPER = (item) ->
            gson.fromJson(
                    item.get("object").s(),
                    FlowResources.class
            );

    public AwsDynamoDbFlowResourcesDaoImpl(DynamoDbClient dynamoDbClient) {
        super(dynamoDbClient, TABLE_NAME, PARTITION_KEY);
    }

    public Optional<FlowResources> getById(String flowId) {
        return super.getById(flowId, DYNAMO_DB_ITEM_MAPPER);
    }

    public FlowResources save(FlowResources flowResources) {
        return super.save(
                flowResources,
                f -> AttributeValue.fromS(flowResources.flowId())
        );
    }
}
