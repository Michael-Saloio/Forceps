package views;

// THESE ARE FOR CONNECTION 
import database.DatabaseConnection;
import database.AuditLogger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// THESE ARE FOR PASSWORD HASHING 
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

import java.awt.*;

public class LoginScreen extends JFrame {

    private JTextField userIdField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton forgotPasswordButton;

    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 64);
    private static final Font SUBTITLE_FONT = new Font("Arial", Font.PLAIN, 16);
    private static final Color HINT_COLOR = Color.GRAY;

    // TESTING FOR CONNECTION
    private void testConnection() {

        try (Connection conn = DatabaseConnection.getConnection()) {

            System.out.println("Database connected successfully!");

        } catch (Exception e) {

            System.out.println("Database connection failed!");
            e.printStackTrace();
        }
    }

    // Constructor for the LoginScreen class
    public LoginScreen() {
        setTitle("Forceps DBMS - Login");
        setSize(400, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initializeComponents();
        setupListeners();

        // TEST CONNECTION
        testConnection();

        // Responsible for pulling inital focus away to root pane when window opens
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                getRootPane().requestFocusInWindow();
            }
        });
    }

    // Initializes and adds components to the JFrame
    private void initializeComponents() {
        add(createNorthPanel(), BorderLayout.NORTH);
        add(createFormPanel(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    // Creates the north panel containing the title + subtitle
    private JPanel createNorthPanel() {
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));

        JLabel headerLabel = new JLabel("FORCEPS", JLabel.CENTER);
        headerLabel.setFont(TITLE_FONT);
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(35, 0, 0, 0));
        northPanel.add(headerLabel);

        JLabel subtitleLabel = new JLabel("Smarter Data for Stronger Lives", JLabel.CENTER);
        subtitleLabel.setFont(SUBTITLE_FONT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 35, 0));
        northPanel.add(subtitleLabel);

        return northPanel;
    }

    // Creates the form panel containing the user ID field, password field, and
    // login button
    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridLayout(3, 3, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 50, 15, 50));

        userIdField = new JTextField();
        userIdField.setText("Account ID");
        userIdField.setForeground(Color.GRAY);

        passwordField = new JPasswordField();
        passwordField.setText("Password");
        passwordField.setEchoChar((char) 0); // Disables mask so password hint initially shows
        passwordField.setForeground(Color.GRAY);

        loginButton = new JButton("Login");
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        formPanel.add(userIdField);
        formPanel.add(passwordField);
        formPanel.add(loginButton);

        return formPanel;
    }

    // Creates the footer panel containing the "Forgot your password?" button
    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel();
        footerPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 20, 0));

        forgotPasswordButton = new JButton("Forgot your password?");
        forgotPasswordButton.setBorderPainted(false);
        forgotPasswordButton.setContentAreaFilled(false);
        forgotPasswordButton.setFocusPainted(false);
        forgotPasswordButton.setOpaque(false);
        forgotPasswordButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        footerPanel.add(forgotPasswordButton);

        return footerPanel;
    }

    // Sets up listeners for user interactions with the login screen components
    private void setupListeners() {
        userIdField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (userIdField.getText().equals("Account ID")) {
                    userIdField.setText("");
                    userIdField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (userIdField.getText().isEmpty()) {
                    userIdField.setForeground(HINT_COLOR);
                    userIdField.setText("Account ID");
                }
            }
        });

        passwordField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (String.valueOf(passwordField.getPassword()).equals("Password")) {
                    passwordField.setText("");
                    passwordField.setEchoChar('•'); // Restores mask to obscure password input
                    passwordField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (passwordField.getPassword().length == 0) {
                    passwordField.setText("Password");
                    passwordField.setEchoChar((char) 0); // Turns off mask to show password hint
                    passwordField.setForeground(HINT_COLOR);
                }
            }
        });

        loginButton.addActionListener(e -> {
            try {
                Long userId = Long.parseLong(userIdField.getText());
                char[] passwordChars = passwordField.getPassword();
                handleLogin(userId, passwordChars);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid Account ID.", "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        forgotPasswordButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Contact a System Administrator to set a temporary password. They can use Manage accounts after signing in.",
                    "Password help", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    // TO HASH THE PASSWORD -> WHEN CREATING NEW USER ACCOUNT DO THIS:
    // String hashedPassword = hashPassword(password)
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(
                    password.getBytes(
                            StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(
                        String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error hashing password", e);
        }
    }

    // Controls the logic behind the login process, including authentication and
    // feedback to the user
    // actual authentication logic here
    private void handleLogin(Long userId, char[] passwordChars) {
        // Convert the entered password from char[] to String
        String enteredPassword = String.valueOf(passwordChars);

        // Hash the entered password using SHA-256
        String hashedEnteredPassword = hashPassword(enteredPassword);

        // SQL query to get user account and staff info
        String sql = """
                SELECT
                    ua.account_id,
                    ua.password_hash,
                    r.role_name,
                    s.first_name,
                    s.last_name,
                    s.staff_id
                FROM User_account ua
                JOIN Staff s
                    ON ua.staff_id = s.staff_id
                JOIN Role r
                    ON s.role_id = r.role_id
                WHERE ua.account_id = ?
                AND ua.account_status = 'ACTIVE'
                """;

        try (
                // Connect to database
                Connection conn = DatabaseConnection.getConnection();

                // Create a prepared statement
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Set the account ID parameter
            stmt.setLong(1, userId);

            // Execute the query
            ResultSet rs = stmt.executeQuery();

            // Check whether a matching account was found
            if (rs.next()) {
                // Get the password hash stored in the database
                String storedPassword = rs.getString("password_hash");
                // Get the user's role
                String role = rs.getString("role_name");
                // Get first name
                String firstName = rs.getString("first_name");
                // Get last name
                String lastName = rs.getString("last_name");
                long staffId = rs.getLong("staff_id");
                // Compare the stored hash with the hash of the entered password
                if (storedPassword.equals(hashedEnteredPassword)) {

                    AuditLogger.logBestEffort(staffId, null, "LOGIN_SUCCEEDED", "USER_ACCOUNT", Long.toString(userId), "SUCCESS", "Staff login succeeded.");

                    JOptionPane.showMessageDialog(
                            this,
                            "Welcome "
                                    + firstName
                                    + " "
                                    + lastName
                                    + "\nRole: "
                                    + role,
                            "Login Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                   new DashboardScreen(
                        firstName + " " + lastName, role, staffId ).setVisible(true);
                        dispose();

                } else {
                    // Password was incorrect
                    JOptionPane.showMessageDialog(
                            this,
                            "Invalid Account ID or Password.",
                            "Login Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // No active account found with the entered ID
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid Account ID or Password.",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            // Show database error message
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to connect to database.\n"
                            + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);

            e.printStackTrace();
        } finally {
            // Clear password characters from memory
            Arrays.fill(passwordChars, '0');
        }
    }

    public static void main(String[] args) {
        try {
            // Set the look and feel to the system's default
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new LoginScreen().setVisible(true));
    }

}
