package io.eel.database_query_runner_deployments_aws_lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import io.eel.common.dao.FlowDao;
import io.eel.common.dao.QueryResultCsvDao;
import io.eel.common.dao.SqlDataSourceSecretDao;
import io.eel.common.model.*;
import io.eel.common.model.query_result.QueryResultUtils;
import io.eel.common_aws.AwsDynamoDbFlowDaoImpl;
import io.eel.common_aws.AwsSecretsManagerSqlDataSourceSecretDaoImpl;
import io.eel.common_aws.S3QueryResultCsvDaoImpl;
import io.eel.database_query_runner.DatabaseQueryRunner;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class StreamLambdaHandler implements RequestHandler<Map<String, String>, StorageLocation> {

    private final static Logger log = Logger.getLogger(StreamLambdaHandler.class.getName());

    private final static FlowDao flowDao;

    private final static SqlDataSourceSecretDao sqlDataSourceSecretDao;

    private final static QueryResultCsvDao queryResultCsvDao;

    static {
        queryResultCsvDao = new S3QueryResultCsvDaoImpl(S3Client.create());

        flowDao = new AwsDynamoDbFlowDaoImpl(DynamoDbClient.create());

        sqlDataSourceSecretDao = new AwsSecretsManagerSqlDataSourceSecretDaoImpl(SecretsManagerClient.create());
    }

    @Override
    public StorageLocation handleRequest(Map<String, String> event, Context context) {
        log.info("Event: " + event);
        log.info("Context: " + context);

        try {
            // Get data source configuration
            final UUID flowId = UUID.fromString(event.get("flowId"));
            final int version = Integer.parseInt(event.get("version"));
            final String canonicalId = Flow.Utils.getCanonicalId(flowId, version);

            final String inputSheet = event.get("inputSheet");

            // This should be the flow id.
            // todo: fix this later.
//            final String destinationBucket = event.get("ExecutionId");
            final String destinationBucket = event.get("destinationBucket");
            // This should be the execution id.
            // todo: fix this later.
//            final String destinationKey = event.getId();
            final String destinationKey = UUID.randomUUID() + "/" + inputSheet + ".csv";
            final StorageLocation storageLocation = new StorageLocation(destinationBucket, destinationKey, flowId.toString(), inputSheet);

            log.info("Getting flow with canonical id of " + canonicalId + " and input sheet of " + inputSheet + ".  Will write result to bucket " + storageLocation.bucket() + " and key " + storageLocation.key());

            Flow flow = flowDao.getFlowByIdAndVersion(flowId.toString(), version)
                    .orElseThrow(() -> new RuntimeException("Could not find flow with canonical id of " + canonicalId));

            // Get the flow's configured query for the given input sheet.
            Query query = flow.getScheduledBatchConfiguration().sheetQueries().get(inputSheet);

            // Retrieve the data source secret.
            final String secretId = query.dataSource().getSecretId();
            log.info("Retrieving secret id of " + secretId);
            SqlDataSourceSecret secret = sqlDataSourceSecretDao.getById(secretId);

            // Instantiate database query runner to run the configured query.  This method returns the ResultSet as a CSV byte[].
            final byte[] csvBytes = new DatabaseQueryRunner(
                    secret.databaseDialect(),
                    secret.jdbcUrl(),
                    secret.username(),
                    secret.password(),
                    query.sql()
            ).execute(QueryResultUtils::convertResultSetToCsvBytes);

            // Write the CSV byte[] to S3.
            return queryResultCsvDao.save(csvBytes, storageLocation);
        } catch (Throwable t) {
            log.severe("Encountered error: " + t.getMessage());
            t.printStackTrace();

            throw t;
        }
    }

}
