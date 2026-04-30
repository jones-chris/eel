package io.eel.manifest_generator_core;

import io.eel.common.WorkbookValidator.Manifest;
import io.eel.common.http.BaseController;
import io.eel.common.model.Flow;
import io.eel.common.model.WorkbookMetadata;
import io.eel.manifest_generator_core.service.ManifestService;
import io.eel.manifest_generator_core.service.TransformationJarService;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.eel.common.http.Constants.*;

public class ManifestController extends BaseController {

    private ManifestService manifestService;

    private TransformationJarService transformationJarService;

    private ManifestController() {
        super();
    }

    public ManifestController(ManifestService manifestService, TransformationJarService transformationJarService) {
        super();

        this.manifestService = manifestService;
        this.transformationJarService = transformationJarService;

        this.addRouteHandler(
                GET, "/manifest",
                (request, response) -> {
                    Optional<UUID> manifestUuid = Optional.ofNullable(request.getQueryParameters().get("uuid").getFirst())
                            .map(UUID::fromString);

                    Optional<Integer> manifestVersion = Optional.ofNullable(request.getQueryParameters().get("version").getFirst())
                            .map(Integer::parseInt);

                    if (manifestUuid.isEmpty() || manifestVersion.isEmpty()) {
                        clientError(response);
                        return;
                    }

                    this.manifestService.getManifest(manifestUuid.get(), manifestVersion.get())
                        .ifPresentOrElse(
                                manifest -> ok(response).setBody(gson.toJson(manifest)),
                                () -> notFound(response)
                        );
                }
        ).addRouteHandler(
                POST, "/manifest",
                (request, response) -> {
                    if (request.getBody().isEmpty()) {
                        log.severe("Request body is empty");

                        notFound(response);
                        return;
                    }

                    // Get the workbook's bucket and key.
                    final String bucket = request.getBody().get("bucket").getAsString();
                    final String key = request.getBody().get("key").getAsString();

                    // Get workbook metadata like author, name, and version from request body.
                    final WorkbookMetadata workbookMetadata = gson.fromJson(request.getBody(), WorkbookMetadata.class);

                    Manifest manifest = this.manifestService.createManifest(bucket, key, workbookMetadata);

                    // Craft the HTTP response.
                    created(response).setBody(gson.toJson(manifest));
                }
        ).addRouteHandler(
                POST, "/artifact/build",
                (request, response) -> {
                    Optional<UUID> manifestUuid = Optional.ofNullable(request.getQueryParameters().get("uuid").getFirst())
                            .map(UUID::fromString);

                    Optional<Integer> manifestVersion = Optional.ofNullable(request.getQueryParameters().get("version").getFirst())
                            .map(Integer::parseInt);

                    if (manifestUuid.isEmpty() || manifestVersion.isEmpty()) {
                        clientError(response);
                        return;
                    }

                    String flowCanonicalId = Flow.Utils.getCanonicalId(manifestUuid.get(), manifestVersion.get());

                    final String presignedUrl = this.transformationJarService.buildJar(flowCanonicalId);
                    log.info("Generated presigned URL: " + presignedUrl);

                    ok(response)
                            .setBody(
                                    gson.toJson(
                                            Map.of("url", presignedUrl)
                                    )
                            );
                }
        );
    }

}
