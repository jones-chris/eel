package io.eel.manifest_generator_core.dao;

import io.eel.common.WorkbookValidator;

import java.util.UUID;

public interface ManifestDao {

    WorkbookValidator.Manifest getManifest(UUID uuid);

    WorkbookValidator.Manifest createManifest(WorkbookValidator.Manifest manifest);

}
