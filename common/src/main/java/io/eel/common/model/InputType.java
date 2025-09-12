package io.eel.common.model;

public enum InputType {
    // GENERIC
    SCHEDULED_BATCH,
    KAFKA,
    // AWS
    SQS,
    KINESIS,
    S3,
    // GCP
    PUB_SUB
}