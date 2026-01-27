package io.eel.eel_runner_infra_provisioner_core.stacks;

public interface EelStreamingStack {

    void buildEelRuntimePlatform(String canonicalId);

    void buildDeadLetterQueue(String canonicalId);

}
