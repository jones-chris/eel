package io.eel.eel_runner_infra_provisioner_core.stacks;

public interface EelBatchProcessorStack {

    void deploy(String canonicalId, String cronExpression, String flowId);

    void buildCronSchedule(String canonicalId, String cronExpression, String flowId);

    void buildLandingBucket(String canonicalId);

//    void buildLandingBucketTrigger(String canonicalId);

    void buildEelRuntimePlatform(String flowId, String canonicalId);

    void buildDeadLetterQueue(String canonicalId);

}
