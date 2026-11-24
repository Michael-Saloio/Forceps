USE forceps;
DELIMITER //

DROP PROCEDURE IF EXISTS add_medical_record//
CREATE PROCEDURE add_medical_record(
    IN p_patient_id BIGINT,
    IN p_visit_id BIGINT,
    IN p_clinician_staff_id BIGINT,
    IN p_height_cm DECIMAL(5,2),
    IN p_weight_kg DECIMAL(6,2),
    IN p_vitals_summary VARCHAR(500),
    IN p_diagnosis VARCHAR(500),
    IN p_treatment TEXT,
    IN p_notes TEXT
)
BEGIN
    DECLARE v_visit_date DATE DEFAULT CURRENT_DATE;
    IF p_visit_id IS NOT NULL THEN
        SET v_visit_date = NULL;
        SELECT DATE(v.check_in_at) INTO v_visit_date
        FROM Visit v JOIN Appointment a ON a.appt_id = v.appt_id
        WHERE v.visit_id = p_visit_id AND a.patient_id = p_patient_id
        LIMIT 1;
        IF v_visit_date IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'The visit does not belong to this patient.';
        END IF;
    END IF;

    INSERT INTO Medical_Record(patient_id, visit_id, clinician_staff_id, visit_date, height_cm, weight_kg,
        vitals_summary, diagnosis, treatment, notes)
    VALUES(p_patient_id, p_visit_id, p_clinician_staff_id, v_visit_date, p_height_cm, p_weight_kg,
        p_vitals_summary, p_diagnosis, p_treatment, p_notes);
END//

DROP PROCEDURE IF EXISTS update_medical_record//
CREATE PROCEDURE update_medical_record(
    IN p_record_id BIGINT,
    IN p_height_cm DECIMAL(5,2),
    IN p_weight_kg DECIMAL(6,2),
    IN p_vitals_summary VARCHAR(500),
    IN p_diagnosis VARCHAR(500),
    IN p_treatment TEXT,
    IN p_notes TEXT,
    IN p_expected_version INT
)
BEGIN
    UPDATE Medical_Record SET height_cm=p_height_cm, weight_kg=p_weight_kg, vitals_summary=p_vitals_summary,
        diagnosis=p_diagnosis, treatment=p_treatment, notes=p_notes, row_version=row_version+1
    WHERE record_id=p_record_id AND row_version=p_expected_version;
    IF ROW_COUNT()=0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'This record changed since you opened it. Refresh the chart and review the latest version before editing again.';
    END IF;
END//

DROP PROCEDURE IF EXISTS add_treatment_record//
CREATE PROCEDURE add_treatment_record(
    IN p_visit_id BIGINT,
    IN p_type_code VARCHAR(30),
    IN p_staff_id BIGINT,
    IN p_notes VARCHAR(500)
)
BEGIN
    IF NOT EXISTS(
        SELECT 1 FROM Visit v JOIN Appointment a ON a.appt_id=v.appt_id
        JOIN Staff s ON s.staff_id=p_staff_id JOIN Role r ON r.role_id=s.role_id
        JOIN Treatment_Type tt ON tt.type_code=p_type_code AND tt.is_active=TRUE
        WHERE v.visit_id=p_visit_id AND v.visit_status='CHECKED IN'
          AND a.patient_id IS NOT NULL AND s.termination_date IS NULL
          AND r.role_name IN ('Doctor','Nurse')
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A valid active visit and active doctor or nurse are required to record treatment.';
    END IF;
    INSERT INTO Treatment_Record(visit_id,type_code,performed_by_staff_id,performed_at,charged_cost,notes)
    SELECT p_visit_id,tt.type_code,p_staff_id,CURRENT_TIMESTAMP,tt.standard_cost,p_notes
    FROM Treatment_Type tt WHERE tt.type_code=p_type_code AND tt.is_active=TRUE;
END//

DROP PROCEDURE IF EXISTS add_visit_vital_sign//
CREATE PROCEDURE add_visit_vital_sign(
    IN p_visit_id BIGINT,
    IN p_staff_id BIGINT,
    IN p_height_cm DECIMAL(5,2),
    IN p_weight_kg DECIMAL(6,2),
    IN p_heart_rate INT,
    IN p_bp_systolic INT,
    IN p_bp_diastolic INT,
    IN p_temperature_c DECIMAL(4,1),
    IN p_spo2 DECIMAL(5,2),
    IN p_notes VARCHAR(500)
)
BEGIN
    IF NOT EXISTS(SELECT 1 FROM Visit v JOIN Staff s ON s.staff_id=p_staff_id JOIN Role r ON r.role_id=s.role_id
        WHERE v.visit_id=p_visit_id AND v.visit_status='CHECKED IN' AND s.termination_date IS NULL
          AND r.role_name IN ('Doctor','Nurse')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='An active visit and active doctor or nurse are required to record vital signs.';
    END IF;
    INSERT INTO Vital_Sign(visit_id,recorded_by_staff_id,measured_at,height_cm,weight_kg,heart_rate,bp_systolic,bp_diastolic,temperature_c,spo2,notes)
    VALUES(p_visit_id,p_staff_id,CURRENT_TIMESTAMP,p_height_cm,p_weight_kg,p_heart_rate,p_bp_systolic,p_bp_diastolic,p_temperature_c,p_spo2,p_notes);
END//

DROP PROCEDURE IF EXISTS create_invoice//
CREATE PROCEDURE create_invoice(
    IN p_patient_id BIGINT,
    IN p_appointment_id BIGINT,
    IN p_created_by_staff_id BIGINT,
    IN p_description VARCHAR(500),
    IN p_amount DECIMAL(10,2),
    IN p_due_date DATE
)
BEGIN
    DECLARE v_appointment_patient_id BIGINT DEFAULT NULL;
    IF p_description = 'Visit completion fee' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Visit completion invoices are generated automatically at checkout.';
    END IF;
    IF p_appointment_id IS NOT NULL THEN
        SELECT patient_id INTO v_appointment_patient_id FROM Appointment WHERE appt_id=p_appointment_id LIMIT 1;
        IF v_appointment_patient_id IS NULL OR v_appointment_patient_id <> p_patient_id THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Appointment does not belong to the selected patient.';
        END IF;
    END IF;
    INSERT INTO Invoice(patient_id, appointment_id, created_by_staff_id, invoice_date, description, amount, status, due_date)
    VALUES(p_patient_id, p_appointment_id, p_created_by_staff_id, CURRENT_DATE, p_description, p_amount, 'UNPAID', p_due_date);
END//

DROP PROCEDURE IF EXISTS update_invoice//
CREATE PROCEDURE update_invoice(
    IN p_invoice_id BIGINT,
    IN p_appointment_id BIGINT,
    IN p_description VARCHAR(500),
    IN p_amount DECIMAL(10,2),
    IN p_due_date DATE
)
BEGIN
    DECLARE v_patient_id BIGINT DEFAULT NULL;
    IF p_description = 'Visit completion fee' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Visit completion invoices are generated automatically at checkout.';
    END IF;
    SELECT patient_id INTO v_patient_id FROM Invoice WHERE invoice_id=p_invoice_id AND status='UNPAID' LIMIT 1;
    IF v_patient_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invoice is paid or no longer exists.';
    END IF;
    IF p_appointment_id IS NOT NULL AND NOT EXISTS(
        SELECT 1 FROM Appointment WHERE appt_id=p_appointment_id AND patient_id=v_patient_id
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Appointment does not belong to the invoice patient.';
    END IF;
    UPDATE Invoice SET appointment_id=p_appointment_id, description=p_description, amount=p_amount, due_date=p_due_date
    WHERE invoice_id=p_invoice_id AND status='UNPAID';
END//

DROP PROCEDURE IF EXISTS mark_invoice_paid//
CREATE PROCEDURE mark_invoice_paid(IN p_invoice_id BIGINT, IN p_paid_by_staff_id BIGINT)
BEGIN
    UPDATE Invoice SET status='PAID', paid_date=CURRENT_DATE, paid_by_staff_id=p_paid_by_staff_id
    WHERE invoice_id=p_invoice_id AND status='UNPAID';
    IF ROW_COUNT() = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invoice is already paid or no longer exists.';
    END IF;
END//

DROP PROCEDURE IF EXISTS assign_appointment_nurse//
CREATE PROCEDURE assign_appointment_nurse(IN p_appt_id BIGINT, IN p_nurse_id BIGINT)
BEGIN
    IF NOT EXISTS(SELECT 1 FROM Appointment WHERE appt_id=p_appt_id AND appt_status <> 'CANCELLED') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Appointment not found or already cancelled.';
    END IF;
    UPDATE Appointment SET nurse_id=p_nurse_id
    WHERE appt_id=p_appt_id AND appt_status <> 'CANCELLED';
END//

DROP PROCEDURE IF EXISTS consume_remote_access//
CREATE PROCEDURE consume_remote_access(
    IN p_access_id BIGINT,
    IN p_staff_id BIGINT,
    IN p_code_hash VARCHAR(255),
    OUT p_patient_id BIGINT
)
BEGIN
    SET p_patient_id = NULL;
    SELECT patient_id INTO p_patient_id FROM Remote_access
    WHERE access_id=p_access_id AND staff_id=p_staff_id AND code_hash=p_code_hash
        AND consumed_at IS NULL AND revoked_at IS NULL AND expires_at>CURRENT_TIMESTAMP
    LIMIT 1 FOR UPDATE;
    IF p_patient_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Access grant is invalid, expired, or revoked.';
    END IF;
    UPDATE Remote_access SET consumed_at=CURRENT_TIMESTAMP
    WHERE access_id=p_access_id AND staff_id=p_staff_id AND consumed_at IS NULL
        AND revoked_at IS NULL AND expires_at>CURRENT_TIMESTAMP;
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Access grant has already been used.';
    END IF;
END//

DROP PROCEDURE IF EXISTS save_remote_patient_note//
CREATE PROCEDURE save_remote_patient_note(
    IN p_access_id BIGINT,
    IN p_staff_id BIGINT,
    IN p_patient_id BIGINT,
    IN p_record_id BIGINT,
    IN p_remote_notes TEXT
)
BEGIN
    IF NOT EXISTS(
        SELECT 1 FROM Medical_Record mr JOIN Remote_access ra ON ra.patient_id=mr.patient_id
        WHERE mr.record_id=p_record_id AND mr.patient_id=p_patient_id AND ra.access_id=p_access_id
            AND ra.staff_id=p_staff_id AND ra.consumed_at IS NOT NULL
            AND ra.revoked_at IS NULL AND ra.expires_at>CURRENT_TIMESTAMP
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'There is no valid active grant for this patient record.';
    END IF;
    UPDATE Medical_Record mr SET mr.remote_notes=p_remote_notes
    WHERE mr.record_id=p_record_id AND mr.patient_id=p_patient_id AND EXISTS(
        SELECT 1 FROM Remote_access ra WHERE ra.access_id=p_access_id AND ra.staff_id=p_staff_id
            AND ra.patient_id=mr.patient_id AND ra.consumed_at IS NOT NULL
            AND ra.revoked_at IS NULL AND ra.expires_at>CURRENT_TIMESTAMP
    );
END//

DELIMITER ;
