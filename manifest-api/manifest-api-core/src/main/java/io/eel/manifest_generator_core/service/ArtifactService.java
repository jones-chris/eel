package io.eel.manifest_generator_core.service;

import io.eel.manifest_generator_core.model.ArtifactBuild;

import java.util.Optional;

public interface ArtifactService {

    String getPresignedUrl(String flowCanonicalId);

    void buildJar(String bucket, String key);

    Optional<ArtifactBuild> getArtifactBuild(String flowCanonicalId);

}
