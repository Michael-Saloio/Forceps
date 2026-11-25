-- Idempotent migration for visit-scoped vital signs and billable treatment events.
USE forceps;

SET @has_record_version = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Medical_Record' AND COLUMN_NAME = 'row_version'
);
SET @version_sql = IF(@has_record_version = 0,
    'ALTER TABLE Medical_Record ADD COLUMN row_version INT NOT NULL DEFAULT 0 AFTER remote_notes',
    'SELECT 1');
PREPARE version_stmt FROM @version_sql;
EXECUTE version_stmt;
DEALLOCATE PREPARE version_stmt;

-- Link one checkout invoice to its completed visit. Existing non-visit invoices
-- remain NULL; the correlated count avoids assigning a visit twice.
SET @has_invoice_visit = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Invoice' AND COLUMN_NAME = 'visit_id'
);
SET @invoice_visit_sql = IF(@has_invoice_visit = 0,
    'ALTER TABLE Invoice ADD COLUMN visit_id BIGINT NULL AFTER patient_id',
    'SELECT 1');
PREPARE invoice_visit_stmt FROM @invoice_visit_sql;
EXECUTE invoice_visit_stmt;
DEALLOCATE PREPARE invoice_visit_stmt;

UPDATE Invoice i
JOIN Visit v ON v.appt_id = i.appointment_id
SET i.visit_id = v.visit_id
WHERE i.visit_id IS NULL
  AND i.description = 'Visit completion fee'
  AND (SELECT COUNT(*) FROM Invoice i2 WHERE i2.appointment_id = i.appointment_id
       AND i2.description = 'Visit completion fee') = 1;

SET @has_visit_invoice_index = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Invoice' AND INDEX_NAME = 'uq_invoice_visit'
);
SET @visit_invoice_index_sql = IF(@has_visit_invoice_index = 0,
    'CREATE UNIQUE INDEX uq_invoice_visit ON Invoice(visit_id)',
    'SELECT 1');
PREPARE visit_invoice_index_stmt FROM @visit_invoice_index_sql;
EXECUTE visit_invoice_index_stmt;
DEALLOCATE PREPARE visit_invoice_index_stmt;

SET @has_visit_invoice_fk = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Invoice' AND CONSTRAINT_NAME = 'fk_invoice_visit'
);
SET @visit_invoice_fk_sql = IF(@has_visit_invoice_fk = 0,
    'ALTER TABLE Invoice ADD CONSTRAINT fk_invoice_visit FOREIGN KEY (visit_id) REFERENCES Visit(visit_id)',
    'SELECT 1');
PREPARE visit_invoice_fk_stmt FROM @visit_invoice_fk_sql;
EXECUTE visit_invoice_fk_stmt;
DEALLOCATE PREPARE visit_invoice_fk_stmt;

CREATE TABLE IF NOT EXISTS Vital_Sign (
    vital_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    visit_id BIGINT NOT NULL,
    recorded_by_staff_id BIGINT NOT NULL,
    measured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    height_cm DECIMAL(5,2) NULL,
    weight_kg DECIMAL(6,2) NULL,
    heart_rate INT NULL,
    bp_systolic INT NULL,
    bp_diastolic INT NULL,
    temperature_c DECIMAL(4,1) NULL,
    spo2 DECIMAL(5,2) NULL,
    notes VARCHAR(500) NULL,
    row_version INT NOT NULL DEFAULT 0,
    INDEX idx_vital_visit_time (visit_id, measured_at),
    FOREIGN KEY (visit_id) REFERENCES Visit(visit_id),
    FOREIGN KEY (recorded_by_staff_id) REFERENCES Staff(staff_id)
);

CREATE TABLE IF NOT EXISTS Treatment_Type (
    type_code VARCHAR(30) PRIMARY KEY,
    type_name VARCHAR(100) NOT NULL UNIQUE,
    standard_cost DECIMAL(10,2) NOT NULL CHECK (standard_cost >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO Treatment_Type(type_code, type_name, standard_cost, is_active) VALUES
('CONSULT', 'Clinical consultation', 0.00, TRUE),
('IV', 'IV therapy', 35.00, TRUE),
('INHALER', 'Inhaler treatment', 15.00, TRUE),
('VACCINE', 'Vaccine / injection', 25.00, TRUE),
('LAB', 'Sample collection', 20.00, TRUE),
('WOUND', 'Wound care', 30.00, TRUE),
('IMAGING', 'Imaging procedure', 115.00, TRUE)
ON DUPLICATE KEY UPDATE type_name=VALUES(type_name);

CREATE TABLE IF NOT EXISTS Treatment_Record (
    treatment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    visit_id BIGINT NOT NULL,
    type_code VARCHAR(30) NOT NULL,
    performed_by_staff_id BIGINT NOT NULL,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    charged_cost DECIMAL(10,2) NOT NULL CHECK (charged_cost >= 0),
    notes VARCHAR(500) NULL,
    row_version INT NOT NULL DEFAULT 0,
    INDEX idx_treatment_visit_time (visit_id, performed_at),
    FOREIGN KEY (visit_id) REFERENCES Visit(visit_id),
    FOREIGN KEY (type_code) REFERENCES Treatment_Type(type_code),
    FOREIGN KEY (performed_by_staff_id) REFERENCES Staff(staff_id)
);
