package io.eel.manifest_generator_core.dao;

import io.eel.manifest_generator_core.model.ArtifactBuild;

import java.util.Optional;

public interface ArtifactDao {

    Optional<ArtifactBuild> getArtifactBuild(String flowCanonicalId);

    void saveArtifactBuild(ArtifactBuild artifactBuild);

}
