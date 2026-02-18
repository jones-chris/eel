package io.eel.flow_api_core.exception;

public class ImmutableFlowException extends Exception {

    private ImmutableFlowException() {}

    public ImmutableFlowException(String canonicalId) {
        super("Cannot change flow with canonical id of " + canonicalId + " because it is finalized/immutable.  You need to increment the flow instead.");
    }

}
