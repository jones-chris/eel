package io.eel.manifest_generator_core.service;

import io.eel.common.WorkbookValidator;
import io.eel.common.model.WorkbookMetadata;
import io.eel.manifest_generator_core.exception.ManifestGenerationException;

import java.util.Optional;
import java.util.UUID;

public interface ManifestService {

    WorkbookValidator.Manifest createManifest(String bucket, String key, WorkbookMetadata workbookMetadata) throws ManifestGenerationException;

    Optional<WorkbookValidator.Manifest> getManifest(UUID id, int version);

}
