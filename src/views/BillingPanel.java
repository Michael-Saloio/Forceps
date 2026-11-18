package views;

import database.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

/** Create patient invoices and track whether they have been paid. */
public class BillingPanel extends JPanel {
    private final long staffId;
    private final JTextField patientId = new JTextField(10);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Invoice", "Patient", "Date", "Description", "Amount", "Status", "Due date"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    public BillingPanel(long staffId) {
        this.staffId = staffId;
        setLayout(new BorderLayout(8, 8));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Patient ID:")); controls.add(patientId);
        JButton search = new JButton("Search invoices");
        JButton create = new JButton("Create invoice");
        JButton paid = new JButton("Mark selected paid");
        controls.add(search); controls.add(create); controls.add(paid);
        add(controls, BorderLayout.NORTH);
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        search.addActionListener(e -> loadInvoices());
        create.addActionListener(e -> createInvoice());
        paid.addActionListener(e -> markPaid(table));
    }

    private void loadInvoices() {
        model.setRowCount(0);
        String id = patientId.getText().trim();
        if (!validId(id, true)) return;
        String sql = "SELECT invoice_id, patient_id, invoice_date, description, amount, status, due_date " +
                "FROM Invoice WHERE (? = '' OR patient_id = ?) ORDER BY invoice_date DESC, invoice_id DESC";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setString(1, id);
            if (id.isEmpty()) q.setNull(2, Types.BIGINT); else q.setLong(2, Long.parseLong(id));
            try (ResultSet r = q.executeQuery()) {
                while (r.next()) model.addRow(new Object[]{r.getLong("invoice_id"), r.getLong("patient_id"),
                        r.getDate("invoice_date"), r.getString("description"), r.getBigDecimal("amount"),
                        r.getString("status"), r.getDate("due_date")});
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void createInvoice() {
        JTextField patient = new JTextField(patientId.getText().trim(), 16);
        JTextField description = new JTextField(24);
        JTextField amount = new JTextField(12);
        JTextField dueDate = new JTextField(LocalDate.now().plusDays(30).toString(), 16);
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Patient ID *")); form.add(patient);
        form.add(new JLabel("Description *")); form.add(description);
        form.add(new JLabel("Amount *")); form.add(amount);
        form.add(new JLabel("Due date (YYYY-MM-DD) *")); form.add(dueDate);
        if (JOptionPane.showConfirmDialog(this, form, "New invoice", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String id = patient.getText().trim();
        if (!validId(id, false)) return;
        if (description.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Invoice description is required.");
            return;
        }
        try {
            BigDecimal value = new BigDecimal(amount.getText().trim());
            if (value.signum() <= 0 || value.scale() > 2) throw new NumberFormatException();
            Date due = Date.valueOf(dueDate.getText().trim());
            String sql = "INSERT INTO Invoice(patient_id, created_by_staff_id, invoice_date, description, amount, status, due_date) " +
                    "VALUES(?,?,CURRENT_DATE,?,?, 'UNPAID',?)";
            try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
                q.setLong(1, Long.parseLong(id)); q.setLong(2, staffId); q.setString(3, description.getText().trim());
                q.setBigDecimal(4, value); q.setDate(5, due); q.executeUpdate();
            }
            patientId.setText(id);
            loadInvoices();
            JOptionPane.showMessageDialog(this, "Invoice created.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a positive amount with up to two decimal places.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid due date in YYYY-MM-DD format.");
        } catch (SQLException ex) { showError(ex); }
    }

    private void markPaid(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an invoice first."); return; }
        int modelRow = table.convertRowIndexToModel(row);
        long invoiceId = ((Number) model.getValueAt(modelRow, 0)).longValue();
        String sql = "UPDATE Invoice SET status='PAID', paid_date=CURRENT_DATE, paid_by_staff_id=? WHERE invoice_id=? AND status='UNPAID'";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, staffId);
            q.setLong(2, invoiceId);
            if (q.executeUpdate() == 0) JOptionPane.showMessageDialog(this, "Invoice is already paid or no longer exists.");
            loadInvoices();
        } catch (SQLException ex) { showError(ex); }
    }

    private boolean validId(String value, boolean allowEmpty) {
        if (allowEmpty && value.isEmpty()) return true;
        try { if (Long.parseLong(value) > 0) return true; } catch (NumberFormatException ignored) { }
        JOptionPane.showMessageDialog(this, "Patient ID must be a positive whole number.");
        return false;
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Database operation failed: " + ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
    }
}
