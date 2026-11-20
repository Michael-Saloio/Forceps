-- One-time migration for an existing Forceps database.
-- Fresh installations already receive these changes from sql/schema.sql.
USE forceps;

-- Permit the application to let MySQL allocate record IDs.
ALTER TABLE Staff MODIFY COLUMN staff_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE User_account MODIFY COLUMN account_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE Patient MODIFY COLUMN patient_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE Appointment MODIFY COLUMN appt_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE Remote_access MODIFY COLUMN access_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE Visit MODIFY COLUMN visit_id BIGINT NOT NULL AUTO_INCREMENT;

-- Existing appointments remain unassigned until a receptionist updates them.
ALTER TABLE Appointment
    ADD COLUMN nurse_id BIGINT NULL AFTER doctor_id,
    ADD CONSTRAINT fk_appointment_nurse
        FOREIGN KEY (nurse_id) REFERENCES Nurse(staff_id);
