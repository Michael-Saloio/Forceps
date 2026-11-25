package views;

import database.DatabaseConnection;
import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Grants a remote worker time-limited access to one patient's records. */
public class GrantRemoteAccessScreen extends JFrame {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String staffId;
    private final long grantedByStaffId;
    private String employeeName;
    private JTextField staffIdField;
    private JTextField accessIdField;
    private JTextField patientIdField;
    private JTextField grantedByField;
    private JTextField createdOnField;
    private JTextField expiryField;
    private JTextField accessCodeField;
    private JButton submitButton;
    private JButton cancelButton;

    public GrantRemoteAccessScreen(String staffId) {
        this(staffId, findActiveAdministratorId());
    }

    public GrantRemoteAccessScreen(String staffId, long grantedByStaffId) {
        this.staffId = staffId == null ? "" : staffId.trim();
        this.grantedByStaffId = grantedByStaffId;
        loadStaffDetails();
        configureWindowProperties();
        initializeComponents();
        setupListeners();
    }

    private void loadStaffDetails() {
        employeeName = "Staff ID " + staffId;
        String sql = "SELECT s.first_name, s.last_name FROM Staff s JOIN Role r ON r.role_id=s.role_id "
                + "WHERE s.staff_id=? AND r.role_name='Remote Worker' AND s.termination_date IS NULL";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setString(1, staffId);
            try (ResultSet r = q.executeQuery()) {
                if (r.next()) employeeName = r.getString("first_name") + " " + r.getString("last_name");
                else showMessage("This staff ID is not an active remote worker.");
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void configureWindowProperties() {
        setTitle("Forceps DBMS - Generate Remote Access Code");
        setSize(480, 440);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
    }

    private void initializeComponents() {
        add(createFormPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Generate Remote Access Code - " + employeeName));
        staffIdField = new JTextField(staffId);
        staffIdField.setEditable(false);
        accessIdField = new JTextField("Generated on submit");
        accessIdField.setEditable(false);
        patientIdField = new JTextField(15);
        grantedByField = new JTextField(grantedByStaffId > 0 ? Long.toString(grantedByStaffId) : "Unavailable");
        grantedByField.setEditable(false);
        createdOnField = new JTextField(LocalDateTime.now().format(TIMESTAMP_FORMAT));
        createdOnField.setEditable(false);
        expiryField = new JTextField(LocalDateTime.now().plusDays(1).withNano(0).format(TIMESTAMP_FORMAT));
        accessCodeField = new JTextField("Generated on submit");
        accessCodeField.setEditable(false);

        formPanel.add(new JLabel("Employee / Staff ID:")); formPanel.add(staffIdField);
        formPanel.add(new JLabel("Access ID:")); formPanel.add(accessIdField);
        formPanel.add(new JLabel("Patient ID:")); formPanel.add(patientIdField);
        formPanel.add(new JLabel("Granted By (staff ID):")); formPanel.add(grantedByField);
        formPanel.add(new JLabel("Created On:")); formPanel.add(createdOnField);
        formPanel.add(new JLabel("Expiry (YYYY-MM-DD HH:MM:SS):")); formPanel.add(expiryField);
        formPanel.add(new JLabel("Access Code:")); formPanel.add(accessCodeField);
        return formPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cancelButton = new JButton("Cancel");
        submitButton = new JButton("Submit");
        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);
        return buttonPanel;
    }

    private void setupListeners() {
        submitButton.addActionListener(e -> handleSubmit());
        cancelButton.addActionListener(e -> dispose());
    }

    private void handleSubmit() {
        long workerId;
        long patientId;
        LocalDateTime expiry;
        try {
            workerId = Long.parseLong(staffId);
            patientId = Long.parseLong(patientIdField.getText().trim());
            expiry = LocalDateTime.parse(expiryField.getText().trim(), TIMESTAMP_FORMAT);
        } catch (NumberFormatException ex) {
            showMessage("Staff ID and Patient ID must be numbers.");
            return;
        } catch (DateTimeParseException ex) {
            showMessage("Expiry must use YYYY-MM-DD HH:MM:SS format.");
            expiryField.requestFocus();
            return;
        }
        if (grantedByStaffId <= 0) { showMessage("Could not identify an active system administrator."); return; }
        if (!expiry.isAfter(LocalDateTime.now())) { showMessage("Expiry must be in the future."); return; }

        String patientName;
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(
                "SELECT first_name, last_name FROM Patient WHERE patient_id=?")) {
            q.setLong(1, patientId);
            try (ResultSet r = q.executeQuery()) {
                if (!r.next()) { showMessage("Patient ID " + patientId + " was not found."); return; }
                patientName = r.getString("first_name") + " " + r.getString("last_name");
            }
        } catch (SQLException ex) { showError(ex); return; }

        String code = generateCode();
        try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL grant_remote_access(?, ?, ?, ?, ?, ?)}")) {
            q.setNull(1, Types.BIGINT); // MySQL assigns the access ID.
            q.setLong(2, workerId);
            q.setLong(3, patientId);
            q.setLong(4, grantedByStaffId);
            q.setString(5, sha256(code));
            q.setTimestamp(6, Timestamp.valueOf(expiry));
            q.execute();
            try (Statement idQuery = c.createStatement(); ResultSet r = idQuery.executeQuery("SELECT LAST_INSERT_ID()")) {
                if (!r.next()) throw new SQLException("MySQL did not return the new access ID.");
                accessIdField.setText(Long.toString(r.getLong(1)));
            }
            accessCodeField.setText(code);
            showMessage("Access granted to " + employeeName + " for patient " + patientName + ".\n"
                    + "Give the worker this access ID and code securely. The code is shown once.\n"
                    + "Access ID: " + accessIdField.getText() + "\nAccess code: " + code);
            dispose();
        } catch (SQLException ex) { showError(ex); }
    }

    private static long findActiveAdministratorId() {
        String sql = "SELECT s.staff_id FROM Staff s JOIN Role r ON r.role_id=s.role_id "
                + "WHERE r.role_name='System Administrator' AND s.termination_date IS NULL ORDER BY s.staff_id LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection(); Statement q = c.createStatement(); ResultSet r = q.executeQuery(sql)) {
            return r.next() ? r.getLong(1) : -1;
        } catch (SQLException ex) { return -1; }
    }

    private static String generateCode() {
        byte[] bytes = new byte[8];
        new SecureRandom().nextBytes(bytes);
        StringBuilder result = new StringBuilder(16);
        for (byte value : bytes) result.append(String.format(java.util.Locale.ROOT, "%02x", value));
        return result.toString();
    }

    private static String sha256(String value) throws SQLException {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte valueByte : bytes) result.append(String.format(java.util.Locale.ROOT, "%02x", valueByte));
            return result.toString();
        } catch (Exception ex) { throw new SQLException("Unable to hash the access code.", ex); }
    }

    private void showMessage(String message) { JOptionPane.showMessageDialog(this, message); }
    private void showError(SQLException ex) { JOptionPane.showMessageDialog(this, "Remote access grant failed: " + ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE); }
}
