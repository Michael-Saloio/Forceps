package views;

import database.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Role-based application dashboard and navigation. */
public class DashboardScreen extends JFrame {
    private final JPanel mainPanel = new JPanel(new CardLayout());
    private final CardLayout layout = (CardLayout) mainPanel.getLayout();
    private final String staffName;
    private final String role;
    private final long staffId;
    private MedicalRecordsPanel recordsPanel;
    private AppointmentsPanel appointmentsPanel;
    private BillingPanel billingPanel;
    private PatientRegistrationPanel patientPanel;
    private SystemAdminPanel adminPanel;
    private RemoteAccessPanel remotePanel;

    public DashboardScreen(String name, String userRole, long staffId) {
        this.staffName = name;
        this.role = userRole == null ? "" : userRole;
        this.staffId = staffId;
        setTitle("Forceps Hospital Dashboard");
        setSize(1180, 760);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        prepareRoleViews();
        mainPanel.add(homePage(), "Home");
        mainPanel.add(profilePage(), "Profile");
        mainPanel.add(searchPage(), "Search");
        mainPanel.add(rolePage(), "Role");
        if (recordsPanel != null) mainPanel.add(recordsPanel, "Medical Records");
        if (appointmentsPanel != null) mainPanel.add(appointmentsPanel, "Appointments");
        if (billingPanel != null) mainPanel.add(billingPanel, "Billing");
        if (patientPanel != null) mainPanel.add(patientPanel, "Register Patient");
        if (adminPanel != null) mainPanel.add(adminPanel, "System Administration");
        if (remotePanel != null) mainPanel.add(remotePanel, "Remote Access");

        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(new Color(239, 244, 248));
        menu.setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 12));
        menu.setPreferredSize(new Dimension(238, 0));
        JLabel navigation = new JLabel("WORKSPACE");
        navigation.setFont(navigation.getFont().deriveFont(Font.BOLD, 12f));
        navigation.setForeground(new Color(96, 117, 135));
        navigation.setBorder(BorderFactory.createEmptyBorder(0, 8, 12, 0));
        menu.add(navigation);
        addNav(menu, "Dashboard", "Home");
        addNav(menu, "Profile", "Profile");
        addNav(menu, "Search Patient", "Search");
        addNav(menu, "Role Panel", "Role");
        if (recordsPanel != null) addNav(menu, "Patient Records / Treatments", "Medical Records");
        if (appointmentsPanel != null) addNav(menu, role.equalsIgnoreCase("Nurse") ? "Patient Assignments" : "Appointments", "Appointments");
        if (billingPanel != null) addNav(menu, "Billing", "Billing");
        if (patientPanel != null) addNav(menu, "Register Patient / Barcode", "Register Patient");
        if (adminPanel != null) {
            addAdminNav(menu, "Onboard Staff", "Onboard");
            addAdminNav(menu, "Offboard Staff", "Offboard");
            addAdminNav(menu, "Grant Remote Access", "Grant remote access");
            addAdminNav(menu, "Revoke Remote Access", "Revoke remote access");
            addAdminNav(menu, "Manage Accounts", "Manage accounts");
            addAdminNav(menu, "Audit Trail", "Audit trail");
        }
        if (remotePanel != null) addNav(menu, "Granted Patient Records", "Remote Access");

        JPanel shell = new JPanel(new BorderLayout());
        shell.add(menu, BorderLayout.WEST);
        shell.add(mainPanel, BorderLayout.CENTER);
        add(topBar(), BorderLayout.NORTH);
        add(shell, BorderLayout.CENTER);
    }

    private void prepareRoleViews() {
        if (isClinicalRole()) recordsPanel = new MedicalRecordsPanel(staffId, role);
        if (isReceptionist()) {
            billingPanel = new BillingPanel(staffId);
            patientPanel = new PatientRegistrationPanel(staffId);
        }
        if (isClinicalRole()) appointmentsPanel = new AppointmentsPanel(role, staffId);
        else if (isReceptionist()) appointmentsPanel = new AppointmentsPanel(role, staffId, patientPanel::showBarcodeForPatient);
        if (isAdminRole()) adminPanel = new SystemAdminPanel(staffId);
        if (role.equalsIgnoreCase("Remote Worker")) remotePanel = new RemoteAccessPanel(staffId);
    }

    private void addNav(JPanel menu, String label, String card) {
        JButton button = navigationButton(label);
        button.addActionListener(e -> layout.show(mainPanel, card));
        menu.add(button);
        menu.add(Box.createVerticalStrut(5));
    }

    private JPanel homePage() {
        JPanel panel = new JPanel(new BorderLayout(18, 18));
        panel.setBackground(new Color(245, 248, 251));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 34, 30, 34));
        JPanel welcome = new JPanel();
        welcome.setOpaque(false);
        welcome.setLayout(new BoxLayout(welcome, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Good day, " + staffName);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        JLabel subtitle = new JLabel("Your " + role + " workspace is ready. Choose a task to get started.");
        subtitle.setForeground(new Color(92, 111, 128));
        subtitle.setBorder(BorderFactory.createEmptyBorder(7, 0, 20, 0));
        welcome.add(title);
        welcome.add(subtitle);
        JPanel tasks = new JPanel(new GridLayout(0, 2, 16, 16));
        tasks.setOpaque(false);
        if (isClinicalRole()) {
            addTaskCard(tasks, "Patient records", "Review diagnoses, treatments, and visit history.", "Open records", () -> layout.show(mainPanel, "Medical Records"));
            addTaskCard(tasks, "Appointments", "Review your assigned appointments and visits.", "Open appointments", () -> layout.show(mainPanel, "Appointments"));
        } else if (isReceptionist()) {
            addTaskCard(tasks, "Patient registration", "Register a patient and print a patient-ID wristband.", "Register patient", () -> layout.show(mainPanel, "Register Patient"));
            addTaskCard(tasks, "Appointments", "Book, check in, check out, or record a no-show.", "Manage appointments", () -> layout.show(mainPanel, "Appointments"));
            addTaskCard(tasks, "Billing", "Review, print, and update patient invoices.", "Open billing", () -> layout.show(mainPanel, "Billing"));
        } else if (isAdminRole()) {
            for (String tab : new String[]{"Onboard", "Offboard", "Grant remote access", "Revoke remote access", "Manage accounts", "Audit trail"}) {
                addTaskCard(tasks, tab, "System administration task", "Open " + tab.toLowerCase(), () -> {
                    adminPanel.showTab(tab); layout.show(mainPanel, "System Administration");
                });
            }
        } else if (role.equalsIgnoreCase("Remote Worker")) {
            addTaskCard(tasks, "Granted patient records", "Open records only through an active, single-use access grant.", "Open remote access", () -> layout.show(mainPanel, "Remote Access"));
        }
        panel.add(welcome, BorderLayout.NORTH);
        panel.add(tasks, BorderLayout.CENTER);
        return panel;
    }

    private JPanel topBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(new Color(25, 48, 73));
        bar.setBorder(BorderFactory.createEmptyBorder(13, 22, 13, 22));
        JLabel brand = new JLabel("FORCEPS  |  Hospital Operations");
        brand.setForeground(Color.WHITE);
        brand.setFont(brand.getFont().deriveFont(Font.BOLD, 19f));
        JLabel identity = new JLabel(staffName + "  ·  " + role);
        identity.setForeground(new Color(222, 235, 243));
        bar.add(brand, BorderLayout.WEST);
        bar.add(identity, BorderLayout.EAST);
        return bar;
    }

    private void addTaskCard(JPanel parent, String title, String description, String actionLabel, Runnable action) {
        JPanel card = new JPanel(new BorderLayout(8, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 229, 236)),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 17f));
        JLabel details = new JLabel("<html><div style='width:250px'>" + description + "</div></html>");
        details.setForeground(new Color(92, 111, 128));
        JButton open = new JButton(actionLabel);
        open.setFocusPainted(false);
        open.addActionListener(e -> action.run());
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(heading); content.add(Box.createVerticalStrut(8)); content.add(details);
        card.add(content, BorderLayout.CENTER); card.add(open, BorderLayout.SOUTH);
        parent.add(card);
    }

    private JButton navigationButton(String text) {
        JButton button = new JButton(text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setPreferredSize(new Dimension(210, 42));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(219, 228, 235)),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        return button;
    }

    private JPanel profilePage() {
        JPanel panel = new JPanel(new GridLayout(3, 1));
        panel.add(new JLabel("My Profile"));
        panel.add(new JLabel("Name: " + staffName));
        panel.add(new JLabel("Role: " + role));
        return panel;
    }

    private JPanel rolePage() {
        if (isClinicalRole()) {
            return new ClinicalWorkspace(role, staffName,
                    () -> layout.show(mainPanel, "Medical Records"),
                    () -> layout.show(mainPanel, "Appointments"));
        }
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel(role + " Workspace"), BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(0, 1, 10, 10));
        if (isReceptionist()) {
            addAction(actions, "Register Patient / Print Barcode", "Register Patient");
            addAction(actions, "Appointments", "Appointments");
            addAction(actions, "Billing / Print Invoice", "Billing");
        } else if (isAdminRole()) {
            addAdminAction(actions, "Onboard", "Onboard");
            addAdminAction(actions, "Offboard", "Offboard");
            addAdminAction(actions, "Grant Remote Access", "Grant remote access");
            addAdminAction(actions, "Revoke Remote Access", "Revoke remote access");
            addAdminAction(actions, "Manage Accounts", "Manage accounts");
            addAdminAction(actions, "Audit Trail", "Audit trail");
        } else if (role.equalsIgnoreCase("Remote Worker")) {
            addAction(actions, "Patient Record Access", "Remote Access");
        } else {
            actions.add(new JLabel("No workflow is configured for this role."));
        }
        panel.add(actions, BorderLayout.CENTER);
        return panel;
    }

    private void addAction(JPanel parent, String title, String card) {
        JButton button = navigationButton(title);
        button.addActionListener(e -> layout.show(mainPanel, card));
        parent.add(button);
    }

    private void addAdminAction(JPanel parent, String title, String tab) {
        JButton button = navigationButton(title);
        button.addActionListener(e -> {
            adminPanel.showTab(tab);
            layout.show(mainPanel, "System Administration");
        });
        parent.add(button);
    }

    private void addAdminNav(JPanel parent, String title, String tab) {
        JButton button = navigationButton(title);
        button.addActionListener(e -> {
            adminPanel.showTab(tab);
            layout.show(mainPanel, "System Administration");
        });
        parent.add(button);
        parent.add(Box.createVerticalStrut(5));
    }

    private JPanel searchPage() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField first = new JTextField(10);
        JTextField last = new JTextField(10);
        JTextField dob = new JTextField(10);
        JButton search = new JButton("Search");
        JButton open = new JButton(isReceptionist() ? "Open billing" : "Open patient records");
        controls.add(new JLabel("First name:")); controls.add(first);
        controls.add(new JLabel("Last name:")); controls.add(last);
        controls.add(new JLabel("DOB (YYYY-MM-DD):")); controls.add(dob);
        controls.add(search); controls.add(open);
        DefaultTableModel results = new DefaultTableModel(new Object[]{"ID", "Name", "DOB", "Phone"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(results);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Runnable openSelected = () -> {
            int selected = table.getSelectedRow();
            if (selected < 0) { JOptionPane.showMessageDialog(this, "Select a patient first."); return; }
            int row = table.convertRowIndexToModel(selected);
            String id = String.valueOf(results.getValueAt(row, 0));
            if (recordsPanel != null) {
                recordsPanel.openPatient(id);
                layout.show(mainPanel, "Medical Records");
            } else if (billingPanel != null) {
                billingPanel.openPatient(id);
                layout.show(mainPanel, "Billing");
            } else if (remotePanel != null) {
                JOptionPane.showMessageDialog(this, "Patient details are only available through a valid remote-access grant.");
            } else {
                JOptionPane.showMessageDialog(this, "Your role does not have access to patient records.");
            }
        };
        open.addActionListener(e -> openSelected.run());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) openSelected.run();
            }
        });
        search.addActionListener(e -> {
            results.setRowCount(0);
            String firstName = first.getText().trim();
            String lastName = last.getText().trim();
            String birth = dob.getText().trim();
            if (firstName.isEmpty() && lastName.isEmpty() && birth.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a name or date of birth to search."); return;
            }
            try {
                List<String[]> patients = searchPatient(firstName, lastName, birth);
                for (String[] patient : patients) results.addRow(patient);
                if (patients.isEmpty()) JOptionPane.showMessageDialog(this, "No patients found.");
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Enter date of birth as YYYY-MM-DD.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Search failed: " + ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private List<String[]> searchPatient(String first, String last, String birth) throws SQLException {
        java.sql.Date date = birth.isEmpty() ? null : java.sql.Date.valueOf(birth);
        String sql = "SELECT patient_id, first_name, last_name, date_of_birth, phone FROM Patient " +
                "WHERE (? = '' OR LOWER(first_name)=LOWER(?)) AND (? = '' OR LOWER(last_name)=LOWER(?)) " +
                "AND (? IS NULL OR date_of_birth=?) ORDER BY last_name, first_name";
        List<String[]> patients = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setString(1, first); q.setString(2, first); q.setString(3, last); q.setString(4, last);
            if (date == null) { q.setNull(5, Types.DATE); q.setNull(6, Types.DATE); }
            else { q.setDate(5, date); q.setDate(6, date); }
            try (ResultSet r = q.executeQuery()) {
                while (r.next()) patients.add(new String[]{r.getString("patient_id"),
                        r.getString("first_name") + " " + r.getString("last_name"),
                        r.getString("date_of_birth"), r.getString("phone")});
            }
        }
        return patients;
    }

    private boolean isClinicalRole() { return role.equalsIgnoreCase("Doctor") || role.equalsIgnoreCase("Nurse"); }
    private boolean isReceptionist() { return role.equalsIgnoreCase("Receptionist"); }
    private boolean isAdminRole() { return role.equalsIgnoreCase("System Administrator") || role.equalsIgnoreCase("Administrator"); }
}
