package io.eel.eel_runner_infra_provisioner_core.stacks.orchestrator;

import io.eel.common.model.Flow;

public interface EelBatchProcessorStackOrchestrator {

    void deploy(final Flow flow);

    void delete(final Flow flow);

}
