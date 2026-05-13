package io.eel.dao;

import io.eel.common.model.StorageLocation;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Optional;

public interface WorkbookLoggerDao {

    Optional<StorageLocation> log(Workbook workbook, String workbookName);

}
