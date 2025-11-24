package io.eel.service;

import io.eel.model.WorkbookOutput;
import io.eel.model.proxy.WorkbookProxy;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class WorkbookCalculationEngine {

    private static final String EXCEL_TL_XLSX_RESOURCE_FILE_PATH = "/eel.xlsx";

    private final Map<String, Object[][]> workbookInputs = new HashMap<>();

    public WorkbookCalculationEngine withInput(String worksheetName, Object[][] data) {
        if (this.workbookInputs.get(worksheetName) != null) {
            throw new IllegalArgumentException("Input data for worksheet " + worksheetName + " already exists");
        }

        this.workbookInputs.put(worksheetName, data);
        return this;
    }

    public WorkbookCalculationEngine withZipFileInputs(ZipInputStream zipInputStream) throws IOException {
        final Map<String, Object[][]> inputData = ZipCsvProcessor.unzipAndParseCsvs(zipInputStream);

        inputData.forEach(this::withInput);

        return this;
    }

    public WorkbookOutput runWorkbook() {
        try (
            InputStream inputStream = this.getClass().getResourceAsStream(EXCEL_TL_XLSX_RESOURCE_FILE_PATH);
            WorkbookProxy workbookProxy = new WorkbookProxy(new XSSFWorkbook(inputStream))
        ) {
//            if (this.workbookInputs.isEmpty()) {
//                throw new IllegalArgumentException("No input data provided");
//            }

            // Set inputs.
            // todo: parallelize the copy input steps.
            this.workbookInputs.forEach((sheetName, data) -> {
                Sheet sheet = workbookProxy.getWorkbook().getSheet(sheetName);

                // Get header row.
                Row headerRow = sheet.getRow(0);
                int numOfColumns = new Object[headerRow.getLastCellNum()].length;

                for (int rowIdx = 0; rowIdx < data.length; rowIdx++) {
                    // Get the row or creae it if it does not exist.
                    Row row = sheet.getRow(rowIdx);
                    if (row == null) {
                        row = sheet.createRow(rowIdx);
                    }

                    for (int cellIdx = 0; cellIdx < numOfColumns; cellIdx++) {
                        Cell cell = row.getCell(cellIdx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        Object value = data[rowIdx][cellIdx];

                        if (value == null) {
                            continue;
                        }

                        // todo:  Make this a debug logging statement.
                        System.out.println("Attempting to set cell " + sheetName + ":" + cell.getAddress().formatAsString() + " to " + value);

                        if (value instanceof String) {
                            if (value.toString().isEmpty()) continue;
                            cell.setCellValue((String) value);
                        } else if (value instanceof Integer) {
                            cell.setCellValue((Integer) value);
                        } else if (value instanceof Double) {
                            cell.setCellValue((Double) value);
                        } else if (value instanceof Boolean) {
                            cell.setCellValue((Boolean) value);
                        } else {
                            throw new IllegalArgumentException("Unsupported input data type: " + value.getClass().getName());
                        }
                    }
                }
            });

            // Calculate all formulas.
            workbookProxy.getWorkbook().getCreationHelper().createFormulaEvaluator().evaluateAll();

            // Check exit code.
            // todo:  Throw a checked exception here so we can handle it.
            workbookProxy.assertIsSuccessful();

            return workbookProxy.getOutputs();
        } catch (Throwable t) {
            throw new RuntimeException(t);  // todo:  change this exception.
        }
    }

}
