package io.eel.manifest_generator_core.model;

public record ArtifactBuild(
        String flowCanonicalId,
        String bucketName,
        String objectKey,
        String error
) {}
