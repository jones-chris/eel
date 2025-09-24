package io.eel.common.model;

import java.util.UUID;

// Flow-related configuration records.

public class Flow{

    private UUID id = UUID.randomUUID();

    private int version = 0;  // This makes the Flow immutable.

    private String author;

    private InputType inputType;

    private ScheduledBatchConfiguration scheduledBatchConfiguration;

    private StreamingConfiguration streamingConfiguration;

    private OutputConfiguration outputConfiguration;

    private boolean isFinalized;

    private Flow() {}

    private Flow(
            String author,
            InputType inputType,
            ScheduledBatchConfiguration scheduledBatchConfiguration,
            StreamingConfiguration streamingConfiguration,
            OutputConfiguration outputConfiguration,
            boolean isFinalized
    ) {
        this.author = author;
        this.inputType = inputType;
        this.scheduledBatchConfiguration = scheduledBatchConfiguration;
        this.streamingConfiguration = streamingConfiguration;
        this.outputConfiguration = outputConfiguration;
        this.isFinalized = isFinalized;
    }

    /**
     * This constructor is intended to be used when instantiating a {@link Flow} from a database record.  All fields are
     * exposed in this constructor.
     *
     * @param id
     * @param version
     * @param author
     * @param inputType
     * @param scheduledBatchConfiguration
     * @param streamingConfiguration
     * @param outputConfiguration
     * @param isFinalized
     */
    public Flow(
        UUID id,
        int version,
        String author,
        InputType inputType,
        ScheduledBatchConfiguration scheduledBatchConfiguration,
        StreamingConfiguration streamingConfiguration,
        OutputConfiguration outputConfiguration,
        boolean isFinalized
    ) {
        this.id = id;
        this.version = version;
        this.author = author;
        this.inputType = inputType;
        this.scheduledBatchConfiguration = scheduledBatchConfiguration;
        this.streamingConfiguration = streamingConfiguration;
        this.outputConfiguration = outputConfiguration;
        this.isFinalized = isFinalized;
    }

    public static Flow create(String author) {
        return new Flow(author, null, null, null, null, false);
    }

    public Flow increment() {
        this.version = this.version + 1;
        return this;
    }

    public UUID getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }

    public InputType getInputType() {
        return inputType;
    }

    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }

    public ScheduledBatchConfiguration getScheduledBatchConfiguration() {
        return scheduledBatchConfiguration;
    }

    public void setScheduledBatchConfiguration(ScheduledBatchConfiguration scheduledBatchConfiguration) {
        this.scheduledBatchConfiguration = scheduledBatchConfiguration;
    }

    public StreamingConfiguration getStreamingConfiguration() {
        return streamingConfiguration;
    }

    public void setStreamingConfiguration(StreamingConfiguration streamingConfiguration) {
        this.streamingConfiguration = streamingConfiguration;
    }

    public OutputConfiguration getOutputConfiguration() {
        return outputConfiguration;
    }

    public void setOutputConfiguration(OutputConfiguration outputConfiguration) {
        this.outputConfiguration = outputConfiguration;
    }

    public boolean isFinalized() {
        return isFinalized;
    }

    public void setFinalized(boolean finalized) {
        isFinalized = finalized;
    }
}

// Output configuration records.

record DirectoryOutputConfiguration(
        String directory
) { }

record QueueOutputConfiguration(
        String queueUrl
) { }
