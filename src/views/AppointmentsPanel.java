package views;

import database.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/** Role-filtered appointment list; receptionists can book and remove appointments. */
public class AppointmentsPanel extends JPanel {
    private final String role;
    private final long staffId;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Appointment", "Patient ID", "Patient", "Doctor", "Nurse", "Date and time", "Status", "Reason"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    public AppointmentsPanel(String role, long staffId) {
        this.role = role;
        this.staffId = staffId;
        setLayout(new BorderLayout(8, 8));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refresh = new JButton("Refresh appointments");
        actions.add(refresh);
        if (role.equalsIgnoreCase("Receptionist")) {
            JButton book = new JButton("Book appointment");
            JButton assignNurse = new JButton("Assign / change nurse");
            JButton delete = new JButton("Delete / cancel selected");
            actions.add(book);
            actions.add(assignNurse);
            actions.add(delete);
            book.addActionListener(e -> bookAppointment());
            assignNurse.addActionListener(e -> assignNurse());
            delete.addActionListener(e -> deleteSelected());
        }
        add(actions, BorderLayout.NORTH);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh.addActionListener(e -> loadAppointments());
        loadAppointments();
    }

    private void loadAppointments() {
        model.setRowCount(0);
        String roleFilter = role.equalsIgnoreCase("Doctor") ? " AND a.doctor_id=?" :
                role.equalsIgnoreCase("Nurse") ? " AND a.nurse_id=?" : "";
        String sql = "SELECT a.appt_id, a.patient_id, " +
                "COALESCE(CONCAT(p.first_name,' ',p.last_name), CONCAT(a.prospective_first_name,' ',a.prospective_last_name)) AS patient_name, " +
                "CONCAT(d.first_name,' ',d.last_name) AS doctor_name, " +
                "CONCAT(n.first_name,' ',n.last_name) AS nurse_name, a.scheduled_at, a.appt_status, a.reason " +
                "FROM Appointment a LEFT JOIN Patient p ON p.patient_id=a.patient_id " +
                "JOIN Staff d ON d.staff_id=a.doctor_id LEFT JOIN Staff n ON n.staff_id=a.nurse_id " +
                "WHERE a.appt_status <> 'CANCELLED'" + roleFilter + " ORDER BY a.scheduled_at";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            if (!roleFilter.isEmpty()) q.setLong(1, staffId);
            try (ResultSet r = q.executeQuery()) {
                while (r.next()) model.addRow(new Object[]{r.getLong("appt_id"), r.getObject("patient_id"),
                        r.getString("patient_name"), r.getString("doctor_name"), r.getString("nurse_name"),
                        r.getTimestamp("scheduled_at"), r.getString("appt_status"), r.getString("reason")});
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void bookAppointment() {
        JTextField patientId = new JTextField(10);
        JComboBox<StaffOption> doctor = new JComboBox<>();
        JComboBox<StaffOption> nurse = new JComboBox<>();
        JTextField date = new JTextField(LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0).toString(), 18);
        JTextField reason = new JTextField(24);
        try {
            loadStaffOptions("Doctor", doctor);
            loadStaffOptions("Nurse", nurse);
        } catch (SQLException ex) { showError(ex); return; }
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Patient ID")); form.add(patientId);
        form.add(new JLabel("Doctor")); form.add(doctor);
        form.add(new JLabel("Assigned nurse")); form.add(nurse);
        form.add(new JLabel("Date/time (YYYY-MM-DDTHH:MM)")); form.add(date);
        form.add(new JLabel("Reason")); form.add(reason);
        if (JOptionPane.showConfirmDialog(this, form, "Book appointment", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            long patient = Long.parseLong(patientId.getText().trim());
            if (patient <= 0 || doctor.getSelectedItem() == null || nurse.getSelectedItem() == null) throw new IllegalArgumentException("Select a patient, doctor, and nurse.");
            Timestamp scheduledAt = Timestamp.valueOf(LocalDateTime.parse(date.getText().trim()));
            try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL book_appointment(?, ?, ?, ?, ?, ?, ?)}")) {
                q.setNull(1, Types.BIGINT); // MySQL allocates the appointment ID.
                q.setLong(2, patient);
                q.setLong(3, ((StaffOption) doctor.getSelectedItem()).id());
                q.setLong(4, ((StaffOption) nurse.getSelectedItem()).id());
                q.setLong(5, staffId);
                q.setTimestamp(6, scheduledAt);
                q.setString(7, reason.getText().trim());
                q.execute();
            }
            loadAppointments();
            showMessage("Appointment booked.");
        } catch (NumberFormatException ex) {
            showMessage("Enter a positive numeric patient ID.");
        } catch (DateTimeParseException ex) {
            showMessage("Enter the date and time as YYYY-MM-DDTHH:MM.");
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
        } catch (SQLException ex) { showError(ex); }
    }

    private void deleteSelected() {
        int selected = table.getSelectedRow();
        if (selected < 0) { showMessage("Select an appointment first."); return; }
        int row = table.convertRowIndexToModel(selected);
        long appointmentId = ((Number) model.getValueAt(row, 0)).longValue();
        int choice = JOptionPane.showConfirmDialog(this,
                "Remove appointment " + appointmentId + "? If a visit is already linked, it will be cancelled to preserve visit history.",
                "Confirm appointment removal", JOptionPane.OK_CANCEL_OPTION);
        if (choice != JOptionPane.OK_OPTION) return;
        try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL delete_appointment(?)}")) {
            q.setLong(1, appointmentId);
            q.execute();
            loadAppointments();
            showMessage("Appointment removed or cancelled.");
        } catch (SQLException ex) { showError(ex); }
    }

    private void assignNurse() {
        int selected = table.getSelectedRow();
        if (selected < 0) { showMessage("Select an appointment first."); return; }
        int row = table.convertRowIndexToModel(selected);
        long appointmentId = ((Number) model.getValueAt(row, 0)).longValue();
        JComboBox<StaffOption> nurse = new JComboBox<>();
        try { loadStaffOptions("Nurse", nurse); }
        catch (SQLException ex) { showError(ex); return; }
        if (nurse.getItemCount() == 0) { showMessage("No active nurses are available."); return; }
        if (JOptionPane.showConfirmDialog(this, nurse, "Assign nurse to appointment " + appointmentId, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL assign_appointment_nurse(?, ?)}")) {
            q.setLong(1, appointmentId);
            q.setLong(2, ((StaffOption) nurse.getSelectedItem()).id());
            q.execute();
            loadAppointments();
            showMessage("Nurse assigned to appointment " + appointmentId + ".");
        } catch (SQLException ex) { showError(ex); }
    }

    private void loadStaffOptions(String roleName, JComboBox<StaffOption> target) throws SQLException {
        String sql = "SELECT s.staff_id, s.first_name, s.last_name FROM Staff s JOIN Role r ON r.role_id=s.role_id " +
                "WHERE r.role_name=? AND s.termination_date IS NULL ORDER BY s.last_name, s.first_name";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setString(1, roleName);
            try (ResultSet r = q.executeQuery()) {
                while (r.next()) target.addItem(new StaffOption(r.getLong("staff_id"), r.getString("first_name") + " " + r.getString("last_name")));
            }
        }
    }

    private record StaffOption(long id, String name) {
        @Override public String toString() { return name + " (" + id + ")"; }
    }

    private void showMessage(String message) { JOptionPane.showMessageDialog(this, message); }
    private void showError(SQLException ex) { JOptionPane.showMessageDialog(this, "Appointment operation failed: " + ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE); }
}
