package views;

import db.Database;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class DashboardScreen extends JFrame {

    JPanel mainPanel;
    CardLayout layout;
    String staffName;
    String role;

    public DashboardScreen(String name, String userRole) {

        staffName = name;
        role = userRole;

        setTitle("Forceps Hospital Dashboard");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        layout = new CardLayout();
        mainPanel = new JPanel(layout);

        mainPanel.add(homePage(), "Home");
        mainPanel.add(profilePage(), "Profile");
        mainPanel.add(searchPage(), "Search");
        mainPanel.add(rolePage(), "Role");

        JPanel menu = new JPanel(new GridLayout(4, 1));

        JButton home = new JButton("Dashboard");
        JButton profile = new JButton("Profile");
        JButton search = new JButton("Search Patient");
        JButton workspace = new JButton("Role Panel");

        home.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                layout.show(mainPanel, "Home");
            }
        });

        profile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                layout.show(mainPanel, "Profile");
            }
        });

        search.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                layout.show(mainPanel, "Search");
            }
        });

        workspace.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                layout.show(mainPanel, "Role");
            }
        });

        menu.add(home);
        menu.add(profile);
        menu.add(search);
        menu.add(workspace);

        add(menu, BorderLayout.WEST);
        add(mainPanel, BorderLayout.CENTER);
    }

    // Main dashboard
    JPanel homePage() {

        JPanel panel = new JPanel(new GridLayout(4, 1));

        panel.add(new JLabel("Welcome to Forceps Hospital"));
        panel.add(new JLabel("Staff: " + staffName));
        panel.add(new JLabel("Role: " + role));
        panel.add(new JLabel("Choose an option from the menu."));

        return panel;
    }

    // Profile page
    JPanel profilePage() {

        JPanel panel = new JPanel(new GridLayout(3, 1));

        panel.add(new JLabel("My Profile"));
        panel.add(new JLabel("Name: " + staffName));
        panel.add(new JLabel("Role: " + role));

        return panel;
    }

    // Patient search page
    JPanel searchPage() {

        JPanel panel = new JPanel(new BorderLayout());
        JPanel top = new JPanel();

        JTextField lastName = new JTextField(12);
        JTextField dob = new JTextField(10);

        JButton searchButton = new JButton("Search");

        top.add(new JLabel("Last Name:"));
        top.add(lastName);

        top.add(new JLabel("DOB (YYYY-MM-DD):"));
        top.add(dob);
        top.add(searchButton);

        String[] columns = { "ID", "Name", "DOB", "Phone" };

        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);

        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                model.setRowCount(0);

                String name = lastName.getText().trim();
                String birth = dob.getText().trim();

                if (name.isEmpty() || birth.isEmpty()) {
                    JOptionPane.showMessageDialog(
                            DashboardScreen.this,
                            "Please enter last name and date of birth.");
                    return;
                }

                try {

                    java.util.List<String[]> patients = Database.searchPatient(name, birth);

                    for (String[] patient : patients) {
                        model.addRow(patient);
                    }

                    if (patients.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                DashboardScreen.this,
                                "No patients found.");
                    }

                } catch (Exception ex) {

                    JOptionPane.showMessageDialog(
                            DashboardScreen.this,
                            "Search failed: " + ex.getMessage());
                }
            }
        });

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    // Role workspace
    JPanel rolePage() {

        JPanel panel = new JPanel(new GridLayout(4, 1));

        panel.add(new JLabel(role + " Workspace"));

        if (role.equalsIgnoreCase("Doctor")) {

            panel.add(new JButton("Patient Records"));
            panel.add(new JButton("Treatments"));
            panel.add(new JButton("Appointments"));

        } else if (role.equalsIgnoreCase("Nurse")) {

            panel.add(new JButton("Patient Assignments"));
            panel.add(new JButton("Vital Signs"));
            panel.add(new JButton("Treatments"));

        } else if (role.equalsIgnoreCase("Receptionist")) {

            panel.add(new JButton("Register Patient"));
            panel.add(new JButton("Appointments"));
            panel.add(new JButton("Billing"));

        } else if (role.equalsIgnoreCase("Administrator")) {

            panel.add(new JButton("Manage Accounts"));
            panel.add(new JButton("Permissions"));
            panel.add(new JButton("Audit Logs"));

        } else if (role.equalsIgnoreCase("HR")) {

            panel.add(new JButton("Employee Onboarding"));
            panel.add(new JButton("Employee Offboarding"));
        }
        return panel;
    }
}
