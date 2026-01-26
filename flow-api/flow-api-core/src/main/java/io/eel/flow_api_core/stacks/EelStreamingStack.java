package io.eel.flow_api_core.stacks;

public interface EelStreamingStack {

    void buildEelRuntimePlatform(String canonicalId);

    void buildDeadLetterQueue(String canonicalId);

}
