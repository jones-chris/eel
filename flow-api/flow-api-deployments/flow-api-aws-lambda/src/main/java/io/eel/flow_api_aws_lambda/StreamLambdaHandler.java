package io.eel.flow_api_aws_lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.eel.common.http.BaseController;
import io.eel.common.http.HttpRequest;
import io.eel.common.http.HttpResponse;
import io.eel.common.mappers.RequestMapper;
import io.eel.common.mappers.aws.ApiGatewayProxyRequestMapper;
import io.eel.common_aws.AwsDynamoDbFlowDaoImpl;
import io.eel.flow_api_aws_lambda.stacks.service.AwsEelBatchProcessorStackOrchestratorImpl;
import io.eel.flow_api_core.stacks.EelBatchProcessorStackOrchestrator;
import io.eel.flow_api_core.FlowController;
import io.eel.common.dao.FlowDao;
import io.eel.flow_api_core.service.FlowService;
import io.eel.flow_api_core.service.FlowServiceImpl;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;

import static io.eel.common.http.Constants.internalServerError;


@Slf4j
public class StreamLambdaHandler implements RequestHandler<Map<String, Object>, HttpResponse> {

    private final static RequestMapper<Map<String, Object>> requestMapper = new ApiGatewayProxyRequestMapper();

    private final static FlowDao flowDao;

    private final static EelBatchProcessorStackOrchestrator eelBatchProcessorStackOrchestrator;

    private final static FlowService flowService;

    private final static BaseController flowController;

    static {
        final String s3StagingBucketName = System.getenv("S3_STAGING_BUCKET_NAME");
        DynamoDbClient dynamoDbClient = DynamoDbClient.create();

        flowDao = new AwsDynamoDbFlowDaoImpl(dynamoDbClient, s3StagingBucketName);

        eelBatchProcessorStackOrchestrator = new AwsEelBatchProcessorStackOrchestratorImpl(dynamoDbClient);

        flowService = new FlowServiceImpl(flowDao, eelBatchProcessorStackOrchestrator);

        flowController = new FlowController(flowService);
    }

    @Override
    public HttpResponse handleRequest(Map<String, Object> event, Context context) {
        final HttpResponse response = new HttpResponse();

        try {
            log.debug("Inside handleRequest");
            log.debug("Event: {}", event);

            final HttpRequest request = requestMapper.map(event);

            flowController.handle(request, response);

            return response;
        } catch (Throwable t) {
            log.error("", t);

            internalServerError(response);
            return response;
        }
    }

}
