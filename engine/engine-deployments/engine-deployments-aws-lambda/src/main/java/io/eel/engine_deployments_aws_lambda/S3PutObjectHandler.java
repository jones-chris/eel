package io.eel.engine_deployments_aws_lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import io.eel.engine_deployments_aws_lambda.dao.S3WorkbookInputsDaoImpl;
import io.eel.engine_deployments_aws_lambda.dao.WorkbookInputsDao;
import io.eel.service.WorkbookCalculationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;


// https://github.com/aws-samples/serverless-snippets/blob/main/integration-s3-to-lambda/Handler.java
public class S3PutObjectHandler implements RequestHandler<S3Event, String> {

    private static final Logger log = LoggerFactory.getLogger(S3PutObjectHandler.class);

    private static final WorkbookInputsDao workbookInputsDao;

    static {
        workbookInputsDao = new S3WorkbookInputsDaoImpl(S3Client.create());
    }

    @Override
    public String handleRequest(S3Event s3event, Context context) {
        log.info("s3Event: {}", s3event);
        log.info("context: {}", context);

        try {
            // todo:  only 1 record is expected.  Make sure the batch configuration is set up to process only 1 record too.
            if (s3event.getRecords().size() != 1) {
                throw new RuntimeException("Expected only 1 record, but received " + s3event.getRecords().size());
            }

            S3EventNotification.S3EventNotificationRecord record = s3event.getRecords().getFirst();

            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getUrlDecodedKey();

            workbookInputsDao.getWorkbookInputs(bucket, key)
                    .ifPresentOrElse(
                            zipInputStream -> {
                                // todo:  clean this up
                                try {
                                    new WorkbookCalculationEngine()
                                            .withZipFileInputs(zipInputStream)
                                            .runWorkbook();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            () -> {
                                throw new RuntimeException("Could not find object " + bucket + "/" + key);
                            }
                    );

            return "Success";
        } catch (Throwable t) {
            log.error("", t);

            throw t;
        }
    }

}