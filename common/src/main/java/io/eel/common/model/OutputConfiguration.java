package io.eel.common.model;

public record OutputConfiguration(
        OutputType outputType,
        DirectoryOutputConfiguration directoryOutputConfiguration,
        QueueOutputConfiguration queueOutputConfiguration
) {

    enum OutputType {
        DIRECTORY,
        QUEUE,
        EMAIL
    }

}