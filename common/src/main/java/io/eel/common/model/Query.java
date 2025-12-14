package io.eel.common.model;

public record Query(
        String sql,
        SqlDatabaseDataSource sqlDatabaseDataSource
) { }
