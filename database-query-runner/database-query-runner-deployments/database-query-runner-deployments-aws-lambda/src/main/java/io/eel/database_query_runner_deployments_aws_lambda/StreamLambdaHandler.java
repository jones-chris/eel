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

import java.util.UUID;
import java.util.logging.Logger;

public class StreamLambdaHandler implements RequestHandler<ScheduledEvent, Boolean> {

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
    public Boolean handleRequest(ScheduledEvent event, Context context) {
        log.info("Event: " + event);
        log.info("Context: " + context);

        try {
            // Get data source configuration
            log.info("" + event.getDetail());
            final UUID id = UUID.fromString(event.getDetail().get("flowId").toString());
            final Integer version = (Integer) event.getDetail().get("version");
            final String canonicalId = Flow.Utils.getCanonicalId(id, version);

            final String inputSheet = event.getDetail().get("inputSheet").toString();

            // This should be the flow id.
            final String destinationBucket = event.getDetail().get("destinationBucket").toString();
            // This should be the execution id.
            final String destinationKey = event.getId();

            log.info("Getting flow with canonical id of " + canonicalId + " and input sheet of " + inputSheet + ".  Will write result to bucket " + destinationBucket + " and key " + destinationKey);

            Flow flow = flowDao.getFlowByCanonicalId(canonicalId)
                    .orElseThrow(() -> new RuntimeException("Could not find flow with canonical id of " + canonicalId));

            log.info("Successfully retrieved flow: " + flow);
            // Get the flow's configured query for the given input sheet.
            Query query = flow.getScheduledBatchConfiguration().sheetQueries().get(inputSheet);

            // Retrieve the data source secret.
            final String secretId = query.sqlDatabaseDataSource().getSecretId();
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
            queryResultCsvDao.save(csvBytes, destinationBucket, destinationKey);

            return true;
        } catch (Throwable t) {
            log.severe("Encountered error: " + t.getMessage());
            t.printStackTrace();

            return false;
        }
    }

}
