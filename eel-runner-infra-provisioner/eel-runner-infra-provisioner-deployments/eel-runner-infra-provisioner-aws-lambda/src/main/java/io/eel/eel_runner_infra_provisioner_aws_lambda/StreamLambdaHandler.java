package io.eel.eel_runner_infra_provisioner_aws_lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import java.util.logging.Logger;

public class StreamLambdaHandler implements RequestHandler<SQSEvent, Boolean> {

    private final static Logger log = Logger.getLogger(StreamLambdaHandler.class.getName());

//    private final static RequestMapper<Map<String, Object>> requestMapper = new ApiGatewayProxyRequestMapper();
//
//    static {
//        flowDao = new AwsDynamoDbFlowDaoImpl(DynamoDbClient.create(), System.getenv("S3_STAGING_BUCKET_NAME"));
//        flowService = new FlowServiceImpl(flowDao);
//
//        flowController = new FlowController(flowService);
//    }

    @Override
    public Boolean handleRequest(SQSEvent event, Context context) {
        log.info("Inside handleRequest");
        log.info("Event: " + event);

//        final HttpRequest request = requestMapper.map(event);
//        final HttpResponse response = new HttpResponse();

//        flowController.handle(request, response);

        return true;
    }

}
