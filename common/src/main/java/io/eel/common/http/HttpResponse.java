package io.eel.common.http;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HttpResponse {

    private int status;

    private final Map<String, String> headers = new HashMap<>();

    private String body;

    public int getStatus() {
        return status;
    }

    public HttpResponse setStatus(int status) {
        this.status = status;
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
        return status == that.status && Objects.equals(headers, that.headers) && Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, headers, body);
    }

}
