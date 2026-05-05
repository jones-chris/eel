package io.eel.flow_api_aws_lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.eel.common.dao.FlowExecutionDao;
import io.eel.common.http.BaseController;
import io.eel.common.http.HttpRequest;
import io.eel.common.http.HttpResponse;
import io.eel.common.mappers.RequestMapper;
import io.eel.common.mappers.aws.ApiGatewayProxyRequestMapper;
import io.eel.common_aws.AwsDynamoDbFlowDaoImpl;
import io.eel.common_aws.AwsDynamoDbFlowExecutionDaoImpl;
import io.eel.flow_api_aws_lambda.dao.AwsSqsFlowInfrastructureActionQueueDaoImpl;
import io.eel.flow_api_core.FlowController;
import io.eel.common.dao.FlowDao;
import io.eel.flow_api_core.dao.FlowInfrastructureActionQueueDao;
import io.eel.flow_api_core.service.FlowExecutionService;
import io.eel.flow_api_core.service.FlowExecutionServiceImpl;
import io.eel.flow_api_core.service.FlowService;
import io.eel.flow_api_core.service.FlowServiceImpl;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class StreamLambdaHandler implements RequestHandler<Map<String, Object>, HttpResponse> {

    private final static Logger log = Logger.getLogger(StreamLambdaHandler.class.getName());

    private final static RequestMapper<Map<String, Object>> requestMapper = new ApiGatewayProxyRequestMapper();

    private final static FlowInfrastructureActionQueueDao flowInfrastructureActionQueueDao;

    private final static FlowService flowService;

    private final static FlowExecutionService flowExecutionService;

    private final static BaseController flowController;

    static {
        final String s3LandingBucketName = Optional.ofNullable(System.getenv("S3_STAGING_BUCKET_NAME")).orElseThrow();
        final String infraActionSqsQueueUrl = Optional.ofNullable(System.getenv("INFRA_ACTION_SQS_QUEUE_URL")).orElseThrow();

        final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
        final FlowDao flowDao = new AwsDynamoDbFlowDaoImpl(dynamoDbClient, s3LandingBucketName);
        final FlowExecutionDao flowExecutionDao = new AwsDynamoDbFlowExecutionDaoImpl(dynamoDbClient);

        flowInfrastructureActionQueueDao = new AwsSqsFlowInfrastructureActionQueueDaoImpl(infraActionSqsQueueUrl);
        flowService = new FlowServiceImpl(flowDao, flowInfrastructureActionQueueDao);
        flowExecutionService = new FlowExecutionServiceImpl(flowExecutionDao);
        flowController = new FlowController(flowService, flowExecutionService);
    }

    @Override
    public HttpResponse handleRequest(Map<String, Object> event, Context context) {
        log.info("Inside handleRequest");
        log.info("Event: " + event);

        final HttpRequest request = requestMapper.map(event);
        final HttpResponse response = new HttpResponse();

        flowController.handle(request, response);

        return response;
    }

}
