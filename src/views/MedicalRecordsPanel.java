package views;

import database.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/** Search, review, create, and update clinical records for an identified patient. */
public class MedicalRecordsPanel extends JPanel {
    private final long clinicianStaffId;
    private final JTextField patientId = new JTextField(10);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Record", "Patient", "Visit / check-in", "Clinician", "Diagnosis", "Treatment", "Notes"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    public MedicalRecordsPanel(long clinicianStaffId) {
        this.clinicianStaffId = clinicianStaffId;
        setLayout(new BorderLayout(8, 8));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Patient ID / scanned barcode:"));
        controls.add(patientId);
        JButton search = new JButton("Search records");
        JButton add = new JButton("Add record");
        JButton edit = new JButton("Edit selected");
        controls.add(search);
        controls.add(add);
        controls.add(edit);
        add(controls, BorderLayout.NORTH);

        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel note = new JLabel("Identify the patient by ID or scan the barcode; visit/check-in timestamps come from the linked visit.");
        add(note, BorderLayout.SOUTH);

        search.addActionListener(e -> loadRecords());
        add.addActionListener(e -> createRecord());
        edit.addActionListener(e -> editSelectedRecord());
        patientId.addActionListener(e -> loadRecords());
    }

    /** A patient ID is required so searches never expose every patient's history. */
    private void loadRecords() {
        model.setRowCount(0);
        String id = patientId.getText().trim();
        if (!validId(id, false)) return;

        String sql = "SELECT mr.record_id, p.patient_id, p.first_name, p.last_name, " +
                "COALESCE(DATE_FORMAT(v.check_in_at, '%Y-%m-%d %H:%i'), DATE_FORMAT(mr.visit_date, '%Y-%m-%d')) AS visit_time, " +
                "CONCAT(s.first_name, ' ', s.last_name) AS clinician, mr.diagnosis, mr.treatment, mr.notes " +
                "FROM Medical_Record mr " +
                "JOIN Patient p ON p.patient_id = mr.patient_id " +
                "JOIN Staff s ON s.staff_id = mr.clinician_staff_id " +
                "LEFT JOIN Visit v ON v.visit_id = mr.visit_id " +
                "WHERE mr.patient_id = ? ORDER BY COALESCE(v.check_in_at, mr.visit_date) DESC, mr.record_id DESC";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, Long.parseLong(id));
            try (ResultSet r = q.executeQuery()) {
                while (r.next()) {
                    model.addRow(new Object[]{r.getLong("record_id"),
                            r.getLong("patient_id") + " - " + r.getString("first_name") + " " + r.getString("last_name"),
                            r.getString("visit_time"), r.getString("clinician"), r.getString("diagnosis"),
                            r.getString("treatment"), r.getString("notes")});
                }
            }
            if (model.getRowCount() == 0) JOptionPane.showMessageDialog(this, "No medical records found for patient " + id + ".");
        } catch (NumberFormatException ex) {
            showMessage("Enter a positive whole-number patient ID (or scan a barcode containing that ID).");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /** New records can link to a checked-in visit; its timestamp is read from Visit and cannot be edited here. */
    private void createRecord() {
        JTextField patient = new JTextField(patientId.getText().trim(), 18);
        JTextField visit = new JTextField(12);
        JTextField diagnosis = new JTextField(32);
        JTextArea treatment = new JTextArea(3, 32);
        JTextArea notes = new JTextArea(4, 32);
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Patient ID / barcode *")); form.add(patient);
        form.add(new JLabel("Checked-in visit ID (optional)")); form.add(visit);
        form.add(new JLabel("Diagnosis *")); form.add(diagnosis);
        form.add(new JLabel("Treatment")); form.add(new JScrollPane(treatment));
        form.add(new JLabel("Additional notes")); form.add(new JScrollPane(notes));
        form.add(new JLabel("Visit date/time")); form.add(new JLabel("Automatic from visit; otherwise today"));

        if (JOptionPane.showConfirmDialog(this, form, "New medical record", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String patientKey = patient.getText().trim();
        String visitKey = visit.getText().trim();
        if (!validId(patientKey, false) || !validId(visitKey, true)) return;
        if (diagnosis.getText().trim().isEmpty()) { showMessage("Diagnosis is required."); return; }

        try (Connection c = DatabaseConnection.getConnection()) {
            long patientValue = Long.parseLong(patientKey);
            if (!patientExists(c, patientValue)) { showMessage("Patient " + patientKey + " was not found."); return; }
            Long visitValue = visitKey.isEmpty() ? null : Long.parseLong(visitKey);
            Date visitDate = visitValue == null ? new Date(System.currentTimeMillis()) : checkedInVisitDate(c, visitValue, patientValue);
            if (visitValue != null && visitDate == null) {
                showMessage("The visit was not found for this patient. Use a checked-in visit ID or leave it blank.");
                return;
            }

            String sql = "INSERT INTO Medical_Record(patient_id, visit_id, clinician_staff_id, visit_date, diagnosis, treatment, notes) VALUES(?,?,?,?,?,?,?)";
            try (PreparedStatement q = c.prepareStatement(sql)) {
                q.setLong(1, patientValue);
                if (visitValue == null) q.setNull(2, Types.BIGINT); else q.setLong(2, visitValue);
                q.setLong(3, clinicianStaffId);
                q.setDate(4, visitDate);
                q.setString(5, diagnosis.getText().trim());
                q.setString(6, emptyToNull(treatment.getText()));
                q.setString(7, emptyToNull(notes.getText()));
                q.executeUpdate();
            }
            patientId.setText(patientKey);
            loadRecords();
            showMessage("Medical record saved.");
        } catch (NumberFormatException ex) {
            showMessage("Enter positive whole-number patient and visit IDs.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /** Clinical text can be corrected, but patient, clinician, visit link, and visit time remain fixed. */
    private void editSelectedRecord() {
        int row = selectedModelRow();
        if (row < 0) { showMessage("Select a record first."); return; }
        long recordId = ((Number) model.getValueAt(row, 0)).longValue();
        JTextField diagnosis = new JTextField(cellText(row, 4), 32);
        JTextArea treatment = new JTextArea(cellText(row, 5), 3, 32);
        JTextArea notes = new JTextArea(cellText(row, 6), 4, 32);
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Diagnosis *")); form.add(diagnosis);
        form.add(new JLabel("Treatment")); form.add(new JScrollPane(treatment));
        form.add(new JLabel("Additional notes")); form.add(new JScrollPane(notes));
        form.add(new JLabel("Patient / visit / timestamp")); form.add(new JLabel("Read-only"));
        if (JOptionPane.showConfirmDialog(this, form, "Edit medical record", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (diagnosis.getText().trim().isEmpty()) { showMessage("Diagnosis is required."); return; }

        String sql = "UPDATE Medical_Record SET diagnosis=?, treatment=?, notes=? WHERE record_id=?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setString(1, diagnosis.getText().trim());
            q.setString(2, emptyToNull(treatment.getText()));
            q.setString(3, emptyToNull(notes.getText()));
            q.setLong(4, recordId);
            if (q.executeUpdate() == 0) { showMessage("This record no longer exists. Refresh and try again."); return; }
            loadRecords();
            showMessage("Medical record updated.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private boolean patientExists(Connection c, long id) throws SQLException {
        try (PreparedStatement q = c.prepareStatement("SELECT 1 FROM Patient WHERE patient_id=?")) {
            q.setLong(1, id);
            try (ResultSet r = q.executeQuery()) { return r.next(); }
        }
    }

    /** Return the stored calendar date only for a visit belonging to this patient. */
    private Date checkedInVisitDate(Connection c, long visitId, long patientId) throws SQLException {
        String sql = "SELECT DATE(v.check_in_at) AS visit_date FROM Visit v " +
                "JOIN Appointment a ON a.appt_id=v.appt_id WHERE v.visit_id=? AND a.patient_id=?";
        try (PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, visitId); q.setLong(2, patientId);
            try (ResultSet r = q.executeQuery()) { return r.next() ? r.getDate("visit_date") : null; }
        }
    }

    private int selectedModelRow() {
        int row = table.getSelectedRow();
        return row < 0 ? -1 : table.convertRowIndexToModel(row);
    }

    private String cellText(int row, int column) {
        Object value = model.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private boolean validId(String value, boolean allowEmpty) {
        if (allowEmpty && value.isEmpty()) return true;
        try { if (Long.parseLong(value) > 0) return true; } catch (NumberFormatException ignored) { }
        showMessage("Enter a positive whole-number ID.");
        return false;
    }

    private String emptyToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Medical records operation failed: " + ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
    }
}
