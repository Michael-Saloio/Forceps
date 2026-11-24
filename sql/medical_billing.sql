USE forceps;

-- Apply after Patient, Staff, and Appointment have been created.
CREATE TABLE IF NOT EXISTS Medical_Record (
    record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    visit_id BIGINT NULL,
    clinician_staff_id BIGINT NOT NULL,
    visit_date DATE NOT NULL,
    height_cm DECIMAL(5,2) NULL,
    weight_kg DECIMAL(6,2) NULL,
    vitals_summary VARCHAR(500) NULL,
    diagnosis VARCHAR(500) NOT NULL,
    treatment TEXT,
    notes TEXT,
    remote_notes TEXT NULL,
    row_version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_medical_record_patient_visit (patient_id, visit_date),
    CONSTRAINT chk_medical_record_height CHECK (height_cm IS NULL OR (height_cm > 0 AND height_cm <= 300)),
    CONSTRAINT chk_medical_record_weight CHECK (weight_kg IS NULL OR (weight_kg > 0 AND weight_kg <= 1000)),
    FOREIGN KEY (patient_id) REFERENCES Patient(patient_id),
    FOREIGN KEY (visit_id) REFERENCES Visit(visit_id),
    FOREIGN KEY (clinician_staff_id) REFERENCES Staff(staff_id)
);

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
('CONSULT', 'Clinical consultation', 0.00, TRUE), ('IV', 'IV therapy', 35.00, TRUE),
('INHALER', 'Inhaler treatment', 15.00, TRUE), ('VACCINE', 'Vaccine / injection', 25.00, TRUE),
('LAB', 'Sample collection', 20.00, TRUE), ('WOUND', 'Wound care', 30.00, TRUE),
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

CREATE TABLE IF NOT EXISTS Invoice (
    invoice_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    visit_id BIGINT NULL UNIQUE,
    appointment_id BIGINT NULL,
    created_by_staff_id BIGINT NOT NULL,
    invoice_date DATE NOT NULL,
    description VARCHAR(500) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status ENUM('UNPAID', 'PAID') NOT NULL DEFAULT 'UNPAID',
    due_date DATE NOT NULL,
    paid_date DATE NULL,
    paid_by_staff_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_invoice_patient_date (patient_id, invoice_date),
    CONSTRAINT chk_invoice_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_invoice_payment_status CHECK (
        (status = 'UNPAID' AND paid_date IS NULL AND paid_by_staff_id IS NULL) OR
        (status = 'PAID' AND paid_date IS NOT NULL AND paid_by_staff_id IS NOT NULL)
    ),
    FOREIGN KEY (patient_id) REFERENCES Patient(patient_id),
    FOREIGN KEY (visit_id) REFERENCES Visit(visit_id),
    FOREIGN KEY (appointment_id) REFERENCES Appointment(appt_id),
    FOREIGN KEY (created_by_staff_id) REFERENCES Staff(staff_id),
    FOREIGN KEY (paid_by_staff_id) REFERENCES Staff(staff_id)
);
