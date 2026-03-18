package io.eel.engine_deployments_aws_lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.opencsv.CSVReader;
import io.eel.common.dao.QueryResultCsvDao;
import io.eel.common.dao.WorkbookDao;
import io.eel.common.model.FlowExecution;
import io.eel.common.model.StorageLocation;
import io.eel.common_aws.AwsS3WorkbookDaoImpl;
import io.eel.common_aws.S3QueryResultCsvDaoImpl;
import io.eel.common_aws.AwsDynamoDbFlowExecutionDaoImpl;
import io.eel.service.WorkbookCalculationEngine;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;


// https://github.com/aws-samples/serverless-snippets/blob/main/integration-s3-to-lambda/Handler.java
public class S3PutObjectHandler implements RequestHandler<SQSEvent, String> {

    private static final Logger log = Logger.getLogger(S3PutObjectHandler.class.getName());

    private static final Type storageLocationListType = new TypeToken<List<StorageLocation>>(){}.getType();

    private static final String flowExecutionBucketName;

    private static final QueryResultCsvDao queryResultCsvDao;

    private static final WorkbookDao workbookDao;

    private static final AwsDynamoDbFlowExecutionDaoImpl flowExecutionDao;

    private static final Gson gson = new Gson();

    static {
        flowExecutionBucketName = Optional.ofNullable(System.getenv("FLOW_EXECUTION_BUCKET_NAME"))
                .orElseThrow(() -> new RuntimeException("Could not find FLOW_EXECUTION_BUCKET_NAME env variable"));

        S3Client s3Client = S3Client.create();
        queryResultCsvDao = new S3QueryResultCsvDaoImpl(s3Client);
        workbookDao = new AwsS3WorkbookDaoImpl(s3Client);

        flowExecutionDao = new AwsDynamoDbFlowExecutionDaoImpl(DynamoDbClient.create());
    }

    @Override
    public String handleRequest(SQSEvent sqsEvent, Context context) {
        log.info("sqsEvent: " + sqsEvent);
        log.info("context: " + context);

        FlowExecution flowExecution = null;

        try {
            if (sqsEvent.getRecords().isEmpty()) {
                throw new RuntimeException("Expected at least 1 record, but received SQS message with 0 records");
            }

            String lambdaFunctionFlowId = context.getFunctionName();
            OffsetDateTime executionTimeStamp = OffsetDateTime.now(ZoneId.of("UTC"));
            String key = this.buildS3FlowExecutionKey(lambdaFunctionFlowId, executionTimeStamp.toEpochSecond());
            flowExecution = new FlowExecution(
                    UUID.fromString(lambdaFunctionFlowId),
                    executionTimeStamp,
                    flowExecutionBucketName,
                    key,
                    FlowExecution.FlowExecutionStatus.RUNNING
            );
            flowExecutionDao.save(flowExecution);

            log.info("Running flow " + lambdaFunctionFlowId + " and execution " + executionTimeStamp.toEpochSecond());
            WorkbookCalculationEngine engine = new WorkbookCalculationEngine();

            sqsEvent.getRecords()
                    .forEach(record -> {
                        log.info("record body is: " + record.getBody());

                        List<StorageLocation> storageLocations = gson.fromJson(record.getBody(), storageLocationListType);

                        log.info("storageLocations is: " + gson.toJson(storageLocations));

                        // Check that every record is from this flow id.
                        for (StorageLocation storageLocation : storageLocations) {
                            log.info("storageLocation is: " + gson.toJson(storageLocation));

                            String messageFlowId = storageLocation.flowId();

                            if (! messageFlowId.equalsIgnoreCase(lambdaFunctionFlowId)) {
                                throw new RuntimeException("SQS message included a record with a flow id of " + messageFlowId + " but expected a flow id of " + lambdaFunctionFlowId);
                            }

                            queryResultCsvDao.get(storageLocation)
                                    .ifPresentOrElse(
                                            inputStream -> {
                                                CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream));
                                                engine.withInput(storageLocation.inputSheetName(), csvReader);
                                            },
                                            () -> {
                                                throw new RuntimeException("Could not find object: " + storageLocation);
                                            }
                                    );
                        }
                    });

            log.info("Running workbook"); // todo:  make this a debug statement.
            engine.runWorkbook();

            // Write workbook to S3 for debugging.
            log.info("Saving workbook");
            workbookDao.save(engine.getWorkbookProxy(), flowExecutionBucketName, key);

            // Write execution metadata and location of workbook to dynamo DB.
            log.info("Saving flow execution metadata");
            flowExecutionDao.save(FlowExecution.completedFlowExecution(flowExecution));

            return "Success";
        } catch (Throwable t) {
            log.severe(t.getMessage());

            t.printStackTrace();

            if (flowExecution != null) {
                flowExecutionDao.save(FlowExecution.failedFlowExecution(flowExecution));
            }

            throw new RuntimeException(t);
        }
    }

    private String buildS3FlowExecutionKey(String lambdaFunctionFlowId, long executionTimeStampEpochSeconds) {
        return "/" + lambdaFunctionFlowId + "/" + executionTimeStampEpochSeconds + ".xlsx";
    }

}