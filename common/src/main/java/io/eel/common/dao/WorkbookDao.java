package io.eel.common.dao;

import io.eel.common.model.WorkbookProxy;

import java.util.Optional;

public interface WorkbookDao {

    Optional<WorkbookProxy> getWorkbook(String bucket, String key);

    void save(WorkbookProxy workbookProxy, String bucket, String key);

}
