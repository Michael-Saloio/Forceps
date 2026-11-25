package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Writes a minimal, append-only application audit event. Never pass passwords or clinical text. */
public final class AuditLogger {
    private AuditLogger() { }

    public static void log(Connection connection, long staffId, Long patientId, String action,
            String targetType, String targetId, String outcome, String details) throws SQLException {
        String sql = "INSERT INTO Audit_Log(staff_id, patient_id, occurred_at, action_type, target_type, target_id, outcome, details) " +
                "VALUES(?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?)";
        try (PreparedStatement q = connection.prepareStatement(sql)) {
            q.setLong(1, staffId);
            if (patientId == null) q.setNull(2, java.sql.Types.BIGINT); else q.setLong(2, patientId);
            q.setString(3, action);
            q.setString(4, targetType);
            q.setString(5, targetId);
            q.setString(6, outcome);
            q.setString(7, details);
            q.executeUpdate();
        }
    }

    public static void log(long staffId, Long patientId, String action, String targetType,
            String targetId, String outcome, String details) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            log(c, staffId, patientId, action, targetType, targetId, outcome, details);
        }
    }

    /** Audit storage is optional until the migration is applied; never block the user action. */
    public static void logBestEffort(Connection connection, long staffId, Long patientId, String action,
            String targetType, String targetId, String outcome, String details) {
        try { log(connection, staffId, patientId, action, targetType, targetId, outcome, details); }
        catch (SQLException ex) { System.err.println("Audit event could not be stored: " + ex.getMessage()); }
    }

    public static void logBestEffort(long staffId, Long patientId, String action, String targetType,
            String targetId, String outcome, String details) {
        try { log(staffId, patientId, action, targetType, targetId, outcome, details); }
        catch (SQLException ex) { System.err.println("Audit event could not be stored: " + ex.getMessage()); }
    }
}
