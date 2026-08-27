package io.eel.common.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WorkbookOutput {

    private Optional<StorageLocation> storageLocation = Optional.empty();

    private final List<String> headers;

    private final Map<String, Object[][]> output;

    public WorkbookOutput(Map<String, Object[][]> output, List<String> headers) {
        this.output = output;

        if (headers.isEmpty()) {
            throw new IllegalArgumentException("headers cannot be empty");
        }
        this.headers = headers;
    }

    public Optional<StorageLocation> getStorageLocation() {
        return this.storageLocation;
    }

    public void setStorageLocation(StorageLocation storageLocation) {
        if (storageLocation == null) {
            this.storageLocation = Optional.empty();
        } else {
            this.storageLocation = Optional.of(storageLocation);
        }
    }

    public Map<String, Object[][]> getAllOutputs() {
        return this.output;
    }

    public List<String> getHeaders() {
        return this.headers;
    }

    public void logOutput() {
        for (Object[][] output : this.getAllOutputs().values()) {
            for (Object[] row : output) {
                for (Object cell : row) {
                    System.out.println(cell + " ");
                }
                System.out.println();
            }
            System.out.println();
        }
    }

}
