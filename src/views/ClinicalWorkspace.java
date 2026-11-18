package views;
import javax.swing.*;
import java.awt.*;
public class ClinicalWorkspace extends JPanel {
    public ClinicalWorkspace(String role, String name) {
        setLayout(new GridLayout(4, 1, 10, 10));
        add(new JLabel("Welcome " + name + " - " + role));
        if (role.equalsIgnoreCase("Doctor")) {
            addButton("Patient Records");
            addButton("Treatments");
            addButton("Appointments");}
        if (role.equalsIgnoreCase("Nurse")) {
            addButton("Patient Assignments");
            addButton("Vital Signs");
            addButton("Treatments");}}
    void addButton(String text) {
        JButton button = new JButton(text);
        add(button);
        button.addActionListener(e -> {
            String info = JOptionPane.showInputDialog(
                "Enter " + text + " information:");
            if (info != null && !info.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this, text + ": " + info);}});}}
