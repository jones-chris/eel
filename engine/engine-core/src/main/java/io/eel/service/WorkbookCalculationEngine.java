package io.eel.service;

import com.google.gson.Gson;
import com.opencsv.CSVReader;
import io.eel.common.WorkbookValidator;
import io.eel.common.model.StorageLocation;
import io.eel.common.model.WorkbookOutput;
import io.eel.common.model.WorkbookProxy;
import io.eel.dao.TempFileWorkbookLoggerDaoImpl;
import io.eel.dao.WorkbookLoggerDao;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Logger;


public class WorkbookCalculationEngine {

    private static final Logger log = Logger.getLogger(WorkbookCalculationEngine.class.getName());

    private static final String EXCEL_TL_XLSX_RESOURCE_FILE_PATH = "/eel.xlsx";

    private static final String MANIFEST_RESOURCE_FILE_PATH = "/manifest.json";

    private static final WorkbookValidator.Manifest manifest;

    private WorkbookProxy workbookProxy;

    private final WorkbookLoggerDao workbookLoggerDao;

    static {
        try (InputStream manifestInputStream = WorkbookCalculationEngine.class.getResourceAsStream(MANIFEST_RESOURCE_FILE_PATH)) {
            assert manifestInputStream != null;

            String manifestJson = new String(manifestInputStream.readAllBytes(), StandardCharsets.UTF_8);

            manifest = new Gson().fromJson(manifestJson, WorkbookValidator.Manifest.class);
        } catch (Throwable e) {
            e.printStackTrace();

            throw new RuntimeException(e);
        }
    }

    public WorkbookCalculationEngine() {
        this.workbookLoggerDao = new TempFileWorkbookLoggerDaoImpl();

        this.loadXlsxFile();
    }

    public WorkbookCalculationEngine(WorkbookLoggerDao workbookLoggerDao) {
        this.workbookLoggerDao = workbookLoggerDao;

        this.loadXlsxFile();
    }

    public WorkbookCalculationEngine withInput(String worksheetName, CSVReader csvReader) {
        try {
            String[] nextLine;

            int rowIdx = 0;

            Sheet sheet = Optional.ofNullable(this.workbookProxy.getWorkbook().getSheet(worksheetName))
                    .orElseThrow(() -> new RuntimeException("Could not find sheet with name " + worksheetName));

            Row headerRow = sheet.getRow(0);
            int numOfColumns = new Object[headerRow.getLastCellNum()].length;

            while ((nextLine = csvReader.readNext()) != null) {
                // Check that num of columns match.
                if (numOfColumns != nextLine.length) {
                    throw new RuntimeException("CSV contains a row at index " + rowIdx + " that is not " + numOfColumns + " items");
                }

                // Get the row or create it if it does not exist.
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    row = sheet.createRow(rowIdx);
                }

                for (int cellIdx = 0; cellIdx < numOfColumns; cellIdx++) {
                    Cell cell = row.getCell(cellIdx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String value = nextLine[cellIdx];

                    if (value == null || value.isBlank()) {
                        continue;
                    }

                    // todo:  Make this a debug logging statement.
//                    log.info("Attempting to set cell {}:{} to {}", worksheetName, cell.getAddress().formatAsString(), value);

                    final CellType cellType = cell.getCellType();
                    if (cellType.equals(CellType.STRING)) {
                        cell.setCellValue(value);
                    } else if (cellType.equals(CellType.NUMERIC)) {
                        cell.setCellValue(Double.parseDouble(value));
                    } else if (cellType.equals(CellType.BOOLEAN)) {
                        cell.setCellValue(Boolean.parseBoolean(value));
                    } else {
                        throw new IllegalArgumentException("Unsupported input data type: " + value.getClass().getName());
                    }
                }

                rowIdx++;
            }

            return this;
        } catch (Throwable t) {
            t.printStackTrace();

            throw new RuntimeException(t);
        }
    }

    public WorkbookProxy getWorkbookProxy() {
        return this.workbookProxy;
    }

    public WorkbookValidator.Manifest getManifest() {
        return manifest;
    }

    public WorkbookOutput runWorkbook() throws Exception {
        WorkbookOutput workbookOutput = null;

        try {
            // Calculate all formulas.
            this.workbookProxy.getWorkbook().getCreationHelper().createFormulaEvaluator().evaluateAll();

            // Check exit code.
            // todo:  Throw a checked exception here so we can handle it.
            this.workbookProxy.assertIsSuccessful();

            workbookOutput = workbookProxy.getOutputs();
        } catch (Throwable t) {
            throw new RuntimeException(t);  // todo:  change this exception.
        } finally {
            // Write workbook to disk so that the user can open it for debugging, if needed.
            Optional<StorageLocation> logStorageLocation = this.workbookLoggerDao.log(workbookProxy.getWorkbook(), manifest.name());
            if (workbookOutput != null && logStorageLocation.isPresent()) {
                log.info("Adding storage location to response");
                workbookOutput.setStorageLocation(logStorageLocation.get());
            }

            this.workbookProxy.close();
        }

        return workbookOutput;
    }

    private void loadXlsxFile() {
        try (InputStream inputStream = WorkbookCalculationEngine.class.getResourceAsStream(EXCEL_TL_XLSX_RESOURCE_FILE_PATH)) {
            this.workbookProxy = new WorkbookProxy(new XSSFWorkbook(inputStream));
        } catch (IOException e) {
            e.printStackTrace();

            throw new RuntimeException(e);
        }
    }

}
