package views;

import database.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.LongConsumer;

/** Role-filtered appointment list; receptionists can book and remove appointments. */
public class AppointmentsPanel extends JPanel {
    private final String role;
    private final long staffId;
    private final LongConsumer printPatientBarcode;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Appointment", "Patient ID", "Patient", "Doctor", "Nurse", "Date and time", "Status", "Reason", "Visit ID", "Visit status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    public AppointmentsPanel(String role, long staffId) {
        this(role, staffId, null);
    }

    public AppointmentsPanel(String role, long staffId, LongConsumer printPatientBarcode) {
        this.role = role;
        this.staffId = staffId;
        this.printPatientBarcode = printPatientBarcode;
        setLayout(new BorderLayout(8, 8));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refresh = new JButton("Refresh appointments");
        actions.add(refresh);
        if (role.equalsIgnoreCase("Receptionist")) {
            JButton checkIn = new JButton("Check in selected");
            JButton checkOut = new JButton("Check out / create invoice");
            JButton book = new JButton("Book appointment");
            JButton assignNurse = new JButton("Assign / change nurse");
            JButton delete = new JButton("Delete / cancel selected");
            actions.add(checkIn);
            actions.add(checkOut);
            actions.add(book);
            actions.add(assignNurse);
            actions.add(delete);
            checkIn.addActionListener(e -> checkInSelected());
            checkOut.addActionListener(e -> checkOutSelected());
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
                "CONCAT(n.first_name,' ',n.last_name) AS nurse_name, a.scheduled_at, a.appt_status, a.reason, v.visit_id, v.visit_status " +
                "FROM Appointment a LEFT JOIN Patient p ON p.patient_id=a.patient_id " +
                "JOIN Staff d ON d.staff_id=a.doctor_id LEFT JOIN Staff n ON n.staff_id=a.nurse_id " +
                "LEFT JOIN Visit v ON v.appt_id=a.appt_id " +
                "WHERE a.appt_status <> 'CANCELLED'" + roleFilter + " ORDER BY a.scheduled_at";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            if (!roleFilter.isEmpty()) q.setLong(1, staffId);
            try (ResultSet r = q.executeQuery()) {
                while (r.next()) model.addRow(new Object[]{r.getLong("appt_id"), r.getObject("patient_id"),
                        r.getString("patient_name"), r.getString("doctor_name"), r.getString("nurse_name"),
                        r.getTimestamp("scheduled_at"), r.getString("appt_status"), r.getString("reason"),
                        r.getObject("visit_id"), r.getString("visit_status")});
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void checkInSelected() {
        int row = selectedModelRow();
        if (row < 0) { showMessage("Select an appointment first."); return; }
        Object patientValue = model.getValueAt(row, 1);
        if (patientValue == null) {
            showMessage("Register this walk-in as a patient before check-in so the visit can be billed.");
            return;
        }
        if (model.getValueAt(row, 8) != null) { showMessage("This appointment already has a visit."); return; }
        long appointmentId = ((Number) model.getValueAt(row, 0)).longValue();
        try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL log_visit_check_in(?, ?, ?)}")) {
            q.setNull(1, Types.BIGINT); // Visit IDs are allocated by MySQL.
            q.setLong(2, appointmentId);
            q.setLong(3, staffId);
            q.execute();
            loadAppointments();
            if (printPatientBarcode != null) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "Patient checked in. Print the patient-ID wristband barcode now?",
                        "Check-in complete", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) printPatientBarcode.accept(((Number) patientValue).longValue());
            } else {
                showMessage("Patient checked in.");
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void checkOutSelected() {
        int row = selectedModelRow();
        if (row < 0) { showMessage("Select an appointment first."); return; }
        Object visitValue = model.getValueAt(row, 8);
        String visitStatus = String.valueOf(model.getValueAt(row, 9));
        if (visitValue == null || !"CHECKED IN".equalsIgnoreCase(visitStatus)) {
            showMessage("Only a checked-in visit can be checked out.");
            return;
        }
        long visitId = ((Number) visitValue).longValue();
        long appointmentId = ((Number) model.getValueAt(row, 0)).longValue();
        try (Connection c = DatabaseConnection.getConnection()) {
            try (CallableStatement q = c.prepareCall("{CALL log_visit_check_out(?)}")) {
                q.setLong(1, visitId);
                q.execute(); // The database trigger creates the $10 visit completion invoice.
            }
            Path invoiceFile = saveVisitInvoice(c, appointmentId);
            loadAppointments();
            int choice = JOptionPane.showConfirmDialog(this,
                    "Visit checked out. The $10 invoice was saved to:\n" + invoiceFile + "\n\nOpen the printable invoice now?",
                    "Invoice created", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(invoiceFile.toUri());
            }
        } catch (SQLException ex) {
            showError(ex);
        } catch (IOException ex) {
            loadAppointments();
            showMessage("Visit checked out and the $10 invoice was created, but its printable file could not be saved: " + ex.getMessage());
        }
    }

    private Path saveVisitInvoice(Connection c, long appointmentId) throws SQLException, IOException {
        String sql = "SELECT i.invoice_id, i.patient_id, CONCAT(p.first_name, ' ', p.last_name) AS patient_name, " +
                "i.invoice_date, i.due_date, i.description, i.amount, i.status " +
                "FROM Invoice i JOIN Patient p ON p.patient_id=i.patient_id " +
                "WHERE i.appointment_id=? AND i.description='Visit completion fee' " +
                "ORDER BY i.invoice_id DESC LIMIT 1";
        try (PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, appointmentId);
            try (ResultSet r = q.executeQuery()) {
                if (!r.next()) throw new SQLException("Checkout completed, but its invoice was not created.");
                long invoiceId = r.getLong("invoice_id");
                Path directory = Path.of(System.getProperty("user.home"), "Documents", "Forceps", "Invoices");
                Files.createDirectories(directory);
                Path file = directory.resolve("Invoice-" + invoiceId + ".html");
                String html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>Invoice " + invoiceId +
                        "</title><style>body{font:16px Arial,sans-serif;max-width:760px;margin:40px auto;color:#222}" +
                        "h1{margin-bottom:4px}.muted{color:#666}.line{border-top:1px solid #bbb;margin:28px 0}" +
                        "table{width:100%;border-collapse:collapse}td{padding:9px;border-bottom:1px solid #ddd}" +
                        ".amount{text-align:right;font-weight:bold}@media print{button{display:none}body{margin:0}}</style></head><body>" +
                        "<button onclick=\"window.print()\">Print invoice</button><h1>Forceps Hospital</h1>" +
                        "<div class=\"muted\">Invoice</div><div class=\"line\"></div><table>" +
                        row("Invoice", Long.toString(invoiceId)) + row("Patient", escapeHtml(r.getString("patient_name")) +
                        " (ID " + r.getLong("patient_id") + ")") + row("Appointment", Long.toString(appointmentId)) +
                        row("Invoice date", r.getDate("invoice_date").toString()) + row("Due date", r.getDate("due_date").toString()) +
                        row("Status", escapeHtml(r.getString("status"))) + "</table><div class=\"line\"></div><table>" +
                        row(escapeHtml(r.getString("description")), String.format(java.util.Locale.ROOT, "$%.2f", r.getBigDecimal("amount"))) +
                        "</table><p class=\"muted\">Thank you.</p></body></html>";
                Files.writeString(file, html, StandardCharsets.UTF_8);
                return file;
            }
        }
    }

    private String row(String label, String value) {
        return "<tr><td>" + label + "</td><td class=\"amount\">" + value + "</td></tr>";
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private int selectedModelRow() {
        int selected = table.getSelectedRow();
        return selected < 0 ? -1 : table.convertRowIndexToModel(selected);
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
