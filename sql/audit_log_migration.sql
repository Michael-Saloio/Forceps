USE forceps;

CREATE TABLE IF NOT EXISTS Audit_Log (
    audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    patient_id BIGINT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    action_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(100) NULL,
    outcome VARCHAR(30) NOT NULL,
    details VARCHAR(500) NULL,
    INDEX idx_audit_staff_time (staff_id, occurred_at),
    INDEX idx_audit_patient_time (patient_id, occurred_at),
    INDEX idx_audit_action_time (action_type, occurred_at),
    CONSTRAINT fk_audit_staff FOREIGN KEY (staff_id) REFERENCES Staff(staff_id),
    CONSTRAINT fk_audit_patient FOREIGN KEY (patient_id) REFERENCES Patient(patient_id)
);
