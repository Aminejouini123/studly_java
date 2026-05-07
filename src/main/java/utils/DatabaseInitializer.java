package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize(String sqlFilePath) {
        Connection connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            System.err.println("Could not establish connection for database initialization.");
            return;
        }

        try (InputStream in = new java.io.FileInputStream(sqlFilePath)) {
            initialize(connection, in);
        } catch (IOException e) {
            System.err.println("Error reading SQL file: " + e.getMessage());
        }
    }

    public static void initializeFromResource(String resourcePath) {
        String normalized = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        Connection connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            System.err.println("Could not establish connection for database initialization.");
            return;
        }

        try (InputStream in = DatabaseInitializer.class.getResourceAsStream(normalized)) {
            if (in == null) {
                System.err.println("SQL resource not found on classpath: " + normalized);
                return;
            }
            initialize(connection, in);
        } catch (IOException e) {
            System.err.println("Error reading SQL resource: " + e.getMessage());
        }
    }

    /**
     * Best-effort initializer:
     * 1) Try classpath resource (/schema.sql) for packaged runs.
     * 2) Fall back to filesystem ("schema.sql") for IDE runs where working dir is project root.
     */
    public static void initializeDefault() {
        String resourcePath = "/schema.sql";
        try (InputStream in = DatabaseInitializer.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                Connection connection = MyDatabase.getInstance().getConnection();
                if (connection == null) {
                    System.err.println("Could not establish connection for database initialization.");
                    return;
                }
                initialize(connection, in);
                return;
            }
        } catch (IOException ignored) {
            // handled by fallback below
        }

        initialize("schema.sql");
    }

    private static void initialize(Connection connection, InputStream sqlStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(sqlStream, StandardCharsets.UTF_8))) {
            StringBuilder sqlBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // Skip comments and empty lines
                if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                    continue;
                }
                sqlBuilder.append(line);
                if (trimmed.endsWith(";")) {
                    String sql = sqlBuilder.toString();
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(sql);
                    } catch (SQLException e) {
                        System.err.println("Error executing SQL: " + sql);
                        System.err.println("Error message: " + e.getMessage());
                    }
                    sqlBuilder.setLength(0); // Reset for next statement
                }
            }
            System.out.println("Database initialization completed successfully.");
        }
    }

    public static void main(String[] args) {
        // Run this to initialize the database
        initialize("schema.sql");
    }
}
