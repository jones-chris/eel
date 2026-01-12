package io.eel.eel_runner_infra_provisioner_aws_lambda.orchestrator;

public interface EelBatchProcessorStackOrchestrator {

    void deploy(final String flowId, final int version);

    void delete(final String flowId, final int version);

}
