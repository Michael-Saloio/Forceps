CREATE DATABASE IF NOT EXISTS forceps;
USE forceps;

-- create role
CREATE TABLE Role (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(250)
);

-- add roles for doctor and nurse
INSERT INTO Role (role_name)
VALUES
('Doctor'),
('Nurse'),
('Receptionist'), -- is regular staff
('System Administrator'), -- is regular staff
('Remote Worker'); -- is regular staff 


-- create staff ADD DATE OF BIRTH I FORGOT ABOUT IT (NOT NULL)
CREATE TABLE Staff (
    staff_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(25),
    email VARCHAR(254) NOT NULL UNIQUE,
    department VARCHAR(100),
    hire_date DATE NOT NULL,
    termination_date DATE NULL,
    termination_reason VARCHAR(500) NULL,

    FOREIGN KEY (role_id) REFERENCES Role(role_id)

);

-- doctor staff role type 
CREATE TABLE Doctor (
    staff_id BIGINT NOT NULL PRIMARY KEY,
    license_no VARCHAR(50) NOT NULL UNIQUE,
    speciality VARCHAR(100) NOT NULL,

    FOREIGN KEY (staff_id) REFERENCES Staff(staff_id)
);

-- nurse staff role type
CREATE TABLE Nurse (
    staff_id BIGINT NOT NULL PRIMARY KEY,
    license_no VARCHAR(50) NOT NULL UNIQUE,

    FOREIGN KEY (staff_id) REFERENCES Staff(staff_id)
);


-- user account to be able to login
CREATE TABLE User_account (
    account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_id BIGINT NOT NULL UNIQUE, 
    created_by_staff_id BIGINT, 
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL, 
    account_status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    FOREIGN KEY (staff_id) REFERENCES Staff(staff_id),
    FOREIGN KEY (created_by_staff_id) REFERENCES Staff(staff_id)
);
-- create patient table 
CREATE TABLE Patient (
    patient_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL, 
    phone VARCHAR(25),
    street VARCHAR(150),
    city VARCHAR(100),
    province VARCHAR(50),
    postal_code VARCHAR(15),
    created_at TIMESTAMP NOT NULL
);
-- create appt table 
CREATE TABLE Appointment (
    appt_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NULL,
    prospective_first_name VARCHAR(100) NULL,
    prospective_last_name VARCHAR(100) NULL,
    prospective_dob DATE NULL,
    doctor_id BIGINT NOT NULL,
    nurse_id BIGINT NOT NULL,
    booked_by_staff_id BIGINT NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    appt_status VARCHAR(30) NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,

    FOREIGN KEY (patient_id) REFERENCES Patient(patient_id),
    FOREIGN KEY (doctor_id) REFERENCES Doctor(staff_id),
    FOREIGN KEY (nurse_id) REFERENCES Nurse(staff_id),
    FOREIGN KEY (booked_by_staff_id) REFERENCES Staff(staff_id)
);

-- create visit table
CREATE TABLE Visit(
    visit_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    appt_id BIGINT UNIQUE NOT NULL, 
    checked_in_by_staff_id BIGINT NOT NULL,
    check_in_at TIMESTAMP NOT NULL,
    checked_out_at TIMESTAMP NULL,
    visit_status VARCHAR(30) NOT NULL, 

    FOREIGN KEY (appt_id) REFERENCES Appointment(appt_id),
    FOREIGN KEY (checked_in_by_staff_id) REFERENCES Staff(staff_id)
);

-- setup remote access
CREATE TABLE Remote_access (
    access_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    granted_by_staff_id BIGINT NOT NULL,
    code_hash VARCHAR(255) UNIQUE NOT NULL,
    granted_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    revoked_at TIMESTAMP NULL,

    FOREIGN KEY (staff_id) REFERENCES Staff(staff_id),
    FOREIGN KEY (patient_id) REFERENCES Patient(patient_id),
    FOREIGN KEY (granted_by_staff_id) REFERENCES Staff(staff_id)

);
-- Patient visit notes and diagnoses entered by clinical staff.
CREATE TABLE Medical_Record (
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

-- Patient invoices and payment tracking.
CREATE TABLE Invoice (
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

-- Append-only event history used by the System Administrator audit screen.
CREATE TABLE Audit_Log (
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
    FOREIGN KEY (staff_id) REFERENCES Staff(staff_id),
    FOREIGN KEY (patient_id) REFERENCES Patient(patient_id)
);
