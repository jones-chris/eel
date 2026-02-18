package io.eel.flow_api_core.exception;

public class ResourceNotFoundException extends Exception {

    private ResourceNotFoundException() {}

    public ResourceNotFoundException(String id) {
        super("Could not find resource with id of " + id);
    }

}
