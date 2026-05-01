package io.eel.manifest_api_aws_lambda.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.eel.common.WorkbookValidator;
import io.eel.common_aws.BaseAwsDynamoDbDao;
import io.eel.manifest_generator_core.dao.ArtifactDao;
import io.eel.manifest_generator_core.model.ArtifactBuild;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class AwsDynamoDbArtifactDaoImpl extends BaseAwsDynamoDbDao<ArtifactBuild, String> implements ArtifactDao {

    private static final String TABLE_NAME = "eel-artifacts";

    private static final String PARTITION_KEY = "canonicalId";

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Function<Map<String, AttributeValue>, ArtifactBuild> DYNAMO_DB_ITEM_MAPPER = (item) -> gson.fromJson(item.get(OBJECT_KEY).s(), ArtifactBuild.class);

    public AwsDynamoDbArtifactDaoImpl(DynamoDbClient dynamoDbClient) {
        super(dynamoDbClient, TABLE_NAME, PARTITION_KEY);
    }

    @Override
    public Optional<ArtifactBuild> getArtifactBuild(String flowCanonicalId) {
        return super.getOneById(flowCanonicalId, DYNAMO_DB_ITEM_MAPPER);
    }

    @Override
    public void saveArtifactBuild(ArtifactBuild artifactBuild) {
        super.save(
                artifactBuild,
                ab -> AttributeValue.fromS(artifactBuild.flowCanonicalId())
        );
    }
}
