package views;

import database.DatabaseConnection;
import database.AuditLogger;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;

/** Remote-worker access list; every view and note edit is constrained to an active grant. */
public class RemoteAccessPanel extends JPanel {
    private final long remoteWorkerStaffId;
    private final DefaultTableModel pendingModel = model("Access ID", "Patient ID", "Patient", "Expires");
    private final DefaultTableModel grantedModel = model("Access ID", "Patient ID", "Patient", "Expires");
    private final JTable pendingTable = new JTable(pendingModel);
    private final JTable grantedTable = new JTable(grantedModel);
    private final JLabel notice = new JLabel("Remote access is limited to active, unexpired patient grants.");

    public RemoteAccessPanel(long remoteWorkerStaffId) {
        this.remoteWorkerStaffId = remoteWorkerStaffId;
        setLayout(new BorderLayout(8, 8));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("New patient records granted", grantList(pendingTable, true));
        tabs.addTab("Granted patient records", grantList(grantedTable, false));
        add(notice, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        loadGrants();
    }

    private JPanel grantList(JTable table, boolean pending) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refresh = new JButton("Refresh");
        JButton open = new JButton(pending ? "Enter access ID and code" : "Open patient records");
        actions.add(refresh); actions.add(open);
        table.setAutoCreateRowSorter(true); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        panel.add(actions, BorderLayout.NORTH); panel.add(new JScrollPane(table), BorderLayout.CENTER);
        refresh.addActionListener(e -> loadGrants());
        open.addActionListener(e -> { if (pending) consumeSelectedGrant(); else openGrantedRecords(); });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    if (pending) consumeSelectedGrant(); else openGrantedRecords();
                }
            }
        });
        return panel;
    }

    private void loadGrants() {
        pendingModel.setRowCount(0); grantedModel.setRowCount(0);
        String base = "SELECT ra.access_id, p.patient_id, CONCAT(p.first_name,' ',p.last_name) AS patient_name, ra.expires_at " +
                "FROM Remote_access ra JOIN Patient p ON p.patient_id=ra.patient_id " +
                "WHERE ra.staff_id=? AND ra.revoked_at IS NULL AND ra.expires_at>CURRENT_TIMESTAMP AND ";
        try (Connection c = DatabaseConnection.getConnection()) {
            try (PreparedStatement q = c.prepareStatement(base + "ra.consumed_at IS NULL ORDER BY ra.expires_at")) {
                q.setLong(1, remoteWorkerStaffId);
                try (ResultSet r = q.executeQuery()) {
                    while (r.next()) pendingModel.addRow(new Object[]{r.getLong("access_id"), r.getLong("patient_id"), r.getString("patient_name"), r.getTimestamp("expires_at")});
                }
            }
            try (PreparedStatement q = c.prepareStatement(base + "ra.consumed_at IS NOT NULL ORDER BY ra.expires_at")) {
                q.setLong(1, remoteWorkerStaffId);
                try (ResultSet r = q.executeQuery()) {
                    while (r.next()) grantedModel.addRow(new Object[]{r.getLong("access_id"), r.getLong("patient_id"), r.getString("patient_name"), r.getTimestamp("expires_at")});
                }
            }
            if (pendingModel.getRowCount() == 0 && grantedModel.getRowCount() == 0) {
                notice.setText("No patient-record access is pending or active. Contact the System Administrator at " + administratorContact() + ".");
            } else {
                notice.setText("Only records attached to your active grants are available. Access codes are required the first time.");
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void consumeSelectedGrant() {
        int selected = pendingTable.getSelectedRow();
        if (selected < 0) { showMessage("Select a newly granted record first."); return; }
        int row = pendingTable.convertRowIndexToModel(selected);
        long accessId = ((Number) pendingModel.getValueAt(row, 0)).longValue();
        JPasswordField code = new JPasswordField(18);
        if (JOptionPane.showConfirmDialog(this, code, "Enter access code for ID " + accessId, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String entered = new String(code.getPassword());
        try {
            String codeHash = sha256(entered);
            long patientId;
            try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL consume_remote_access(?, ?, ?, ?)}")) {
                q.setLong(1, accessId);
                q.setLong(2, remoteWorkerStaffId);
                q.setString(3, codeHash);
                q.registerOutParameter(4, Types.BIGINT);
                q.execute();
                patientId = q.getLong(4);
            }
            AuditLogger.logBestEffort(remoteWorkerStaffId, patientId, "REMOTE_ACCESS_CODE_CONSUMED", "REMOTE_ACCESS",
                    Long.toString(accessId), "SUCCESS", "Single-use patient access code accepted.");
            loadGrants();
            openPatientRecords(accessId, patientId);
        } catch (SQLException ex) {
            showDenied();
        }
    }

    private void openGrantedRecords() {
        int selected = grantedTable.getSelectedRow();
        if (selected < 0) { showMessage("Select a granted patient first."); return; }
        int row = grantedTable.convertRowIndexToModel(selected);
        openPatientRecords(((Number) grantedModel.getValueAt(row, 0)).longValue(),
                ((Number) grantedModel.getValueAt(row, 1)).longValue());
    }

    private void openPatientRecords(long accessId, long patientId) {
        if (!hasActiveGrant(accessId, patientId)) { showDenied(); loadGrants(); return; }
        DefaultTableModel records = model("Record", "Visit date", "Diagnosis", "Treatment", "Vitals", "Clinical notes");
        JTable table = new JTable(records);
        table.setAutoCreateRowSorter(true); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        String sql = "SELECT mr.record_id, mr.visit_date, mr.diagnosis, mr.treatment, mr.vitals_summary, mr.notes " +
                "FROM Medical_Record mr WHERE mr.patient_id=? AND EXISTS (SELECT 1 FROM Remote_access ra WHERE ra.access_id=? AND ra.staff_id=? " +
                "AND ra.patient_id=mr.patient_id AND ra.consumed_at IS NOT NULL AND ra.revoked_at IS NULL AND ra.expires_at>CURRENT_TIMESTAMP) " +
                "ORDER BY mr.visit_date DESC, mr.record_id DESC";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, patientId); q.setLong(2, accessId); q.setLong(3, remoteWorkerStaffId);
            try (ResultSet r = q.executeQuery()) {
                while (r.next()) records.addRow(new Object[]{r.getLong("record_id"), r.getDate("visit_date"), r.getString("diagnosis"),
                        r.getString("treatment"), r.getString("vitals_summary"), r.getString("notes")});
            }
            if (records.getRowCount() == 0) { showMessage("No medical records are available for this patient."); return; }
            AuditLogger.logBestEffort(remoteWorkerStaffId, patientId, "REMOTE_PATIENT_RECORDS_VIEWED", "REMOTE_ACCESS",
                    Long.toString(accessId), "SUCCESS", "Granted patient records opened read-only.");
            JOptionPane.showMessageDialog(this, new JScrollPane(table), "Granted patient records (read-only)", JOptionPane.PLAIN_MESSAGE);
        } catch (SQLException ex) { showError(ex); }
    }

    private boolean hasActiveGrant(long accessId, long patientId) {
        String sql = "SELECT 1 FROM Remote_access WHERE access_id=? AND staff_id=? AND patient_id=? AND consumed_at IS NOT NULL " +
                "AND revoked_at IS NULL AND expires_at>CURRENT_TIMESTAMP LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, accessId); q.setLong(2, remoteWorkerStaffId); q.setLong(3, patientId);
            try (ResultSet r = q.executeQuery()) { return r.next(); }
        } catch (SQLException ex) { showError(ex); return false; }
    }

    private void showDenied() {
        showMessage("You have no valid patient-record request, or it expired, was revoked, or the code was incorrect. Contact the System Administrator at " + administratorContact() + ".");
    }

    private String administratorContact() {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(
                "SELECT s.email FROM Staff s JOIN Role r ON r.role_id=s.role_id WHERE r.role_name='System Administrator' AND s.termination_date IS NULL ORDER BY s.staff_id LIMIT 1");
             ResultSet r = q.executeQuery()) {
            if (r.next()) return r.getString(1);
        } catch (SQLException ignored) { }
        return "your system administrator";
    }

    private String sha256(String value) throws SQLException {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) hex.append(String.format(java.util.Locale.ROOT, "%02x", b));
            return hex.toString();
        } catch (Exception ex) { throw new SQLException("Could not validate access code.", ex); }
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
    }

    private void showMessage(String text) { JOptionPane.showMessageDialog(this, text); }
    private void showError(SQLException ex) { JOptionPane.showMessageDialog(this, "Remote access failed: " + ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE); }
}
