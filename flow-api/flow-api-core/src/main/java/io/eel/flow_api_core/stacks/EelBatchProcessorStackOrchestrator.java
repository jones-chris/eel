package io.eel.flow_api_core.stacks;

import io.eel.common.model.Flow;

public interface EelBatchProcessorStackOrchestrator {

    void deploy(final Flow flow);

    void delete(final Flow flow);

}
