package io.eel.manifest_api_aws_lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.eel.common.model.WorkbookMetadata;
import io.eel.manifest_api_aws_lambda.dao.AwsDynamoDbManifestDaoImpl;
import io.eel.manifest_api_aws_lambda.dao.AwsS3WorkbookDaoImpl;
import io.eel.manifest_generator_core.service.ManifestService;
import io.eel.manifest_generator_core.service.ManifestServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

// https://github.com/aws-samples/serverless-snippets/blob/main/integration-s3-to-lambda/Handler.java
public class S3PutObjectHandler implements RequestHandler<S3Event, String> {

    private static final Logger log = LoggerFactory.getLogger(S3PutObjectHandler.class);

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final ManifestService manifestService;

    static {
        manifestService = new ManifestServiceImpl(
                new AwsS3WorkbookDaoImpl(S3Client.builder().build()),
                new AwsDynamoDbManifestDaoImpl(DynamoDbClient.create())
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

                        log.info("Getting workbook at bucket {} and key {}", bucket, key);

                        // todo: get the workbook metadata somehow.
                        final WorkbookMetadata workbookMetadata = new WorkbookMetadata("me", "myEelTransformation", 0);

                        manifestService.createManifest(bucket, key, workbookMetadata);
                    });

            return "Success";
        } catch (Exception e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

}