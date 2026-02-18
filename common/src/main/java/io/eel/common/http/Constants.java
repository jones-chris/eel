package io.eel.common.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

public class Constants {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static void notFound(HttpResponse httpResponse) {
        httpResponse
                .setStatusCode(404)
                .setBody(null);
    }

    public static void clientError(HttpResponse httpResponse) {
        httpResponse
                .setStatusCode(400)
                .setBody(null);
    }

    public static void clientError(HttpResponse httpResponse, String body) {
        httpResponse
                .setStatusCode(400)
                .setBody(
                        gson.toJson(
                                Map.of("message", body)
                        )
                );
    }

    public static void internalServerError(HttpResponse httpResponse) {
        httpResponse
                .setStatusCode(500)
                .setBody(null);
    }

    public static HttpResponse created(HttpResponse httpResponse) {
        return httpResponse
                .setStatusCode(201)
                .setBody(null);
    }

    /**
     * Sets the status code of the {@link HttpResponse} to 200 Ok.
     *
     * @param httpResponse {@link HttpResponse}
     * @return {@link HttpResponse} because developers are likely/expected to chain calls to this method to set the body
     * or headers before the response is sent back to the client.
     */
    public static HttpResponse ok(HttpResponse httpResponse) {
        return httpResponse
                .setStatusCode(200);
    }

}
