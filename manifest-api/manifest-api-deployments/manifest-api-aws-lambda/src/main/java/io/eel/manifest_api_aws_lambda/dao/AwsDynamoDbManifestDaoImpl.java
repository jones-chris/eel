package io.eel.manifest_api_aws_lambda.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.eel.common.WorkbookValidator;
import io.eel.manifest_generator_core.dao.ManifestDao;
import org.apache.commons.lang3.NotImplementedException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Map;
import java.util.UUID;

public class AwsDynamoDbManifestDaoImpl implements ManifestDao {

    private static final String TABLE_NAME = "eel-manifests";

    private final DynamoDbClient dynamoDbClient;

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public AwsDynamoDbManifestDaoImpl(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public WorkbookValidator.Manifest getManifest(UUID uuid) {
        throw new NotImplementedException();
    }

    @Override
    public void saveManifest(WorkbookValidator.Manifest manifest) {
        final String manifestJsonString = gson.toJson(manifest);

        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(
                        Map.of(
                            "transformationId", AttributeValue.fromS(manifest.id().toString()),
                            "manifest", AttributeValue.fromS(manifestJsonString)
                        )
                ).build();

        this.dynamoDbClient.putItem(putItemRequest);
    }

    @Override
    public boolean deleteManifest(UUID uuid) {
        throw new NotImplementedException();
    }

}
