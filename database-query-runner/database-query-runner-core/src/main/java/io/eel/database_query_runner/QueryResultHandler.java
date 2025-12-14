package io.eel.database_query_runner;

import java.sql.ResultSet;

public interface QueryResultHandler {

    void handle(ResultSet resultSet);

}
