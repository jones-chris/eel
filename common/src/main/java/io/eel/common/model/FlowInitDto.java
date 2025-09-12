package io.eel.common.model;

import java.util.UUID;

public record FlowInitDto(
        UUID id,
        int version,
        String transformationStagingUrl
) { }
