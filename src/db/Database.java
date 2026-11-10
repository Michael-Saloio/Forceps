package db;

import java.sql.*;
import java.util.ArrayList;

public class Database {

    // Connect to MySQL
    public static Connection connect() throws SQLException {

        String url = "jdbc:mysql://localhost:3306/forceps";
        String user = "root";
        String password = "CHANGE ME";

        Connection con = DriverManager.getConnection(
                url, user, password);

        return con;
    }

    // Search for a patient
    public static ArrayList<String[]> searchPatient(
            String name, String dob) throws SQLException {

        ArrayList<String[]> patients = new ArrayList<String[]>();

        String sql = "SELECT patient_id, first_name, last_name, " +
                "date_of_birth, phone FROM PATIENT " +
                "WHERE last_name = ? AND date_of_birth = ?";

        try (Connection con = connect();
                PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setDate(2, Date.valueOf(dob));

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    String[] patient = new String[4];

                    patient[0] = rs.getString("patient_id");

                    patient[1] = rs.getString("first_name") + " " +
                            rs.getString("last_name");

                    patient[2] = rs.getString("date_of_birth");

                    patient[3] = rs.getString("phone");

                    patients.add(patient);
                }
            }
        }

        return patients;
    }
}
