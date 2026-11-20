package views;

import javax.swing.*;
import java.awt.*;

/** Doctor and nurse actions connected to patient charts and assigned appointments. */
public class ClinicalWorkspace extends JPanel {
    public ClinicalWorkspace(String role, String name, Runnable openRecords, Runnable openAppointments) {
        setLayout(new BorderLayout(8, 8));
        add(new JLabel("Welcome " + name + " - " + role), BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(0, 1, 10, 10));
        if (role.equalsIgnoreCase("Doctor")) {
            addAction(actions, "Patient Records", openRecords);
            addAction(actions, "Treatment Plan", openRecords);
            addAction(actions, "Appointments", openAppointments);
        } else if (role.equalsIgnoreCase("Nurse")) {
            addAction(actions, "Patient Assignments", openAppointments);
            addAction(actions, "Vital Signs", openRecords);
            addAction(actions, "Treatments", openRecords);
        }
        add(actions, BorderLayout.CENTER);
    }

    private void addAction(JPanel parent, String label, Runnable action) {
        JButton button = new JButton(label);
        button.addActionListener(e -> action.run());
        parent.add(button);
    }
}
