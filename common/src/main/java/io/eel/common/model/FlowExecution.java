package io.eel.common.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FlowExecution(
        UUID flowId,
        OffsetDateTime executionTimeStamp, // This should always be in UTC.
        String workbookBucket,
        String workbookKey
) {}
