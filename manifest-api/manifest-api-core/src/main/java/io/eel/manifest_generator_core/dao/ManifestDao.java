package io.eel.manifest_generator_core.dao;

import io.eel.common.WorkbookValidator;

import java.util.UUID;

public interface ManifestDao {

    WorkbookValidator.Manifest getManifest(UUID uuid);

    void saveManifest(WorkbookValidator.Manifest manifest);

    boolean deleteManifest(UUID uuid);

}
