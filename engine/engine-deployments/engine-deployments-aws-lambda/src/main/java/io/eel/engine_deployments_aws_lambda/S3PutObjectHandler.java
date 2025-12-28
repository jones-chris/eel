package io.eel.engine_deployments_aws_lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.Gson;
import com.opencsv.CSVReader;
import io.eel.common.dao.QueryResultCsvDao;
import io.eel.common.model.StorageLocation;
import io.eel.common_aws.S3QueryResultCsvDaoImpl;
import io.eel.service.WorkbookCalculationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.InputStreamReader;


// https://github.com/aws-samples/serverless-snippets/blob/main/integration-s3-to-lambda/Handler.java
public class S3PutObjectHandler implements RequestHandler<SQSEvent, String> {

    private static final Logger log = LoggerFactory.getLogger(S3PutObjectHandler.class);

    private static final QueryResultCsvDao queryResultCsvDao;

    private static final Gson gson = new Gson();

    static {
        queryResultCsvDao = new S3QueryResultCsvDaoImpl(S3Client.create());
    }

    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {
        log.info("sqsEvent: {}", sqsEvent);
        log.info("context: {}", context);

        try {
            // Check that every record is from this flow id.
            if (sqsEvent.getRecords().isEmpty()) {
                throw new RuntimeException("Expected at least 1 record, but received SQS message with 0 records");
            }

            WorkbookCalculationEngine engine = new WorkbookCalculationEngine();

            sqsEvent.getRecords().parallelStream()
                    .forEach(record -> {
                        StorageLocation storageLocation = gson.fromJson(record.getBody(), StorageLocation.class);
                        String messageFlowId = storageLocation.flowId();
                        String lambdaFunctionFlowId = context.getFunctionName();

                        if (! messageFlowId.equalsIgnoreCase(lambdaFunctionFlowId)) {
                            throw new RuntimeException("SQS message included a record with a flow id of " + messageFlowId + " but expected a flow id of " + lambdaFunctionFlowId);
                        }

                        queryResultCsvDao.get(storageLocation)
                                .ifPresentOrElse(
                                        inputStream -> {
                                            CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream));
                                            engine.withInput("", csvReader);
                                        },
                                        () -> {
                                            throw new RuntimeException("Could not find object: " + storageLocation);
                                        }
                                );
                    });


//            S3EventNotification.S3EventNotificationRecord record = s3event.getRecords().getFirst();
//
//            String bucket = record.getS3().getBucket().getName();
//            String key = record.getS3().getObject().getKey();
//
//            workbookInputsDao.getWorkbookInputs(bucket, key)
//                    .ifPresentOrElse(
//                            zipInputStream -> {
//                                // todo:  clean this up
//                                try {
//                                    new WorkbookCalculationEngine()
//                                            .withZipFileInputs(zipInputStream)
//                                            .runWorkbook();
//                                } catch (IOException e) {
//                                    throw new RuntimeException(e);
//                                }
//                            },
//                            () -> {
//                                throw new RuntimeException("Could not find object " + bucket + "/" + key);
//                            }
//                    );

            return "Success";
        } catch (Throwable t) {
            log.error("", t);

            throw t;
        }
    }

}