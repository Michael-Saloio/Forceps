package views;

<<<<<<< HEAD
import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RevokeRemoteAccessScreen extends JFrame {

    private static final String REVOKED_BY_ADMIN_ID = "90909";
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String staffId;
    private String employeeName;

=======
import database.DatabaseConnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Revokes an existing remote access grant for a selected remote worker. */
public class RevokeRemoteAccessScreen extends JFrame {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String staffId;
    private final long revokedByStaffId;
    private String employeeName;
>>>>>>> develop
    private JTextField staffIdField;
    private JTextField employeeNameField;
    private JTextField accessIdField;
    private JTextField patientIdField;
    private JTextField revokedByField;
    private JTextField revokedOnField;
<<<<<<< HEAD

    private JButton submitButton;
    private JButton cancelButton;

    // Constructor taking only the selected staff ID
    public RevokeRemoteAccessScreen(String staffId) {
        this.staffId = staffId;

        loadStaffAndAccessDetails();
        configureWindowProperties();
        initializeComponents();
        setupListeners();

        // Pull initial focus away to root pane when window opens
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                getRootPane().requestFocusInWindow();
            }
        });
    }

    // Queries employee and active remote access details from the database using staffId
    // TO DO: DATABASE LOGIC NEEDED HERE //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void loadStaffAndAccessDetails() {
        this.employeeName = "Staff ID " + staffId;

    }

    // Window formatting 
=======
    private JButton submitButton;
    private JButton cancelButton;

    public RevokeRemoteAccessScreen(String staffId) {
        this(staffId, findActiveAdministratorId());
    }

    public RevokeRemoteAccessScreen(String staffId, long revokedByStaffId) {
        this.staffId = staffId == null ? "" : staffId.trim();
        this.revokedByStaffId = revokedByStaffId;
        loadStaffDetails();
        configureWindowProperties();
        initializeComponents();
        setupListeners();
    }

    private void loadStaffDetails() {
        employeeName = "Staff ID " + staffId;
        String sql = "SELECT s.first_name, s.last_name FROM Staff s JOIN Role r ON r.role_id=s.role_id "
                + "WHERE s.staff_id=? AND r.role_name='Remote Worker'";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setString(1, staffId);
            try (ResultSet r = q.executeQuery()) {
                if (r.next()) employeeName = r.getString("first_name") + " " + r.getString("last_name");
                else showMessage("This staff ID is not a remote worker.");
            }
        } catch (SQLException ex) { showError(ex); }
    }

>>>>>>> develop
    private void configureWindowProperties() {
        setTitle("Forceps DBMS - Revoke Remote Access");
        setSize(480, 460);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
    }

<<<<<<< HEAD
    // Initializes and adds components to the JFrame
=======
>>>>>>> develop
    private void initializeComponents() {
        add(createFormPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

<<<<<<< HEAD
    // Creates the form panel containing the revocation details
    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Revoke Remote Access - " + employeeName));

        staffIdField = new JTextField(staffId != null ? staffId : "");
        staffIdField.setEditable(false);

        employeeNameField = new JTextField(employeeName);
        employeeNameField.setEditable(false);

        accessIdField = new JTextField(15);

        patientIdField = new JTextField(15);
        patientIdField.setEditable(false); // View-only; populated when loading the access record

        revokedByField = new JTextField(REVOKED_BY_ADMIN_ID);
        revokedByField.setEditable(false);

        revokedOnField = new JTextField(LocalDateTime.now().format(TIMESTAMP_FORMAT));
        revokedOnField.setEditable(false);

        formPanel.add(new JLabel("Employee / Staff ID:"));
        formPanel.add(staffIdField);

        formPanel.add(new JLabel("Employee Name:"));
        formPanel.add(employeeNameField);

        formPanel.add(new JLabel("Access ID:"));
        formPanel.add(accessIdField);

        formPanel.add(new JLabel("Patient ID:"));
        formPanel.add(patientIdField);

        formPanel.add(new JLabel("Revoked By:"));
        formPanel.add(revokedByField);

        formPanel.add(new JLabel("Revoked On:"));
        formPanel.add(revokedOnField);

        return formPanel;
    }

    // Creates the south button panel holding detail options
=======
    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Revoke Remote Access - " + employeeName));
        staffIdField = new JTextField(staffId);
        staffIdField.setEditable(false);
        employeeNameField = new JTextField(employeeName);
        employeeNameField.setEditable(false);
        accessIdField = new JTextField(15);
        patientIdField = new JTextField(15);
        patientIdField.setEditable(false);
        revokedByField = new JTextField(revokedByStaffId > 0 ? Long.toString(revokedByStaffId) : "Unavailable");
        revokedByField.setEditable(false);
        revokedOnField = new JTextField(LocalDateTime.now().format(TIMESTAMP_FORMAT));
        revokedOnField.setEditable(false);

        formPanel.add(new JLabel("Employee / Staff ID:")); formPanel.add(staffIdField);
        formPanel.add(new JLabel("Employee Name:")); formPanel.add(employeeNameField);
        formPanel.add(new JLabel("Access ID:")); formPanel.add(accessIdField);
        formPanel.add(new JLabel("Patient ID (from grant):")); formPanel.add(patientIdField);
        formPanel.add(new JLabel("Revoked By (staff ID):")); formPanel.add(revokedByField);
        formPanel.add(new JLabel("Revoked On:")); formPanel.add(revokedOnField);
        return formPanel;
    }

>>>>>>> develop
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cancelButton = new JButton("Cancel");
        submitButton = new JButton("Revoke Access");
<<<<<<< HEAD

        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);

        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return buttonPanel;
    }

    // Sets up listeners for user interactions with the buttons
    private void setupListeners() {
        submitButton.addActionListener(e -> handleSubmit());
        cancelButton.addActionListener(e -> dispose());
    }

    // Validates required inputs before revoking
    private boolean validateInputs() {
        if (accessIdField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please provide the Access ID.");
            return false;
        }
        return true;
    }

    // Handles revocation confirmation and updates state
    // TO DO: DATABASE LOGIC NEEDED HERE //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void handleSubmit() {
        if (!validateInputs()) {
            return;
        }

        String accessId = accessIdField.getText().trim();
        String patientId = patientIdField.getText().trim();
        String revokedBy = revokedByField.getText().trim();
        String revokedOn = revokedOnField.getText().trim();

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to revoke remote access (" + accessId + ") for " + employeeName + "?",
                "Confirm Revocation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        // TODO: Mark access record as revoked/inactive in DB ///////

        JOptionPane.showMessageDialog(
                this,
                "You have revoked " + employeeName + "'s remote access."
        );

        dispose();
    }
}
=======
        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);
        return buttonPanel;
    }

    private void setupListeners() {
        submitButton.addActionListener(e -> handleSubmit());
        cancelButton.addActionListener(e -> dispose());
        accessIdField.addActionListener(e -> retrievePatientId());
    }

    /** Loads the patient linked to this access ID and the selected remote worker. */
    private Long retrievePatientId() {
        long workerId;
        long accessId;
        try {
            workerId = Long.parseLong(staffId);
            accessId = Long.parseLong(accessIdField.getText().trim());
        } catch (NumberFormatException ex) {
            showMessage("Enter a numeric access ID.");
            return null;
        }
        String sql = "SELECT ra.patient_id, p.first_name, p.last_name FROM Remote_access ra "
                + "JOIN Patient p ON p.patient_id=ra.patient_id "
                + "WHERE ra.access_id=? AND ra.staff_id=? AND ra.revoked_at IS NULL";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, accessId);
            q.setLong(2, workerId);
            try (ResultSet r = q.executeQuery()) {
                if (!r.next()) {
                    patientIdField.setText("");
                    showMessage("No active access grant with that ID was found for " + employeeName + ".");
                    return null;
                }
                long patientId = r.getLong("patient_id");
                patientIdField.setText(Long.toString(patientId));
                return patientId;
            }
        } catch (SQLException ex) { showError(ex); return null; }
    }

    private void handleSubmit() {
        if (accessIdField.getText().trim().isEmpty()) { showMessage("Enter the Access ID."); return; }
        if (revokedByStaffId <= 0) { showMessage("Could not identify an active system administrator."); return; }
        Long patientId = retrievePatientId();
        if (patientId == null) return;
        long accessId = Long.parseLong(accessIdField.getText().trim());

        int choice = JOptionPane.showConfirmDialog(this,
                "Revoke access " + accessId + " for " + employeeName + " to patient " + patientId + "?",
                "Confirm Revocation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL revoke_remote_access(?)}")) {
            q.setLong(1, accessId);
            q.execute();
            revokedOnField.setText(LocalDateTime.now().format(TIMESTAMP_FORMAT));
            showMessage("Remote access " + accessId + " for patient " + patientId + " was revoked.");
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

    private void showMessage(String message) { JOptionPane.showMessageDialog(this, message); }
    private void showError(SQLException ex) { JOptionPane.showMessageDialog(this, "Remote access revocation failed: " + ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE); }
}
>>>>>>> develop
