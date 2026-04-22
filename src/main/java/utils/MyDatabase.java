package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private final String URL = "jdbc:mysql://localhost:3306/projet_db";
    private final String USER = "root";
    private final String PASSWORD = "";
    private Connection connection;
    private static MyDatabase instance;

    private MyDatabase() {
        connect();
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected to database");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            connection = null;
        }
    }

    public static MyDatabase getInstance() {
        if(instance == null)
            instance = new MyDatabase();
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                System.out.println("Reconnecting to the database...");
                connect();
            }
        } catch (SQLException e) {
            System.err.println("Error reconnecting: " + e.getMessage());
            connect();
        }
        return connection;
    }
}
