package views;

import database.DatabaseConnection;
import database.AuditLogger;
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
    private final JLabel patientSummary = new JLabel("No patient selected");
    private final DefaultTableModel records = model(
            "Record", "Patient", "Visit time", "Clinician", "Height cm", "Weight kg", "Vitals", "Diagnosis", "Treatment", "Clinical notes", "Version");
    private final DefaultTableModel appointments = model(
            "Appointment", "Date and time", "Patient", "Doctor", "Nurse", "Status", "Reason");
    private final DefaultTableModel treatments = model("Treatment", "Visit", "Performed at", "Treatment", "Charged cost", "Clinician", "Notes");
    private final DefaultTableModel vitalHistory = model("Visit", "Measured at", "Height cm", "Weight kg", "Heart rate", "BP", "Temp °C", "SpO₂ %", "Recorded by", "Notes");
    private final JTable recordsTable = new JTable(records);
    private final JTable appointmentsTable = new JTable(appointments);
    private final JTable treatmentsTable = new JTable(treatments);
    private final JTable vitalTable = new JTable(vitalHistory);

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
        JButton addTreatment = new JButton("Record billable treatment");
        JButton edit = new JButton("Edit selected record");
        controls.add(search);
        controls.add(add);
        controls.add(addTreatment);
        controls.add(edit);
        JPanel chartHeader = new JPanel(new BorderLayout(2, 2));
        patientSummary.setBorder(BorderFactory.createEmptyBorder(2, 12, 8, 12));
        chartHeader.add(controls, BorderLayout.NORTH);
        chartHeader.add(patientSummary, BorderLayout.SOUTH);
        add(chartHeader, BorderLayout.NORTH);

        configureTable(recordsTable);
        recordsTable.removeColumn(recordsTable.getColumnModel().getColumn(10)); // Concurrency token stays hidden from users.
        configureTable(appointmentsTable);
        configureTable(treatmentsTable);
        configureTable(vitalTable);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Medical records and treatments", new JScrollPane(recordsTable));
        tabs.addTab("Billable treatment events", new JScrollPane(treatmentsTable));
        tabs.addTab("Timestamped vital signs", new JScrollPane(vitalTable));
        tabs.addTab("Appointment history", new JScrollPane(appointmentsTable));
        add(tabs, BorderLayout.CENTER);
        add(new JLabel("Scan the patient-ID barcode or enter the ID. Record billable treatments during an active visit; they appear on the checkout invoice."), BorderLayout.SOUTH);

        search.addActionListener(e -> loadPatientChart());
        patientId.addActionListener(e -> loadPatientChart());
        add.addActionListener(e -> createRecord());
        addTreatment.addActionListener(e -> createTreatment());
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
        treatments.setRowCount(0);
        vitalHistory.setRowCount(0);
        patientSummary.setText("No patient selected");
        String id = normalizeBarcode(patientId.getText());
        if (!validId(id)) return;
        patientId.setText(id);
        try (Connection c = DatabaseConnection.getConnection()) {
            if (!patientExists(c, Long.parseLong(id))) {
                showMessage("No patient found for ID/barcode " + id + ".");
                return;
            }
            try (PreparedStatement q = c.prepareStatement("SELECT first_name,last_name,date_of_birth,phone FROM Patient WHERE patient_id=?")) {
                q.setLong(1, Long.parseLong(id));
                try (ResultSet r = q.executeQuery()) {
                    if (r.next()) patientSummary.setText("Patient: " + r.getString("first_name") + " " + r.getString("last_name")
                            + " | DOB: " + r.getDate("date_of_birth") + " | Phone: " + java.util.Objects.toString(r.getString("phone"), "—"));
                }
            }
            loadRecords(c, Long.parseLong(id));
            loadTreatmentHistory(c, Long.parseLong(id));
            loadVitalHistory(c, Long.parseLong(id));
            loadAppointments(c, Long.parseLong(id));
            AuditLogger.logBestEffort(c, clinicianStaffId, Long.parseLong(id), "PATIENT_CHART_VIEWED", "PATIENT", id, "SUCCESS", "Patient chart opened.");
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
                "mr.vitals_summary, mr.diagnosis, mr.treatment, mr.notes, mr.row_version " +
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
                        r.getString("treatment"), r.getString("notes"), r.getInt("row_version")});
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

    private void loadTreatmentHistory(Connection c, long patientValue) throws SQLException {
        String sql = "SELECT tr.treatment_id,tr.visit_id,tr.performed_at,tt.type_name,tr.charged_cost, " +
                "CONCAT(s.first_name,' ',s.last_name) AS clinician,tr.notes " +
                "FROM Treatment_Record tr JOIN Treatment_Type tt ON tt.type_code=tr.type_code " +
                "JOIN Staff s ON s.staff_id=tr.performed_by_staff_id " +
                "JOIN Visit v ON v.visit_id=tr.visit_id JOIN Appointment a ON a.appt_id=v.appt_id " +
                "WHERE a.patient_id=? ORDER BY tr.performed_at DESC,tr.treatment_id DESC";
        try (PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, patientValue);
            try (ResultSet r = q.executeQuery()) {
                while (r.next()) treatments.addRow(new Object[]{r.getLong("treatment_id"),r.getLong("visit_id"),
                        r.getTimestamp("performed_at"),r.getString("type_name"),r.getBigDecimal("charged_cost"),
                        r.getString("clinician"),r.getString("notes")});
            }
        }
    }

    private void loadVitalHistory(Connection c, long patientValue) throws SQLException {
        String sql = "SELECT vs.visit_id,vs.measured_at,vs.height_cm,vs.weight_kg,vs.heart_rate,vs.bp_systolic,vs.bp_diastolic, " +
                "vs.temperature_c,vs.spo2,CONCAT(s.first_name,' ',s.last_name) AS staff_name,vs.notes " +
                "FROM Vital_Sign vs JOIN Visit v ON v.visit_id=vs.visit_id JOIN Appointment a ON a.appt_id=v.appt_id " +
                "JOIN Staff s ON s.staff_id=vs.recorded_by_staff_id WHERE a.patient_id=? ORDER BY vs.measured_at DESC,vs.vital_id DESC";
        try (PreparedStatement q = c.prepareStatement(sql)) {
            q.setLong(1, patientValue);
            try (ResultSet r = q.executeQuery()) {
                while (r.next()) {
                    String bp = r.getObject("bp_systolic") == null ? "" : r.getInt("bp_systolic") + "/" + r.getInt("bp_diastolic");
                    vitalHistory.addRow(new Object[]{r.getLong("visit_id"),r.getTimestamp("measured_at"),r.getBigDecimal("height_cm"),
                            r.getBigDecimal("weight_kg"),r.getObject("heart_rate"),bp,r.getBigDecimal("temperature_c"),
                            r.getBigDecimal("spo2"),r.getString("staff_name"),r.getString("notes")});
                }
            }
        }
    }

    private void createTreatment() {
        String id = normalizeBarcode(patientId.getText());
        if (!validId(id)) return;
        JComboBox<VisitOption> visitChoice = new JComboBox<>();
        JComboBox<TreatmentOption> typeChoice = new JComboBox<>();
        JTextField note = new JTextField(28);
        try (Connection c = DatabaseConnection.getConnection()) {
            try (PreparedStatement q = c.prepareStatement("SELECT v.visit_id,v.check_in_at FROM Visit v JOIN Appointment a ON a.appt_id=v.appt_id WHERE a.patient_id=? AND v.visit_status='CHECKED IN' ORDER BY v.check_in_at DESC")) {
                q.setLong(1, Long.parseLong(id));
                try (ResultSet r = q.executeQuery()) { while (r.next()) visitChoice.addItem(new VisitOption(r.getLong(1), r.getTimestamp(2))); }
            }
            try (PreparedStatement q = c.prepareStatement("SELECT type_code,type_name,standard_cost FROM Treatment_Type WHERE is_active=TRUE ORDER BY type_name"); ResultSet r = q.executeQuery()) {
                while (r.next()) typeChoice.addItem(new TreatmentOption(r.getString(1), r.getString(2), r.getBigDecimal(3)));
            }
        } catch (SQLException ex) { showError(ex); return; }
        if (visitChoice.getItemCount() == 0) { showMessage("This patient has no active checked-in visit. Treatment must be recorded before checkout."); return; }
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Active visit *")); form.add(visitChoice);
        form.add(new JLabel("Treatment type *")); form.add(typeChoice);
        form.add(new JLabel("Notes")); form.add(note);
        if (JOptionPane.showConfirmDialog(this, form, "Record treatment", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        VisitOption visit = (VisitOption) visitChoice.getSelectedItem();
        TreatmentOption type = (TreatmentOption) typeChoice.getSelectedItem();
        if (visit == null || type == null) { showMessage("Choose a visit and treatment type."); return; }
        try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL add_treatment_record(?, ?, ?, ?)}")) {
            q.setLong(1, visit.id); q.setString(2, type.code); q.setLong(3, clinicianStaffId);
            String details = note.getText().trim();
            if (details.isEmpty()) q.setNull(4, Types.VARCHAR); else q.setString(4, details);
            q.execute();
            AuditLogger.logBestEffort(c, clinicianStaffId, Long.parseLong(id), "TREATMENT_RECORDED", "VISIT", Long.toString(visit.id), "SUCCESS", "A billable treatment event was recorded.");
            loadPatientChart();
            showMessage(type.name + " was recorded at the catalog price $" + type.cost + ". It will appear on the visit invoice.");
        } catch (SQLException ex) { showError(ex); }
    }

    private void createRecord() {
        String patientKey = normalizeBarcode(patientId.getText());
        if (!validId(patientKey)) return;
        JTextField visit = new JTextField(12);
        JTextField height = new JTextField(10);
        JTextField weight = new JTextField(10);
        JTextField heartRate = new JTextField(8), systolic = new JTextField(8), diastolic = new JTextField(8);
        JTextField temperature = new JTextField(8), spo2 = new JTextField(8), vitals = new JTextField(20);
        JTextField diagnosis = new JTextField(30);
        JComboBox<String> treatment = new JComboBox<>(new String[]{"", "IV", "Inhaler", "Vaccine / injection", "Sample collected", "Wound / surgical care", "Other"});
        treatment.setEditable(true);
        JTextArea notes = new JTextArea(4, 30);
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Patient ID / barcode")); form.add(new JLabel(patientKey));
        form.add(new JLabel("Visit ID (optional)")); form.add(visit);
        form.add(new JLabel("Height (cm)")); form.add(height);
        form.add(new JLabel("Weight (kg)")); form.add(weight);
        form.add(new JLabel("Heart rate (bpm)")); form.add(heartRate);
        form.add(new JLabel("Blood pressure (systolic / diastolic)"));
        JPanel bloodPressure = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0)); bloodPressure.add(systolic); bloodPressure.add(new JLabel("/")); bloodPressure.add(diastolic); form.add(bloodPressure);
        form.add(new JLabel("Temperature (°C)")); form.add(temperature);
        form.add(new JLabel("Oxygen saturation (%)")); form.add(spo2);
        form.add(new JLabel("Other vitals / note")); form.add(vitals);
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
            Integer heartValue = optionalInteger(heartRate.getText().trim(), "heart rate", 20, 300);
            Integer systolicValue = optionalInteger(systolic.getText().trim(), "systolic blood pressure", 30, 300);
            Integer diastolicValue = optionalInteger(diastolic.getText().trim(), "diastolic blood pressure", 20, 200);
            if ((systolicValue == null) != (diastolicValue == null)) throw new IllegalArgumentException("Enter both systolic and diastolic blood pressure values.");
            BigDecimal temperatureValue = optionalRangedDecimal(temperature.getText().trim(), "temperature", new BigDecimal("25"), new BigDecimal("45"));
            BigDecimal spo2Value = optionalRangedDecimal(spo2.getText().trim(), "oxygen saturation", new BigDecimal("0"), new BigDecimal("100"));
            boolean hasVitals = heightValue != null || weightValue != null || heartValue != null || systolicValue != null || temperatureValue != null || spo2Value != null || !vitals.getText().trim().isEmpty();
            if (hasVitals && visitValue == null) throw new IllegalArgumentException("Vital signs must be attached to an active visit. Enter its Visit ID.");
            if (visitValue != null && !visitBelongsToPatient(visitValue, patientValue)) {
                showMessage("That visit does not belong to the selected patient."); return;
            }
            String vitalsSummary = vitalsSummary(heartValue, systolicValue, diastolicValue, temperatureValue, spo2Value, vitals.getText());
            try (Connection c = DatabaseConnection.getConnection()) {
                c.setAutoCommit(false);
                try (CallableStatement q = c.prepareCall("{CALL add_medical_record(?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
                    q.setLong(1, patientValue);
                    if (visitValue == null) q.setNull(2, Types.BIGINT); else q.setLong(2, visitValue);
                    q.setLong(3, clinicianStaffId);
                    if (heightValue == null) q.setNull(4, Types.DECIMAL); else q.setBigDecimal(4, heightValue);
                    if (weightValue == null) q.setNull(5, Types.DECIMAL); else q.setBigDecimal(5, weightValue);
                    if (vitalsSummary == null) q.setNull(6, Types.VARCHAR); else q.setString(6, vitalsSummary);
                    q.setString(7, diagnosis.getText().trim());
                    q.setString(8, emptyToNull(String.valueOf(treatment.getEditor().getItem())));
                    q.setString(9, emptyToNull(notes.getText()));
                    q.execute();
                }
                if (hasVitals) {
                    try (CallableStatement q = c.prepareCall("{CALL add_visit_vital_sign(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
                        q.setLong(1, visitValue); q.setLong(2, clinicianStaffId);
                        if (heightValue == null) q.setNull(3, Types.DECIMAL); else q.setBigDecimal(3, heightValue);
                        if (weightValue == null) q.setNull(4, Types.DECIMAL); else q.setBigDecimal(4, weightValue);
                        if (heartValue == null) q.setNull(5, Types.INTEGER); else q.setInt(5, heartValue);
                        if (systolicValue == null) q.setNull(6, Types.INTEGER); else q.setInt(6, systolicValue);
                        if (diastolicValue == null) q.setNull(7, Types.INTEGER); else q.setInt(7, diastolicValue);
                        if (temperatureValue == null) q.setNull(8, Types.DECIMAL); else q.setBigDecimal(8, temperatureValue);
                        if (spo2Value == null) q.setNull(9, Types.DECIMAL); else q.setBigDecimal(9, spo2Value);
                        if (vitals.getText().trim().isEmpty()) q.setNull(10, Types.VARCHAR); else q.setString(10, vitals.getText().trim());
                        q.execute();
                    }
                    AuditLogger.logBestEffort(c, clinicianStaffId, patientValue, "VITAL_SIGNS_RECORDED", "VISIT", Long.toString(visitValue), "SUCCESS", "Time-stamped vital sign measurements recorded.");
                }
                AuditLogger.logBestEffort(c, clinicianStaffId, patientValue, "MEDICAL_RECORD_CREATED", "PATIENT", patientKey, "SUCCESS", "Clinical record created.");
                c.commit();
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
        int expectedVersion = ((Number) records.getValueAt(row, 10)).intValue();
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
            try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL update_medical_record(?, ?, ?, ?, ?, ?, ?, ?)}")) {
                q.setLong(1, recordId);
                if (heightValue == null) q.setNull(2, Types.DECIMAL); else q.setBigDecimal(2, heightValue);
                if (weightValue == null) q.setNull(3, Types.DECIMAL); else q.setBigDecimal(3, weightValue);
                q.setString(4, emptyToNull(vitals.getText()));
                q.setString(5, diagnosis.getText().trim());
                q.setString(6, emptyToNull(treatment.getText()));
                q.setString(7, emptyToNull(notes.getText()));
                q.setInt(8, expectedVersion);
                if (q.executeUpdate() == 0) { showMessage("Record no longer exists."); return; }
                AuditLogger.logBestEffort(c, clinicianStaffId, null, "MEDICAL_RECORD_UPDATED", "MEDICAL_RECORD", Long.toString(recordId), "SUCCESS", "Clinical record updated.");
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

    private Integer optionalInteger(String input, String label, int minimum, int maximum) {
        if (input.isEmpty()) return null;
        try {
            int value = Integer.parseInt(input);
            if (value < minimum || value > maximum) throw new IllegalArgumentException("Enter " + label + " from " + minimum + " to " + maximum + ".");
            return value;
        } catch (NumberFormatException ex) { throw new IllegalArgumentException("Enter a whole-number " + label + "."); }
    }

    private BigDecimal optionalRangedDecimal(String input, String label, BigDecimal minimum, BigDecimal maximum) {
        if (input.isEmpty()) return null;
        try {
            BigDecimal value = new BigDecimal(input);
            if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0)
                throw new IllegalArgumentException("Enter " + label + " from " + minimum + " to " + maximum + ".");
            return value;
        } catch (NumberFormatException ex) { throw new IllegalArgumentException("Enter a numeric " + label + "."); }
    }

    private String vitalsSummary(Integer heart, Integer systolic, Integer diastolic, BigDecimal temp, BigDecimal oxygen, String extra) {
        java.util.List<String> values = new java.util.ArrayList<>();
        if (heart != null) values.add("HR " + heart + " bpm");
        if (systolic != null && diastolic != null) values.add("BP " + systolic + "/" + diastolic);
        if (temp != null) values.add("Temp " + temp + " °C");
        if (oxygen != null) values.add("SpO2 " + oxygen + "%");
        if (extra != null && !extra.trim().isEmpty()) values.add(extra.trim());
        return values.isEmpty() ? null : String.join("; ", values);
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

    private record VisitOption(long id, Timestamp checkedInAt) {
        @Override public String toString() { return "Visit " + id + " — " + checkedInAt; }
    }

    private record TreatmentOption(String code, String name, BigDecimal cost) {
        @Override public String toString() { return name + " ($" + cost + ")"; }
    }

    private void showMessage(String text) { JOptionPane.showMessageDialog(this, text); }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Medical records operation failed: " + ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
    }
}
