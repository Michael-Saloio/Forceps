package views;

import database.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** System administrator workflows backed by the project's stored procedures. */
public class SystemAdminPanel extends JPanel {
    private final long administratorStaffId;
    private final JTabbedPane tabs = new JTabbedPane();
    private final DefaultTableModel staffModel = nonEditableModel("Staff ID", "Role", "Name", "Email", "Account status");
    private final JTable staffTable = new JTable(staffModel);
    private final DefaultTableModel accessModel = nonEditableModel("Access ID", "Remote worker", "Patient", "Expires");
    private final JTable accessTable = new JTable(accessModel);
    private JComboBox<StaffOption> workerChoice;
    private JComboBox<PatientOption> patientChoice;
    private JTextField expiryField;

    public SystemAdminPanel(long administratorStaffId) {
        this.administratorStaffId = administratorStaffId;
        setLayout(new BorderLayout());
        tabs.addTab("Onboard", onboardingPanel());
        tabs.addTab("Offboard", offboardingPanel());
        tabs.addTab("Grant remote access", grantPanel());
        tabs.addTab("Revoke remote access", revocationPanel());
        add(tabs, BorderLayout.CENTER);
    }

    public void showTab(String name) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (tabs.getTitleAt(i).equalsIgnoreCase(name)) { tabs.setSelectedIndex(i); return; }
        }
    }

    private JPanel onboardingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        JComboBox<String> role = new JComboBox<>(new String[]{"Doctor", "Nurse", "Receptionist", "System Administrator", "Remote Worker"});
        JTextField first = new JTextField(20), last = new JTextField(20), phone = new JTextField(20), email = new JTextField(24);
        JTextField department = new JTextField(20), hireDate = new JTextField(LocalDate.now().toString(), 14);
        JTextField username = new JTextField(20), license = new JTextField(20), specialty = new JTextField(20);
        JPasswordField password = new JPasswordField(20);
        form.add(new JLabel("Role")); form.add(role);
        form.add(new JLabel("First name *")); form.add(first);
        form.add(new JLabel("Last name *")); form.add(last);
        form.add(new JLabel("Phone")); form.add(phone);
        form.add(new JLabel("Email *")); form.add(email);
        form.add(new JLabel("Department")); form.add(department);
        form.add(new JLabel("Hire date (YYYY-MM-DD) *")); form.add(hireDate);
        form.add(new JLabel("Username *")); form.add(username);
        form.add(new JLabel("Temporary password *")); form.add(password);
        form.add(new JLabel("License number (doctor/nurse)")); form.add(license);
        form.add(new JLabel("Specialty (doctor)")); form.add(specialty);
        JButton create = new JButton("Create account");
        form.add(new JLabel("IDs are assigned automatically")); form.add(create);
        panel.add(new JScrollPane(form));
        create.addActionListener(e -> createStaff((String) role.getSelectedItem(), first, last, phone, email,
                department, hireDate, username, password, license, specialty));
        return panel;
    }

    private void createStaff(String role, JTextField first, JTextField last, JTextField phone, JTextField email,
            JTextField department, JTextField hire, JTextField username, JPasswordField password,
            JTextField license, JTextField specialty) {
        if (first.getText().trim().isEmpty() || last.getText().trim().isEmpty() || email.getText().trim().isEmpty()
                || username.getText().trim().isEmpty() || password.getPassword().length == 0) {
            showMessage("Fill in all required fields."); return;
        }
        if (!email.getText().contains("@")) { showMessage("Enter a valid email address."); return; }
        try {
            Date hireDate = Date.valueOf(LocalDate.parse(hire.getText().trim()));
            String passwordHash = hash(new String(password.getPassword()));
            String procedure;
            int count;
            if ("Doctor".equals(role)) { procedure = "add_doctor"; count = 15; }
            else if ("Nurse".equals(role)) { procedure = "add_nurse"; count = 14; }
            else if ("Receptionist".equals(role)) { procedure = "add_receptionist"; count = 13; }
            else if ("Remote Worker".equals(role)) { procedure = "add_remote_worker"; count = 13; }
            else { procedure = "add_system_admin"; count = 13; }
            String placeholders = String.join(",", java.util.Collections.nCopies(count, "?"));
            try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL " + procedure + "(" + placeholders + ")}")) {
                q.setNull(1, Types.BIGINT); q.setNull(2, Types.BIGINT); q.setLong(3, administratorStaffId);
                q.setString(4, first.getText().trim()); q.setString(5, last.getText().trim());
                setOptional(q, 6, phone.getText()); q.setString(7, email.getText().trim());
                setOptional(q, 8, department.getText()); q.setDate(9, hireDate); q.setNull(10, Types.DATE);
                if ("Doctor".equals(role)) {
                    if (license.getText().trim().isEmpty() || specialty.getText().trim().isEmpty()) { showMessage("Doctor license and specialty are required."); return; }
                    q.setString(11, license.getText().trim()); q.setString(12, specialty.getText().trim());
                    q.setString(13, username.getText().trim()); q.setString(14, passwordHash); q.setString(15, "ACTIVE");
                } else if ("Nurse".equals(role)) {
                    if (license.getText().trim().isEmpty()) { showMessage("Nurse license number is required."); return; }
                    q.setString(11, license.getText().trim()); q.setString(12, username.getText().trim());
                    q.setString(13, passwordHash); q.setString(14, "ACTIVE");
                } else {
                    q.setString(11, username.getText().trim()); q.setString(12, passwordHash); q.setString(13, "ACTIVE");
                }
                q.execute();
                long id;
                try (PreparedStatement find = c.prepareStatement("SELECT staff_id FROM Staff WHERE email=?")) {
                    find.setString(1, email.getText().trim());
                    try (ResultSet r = find.executeQuery()) { if (!r.next()) throw new SQLException("Created staff member could not be found."); id = r.getLong(1); }
                }
                showMessage(role + " account created. Staff ID: " + id + ". Share the temporary password securely.");
                password.setText("");
            }
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            showMessage("Enter hire date as YYYY-MM-DD.");
        } catch (SQLException ex) { showError(ex); }
    }

    private JPanel offboardingPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField search = new JTextField(24);
        JButton find = new JButton("Search staff ID, first name, last name, or email");
        JButton offboard = new JButton("Offboard selected");
        controls.add(new JLabel("Search:")); controls.add(search); controls.add(find); controls.add(offboard);
        staffTable.setAutoCreateRowSorter(true); staffTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(controls, BorderLayout.NORTH); panel.add(new JScrollPane(staffTable), BorderLayout.CENTER);
        find.addActionListener(e -> searchStaff(search.getText().trim()));
        search.addActionListener(e -> searchStaff(search.getText().trim()));
        offboard.addActionListener(e -> handleSubmit());
        return panel;
    }

    private void searchStaff(String term) {
        if (term.isEmpty()) { showMessage("Enter a staff ID, name, or email."); return; }
        staffModel.setRowCount(0);
        String sql = "SELECT s.staff_id, r.role_name, CONCAT(s.first_name,' ',s.last_name) AS full_name, s.email, ua.account_status " +
                "FROM Staff s JOIN Role r ON r.role_id=s.role_id LEFT JOIN User_account ua ON ua.staff_id=s.staff_id " +
                "WHERE CAST(s.staff_id AS CHAR)=? OR LOWER(s.first_name) LIKE LOWER(?) OR LOWER(s.last_name) LIKE LOWER(?) OR LOWER(s.email) LIKE LOWER(?) " +
                "ORDER BY s.last_name,s.first_name";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setString(1, term); q.setString(2, "%" + term + "%"); q.setString(3, "%" + term + "%"); q.setString(4, "%" + term + "%");
            try (ResultSet r = q.executeQuery()) {
                while (r.next()) staffModel.addRow(new Object[]{r.getLong("staff_id"), r.getString("role_name"),
                        r.getString("full_name"), r.getString("email"), r.getString("account_status")});
            }
            if (staffModel.getRowCount() == 0) showMessage("No staff found.");
        } catch (SQLException ex) { showError(ex); }
    }

    private void handleSubmit() {
        int selected = staffTable.getSelectedRow();
        if (selected < 0) { showMessage("Select an employee first."); return; }
        int row = staffTable.convertRowIndexToModel(selected);
        long staffId = ((Number) staffModel.getValueAt(row, 0)).longValue();
        String name = String.valueOf(staffModel.getValueAt(row, 2));
        JTextArea reason = new JTextArea(4, 32);
        if (JOptionPane.showConfirmDialog(this, new JScrollPane(reason), "Reason for offboarding " + name, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String terminationReason = reason.getText().trim();
        if (terminationReason.isEmpty()) { showMessage("A termination reason is required."); return; }
        if (terminationReason.length() > 500) { showMessage("The termination reason must be 500 characters or fewer."); return; }
        try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL offboard_employee(?, ?, ?)}")) {
            q.setLong(1, staffId); q.setDate(2, Date.valueOf(LocalDate.now())); q.setString(3, terminationReason);
            q.execute();
            showMessage(name + " was offboarded. The staff and account history were retained.");
            searchStaff(Long.toString(staffId));
        } catch (SQLException ex) { showError(ex); }
    }

    private JPanel grantPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        workerChoice = new JComboBox<>(); patientChoice = new JComboBox<>();
        expiryField = new JTextField(LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString(), 20);
        JButton reload = new JButton("Reload people and patients");
        JButton grant = new JButton("Generate code and grant access");
        form.add(new JLabel("Remote worker")); form.add(workerChoice);
        form.add(new JLabel("Patient")); form.add(patientChoice);
        form.add(new JLabel("Expiry (YYYY-MM-DDTHH:MM)")); form.add(expiryField);
        form.add(reload); form.add(grant);
        panel.add(form);
        reload.addActionListener(e -> loadGrantChoices());
        grant.addActionListener(e -> grantAccess());
        loadGrantChoices();
        return panel;
    }

    private void loadGrantChoices() {
        workerChoice.removeAllItems(); patientChoice.removeAllItems();
        try (Connection c = DatabaseConnection.getConnection()) {
            String workers = "SELECT s.staff_id, s.first_name, s.last_name FROM Staff s JOIN Role r ON r.role_id=s.role_id WHERE r.role_name='Remote Worker' AND s.termination_date IS NULL ORDER BY s.last_name";
            try (Statement q = c.createStatement(); ResultSet r = q.executeQuery(workers)) {
                while (r.next()) workerChoice.addItem(new StaffOption(r.getLong(1), r.getString(2) + " " + r.getString(3)));
            }
            try (Statement q = c.createStatement(); ResultSet r = q.executeQuery("SELECT patient_id, first_name, last_name FROM Patient ORDER BY last_name, first_name")) {
                while (r.next()) patientChoice.addItem(new PatientOption(r.getLong(1), r.getString(2) + " " + r.getString(3)));
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void grantAccess() {
        if (workerChoice.getSelectedItem() == null || patientChoice.getSelectedItem() == null) { showMessage("A remote worker and patient must be available."); return; }
        try {
            LocalDateTime expiry = LocalDateTime.parse(expiryField.getText().trim());
            if (!expiry.isAfter(LocalDateTime.now())) { showMessage("Expiry must be in the future."); return; }
            StaffOption worker = (StaffOption) workerChoice.getSelectedItem();
            PatientOption patient = (PatientOption) patientChoice.getSelectedItem();
            String code = generateCode();
            try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL grant_remote_access(?, ?, ?, ?, ?, ?)}")) {
                q.setNull(1, Types.BIGINT);
                q.setLong(2, worker.id); q.setLong(3, patient.id); q.setLong(4, administratorStaffId);
                q.setString(5, hash(code)); q.setTimestamp(6, Timestamp.valueOf(expiry));
                q.execute();
                long accessId;
                try (Statement idQuery = c.createStatement(); ResultSet r = idQuery.executeQuery("SELECT LAST_INSERT_ID()")) {
                    if (!r.next()) throw new SQLException("MySQL did not return the access ID.");
                    accessId = r.getLong(1);
                }
                JOptionPane.showMessageDialog(this, "Remote access granted. Give the worker these values securely:\nAccess ID: " + accessId + "\nAccess code: " + code + "\nThe code is shown once.");
            }
        } catch (DateTimeParseException ex) {
            showMessage("Enter expiry as YYYY-MM-DDTHH:MM.");
        } catch (SQLException ex) { showError(ex); }
    }

    private JPanel revocationPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refresh = new JButton("Refresh active grants"), revoke = new JButton("Revoke selected grant");
        controls.add(refresh); controls.add(revoke);
        accessTable.setAutoCreateRowSorter(true); accessTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(controls, BorderLayout.NORTH); panel.add(new JScrollPane(accessTable), BorderLayout.CENTER);
        refresh.addActionListener(e -> loadAccessGrants()); revoke.addActionListener(e -> revokeSelected());
        loadAccessGrants();
        return panel;
    }

    private void loadAccessGrants() {
        accessModel.setRowCount(0);
        String sql = "SELECT ra.access_id, CONCAT(s.first_name,' ',s.last_name) AS worker, " +
                "CONCAT(p.first_name,' ',p.last_name) AS patient, ra.expires_at " +
                "FROM Remote_access ra JOIN Staff s ON s.staff_id=ra.staff_id JOIN Patient p ON p.patient_id=ra.patient_id " +
                "WHERE ra.revoked_at IS NULL AND ra.expires_at>CURRENT_TIMESTAMP ORDER BY ra.expires_at";
        try (Connection c = DatabaseConnection.getConnection(); Statement q = c.createStatement(); ResultSet r = q.executeQuery(sql)) {
            while (r.next()) accessModel.addRow(new Object[]{r.getLong("access_id"), r.getString("worker"), r.getString("patient"), r.getTimestamp("expires_at")});
        } catch (SQLException ex) { showError(ex); }
    }

    private void revokeSelected() {
        int selected = accessTable.getSelectedRow();
        if (selected < 0) { showMessage("Select an active grant first."); return; }
        int row = accessTable.convertRowIndexToModel(selected);
        long id = ((Number) accessModel.getValueAt(row, 0)).longValue();
        if (JOptionPane.showConfirmDialog(this, "Revoke access " + id + "?", "Confirm revocation", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL revoke_remote_access(?)}")) {
            q.setLong(1, id); q.execute(); loadAccessGrants(); showMessage("Remote access revoked.");
        } catch (SQLException ex) { showError(ex); }
    }

    private String generateCode() {
        byte[] bytes = new byte[8]; new SecureRandom().nextBytes(bytes);
        StringBuilder value = new StringBuilder();
        for (byte b : bytes) value.append(String.format(java.util.Locale.ROOT, "%02x", b));
        return value.toString();
    }

    private String hash(String value) throws SQLException {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) hex.append(String.format(java.util.Locale.ROOT, "%02x", b));
            return hex.toString();
        } catch (Exception ex) { throw new SQLException("Unable to hash access credentials.", ex); }
    }

    private void setOptional(PreparedStatement q, int index, String value) throws SQLException {
        String trimmed = value.trim(); if (trimmed.isEmpty()) q.setNull(index, Types.VARCHAR); else q.setString(index, trimmed);
    }

    private static DefaultTableModel nonEditableModel(String... columns) {
        return new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
    }

    private void showMessage(String message) { JOptionPane.showMessageDialog(this, message); }
    private void showError(SQLException ex) { JOptionPane.showMessageDialog(this, "Administrator operation failed: " + ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE); }

    private static final class StaffOption {
        final long id; final String name;
        StaffOption(long id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name + " (" + id + ")"; }
    }
    private static final class PatientOption {
        final long id; final String name;
        PatientOption(long id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name + " (" + id + ")"; }
    }
}
