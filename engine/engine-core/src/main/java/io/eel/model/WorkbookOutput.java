package io.eel.model;

import java.util.Map;

public class WorkbookOutput {

    private final Map<String, Object[][]> output;

    public WorkbookOutput(Map<String, Object[][]> output) {
        this.output = output;
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
