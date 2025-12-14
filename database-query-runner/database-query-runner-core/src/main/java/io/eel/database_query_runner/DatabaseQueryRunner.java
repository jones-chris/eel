package io.eel.database_query_runner;

import io.eel.common.model.DatabaseDialect;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;


/**
 * A utility class to dynamically load a JDBC driver, establish a connection,
 * and execute a single SQL query based on provided parameters.
 */
public class DatabaseQueryRunner {

    private final static Logger log = Logger.getLogger(DatabaseQueryRunner.class.getName());

    private final String jdbcURL;

    private final String username;

    private final String password;

    private final String sqlQuery;

    private final DatabaseDialect dialect;

    /**
     * Constructor to initialize the database connection details and the SQL to execute.
     *
     * @param dialect The database type (e.g., MYSQL, POSTGRESQL).
     * @param jdbcURL The JDBC connection string.
     * @param username The database username.
     * @param password The database password.
     * @param sqlQuery The SQL statement to be executed.
     */
    public DatabaseQueryRunner(DatabaseDialect dialect, String jdbcURL, String username, String password, String sqlQuery) {
        this.dialect = dialect;
        this.jdbcURL = jdbcURL;
        this.username = username;
        this.password = password;
        this.sqlQuery = sqlQuery;
    }

    /**
     * Dynamically loads the JDBC driver for the specified dialect.
     *
     * @throws RuntimeException if the driver class cannot be found.
     */
    private void loadDriver() {
        String driverClassName = this.dialect.getDriverClassName();

        log.info("Attempting to load JDBC driver: " + driverClassName);

        try {
            // Dynamically loads the driver class. This is often not strictly required
            // for modern JDBC 4.0+ drivers, but is good practice for compatibility.
            Class.forName(driverClassName);
            log.info("Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            final String message = "Could not find the JDBC driver class for " + dialect + ": " + driverClassName + ". " +
                    "Ensure the necessary JAR file is in your classpath: " + e;

            log.severe(message);

            throw new RuntimeException(message);
        }
    }

    /**
     * Executes the configured SQL query against the database.
     * This method handles establishing the connection, executing the query,
     * and closing all resources (Connection, Statement, ResultSet).
     */
    public void execute(QueryResultHandler queryResultHandler) {
        // 1. Load the JDBC Driver
        loadDriver();

        // 2. Execute the Query and Manage Resources
        try (
                Connection connection = DriverManager.getConnection(this.jdbcURL, this.username, this.password);
                Statement statement = connection.createStatement();
        ) {
            log.info("Connection established successfully.");
            log.info("Executing SQL: " + this.sqlQuery);

            try (ResultSet resultSet = statement.executeQuery(this.sqlQuery)) {
                queryResultHandler.handle(resultSet);
            }
        } catch (Throwable t) {
            log.severe("Database error occurred while executing SQL: " + t.getMessage());
            throw new RuntimeException(t);
        }
    }
}
