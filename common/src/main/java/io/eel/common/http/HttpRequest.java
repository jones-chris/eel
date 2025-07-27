package io.eel.common.http;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HttpRequest {

    private String httpMethod;

    private String path;

    private JsonObject body = new JsonObject();

    private final Map<String, List<String>> queryParameters = new HashMap<>();

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public JsonObject getBody() {
        return body;
    }

    public void setBody(JsonObject body) {
        this.body = body;
    }

    public Map<String, List<String>> getQueryParameters() {
        return queryParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        HttpRequest that = (HttpRequest) o;
        return Objects.equals(httpMethod, that.httpMethod) && Objects.equals(path, that.path) && Objects.equals(body, that.body) && Objects.equals(queryParameters, that.queryParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(httpMethod, path, body, queryParameters);
    }
}
