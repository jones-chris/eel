package io.eel.model;

import org.apache.poi.ss.usermodel.Sheet;

import java.util.Map;

public class WorkbookOutput {

    private final Map<Sheet, Object[][]> output;

    public WorkbookOutput(Map<Sheet, Object[][]> output) {
        this.output = output;
    }

    public Map<Sheet, Object[][]> getAllOutputs() {
        return this.output;
    }

    public Object[][] getOutput(Sheet sheet) {
        return this.output.get(sheet);
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
