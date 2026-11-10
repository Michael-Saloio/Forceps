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

