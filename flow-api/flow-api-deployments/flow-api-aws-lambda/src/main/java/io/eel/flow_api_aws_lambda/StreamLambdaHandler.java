package io.eel.flow_api_aws_lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.eel.common.http.BaseController;
import io.eel.common.http.HttpRequest;
import io.eel.common.http.HttpResponse;
import io.eel.common.mappers.RequestMapper;
import io.eel.common.mappers.aws.ApiGatewayProxyRequestMapper;
import io.eel.common_aws.AwsDynamoDbFlowDaoImpl;
import io.eel.eel_runner_infra_provisioner_aws_lambda.stacks.AwsLambdaEelBatchProcessorStack;
import io.eel.eel_runner_infra_provisioner_core.stacks.EelBatchProcessorStack;
import io.eel.flow_api_core.FlowController;
import io.eel.common.dao.FlowDao;
import io.eel.flow_api_core.service.FlowService;
import io.eel.flow_api_core.service.FlowServiceImpl;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.Map;
import java.util.logging.Logger;

public class StreamLambdaHandler implements RequestHandler<Map<String, Object>, HttpResponse> {

    private final static Logger log = Logger.getLogger(StreamLambdaHandler.class.getName());

    private final static RequestMapper<Map<String, Object>> requestMapper = new ApiGatewayProxyRequestMapper();

    private final static FlowDao flowDao;

    private final static EelBatchProcessorStack eelBatchProcessorStack;

    private final static FlowService flowService;

    private final static BaseController flowController;

    static {
        flowDao = new AwsDynamoDbFlowDaoImpl(DynamoDbClient.create(), System.getenv("S3_STAGING_BUCKET_NAME"));
        eelBatchProcessorStack = new AwsLambdaEelBatchProcessorStack(
                SchedulerClient.create(),
                LambdaClient.create(),
                S3Client.create(),
                SqsClient.create(),
                IamClient.builder().build(),
                SfnClient.create()
        );
        flowService = new FlowServiceImpl(flowDao, eelBatchProcessorStack);

        flowController = new FlowController(flowService);
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
