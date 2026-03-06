package io.eel.common.mappers.aws;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.eel.common.http.HttpRequest;
import io.eel.common.mappers.RequestMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ApiGatewayProxyRequestMapper implements RequestMapper<Map<String, Object>> {

    private static final Logger log = Logger.getLogger(ApiGatewayProxyRequestMapper.class.getName());

    private final static Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    @Override
    public HttpRequest map(Map<String, Object> event) {
        JsonObject bodyJson = gson.fromJson(gson.toJson(event), JsonObject.class);
        log.info("bodyJson: " + bodyJson);

        // We need to split the query string parameters after deserializing them.
        Optional<JsonObject> unsplitQueryParametersJsonOptional = Optional.ofNullable(bodyJson.getAsJsonObject("queryStringParameters"));
        Map<String, String> unsplitQueryParameters = new HashMap<>();
        if (unsplitQueryParametersJsonOptional.isPresent()) {
            unsplitQueryParameters = gson.fromJson(
                    unsplitQueryParametersJsonOptional.get(),
                    TypeToken.getParameterized(Map.class, String.class, String.class).getType()
            );
        }

        Map<String, List<String>> splitQueryParameters = unsplitQueryParameters.entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                value -> List.of(value.getValue().split(","))
                        )
                );

        HttpRequest httpRequest = new HttpRequest();
        if (bodyJson.get("body") != null) {
            String payloadString = bodyJson.get("body").getAsString();
            JsonObject payload = JsonParser.parseString(payloadString).getAsJsonObject();
            httpRequest.setBody(payload);
        }
        httpRequest.setHttpMethod(bodyJson.getAsJsonObject("requestContext").getAsJsonObject("http").get("method").getAsString());
        httpRequest.setPath(bodyJson.getAsJsonObject("requestContext").getAsJsonObject("http").get("path").getAsString());
        splitQueryParameters.forEach((key, value) -> httpRequest.getQueryParameters().put(key, value));
        return httpRequest;
    }

}
