package io.eel.common_aws;

import com.google.gson.Gson;
import io.eel.common.dao.FlowDao;
import io.eel.common.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class AwsDynamoDbFlowDaoImpl extends BaseAwsDynamoDbDao<Flow, String> implements FlowDao {

    private static final String TABLE_NAME = "eel-flows";

    private static final String PARTITION_KEY = "id";

    private static final Gson gson = new Gson();

    private static final int PRESIGNED_URL_EXPIRATION_IN_MINS = 3;

    private String STAGING_BUCKET_NAME;

    private static final Function<Map<String, AttributeValue>, Flow> DYNAMO_DB_ITEM_MAPPER = (item) ->
            gson.fromJson(item.get("object").s(), Flow.class);

    /**
     * A constructor intended to be used if you do NOT intend to call {@link AwsDynamoDbFlowDaoImpl#generateTransformationStagingPresignedUrl(UUID)}.
     * Note that if you do, then that method will throw an error.
     *
     * @param dynamoDbClient {@link DynamoDbClient}
     */
    public AwsDynamoDbFlowDaoImpl(DynamoDbClient dynamoDbClient) {
        super(dynamoDbClient, TABLE_NAME, PARTITION_KEY);
    }

    public AwsDynamoDbFlowDaoImpl(
            DynamoDbClient dynamoDbClient,
            String stagingBucketName
    ) {
        super(dynamoDbClient, TABLE_NAME, PARTITION_KEY);
        STAGING_BUCKET_NAME = stagingBucketName;
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
     * This is a thin wrapper around {@link BaseAwsDynamoDbDao#getById(Object, Function)}
     * @param canonicalId {@link String}
     * @return {@link Optional<Flow>}
     */
    @Override
    public Optional<Flow> getFlowByCanonicalId(String canonicalId) {
        return super.getById(canonicalId, DYNAMO_DB_ITEM_MAPPER);
    }

    @Override
    public Flow updateFlow(Flow flow) {
        return super.save(
                flow,
                f -> AttributeValue.fromS(f.getCanonicalId())
        );
    }

    @Override
    public Flow createNewFlow(String author) {
        final Flow flow = Flow.create(author);

        return super.save(
                flow,
                f -> AttributeValue.fromS(f.getCanonicalId())
        );
    }

    @Override
    public Flow incrementFlow(Flow flow) {
        return super.save(
                flow,
                f -> AttributeValue.fromS(f.getCanonicalId())
        );
    }

    @Override
    public String generateTransformationStagingPresignedUrl(UUID flowId) {
        try (S3Presigner preSigner = S3Presigner.create()) {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(STAGING_BUCKET_NAME)
                    .key(flowId.toString()) // todo:  make this the canonical id instead??
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(PRESIGNED_URL_EXPIRATION_IN_MINS))
                    .putObjectRequest(objectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = preSigner.presignPutObject(presignRequest);

            String url = presignedRequest.url().toString();
            System.out.println("Presigned URL to upload a file to: " + url);
            System.out.println("HTTP method: " + presignedRequest.httpRequest().method());
            System.out.println("Presigned external URL to a upload file to: " + presignedRequest.url().toExternalForm());

            return url;
        }
    }

}
