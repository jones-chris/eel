package io.eel.manifest_api_aws_lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.eel.common.model.Flow;
import io.eel.common.model.TransformationExtractionType;
import io.eel.common.model.WorkbookMetadata;
import io.eel.manifest_api_aws_lambda.dao.AwsDynamoDbArtifactDaoImpl;
import io.eel.manifest_api_aws_lambda.dao.AwsDynamoDbManifestDaoImpl;
import io.eel.common_aws.AwsS3WorkbookDaoImpl;
import io.eel.manifest_generator_core.service.ManifestService;
import io.eel.manifest_generator_core.service.ManifestServiceImpl;
import io.eel.manifest_generator_core.service.ArtifactService;
import io.eel.manifest_generator_core.service.ArtifactServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.UUID;

// https://github.com/aws-samples/serverless-snippets/blob/main/integration-s3-to-lambda/Handler.java
public class S3PutObjectHandler implements RequestHandler<S3Event, String> {

    private static final Logger log = LoggerFactory.getLogger(S3PutObjectHandler.class);

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final ManifestService manifestService;

    private static final ArtifactService transformationJarService;

    static {
        final S3Client s3Client = S3Client.create();
        final DynamoDbClient dynamoDbClient = DynamoDbClient.create();

        manifestService = new ManifestServiceImpl(
                new AwsS3WorkbookDaoImpl(s3Client),
                new AwsDynamoDbManifestDaoImpl(dynamoDbClient)
        );

        transformationJarService = new ArtifactServiceImpl(
                s3Client,
                new AwsDynamoDbArtifactDaoImpl(dynamoDbClient)
        );
    }

    @Override
    public String handleRequest(S3Event s3event, Context context) {
        log.info("s3Event: {}", s3event);
        log.info("context: {}", context);

        try {
            s3event.getRecords().parallelStream()
                    .forEach(record -> {
                        String bucket = record.getS3().getBucket().getName();
                        String key = record.getS3().getObject().getUrlDecodedKey();

                        if (key.startsWith(TransformationExtractionType.MANIFEST.getPrefix())) {
                            log.info("Getting workbook at bucket {} and key {} to build manifest", bucket, key);

                            // todo: get the workbook metadata somehow.  Get it from the flow??
                            final WorkbookMetadata workbookMetadata = new WorkbookMetadata("me", "myEelTransformation", 0);

                            manifestService.createManifest(bucket, key, workbookMetadata);
                        } else if (key.startsWith(TransformationExtractionType.ARTIFACT.getPrefix())) {
                            log.info("Getting workbook at bucket {} and key {} to build artifact", bucket, key);

                            String strippedKey = key.replace(TransformationExtractionType.ARTIFACT.getPrefix(), "");

                            String canonicalId = Flow.Utils.getCanonicalId(UUID.fromString(strippedKey), 0);

                            transformationJarService.buildJar(canonicalId);
                        } else {
                            log.warn("Received S3 event for object with key {} which does not match expected prefixes", key);
                            throw new RuntimeException("Unexpected S3 object key prefix");
                        }
                    });

            return "Success";
        } catch (Exception e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

}