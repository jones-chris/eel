package io.eel.flow_api_core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.eel.common.http.BaseController;
import io.eel.common.http.HttpRequest;
import io.eel.common.model.Flow;
import io.eel.flow_api_core.exception.ImmutableFlowException;
import io.eel.flow_api_core.exception.ResourceNotFoundException;
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

                    final Integer version = request.getQueryParameters().get("version")
                            .stream()
                            .map(Integer::parseInt)
                            .toList()
                            .getFirst();

                    if (version == null) {
                        log.severe("Empty or non-existent version query parameter");

                        clientError(response);
                        return;
                    }

                    final String canonicalId = Flow.Utils.getCanonicalId(id, version);
                    this.flowService.getFlowByCanonicalId(canonicalId)
                            .ifPresentOrElse(
                                    flow -> ok(response).setBody(gson.toJson(flow)),
                                    () -> notFound(response)
                            );
                }
        ).addRouteHandler(
                // Updates an existing flow.  Note that this can only be done to un-finalized flows because finalized flows
                // are immutable.
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

                    if (! request.getQueryParameters().containsKey("version")) {
                        log.severe("No 'version' query parameter");

                        if (request.getQueryParameters().get("version").isEmpty()) {
                            log.severe("'version' query parameter is an empty list");
                        }

                        clientError(response);
                        return;
                    }

                    final UUID id = UUID.fromString(request.getQueryParameters().get("id").getFirst());
                    final int version = Integer.parseInt(request.getQueryParameters().get("version").getFirst());
                    final String canonicalId = Flow.Utils.getCanonicalId(id, version);
                    Flow newFlow = gson.fromJson(request.getBody(), Flow.class);

                    try {
                        Flow persistedFlow = this.flowService.updateFlow(canonicalId, newFlow);
                        ok(response).setBody(gson.toJson(persistedFlow));
                    } catch (ResourceNotFoundException ex) {
                        notFound(response);
                    } catch (ImmutableFlowException ex) {
                        clientError(response);
                    } catch (Throwable t) {
                        internalServerError(response);
                    }
                }
        ).addRouteHandler(
                "POST", "/flow/new",
                (request, response) -> {
                    final Flow flow = this.flowService.createNewFlow();

                    // Return UUID and presigned URL.
                    created(response).setBody(
                            gson.toJson(flow)
                    );

                }
        ).addRouteHandler(
                // Note that this can only be done to finalized flows because finalized flows are immutable.
                "POST", "/flow/increment",
                (request, response) -> {
                    deserializeRequestBody(request)
                            .ifPresentOrElse(
                                    f -> {
                                        if (! f.isFinalized()) {
                                            String message = "Flow with canonical id of " + f.getCanonicalId() + " is not finalized";

                                            log.severe(message);
                                            clientError(response, message);

                                            return;
                                        }

                                        final Flow newFlowVersion = f.increment();
                                        final Flow persistedFlow = this.flowService.incrementFlow(newFlowVersion);

                                        created(response).setBody(gson.toJson(persistedFlow));
                                    },
                                    () -> {
                                        clientError(response);
                                    }
                            );
                }
        ).addRouteHandler(
                "GET", "/flow/transformationLandingUrl",
                (request, response) -> {
                    if (! request.getQueryParameters().containsKey("id")) {
                        log.severe("No 'id' query parameter");

                        if (request.getQueryParameters().get("id").isEmpty()) {
                            log.severe("'id' query parameter value is an empty list");
                        }

                        clientError(response);
                        return;
                    }

                    final UUID flowId = UUID.fromString(request.getQueryParameters().get("id").getFirst());
                    final String transformationLandingUrl = this.flowService.generateTransformationStagingPresignedUrl(flowId);

                    ok(response).setBody(
                            gson.toJson(
                                    Map.of(
                                            "id", flowId.toString(),
                                            "url", transformationLandingUrl
                                    )
                            )
                    );
                }
        ).addRouteHandler(
                "GET", "/flow/finalize",
                (request, response) -> {
                    deserializeRequestBody(request)
                            .ifPresentOrElse(
                                    flow -> {
                                        if (flow.isFinalized()) {
                                            String message = "Flow with canonical id of " + f.getCanonicalId() + " is already finalized";

                                            log.severe(message);
                                            clientError(response, message);

                                            return;
                                        }

                                        final Flow persistedFlow = this.flowService.finalizeFlow(flow);

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
