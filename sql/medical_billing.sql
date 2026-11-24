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

CREATE TABLE IF NOT EXISTS Invoice (
    invoice_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
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
    FOREIGN KEY (appointment_id) REFERENCES Appointment(appt_id),
    FOREIGN KEY (created_by_staff_id) REFERENCES Staff(staff_id),
    FOREIGN KEY (paid_by_staff_id) REFERENCES Staff(staff_id)
);
