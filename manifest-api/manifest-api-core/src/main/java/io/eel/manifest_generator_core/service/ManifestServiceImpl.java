package io.eel.manifest_generator_core.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.eel.common.WorkbookValidator;
import io.eel.common.model.Flow;
import io.eel.common.model.WorkbookMetadata;
import io.eel.manifest_generator_core.dao.ManifestDao;
import io.eel.manifest_generator_core.dao.WorkbookDao;
import io.eel.manifest_generator_core.exception.ManifestGenerationException;
import io.eel.model.proxy.WorkbookProxy;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class ManifestServiceImpl implements ManifestService {

    private final static Logger log = Logger.getLogger(ManifestServiceImpl.class.getName());

    private final WorkbookDao workbookDao;

    private final ManifestDao manifestDao;

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public ManifestServiceImpl(WorkbookDao workbookDao, ManifestDao manifestDao) {
        this.workbookDao = workbookDao;
        this.manifestDao = manifestDao;
    }

    @Override
    public WorkbookValidator.Manifest createManifest(
            String bucket,
            String key,
            WorkbookMetadata workbookMetadata
    ) throws ManifestGenerationException {
        try (
                final WorkbookProxy workbookProxy = this.workbookDao.getWorkbook(bucket, key)
                        .orElseThrow(() -> new RuntimeException("Error encountered when retrieving workbook"));
        ) {
            // Call validator to create the manifest.
            WorkbookValidator.Manifest manifest = new WorkbookValidator(
                    workbookProxy.getWorkbook(),
                    workbookMetadata.author(), // todo:  make this constructor take a WorkbookMetadata parameter instead of unpacking the object into separate parameters.
                    workbookMetadata.name(),
                    workbookMetadata.version(),
                    UUID.fromString(key)
            ).assertIsValid()
            .createManifest();

            log.info("Created manifest: " + gson.toJson(manifest));

            // Persist the manifest.
            manifest = this.manifestDao.saveManifest(manifest);

            return manifest;
        } catch (Throwable t) {
            log.severe(t.getMessage()); // todo:  log stacktrace here too.
            throw new ManifestGenerationException(t.getMessage());
        }
    }

    @Override
    public Optional<WorkbookValidator.Manifest> getManifest(UUID id, int version) {
        try {
            final String manifestCanonicalId = Flow.Utils.getCanonicalId(id, version);
            return this.manifestDao.getManifest(manifestCanonicalId);
        } catch (Throwable t) {
            log.severe(t.getMessage()); // todo:  log stack trace here too.
            return Optional.empty();
        }
    }

}
