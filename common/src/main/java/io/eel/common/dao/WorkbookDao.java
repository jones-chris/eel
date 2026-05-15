package io.eel.common.dao;

import io.eel.common.model.WorkbookProxy;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Optional;

public interface WorkbookDao {

    Optional<WorkbookProxy> getWorkbook(String bucket, String key);

    void save(Workbook workbook, String bucket, String key);

}
