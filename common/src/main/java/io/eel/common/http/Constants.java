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

}
