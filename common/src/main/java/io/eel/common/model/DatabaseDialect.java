package io.eel.common.model;

/**
 * Defines the supported database dialects and their corresponding JDBC driver class names.
 */
public enum DatabaseDialect {
    MYSQL("com.mysql.cj.jdbc.Driver"),
    POSTGRESQL("org.postgresql.Driver"),
    ORACLE("oracle.jdbc.driver.OracleDriver"),
    SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver"),
    HSQLDB("org.hsqldb.jdbc.JDBCDriver");

    private final String driverClassName;

    DatabaseDialect(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getDriverClassName() {
        return this.driverClassName;
    }
}
