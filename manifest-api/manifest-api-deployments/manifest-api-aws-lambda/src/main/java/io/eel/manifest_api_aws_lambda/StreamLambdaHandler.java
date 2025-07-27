package io.eel.manifest_api_aws_lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.eel.common.http.HttpRequest;
import io.eel.common.http.HttpResponse;
import io.eel.manifest_api_aws_lambda.dao.AwsDynamoDbManifestDaoImpl;
import io.eel.manifest_api_aws_lambda.dao.AwsS3WorkbookDaoImpl;
import io.eel.manifest_generator_core.ManifestController;
import io.eel.manifest_generator_core.dao.ManifestDao;
import io.eel.manifest_generator_core.dao.WorkbookDao;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StreamLambdaHandler implements RequestHandler<Map<String, Object>, HttpResponse> {

    private static final Logger log = Logger.getLogger(StreamLambdaHandler.class.getName());

    private final static Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private final static ManifestDao manifestDao;

    private final static WorkbookDao workbookDao;

    private final static ManifestController manifestController;

    private final static Supplier<HttpResponse> defaultInternalServerErrorResponseSupplier = () -> {
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setStatus(500);
        return httpResponse;
    };

    static {
        manifestDao = new AwsDynamoDbManifestDaoImpl(DynamoDbClient.create());
        workbookDao = new AwsS3WorkbookDaoImpl(S3Client.builder().build());

        manifestController = new ManifestController(manifestDao, workbookDao);
    }

    @Override
    public HttpResponse handleRequest(Map<String, Object> event, Context context) {
        log.info("Inside handleRequest");
        log.info("Event: " + event);

        final HttpRequest httpRequest = toHttpRequest(event);
        final HttpResponse httpResponse = new HttpResponse();

        try {
            log.info("Calling manifest controller");

            manifestController.handle(httpRequest, httpResponse);
            return httpResponse;
        } catch (Throwable t) {
            log.severe(t.getMessage());
            return defaultInternalServerErrorResponseSupplier.get();
        }
    }

    private static HttpRequest toHttpRequest(Map<String, Object> event) {
        JsonObject bodyJson = gson.fromJson(gson.toJson(event), JsonObject.class);
        log.info("bodyJson: " + bodyJson);

        // We need to split the query string parameters after deserializing them.
        JsonObject unsplitQueryParametersJson = bodyJson.getAsJsonObject("queryStringParameters");
        Map<String, String> unsplitQueryParameters = gson.fromJson(
                unsplitQueryParametersJson,
                TypeToken.getParameterized(Map.class, String.class, String.class).getType()
        );
        Map<String, List<String>> splitQueryParameters = unsplitQueryParameters.entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                value -> List.of(value.getValue().split(","))
                        )
                );

        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setBody(bodyJson.get("body").getAsJsonObject());
        httpRequest.setHttpMethod(bodyJson.get("httpMethod").getAsString());
        httpRequest.setPath( bodyJson.get("path").getAsString());
        splitQueryParameters.forEach((key, value) -> httpRequest.getQueryParameters().put(key, value));
        return httpRequest;
    }

}
