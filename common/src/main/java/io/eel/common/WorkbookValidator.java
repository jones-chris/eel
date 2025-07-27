package io.eel.common;

import io.eel.common.exception.WorkbookValidationException;
import org.apache.poi.ss.usermodel.*;

import java.util.*;
import java.util.logging.Logger;

public class WorkbookValidator {

    private static final Logger log = Logger.getLogger(WorkbookValidator.class.getName());

    private final List<Sheet> inputSheets = new ArrayList<>();

    private final List<Sheet> outputSheets = new ArrayList<>();

    private final Workbook workbook;

    private final String author;

    private final String name;

    private final int version;

    public WorkbookValidator(Workbook workbook, String author, String name, int version) {
        this.workbook = workbook;
        this.author = (author == null) ? System.getProperty("user.name") : author;
        this.name = name;
        this.version = version;
    }

    public WorkbookValidator assertIsValid() {
        // Check that there is a metadata worksheet
        assertMetadataWorksheetIsValid(workbook);

        // Check that there is at least one input worksheet
        for (Sheet sheet : workbook) {
            String sheetName = sheet.getSheetName().toLowerCase();
            if (sheetName.startsWith(Constants.INPUT_SHEET_PREFIX.toLowerCase())) {
                this.inputSheets.add(sheet);

                assertInputSheetIsValid(sheet);
            } else if (sheetName.startsWith(Constants.OUTPUT_SHEET_PREFIX.toLowerCase())) {
                this.outputSheets.add(sheet);

                assertOutputSheetIsValid(sheet);
            }
            else {
                // transformer/calculation sheet validation.
            }
        }

        // If no input sheets exist, then throw an exception.
        if (this.inputSheets.isEmpty()) {
            throw new WorkbookValidationException(
                    String.format("There should be at least one input sheet.  An input sheet's name starts with '%s'", Constants.INPUT_SHEET_PREFIX)
            );
        }

        // Check that there is at least one output worksheet
        if (this.outputSheets.isEmpty()) {
            throw new WorkbookValidationException(
                    String.format("There should be at least one output sheet.  An output sheet's name starts with '%s'", Constants.OUTPUT_SHEET_PREFIX)
            );
        }

        return this;
    }

    public Manifest createManifest() {
        List<SheetMetadata> inputSheetMetadata = this.inputSheets.stream()
                .map(WorkbookValidator::getSheetMetadata)
                .toList();
        log.info("inputSheetMetadata: " + inputSheetMetadata);

        List<SheetMetadata> outputSheetMetadata = this.outputSheets.stream()
                .map(WorkbookValidator::getSheetMetadata)
                .toList();
        log.info("outputSheetMetadata: " + outputSheetMetadata);

        return new Manifest(
                UUID.randomUUID(),
                this.author,
                this.name,
                this.version,
                inputSheetMetadata,
                outputSheetMetadata
        );
    }

    private static void assertMetadataWorksheetIsValid(Workbook workbook) {
        // Check that metadata worksheet exists.
        Optional.ofNullable(workbook.getSheet(Constants.METADATA))
                .ifPresentOrElse(
                        (sheet) -> {
                            Cell a1 = sheet.getRow(0).getCell(0);

                            // Check that metadata!A1 has a formula.
                            String formula = a1.getCellFormula();
                            if (formula == null) {
                                throw new WorkbookValidationException("metadata!A1 must have a formula that returns an integer");
                            }

                            // Check that metadata!A1's formula returns an integer.
//                            if (! a1.getCellType().equals(CellType.NUMERIC)) {
//                                throw new WorkbookValidationException("metadata!A1 must be formatted as a NUMERIC type");
//                            }
                        },
                        () -> {
                            throw new WorkbookValidationException(
                                    String.format("Did not find a sheet named '%s'", Constants.METADATA)
                            );
                        }
                );
    }

    private static void assertInputSheetIsValid(Sheet sheet) {
        String sheetName = sheet.getSheetName();

        // 1. The input sheet's first row must be row A/0.
        int firstRowNum = sheet.getFirstRowNum();
        assertFirstRowIsRowA(firstRowNum, sheetName);

        // 2. The input sheet's first cell must be A1.
        short firstCellNum = sheet.getRow(sheet.getFirstRowNum()).getFirstCellNum();
        assertFirstCellIsA1(firstCellNum, sheetName);

        // 3. The headers should be a contiguous row of cells.
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        assertHeaderRowIsValid(headerRow, sheetName);
    }

    private static void assertOutputSheetIsValid(Sheet sheet) {
        String sheetName = sheet.getSheetName();

        // 1. The sheet's first row must be row A/0.
        int firstRowNum = sheet.getFirstRowNum();
        assertFirstRowIsRowA(firstRowNum, sheetName);

        // 2. The sheet's first cell must be A1.
        short firstCellNum = sheet.getRow(sheet.getFirstRowNum()).getFirstCellNum();
        assertFirstCellIsA1(firstCellNum, sheetName);

        // 3. The headers should be a contiguous row of cells.
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        assertHeaderRowIsValid(headerRow, sheetName);
    }

    private static void assertFirstRowIsRowA(int firstRowNumber, String sheetName) {
        if (firstRowNumber != 0) {
            throw new WorkbookValidationException(
                    String.format("[%s] Data must start in row 1 of each input sheet, but starts in %d", sheetName, firstRowNumber)
            );
        }
    }

    private static void assertFirstCellIsA1(int firstCellNumber, String sheetName) {
        if (firstCellNumber != 0) {
            throw new WorkbookValidationException(
                    String.format("[%s] Data must start in cell A1 for each input sheet, but starts in %d", sheetName, firstCellNumber)
            );
        }
    }

    private static void assertHeaderRowIsValid(Row headerRow, String sheetName) {
        short lastCellNum = headerRow.getLastCellNum();
        for (int i = 0; i < lastCellNum; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) {
                throw new WorkbookValidationException(
                        String.format("[%s] Each cell in the header row must not be blank or null, but cell at index %d is null", sheetName, i)
                );
            }

            if (CellType.BLANK.equals(cell.getCellType()) || cell.getStringCellValue().isEmpty() || cell.getStringCellValue().isBlank()) {
                throw new WorkbookValidationException(
                        String.format("[%s] Each cell in the header row must not be blank, but cell %s is blank", sheetName, cell.getAddress().formatAsString())
                );
            }
        }
    }

    private static SheetMetadata getSheetMetadata(Sheet sheet) {
        Map<String, Object> columnDataTypes = new LinkedHashMap<>(); // Using a LinkedHashMap to maintain insertion order.
        Row headerRow = sheet.getRow(0);
        short lastCellNumber = headerRow.getLastCellNum();
        for (int i = 0; i < lastCellNumber; i++) {
            Cell cell = headerRow.getCell(i);
            String cellValue = cell.getStringCellValue();
            String type = Constants.BUILT_IN_FORMAT_TO_SQL_TYPE_MAP.get(cell.getCellStyle().getDataFormatString());

            columnDataTypes.put(cellValue, type);
        }


        return new SheetMetadata(
                sheet.getSheetName(),
                sheet.getRow(0).getLastCellNum(),
                new ArrayList<>(columnDataTypes.keySet()),
                columnDataTypes
        );
    }

    public record Manifest(
            UUID id,
            String author,
            String name,
            int version,
            List<SheetMetadata> inputSheetsMetadata,
            List<SheetMetadata> outputSheetsMetadata
    ) {}

    record SheetMetadata(
            String name,
            int numberOfColumns,
            List<String> columnNames,
            Map<String, Object> columnDataTypes
    ) {}

}
