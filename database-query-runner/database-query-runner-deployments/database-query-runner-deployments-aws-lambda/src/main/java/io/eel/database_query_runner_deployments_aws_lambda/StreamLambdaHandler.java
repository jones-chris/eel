package io.eel.database_query_runner_deployments_aws_lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
//import io.eel.common.dao.FlowDao;
//import io.eel.common.model.*;
//import io.eel.common_aws.AwsDynamoDbFlowDaoImpl;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.UUID;
import java.util.logging.Logger;

public class StreamLambdaHandler implements RequestHandler<ScheduledEvent, Boolean> {

    private final static Logger log = Logger.getLogger(StreamLambdaHandler.class.getName());

    private final static Gson gson = new GsonBuilder().create();

//    private final static FlowDao flowDao;

//    static {
//        flowDao = new AwsDynamoDbFlowDaoImpl(
//                DynamoDbClient.create()
//        );
//    }

    @Override
    public Boolean handleRequest(ScheduledEvent event, Context context) {
        return true;
//        log.info("Event: " + event);
//        log.info("Context: " + context);
//
//        try {
//            // Get data source configuration
//            final UUID id = UUID.fromString(event.getDetail().get("id").toString());
//            final Integer version = (Integer) event.getDetail().get("version");
//            final String canonicalId = Flow.Utils.getCanonicalId(id, version);
//
//            final String inputSheet = event.getDetail().get("inputSheet").toString();
//
//            Flow flow = flowDao.getFlowByCanonicalId(canonicalId)
//                    .orElseThrow(() -> new RuntimeException("Could not find flow with canonical id of " + canonicalId));
//
//            Query query = flow.getScheduledBatchConfiguration().sheetQueries().get(inputSheet);
//
//            // todo: retrieve and extract secret.
//
//            // Instantiate database query runner and run query and pass in result set handler.
////            new DatabaseQueryRunner(
////                    DatabaseDialect.HSQLDB,
////                    jdbcUrl,
////                    username,
////                    password,
////                    query.sql()
////            ).execute(resultSetHandler);
//
//            return true;
//        } catch (Throwable t) {
//            log.severe("Encountered error when running SQL: " + t.getMessage());
//
//            return false;
//        }
    }

}
