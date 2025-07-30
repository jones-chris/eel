package io.eel.flow_api_core;

import io.eel.common.http.BaseController;
import io.eel.flow_api_core.dao.FlowDao;

import java.util.*;

import static io.eel.common.http.Constants.clientError;
import static io.eel.common.http.Constants.notFound;

public class FlowController extends BaseController {

    private FlowDao flowDao;

    private FlowController() {
        super();
    }

    public FlowController(FlowDao flowDao) {
        this.flowDao = flowDao;

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

                    Set<UUID> flowIds = this.flowDao.getFlowsByUser(userNames.get().getFirst());

                    response.setStatusCode(200)
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

                    this.flowDao.getFlowById(id)
                            .ifPresentOrElse(
                                    flow -> response.setStatusCode(200).setBody(gson.toJson(flow)),
                                    () -> notFound(response)
                            );
                }
        );
    }

}
