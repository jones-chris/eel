package io.eel.common.model;

import io.eel.common.Constants;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

public class WorkbookProxy implements AutoCloseable {

    private static final Logger log = Logger.getLogger(WorkbookProxy.class.getName());

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

    /**
     * Returns the underlying workbook instance.
     *
     * @return {@link Workbook}
     */
    public Workbook getWorkbook() {
        return this.workbook;
    }

    /**
     * Asserts that the workbook calculation was successful by checking the value of the first cell in the metadata sheet.
     */
    public void assertIsSuccessful() {
        Cell cell = this.workbook.getSheet(Constants.METADATA).getRow(0).getCell(0);

        int result = (int) cell.getNumericCellValue();
        if (result != 0) {
            throw new RuntimeException("Non-zero result: " + result);
        }
    }

    /**
     * Extracts the output data from the workbook.  The output data is expected to be in sheets that start with "output_"
     * in their name. The first row of the output sheet is expected to contain the column names, and the first column of
     * each row is expected to contain a boolean value indicating whether the row should be included in the output or not.
     *
     * @return {@link WorkbookOutput}
     */
    // todo:  Try reducing the cognitive complexity of this method.
    public WorkbookOutput getOutputs() {
        List<String> headers = new ArrayList<>();
        Map<String, Object[][]> outputData = new HashMap<>();

        for (int sheetIdx = 0; sheetIdx < workbook.getNumberOfSheets(); sheetIdx++) {
            Sheet sheet = workbook.getSheetAt(sheetIdx);
            if (sheet.getSheetName().toLowerCase().startsWith("output")) {
                Set<Integer> columnIndicesToSkip = new HashSet<>();
                for (int cellIdx = 0; cellIdx < sheet.getRow(0).getLastCellNum(); cellIdx++) {
                    Cell cell = sheet.getRow(0).getCell(cellIdx);
                    String cellValue = dataFormatter.formatCellValue(cell);

                    if (Constants.IGNORED_COLUMN_NAMES.contains(cellValue.toLowerCase())) {
                        columnIndicesToSkip.add(cellIdx);
                        continue;
                    }

                    if (Constants.IGNORED_COLUMN_NAME_PREFIXES.stream().anyMatch(prefix -> cellValue.toLowerCase().startsWith(prefix.toLowerCase()))) {
                        columnIndicesToSkip.add(cellIdx);
                    }
                }

                // First, extract the headers from the first row.
                Row headerRow = sheet.getRow(0);
                for (int cellIdx = 0; cellIdx < headerRow.getLastCellNum(); cellIdx++) {
                    headers.add(headerRow.getCell(cellIdx).getStringCellValue());
                }

                // Second, get the non-header data from the output sheet.
                List<Object[]> sheetDataBlock = new ArrayList<>();
                for (int rowIdx = 1; rowIdx < sheet.getLastRowNum() + 1; rowIdx++) {
                    // Get row
                    Row row = sheet.getRow(rowIdx);
                    List<Object> rowDataBlock = new ArrayList<>();

                    // Short circuit logic based on the include_in_output column.  If the value is false, then we skip the
                    // row entirely...
                    boolean includeRowInOutput = (Boolean) Constants.getCellValue(row.getCell(0));
                    if (!includeRowInOutput) {
                        continue;
                    }

                    // ...otherwise extract the data from the row and add it to the output data block.
                    for (int cellIdx = 1; cellIdx < row.getLastCellNum(); cellIdx++) {
                        if (columnIndicesToSkip.contains(cellIdx)) {
                            log.info("Skipping cell at address " + Constants.getCellAddress(row.getCell(cellIdx)) + " because it is in the ignored column names list.");
                            continue;
                        }

                        Object cellValue = Constants.getCellValue(row.getCell(cellIdx));

                        if (cellValue != null && cellValue.toString().trim().isEmpty()) {
                            cellValue = null;
                        }

                        rowDataBlock.add(cellValue);
                    }

                    // We don't add empty rows to the output.
                    if (hasAllEmptyElements(rowDataBlock)) {
                        continue;
                    }

                    sheetDataBlock.add(rowDataBlock.toArray());
                }

                outputData.put(sheet.getSheetName(), sheetDataBlock.toArray(new Object[0][]));
            }
        }

        return new WorkbookOutput(outputData, headers);
    }

    /**
     * Close the underlying workbook resource.  This method is called automatically when using a try-with-resources.
     *
     * @throws IOException if an I/O error occurs while closing the workbook.
     */
    @Override
    public void close() throws IOException {
        this.workbook.close();
    }

    /**
     * Check if all elements in the list are empty based on different data types.  For example, data type values that
     * are considered empty are 1) null for all data types, empty string for the string data type, and zero for the numeric
     * data type).
     *
     * @param list The {@link List<Object>} to check for empty elements.
     * @return true if all elements in the list are empty, false otherwise.
     */
    private static boolean hasAllEmptyElements(List<Object> list) {
        return list.stream()
                .allMatch(
                        obj -> obj == null
                                || (obj instanceof String && ((String) obj).trim().isEmpty())
                                || (obj instanceof Number && ((Number) obj).doubleValue() == 0)
                );
    }
}
