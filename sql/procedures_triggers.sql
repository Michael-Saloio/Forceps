USE forceps;

DELIMITER //

-- create procedure for adding a doctor acct
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
CREATE PROCEDURE book_appointment (
    IN param_appt_id BIGINT,
    IN param_patient_id BIGINT,
    IN param_doctor_id BIGINT,
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
CREATE PROCEDURE book_prospective_appointment (
    IN param_appt_id BIGINT,

    IN param_prospective_first_name VARCHAR(100),
    IN param_prospective_last_name VARCHAR(100),
    IN param_prospective_dob DATE,

    IN param_doctor_id BIGINT,
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

-- procedure to log visit / check in 
CREATE PROCEDURE log_visit_check_in (
    IN param_visit_id BIGINT,
    IN param_appt_id BIGINT,
    IN param_checked_in_by_staff_id BIGINT
)
BEGIN

    START TRANSACTION;

    INSERT INTO Visit(
        visit_id,
        appt_id,
        checked_in_by_staff_id,
        check_in_at,
        check_out_at,
        visit_status,
        row_version

    )
    VALUES(
        param_visit_id,
        param_appt_id,
        param_checked_in_by_staff_id,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        "CHECKED IN", -- can be updated to checked out in java file
        1 -- will be updated in java file when someone updates 
    );

    COMMIT;

END//

DELIMITER ;


DELIMITER //

-- procedure to grant remote access
CREATE PROCEDURE grant_remote_access (
    IN param_access_id BIGINT,
    IN param_staff_id BIGINT,
    IN param_patient_id BIGINT,
    IN param_granted_by_staff_id BIGINT,

    IN param_code_hash VARCHAR(255),
    IN param_expires_at TIMESTAMP
)
BEGIN

    START TRANSACTION;

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

