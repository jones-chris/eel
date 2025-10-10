package io.eel.manifest_generator_core;

import io.eel.common.WorkbookValidator.Manifest;
import io.eel.common.http.BaseController;
import io.eel.common.model.WorkbookMetadata;
import io.eel.manifest_generator_core.dao.ManifestDao;
import io.eel.manifest_generator_core.dao.WorkbookDao;
import io.eel.manifest_generator_core.service.ManifestService;

import java.util.Optional;
import java.util.UUID;

import static io.eel.common.http.Constants.notFound;

public class ManifestController extends BaseController {

    // todo:  clean up this unused field.
    private ManifestDao manifestDao;

    private ManifestService manifestService;

    // todo:  clean up this unused field.
    private WorkbookDao workbookDao;

    private ManifestController() {
        super();
    }

    public ManifestController(ManifestDao manifestDao, WorkbookDao workbookDao, ManifestService manifestService) {
        super();

        this.manifestDao = manifestDao;
        this.workbookDao = workbookDao;
        this.manifestService = manifestService;

        this.addRouteHandler(
                GET, "/manifest",
                (request, response) -> {
                    Optional<UUID> manifestUuid = Optional.ofNullable(request.getQueryParameters().get("uuid").getFirst())
                            .map(UUID::fromString);

                    if (manifestUuid.isEmpty()) {
                        response.setStatusCode(400);
                        return;
                    }

                    this.manifestService.getManifest(manifestUuid.get())
                        .ifPresentOrElse(
                                manifest -> response.setStatusCode(200).setBody(gson.toJson(manifest)),
                                () -> notFound(response)
                        );
                }
        ).addRouteHandler(
                POST, "/manifest",
                (request, response) -> {
                    if (request.getBody().isEmpty()) {
                        log.severe("Request body is empty");

                        response.setStatusCode(400);
                        return;
                    }

                    // Get the workbook's bucket and key.
                    final String bucket = request.getBody().get("bucket").getAsString();
                    final String key = request.getBody().get("key").getAsString();

                    // Get workbook metadata like author, name, and version from request body.
                    final WorkbookMetadata workbookMetadata = gson.fromJson(request.getBody(), WorkbookMetadata.class);

                    Manifest manifest = this.manifestService.createManifest(bucket, key, workbookMetadata);

                    // Craft the HTTP response.
                    response.setStatusCode(201)
                            .setBody(gson.toJson(manifest));
                }
        );
    }

}
