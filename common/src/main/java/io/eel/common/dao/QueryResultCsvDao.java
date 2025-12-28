package io.eel.common.dao;

import io.eel.common.model.StorageLocation;

import java.io.InputStream;
import java.util.Optional;

public interface QueryResultCsvDao {

    StorageLocation save(byte[] queryResultCsvBytes, StorageLocation storageLocation);

    Optional<InputStream> get(StorageLocation storageLocation);

}
