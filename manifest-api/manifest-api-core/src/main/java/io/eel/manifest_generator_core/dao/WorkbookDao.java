package io.eel.manifest_generator_core.dao;

import io.eel.model.proxy.WorkbookProxy;

import java.util.Optional;

public interface WorkbookDao {

    Optional<WorkbookProxy> getWorkbook(String bucket, String key);

}
