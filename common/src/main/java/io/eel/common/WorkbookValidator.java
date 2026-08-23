package io.eel.common;

import io.eel.common.exception.WorkbookValidationException;
import io.eel.common.model.Flow;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;

import java.util.*;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.logging.Logger;

public class WorkbookValidator {

    private static final Logger log = Logger.getLogger(WorkbookValidator.class.getName());

    private final List<Sheet> inputSheets = new ArrayList<>();

    private final List<Sheet> outputSheets = new ArrayList<>();

    // Named range metadata collected during validation
    private final List<NamedRangeMetadata> inputNamedRanges = new ArrayList<>();

    private final List<NamedRangeMetadata> outputNamedRanges = new ArrayList<>();

    private final Workbook workbook;

    private final String author;

    private final String name;

    private final int version;

    private final UUID id;

    public WorkbookValidator(Workbook workbook, String author, String name, int version, UUID id, String description) {
        this.workbook = workbook;
        this.author = (author == null) ? System.getProperty("user.name") : author;
        this.name = name;
        this.version = version;
        this.id = id;
    }

    public WorkbookValidator assertIsValid() {
        // Only XSSF (XSSFWorkbook) is supported.
        if (! (workbook instanceof XSSFWorkbook)) {
            throw new WorkbookValidationException("Only XSSF (XSSFWorkbook) workbooks are supported for named ranges");
        }

        // Check that there is a metadata worksheet
        assertMetadataWorksheetIsValid(this.workbook);

        // Check that there is at least one input worksheet
        for (Sheet sheet : this.workbook) {
            String sheetName = sheet.getSheetName().toLowerCase();
            if (sheetName.startsWith(Constants.INPUT_SHEET_PREFIX.toLowerCase())) {
                this.inputSheets.add(sheet);

                assertInputSheetIsValid(sheet);
            } else if (sheetName.startsWith(Constants.OUTPUT_SHEET_PREFIX.toLowerCase())) {
                this.outputSheets.add(sheet);

                assertOutputSheetIsValid(sheet);
            }
        }

        // Collect input and output named ranges.
        for (Name name : this.workbook.getAllNames()) {
            String nameName = name.getNameName().toLowerCase();

            if (nameName.startsWith(Constants.INPUT_SHEET_PREFIX.toLowerCase())) {
                this.inputNamedRanges.add(buildNamedRangeMetadata(name));
            } else if (nameName.startsWith(Constants.OUTPUT_SHEET_PREFIX.toLowerCase())) {
                this.outputNamedRanges.add(buildNamedRangeMetadata(name));
            }
        }

        // Check that there is at least one output worksheet or named range.
        if (this.outputSheets.isEmpty() && this.outputNamedRanges.isEmpty()) {
            throw new WorkbookValidationException(
                    String.format("There should be at least one output sheet or named range.  An output sheet's or named range's name starts with '%s'", Constants.OUTPUT_SHEET_PREFIX)
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

        List<NamedRangeMetadata> inputNamedRangeMetadata = new ArrayList<>(this.inputNamedRanges);
        log.info("inputNamedRangeMetadata: " + inputNamedRangeMetadata);

        List<NamedRangeMetadata> outputNamedRangeMetadata = new ArrayList<>(this.outputNamedRanges);
        log.info("outputNamedRangeMetadata: " + outputNamedRangeMetadata);

        return new Manifest(
                this.id,
                this.author,
                this.name,
                this.version,
                Flow.Utils.getCanonicalId(this.id, this.version),
                inputSheetMetadata,
                outputSheetMetadata,
                inputNamedRangeMetadata,
                outputNamedRangeMetadata
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

            // The following 2 checks apply to all header rows.
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

            // The next check applies only to the first header column in an output sheet.  It must be called "include_in_output"
            // (case-insensitive) and be a boolean.  This acts as a "short circuit" for knowing whether to include/exclude
            // the row from the extracted output after calculations are run.
            if (i == 0 && sheetName.toLowerCase().startsWith(Constants.OUTPUT_SHEET_PREFIX.toLowerCase())) {
                String firstHeaderCellValue = cell.getStringCellValue();
                if (! firstHeaderCellValue.equalsIgnoreCase(Constants.INCLUDE_IN_OUTPUT_COLUMN_NAME)) {
                    throw new WorkbookValidationException(
                            String.format("[%s] The first header column in an output sheet must be called '%s' (case-insensitive), but is called '%s'", sheetName, Constants.INCLUDE_IN_OUTPUT_COLUMN_NAME, firstHeaderCellValue)
                    );
                }
            }
        }
    }

    private static SheetMetadata getSheetMetadata(Sheet sheet) {
        List<ColumnMetadata> columnMetadata = new ArrayList<>();
        Row headerRow = sheet.getRow(0);
        short lastCellNumber = headerRow.getLastCellNum();
        for (int i = 0; i < lastCellNumber; i++) {
            Cell cell = headerRow.getCell(i);
            String cellValue = cell.getStringCellValue();

            if (Constants.IGNORED_COLUMN_NAMES.contains(cellValue.toLowerCase())) {
                continue;
            }

            if (Constants.IGNORED_COLUMN_NAME_PREFIXES.stream().anyMatch(prefix -> cellValue.toLowerCase().startsWith(prefix.toLowerCase()))) {
                continue;
            }

            String type = Constants.BUILT_IN_FORMAT_TO_SQL_TYPE_MAP.get(cell.getCellStyle().getDataFormatString());
            String comment = cell.getCellComment() != null ? cell.getCellComment().getString().getString() : null;

            columnMetadata.add(
                    new ColumnMetadata(cellValue, type, comment)
            );
        }

        List<String> columnNames = columnMetadata.stream()
                .map(ColumnMetadata::name)
                .toList();

        return new SheetMetadata(
                sheet.getSheetName(),
                sheet.getRow(0).getLastCellNum(),
                columnNames,
                columnMetadata
        );
    }

    private String getNameComment(Name name) {
        String nameComment = name.getComment();
        String refersTo = name.getRefersToFormula();

        // The named range must refer to a single cell, not a range of cells.
        AreaReference namedRangeAreaReference = new AreaReference(refersTo, SpreadsheetVersion.EXCEL2007);
        if (! namedRangeAreaReference.isSingleCell()) {
            throw new WorkbookValidationException(
                    String.format("Named range '%s' refers to a range of cells, but must refer to a single cell", name.getNameName())
            );
        }

        // Get data type and comment (if it exists) for the single celled named range.
        CellReference cellReference = namedRangeAreaReference.getFirstCell();
        Cell cell = this.workbook.getSheet(cellReference.getSheetName())
                .getRow(cellReference.getRow())
                .getCell(cellReference.getCol());

        return Optional.ofNullable(cell.getCellComment())
                .map(Comment::getString)
                .map(RichTextString::getString)
                .orElse(nameComment);
    }

    private NamedRangeMetadata buildNamedRangeMetadata(Name name) {
        // Strip the "input_" or "output_" prefix from the named range name to get the canonical named range name.
        String nameName = name.getNameName();
        if (nameName.toLowerCase().startsWith(Constants.INPUT_SHEET_PREFIX)) {
            nameName = nameName.replace(Constants.INPUT_SHEET_PREFIX, "");
        } else if (nameName.toLowerCase().startsWith(Constants.OUTPUT_SHEET_PREFIX)) {
            nameName = nameName.replace(Constants.OUTPUT_SHEET_PREFIX, "");
        } else {
            throw new WorkbookValidationException(
                    String.format("Named range '%s' must start with either '%s' or '%s'", name.getNameName(), Constants.INPUT_SHEET_PREFIX, Constants.OUTPUT_SHEET_PREFIX)
            );
        }

        String refersTo = name.getRefersToFormula();

        // The named range must refer to a single cell, not a range of cells.
        AreaReference namedRangeAreaReference = new AreaReference(refersTo, SpreadsheetVersion.EXCEL2007);
        if (! namedRangeAreaReference.isSingleCell()) {
            throw new WorkbookValidationException(
                    String.format("Named range '%s' refers to a range of cells, but must refer to a single cell", nameName)
            );
        }

        // Get data type and comment (if it exists) for the single celled named range.
        CellReference cellReference = namedRangeAreaReference.getFirstCell();
        Cell cell = this.workbook.getSheet(cellReference.getSheetName())
                .getRow(cellReference.getRow())
                .getCell(cellReference.getCol());
        String dataType = Constants.BUILT_IN_FORMAT_TO_SQL_TYPE_MAP.get(cell.getCellStyle().getDataFormatString());
        String cellComment = this.getNameComment(name);

        return new NamedRangeMetadata(
                nameName,
                cellComment,
                1, // Only single cell named ranges are allowed, so number of rows and columns is 1.
                1,
                refersTo,
                dataType
        );
    }

    public record Manifest(
            UUID flowId,
            String author,
            String name,
            int flowVersion,
            String flowCanonicalId,
            List<SheetMetadata> inputSheetsMetadata,
            List<SheetMetadata> outputSheetsMetadata,
            List<NamedRangeMetadata> inputNamedRanges,
            List<NamedRangeMetadata> outputNamedRanges
    ) {}

    public record SheetMetadata(
            String name,
            int numberOfColumns,
            List<String> columnNames,
            List<ColumnMetadata> columnsMetadata
    ) {}

    public record NamedRangeMetadata(
            String name,
            String comment,
            int numberOfRows,
            int numberOfColumns,
            String address,
            String dataType
    ) {}

    public record ColumnMetadata(
            String name,
            String dataType,
            String comment
    ) {}

}
