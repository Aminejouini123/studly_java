package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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

        try (BufferedReader reader = new BufferedReader(new FileReader(sqlFilePath))) {
            StringBuilder sqlBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip comments and empty lines
                if (line.trim().startsWith("--") || line.trim().isEmpty()) {
                    continue;
                }
                sqlBuilder.append(line);
                if (line.trim().endsWith(";")) {
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
        } catch (IOException e) {
            System.err.println("Error reading SQL file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Run this to initialize the database
        initialize("schema.sql");
    }
}
