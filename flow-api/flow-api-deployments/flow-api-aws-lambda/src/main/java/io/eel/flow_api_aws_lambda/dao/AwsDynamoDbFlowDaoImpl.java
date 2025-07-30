package io.eel.flow_api_aws_lambda.dao;

import io.eel.common.model.Flow;
import io.eel.common_aws.BaseAwsDynamoDbDao;
import io.eel.flow_api_core.dao.FlowDao;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AwsDynamoDbFlowDaoImpl extends BaseAwsDynamoDbDao<Flow, UUID> implements FlowDao {

    private static final String TABLE_NAME = "eel-flows";

    private static final String PARTITION_KEY = "flowId";

    public AwsDynamoDbFlowDaoImpl(DynamoDbClient dynamoDbClient) {
        super(dynamoDbClient, TABLE_NAME, PARTITION_KEY);
    }

    /**
     * This is a custom DAO method to retrieve all flows IDs for a given username.
     * @param userName {@link String}
     * @return {@link Set<UUID>}
     */
    @Override
    public Set<UUID> getFlowsByUser(String userName) {
        return Set.of(); // todo:  add logic for this custom method.
    }

    /**
     * This is a thin wrapper around {@link BaseAwsDynamoDbDao#getById(Object)}
     * @param id {@link UUID}
     * @return {@link Optional<Flow>}
     */
    @Override
    public Optional<Flow> getFlowById(UUID id) {
        return super.getById(id);
    }
}
