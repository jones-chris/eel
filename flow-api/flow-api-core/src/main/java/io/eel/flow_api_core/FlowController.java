package io.eel.flow_api_core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.eel.common.http.BaseController;
import io.eel.common.http.HttpRequest;
import io.eel.common.model.Flow;
import io.eel.common.model.FlowInitDto;
import io.eel.flow_api_core.service.FlowService;

import java.util.*;

import static io.eel.common.http.Constants.*;

public class FlowController extends BaseController {

    private final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private FlowService flowService;

    private FlowController() {
        super();
    }

    public FlowController(FlowService flowService) {
        this.flowService = flowService;

        this.addRouteHandler(
                // Get all flows for a user.
                GET, "/flow/list",
                (request, response) -> {
                    if (request.getQueryParameters().isEmpty()) {
                        log.severe("No query parameters");

                        clientError(response);
                        return;
                    }

                    final Optional<List<String>> userNames = Optional.ofNullable(request.getQueryParameters().get("userName"));
                    if (userNames.isEmpty() || userNames.get().isEmpty()) {
                        log.severe("Empty or non-existent username query parameter");

                        clientError(response);
                        return;
                    }

                    Set<UUID> flowIds = this.flowService.getFlowsByUser(userNames.get().getFirst());

                    ok(response)
                            .setBody(gson.toJson(Map.of("flowIds", flowIds)));
                }
        ).addRouteHandler(
                // Get a flow by a flow ID.
                GET, "/flow",
                (request, response) -> {
                    if (request.getQueryParameters().isEmpty()) {
                        log.severe("No query parameters");

                        clientError(response);
                        return;
                    }

                    final UUID id = request.getQueryParameters().get("id")
                            .stream()
                            .map(UUID::fromString)
                            .toList()
                            .getFirst();

                    if (id == null) {
                        log.severe("Empty or non-existent id query parameter");

                        clientError(response);
                        return;
                    }

                    this.flowService.getFlowById(id)
                            .ifPresentOrElse(
                                    flow -> ok(response).setBody(gson.toJson(flow)),
                                    () -> notFound(response)
                            );
                }
//        ).addRouteHandler(
//                POST, "/flow",
//                (request, response) -> {
//                    final Flow flow = Flow.create();
//
//                    final Flow persistedFlow = this.flowDao.flo(flow);
//
//                    created(response)
//                            .setBody(gson.toJson(persistedFlow));
//                }
        ).addRouteHandler(
                // Updates an existing flow.
                PUT, "/flow/update",
                (request, response) -> {
                    if (request.getBody().isEmpty()) {
                        log.severe("No request body");

                        clientError(response);
                        return;
                    }

                    if (! request.getQueryParameters().containsKey("id")) {
                        log.severe("No 'id' query parameter");

                        if (request.getQueryParameters().get("id").isEmpty()) {
                            log.severe("'id' query parameter value is an empty list");
                        }

                        clientError(response);
                        return;
                    }

                    final UUID id = UUID.fromString(request.getQueryParameters().get("id").getFirst());
                    Flow newFlow = gson.fromJson(request.getBody(), Flow.class);

                    this.flowService.getFlowById(id)
                            .ifPresentOrElse(
                                    (originalFlow) -> {
                                        // Performs a complete overwrite of the existing flow.
                                        Flow persistedFlow = this.flowService.updateFlow(newFlow);

                                        ok(response).setBody(gson.toJson(persistedFlow));
                                    },
                                    () -> notFound(response)
                            );
                }
        ).addRouteHandler(
                "POST", "/flow/new",
                (request, response) -> {
                    final FlowInitDto flowInit = this.flowService.createNewFlow();

                    // Return UUID and presigned URL.
                    created(response).setBody(
                            gson.toJson(flowInit)
                    );

                }
        ).addRouteHandler(
                "POST", "/flow/increment",
                (request, response) -> {
                    deserializeRequestBody(request)
                            .ifPresentOrElse(
                                    f -> {
                                        final Flow newFlowVersion = f.increment();
                                        final Flow persistedFlow = this.flowService.updateFlow(newFlowVersion);

                                        created(response).setBody(gson.toJson(persistedFlow));
                                    },
                                    () -> {
                                        clientError(response);
                                    }
                            );
                }
        );
    }

    private Optional<Flow> deserializeRequestBody(HttpRequest request) {
        if (request.getBody().isEmpty()) {
            log.severe("No request body");
            return Optional.empty();
        }

        return Optional.ofNullable(gson.fromJson(request.getBody(), Flow.class));
    }

}
