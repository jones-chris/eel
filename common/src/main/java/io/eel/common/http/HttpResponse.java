package io.eel.common.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HttpResponse {

    private int statusCode;

    private final Map<String, String> headers = new HashMap<>();

    private String body;

    public int getStatusCode() {
        return this.statusCode;
    }

    public HttpResponse setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public String getBody() {
        return this.body;
    }

    public HttpResponse setBody(String body) {
        this.body = body;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        HttpResponse that = (HttpResponse) o;
        return statusCode == that.statusCode && Objects.equals(headers, that.headers) && Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, headers, body);
    }

}
