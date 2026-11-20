package views;

import javax.swing.*;
import java.awt.*;

public class OffBoardStaffScreen extends JFrame {
    private String staffId;

    private JTextField staffIdField;
    private JTextField roleField;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField dobField;
    private JTextArea notesArea;

    private JButton submitButton;
    private JButton cancelButton;

    public OffBoardStaffScreen(String staffId) {
        this.staffId = staffId;

        configureWindowProperties();
        initializeComponents();
        setupListeners();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                getRootPane().requestFocusInWindow();
            }
        });
    }

    private void configureWindowProperties() {
        setTitle("Forceps DBMS - Offboard Staff (ID: " + staffId + ")");
        setSize(450, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
    }

    private void initializeComponents() {
        add(createFormPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Offboarding - ID: " + staffId));

        staffIdField = new JTextField(staffId != null ? staffId : "");
        staffIdField.setEditable(false);

        roleField = new JTextField();
        roleField.setEditable(false);

        firstNameField = new JTextField();
        firstNameField.setEditable(false);

        lastNameField = new JTextField();
        lastNameField.setEditable(false);

        dobField = new JTextField();
        dobField.setEditable(false);

        notesArea = new JTextArea(4, 25);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScrollPane = new JScrollPane(notesArea);

        formPanel.add(new JLabel("Staff ID:"));
        formPanel.add(staffIdField);

        formPanel.add(new JLabel("Role:"));
        formPanel.add(roleField);

        formPanel.add(new JLabel("First Name:"));
        formPanel.add(firstNameField);

        formPanel.add(new JLabel("Last Name:"));
        formPanel.add(lastNameField);

        formPanel.add(new JLabel("Date of Birth:"));
        formPanel.add(dobField);

        formPanel.add(new JLabel("Reason for Removal:"));
        formPanel.add(notesScrollPane);

        return formPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cancelButton = new JButton("Cancel");
        submitButton = new JButton("OffBoard");

        buttonPanel.add(cancelButton);
        buttonPanel.add(submitButton);

        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return buttonPanel;
    }

    private void setupListeners() {
        submitButton.addActionListener(e -> handleSubmit());
        cancelButton.addActionListener(e -> dispose());
    }

    private boolean validateInputs() {
        if (notesArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please provide a description");
            return false;
        }
        return true;
    }

    private void handleSubmit() {
        if (!validateInputs()) {
            return;
        }

        String idText = staffIdField.getText().trim();
        String role = roleField.getText().trim();
        String fName = firstNameField.getText().trim();
        String lName = lastNameField.getText().trim();
        String dobText = dobField.getText().trim();
        String reason = notesArea.getText().trim();

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to Offboard Staff" + fName + " " + lName + "(ID: " + idText + ")?",
            "Confirm Submission",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        // TODO: Execute database offboarding logic here using idText and reason

        JOptionPane.showMessageDialog(
            this,
            "Staff member Offboarded:\n" +
            "Staff ID: " + idText + "\n" +
            "Role: " + role + "\n" +
            "First Name: " + fName + "\n" +
            "Last Name: " + lName + "\n" +
            "DOB: " + dobText + "\n" +
            "Reason: " + reason
        );

        dispose();
    }
}