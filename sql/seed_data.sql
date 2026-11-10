USE forceps;

CALL add_doctor(
    11011019, -- staff id
    12345, -- account id
    NULL,

    'Jane',
    'Doe',

    '123-456-7890',
    'drjanedoe@email.com',
    'Family Medicine',

    '2026-11-01',
    NULL,

    'PHD0001',
    'General Practitioner',

    'drjanedoe',
    '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8',

    'ACTIVE'
);

CALL add_nurse(
    320006, -- staff id
    11221, -- account id
    11011019, -- account created by the doctor staf id 

    'Amy',
    'Rose',

    NULL,
    'amyrose@email.com',
    'Labour and Delivery',

    '2026-11-01',
    NULL,

    'RN0001',

    'amyrose',
    '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8',

    'ACTIVE'
);


/*
-- sample data -> create new staff -> doctor account (role 1)
INSERT INTO Staff (
    staff_id,
    role_id,
    first_name,
    last_name,
    email,
    hire_date
) VALUES (
    11011019,
    1,
    "Jane",
    "Doe",
    "drjanedoe@email.com",
    "2026-11-01"
);

-- put this doctor into doctor table

INSERT INTO Doctor (
    staff_id,
    license_no,
    speciality

) VALUES (
    11011019,
    "PHD0001",
    "General Practitioner"
);

-- now create the account for this doctor
INSERT INTO User_account (
    account_id,
    staff_id, 
    created_by_staff_id, 
    username,
    password_hash, 
    account_status,
    created_at,
    updated_at
) VALUES (
    12345,
    11011019, 
    23, 
    "drjanedoe",
    password_hash, 
    "ACTIVE",
    "2026-11-01 06:15:00",
    "2026-11-01 06:15:00"
);
*/