package io.eel.common.model;

public record StorageLocation(
        String bucket,
        String key,
        String flowId
) { }