package views;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class GrantRemoteAccessScreen extends JFrame {

    private static final String GRANTED_BY_ADMIN_ID = "90909";
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String staffId;
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

    // Constructor taking only the selected staff ID
    public GrantRemoteAccessScreen(String staffId) {
        this.staffId = staffId;

        loadStaffDetails();
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

    // Queries employee details from the database using staffId
    // TO DO: DATABASE LOGIC NEEDED HERE //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void loadStaffDetails() {
        // Default fallback before DB query is wired up
        this.employeeName = "Staff ID " + staffId;

        // Example structure once connected to JDBC:
        // SELECT first_name, last_name FROM staff WHERE staff_id = ?
        // this.employeeName = firstName + " " + lastName;
    }

    // Window formatting matching other child screens
    private void configureWindowProperties() {
        setTitle("Forceps DBMS - Generate Remote Access Code");
        setSize(480, 440);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
    }

    // Initializes and adds components to the JFrame
    private void initializeComponents() {
        add(createFormPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    // Creates the form panel containing the remote access details
    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Generate Remote Access Code - " + employeeName));

        staffIdField = new JTextField(staffId != null ? staffId : "");
        staffIdField.setEditable(false);

        accessIdField = new JTextField(15);
        patientIdField = new JTextField(15);

        grantedByField = new JTextField(GRANTED_BY_ADMIN_ID);
        grantedByField.setEditable(false);

        String nowTimestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        createdOnField = new JTextField(nowTimestamp);
        createdOnField.setEditable(false);

        expiryField = new JTextField(LocalDateTime.now().plusDays(1).format(TIMESTAMP_FORMAT));
        accessCodeField = new JTextField(15);

        formPanel.add(new JLabel("Employee / Staff ID:"));
        formPanel.add(staffIdField);

        formPanel.add(new JLabel("Access ID:"));
        formPanel.add(accessIdField);

        formPanel.add(new JLabel("Patient ID:"));
        formPanel.add(patientIdField);

        formPanel.add(new JLabel("Granted By:"));
        formPanel.add(grantedByField);

        formPanel.add(new JLabel("Created On:"));
        formPanel.add(createdOnField);

        formPanel.add(new JLabel("Expiry (YYYY-MM-DD HH:MM:SS):"));
        formPanel.add(expiryField);

        formPanel.add(new JLabel("Access Code:"));
        formPanel.add(accessCodeField);

        return formPanel;
    }

    // Creates the south button panel holding detail options
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cancelButton = new JButton("Cancel");
        submitButton = new JButton("Submit");

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

    // Validates that required fields are filled and expiry matches YYYY-MM-DD HH:MM:SS
    private boolean validateInputs() {
        if (accessIdField.getText().trim().isEmpty()
                || patientIdField.getText().trim().isEmpty()
                || expiryField.getText().trim().isEmpty()
                || accessCodeField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields before submitting.");
            return false;
        }

        try {
            LocalDateTime.parse(expiryField.getText().trim(), TIMESTAMP_FORMAT);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Expiry must be in YYYY-MM-DD HH:MM:SS format (e.g., 2026-11-20 14:30:00)."
            );
            expiryField.requestFocus();
            return false;
        }

        return true;
    }

    // Handles submission and displays confirmation popup
    // TO DO: DATABASE LOGIC NEEDED HERE //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void handleSubmit() {
        if (!validateInputs()) {
            return;
        }

        String accessId = accessIdField.getText().trim();
        String patientId = patientIdField.getText().trim();
        String grantedBy = grantedByField.getText().trim();
        String createdOn = createdOnField.getText().trim();
        String expiry = expiryField.getText().trim();
        String accessCode = accessCodeField.getText().trim();

        // TODO: Insert remote access grant (staffId, accessId, patientId, grantedBy, createdOn, expiry, accessCode) into DB

        JOptionPane.showMessageDialog(
                this,
                "You have granted " + employeeName + " remote access."
        );

        dispose();
    }
}