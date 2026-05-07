package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MyDatabase {

    // Add conservative timeouts so the JavaFX UI doesn't hang forever if MySQL is up but not responsive.
    // MySQL Connector/J supports these as connection properties (milliseconds).
    private final String URL = "jdbc:mysql://localhost:3306/projet_db"
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=UTC"
            + "&connectTimeout=3000"
            + "&socketTimeout=5000";
    private final String USER = "root";
    private final String PASSWORD = "";
    private Connection connection;
    private static MyDatabase instance;

    private MyDatabase() {
        connect();
    }

    private void connect() {
        try {
            // DriverManager login timeout is in seconds (best-effort; not all drivers honor it perfectly).
            DriverManager.setLoginTimeout(5);

            Properties props = new Properties();
            props.setProperty("user", USER);
            props.setProperty("password", PASSWORD);
            // Keep these duplicated as properties too; some environments prefer one form over the other.
            props.setProperty("connectTimeout", "3000");
            props.setProperty("socketTimeout", "5000");
            props.setProperty("useSSL", "false");
            props.setProperty("allowPublicKeyRetrieval", "true");
            props.setProperty("serverTimezone", "UTC");

            connection = DriverManager.getConnection(URL, props);
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
