package views;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.text.ParseException;

public class StaffDetailsScreen extends JFrame {

    // Controls which view is presented
    public enum FormMode {
        ADD,
        EDIT,
        VIEW
    }

    private FormMode mode;
    private String staffId;

    private JTextField staffIdField;
    private JComboBox<String> roleBox;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JFormattedTextField dateOfBirth;
    private JFormattedTextField phoneField;
    private JTextField emailField;
    private JFormattedTextField hireDateField;
    private JFormattedTextField terminationDateField;
    private JLabel terminationDateLabel;

    private JRadioButton activeButton;
    private JRadioButton inActiveButton;
    private ButtonGroup statusGroup;

    private JButton submitButton;
    private JButton cancelButton;

    // Constructor supporting ADD, EDIT, and VIEW modes
    // Takes the form mode and staff id (null if ADD)
    public StaffDetailsScreen(FormMode mode, String staffId) {
        this.mode = mode;
        this.staffId = staffId;

        configureWindowProperties();
        initializeComponents();
        setupListeners();
        applyModeSettings();

        // Pull initial focus away to root pane when window opens
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                getRootPane().requestFocusInWindow();
            }
        });
    }

    // View formatting for each mode
    private void configureWindowProperties() {
        String titleAction = "";
        switch (mode) {
            case ADD:
                titleAction = "ADD New Staff Member";
                break;
            case EDIT:
                titleAction = "EDIT Staff (ID: " + staffId + ")";
                break;
            case VIEW:
                titleAction = "VIEW Staff (ID: " + staffId + ")";
                break;
        }
        setTitle("Forceps DBMS - " + titleAction);
        setSize(450, mode == FormMode.ADD ? 400 : 470);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
    }

    // Initializes and adds components to the JFrame
    private void initializeComponents() {
        add(createFormPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    // Creates the form panel containing the account details
    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createTitledBorder("Staff Member Details"));

        // Phone & Date Formatters
        try {
            MaskFormatter phoneMask = new MaskFormatter("(###) ###-####");
            phoneMask.setPlaceholderCharacter('_');
            phoneField = new JFormattedTextField(phoneMask);

            MaskFormatter dateMask = new MaskFormatter("##-##-####");
            dateMask.setPlaceholderCharacter('_');
            hireDateField = new JFormattedTextField(dateMask);
            dateOfBirth = new JFormattedTextField(dateMask);
            terminationDateField = new JFormattedTextField(dateMask);

        } catch (ParseException e) {
            e.printStackTrace();
            phoneField = new JFormattedTextField();
            hireDateField = new JFormattedTextField();
            dateOfBirth = new JFormattedTextField();
            terminationDateField = new JFormattedTextField();
        }

        //Staff Id
        staffIdField = new JTextField(10);
        formPanel.add(new JLabel("Staff ID:"));
        formPanel.add(staffIdField);

        // Role Dropdown
        String[] roles = {"Doctor", "Nurse", "Receptionist", "System Administrator", "Remote Worker"};
        roleBox = new JComboBox<>(roles);
        formPanel.add(new JLabel("Role:"));
        formPanel.add(roleBox);

        // First Name
        firstNameField = new JTextField();
        formPanel.add(new JLabel("First Name:"));
        formPanel.add(firstNameField);

        // Last Name
        lastNameField = new JTextField();
        formPanel.add(new JLabel("Last Name:"));
        formPanel.add(lastNameField);

        // Date of birth
        formPanel.add(new JLabel("Date of Birth (DD-MM-YYYY):"));
        formPanel.add(dateOfBirth);

        // Email
        emailField = new JTextField(20);
        formPanel.add(new JLabel("Email Address:"));
        formPanel.add(emailField);

        // Phone number
        formPanel.add(new JLabel("Phone Number:"));
        formPanel.add(phoneField);

        // Hire Date
        formPanel.add(new JLabel("Hire Date (DD-MM-YYYY):"));
        formPanel.add(hireDateField);

        // Termination Date (Only for EDIT and VIEW modes)
        if (mode != FormMode.ADD) {
            terminationDateLabel = new JLabel("Termination Date (If Applicable):");
            formPanel.add(terminationDateLabel);
            formPanel.add(terminationDateField);
        }

        // Account Status Radio Buttons
        activeButton = new JRadioButton("Active");
        inActiveButton = new JRadioButton("Inactive");
        statusGroup = new ButtonGroup();
        statusGroup.add(activeButton);
        statusGroup.add(inActiveButton);
        activeButton.setSelected(true);

        // South Panel to hold radio buttons seperate from FormPanel
        // Corrects the boundaries from the coloumn constraints BY FromPanel 
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        statusPanel.add(activeButton);
        statusPanel.add(inActiveButton);

        // aCCOUTN Status
        formPanel.add(new JLabel("Account Status:"));
        formPanel.add(statusPanel);

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

    // Sets up listeners for user interactions with the user account components
    private void setupListeners() {
        submitButton.addActionListener(e -> handleSubmit());
        cancelButton.addActionListener(e -> dispose());
    }

    // Applies formatting and constraints based on the mode selected
    private void applyModeSettings() {
        if (mode == FormMode.ADD) {
            submitButton.setText("Create");
            setFieldsEditable(true);
            
        } else {
            staffIdField.setText(staffId);
            staffIdField.setEditable(false); // Primary key should not changed for existing accounts

            if (mode == FormMode.EDIT) {
                submitButton.setText("Save Changes");
                setFieldsEditable(true);
                staffIdField.setEditable(false);

                // Locks down all fields for view-only mode
            } else if (mode == FormMode.VIEW) {
                setFieldsEditable(false);
                submitButton.setVisible(false); 
                cancelButton.setText("Close");
            }
        }
    }

    private void setFieldsEditable(boolean editable) {
        staffIdField.setEditable(editable); 
        roleBox.setEnabled(editable);
        firstNameField.setEditable(editable);
        lastNameField.setEditable(editable);
        dateOfBirth.setEditable(editable);
        phoneField.setEditable(editable);
        emailField.setEditable(editable);
        hireDateField.setEditable(editable);
        if (terminationDateField != null) {
            terminationDateField.setEditable(editable);
        }
        activeButton.setEnabled(editable);
        inActiveButton.setEnabled(editable);

        if (!editable) {
            // Clears comestics associated accoutn details under view mode
            staffIdField.setBorder(null);
            firstNameField.setBorder(null);
            lastNameField.setBorder(null);
            dateOfBirth.setBorder(null);
            phoneField.setBorder(null);
            emailField.setBorder(null);
            hireDateField.setBorder(null);
            if (terminationDateField != null) {
                terminationDateField.setBorder(null);
            }

            // Matches background color to panel background for view mode
            Color backgroundBg = getBackground();
            staffIdField.setBackground(backgroundBg);
            firstNameField.setBackground(backgroundBg);
            lastNameField.setBackground(backgroundBg);
            dateOfBirth.setBackground(backgroundBg);
            phoneField.setBackground(backgroundBg);
            emailField.setBackground(backgroundBg);
            hireDateField.setBackground(backgroundBg);
            if (terminationDateField != null) {
                terminationDateField.setBackground(backgroundBg);
            } 

        } else {
            // RESTORES default cosmestics for accoutn details under ADD / EDIT modes
            staffIdField.setBorder(UIManager.getBorder("TextField.border"));
            firstNameField.setBorder(UIManager.getBorder("TextField.border"));
            lastNameField.setBorder(UIManager.getBorder("TextField.border"));
            dateOfBirth.setBorder(UIManager.getBorder("TextField.border"));
            phoneField.setBorder(UIManager.getBorder("TextField.border"));
            emailField.setBorder(UIManager.getBorder("TextField.border"));
            hireDateField.setBorder(UIManager.getBorder("TextField.border"));
            if (terminationDateField != null) {
                terminationDateField.setBorder(UIManager.getBorder("TextField.border"));
            }

            staffIdField.setBackground(Color.WHITE);
            firstNameField.setBackground(Color.WHITE);
            lastNameField.setBackground(Color.WHITE);
            dateOfBirth.setBackground(Color.WHITE);
            phoneField.setBackground(Color.WHITE);
            emailField.setBackground(Color.WHITE);
            hireDateField.setBackground(Color.WHITE);
            if (terminationDateField != null) {
                terminationDateField.setBackground(Color.WHITE);
            }
        }
    }

    // Validates account detail inputs by checking to see if each field was filled
    private boolean validateInputs() {
        boolean missingAddId = (mode == FormMode.ADD && staffIdField.getText().trim().isEmpty());

        if (missingAddId
            || firstNameField.getText().trim().isEmpty() 
            || lastNameField.getText().trim().isEmpty()
            || dateOfBirth.getText().contains("_")
            || phoneField.getText().contains("_")
            || emailField.getText().trim().isEmpty()
            || hireDateField.getText().trim().contains("_")) {
            JOptionPane.showMessageDialog(this, "Please fill in the form in its entirety before submitting.");
            return false;
        }
        return true;
    }

    // Handles the logic behind the ADD and EDIT modes of user accounts, including
    // minor email validation and final feedback to the user
    // TO DO: DATABASE LOGIC NEEDED HERE //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void handleSubmit() {
        if (!validateInputs()) {
            return;
        } 

        if (!emailField.getText().contains("@")) {
            JOptionPane.showMessageDialog(this, "Email must contain an '@' and domain.");
            emailField.requestFocus();
            return; 
        }
           
        String enteredStaffId = staffIdField.getText().trim();
        String role = (String) roleBox.getSelectedItem();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String dob = dateOfBirth.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String hireDate = hireDateField.getText().trim();
        String termDate = (terminationDateField != null) ? terminationDateField.getText().trim() : "N/A";
        String status = activeButton.isSelected() ? "Active" : "Inactive";

        if (mode == FormMode.ADD) {
            JOptionPane.showMessageDialog(this, "New staff member created:\n" +
                    "Staff ID: " + enteredStaffId + "\n" +
                    "Role: " + role + "\n" +
                    "First Name: " + firstName + "\n" +
                    "Last Name: " + lastName + "\n" +
                    "Date of Birth: " + dob + "\n" +
                    "Phone: " + phone + "\n" +
                    "Email: " + email + "\n" +
                    "Hire Date: " + hireDate + "\n" +
                    "Status: " + status);
        } else if (mode == FormMode.EDIT) {
            JOptionPane.showMessageDialog(this, "Changes saved for Staff ID: " + enteredStaffId + "\n" +
                    "Role: " + role + "\n" +
                    "First Name: " + firstName + "\n" +
                    "Last Name: " + lastName + "\n" +
                    "Date of Birth: " + dob + "\n" +
                    "Phone: " + phone + "\n" +
                    "Email: " + email + "\n" +
                    "Hire Date: " + hireDate + "\n" +
                    "Termination Date: " + termDate + "\n" +
                    "Status: " + status);
        }

        dispose();
    }
}