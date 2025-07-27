package io.eel.model.proxy;

import io.eel.common.Constants;
import io.eel.model.WorkbookOutput;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkbookProxy implements AutoCloseable {

    private final Workbook workbook;

    private final DataFormatter dataFormatter = new DataFormatter();

    public WorkbookProxy(Workbook workbook) {
        this.workbook = workbook;
    }

    public WorkbookProxy(InputStream inputStream) {
        try {
            this.workbook = new XSSFWorkbook(inputStream);
        } catch (IOException e) {
            // todo:  Add logging here.
            throw new RuntimeException(e);
        }
    }

    public Workbook getWorkbook() {
        return this.workbook;
    }

    public void assertIsSuccessful() {
        Cell cell = this.workbook.getSheet(Constants.METADATA).getRow(0).getCell(0);

        int result = (int) cell.getNumericCellValue();
        if (result != 0) {
            throw new RuntimeException("Non-zero result: " + result);
        }
    }

    public WorkbookOutput getOutputs() {
        Map<Sheet, Object[][]> outputData = new HashMap<>();

        for (int sheetIdx = 0; sheetIdx < workbook.getNumberOfSheets(); sheetIdx++) {
            Sheet sheet = workbook.getSheetAt(sheetIdx);
            if (sheet.getSheetName().toLowerCase().startsWith("output")) {
                // Get data from sheet.  It MUST be a continuous block of text.  The first blank row that is encountered
                // signals the end of the output data block.
                List<Object[]> sheetDataBlock = new ArrayList<>();
                for (int rowIdx = 1; rowIdx < 50; rowIdx++) { // todo:  check why 50 is used here.
                    // Get row
                    Row row = sheet.getRow(rowIdx);
                    List<Object> rowDataBlock = new ArrayList<>();
                    for (int cellIdx = 0; cellIdx < row.getLastCellNum(); cellIdx++) {
                        // Get cell
                        Cell cell = row.getCell(cellIdx);

                        // Get value
                        Object cellValue = Constants.getCellValue(cell);

                        if (cellValue == null || cellValue.toString().trim().isEmpty()) {
                            break;
                        }

                        // Add to output data block.
                        rowDataBlock.add(cellValue);
                    }

                    // Add row data block to sheet data block.
                    if (rowDataBlock.isEmpty() || hasAllEmptyStrings(rowDataBlock)) {
                        break;
                    }
                    sheetDataBlock.add(rowDataBlock.toArray());
                }

                outputData.put(sheet, sheetDataBlock.toArray(new Object[0][]));
            }
        }

        return new WorkbookOutput(outputData);
    }

    @Override
    public void close() throws Exception {
        this.workbook.close();
    }

    private static boolean hasAllEmptyStrings(List<Object> list) {
        return list.stream()
                .allMatch(obj -> obj.toString().isEmpty());
    }
}
