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
import java.util.stream.Collectors;

public class AwsDynamoDbFlowDaoImpl extends BaseAwsDynamoDbDao<Flow, String> implements FlowDao {

    private static final String TABLE_NAME = "eel-flows2"; // todo:  change this later when we know this new table schema works.

    private static final String USERNAME_INDEX = "userNameIndex";

    private static final String PARTITION_KEY = "id";

    private static final String SORT_KEY = "version";

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
        super(dynamoDbClient, TABLE_NAME, PARTITION_KEY, SORT_KEY);
    }

    public AwsDynamoDbFlowDaoImpl(
            DynamoDbClient dynamoDbClient,
            String stagingBucketName
    ) {
        super(dynamoDbClient, TABLE_NAME, PARTITION_KEY, SORT_KEY);
        STAGING_BUCKET_NAME = stagingBucketName;
    }

    /**
     * Retrieves all flows IDs for a given username/author.
     * @param userName {@link String}
     * @return {@link Set<UUID>}
     */
    @Override
    public Set<UUID> getFlowsByUser(String userName) {
        return this.getPageById(userName, USERNAME_INDEX, DYNAMO_DB_ITEM_MAPPER)
                .stream()
                .map(Flow::getId)
                .collect(Collectors.toSet());
    }

    /**
     * This is a thin wrapper around {@link BaseAwsDynamoDbDao#getOneById(Object, AttributeValue, Function)}}
     * @param flowId {@link String}
     * @param version {@link int}
     * @return {@link Optional<Flow>}
     */
    @Override
    public Optional<Flow> getFlowByIdAndVersion(String flowId, int version) {
        return super.getOneById(
                flowId,
                AttributeValue.fromN(Integer.toString(version)),
                DYNAMO_DB_ITEM_MAPPER
        );
    }

    @Override
    public Set<Integer> getFlowVersions(UUID flowId) {
        return super.getPageById(flowId.toString(), DYNAMO_DB_ITEM_MAPPER)
                .stream()
                .map(Flow::getVersion)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public Flow updateFlow(Flow flow) {
        return super.save(
                flow,
                f -> AttributeValue.fromS(f.getId().toString()),
                f -> Map.of(
                        SORT_KEY, AttributeValue.fromN(Integer.toString(f.getVersion())),
                        OBJECT_KEY, AttributeValue.fromS(gson.toJson(f)),
                        "userName", AttributeValue.fromS(f.getAuthor())
                )
        );
    }

    @Override
    public Flow createNewFlow(String author) {
        final Flow flow = Flow.create(author);

        return super.save(
                flow,
                f -> AttributeValue.fromS(f.getId().toString()),
                f -> Map.of(
                        SORT_KEY, AttributeValue.fromN(Integer.toString(f.getVersion())),
                        OBJECT_KEY, AttributeValue.fromS(gson.toJson(f)),
                        "userName", AttributeValue.fromS(f.getAuthor())
                )
        );
    }

    @Override
    public Flow incrementFlow(Flow flow) {
        return super.save(
                flow,
                f -> AttributeValue.fromS(f.getId().toString()),
                f -> Map.of(
                        SORT_KEY, AttributeValue.fromN(Integer.toString(f.getVersion())),
                        OBJECT_KEY, AttributeValue.fromS(gson.toJson(f)),
                        "userName", AttributeValue.fromS(f.getAuthor())
                )
        );
    }

    @Override
    public String generateTransformationStagingPresignedUrl(UUID flowId, TransformationExtractionType extractionType) {
        try (S3Presigner preSigner = S3Presigner.create()) {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(STAGING_BUCKET_NAME)
                    .key(extractionType.getPrefix() + flowId.toString())
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
