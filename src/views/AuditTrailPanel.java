package views;

import database.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/** System-admin-only, read-only audit search. */
public final class AuditTrailPanel extends JPanel {
    private final JTextField staffId = new JTextField(9);
    private final JTextField patientId = new JTextField(9);
    private final JTextField actionType = new JTextField(12);
    private final JTextField fromDate = new JTextField(10);
    private final JTextField toDate = new JTextField(10);
    private final JLabel status = new JLabel("Audit events are read-only.");
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Audit ID", "When", "Staff ID", "Patient ID", "Action", "Target", "Target ID", "Outcome", "Details"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    public AuditTrailPanel() {
        setLayout(new BorderLayout(8, 8));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton search = new JButton("Search audit history");
        filters.add(new JLabel("Staff ID")); filters.add(staffId);
        filters.add(new JLabel("Patient ID")); filters.add(patientId);
        filters.add(new JLabel("Action")); filters.add(actionType);
        filters.add(new JLabel("From (YYYY-MM-DD)")); filters.add(fromDate);
        filters.add(new JLabel("To (YYYY-MM-DD)")); filters.add(toDate);
        filters.add(search);
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        add(filters, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        search.addActionListener(e -> load());
        fromDate.addActionListener(e -> load());
        toDate.addActionListener(e -> load());
        load();
    }

    private void load() {
        model.setRowCount(0);
        String sql = "SELECT audit_id, occurred_at, staff_id, patient_id, action_type, target_type, target_id, outcome, details " +
                "FROM Audit_Log WHERE (? IS NULL OR staff_id=?) AND (? IS NULL OR patient_id=?) " +
                "AND (?='' OR action_type LIKE CONCAT('%', ?, '%')) " +
                "AND (? IS NULL OR occurred_at >= ?) AND (? IS NULL OR occurred_at < DATE_ADD(?, INTERVAL 1 DAY)) " +
                "ORDER BY occurred_at DESC, audit_id DESC LIMIT 1000";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            setOptionalLong(q, 1, 2, staffId.getText().trim());
            setOptionalLong(q, 3, 4, patientId.getText().trim());
            String action = actionType.getText().trim(); q.setString(5, action); q.setString(6, action);
            setOptionalDate(q, 7, 8, fromDate.getText().trim());
            setOptionalDate(q, 9, 10, toDate.getText().trim());
            try (ResultSet r = q.executeQuery()) {
                while (r.next()) model.addRow(new Object[]{r.getLong("audit_id"), r.getTimestamp("occurred_at"),
                        r.getLong("staff_id"), nullableLong(r, "patient_id"), r.getString("action_type"),
                        r.getString("target_type"), r.getString("target_id"), r.getString("outcome"), r.getString("details")});
            }
            status.setText(model.getRowCount() + " audit events shown (maximum 1,000). Records are read-only.");
        } catch (NumberFormatException ex) {
            status.setText("Staff and patient IDs must be positive whole numbers.");
        } catch (IllegalArgumentException ex) {
            status.setText("Enter filter dates as YYYY-MM-DD.");
        } catch (SQLException ex) {
            status.setText("Apply sql/audit_log_migration.sql to enable audit history, then refresh. " + ex.getMessage());
        }
    }

    private void setOptionalLong(PreparedStatement q, int nullIndex, int valueIndex, String text) throws SQLException {
        if (text.isEmpty()) { q.setNull(nullIndex, Types.BIGINT); q.setNull(valueIndex, Types.BIGINT); return; }
        long value = Long.parseLong(text);
        if (value <= 0) throw new NumberFormatException();
        q.setLong(nullIndex, value); q.setLong(valueIndex, value);
    }

    private void setOptionalDate(PreparedStatement q, int nullIndex, int valueIndex, String text) throws SQLException {
        if (text.isEmpty()) { q.setNull(nullIndex, Types.DATE); q.setNull(valueIndex, Types.DATE); return; }
        Date date = Date.valueOf(text);
        q.setDate(nullIndex, date); q.setDate(valueIndex, date);
    }

    private Object nullableLong(ResultSet r, String column) throws SQLException {
        long value = r.getLong(column);
        return r.wasNull() ? null : value;
    }

}
