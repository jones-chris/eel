package io.eel.common.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import static io.eel.common.http.Constants.internalServerError;
import static io.eel.common.http.Constants.notFound;

public abstract class BaseController {

    protected static final Logger log = Logger.getLogger(BaseController.class.getName());

    protected final static Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static final String GET = "GET";

    public static final String POST = "POST";

    public static final String PUT = "PUT";

    public static final String DELETE = "DELETE";

    private final Map<
            Pair<String, String>, // The HTTP method and path.
            BiConsumer<HttpRequest, HttpResponse> // The handler function.
    > routeHandlers = new HashMap<>();

    public final BaseController addRouteHandler(final String httpMethod, final String path, BiConsumer<HttpRequest, HttpResponse> handler) {
        Pair<String, String> httpMethodAndPath = Pair.of(httpMethod, path);
        if (this.routeHandlers.get(httpMethodAndPath) != null) {
            throw new IllegalArgumentException("A handler already exists for HTTP method " + httpMethod + " and path " + path);
        }

        this.routeHandlers.put(httpMethodAndPath, handler);

        return this;
    }

    public final void handle(HttpRequest request, HttpResponse response) {
        try {
            Optional.ofNullable(
                    this.routeHandlers.get(
                            Pair.of(
                                    request.getHttpMethod(),
                                    request.getPath()
                            )
                    )
            ).ifPresentOrElse(
                    (handler) -> {
                                log.info("Route handler found for HTTP method and path: " + request.getHttpMethod() + " " + request.getPath());
                                handler.accept(request, response);
                            },
                            () -> {
                                log.severe("No route handler found for HTTP method and path: " + request.getHttpMethod() + " " + request.getPath());
                                notFound(response);
                            }
                    );
        } catch (Throwable t) {
            log.severe(t.getMessage());

            // All responses will be a 500 response by default if a route handler throws an exception.
            internalServerError(response);
        }
    }

}
