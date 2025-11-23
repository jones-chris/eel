package io.eel.eel_runner_infra_provisioner_core.stacks;

public class EelBatchProcessorStackResources {

    private String cronScheduleId;

    private String landingBucketId;

    private String runtimePlatformId;

    private String deadLetterQueueId;

    public EelBatchProcessorStackResources() {}

    public EelBatchProcessorStackResources(
            String cronScheduleId,
            String landingBucketId,
            String runtimePlatformId,
            String deadLetterQueueId
    ) {
        this.cronScheduleId = cronScheduleId;
        this.landingBucketId = landingBucketId;
        this.runtimePlatformId = runtimePlatformId;
        this.deadLetterQueueId = deadLetterQueueId;
    }

    public String getDeadLetterQueueId() {
        return deadLetterQueueId;
    }

    public void setDeadLetterQueueId(String deadLetterQueueId) {
        this.deadLetterQueueId = deadLetterQueueId;
    }

    public String getRuntimePlatformId() {
        return runtimePlatformId;
    }

    public void setRuntimePlatformId(String runtimePlatformId) {
        this.runtimePlatformId = runtimePlatformId;
    }

    public String getLandingBucketId() {
        return landingBucketId;
    }

    public void setLandingBucketId(String landingBucketId) {
        this.landingBucketId = landingBucketId;
    }

    public String getCronScheduleId() {
        return cronScheduleId;
    }

    public void setCronScheduleId(String cronScheduleId) {
        this.cronScheduleId = cronScheduleId;
    }

}
