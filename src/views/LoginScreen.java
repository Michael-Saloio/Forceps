package views;

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

    // Constructor for the LoginScreen class
    public LoginScreen() {
        setTitle("Forceps DBMS - Login");
        setSize(400,420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        setLocationRelativeTo(null); 
        setLayout(new BorderLayout()); 

        initializeComponents();
        setupListeners();

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

    // Creates the form panel containing the user ID field, password field, and login button
    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridLayout(3, 3, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 50, 15, 50));
        
        userIdField = new JTextField();
        userIdField.setText("User ID");
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
    // TO DO: Implement "forgot your password" functionality in the future
    private void setupListeners() {
        userIdField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (userIdField.getText().equals("User ID")) {
                    userIdField.setText("");
                    userIdField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (userIdField.getText().isEmpty()) {
                    userIdField.setForeground(HINT_COLOR);
                    userIdField.setText("User ID");
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
                int userId = Integer.parseInt(userIdField.getText());
                char[] passwordChars = passwordField.getPassword();
                handleLogin(userId, passwordChars);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid User ID.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Future feature to be implemented, currently a placeholder 
        forgotPasswordButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "This feature is a work in progress.", "Forgot Password", JOptionPane.WARNING_MESSAGE);
        });
    }
    
    // Controls the logic behind the login process, including authentication and feedback to the user
    // TO DO: Implement actual authentication logic here ////////////////////////////////////////////////////////////////////////////
    // Mock authentication for demo purposes
    private void handleLogin(int userId, char[] passwordChars) {
        try {
            if (userId == 12345 && String.valueOf(passwordChars).equals("password")) {
                JOptionPane.showMessageDialog(this, "Login Successful!", "Success", JOptionPane.INFORMATION_MESSAGE); 
            } else {
                JOptionPane.showMessageDialog(this, "Invalid User ID or Password. Please try again.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        } finally {
            Arrays.fill(passwordChars, '0'); // Wipes password from memory
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