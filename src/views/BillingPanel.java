package views;

import database.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

/** Create, update, search, and record payment status for patient invoices. */
public class BillingPanel extends JPanel {
    private final long staffId;
    private final JTextField patientId = new JTextField(10);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "Invoice", "Patient", "Appointment", "Date", "Description", "Amount", "Status", "Due date",
                    "Paid date" },
            0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public BillingPanel(long staffId) {
        this.staffId = staffId;
        setLayout(new BorderLayout(8, 8));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Patient ID:"));
        controls.add(patientId);
        JButton search = new JButton("Search invoices");
        JButton create = new JButton("Create invoice");
        JButton edit = new JButton("Edit unpaid invoice");
        JButton paid = new JButton("Mark selected paid");
        controls.add(search);
        controls.add(create);
        controls.add(edit);
        controls.add(paid);
        add(controls, BorderLayout.NORTH);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        search.addActionListener(e -> loadInvoices());
        create.addActionListener(e -> createInvoice());
        edit.addActionListener(e -> editSelectedInvoice());
        paid.addActionListener(e -> markPaid());
    }

    /** Load invoices for one patient, or all invoices when the filter is blank. */
    private void loadInvoices() {
        model.setRowCount(0);
        String id = patientId.getText().trim();
        if (!validId(id, true))
            return;
        String sql = "SELECT invoice_id, patient_id, appointment_id, invoice_date, description, amount, status, due_date, paid_date "
                +
                "FROM Invoice WHERE (? = '' OR patient_id = ?) ORDER BY invoice_date DESC, invoice_id DESC";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setString(1, id);
            if (id.isEmpty())
                q.setNull(2, Types.BIGINT);
            else
                q.setLong(2, Long.parseLong(id));
            try (ResultSet r = q.executeQuery()) {
                while (r.next())
                    model.addRow(new Object[] { r.getLong("invoice_id"), r.getLong("patient_id"),
                            r.getObject("appointment_id"), r.getDate("invoice_date"), r.getString("description"),
                            r.getBigDecimal("amount"), r.getString("status"), r.getDate("due_date"),
                            r.getDate("paid_date") });
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /**
     * Create an invoice and optionally link it to an appointment for the same
     * patient.
     */
    private void createInvoice() {
        JTextField patient = new JTextField(patientId.getText().trim(), 16);
        JTextField appointment = new JTextField(12);
        JTextField description = new JTextField(24);
        JTextField amount = new JTextField(12);
        JTextField dueDate = new JTextField(LocalDate.now().plusDays(30).toString(), 16);
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Patient ID *"));
        form.add(patient);
        form.add(new JLabel("Appointment ID (optional)"));
        form.add(appointment);
        form.add(new JLabel("Description *"));
        form.add(description);
        form.add(new JLabel("Amount *"));
        form.add(amount);
        form.add(new JLabel("Due date (YYYY-MM-DD) *"));
        form.add(dueDate);
        if (JOptionPane.showConfirmDialog(this, form, "New invoice",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return;
        String patientKey = patient.getText().trim();
        String appointmentKey = appointment.getText().trim();
        if (!validId(patientKey, false) || !validId(appointmentKey, true))
            return;
        if (description.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Invoice description is required.");
            return;
        }
        try {
            BigDecimal value = validAmount(amount.getText().trim());
            Date due = Date.valueOf(dueDate.getText().trim());
            if (!appointmentKey.isEmpty()
                    && !appointmentBelongsToPatient(Long.parseLong(appointmentKey), Long.parseLong(patientKey))) {
                JOptionPane.showMessageDialog(this, "That appointment does not belong to this patient.");
                return;
            }
            String sql = "INSERT INTO Invoice(patient_id, appointment_id, created_by_staff_id, invoice_date, description, amount, status, due_date) "
                    +
                    "VALUES(?,?,?,CURRENT_DATE,?,?, 'UNPAID',?)";
            try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
                q.setLong(1, Long.parseLong(patientKey));
                if (appointmentKey.isEmpty())
                    q.setNull(2, Types.BIGINT);
                else
                    q.setLong(2, Long.parseLong(appointmentKey));
                q.setLong(3, staffId);
                q.setString(4, description.getText().trim());
                q.setBigDecimal(5, value);
                q.setDate(6, due);
                q.executeUpdate();
            }
            patientId.setText(patientKey);
            loadInvoices();
            JOptionPane.showMessageDialog(this, "Invoice created.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a positive amount with up to two decimal places.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid due date in YYYY-MM-DD format.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /** Allow staff to correct an unpaid invoice; paid invoices stay fixed. */
    private void editSelectedInvoice() {
        int row = selectedModelRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an invoice first.");
            return;
        }
        String status = String.valueOf(model.getValueAt(row, 6));
        if (!"UNPAID".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Only unpaid invoices can be edited.");
            return;
        }
        long invoiceId = ((Number) model.getValueAt(row, 0)).longValue();
        long patientKey = ((Number) model.getValueAt(row, 1)).longValue();
        JTextField appointment = new JTextField(textAt(row, 2), 12);
        JTextField description = new JTextField(textAt(row, 4), 24);
        JTextField amount = new JTextField(textAt(row, 5), 12);
        JTextField dueDate = new JTextField(textAt(row, 7), 16);
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Appointment ID (optional)"));
        form.add(appointment);
        form.add(new JLabel("Description *"));
        form.add(description);
        form.add(new JLabel("Amount *"));
        form.add(amount);
        form.add(new JLabel("Due date (YYYY-MM-DD) *"));
        form.add(dueDate);
        if (JOptionPane.showConfirmDialog(this, form, "Edit invoice",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return;
        String appointmentKey = appointment.getText().trim();
        if (!validId(appointmentKey, true))
            return;
        if (description.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Invoice description is required.");
            return;
        }
        try {
            BigDecimal value = validAmount(amount.getText().trim());
            Date due = Date.valueOf(dueDate.getText().trim());
            if (!appointmentKey.isEmpty() && !appointmentBelongsToPatient(Long.parseLong(appointmentKey), patientKey)) {
                JOptionPane.showMessageDialog(this, "That appointment does not belong to this patient.");
                return;
            }
            String sql = "UPDATE Invoice SET appointment_id=?, description=?, amount=?, due_date=? WHERE invoice_id=? AND status='UNPAID'";
            try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
                if (appointmentKey.isEmpty())
                    q.setNull(1, Types.BIGINT);
                else
                    q.setLong(1, Long.parseLong(appointmentKey));
                q.setString(2, description.getText().trim());
                q.setBigDecimal(3, value);
                q.setDate(4, due);
                q.setLong(5, invoiceId);
                if (q.executeUpdate() == 0) {
                    JOptionPane.showMessageDialog(this, "Invoice is paid or no longer exists.");
                    return;
                }
            }
            loadInvoices();
            JOptionPane.showMessageDialog(this, "Invoice updated.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a positive amount with up to two decimal places.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid due date in YYYY-MM-DD format.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /** Mark the selected invoice paid and record which staff member handled it. */
    private void markPaid() {
        int row = selectedModelRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an invoice first.");
            return;
        }
        long invoiceId = ((Number) model.getValueAt(row, 0)).longValue();
        String sql = "UPDATE Invoice SET status='PAID', paid_date=CURRENT_DATE, paid_by_staff_id=? WHERE invoice_id=? AND status='UNPAID'";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, staffId);
            q.setLong(2, invoiceId);
            if (q.executeUpdate() == 0)
                JOptionPane.showMessageDialog(this, "Invoice is already paid or no longer exists.");
            loadInvoices();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /**
     * Return selected row in model coordinates so sorting cannot target another
     * invoice.
     */
    private int selectedModelRow() {
        int row = table.getSelectedRow();
        return row < 0 ? -1 : table.convertRowIndexToModel(row);
    }

    private String textAt(int row, int column) {
        Object value = model.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private BigDecimal validAmount(String input) {
        BigDecimal value = new BigDecimal(input);
        if (value.signum() <= 0 || value.scale() > 2)
            throw new NumberFormatException();
        return value;
    }

    /** Ensure any supplied appointment belongs to the invoice's patient. */
    private boolean appointmentBelongsToPatient(long appointmentId, long patientId) throws SQLException {
        String sql = "SELECT 1 FROM Appointment WHERE appt_id=? AND patient_id=?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, appointmentId);
            q.setLong(2, patientId);
            try (ResultSet r = q.executeQuery()) {
                return r.next();
            }
        }
    }

    private boolean validId(String value, boolean allowEmpty) {
        if (allowEmpty && value.isEmpty())
            return true;
        try {
            if (Long.parseLong(value) > 0)
                return true;
        } catch (NumberFormatException ignored) {
        }
        JOptionPane.showMessageDialog(this, "Enter a positive whole-number ID.");
        return false;
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Database operation failed: " + ex.getMessage(), "Database error",
                JOptionPane.ERROR_MESSAGE);
    }
}