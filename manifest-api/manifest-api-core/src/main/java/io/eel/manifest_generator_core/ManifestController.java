package io.eel.manifest_generator_core;

import io.eel.common.WorkbookValidator;
import io.eel.common.WorkbookValidator.Manifest;
import io.eel.common.http.BaseController;
import io.eel.common.model.WorkbookMetadata;
import io.eel.manifest_generator_core.dao.ManifestDao;
import io.eel.manifest_generator_core.dao.WorkbookDao;
import io.eel.model.proxy.WorkbookProxy;

import java.util.Optional;
import java.util.UUID;

public class ManifestController extends BaseController {

    private ManifestDao manifestDao;

    private WorkbookDao workbookDao;

    private ManifestController() {
        super();
    }

    public ManifestController(ManifestDao manifestDao, WorkbookDao workbookDao) {
        super();

        this.manifestDao = manifestDao;
        this.workbookDao = workbookDao;

        this.addRouteHandler(
                GET, "/manifest",
                (request, response) -> {
                    Optional<UUID> manifestUuid = Optional.ofNullable(request.getQueryParameters().get("uuid").getFirst())
                            .map(UUID::fromString);

                    if (manifestUuid.isEmpty()) {
                        response.setStatusCode(400);
                        return;
                    }

                    Manifest manifest = this.manifestDao.getManifest(manifestUuid.get());

                    response.setStatusCode(200)
                            .setBody(gson.toJson(manifest));
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

                    // Get workbook from bucket.
                    log.info("Getting workbook at bucket " + bucket + " and key " + key);
                    try (
                            final WorkbookProxy workbookProxy = this.workbookDao.getWorkbook(bucket, key)
                                    .orElseThrow(() -> new RuntimeException("Error encountered when retrieving workbook"));
                    ) {

                        // Get workbook metadata like author, name, and version from request body.
                        final WorkbookMetadata workbookMetadata = gson.fromJson(request.getBody(), WorkbookMetadata.class);

                        // Call validator to create the manifest.
                        Manifest manifest = new WorkbookValidator(
                                workbookProxy.getWorkbook(),
                                workbookMetadata.author(), // todo:  make this constructor take a WorkbookMetadata parameter instead of unpacking the object into separate parameters.
                                workbookMetadata.name(),
                                workbookMetadata.version()
                        ).assertIsValid()
                                .createManifest();
                        log.info("Created manifest: " + gson.toJson(manifest));

                        // Persist the manifest.
                        manifest = this.manifestDao.saveManifest(manifest);

                        // Craft the HTTP response.
                        response.setStatusCode(201)
                                .setBody(gson.toJson(manifest));
                    } catch (Throwable t) {
                        log.severe(t.getMessage());

                        response.setStatusCode(500);
                    }
                }
        );
    }

//    @Override
//    public void handle(HttpRequest request, HttpResponse response) {
//        // GET /manifest
////        if (GET.equals(request.getHttpMethod()) && "/manifest".equals(request.getPath())) {
////            Optional<UUID> manifestUuid = Optional.ofNullable(request.getQueryParameters().get("uuid").getFirst())
////                    .map(UUID::fromString);
////
////            if (manifestUuid.isEmpty()) {
////                response.setStatusCode(400);
////                return;
////            }
////
////            Manifest manifest = this.manifestDao.getManifest(manifestUuid.get());
////
////            response.setStatusCode(200)
////                    .setBody(gson.toJson(manifest));
//        // POST /manifest/create
//        // Creates a new manifest given an Excel workbook.
//        // https://stackoverflow.com/questions/68240622/how-to-upload-files-to-aws-s3-directly-from-the-browserfront-end-using-presign
//        // https://cloud.google.com/storage/docs/access-control/signed-urls
//        } else if (POST.equals(request.getHttpMethod()) && "/manifest".equals(request.getPath())) {
//            if (request.getBody().isEmpty()) {
//                log.severe("Request body is empty");
//
//                response.setStatusCode(400);
//                return;
//            }
//
//            // Get the workbook's bucket and key.
//            final String bucket = request.getBody().get("bucket").getAsString();
//            final String key = request.getBody().get("key").getAsString();
//
//            // Get workbook from bucket.
//            log.info("Getting workbook at bucket " + bucket + " and key " + key);
//            try (
//                    final WorkbookProxy workbookProxy = this.workbookDao.getWorkbook(bucket, key)
//                            .orElseThrow(() -> new RuntimeException("Error encountered when retrieving workbook"));
//            ) {
//
//                // Get workbook metadata like author, name, and version from request body.
//                final WorkbookMetadata workbookMetadata = gson.fromJson(request.getBody(), WorkbookMetadata.class);
//
//                // Call validator to create the manifest.
//                Manifest manifest = new WorkbookValidator(
//                        workbookProxy.getWorkbook(),
//                        workbookMetadata.author(), // todo:  make this constructor take a WorkbookMetadata parameter instead of unpacking the object into separate parameters.
//                        workbookMetadata.name(),
//                        workbookMetadata.version()
//                ).assertIsValid()
//                .createManifest();
//                log.info("Created manifest: " + gson.toJson(manifest));
//
//                // Persist the manifest.
//                manifest = this.manifestDao.saveManifest(manifest);
//
//                // Craft the HTTP response.
//                response.setStatusCode(201)
//                        .setBody(gson.toJson(manifest));
//            } catch (Throwable t) {
//                log.severe(t.getMessage());
//
//                response.setStatusCode(500);
//            }
//        } else {
//            notFound(response);
//        }
//    }

}
