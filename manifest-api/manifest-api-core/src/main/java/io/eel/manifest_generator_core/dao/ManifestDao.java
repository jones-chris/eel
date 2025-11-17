package io.eel.manifest_generator_core.dao;

import io.eel.common.WorkbookValidator;
import io.eel.common.WorkbookValidator.Manifest;

import java.util.Optional;
import java.util.UUID;

public interface ManifestDao {

    Optional<Manifest> getManifest(String canonicalId);

    Manifest saveManifest(Manifest manifest);

    boolean deleteManifest(UUID uuid);

}
