package io.eel.manifest_api_aws_lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.eel.common.http.BaseController;
import io.eel.common.http.HttpRequest;
import io.eel.common.http.HttpResponse;
import io.eel.common.mappers.RequestMapper;
import io.eel.common.mappers.aws.ApiGatewayProxyRequestMapper;
import io.eel.manifest_api_aws_lambda.dao.AwsDynamoDbManifestDaoImpl;
import io.eel.manifest_api_aws_lambda.dao.AwsS3WorkbookDaoImpl;
import io.eel.manifest_generator_core.ManifestController;
import io.eel.manifest_generator_core.dao.ManifestDao;
import io.eel.manifest_generator_core.dao.WorkbookDao;
import io.eel.manifest_generator_core.service.ManifestService;
import io.eel.manifest_generator_core.service.ManifestServiceImpl;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Map;
import java.util.logging.Logger;

public class RestApiHandler implements RequestHandler<Map<String, Object>, HttpResponse> {

    private final static Logger log = Logger.getLogger(RestApiHandler.class.getName());

    private final static RequestMapper<Map<String, Object>> requestMapper = new ApiGatewayProxyRequestMapper();

    private final static ManifestDao manifestDao;

    private final static WorkbookDao workbookDao;

    private final static ManifestService manifestService;

    private final static BaseController manifestController;

    static {
        manifestDao = new AwsDynamoDbManifestDaoImpl(DynamoDbClient.create());
        workbookDao = new AwsS3WorkbookDaoImpl(S3Client.builder().build());
        manifestService = new ManifestServiceImpl(workbookDao, manifestDao);

        manifestController = new ManifestController(manifestDao, workbookDao, manifestService);
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
