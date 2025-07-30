package io.eel.common_aws;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class BaseAwsDynamoDbDao<T, U> {

    public static final String OBJECT_KEY = "object";

    private String tableName;

    private String partitionKey;

    private DynamoDbClient dynamoDbClient;

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private BaseAwsDynamoDbDao() {}

    public BaseAwsDynamoDbDao(
            DynamoDbClient dynamoDbClient,
            String tableName,
            String partitionKey
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.partitionKey = partitionKey;
    }

    public Optional<T> getById(U id) {
        // todo: fix this later.
        return Optional.empty();
    }

    public T save(
        T obj,
        Function<T, AttributeValue> partitionKeyMapper
    ) {
        final String objJson = gson.toJson(obj);

        Map<String, AttributeValue> itemMap = Map.of(
                this.partitionKey, partitionKeyMapper.apply(obj),
                OBJECT_KEY, AttributeValue.fromS(objJson) // todo:  make "object" a constant.
        );

        this.save(itemMap);

        return obj;
    }

    public T save(
            T obj,
            Function<T, AttributeValue> partitionKeyMapper,
            Function<T, Map<String, AttributeValue>> itemMapper
    ) {
        Map<String, AttributeValue> itemMap = itemMapper.apply(obj);

        AttributeValue partitionKeyValue = partitionKeyMapper.apply(obj);
        itemMap.put(this.partitionKey, partitionKeyValue);

        this.save(itemMap);

        return obj;
    }

    public boolean deleteManifest(U id) {
        // todo:  fix this later.
        return true;
    }

    private void save(Map<String, AttributeValue> itemMap) {
        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(this.tableName)
                .item(itemMap)
                .build();

        this.dynamoDbClient.putItem(putItemRequest);
    }

}
