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
    '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', -- password

    'ACTIVE'
);

CALL add_nurse(
    320006, -- staff id
    11221, -- account id
    11011019, -- account created by the doctor staff id 

    'Amy',
    'Rose',

    NULL,
    'amyrose@email.com',
    'Labour and Delivery',

    '2026-11-01',
    NULL,

    'RN0001',

    'amyrose',
    '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8', -- password

    'ACTIVE'
);

CALL add_system_admin(
    90909, -- staff id
    31313, -- account id 
    320006,  -- account created by the nurse staff id 

    "Harry", 
    "Potter",

    "934-934-9349",
    "harrypotter@email.com",
    "Information Technology",

    "2026-11-17",
    NULL,

    "hpotter",
    SHA2('@Temp1000',256),

    "ACTIVE"
);

CALL add_remote_worker(
    8855123, -- staff id
    85132, -- account id 
    90909, -- account created by the system admin

    "Luigi",
    "Mario",

    NULL,
    "mariobrothers@email.com", 
    "Diagnostic Imaging", 

    "2026-10-20", 
    NULL, 

    "lmario",
    SHA2('Nintendo3D$',256),

    "ACTIVE"
);

CALL add_receptionist(
    44567890, -- staff id
    08456, -- account id 
    90909, -- account created by the system admin

    "Twilight",
    "Sparkle",

    NULL,
    "princesstwilightsparkle@email.com", 
    "Reception", 

    "2026-10-23", 
    NULL, 

    "twily7",
    SHA2('Fri3ndship!',256),

    "ACTIVE"
);


CALL add_patient(
    10001, -- patient id

    'Sonic',
    'Hedgehog',

    '1991-06-23',
    '519-123-4567',

    '401 Sunset Avenue',
    'Windsor',
    'Ontario',
    'N9B 3P4'
);


CALL book_appointment(
    00001, -- appt id
    10001, -- patient id
    11011019, -- doctor id 
    44567890, -- booked by staff id

    "2026-11-16 09:00:00", -- scheduled at appt time
    "Annual Checkup"
);

CALL book_prospective_appointment(
    00002, -- appt id

    "Barney", 
    "Dinosaur", 
    "1992-04-06",

    11011019, -- doctor id
    44567890, -- booked by receptionist staff id 

    "2026-12-01 09:00:00", -- scheduled at appt time
    "Walk-in Consultation "

);

CALL log_visit_check_in(
    100001,  -- visit id
    00001, -- appt id
    44567890, -- checked in by receptionist staff id 
);

CALL grant_remote_access(
    44440, -- access id 
    8855123, -- remote worker staff id 
    10001,  -- patient id 
    90909, -- granted by sys admin staff id
    SHA2('TEMPACCESS123',256), 
    "2026-12-31 23:59:00"
);

