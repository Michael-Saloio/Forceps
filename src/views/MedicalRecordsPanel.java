package views;

import database.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

/** Search and manage patient visit records for clinical staff. */
public class MedicalRecordsPanel extends JPanel {
    private final long clinicianStaffId;
    private final JTextField patientId = new JTextField(10);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "Record", "Patient", "Visit", "Visit date", "Clinician", "Diagnosis", "Treatment", "Notes" },
            0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public MedicalRecordsPanel(long clinicianStaffId) {
        this.clinicianStaffId = clinicianStaffId;
        setLayout(new BorderLayout(8, 8));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Patient ID:"));
        controls.add(patientId);
        JButton search = new JButton("Search records");
        JButton add = new JButton("Add record");
        JButton edit = new JButton("Edit selected");
        controls.add(search);
        controls.add(add);
        controls.add(edit);
        add(controls, BorderLayout.NORTH);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        search.addActionListener(e -> loadRecords());
        add.addActionListener(e -> createRecord());
        edit.addActionListener(e -> editSelectedRecord());
    }

    /** Load records for one patient, or all records when the filter is blank. */
    private void loadRecords() {
        model.setRowCount(0);
        String id = patientId.getText().trim();
        if (!validId(id, true))
            return;
        String sql = "SELECT mr.record_id, mr.patient_id, mr.visit_id, mr.visit_date, " +
                "CONCAT(s.first_name, ' ', s.last_name) AS clinician, mr.diagnosis, mr.treatment, mr.notes " +
                "FROM Medical_Record mr JOIN Staff s ON s.staff_id = mr.clinician_staff_id " +
                "WHERE (? = '' OR mr.patient_id = ?) ORDER BY mr.visit_date DESC, mr.record_id DESC";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setString(1, id);
            if (id.isEmpty())
                q.setNull(2, Types.BIGINT);
            else
                q.setLong(2, Long.parseLong(id));
            try (ResultSet r = q.executeQuery()) {
                while (r.next())
                    model.addRow(new Object[] { r.getLong("record_id"), r.getLong("patient_id"),
                            r.getObject("visit_id"), r.getDate("visit_date"), r.getString("clinician"),
                            r.getString("diagnosis"), r.getString("treatment"), r.getString("notes") });
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /** Create a clinical record and optionally attach it to the patient's visit. */
    private void createRecord() {
        JTextField patient = new JTextField(patientId.getText().trim(), 16);
        JTextField visit = new JTextField(12);
        JTextField visitDate = new JTextField(LocalDate.now().toString(), 16);
        JTextField diagnosis = new JTextField(32);
        JTextField treatment = new JTextField(32);
        JTextArea notes = new JTextArea(5, 32);
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Patient ID *"));
        form.add(patient);
        form.add(new JLabel("Visit ID (optional)"));
        form.add(visit);
        form.add(new JLabel("Visit date (YYYY-MM-DD) *"));
        form.add(visitDate);
        form.add(new JLabel("Diagnosis *"));
        form.add(diagnosis);
        form.add(new JLabel("Treatment"));
        form.add(treatment);
        form.add(new JLabel("Notes"));
        form.add(new JScrollPane(notes));
        if (JOptionPane.showConfirmDialog(this, form, "New medical record",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return;
        String patientKey = patient.getText().trim();
        String visitKey = visit.getText().trim();
        if (!validId(patientKey, false) || !validId(visitKey, true))
            return;
        if (diagnosis.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Diagnosis is required.");
            return;
        }
        try {
            Date date = Date.valueOf(visitDate.getText().trim());
            if (!visitKey.isEmpty() && !visitBelongsToPatient(Long.parseLong(visitKey), Long.parseLong(patientKey))) {
                JOptionPane.showMessageDialog(this, "That visit does not belong to this patient.");
                return;
            }
            String sql = "INSERT INTO Medical_Record(patient_id, visit_id, clinician_staff_id, visit_date, diagnosis, treatment, notes) VALUES(?,?,?,?,?,?,?)";
            try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
                q.setLong(1, Long.parseLong(patientKey));
                if (visitKey.isEmpty())
                    q.setNull(2, Types.BIGINT);
                else
                    q.setLong(2, Long.parseLong(visitKey));
                q.setLong(3, clinicianStaffId);
                q.setDate(4, date);
                q.setString(5, diagnosis.getText().trim());
                q.setString(6, treatment.getText().trim());
                q.setString(7, notes.getText().trim());
                q.executeUpdate();
            }
            patientId.setText(patientKey);
            loadRecords();
            JOptionPane.showMessageDialog(this, "Medical record saved.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid visit date in YYYY-MM-DD format.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /**
     * Update the selected record's clinical details without changing its patient or
     * author.
     */
    private void editSelectedRecord() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a record first.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        long recordId = ((Number) model.getValueAt(modelRow, 0)).longValue();
        JTextField visitDate = new JTextField(cellText(modelRow, 3), 16);
        JTextField diagnosis = new JTextField(cellText(modelRow, 5), 32);
        JTextField treatment = new JTextField(cellText(modelRow, 6), 32);
        JTextArea notes = new JTextArea(cellText(modelRow, 7), 5, 32);
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Visit date (YYYY-MM-DD) *"));
        form.add(visitDate);
        form.add(new JLabel("Diagnosis *"));
        form.add(diagnosis);
        form.add(new JLabel("Treatment"));
        form.add(treatment);
        form.add(new JLabel("Notes"));
        form.add(new JScrollPane(notes));
        if (JOptionPane.showConfirmDialog(this, form, "Edit medical record",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
            return;
        if (diagnosis.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Diagnosis is required.");
            return;
        }
        try {
            Date date = Date.valueOf(visitDate.getText().trim());
            String sql = "UPDATE Medical_Record SET visit_date=?, diagnosis=?, treatment=?, notes=? WHERE record_id=?";
            try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
                q.setDate(1, date);
                q.setString(2, diagnosis.getText().trim());
                q.setString(3, treatment.getText().trim());
                q.setString(4, notes.getText().trim());
                q.setLong(5, recordId);
                q.executeUpdate();
            }
            loadRecords();
            JOptionPane.showMessageDialog(this, "Medical record updated.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid visit date in YYYY-MM-DD format.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    /** Ensure an optional visit belongs to the patient selected in the form. */
    private boolean visitBelongsToPatient(long visitId, long patientId) throws SQLException {
        String sql = "SELECT 1 FROM Visit v JOIN Appointment a ON a.appt_id=v.appt_id WHERE v.visit_id=? AND a.patient_id=?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, visitId);
            q.setLong(2, patientId);
            try (ResultSet r = q.executeQuery()) {
                return r.next();
            }
        }
    }

    private String cellText(int row, int column) {
        Object value = model.getValueAt(row, column);
        return value == null ? "" : value.toString();
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
