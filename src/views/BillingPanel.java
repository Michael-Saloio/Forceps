package views;

import database.DatabaseConnection;
import database.AuditLogger;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Receptionist workflow for searching, creating, correcting, paying, and printing invoices. */
public class BillingPanel extends JPanel {
    private final long staffId;
    private final JTextField patientId = new JTextField(10);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Invoice", "Patient ID", "Patient name", "Appointment", "Invoice date", "Description", "Amount", "Status", "Due date", "Paid date"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    public BillingPanel(long staffId) {
        this.staffId = staffId;
        setLayout(new BorderLayout(8, 8));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Patient ID (blank = all invoices):"));
        controls.add(patientId);
        JButton search = new JButton("Search invoices");
        JButton create = new JButton("Create invoice");
        JButton edit = new JButton("Edit unpaid");
        JButton paid = new JButton("Mark paid");
        JButton print = new JButton("Print invoice");
        controls.add(search); controls.add(create); controls.add(edit); controls.add(paid); controls.add(print);
        add(controls, BorderLayout.NORTH);

        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(new JLabel("Invoices are linked to the patient and optional appointment; paid invoices are retained for the audit trail."), BorderLayout.SOUTH);

        search.addActionListener(e -> loadInvoices());
        patientId.addActionListener(e -> loadInvoices());
        create.addActionListener(e -> createInvoice());
        edit.addActionListener(e -> editSelectedInvoice());
        paid.addActionListener(e -> markPaid());
        print.addActionListener(e -> printSelectedInvoice());
    }

    /** Open billing for a patient selected from the dashboard search. */
    public void openPatient(String id) {
        patientId.setText(id == null ? "" : id);
        loadInvoices();
    }

    /** List invoice details with patient names so reception staff do not work from IDs alone. */
    private void loadInvoices() {
        model.setRowCount(0);
        String id = patientId.getText().trim();
        if (!validId(id, true)) return;
        String sql = "SELECT i.invoice_id, i.patient_id, CONCAT(p.first_name, ' ', p.last_name) AS patient_name, " +
                "i.appointment_id, i.invoice_date, i.description, i.amount, i.status, i.due_date, i.paid_date " +
                "FROM Invoice i JOIN Patient p ON p.patient_id=i.patient_id " +
                "WHERE (? = '' OR i.patient_id = ?) ORDER BY i.invoice_date DESC, i.invoice_id DESC";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setString(1, id);
            if (id.isEmpty()) q.setNull(2, Types.BIGINT); else q.setLong(2, Long.parseLong(id));
            try (ResultSet r = q.executeQuery()) {
                while (r.next()) {
                    model.addRow(new Object[]{r.getLong("invoice_id"), r.getLong("patient_id"), r.getString("patient_name"),
                            r.getObject("appointment_id"), r.getDate("invoice_date"), r.getString("description"),
                            r.getBigDecimal("amount"), r.getString("status"), r.getDate("due_date"), r.getDate("paid_date")});
                }
            }
            AuditLogger.logBestEffort(c, staffId, id.isEmpty() ? null : Long.parseLong(id), "INVOICES_VIEWED", "INVOICE", null, "SUCCESS", "Invoice list opened.");
            if (model.getRowCount() == 0) JOptionPane.showMessageDialog(this, "No invoices found.");
        } catch (NumberFormatException ex) {
            showMessage("Enter a positive whole-number patient ID.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /** Record an invoice against a verified patient and, when provided, their own appointment. */
    private void createInvoice() {
        JTextField patient = new JTextField(patientId.getText().trim(), 16);
        JTextField appointment = new JTextField(12);
        JTextField description = new JTextField(26);
        JTextField amount = new JTextField(12);
        JTextField dueDate = new JTextField(LocalDate.now().plusDays(30).toString(), 16);
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Patient ID *")); form.add(patient);
        form.add(new JLabel("Appointment ID (optional)")); form.add(appointment);
        form.add(new JLabel("Description *")); form.add(description);
        form.add(new JLabel("Amount * (up to 2 decimal places)")); form.add(amount);
        form.add(new JLabel("Due date (YYYY-MM-DD) *")); form.add(dueDate);
        if (JOptionPane.showConfirmDialog(this, form, "New invoice", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        String patientKey = patient.getText().trim();
        String appointmentKey = appointment.getText().trim();
        if (!validId(patientKey, false) || !validId(appointmentKey, true)) return;
        if (description.getText().trim().isEmpty()) { showMessage("Invoice description is required."); return; }

        try {
            BigDecimal value = validAmount(amount.getText().trim());
            Date due = parseDate(dueDate.getText().trim(), "due date");
            long patientValue = Long.parseLong(patientKey);
            Long appointmentValue = appointmentKey.isEmpty() ? null : Long.parseLong(appointmentKey);

            try (Connection c = DatabaseConnection.getConnection()) {
                if (!patientExists(c, patientValue)) { showMessage("Patient " + patientKey + " was not found."); return; }
                if (appointmentValue != null && !appointmentBelongsToPatient(c, appointmentValue, patientValue)) {
                    showMessage("That appointment does not belong to the selected patient."); return;
                }
                try (CallableStatement q = c.prepareCall("{CALL create_invoice(?, ?, ?, ?, ?, ?)}")) {
                    q.setLong(1, patientValue);
                    if (appointmentValue == null) q.setNull(2, Types.BIGINT); else q.setLong(2, appointmentValue);
                    q.setLong(3, staffId);
                    q.setString(4, description.getText().trim());
                    q.setBigDecimal(5, value);
                    q.setDate(6, due);
                    q.execute();
                }
                AuditLogger.logBestEffort(c, staffId, patientValue, "INVOICE_CREATED", "PATIENT", Long.toString(patientValue), "SUCCESS", "Invoice created.");
            }
            patientId.setText(patientKey);
            loadInvoices();
            showMessage("Invoice created and saved.");
        } catch (NumberFormatException ex) {
            showMessage("Enter a positive amount with no more than two decimal places.");
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /** Only unpaid invoices can be edited; patient and creator remain immutable. */
    private void editSelectedInvoice() {
        int row = selectedModelRow();
        if (row < 0) { showMessage("Select an invoice first."); return; }
        String status = String.valueOf(model.getValueAt(row, 7));
        if (!"UNPAID".equalsIgnoreCase(status)) { showMessage("Only unpaid invoices can be edited."); return; }

        long invoiceId = ((Number) model.getValueAt(row, 0)).longValue();
        long patientKey = ((Number) model.getValueAt(row, 1)).longValue();
        JTextField appointment = new JTextField(textAt(row, 3), 12);
        JTextField description = new JTextField(textAt(row, 5), 26);
        JTextField amount = new JTextField(textAt(row, 6), 12);
        JTextField dueDate = new JTextField(textAt(row, 8), 16);
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Appointment ID (optional)")); form.add(appointment);
        form.add(new JLabel("Description *")); form.add(description);
        form.add(new JLabel("Amount * (up to 2 decimal places)")); form.add(amount);
        form.add(new JLabel("Due date (YYYY-MM-DD) *")); form.add(dueDate);
        if (JOptionPane.showConfirmDialog(this, form, "Edit unpaid invoice", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        String appointmentKey = appointment.getText().trim();
        if (!validId(appointmentKey, true)) return;
        if (description.getText().trim().isEmpty()) { showMessage("Invoice description is required."); return; }
        try {
            BigDecimal value = validAmount(amount.getText().trim());
            Date due = parseDate(dueDate.getText().trim(), "due date");
            Long appointmentValue = appointmentKey.isEmpty() ? null : Long.parseLong(appointmentKey);
            try (Connection c = DatabaseConnection.getConnection()) {
                if (appointmentValue != null && !appointmentBelongsToPatient(c, appointmentValue, patientKey)) {
                    showMessage("That appointment does not belong to the invoice's patient."); return;
                }
                try (CallableStatement q = c.prepareCall("{CALL update_invoice(?, ?, ?, ?, ?)}")) {
                    q.setLong(1, invoiceId);
                    if (appointmentValue == null) q.setNull(2, Types.BIGINT); else q.setLong(2, appointmentValue);
                    q.setString(3, description.getText().trim()); q.setBigDecimal(4, value);
                    q.setDate(5, due);
                    q.execute();
                }
                AuditLogger.logBestEffort(c, staffId, patientKey, "INVOICE_UPDATED", "INVOICE", Long.toString(invoiceId), "SUCCESS", "Unpaid invoice updated.");
            }
            loadInvoices();
            showMessage("Invoice updated.");
        } catch (NumberFormatException ex) {
            showMessage("Enter a positive amount with no more than two decimal places.");
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /** Mark payment once and record the receptionist and date for the database audit trail. */
    private void markPaid() {
        int row = selectedModelRow();
        if (row < 0) { showMessage("Select an invoice first."); return; }
        if (!"UNPAID".equalsIgnoreCase(String.valueOf(model.getValueAt(row, 7)))) {
            showMessage("This invoice is already paid."); return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Mark invoice " + model.getValueAt(row, 0) + " as paid?",
                "Confirm payment", JOptionPane.OK_CANCEL_OPTION);
        if (confirm != JOptionPane.OK_OPTION) return;

        long invoiceId = ((Number) model.getValueAt(row, 0)).longValue();
        try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL mark_invoice_paid(?, ?)}")) {
            q.setLong(1, invoiceId); q.setLong(2, staffId);
            q.execute();
            AuditLogger.logBestEffort(c, staffId, ((Number)model.getValueAt(row, 1)).longValue(), "INVOICE_PAID", "INVOICE", Long.toString(invoiceId), "SUCCESS", "Invoice payment recorded.");
            loadInvoices();
            showMessage("Payment recorded.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /** Format the selected saved invoice and open the native print dialog (including Save as PDF). */
    private void printSelectedInvoice() {
        int row = selectedModelRow();
        if (row < 0) { showMessage("Select an invoice to print."); return; }
        StringBuilder text = new StringBuilder();
        text.append("FORCEPS HOSPITAL\nINVOICE\n\n");
        text.append("Invoice: ").append(model.getValueAt(row, 0)).append('\n');
        text.append("Patient: ").append(model.getValueAt(row, 2)).append(" (ID ").append(model.getValueAt(row, 1)).append(")\n");
        text.append("Appointment: ").append(valueOrNone(model.getValueAt(row, 3))).append('\n');
        text.append("Invoice date: ").append(valueOrNone(model.getValueAt(row, 4))).append('\n');
        text.append("Due date: ").append(valueOrNone(model.getValueAt(row, 8))).append("\n\n");
        text.append("Description: ").append(model.getValueAt(row, 5)).append('\n');
        text.append("Amount: ").append(String.format(java.util.Locale.ROOT, "%.2f", ((Number) model.getValueAt(row, 6)).doubleValue())).append('\n');
        text.append("Status: ").append(model.getValueAt(row, 7)).append('\n');
        text.append("Paid date: ").append(valueOrNone(model.getValueAt(row, 9))).append('\n');
        text.append("\nThis invoice is a project demonstration record.");

        JTextArea printable = new JTextArea(text.toString());
        printable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        try {
            if (!printable.print(null, null, true, null, null, true)) showMessage("Printing was cancelled.");
            else AuditLogger.logBestEffort(staffId, ((Number)model.getValueAt(row, 1)).longValue(), "INVOICE_PRINTED", "INVOICE", String.valueOf(model.getValueAt(row, 0)), "SUCCESS", "Invoice sent to print dialog.");
        } catch (PrinterException ex) {
            showMessage("Unable to print invoice: " + ex.getMessage());
        }
    }

    private boolean patientExists(Connection c, long id) throws SQLException {
        try (PreparedStatement q = c.prepareStatement("SELECT 1 FROM Patient WHERE patient_id=?")) {
            q.setLong(1, id);
            try (ResultSet r = q.executeQuery()) { return r.next(); }
        }
    }

    private boolean appointmentBelongsToPatient(Connection c, long appointmentId, long patientId) throws SQLException {
        try (PreparedStatement q = c.prepareStatement("SELECT 1 FROM Appointment WHERE appt_id=? AND patient_id=?")) {
            q.setLong(1, appointmentId); q.setLong(2, patientId);
            try (ResultSet r = q.executeQuery()) { return r.next(); }
        }
    }

    /** Return selected row in model coordinates so sorting cannot update or print another invoice. */
    private int selectedModelRow() {
        int row = table.getSelectedRow();
        return row < 0 ? -1 : table.convertRowIndexToModel(row);
    }

    private String textAt(int row, int column) {
        Object value = model.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private String valueOrNone(Object value) {
        return value == null || value.toString().trim().isEmpty() ? "-" : value.toString();
    }

    private BigDecimal validAmount(String input) {
        BigDecimal value = new BigDecimal(input);
        if (value.signum() <= 0 || value.scale() > 2) throw new NumberFormatException();
        return value;
    }

    private Date parseDate(String input, String label) {
        try { return Date.valueOf(LocalDate.parse(input)); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("Enter a valid " + label + " in YYYY-MM-DD format."); }
    }

    private boolean validId(String value, boolean allowEmpty) {
        if (allowEmpty && value.isEmpty()) return true;
        try { if (Long.parseLong(value) > 0) return true; } catch (NumberFormatException ignored) { }
        showMessage("Enter a positive whole-number ID.");
        return false;
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Billing operation failed: " + ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
    }
}
