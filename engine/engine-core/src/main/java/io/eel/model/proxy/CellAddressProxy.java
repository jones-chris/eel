package io.eel.model.proxy;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;

public class CellAddressProxy {

    private final CellAddress cellAddress;

    private final Sheet sheet;

    public CellAddressProxy(Name name, Sheet sheet) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }

        if (sheet == null) {
            throw new IllegalArgumentException("Sheet cannot be null");
        }

        this.sheet = sheet;
        this.cellAddress = new CellAddress(name.getRefersToFormula());
    }

    public CellAddress getCellAddress() {
        return this.cellAddress;
    }

    public Sheet getSheet() {
        return this.sheet;
    }

    public Object getCellValue() {
        Cell cell = this.sheet.getRow(this.cellAddress.getRow()).getCell(this.cellAddress.getColumn());
        if (cell == null) {
            throw new IllegalStateException("Cell is null");
        }

        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING, BLANK -> cell.getStringCellValue();
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> cell.getCellFormula();
            case ERROR -> cell.getErrorCellValue();
            default -> throw new IllegalStateException("Unexpected cell type: " + cell.getCellType());
        };
    }

}
