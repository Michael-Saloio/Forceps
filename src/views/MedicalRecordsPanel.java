package views;

import database.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

/** Search and record patient visits for clinical staff. */
public class MedicalRecordsPanel extends JPanel {
    private final long clinicianStaffId;
    private final JTextField patientId = new JTextField(10);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Record", "Patient", "Visit date", "Clinician", "Diagnosis", "Treatment", "Notes"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    public MedicalRecordsPanel(long clinicianStaffId) {
        this.clinicianStaffId = clinicianStaffId;
        setLayout(new BorderLayout(8, 8));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Patient ID:"));
        controls.add(patientId);
        JButton search = new JButton("Search records");
        JButton add = new JButton("Add record");
        controls.add(search);
        controls.add(add);
        add(controls, BorderLayout.NORTH);
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
        search.addActionListener(e -> loadRecords());
        add.addActionListener(e -> createRecord());
    }

    private void loadRecords() {
        model.setRowCount(0);
        String id = patientId.getText().trim();
        if (!validId(id, true)) return;
        String sql = "SELECT mr.record_id, mr.patient_id, mr.visit_date, " +
                "CONCAT(s.first_name, ' ', s.last_name) AS clinician, mr.diagnosis, mr.treatment, mr.notes " +
                "FROM Medical_Record mr JOIN Staff s ON s.staff_id = mr.clinician_staff_id " +
                "WHERE (? = '' OR mr.patient_id = ?) ORDER BY mr.visit_date DESC, mr.record_id DESC";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setString(1, id);
            if (id.isEmpty()) q.setNull(2, Types.BIGINT); else q.setLong(2, Long.parseLong(id));
            try (ResultSet r = q.executeQuery()) {
                while (r.next()) model.addRow(new Object[]{r.getLong("record_id"), r.getLong("patient_id"),
                        r.getDate("visit_date"), r.getString("clinician"), r.getString("diagnosis"),
                        r.getString("treatment"), r.getString("notes")});
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private void createRecord() {
        JTextField patient = new JTextField(patientId.getText().trim(), 16);
        JTextField visitDate = new JTextField(LocalDate.now().toString(), 16);
        JTextField diagnosis = new JTextField(32);
        JTextField treatment = new JTextField(32);
        JTextArea notes = new JTextArea(5, 32);
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Patient ID *")); form.add(patient);
        form.add(new JLabel("Visit date (YYYY-MM-DD) *")); form.add(visitDate);
        form.add(new JLabel("Diagnosis *")); form.add(diagnosis);
        form.add(new JLabel("Treatment")); form.add(treatment);
        form.add(new JLabel("Notes")); form.add(new JScrollPane(notes));
        if (JOptionPane.showConfirmDialog(this, form, "New medical record", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String id = patient.getText().trim();
        if (!validId(id, false)) return;
        if (diagnosis.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Diagnosis is required.");
            return;
        }
        try {
            Date date = Date.valueOf(visitDate.getText().trim());
            String sql = "INSERT INTO Medical_Record(patient_id, clinician_staff_id, visit_date, diagnosis, treatment, notes) VALUES(?,?,?,?,?,?)";
            try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
                q.setLong(1, Long.parseLong(id)); q.setLong(2, clinicianStaffId); q.setDate(3, date);
                q.setString(4, diagnosis.getText().trim()); q.setString(5, treatment.getText().trim());
                q.setString(6, notes.getText().trim()); q.executeUpdate();
            }
            patientId.setText(id);
            loadRecords();
            JOptionPane.showMessageDialog(this, "Medical record saved.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid visit date in YYYY-MM-DD format.");
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
