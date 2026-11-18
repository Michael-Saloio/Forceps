package views;

import javax.swing.*;
import java.awt.*;

/** Role-specific clinical tools. Shared record navigation is provided by the dashboard. */
public class ClinicalWorkspace extends JPanel {
    public ClinicalWorkspace(String role, String name, long staffId) {
        setLayout(new BorderLayout(8, 8));
        add(new JLabel("Welcome " + name + " - " + role), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        JPanel actions = new JPanel(new GridLayout(3, 1, 10, 10));
        if (role.equalsIgnoreCase("Doctor")) {
            addButton(actions, "Treatments");
            addButton(actions, "Appointments");
        } else if (role.equalsIgnoreCase("Nurse")) {
            addButton(actions, "Patient Assignments");
            addButton(actions, "Vital Signs");
            addButton(actions, "Treatments");
        }
        tabs.addTab("Role tools", actions);
        add(tabs, BorderLayout.CENTER);
    }

    private void addButton(JPanel parent, String text) {
        JButton button = new JButton(text);
        parent.add(button);
        button.addActionListener(e -> {
            String info = JOptionPane.showInputDialog(this, "Enter " + text + " information:");
            if (info != null && !info.trim().isEmpty()) JOptionPane.showMessageDialog(this, text + ": " + info);
        });
    }
}
