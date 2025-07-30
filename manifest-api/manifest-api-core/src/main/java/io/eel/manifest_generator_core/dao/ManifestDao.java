package io.eel.manifest_generator_core.dao;

import io.eel.common.WorkbookValidator;
import io.eel.common.WorkbookValidator.Manifest;

import java.util.UUID;

public interface ManifestDao {

    Manifest getManifest(UUID uuid);

    Manifest saveManifest(Manifest manifest);

    boolean deleteManifest(UUID uuid);

}
