package io.eel.common;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Constants {

    public final static String METADATA = "metadata";

    public final static String INPUT_SHEET_PREFIX = "input_";

    public final static String OUTPUT_SHEET_PREFIX = "output_";

    public final static String INCLUDE_IN_OUTPUT_COLUMN_NAME = "include_in_output";

    public final static Map<String ,String> BUILT_IN_FORMAT_TO_SQL_TYPE_MAP = new HashMap<>() {{
        put("General", "String"); // ex: General
        put("@", "String"); // ex: @
        put("0", "Integer"); // ex: -1235
        put("0.00", "Decimal"); // ex: -1234.57
        put("#,##0", "Integer"); // ex: 1,235
        put("#,##0.00", "Decimal"); // ex: 1,234.57 (one leading zero
        put("#,###.00", "Decimal"); // ex: 1,234.57 (no leading zero)
        put("#,##0_);(#,##0)", "Integer"); // ex: (1,235) (one leading zero)
        put("#,##0.00_);(#,##0.00)", "Decimal"); // ex: (1,234.57) (no leading zero)
        put("[NatNum12 cardinal]0", "String"); // ex: one hundred
        put("[NatNum12 capitalize cardinal]0", "String"); // ex: One hundred
        put("[NatNum12 title cardinal]0", "String"); // ex: One Hundred
        put("[NatNum12 upper cardinal]0", "String"); // ex: ONE HUNDRED
        put("#,##0_);[RED](#,##0)", "Integer"); // ex: (1,235) (one leading zero) (red)
        put("#,##0.00_);[RED](#,##0.00)", "Decimal"); // ex: (1,234.57) (no leading zero) (red)
        put("_(* #,##0.00_);_(* (#,##0.00);_(* \"-\"??_);_(@_)", "Decimal"); // ex: (1234.57) (black)
//        put("m/d/yyyy;@", "Date");
        put("M/D/YYYY H:MM", "DateTime"); // ex: 12/1/1999 13:37
        put("MM/DD/YYYY HH:MM AM/PM", "DateTime"); // ex: 12/01/1999 1:37 PM
        put("MM/DD/YY HH:MM AM/PM", "DateTime"); // ex: 12/01/99 1:37 PM
        put("MM/DD/YYYY HH:MM:SS", "DateTime"); // ex: 12/01/1999 13:37:46
        put("YYYY-MM-DD HH:MM:SS", "DateTime"); // ex: 1999-12-01 13:37:46
        put("YYYY-MM-DD HH:MM:SS.000", "DateTime"); // ex: 1999-12-01 13:37:46.000
        put("YYYY-MM-DD\"T\"HH:MM:SS", "DateTime"); // ex: 1999-12-01T13:37:46 (ISO 8601)
        put("YYYY-MM-DD\"T\"HH:MM:SS.000", "DateTime"); // ex: 1999-12-01T13:37:46.000
        put("MM/DD/YYYY", "Date"); // ex: 12/01/1999
        put("boolean", "Boolean"); // ex: TRUE/FALSE
    }};

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

    private static String getCellAddress(Cell cell) {
        return cell.getSheet().getSheetName() + ":" + cell.getAddress().formatAsR1C1String();
    }

}
