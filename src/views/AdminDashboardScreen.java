package views;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;

public class AdminDashBoardScreen extends JFrame {

    private String staffName;

    private JPanel mainPanel;
    private JPanel searchInputs;
    private JPanel searchAndTablePanel;
    private String selectedStaffId = null;

    JTextField staffId;
    JTextField firstName;
    JTextField lastName;

    JButton searchButton;
    JButton exportButton;
    JButton settingsButton;
    JButton addStaffButton;
    JButton viewRecordButton;
    JButton editRecordButton; 

    DefaultTableModel model;
    JTable table;

    // Constructor for AdminDashBoardScreen
    // Takes account name
    public AdminDashBoardScreen(String name) {
        staffName = name;

        setTitle("Forceps DBMS - System Admin Dashboard");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initializeComponents();
        setupListeners();
    }

    // Initializes and adds components to the JFrame
    private void initializeComponents() {
        mainPanel = createMainPanel();
        add(mainPanel); // Add mainPanel to the JFrame

        // Adds all sub-panels to mainPanel here
        mainPanel.add(createNorthPanel(), BorderLayout.NORTH);
        mainPanel.add(createSearchAndTablePanel(), BorderLayout.CENTER);
        mainPanel.add(createSouthPanel(), BorderLayout.SOUTH);
    }

    // Creates the main parent panel responsible for holding all other panels
    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 

        return panel;
    }

    // Creates the north panel cotnaining welcome message, options panel containing export data btn and settings btn
    private JPanel createNorthPanel() {
        JPanel northSectionPanel = new JPanel(new BorderLayout());
        northSectionPanel.setBorder(BorderFactory.createTitledBorder("System Information & Options"));
        
        JTextArea infoText = new JTextArea("Welcome " + staffName + " to the System Admin View.\nSelect an option or use the search tool to find and review records.");
        infoText.setEditable(false);
        infoText.setOpaque(false);
        northSectionPanel.add(new JScrollPane(infoText), BorderLayout.CENTER);

        JPanel optionsButtonPanel = new JPanel();
        exportButton = new JButton("Export Data");
        settingsButton = new JButton("Settings");
        optionsButtonPanel.add(exportButton);
        optionsButtonPanel.add(settingsButton);
        northSectionPanel.add(optionsButtonPanel, BorderLayout.EAST);

        exportButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        settingsButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return northSectionPanel;
    }

    // Creates the table and search panel 
    private JPanel createSearchAndTablePanel() {
        searchAndTablePanel = new JPanel(new BorderLayout(5, 5));
        searchAndTablePanel.setBorder(BorderFactory.createTitledBorder("Staff Search & Results"));

        // Search inputs at the top of this sub-panel
        searchInputs = new JPanel();
        staffId = new JTextField(15);
        firstName = new JTextField(15);
        lastName = new JTextField(15);
        searchButton = new JButton("Search");

        searchInputs.add(new JLabel("Staff ID:"));
        searchInputs.add(staffId);
        searchInputs.add(new JLabel("First Name:"));
        searchInputs.add(firstName);
        searchInputs.add(new JLabel("Last Name:"));
        searchInputs.add(lastName);
        searchInputs.add(searchButton);

        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Table for results
        String[] columns = { "Staff ID", "Role ID", "First Name", "Last Name", "Email", "Account Active?" };

        // Prevents editing any cell on click or double-click
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Prevents editing any cell on click/double-click
            }
        };
        table = new JTable(model);

        searchAndTablePanel.add(searchInputs, BorderLayout.NORTH);
        searchAndTablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        return searchAndTablePanel;
    }

    // Creates the south panel containing additional account management btns
    private JPanel createSouthPanel() {
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(BorderFactory.createTitledBorder("Staff Account Management"));
        
        JPanel optionsButtonPanel = new JPanel();

        // Create new staff account 
        addStaffButton = new JButton("Create New Staff Account");
        optionsButtonPanel.add(addStaffButton);
        addStaffButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // View record button
        viewRecordButton = new JButton("View");
        optionsButtonPanel.add(viewRecordButton).setEnabled(false);
        viewRecordButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Edit record button
        editRecordButton = new JButton("Edit Record");
        optionsButtonPanel.add(editRecordButton).setEnabled(false);
        southPanel.add(optionsButtonPanel, BorderLayout.EAST);

        return southPanel;
    }

    // Sets up listeners for user interactions with the screen btns components and table manipulation 
    // DATABASE LOGIC NEEDED HERE FOR 
    private void setupListeners() {
        searchButton.addActionListener(e -> {
            if (!validateInputs()) {
                return;
            }

            model.setRowCount(0);
            String id = staffId.getText().trim();
            String firstname = firstName.getText().trim();
            String lastname = lastName.getText().trim();

            try {
                List<String[]> staffMembers = searchStaff(id, firstname, lastname);

                for (String[] staffMember : staffMembers) {
                    model.addRow(staffMember);
                }

                if (staffMembers.isEmpty()) {
                    JOptionPane.showMessageDialog(AdminDashBoardScreen.this, "No staff members found.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(AdminDashBoardScreen.this, "Search failed: " + ex.getMessage());
            }
        });

        // Controls view/edit buttons based on table selection
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = table.getSelectedRow();
                boolean hasSelection = (selectedRow != -1);

                if (hasSelection) {
                    selectedStaffId = model.getValueAt(selectedRow, 0).toString();
                } else {
                    selectedStaffId = null;
                }

                // Enable/Disable buttons based on selection status
                viewRecordButton.setEnabled(hasSelection);
                editRecordButton.setEnabled(hasSelection);
            }
        });

        // Listens for double-clicks to open view mode and cleaning selection on blank space ON TABLE
        table.addMouseListener(new MouseAdapter() {
            @Override 
            public void mousePressed(MouseEvent e) {
                // If user clicks on empty table space below rows, clear selection
                if (table.rowAtPoint(e.getPoint()) == -1) {
                    table.clearSelection();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1 && e.getClickCount() == 2) {
                    // FIX: Added .setVisible(true) so the window actually appears!
                    StaffDetailsScreen viewScreen = new StaffDetailsScreen(StaffDetailsScreen.FormMode.VIEW, selectedStaffId);
                    viewScreen.setVisible(true);
                }
            }
        });

        // Clears selection when clicking OUTSIDE the table on the panel
        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(mainPanel, e.getPoint(), table);
                if (!table.contains(p)) {
                    table.clearSelection();
                }
            }
        });

        // Opens Add New Staff screen
        addStaffButton.addActionListener(e -> {
            StaffDetailsScreen addScreen = new StaffDetailsScreen(StaffDetailsScreen.FormMode.ADD, null);
            addScreen.setVisible(true);
        });

        // Opens View mode
        viewRecordButton.addActionListener(e -> {
            if (selectedStaffId != null) {
                StaffDetailsScreen viewScreen = new StaffDetailsScreen(StaffDetailsScreen.FormMode.VIEW, selectedStaffId);
                viewScreen.setVisible(true);
            }
        });

        // Opens Edit mode
        editRecordButton.addActionListener(e -> {
            if (selectedStaffId != null) {
                StaffDetailsScreen editScreen = new StaffDetailsScreen(StaffDetailsScreen.FormMode.EDIT, selectedStaffId);
                editScreen.setVisible(true);
            }
        });
        
        // Future feature to be implemented, currently a placeholder
        exportButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "This feature is a work in progress.", "Export Data", JOptionPane.WARNING_MESSAGE);
        });

        // Future feature to be implemented, currently a placeholder
        settingsButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "This feature is a work in progress.", "Settings", JOptionPane.WARNING_MESSAGE);
        });
    }

    // Validates search criteria by checkign to see if at least one input was given
    private boolean validateInputs() {
        if (staffId.getText().trim().isEmpty() 
            && firstName.getText().trim().isEmpty() 
            && lastName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(AdminDashBoardScreen.this, "Please enter at least one search criteria.");
            return false;
        }
        return true;
    }

    // ArrayList to hold database query logic
    // TO DO: IMPLEMENT DATABASE LOGIC ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private List<String[]> searchStaff(String id, String firstname, String lastname) {
        List<String[]> results = new ArrayList<>();

        return results;
    }    
}