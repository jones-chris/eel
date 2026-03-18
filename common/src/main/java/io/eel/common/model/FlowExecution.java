package io.eel.common.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FlowExecution(
        UUID flowId,
        OffsetDateTime executionTimeStamp, // This should always be in UTC.
        String workbookBucket,
        String workbookKey,
        FlowExecutionStatus status
) {

    public static FlowExecution completedFlowExecution(FlowExecution flowExecution) {
        return new FlowExecution(
                flowExecution.flowId,
                flowExecution.executionTimeStamp,
                flowExecution.workbookBucket,
                flowExecution.workbookKey,
                FlowExecutionStatus.COMPLETED
        );
    }

    public static FlowExecution failedFlowExecution(FlowExecution flowExecution) {
        return new FlowExecution(
                flowExecution.flowId,
                flowExecution.executionTimeStamp,
                flowExecution.workbookBucket,
                flowExecution.workbookKey,
                FlowExecutionStatus.FAILED
        );
    }

    public enum FlowExecutionStatus {

        RUNNING,
        COMPLETED,
        FAILED

    }

}
