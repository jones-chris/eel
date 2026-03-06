package io.eel.common.model;

public record Query(
        String sql,
        DataSource dataSource
) { }
