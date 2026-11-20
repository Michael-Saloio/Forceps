package views;

import database.DatabaseConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class AppointmentsScreen extends JFrame implements ActionListener {
    JTextField patient, doctor, date;
    JButton addButton, viewButton, deleteButton;
    JTextArea area;

    public AppointmentsScreen() {
        setTitle("Appointments");
        setSize(500, 400);
        setLocationRelativeTo(null);
        patient = new JTextField();
        doctor = new JTextField();
        date = new JTextField();
        addButton = new JButton("Add");
        viewButton = new JButton("View");
        deleteButton = new JButton("Delete");
        area = new JTextArea();
        area.setEditable(false);
        JPanel panel = new JPanel(new GridLayout(4, 2));
        panel.add(new JLabel("Patient ID"));
        panel.add(patient);
        panel.add(new JLabel("Doctor ID"));
        panel.add(doctor);
        panel.add(new JLabel("Date and Time"));
        panel.add(date);
        panel.add(addButton);
        panel.add(viewButton);
        add(panel, BorderLayout.NORTH);
        add(new JScrollPane(area), BorderLayout.CENTER);
        add(deleteButton, BorderLayout.SOUTH);
        addButton.addActionListener(this);
        viewButton.addActionListener(this);
        deleteButton.addActionListener(this);
        viewAppointments();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == addButton) {
            addAppointment();
        }
        if (e.getSource() == viewButton) {
            viewAppointments();
        }
        if (e.getSource() == deleteButton) {
            deleteAppointment();
        }
    }

    void addAppointment() {
        try {
            Connection con = DatabaseConnection.getConnection();
            String sql = "INSERT INTO Appointment " +
                    "(patient_id, doctor_id, scheduled_at) " +
                    "VALUES (?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, Integer.parseInt(patient.getText()));
            ps.setInt(2, Integer.parseInt(doctor.getText()));
            ps.setString(3, date.getText());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Appointment Added");
            ps.close();
            con.close();
            viewAppointments();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    void viewAppointments() {
        area.setText("");
        try {
            Connection con = DatabaseConnection.getConnection();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM Appointment");
            while (rs.next()) {
                area.append(
                        "ID: " + rs.getInt("appt_id") +
                                " | Patient: " + rs.getInt("patient_id") +
                                " | Doctor: " + rs.getInt("doctor_id") +
                                " | Date: " + rs.getString("scheduled_at") +
                                "\n");
            }
            rs.close();
            st.close();
            con.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    void deleteAppointment() {
        String id = JOptionPane.showInputDialog(this, "Appointment ID:");
        if (id == null || id.isEmpty()) {
            return;
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            String sql = "DELETE FROM Appointment WHERE appt_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, Integer.parseInt(id));
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Appointment Deleted");
            ps.close();
            con.close();
            viewAppointments();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }
}
