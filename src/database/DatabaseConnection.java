package database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {

    private static final String DB_URL = setting("FORCEPS_DB_URL", "jdbc:mysql://localhost:3306/forceps");

    private static final String DB_USER = setting("FORCEPS_DB_USER", "root");

    private static final String DB_PASSWORD = setting("FORCEPS_DB_PASSWORD", "");

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static Connection getConnection() throws java.sql.SQLException {

        return DriverManager.getConnection(
                DB_URL,
                DB_USER,
                DB_PASSWORD);
    }

}
