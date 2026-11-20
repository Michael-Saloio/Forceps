package views;

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

    private JTextField staffIdField;
    private JTextField employeeNameField;
    private JTextField accessIdField;
    private JTextField patientIdField;
    private JTextField revokedByField;
    private JTextField revokedOnField;

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
    private void configureWindowProperties() {
        setTitle("Forceps DBMS - Revoke Remote Access");
        setSize(480, 460);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
    }

    // Initializes and adds components to the JFrame
    private void initializeComponents() {
        add(createFormPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

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
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cancelButton = new JButton("Cancel");
        submitButton = new JButton("Revoke Access");

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