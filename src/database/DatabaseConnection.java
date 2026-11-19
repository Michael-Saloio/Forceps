package database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {

    private static final String DB_URL = System.getenv().getOrDefault(
            "FORCEPS_DB_URL", "jdbc:mysql://localhost:3306/forceps");

    private static final String DB_USER = System.getenv().getOrDefault("FORCEPS_DB_USER", "root");

    // XAMPP often uses an empty local root password; set this environment variable otherwise.
    private static final String DB_PASSWORD = System.getenv().getOrDefault("FORCEPS_DB_PASSWORD", "");

    public static Connection getConnection() throws java.sql.SQLException {

        return DriverManager.getConnection(
                DB_URL,
                DB_USER,
                DB_PASSWORD);
    }

}
