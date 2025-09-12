package io.eel.common.http;

import java.util.function.Supplier;

public class Constants {

    public static final Supplier<HttpResponse> defaultInternalServerErrorResponseSupplier = () -> new HttpResponse().setStatusCode(500);

    public static final Supplier<HttpResponse> defaultResourceNotFoundResponseSupplier = () -> new HttpResponse().setStatusCode(404);

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
