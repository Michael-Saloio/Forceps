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
('Nurse');

-- create staff 
CREATE TABLE Staff (
    staff_id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(25),
    email VARCHAR(254) NOT NULL UNIQUE,
    department VARCHAR(100),
    hire_date DATE NOT NULL,
    termination_date DATE NULL,

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
    account_id BIGINT PRIMARY KEY,
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


/*
SELECT
    ua.account_id,
    s.staff_id,
    s.first_name,
    s.last_name,
    r.role_name,
    ua.password_hash
FROM User_account ua
JOIN Staff s
    ON ua.staff_id = s.staff_id
JOIN Role r
    ON s.role_id = r.role_id
WHERE ua.account_id = ?
AND ua.account_status = 'ACTIVE';
*/