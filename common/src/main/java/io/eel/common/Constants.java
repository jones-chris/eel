package io.eel.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Constants {

    private static final Logger log = LogManager.getLogger(Constants.class.getName());

    public final static String METADATA = "metadata";

    public final static String INPUT_SHEET_PREFIX = "input_";

    public final static String OUTPUT_SHEET_PREFIX = "output_";

    public final static String INCLUDE_IN_OUTPUT_COLUMN_NAME = "include_in_output";

    public final static List<String> IGNORED_COLUMN_NAMES = List.of(Constants.INCLUDE_IN_OUTPUT_COLUMN_NAME);

    public final static List<String> IGNORED_COLUMN_NAME_PREFIXES = List.of("ignore_");

    private static final DataFormatter dataFormatter = new DataFormatter();

    public final static Map<String ,String> BUILT_IN_FORMAT_TO_SQL_TYPE_MAP = new HashMap<>() {{
        put("General", "String"); // ex: General
        put("@", "String"); // ex: @
        put("0", "Integer"); // ex: -1235
        put("0.00", "Decimal"); // ex: -1234.57
        put("#,##0", "Integer"); // ex: 1,235
        put("#,##0.00", "Decimal"); // ex: 1,234.57 (one leading zero)
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
        put("M/D/YYYY H:MM", "DateTime(\"M/D/YYYY H:MM\")"); // ex: 12/1/1999 13:37
        put("MM/DD/YYYY HH:MM AM/PM", "DateTime(\"MM/DD/YYYY HH:MM AM/PM\")"); // ex: 12/01/1999 1:37 PM
        put("MM/DD/YY HH:MM AM/PM", "DateTime(\"MM/DD/YY HH:MM AM/PM\")"); // ex: 12/01/99 1:37 PM
        put("MM/DD/YYYY HH:MM:SS", "DateTime(\"MM/DD/YYYY HH:MM:SS\")"); // ex: 12/01/1999 13:37:46
        put("YYYY-MM-DD HH:MM:SS", "DateTime(\"YYYY-MM-DD HH:MM:SS\")"); // ex: 1999-12-01 13:37:46
        put("YYYY-MM-DD HH:MM:SS.000", "DateTime(\"YYYY-MM-DD HH:MM:SS.000\")"); // ex: 1999-12-01 13:37:46.000
        put("YYYY-MM-DD\"T\"HH:MM:SS", "DateTime(\"YYYY-MM-DD\\\"T\\\"HH:MM:SS\")"); // ex: 1999-12-01T13:37:46 (ISO 8601)
        put("YYYY-MM-DD\"T\"HH:MM:SS.000", "DateTime(\"YYYY-MM-DD\\\"T\\\"HH:MM:SS.000\")"); // ex: 1999-12-01T13:37:46.000
        put("MM/DD/YYYY", "Date(\"MM/DD/YYYY\")"); // ex: 12/01/1999
        put("mmm\\ d\", \"yyyy", "Date(\"mmm\\\\ d\\\", \\\"yyyy\")"); // ex: Dec 1, 1999
        put("boolean", "Boolean"); // ex: TRUE/FALSE
    }};

    private static final Map<String, Function<Cell, Object>> BUILT_IN_TYPE_TRANSFORMERS = Map.of(
            // https://poi.apache.org/apidocs/dev/org/apache/poi/ss/usermodel/BuiltinFormats.html
            "General", cell -> {
                CellType type = cell.getCellType();
                if (type == CellType.FORMULA) {
                    type = cell.getCachedFormulaResultType();
                }
                return switch (type) {
                    case STRING -> cell.getStringCellValue();
                    case NUMERIC -> cell.getNumericCellValue();
                    case BOOLEAN -> cell.getBooleanCellValue();
                    case BLANK -> "";
                    case ERROR -> cell.getErrorCellValue();
                    default -> dataFormatter.formatCellValue(cell);
                };
            },
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
        if (cell == null) {
            return "";
        }

        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }

        switch (cellType) {
            case BLANK:
                return "";
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case NUMERIC:
                String format = cell.getCellStyle().getDataFormatString();
                Function<Cell, Object> transformer = BUILT_IN_TYPE_TRANSFORMERS.get(format);

                if (transformer != null) {
                    try {
                        return transformer.apply(cell);
                    } catch (Throwable t) {
                        log.error("Error encountered when transforming cell at " + Constants.getCellAddress(cell));
                        throw t;
                    }
                }

                return cell.getNumericCellValue();
            case STRING:
                return cell.getStringCellValue();
            case ERROR:
                return cell.getErrorCellValue();
            default:
                return dataFormatter.formatCellValue(cell);
        }
    }

    public static String getCellAddress(Cell cell) {
        return cell.getSheet().getSheetName() + ":" + cell.getAddress().formatAsR1C1String();
    }

}
