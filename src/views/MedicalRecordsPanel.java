package views;

import database.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

/** Patient chart, past appointments, visit notes, treatments, and vital signs. */
public class MedicalRecordsPanel extends JPanel {
    private final long clinicianStaffId;
    private final String role;
    private final JTextField patientId = new JTextField(12);
    private final DefaultTableModel records = model(
            "Record", "Patient", "Visit time", "Clinician", "Height cm", "Weight kg", "Vitals", "Diagnosis", "Treatment", "Clinical notes");
    private final DefaultTableModel appointments = model(
            "Appointment", "Date and time", "Patient", "Doctor", "Nurse", "Status", "Reason");
    private final JTable recordsTable = new JTable(records);
    private final JTable appointmentsTable = new JTable(appointments);

    public MedicalRecordsPanel(long clinicianStaffId) {
        this(clinicianStaffId, "Doctor");
    }

    public MedicalRecordsPanel(long clinicianStaffId, String role) {
        this.clinicianStaffId = clinicianStaffId;
        this.role = role;
        setLayout(new BorderLayout(8, 8));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Patient ID or barcode:"));
        controls.add(patientId);
        JButton search = new JButton("Open patient chart");
        JButton add = new JButton("New treatment / visit record");
        JButton edit = new JButton("Edit selected record");
        controls.add(search);
        controls.add(add);
        controls.add(edit);
        add(controls, BorderLayout.NORTH);

        configureTable(recordsTable);
        configureTable(appointmentsTable);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Medical records and treatments", new JScrollPane(recordsTable));
        tabs.addTab("Appointment history", new JScrollPane(appointmentsTable));
        add(tabs, BorderLayout.CENTER);
        add(new JLabel("Barcode scanners can type a patient ID into the field. Patient, visit, and appointment history is read-only."), BorderLayout.SOUTH);

        search.addActionListener(e -> loadPatientChart());
        patientId.addActionListener(e -> loadPatientChart());
        add.addActionListener(e -> createRecord());
        edit.addActionListener(e -> editSelectedRecord());
    }

    public void openPatient(String id) {
        patientId.setText(id == null ? "" : id);
        loadPatientChart();
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private void configureTable(JTable table) {
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
    }

    private void loadPatientChart() {
        records.setRowCount(0);
        appointments.setRowCount(0);
        String id = normalizeBarcode(patientId.getText());
        if (!validId(id)) return;
        patientId.setText(id);
        try (Connection c = DatabaseConnection.getConnection()) {
            if (!patientExists(c, Long.parseLong(id))) {
                showMessage("No patient found for ID/barcode " + id + ".");
                return;
            }
            loadRecords(c, Long.parseLong(id));
            loadAppointments(c, Long.parseLong(id));
            if (records.getRowCount() == 0 && appointments.getRowCount() == 0) {
                showMessage("No records or appointments found for this patient.");
            }
        } catch (NumberFormatException ex) {
            showMessage("Enter a numeric patient ID, or scan a barcode that contains the patient ID.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void loadRecords(Connection c, long id) throws SQLException {
        String sql = "SELECT mr.record_id, p.patient_id, p.first_name, p.last_name, " +
                "COALESCE(a.scheduled_at, v.check_in_at, mr.visit_date) AS visit_time, " +
                "CONCAT(s.first_name, ' ', s.last_name) AS clinician, mr.height_cm, mr.weight_kg, " +
                "mr.vitals_summary, mr.diagnosis, mr.treatment, mr.notes " +
                "FROM Medical_Record mr JOIN Patient p ON p.patient_id=mr.patient_id " +
                "JOIN Staff s ON s.staff_id=mr.clinician_staff_id " +
                "LEFT JOIN Visit v ON v.visit_id=mr.visit_id " +
                "LEFT JOIN Appointment a ON a.appt_id=v.appt_id " +
                "WHERE mr.patient_id=? ORDER BY COALESCE(a.scheduled_at, v.check_in_at, mr.visit_date) DESC, mr.record_id DESC";
        try (PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, id);
            try (ResultSet r = q.executeQuery()) {
                while (r.next()) records.addRow(new Object[]{r.getLong("record_id"),
                        r.getLong("patient_id") + " - " + r.getString("first_name") + " " + r.getString("last_name"),
                        r.getString("visit_time"), r.getString("clinician"), r.getBigDecimal("height_cm"),
                        r.getBigDecimal("weight_kg"), r.getString("vitals_summary"), r.getString("diagnosis"),
                        r.getString("treatment"), r.getString("notes")});
            }
        }
    }

    private void loadAppointments(Connection c, long id) throws SQLException {
        String sql = "SELECT a.appt_id, a.scheduled_at, a.patient_id, " +
                "COALESCE(CONCAT(p.first_name, ' ', p.last_name), CONCAT(a.prospective_first_name, ' ', a.prospective_last_name)) AS patient_name, " +
                "CONCAT(d.first_name, ' ', d.last_name) AS doctor_name, " +
                "CONCAT(n.first_name, ' ', n.last_name) AS nurse_name, a.appt_status, a.reason " +
                "FROM Appointment a LEFT JOIN Patient p ON p.patient_id=a.patient_id " +
                "JOIN Staff d ON d.staff_id=a.doctor_id LEFT JOIN Staff n ON n.staff_id=a.nurse_id " +
                "WHERE a.patient_id=? ORDER BY a.scheduled_at DESC, a.appt_id DESC";
        try (PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, id);
            try (ResultSet r = q.executeQuery()) {
                while (r.next()) appointments.addRow(new Object[]{r.getLong("appt_id"), r.getTimestamp("scheduled_at"),
                        r.getString("patient_name"), r.getString("doctor_name"), r.getString("nurse_name"),
                        r.getString("appt_status"), r.getString("reason")});
            }
        }
    }

    private void createRecord() {
        String patientKey = normalizeBarcode(patientId.getText());
        if (!validId(patientKey)) return;
        JTextField visit = new JTextField(12);
        JTextField height = new JTextField(10);
        JTextField weight = new JTextField(10);
        JTextField vitals = new JTextField(28);
        JTextField diagnosis = new JTextField(30);
        JComboBox<String> treatment = new JComboBox<>(new String[]{"", "IV", "Inhaler", "Vaccine / injection", "Sample collected", "Wound / surgical care", "Other"});
        treatment.setEditable(true);
        JTextArea notes = new JTextArea(4, 30);
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Patient ID / barcode")); form.add(new JLabel(patientKey));
        form.add(new JLabel("Visit ID (optional)")); form.add(visit);
        form.add(new JLabel("Height (cm)")); form.add(height);
        form.add(new JLabel("Weight (kg)")); form.add(weight);
        form.add(new JLabel("Other vitals (e.g. BP, pulse, temperature)")); form.add(vitals);
        form.add(new JLabel("Diagnosis *")); form.add(diagnosis);
        form.add(new JLabel("Treatment")); form.add(treatment);
        form.add(new JLabel("Additional notes")); form.add(new JScrollPane(notes));
        if (JOptionPane.showConfirmDialog(this, form, "New treatment / visit record", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (diagnosis.getText().trim().isEmpty()) { showMessage("Diagnosis is required."); return; }
        try {
            long patientValue = Long.parseLong(patientKey);
            Long visitValue = visit.getText().trim().isEmpty() ? null : Long.valueOf(visit.getText().trim());
            BigDecimal heightValue = optionalDecimal(height.getText().trim(), "height", new BigDecimal("0"), new BigDecimal("300"));
            BigDecimal weightValue = optionalDecimal(weight.getText().trim(), "weight", new BigDecimal("0"), new BigDecimal("1000"));
            if (visitValue != null && !visitBelongsToPatient(visitValue, patientValue)) {
                showMessage("That visit does not belong to the selected patient."); return;
            }
            try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL add_medical_record(?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
                q.setLong(1, patientValue);
                if (visitValue == null) q.setNull(2, Types.BIGINT); else q.setLong(2, visitValue);
                q.setLong(3, clinicianStaffId);
                if (heightValue == null) q.setNull(4, Types.DECIMAL); else q.setBigDecimal(4, heightValue);
                if (weightValue == null) q.setNull(5, Types.DECIMAL); else q.setBigDecimal(5, weightValue);
                q.setString(6, emptyToNull(vitals.getText()));
                q.setString(7, diagnosis.getText().trim());
                q.setString(8, emptyToNull(String.valueOf(treatment.getEditor().getItem())));
                q.setString(9, emptyToNull(notes.getText()));
                q.executeUpdate();
            }
            loadPatientChart();
            showMessage("Patient record saved.");
        } catch (NumberFormatException ex) {
            showMessage("Enter positive whole-number patient and visit IDs.");
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void editSelectedRecord() {
        int selected = recordsTable.getSelectedRow();
        if (selected < 0) { showMessage("Select a record first."); return; }
        int row = recordsTable.convertRowIndexToModel(selected);
        long recordId = ((Number) records.getValueAt(row, 0)).longValue();
        JTextField height = new JTextField(textAt(row, 4), 10);
        JTextField weight = new JTextField(textAt(row, 5), 10);
        JTextField vitals = new JTextField(textAt(row, 6), 28);
        JTextField diagnosis = new JTextField(textAt(row, 7), 30);
        JTextArea treatment = new JTextArea(textAt(row, 8), 3, 30);
        JTextArea notes = new JTextArea(textAt(row, 9), 4, 30);
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Height (cm)")); form.add(height);
        form.add(new JLabel("Weight (kg)")); form.add(weight);
        form.add(new JLabel("Other vitals")); form.add(vitals);
        form.add(new JLabel("Diagnosis *")); form.add(diagnosis);
        form.add(new JLabel("Treatment")); form.add(new JScrollPane(treatment));
        form.add(new JLabel("Additional notes")); form.add(new JScrollPane(notes));
        form.add(new JLabel("Patient, appointment, clinician, time")); form.add(new JLabel("Read-only"));
        if (JOptionPane.showConfirmDialog(this, form, "Edit patient record", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (diagnosis.getText().trim().isEmpty()) { showMessage("Diagnosis is required."); return; }
        try {
            BigDecimal heightValue = optionalDecimal(height.getText().trim(), "height", new BigDecimal("0"), new BigDecimal("300"));
            BigDecimal weightValue = optionalDecimal(weight.getText().trim(), "weight", new BigDecimal("0"), new BigDecimal("1000"));
            try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL update_medical_record(?, ?, ?, ?, ?, ?, ?)}")) {
                q.setLong(1, recordId);
                if (heightValue == null) q.setNull(2, Types.DECIMAL); else q.setBigDecimal(2, heightValue);
                if (weightValue == null) q.setNull(3, Types.DECIMAL); else q.setBigDecimal(3, weightValue);
                q.setString(4, emptyToNull(vitals.getText()));
                q.setString(5, diagnosis.getText().trim());
                q.setString(6, emptyToNull(treatment.getText()));
                q.setString(7, emptyToNull(notes.getText()));
                if (q.executeUpdate() == 0) { showMessage("Record no longer exists."); return; }
            }
            loadPatientChart();
            showMessage("Patient record updated.");
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage());
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

    private boolean visitBelongsToPatient(long visitId, long patientKey) throws SQLException {
        String sql = "SELECT 1 FROM Visit v JOIN Appointment a ON a.appt_id=v.appt_id WHERE v.visit_id=? AND a.patient_id=?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, visitId); q.setLong(2, patientKey);
            try (ResultSet r = q.executeQuery()) { return r.next(); }
        }
    }

    private BigDecimal optionalDecimal(String input, String label, BigDecimal minExclusive, BigDecimal maxInclusive) {
        if (input.isEmpty()) return null;
        try {
            BigDecimal value = new BigDecimal(input);
            if (value.compareTo(minExclusive) <= 0 || value.compareTo(maxInclusive) > 0) {
                throw new IllegalArgumentException("Enter a valid " + label + " in the supported range.");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a valid numeric " + label + ".");
        }
    }

    private boolean validId(String id) {
        try { if (Long.parseLong(id) > 0) return true; } catch (NumberFormatException ignored) { }
        showMessage("Enter a positive numeric patient ID or scan a barcode containing that ID.");
        return false;
    }

    private String normalizeBarcode(String input) {
        String value = input == null ? "" : input.trim().replaceAll("[\\r\\n\\t ]", "");
        if (value.length() > 2 && value.startsWith("*") && value.endsWith("*")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String textAt(int row, int column) {
        Object value = records.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private String emptyToNull(String value) {
        String text = value == null ? "" : value.trim();
        return text.isEmpty() ? null : text;
    }

    private void showMessage(String text) { JOptionPane.showMessageDialog(this, text); }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Medical records operation failed: " + ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
    }
}
