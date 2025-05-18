package io.eel.common;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

public class Constants {

    private static final Map<String, Function<Cell, Object>> BUILT_IN_TYPE_TRANSFORMERS = Map.of(
            // https://poi.apache.org/apidocs/dev/org/apache/poi/ss/usermodel/BuiltinFormats.html
            "General", Cell::getStringCellValue,
            "0", cell -> (int) cell.getNumericCellValue(),
            "0.00", Cell::getNumericCellValue,
            "m/d/yyyy;@", (cell) -> {
                String dateString = cell.getStringCellValue();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
                return LocalDate.parse(dateString, formatter);
            },
            "mm/dd/yyyy", (cell) -> {
                LocalDate localDate = cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
                return formatter.format(localDate);
            }
    );

    public static Object getCellValue(Cell cell) {
        // Check if the cell value is blank or an empty string
        String rawValue = ((XSSFCell) cell).getRawValue();
        if (CellType.BLANK.equals(cell.getCellType()) || rawValue.isEmpty()) {
            return "";
        }

        // Get the relevant cell value transformer and apply it to the cell value.
        var transformer = BUILT_IN_TYPE_TRANSFORMERS.get(cell.getCellStyle().getDataFormatString());
        if (transformer == null) {
            throw new IllegalArgumentException(
                    "Could not find transformer for cell " + cell.getSheet().getSheetName() +
                            ":" + cell.getAddress().formatAsR1C1String() + " with cell style of " +
                            cell.getCellStyle().getDataFormatString()
            );
        }

        try {
            return transformer.apply(cell);
        } catch (Throwable t) {
            System.out.println("Error encountered when transforming cell at " + Constants.getCellAddress(cell));
            throw t;
        }
    }

    public static boolean isCellBlank(Cell cell) {
        try {
            return cell.getStringCellValue().isEmpty();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private static String getCellAddress(Cell cell) {
        return cell.getSheet().getSheetName() + ":" + cell.getAddress().formatAsR1C1String();
    }

}
