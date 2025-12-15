package io.eel.common.model;

public record SqlDataSourceSecret(
    DatabaseDialect databaseDialect,
    String jdbcUrl,
    String username,
    String password
) { }
