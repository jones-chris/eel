package io.eel.database_query_runner;

import java.sql.ResultSet;

@FunctionalInterface
public interface QueryResultHandler {

    Object[][] handle(ResultSet resultSet);

}
