USE forceps;

DELIMITER //

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

DELIMITER ;