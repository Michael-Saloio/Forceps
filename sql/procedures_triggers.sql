USE forceps;

DELIMITER //

-- create procedure for adding a doctor acct
DROP PROCEDURE IF EXISTS add_doctor//
CREATE PROCEDURE add_doctor(
    IN param_staff_id BIGINT,
    IN param_account_id BIGINT,
    IN param_created_by_staff_id BIGINT,

    IN param_first_name VARCHAR(100),
    IN param_last_name VARCHAR(100),

    IN param_phone VARCHAR(25),
    IN param_email VARCHAR(254),
    IN param_department VARCHAR(100),

    IN param_hire_date DATE,
    IN param_termination_date DATE,

    IN param_license_no VARCHAR(50),
    IN param_speciality VARCHAR(100),

    IN param_username VARCHAR(64),
    IN param_password_hash VARCHAR(255),

    IN param_account_status VARCHAR(30)
)
BEGIN

    START TRANSACTION;

    INSERT INTO Staff(
        staff_id,
        role_id,
        first_name,
        last_name,
        phone,
        email,
        department,
        hire_date,
        termination_date
    )
    VALUES(
        param_staff_id,
        1,
        param_first_name,
        param_last_name,
        param_phone,
        param_email,
        param_department,
        param_hire_date,
        param_termination_date
    );

    IF param_staff_id IS NULL OR param_staff_id = 0 THEN SET param_staff_id = LAST_INSERT_ID(); END IF;

    INSERT INTO Doctor(
        staff_id,
        license_no,
        speciality
    )
    VALUES(
        param_staff_id,
        param_license_no,
        param_speciality
    );

    INSERT INTO User_account(
        account_id,
        staff_id,
        created_by_staff_id,
        username,
        password_hash,
        account_status,
        created_at,
        updated_at
    )
    VALUES(
        param_account_id,
        param_staff_id,
        param_created_by_staff_id,
        param_username,
        param_password_hash,
        param_account_status,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );

    COMMIT;

END//

DELIMITER ;

DELIMITER //

-- create procedure for adding a nurse acct
DROP PROCEDURE IF EXISTS add_nurse//
CREATE PROCEDURE add_nurse(
    IN param_staff_id BIGINT,
    IN param_account_id BIGINT,
    IN param_created_by_staff_id BIGINT,

    IN param_first_name VARCHAR(100),
    IN param_last_name VARCHAR(100),

    IN param_phone VARCHAR(25),
    IN param_email VARCHAR(254),
    IN param_department VARCHAR(100),

    IN param_hire_date DATE,
    IN param_termination_date DATE,

    IN param_license_no VARCHAR(50),

    IN param_username VARCHAR(64),
    IN param_password_hash VARCHAR(255),

    IN param_account_status VARCHAR(30)
)
BEGIN

    START TRANSACTION;

    INSERT INTO Staff(
        staff_id,
        role_id,
        first_name,
        last_name,
        phone,
        email,
        department,
        hire_date,
        termination_date
    )
    VALUES(
        param_staff_id,
        2,
        param_first_name,
        param_last_name,
        param_phone,
        param_email,
        param_department,
        param_hire_date,
        param_termination_date
    );

    IF param_staff_id IS NULL OR param_staff_id = 0 THEN SET param_staff_id = LAST_INSERT_ID(); END IF;

    INSERT INTO Nurse(
        staff_id,
        license_no
    )
    VALUES(
        param_staff_id,
        param_license_no
    );

    INSERT INTO User_account(
        account_id,
        staff_id,
        created_by_staff_id,
        username,
        password_hash,
        account_status,
        created_at,
        updated_at
    )
    VALUES(
        param_account_id,
        param_staff_id,
        param_created_by_staff_id,
        param_username,
        param_password_hash,
        param_account_status,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );

    COMMIT;

END//

DELIMITER //

-- create system admin procedure
DROP PROCEDURE IF EXISTS add_system_admin//
CREATE PROCEDURE add_system_admin (
    IN param_staff_id BIGINT,
    IN param_account_id BIGINT,
    IN param_created_by_staff_id BIGINT,

    IN param_first_name VARCHAR(100),
    IN param_last_name VARCHAR(100),

    IN param_phone VARCHAR(25),
    IN param_email VARCHAR(254),
    IN param_department VARCHAR(100),

    IN param_hire_date DATE,
    IN param_termination_date DATE,

    IN param_username VARCHAR(64),
    IN param_password_hash VARCHAR(255),

    IN param_account_status VARCHAR(30)
)
BEGIN

    START TRANSACTION;

    INSERT INTO Staff(
        staff_id,
        role_id,
        first_name,
        last_name,
        phone,
        email,
        department,
        hire_date,
        termination_date
    )
    VALUES(
        param_staff_id,
        4,
        param_first_name,
        param_last_name,
        param_phone,
        param_email,
        param_department,
        param_hire_date,
        param_termination_date
    );

    IF param_staff_id IS NULL OR param_staff_id = 0 THEN SET param_staff_id = LAST_INSERT_ID(); END IF;

    INSERT INTO User_account(
        account_id,
        staff_id,
        created_by_staff_id,
        username,
        password_hash,
        account_status,
        created_at,
        updated_at
    )
    VALUES(
        param_account_id,
        param_staff_id,
        param_created_by_staff_id,
        param_username,
        param_password_hash,
        param_account_status,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );

    COMMIT;

END//

DELIMITER ;

DELIMITER //

-- create remote worker procedure
DROP PROCEDURE IF EXISTS add_remote_worker//
CREATE PROCEDURE add_remote_worker (
    IN param_staff_id BIGINT,
    IN param_account_id BIGINT,
    IN param_created_by_staff_id BIGINT,

    IN param_first_name VARCHAR(100),
    IN param_last_name VARCHAR(100),

    IN param_phone VARCHAR(25),
    IN param_email VARCHAR(254),
    IN param_department VARCHAR(100),

    IN param_hire_date DATE,
    IN param_termination_date DATE,

    IN param_username VARCHAR(64),
    IN param_password_hash VARCHAR(255),

    IN param_account_status VARCHAR(30)
)
BEGIN

    START TRANSACTION;

    INSERT INTO Staff(
        staff_id,
        role_id,
        first_name,
        last_name,
        phone,
        email,
        department,
        hire_date,
        termination_date
    )
    VALUES(
        param_staff_id,
        5,
        param_first_name,
        param_last_name,
        param_phone,
        param_email,
        param_department,
        param_hire_date,
        param_termination_date
    );

    IF param_staff_id IS NULL OR param_staff_id = 0 THEN SET param_staff_id = LAST_INSERT_ID(); END IF;

    INSERT INTO User_account(
        account_id,
        staff_id,
        created_by_staff_id,
        username,
        password_hash,
        account_status,
        created_at,
        updated_at
    )
    VALUES(
        param_account_id,
        param_staff_id,
        param_created_by_staff_id,
        param_username,
        param_password_hash,
        param_account_status,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );

    COMMIT;

END//

DELIMITER ;

DELIMITER //

-- create receptionist procedure
DROP PROCEDURE IF EXISTS add_receptionist//
CREATE PROCEDURE add_receptionist (
    IN param_staff_id BIGINT,
    IN param_account_id BIGINT,
    IN param_created_by_staff_id BIGINT,

    IN param_first_name VARCHAR(100),
    IN param_last_name VARCHAR(100),

    IN param_phone VARCHAR(25),
    IN param_email VARCHAR(254),
    IN param_department VARCHAR(100),

    IN param_hire_date DATE,
    IN param_termination_date DATE,

    IN param_username VARCHAR(64),
    IN param_password_hash VARCHAR(255),

    IN param_account_status VARCHAR(30)
)
BEGIN

    START TRANSACTION;

    INSERT INTO Staff(
        staff_id,
        role_id,
        first_name,
        last_name,
        phone,
        email,
        department,
        hire_date,
        termination_date
    )
    VALUES(
        param_staff_id,
        3,
        param_first_name,
        param_last_name,
        param_phone,
        param_email,
        param_department,
        param_hire_date,
        param_termination_date
    );

    IF param_staff_id IS NULL OR param_staff_id = 0 THEN SET param_staff_id = LAST_INSERT_ID(); END IF;

    INSERT INTO User_account(
        account_id,
        staff_id,
        created_by_staff_id,
        username,
        password_hash,
        account_status,
        created_at,
        updated_at
    )
    VALUES(
        param_account_id,
        param_staff_id,
        param_created_by_staff_id,
        param_username,
        param_password_hash,
        param_account_status,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );

    COMMIT;

END//

DELIMITER ;


DELIMITER //

-- new patient procedure
DROP PROCEDURE IF EXISTS add_patient//
CREATE PROCEDURE add_patient (
    IN param_patient_id BIGINT,

    IN param_first_name VARCHAR(100),
    IN param_last_name VARCHAR(100),

    IN param_date_of_birth DATE,
    IN param_phone VARCHAR(25),

    IN param_street VARCHAR(150),
    IN param_city VARCHAR(100),
    IN param_province VARCHAR(50),
    IN param_postal_code VARCHAR(15)
)
BEGIN

    START TRANSACTION;

    INSERT INTO Patient(
        patient_id,
        first_name,
        last_name,
        date_of_birth,
        phone,
        street,
        city,
        province,
        postal_code,
        created_at
    )
    VALUES (
        param_patient_id,
        param_first_name,
        param_last_name,
        param_date_of_birth,
        param_phone,
        param_street,
        param_city, 
        param_province,
        param_postal_code,
        CURRENT_TIMESTAMP
    );

    COMMIT;

END//

DELIMITER ;

DELIMITER //

-- book appt for EXISTING PATIENT procedure
DROP PROCEDURE IF EXISTS book_appointment//
CREATE PROCEDURE book_appointment (
    IN param_appt_id BIGINT,
    IN param_patient_id BIGINT,
    IN param_doctor_id BIGINT,
    IN param_nurse_id BIGINT,
    IN param_booked_by_staff_id BIGINT,

    IN param_scheduled_at TIMESTAMP,
    IN param_reason VARCHAR(500)
)
BEGIN

    START TRANSACTION;

    INSERT INTO Appointment(
        appt_id,
        patient_id,
        doctor_id,
        nurse_id,
        booked_by_staff_id,
        scheduled_at,
        appt_status,
        reason,
        created_at
    )
    VALUES(
        param_appt_id,
        param_patient_id,
        param_doctor_id,
        param_nurse_id,
        param_booked_by_staff_id,
        param_scheduled_at,
        "SCHEDULED",
        param_reason,
        CURRENT_TIMESTAMP
    );

    COMMIT;

END//

DELIMITER ;

DELIMITER //

-- book prospective patient / walk in appointment procedure
DROP PROCEDURE IF EXISTS book_prospective_appointment//
CREATE PROCEDURE book_prospective_appointment (
    IN param_appt_id BIGINT,

    IN param_prospective_first_name VARCHAR(100),
    IN param_prospective_last_name VARCHAR(100),
    IN param_prospective_dob DATE,

    IN param_doctor_id BIGINT,
    IN param_nurse_id BIGINT,
    IN param_booked_by_staff_id BIGINT,

    IN param_scheduled_at TIMESTAMP,
    IN param_reason VARCHAR(500)
)
BEGIN

    START TRANSACTION;

    INSERT INTO Appointment(
        appt_id,
        patient_id,
        prospective_first_name,
        prospective_last_name,
        prospective_dob,
        doctor_id,
        nurse_id,
        booked_by_staff_id,
        scheduled_at,
        appt_status,
        reason,
        created_at
    )
    VALUES(
        param_appt_id,
        NULL,
        param_prospective_first_name,
        param_prospective_last_name,
        param_prospective_dob,
        param_doctor_id,
        param_nurse_id,
        param_booked_by_staff_id,
        param_scheduled_at,
        "SCHEDULED",
        param_reason,
        CURRENT_TIMESTAMP
    );

    COMMIT;

END//

DELIMITER ;

DELIMITER //

-- procedure to cancel an appointment (not delete)
DROP PROCEDURE IF EXISTS cancel_appointment//
CREATE PROCEDURE cancel_appointment (
    IN param_appt_id BIGINT
)
BEGIN

    START TRANSACTION;

    UPDATE Appointment
    SET appt_status = 'CANCELLED'
    WHERE appt_id = param_appt_id;

    COMMIT;

END//

DELIMITER ;

DELIMITER //

-- procedure to delete an appointment
DROP PROCEDURE IF EXISTS delete_appointment//
CREATE PROCEDURE delete_appointment (
    IN param_appt_id BIGINT
)
BEGIN

    START TRANSACTION;

    IF NOT EXISTS (SELECT 1 FROM Appointment WHERE appt_id = param_appt_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Appointment not found.';
    END IF;

    IF EXISTS (SELECT 1 FROM Visit WHERE appt_id = param_appt_id) THEN
        UPDATE Appointment
        SET appt_status = 'CANCELLED'
        WHERE appt_id = param_appt_id;
    ELSE
        DELETE FROM Appointment
        WHERE appt_id = param_appt_id;
    END IF;

    COMMIT;

END//

DELIMITER ;

DELIMITER //

-- procedure to log visit / check in 
DROP PROCEDURE IF EXISTS log_visit_check_in//
CREATE PROCEDURE log_visit_check_in (
    IN param_visit_id BIGINT,
    IN param_appt_id BIGINT,
    IN param_checked_in_by_staff_id BIGINT
)
BEGIN

    IF NOT EXISTS (
        SELECT 1 FROM Appointment
        WHERE appt_id = param_appt_id
          AND patient_id IS NOT NULL
          AND appt_status <> 'CANCELLED'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Register the patient and use an active appointment before check-in.';
    END IF;

    IF EXISTS (SELECT 1 FROM Visit WHERE appt_id = param_appt_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A visit is already recorded for this appointment.';
    END IF;

    INSERT INTO Visit(
        visit_id,
        appt_id,
        checked_in_by_staff_id,
        check_in_at,
        visit_status

    )
    VALUES(
        param_visit_id,
        param_appt_id,
        param_checked_in_by_staff_id,
        CURRENT_TIMESTAMP,
        "CHECKED IN" -- can be updated to checked out in java file
    );

    UPDATE Appointment SET appt_status = 'CHECKED IN' WHERE appt_id = param_appt_id;

END//

DELIMITER ;

DELIMITER //

-- Register a prospective patient and check them in atomically at their first visit.
DROP PROCEDURE IF EXISTS register_and_check_in_prospective//
CREATE PROCEDURE register_and_check_in_prospective(
    IN p_appt_id BIGINT,
    IN p_checked_in_by_staff_id BIGINT,
    OUT p_patient_id BIGINT,
    OUT p_visit_id BIGINT
)
BEGIN
    DECLARE v_first VARCHAR(100);
    DECLARE v_last VARCHAR(100);
    DECLARE v_dob DATE;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;

    START TRANSACTION;
    SELECT prospective_first_name, prospective_last_name, prospective_dob
      INTO v_first, v_last, v_dob
      FROM Appointment
     WHERE appt_id=p_appt_id AND patient_id IS NULL AND appt_status IN ('SCHEDULED','BOOKED')
     FOR UPDATE;
    IF v_first IS NULL OR v_last IS NULL OR v_dob IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Appointment is not an eligible prospective-patient booking.';
    END IF;
    IF EXISTS (SELECT 1 FROM Visit WHERE appt_id=p_appt_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A visit is already recorded for this appointment.';
    END IF;

    INSERT INTO Patient(first_name,last_name,date_of_birth,created_at)
    VALUES(v_first,v_last,v_dob,CURRENT_TIMESTAMP);
    SET p_patient_id=LAST_INSERT_ID();
    UPDATE Appointment SET patient_id=p_patient_id, prospective_first_name=NULL,
        prospective_last_name=NULL, prospective_dob=NULL, appt_status='CHECKED IN'
     WHERE appt_id=p_appt_id;
    INSERT INTO Visit(appt_id,checked_in_by_staff_id,check_in_at,visit_status)
    VALUES(p_appt_id,p_checked_in_by_staff_id,CURRENT_TIMESTAMP,'CHECKED IN');
    SET p_visit_id=LAST_INSERT_ID();
    COMMIT;
END//

DELIMITER ;

DELIMITER //

DROP PROCEDURE IF EXISTS mark_appointment_no_show//
CREATE PROCEDURE mark_appointment_no_show(IN p_appt_id BIGINT)
BEGIN
    UPDATE Appointment SET appt_status='NO_SHOW'
     WHERE appt_id=p_appt_id AND appt_status IN ('SCHEDULED','BOOKED')
       AND NOT EXISTS (SELECT 1 FROM Visit WHERE Visit.appt_id=Appointment.appt_id);
    IF ROW_COUNT()=0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Appointment cannot be marked no-show.';
    END IF;
END//

DELIMITER ;

DELIMITER //

-- procedure to log visit / check out
DROP PROCEDURE IF EXISTS log_visit_check_out//
CREATE PROCEDURE log_visit_check_out (
    IN param_visit_id BIGINT
)
BEGIN

    UPDATE Visit
    SET checked_out_at = CURRENT_TIMESTAMP,
        visit_status = 'CHECKED OUT'
    WHERE visit_id = param_visit_id
      AND visit_status = 'CHECKED IN';

    IF ROW_COUNT() = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Visit is not checked in or has already been checked out.';
    END IF;

END//

DELIMITER ;

-- Generate one standard visit invoice when a visit transitions to CHECKED OUT.
-- Reception can find it in Billing and use Print Invoice / Save as PDF.
DELIMITER //

DROP TRIGGER IF EXISTS trg_visit_checkout_invoice//
CREATE TRIGGER trg_visit_checkout_invoice
AFTER UPDATE ON Visit
FOR EACH ROW
BEGIN
    IF NEW.visit_status = 'CHECKED OUT'
       AND (OLD.visit_status IS NULL OR OLD.visit_status <> 'CHECKED OUT')
       AND NOT EXISTS (
           SELECT 1
           FROM Invoice i
           WHERE i.appointment_id = NEW.appt_id
             AND i.description = 'Visit completion fee'
       ) THEN
        INSERT INTO Invoice (
            patient_id,
            appointment_id,
            created_by_staff_id,
            invoice_date,
            description,
            amount,
            status,
            due_date
        )
        SELECT
            a.patient_id,
            a.appt_id,
            NEW.checked_in_by_staff_id,
            CURRENT_DATE,
            'Visit completion fee',
            10.00,
            'UNPAID',
            DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY)
        FROM Appointment a
        WHERE a.appt_id = NEW.appt_id;
    END IF;
END//

DELIMITER ;


DELIMITER //

-- procedure to grant remote access
DROP PROCEDURE IF EXISTS grant_remote_access//
CREATE PROCEDURE grant_remote_access (
    IN param_access_id BIGINT,
    IN param_staff_id BIGINT,
    IN param_patient_id BIGINT,
    IN param_granted_by_staff_id BIGINT,

    IN param_code_hash VARCHAR(255),
    IN param_expires_at TIMESTAMP
)
BEGIN
    DECLARE worker_is_active INT DEFAULT 0;
    DECLARE patient_exists INT DEFAULT 0;
    DECLARE administrator_is_active INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF param_staff_id IS NULL OR param_patient_id IS NULL OR param_granted_by_staff_id IS NULL
       OR param_code_hash IS NULL OR CHAR_LENGTH(TRIM(param_code_hash)) = 0 OR param_expires_at IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Worker, patient, administrator, code, and expiry are required.';
    END IF;
    IF param_expires_at <= CURRENT_TIMESTAMP THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'The access expiry must be in the future.';
    END IF;

    START TRANSACTION;

    SELECT COUNT(*) INTO worker_is_active
    FROM Staff s JOIN Role r ON r.role_id = s.role_id
    WHERE s.staff_id = param_staff_id AND r.role_name = 'Remote Worker' AND s.termination_date IS NULL;
    IF worker_is_active = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'The selected staff member is not an active remote worker.';
    END IF;

    SELECT COUNT(*) INTO patient_exists FROM Patient WHERE patient_id = param_patient_id;
    IF patient_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'The selected patient was not found.';
    END IF;

    SELECT COUNT(*) INTO administrator_is_active
    FROM Staff s JOIN Role r ON r.role_id = s.role_id
    WHERE s.staff_id = param_granted_by_staff_id AND r.role_name = 'System Administrator' AND s.termination_date IS NULL;
    IF administrator_is_active = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'An active system administrator is required to grant access.';
    END IF;

    INSERT INTO Remote_access(
        access_id,
        staff_id,
        patient_id,
        granted_by_staff_id,
        code_hash,
        granted_at,
        expires_at,
        consumed_at
    )
    VALUES(
        param_access_id,
        param_staff_id,
        param_patient_id,
        param_granted_by_staff_id,
        param_code_hash,
        CURRENT_TIMESTAMP,
        param_expires_at,
        NULL
    );

    COMMIT;

END//

DELIMITER ;


DELIMITER //

-- procedure to revoke remote access
DROP PROCEDURE IF EXISTS revoke_remote_access//
CREATE PROCEDURE revoke_remote_access (
    IN param_access_id BIGINT
)
BEGIN
    DECLARE existing_access_id BIGINT DEFAULT NULL;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF param_access_id IS NULL OR param_access_id <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A valid access ID is required.';
    END IF;

    START TRANSACTION;

    SELECT access_id INTO existing_access_id
    FROM Remote_access
    WHERE access_id = param_access_id AND revoked_at IS NULL
    FOR UPDATE;

    IF existing_access_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No active grant was found for that access ID.';
    END IF;

    UPDATE Remote_access
    SET revoked_at = CURRENT_TIMESTAMP
    WHERE access_id = param_access_id AND revoked_at IS NULL;

    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'The remote access grant could not be revoked.';
    END IF;

    COMMIT;

END//

DELIMITER ;


DELIMITER //

-- procedure to offboard an employee (does not delete them)
DROP PROCEDURE IF EXISTS offboard_employee//
CREATE PROCEDURE offboard_employee (
    IN param_staff_id BIGINT,
    IN param_termination_date DATE,
    IN param_termination_reason VARCHAR(500)
)
BEGIN
    DECLARE existing_termination_date DATE DEFAULT NULL;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF param_staff_id IS NULL OR param_staff_id <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A valid staff ID is required.';
    END IF;
    IF param_termination_date IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A termination date is required.';
    END IF;
    IF param_termination_reason IS NULL OR CHAR_LENGTH(TRIM(param_termination_reason)) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A termination reason is required.';
    END IF;

    START TRANSACTION;

    SELECT termination_date INTO existing_termination_date
    FROM Staff
    WHERE staff_id = param_staff_id
    FOR UPDATE;

    IF existing_termination_date IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'This staff member is already offboarded.';
    END IF;

    UPDATE Staff
    SET termination_date = param_termination_date,
        termination_reason = param_termination_reason
    WHERE staff_id = param_staff_id;

    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Staff member was not found.';
    END IF;

    UPDATE User_account
    SET account_status = 'DEACTIVATED',
        updated_at = CURRENT_TIMESTAMP
    WHERE staff_id = param_staff_id;

    UPDATE Remote_access
    SET revoked_at = CURRENT_TIMESTAMP
    WHERE staff_id = param_staff_id AND revoked_at IS NULL;

    COMMIT;

END//

DELIMITER ;

DELIMITER //

DROP PROCEDURE IF EXISTS update_user_account_access//
CREATE PROCEDURE update_user_account_access(
    IN p_account_id BIGINT,
    IN p_actor_staff_id BIGINT,
    IN p_new_status VARCHAR(30),
    IN p_new_password_hash VARCHAR(255)
)
BEGIN
    IF NOT EXISTS(SELECT 1 FROM Staff s JOIN Role r ON r.role_id=s.role_id
        WHERE s.staff_id=p_actor_staff_id AND r.role_name='System Administrator' AND s.termination_date IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Only an active System Administrator can manage accounts.';
    END IF;
    IF p_new_status NOT IN ('ACTIVE','DEACTIVATED') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Account status must be ACTIVE or DEACTIVATED.';
    END IF;
    IF p_new_status='ACTIVE' AND EXISTS(
        SELECT 1 FROM User_account ua JOIN Staff s ON s.staff_id=ua.staff_id
        WHERE ua.account_id=p_account_id AND s.termination_date IS NOT NULL
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A terminated employee account cannot be activated.';
    END IF;
    UPDATE User_account SET account_status=p_new_status,
        password_hash=COALESCE(p_new_password_hash,password_hash), updated_at=CURRENT_TIMESTAMP
    WHERE account_id=p_account_id;
    IF ROW_COUNT()=0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='User account was not found.';
    END IF;
END//

DELIMITER ;




