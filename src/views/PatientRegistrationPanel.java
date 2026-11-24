package views;

import database.DatabaseConnection;
import database.AuditLogger;
import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** Reception patient registration plus printable Code 39 patient-ID labels. */
public class PatientRegistrationPanel extends JPanel {
    private final long receptionistStaffId;
    private final JTextField barcodePatientId = new JTextField(12);

    public PatientRegistrationPanel() {
        this(0);
    }

    public PatientRegistrationPanel(long receptionistStaffId) {
        this.receptionistStaffId = receptionistStaffId;
        setLayout(new GridBagLayout());
        JPanel content = new JPanel(new GridLayout(0, 1, 12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        content.add(new JLabel("Register a patient or reprint a patient barcode."));
        JButton register = new JButton("Register new patient");
        JButton barcode = new JButton("Print / reprint barcode");
        JPanel reprint = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reprint.add(new JLabel("Patient ID:")); reprint.add(barcodePatientId); reprint.add(barcode);
        content.add(register);
        content.add(reprint);
        add(content);
        register.addActionListener(e -> registerPatient());
        barcode.addActionListener(e -> printBarcodeForExistingPatient());
    }

    private void registerPatient() {
        JTextField first = new JTextField(24);
        JTextField last = new JTextField(24);
        JTextField dob = new JTextField(LocalDate.now().minusYears(30).toString(), 14);
        JTextField phone = new JTextField(20);
        JTextField street = new JTextField(24);
        JTextField city = new JTextField(20);
        JTextField province = new JTextField(16);
        JTextField postal = new JTextField(12);
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("First name *")); form.add(first);
        form.add(new JLabel("Last name *")); form.add(last);
        form.add(new JLabel("Date of birth (YYYY-MM-DD) *")); form.add(dob);
        form.add(new JLabel("Phone")); form.add(phone);
        form.add(new JLabel("Street")); form.add(street);
        form.add(new JLabel("City")); form.add(city);
        form.add(new JLabel("Province")); form.add(province);
        form.add(new JLabel("Postal code")); form.add(postal);
        if (JOptionPane.showConfirmDialog(this, form, "Register patient", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (first.getText().trim().isEmpty() || last.getText().trim().isEmpty()) {
            showMessage("First and last name are required."); return;
        }
        try {
            LocalDate birth = LocalDate.parse(dob.getText().trim());
            if (birth.isAfter(LocalDate.now())) { showMessage("Date of birth cannot be in the future."); return; }
            long patientId;
            try (Connection c = DatabaseConnection.getConnection(); CallableStatement q = c.prepareCall("{CALL add_patient(?, ?, ?, ?, ?, ?, ?, ?, ?)}")) {
                q.setNull(1, Types.BIGINT);
                q.setString(2, first.getText().trim());
                q.setString(3, last.getText().trim());
                q.setDate(4, Date.valueOf(birth));
                setOptional(q, 5, phone.getText());
                setOptional(q, 6, street.getText());
                setOptional(q, 7, city.getText());
                setOptional(q, 8, province.getText());
                setOptional(q, 9, postal.getText());
                q.execute();
                try (Statement idQuery = c.createStatement(); ResultSet r = idQuery.executeQuery("SELECT LAST_INSERT_ID()")) {
                    if (!r.next()) throw new SQLException("MySQL did not return the new patient ID.");
                    patientId = r.getLong(1);
                }
                if (receptionistStaffId > 0) AuditLogger.logBestEffort(c, receptionistStaffId, patientId, "PATIENT_REGISTERED", "PATIENT", Long.toString(patientId), "SUCCESS", "Patient registered.");
            }
            barcodePatientId.setText(Long.toString(patientId));
            showMessage("Patient registered with ID " + patientId + ". After booking and checking in an appointment, the app will offer to print the wristband barcode.");
        } catch (DateTimeParseException ex) {
            showMessage("Enter the date of birth as YYYY-MM-DD.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void printBarcodeForExistingPatient() {
        try {
            long id = Long.parseLong(barcodePatientId.getText().trim());
            if (id <= 0) throw new NumberFormatException();
            String sql = "SELECT 1 FROM Patient p WHERE p.patient_id=? AND EXISTS (" +
                    "SELECT 1 FROM Appointment a JOIN Visit v ON v.appt_id=a.appt_id WHERE a.patient_id=p.patient_id)";
            try (Connection c = DatabaseConnection.getConnection(); PreparedStatement q = c.prepareStatement(sql)) {
                q.setLong(1, id);
                try (ResultSet r = q.executeQuery()) {
                    if (!r.next()) { showMessage("No checked-in patient found for ID " + id + ". A wristband barcode is available after check-in."); return; }
                }
                if (receptionistStaffId > 0) AuditLogger.logBestEffort(c, receptionistStaffId, id, "BARCODE_REPRINTED", "PATIENT", Long.toString(id), "SUCCESS", "Patient-ID barcode reprinted.");
            }
            showBarcode(id);
        } catch (NumberFormatException ex) {
            showMessage("Enter a positive numeric patient ID.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showBarcode(long patientId) {
        BarcodePanel image = new BarcodePanel(Long.toString(patientId));
        JButton print = new JButton("Print barcode");
        JPanel body = new JPanel(new BorderLayout(8, 8));
        body.add(image, BorderLayout.CENTER);
        body.add(print, BorderLayout.SOUTH);
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Patient barcode", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(body);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        print.addActionListener(e -> {
            try {
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setPrintable((graphics, pageFormat, pageIndex) -> {
                    if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                    double scale = Math.min(pageFormat.getImageableWidth() / image.getWidth(), pageFormat.getImageableHeight() / image.getHeight());
                    g.scale(scale, scale);
                    image.setSize(image.getPreferredSize());
                    image.paint(g);
                    g.dispose();
                    return Printable.PAGE_EXISTS;
                });
                if (job.printDialog()) job.print();
            }
            catch (PrinterException ex) { showMessage("Unable to print barcode: " + ex.getMessage()); }
        });
        dialog.setVisible(true);
    }

    /** Offer the receptionist the patient-ID wristband label after a successful check-in. */
    public void showBarcodeForPatient(long patientId) {
        if (patientId <= 0) return;
        barcodePatientId.setText(Long.toString(patientId));
        showBarcode(patientId);
    }

    private void setOptional(PreparedStatement q, int index, String value) throws SQLException {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) q.setNull(index, Types.VARCHAR); else q.setString(index, trimmed);
    }

    private void showMessage(String message) { JOptionPane.showMessageDialog(this, message); }
    private void showError(SQLException ex) { JOptionPane.showMessageDialog(this, "Patient registration failed: " + ex.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE); }

    private static final class BarcodePanel extends JPanel {
        private static final Map<Character, String> CODE39 = Map.ofEntries(
                Map.entry('0', "nnnwwnwnn"), Map.entry('1', "wnnwnnnnw"), Map.entry('2', "nnwwnnnnw"),
                Map.entry('3', "wnwwnnnnn"), Map.entry('4', "nnnwwnnnw"), Map.entry('5', "wnnwwnnnn"),
                Map.entry('6', "nnwwwnnnn"), Map.entry('7', "nnnwnnwnw"), Map.entry('8', "wnnwnnwnn"),
                Map.entry('9', "nnwwnnwnn"), Map.entry('*', "nwnnwnwnn"));
        private final String value;

        private BarcodePanel(String value) {
            this.value = value;
            setPreferredSize(new Dimension(420, 150));
            setBackground(Color.WHITE);
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.BLACK);
            String encoded = "*" + value + "*";
            // Include Code 39's required quiet zones (at least 10 narrow elements on both sides).
            int totalUnits = encoded.length() - 1 + 20;
            for (int i = 0; i < encoded.length(); i++) {
                String pattern = CODE39.get(encoded.charAt(i));
                for (int j = 0; j < pattern.length(); j++) totalUnits += pattern.charAt(j) == 'w' ? 3 : 1;
            }
            int unit = Math.max(1, (getWidth() - 40) / totalUnits);
            int x = (getWidth() - totalUnits * unit) / 2 + 10 * unit;
            int top = 18;
            for (int i = 0; i < encoded.length(); i++) {
                String pattern = CODE39.get(encoded.charAt(i));
                for (int j = 0; j < pattern.length(); j++) {
                    int width = unit * (pattern.charAt(j) == 'w' ? 3 : 1);
                    if (j % 2 == 0) g2.fillRect(x, top, width, 82);
                    x += width;
                }
                if (i < encoded.length() - 1) x += unit;
            }
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(value, (getWidth() - metrics.stringWidth(value)) / 2, 128);
            g2.dispose();
        }
    }
}
