package io.eel.eel_runner_infra_provisioner_core.stacks;

import java.util.Objects;

public class EelBatchProcessorStackResources {

    private String cronScheduleId;

    private String landingBucketId;

    private String runtimePlatformId;

    private String deadLetterQueueId;

    private String lambdaRoleArn;

    private String lambdaRolePolicyArn;

    private String eventSourceMappingArn;

    private String inputQueueArn;

    public EelBatchProcessorStackResources() {}

    public EelBatchProcessorStackResources(
            String cronScheduleId,
            String landingBucketId,
            String runtimePlatformId,
            String deadLetterQueueId,
            String lambdaRoleArn,
            String lambdaRolePolicyArn,
            String eventSourceMappingArn,
            String inputQueueArn
    ) {
        this.cronScheduleId = cronScheduleId;
        this.landingBucketId = landingBucketId;
        this.runtimePlatformId = runtimePlatformId;
        this.deadLetterQueueId = deadLetterQueueId;
        this.lambdaRoleArn = lambdaRoleArn;
        this.lambdaRolePolicyArn = lambdaRolePolicyArn;
        this.eventSourceMappingArn = eventSourceMappingArn;
        this.inputQueueArn = inputQueueArn;
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

    public String getLambdaRoleArn() {
        return lambdaRoleArn;
    }

    public void setLambdaRoleArn(String lambdaRoleArn) {
        this.lambdaRoleArn = lambdaRoleArn;
    }

    public String getEventSourceMappingArn() {
        return eventSourceMappingArn;
    }

    public void setEventSourceMappingArn(String eventSourceMappingArn) {
        this.eventSourceMappingArn = eventSourceMappingArn;
    }

    public String getInputQueueArn() {
        return inputQueueArn;
    }

    public void setInputQueueArn(String inputQueueArn) {
        this.inputQueueArn = inputQueueArn;
    }

    public String getLambdaRolePolicyArn() {
        return lambdaRolePolicyArn;
    }

    public void setLambdaRolePolicyArn(String lambdaRolePolicyArn) {
        this.lambdaRolePolicyArn = lambdaRolePolicyArn;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EelBatchProcessorStackResources that = (EelBatchProcessorStackResources) o;
        return Objects.equals(cronScheduleId, that.cronScheduleId) && Objects.equals(landingBucketId, that.landingBucketId) && Objects.equals(runtimePlatformId, that.runtimePlatformId) && Objects.equals(deadLetterQueueId, that.deadLetterQueueId) && Objects.equals(lambdaRoleArn, that.lambdaRoleArn) && Objects.equals(lambdaRolePolicyArn, that.lambdaRolePolicyArn) && Objects.equals(eventSourceMappingArn, that.eventSourceMappingArn) && Objects.equals(inputQueueArn, that.inputQueueArn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cronScheduleId, landingBucketId, runtimePlatformId, deadLetterQueueId, lambdaRoleArn, lambdaRolePolicyArn, eventSourceMappingArn, inputQueueArn);
    }

    @Override
    public String toString() {
        return "EelBatchProcessorStackResources{" +
                "cronScheduleId='" + cronScheduleId + '\'' +
                ", landingBucketId='" + landingBucketId + '\'' +
                ", runtimePlatformId='" + runtimePlatformId + '\'' +
                ", deadLetterQueueId='" + deadLetterQueueId + '\'' +
                ", lambdaRoleArn='" + lambdaRoleArn + '\'' +
                ", lambdaRolePolicyArn='" + lambdaRolePolicyArn + '\'' +
                ", eventSourceMappingArn='" + eventSourceMappingArn + '\'' +
                ", inputQueueArn='" + inputQueueArn + '\'' +
                '}';
    }

}
