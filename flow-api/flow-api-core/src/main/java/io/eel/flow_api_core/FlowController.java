package io.eel.flow_api_core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.eel.common.http.BaseController;
import io.eel.common.http.HttpRequest;
import io.eel.common.model.Flow;
import io.eel.common.model.FlowExecution;
import io.eel.common.model.TransformationExtractionType;
import io.eel.flow_api_core.exception.ImmutableFlowException;
import io.eel.flow_api_core.exception.ResourceNotFoundException;
import io.eel.flow_api_core.service.FlowExecutionService;
import io.eel.flow_api_core.service.FlowService;

import java.util.*;

import static io.eel.common.http.Constants.*;

public class FlowController extends BaseController {

    private final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private FlowService flowService;

    private FlowExecutionService flowExecutionService;

    private FlowController() {
        super();
    }

    public FlowController(FlowService flowService, FlowExecutionService flowExecutionService) {
        this.flowService = flowService;
        this.flowExecutionService = flowExecutionService;

//        this.addRouteHandler(
//                // Get all flows for a user.
//                GET, "/flow/list",
//                (request, response) -> {
//                    if (request.getQueryParameters().isEmpty()) {
//                        log.severe("No query parameters");
//
//                        clientError(response);
//                        return;
//                    }
//
//                    final Optional<List<String>> userNames = Optional.ofNullable(request.getQueryParameters().get("userName"));
//                    if (userNames.isEmpty() || userNames.get().isEmpty()) {
//                        log.severe("Empty or non-existent username query parameter");
//
//                        clientError(response);
//                        return;
//                    }
//
//                    Set<UUID> flowIds = this.flowService.getFlowsByUser(userNames.get().getFirst());
//
//                    ok(response)
//                            .setBody(gson.toJson(Map.of("flowIds", flowIds)));
//                }
//        )
//        .addRouteHandler(
//                GET, "/flow/versions",
//                (request, response) -> {
//                    if (request.getQueryParameters().isEmpty()) {
//                        log.severe("No query parameters");
//
//                        clientError(response);
//                        return;
//                    }
//
//                    // Check that flow id query parameter exists and has exactly 1 value.
//                    final List<String> idValues = request.getQueryParameters().get("id");
//                    if (idValues == null || idValues.size() != 1) {
//                        log.severe("Empty, non-existent, or not exactly 1 'id' query parameter: " + idValues);
//
//                        clientError(response);
//                        return;
//                    }
//                    final UUID id = UUID.fromString(idValues.getFirst());
//
//                    List<Integer> flowVersions = this.flowService.getFlowVersionsByFlowId(id)
//                            .stream()
//                            .sorted()
//                            .toList();
//
//                    ok(response)
//                            .setBody(
//                                    gson.toJson(
//                                            Map.of("flowVersions", flowVersions)
//                                    )
//                            );
//                }
//        )
//        .addRouteHandler(
//                // Get a flow by a flow ID.
//                GET, "/flow",
//                (request, response) -> {
//                    // There are 3 validation steps:
//                    // 1) Check that query parameters exist.
//                    if (request.getQueryParameters().isEmpty()) {
//                        log.severe("No query parameters");
//
//                        clientError(response);
//                        return;
//                    }
//
//                    // 2) Get flow id from query parameters.
//                    final List<String> idValues = request.getQueryParameters().get("id");
//                    if (idValues == null || idValues.size() != 1) {
//                        log.severe("Empty, non-existent, or not exactly 1 'id' query parameter: " + idValues);
//
//                        clientError(response);
//                        return;
//                    }
//                    final UUID id = UUID.fromString(idValues.getFirst());
//
//                    // 3) Get version from query parameters.
//                    final List<String> versionValues = request.getQueryParameters().get("version");
//                    if (versionValues == null || versionValues.size() != 1) {
//                        log.severe("Empty, non-existent, or not exactly 1 'version' query parameter: " + versionValues);
//
//                        clientError(response);
//                        return;
//                    }
//                    final int versionInt = Integer.parseInt(versionValues.getFirst());
//
//                    // Get flow by id and version.  If not found, return a 404.  If found, return a 200 with the flow
//                    // in the response body.
//                    this.flowService.getFlowByIdAndVersion(id.toString(), versionInt)
//                            .ifPresentOrElse(
//                                    flow -> ok(response).setBody(gson.toJson(flow)),
//                                    () -> notFound(response)
//                            );
//                }
//        )
//        .addRouteHandler(
//                // Updates the existing flow.  Note that this can only be done to un-finalized flows because finalized flows
//                // are immutable.
//                PUT, "/flow/update",
//                (request, response) -> {
//                    // Check that request body is not null or empty.
//                    if (request.getBody() == null || request.getBody().isEmpty()) {
//                        log.severe("No request body");
//
//                        clientError(response);
//                        return;
//                    }
//                    Flow newFlow = gson.fromJson(request.getBody(), Flow.class);
//
//                    // Check that new flow has an id.
//                    if (newFlow.getId() == null) {
//                        log.severe("Flow id cannot be null");
//
//                        clientError(response);
//                        return;
//                    }
//
//                    try {
//                        Flow persistedFlow = this.flowService.updateFlow(newFlow);
//                        ok(response).setBody(gson.toJson(persistedFlow));
//                    } catch (ResourceNotFoundException ex) {
//                        notFound(response);
//                    } catch (ImmutableFlowException ex) {
//                        clientError(response);
//                    } catch (Throwable t) {
//                        internalServerError(response);
//                    }
//                }
//        )
        this.addRouteHandler(
                POST, "/flow/new",
                (request, response) -> {
                    final Flow flow = this.flowService.createNewFlow();

                    // Return UUID and presigned URL.
                    created(response)
                            .setBody(gson.toJson(flow));
                }
        )
//        .addRouteHandler(
//                // Note that this can only be done to finalized flows because finalized flows are immutable.
//                POST, "/flow/increment",
//                (request, response) -> {
//                    deserializeRequestBody(request)
//                            .ifPresentOrElse(
//                                    f -> {
//                                        if (! f.isFinalized()) {
//                                            String message = "Flow with canonical id of " + f.getCanonicalId() + " is not finalized";
//
//                                            log.severe(message);
//                                            clientError(response, message);
//
//                                            return;
//                                        }
//
//                                        final Flow newFlowVersion = f.increment();
//                                        final Flow persistedFlow = this.flowService.incrementFlow(newFlowVersion);
//
//                                        created(response).setBody(gson.toJson(persistedFlow));
//                                    },
//                                    () -> {
//                                        clientError(response);
//                                    }
//                            );
//                }
//        )
        .addRouteHandler(
                GET, "/flow/transformationLandingUrl",
                (request, response) -> {
                    if (! request.getQueryParameters().containsKey("id")) {
                        log.severe("No 'id' query parameter");

                        if (request.getQueryParameters().get("id").isEmpty()) {
                            log.severe("'id' query parameter value is an empty list");
                        }

                        clientError(response);
                        return;
                    }

                    TransformationExtractionType extractionType = Optional.ofNullable(request.getQueryParameters().get("type"))
                            .filter(list -> ! list.isEmpty())
                            .map(List::getFirst)
                            .map(type -> TransformationExtractionType.valueOf(type.toUpperCase()))
                            .orElseThrow(() -> {;
                                String message = "No 'type' query parameter or empty 'type' query parameter value.  'type' query parameter is required and must be one of: " + Arrays.toString(TransformationExtractionType.values());

                                log.severe(message);
                                return new IllegalArgumentException(message);
                            });

                    final UUID flowId = UUID.fromString(request.getQueryParameters().get("id").getFirst());
                    final String transformationLandingUrl = this.flowService.generateTransformationStagingPresignedUrl(flowId, extractionType);

                    ok(response).setBody(
                            gson.toJson(
                                    Map.of(
                                            "id", flowId.toString(),
                                            "url", transformationLandingUrl
                                    )
                            )
                    );
                }
        );
//        .addRouteHandler(
//                POST, "/flow/deploy",
//                (request, response) -> {
//                    // Request validation.  Make sure the required flow id a version are present.
//                    if (! request.getQueryParameters().containsKey("flowId") || ! request.getQueryParameters().containsKey("version")) {
//                        clientError(response);
//                        return;
//                    }
//
//                    final UUID flowId = UUID.fromString(request.getQueryParameters().get("flowId").getFirst());
//                    final int version = Integer.parseInt(request.getQueryParameters().get("version").getFirst());
//
//                    // Get the flow by the id and version.
//                    Optional<Flow> flowOptional = this.flowService.getFlowByIdAndVersion(flowId.toString(), version);
//
//                    // If not found, return a 404.
//                    if (flowOptional.isEmpty()) {
//                        notFound(response);
//                        return;
//                    }
//
//                    // If found, but it is already finalized/deployed, then return a 400.
//                    Flow flow = flowOptional.get();
//                    if (flow.isFinalized()) {
//                        String message = "Flow with canonical id of " + flow.getCanonicalId() + " is already deployed";
//
//                        log.severe(message);
//                        clientError(response, message);
//
//                        return;
//                    }
//
//                    // Otherwise, finalize/deploy the flow and return a 201.
//                    final Flow persistedFlow = this.flowService.finalizeFlow(flow);
//
//                    created(response).setBody(gson.toJson(persistedFlow));
//                }
//        )
//        .addRouteHandler(
//                GET, "/flow/rollback",
//                (request, response) -> {
//                    // Request validation.  Make sure the required flow id a version are present.
//                    if (! request.getQueryParameters().containsKey("flowId") || ! request.getQueryParameters().containsKey("version")) {
//                        clientError(response);
//                        return;
//                    }
//
//                    final UUID flowId = UUID.fromString(request.getQueryParameters().get("flowId").getFirst());
//                    final int version = Integer.parseInt(request.getQueryParameters().get("version").getFirst());
//
//                    // Get the flow by the id and version.
//                    Optional<Flow> flowOptional = this.flowService.getFlowByIdAndVersion(flowId.toString(), version);
//
//                    // If not found, return a 404.
//                    if (flowOptional.isEmpty()) {
//                        notFound(response);
//                        return;
//                    }
//
//                    // If found, but it is already finalized/deployed, then return a 400.
//                    Flow flow = flowOptional.get();
//                    if (! flow.isFinalized()) {
//                        String message = "Flow with canonical id of " + flow.getCanonicalId() + " is not finalized/deployed";
//
//                        log.severe(message);
//                        clientError(response, message);
//
//                        return;
//                    }
//
//                    // Otherwise, rollback/un-finalize the flow and return a 201.
//                    final Flow persistedFlow = this.flowService.unfinalizeFlow(flow);
//
//                    created(response).setBody(gson.toJson(persistedFlow));
//                }
//        )
//        .addRouteHandler(
//                GET, "/flow/execution/status",
//                (request, response) -> {
//                    if (! request.getQueryParameters().containsKey("flowId") || ! request.getQueryParameters().containsKey("executionTimestamp")) {
//                        clientError(response);
//                        return;
//                    }
//
//                    final UUID flowId = UUID.fromString(request.getQueryParameters().get("flowId").getFirst());
//                    final long executionTimeStamp = Long.parseLong(request.getQueryParameters().get("executionTimestamp").getFirst());
//
//                    // Get flow execution status.
//                    this.flowExecutionService.getById(flowId, executionTimeStamp)
//                            .ifPresentOrElse(
//                                    flowExecution -> ok(response).setBody(gson.toJson(flowExecution)),
//                                    () -> notFound(response)
//                            );
//                }
//        )
//        .addRouteHandler(
//                GET, "flow/execution/list",
//                (request, response) -> {
//                    if (! request.getQueryParameters().containsKey("flowId")) {
//                        clientError(response);
//                        return;
//                    }
//
//                    final UUID flowId = UUID.fromString(request.getQueryParameters().get("flowId").getFirst());
//
//                    // Get flow execution status.
//                    List<FlowExecution> flowExecutions = this.flowExecutionService.getPageByFlowId(flowId);
//
//                    // If no flow executions found, return a 404.
//                    if (flowExecutions.isEmpty()) {
//                        notFound(response);
//                        return;
//                    }
//
//                    // Otherwise, return a 200 with the list of flow executions.
//                    ok(response).setBody(gson.toJson(flowExecutions));
//                }
//        );
    }

    private Optional<Flow> deserializeRequestBody(HttpRequest request) {
        if (request.getBody().isEmpty()) {
            log.severe("No request body");
            return Optional.empty();
        }

        return Optional.ofNullable(gson.fromJson(request.getBody(), Flow.class));
    }

}
