package io.eel.common.dao;

import io.eel.common.model.StorageLocation;

public interface QueryResultCsvDao {

    StorageLocation save(byte[] queryResultCsvBytes, StorageLocation storageLocation);

}
