package io.eel.common.dao;

public interface QueryResultCsvDao {

    void save(byte[] queryResultCsvBytes, String bucket, String key);

}
