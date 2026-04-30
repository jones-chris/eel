package io.eel.manifest_api_aws_lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.eel.common.http.BaseController;
import io.eel.common.http.HttpRequest;
import io.eel.common.http.HttpResponse;
import io.eel.common.mappers.RequestMapper;
import io.eel.common.mappers.aws.ApiGatewayProxyRequestMapper;
import io.eel.manifest_api_aws_lambda.dao.AwsDynamoDbManifestDaoImpl;
import io.eel.common_aws.AwsS3WorkbookDaoImpl;
import io.eel.manifest_generator_core.ManifestController;
import io.eel.manifest_generator_core.service.ManifestServiceImpl;
import io.eel.manifest_generator_core.service.TransformationJarServiceImpl;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Map;
import java.util.logging.Logger;

public class RestApiHandler implements RequestHandler<Map<String, Object>, HttpResponse> {

    private final static Logger log = Logger.getLogger(RestApiHandler.class.getName());

    private final static RequestMapper<Map<String, Object>> requestMapper = new ApiGatewayProxyRequestMapper();

    private final static BaseController manifestController;

    static {
        S3Client s3Client = S3Client.create();

        manifestController = new ManifestController(
                new ManifestServiceImpl(
                        new AwsS3WorkbookDaoImpl(s3Client),
                        new AwsDynamoDbManifestDaoImpl(DynamoDbClient.create())
                ),
                new TransformationJarServiceImpl(s3Client)
        );
    }

    @Override
    public HttpResponse handleRequest(Map<String, Object> event, Context context) {
        log.info("Inside handleRequest");
        log.info("Event: " + event);

        final HttpRequest request = requestMapper.map(event);
        final HttpResponse response = new HttpResponse();

        manifestController.handle(request, response);

        return response;
    }

}
