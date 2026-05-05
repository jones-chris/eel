package io.eel.manifest_api_aws_lambda.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.eel.common.WorkbookValidator.Manifest;
import io.eel.common_aws.BaseAwsDynamoDbDao;
import io.eel.manifest_generator_core.dao.ManifestDao;
import org.apache.commons.lang3.NotImplementedException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class AwsDynamoDbManifestDaoImpl extends BaseAwsDynamoDbDao<Manifest, String> implements ManifestDao {

    private static final String TABLE_NAME = "eel-manifests";

    private static final String PARTITION_KEY = "transformationId";

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Function<Map<String, AttributeValue>, Manifest> DYNAMO_DB_ITEM_MAPPER = (item) -> gson.fromJson(item.get(OBJECT_KEY).s(), Manifest.class);

    public AwsDynamoDbManifestDaoImpl(DynamoDbClient dynamoDbClient) {
        super(dynamoDbClient, TABLE_NAME, PARTITION_KEY);
    }

    @Override
    public Optional<Manifest> getManifest(String canonicalId) {
        return super.getOneById(canonicalId, DYNAMO_DB_ITEM_MAPPER);
    }

    @Override
    public Manifest saveManifest(Manifest manifest) {
        super.save(
                manifest,
                m -> AttributeValue.fromS(manifest.flowCanonicalId())
        );

        return manifest;
    }

    @Override
    public boolean deleteManifest(UUID uuid) {
        throw new NotImplementedException();
    }

}
