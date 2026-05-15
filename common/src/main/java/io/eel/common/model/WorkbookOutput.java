package io.eel.common.model;

import java.util.Map;
import java.util.Optional;

public class WorkbookOutput {

    private Optional<StorageLocation> storageLocation = Optional.empty();

    private final Map<String, Object[][]> output;

    public WorkbookOutput(Map<String, Object[][]> output) {
        this.output = output;
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

    public Object[][] getOutput(String sheetName) {
        return this.output.get(sheetName);
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
