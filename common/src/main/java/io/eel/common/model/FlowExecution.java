package io.eel.common.model;

import java.util.UUID;

public record FlowExecution(
        UUID flowId,
        UUID executionId,
        String workbookBucket,
        String workbookKey
) {}
