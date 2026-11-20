USE forceps;

-- DOCTORS 
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
-- NURSES 
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
-- SYSTEM ADMINS 
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
-- REMOTE WORKERS 
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
-- RECEPTIONISTS 
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
-- PATIENTS 
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
CALL add_patient(
    10002, -- patient id
    'Nikki',
    'Momo',
    '2000-12-05',
    '000-111-2233',
    '123 Nikki St',
    'Florawish',
    'Heartcraft Kingdom',
    'A1B 2C3'
);CALL add_patient(
    10003, -- patient id
    'Benjamin',
    'Tennyson',
    '2010-10-10',
    '101-010-1010',
    '10 Fictional Road',
    'Bellwood',
    'State',
    'B3C 1N9'
);
CALL add_patient(
    10004, -- patient id
    'Apple',
    'Jack',
    '1998-12-12',
    NULL,
    NULL,
    'Ponyville',
    'Equestria',
    NULL
);
CALL add_patient(
    10005, -- patient id
    'Mary',
    'Brown',
    '1977-03-03',
    '226-226-2626',
    '26 Twin Street',
    'Stoneville',
    'Imaginary State',
    NULL
);
CALL add_patient(
    10006, -- patient id
    'James',
    'Bond',
    '1962-01-01',
    NULL,
    '123 Cool Road',
    'City',
    'State',
    'A1A 2B2'
);
CALL add_patient(
    10007, -- patient id
    'John',
    'Smith',
    '1970-09-13',
    '519-991-1919',
    '1919 Serpent Rd',
    'Town',
    'State',
    'B1B 1A1'
);
CALL add_patient(
    10008, -- patient id
    'Octavius',
    'Roman',
    '1969-07-08',
    '111-222-3214',
    '12 Pool Blvd',
    'Swimming Region',
    'State',
    'N8N 8N8'
);

-- APPOINTMENTS 
CALL book_appointment(
    00001, -- appt id
    10001, -- patient id
    11011019, -- doctor id 
    320006, -- nurse id
    44567890, -- booked by staff id
    "2026-11-16 09:00:00", -- scheduled appt time
    "Annual Checkup"
);
CALL book_appointment(
    00002, -- appt id
    10003, -- patient id
    11011019, -- doctor id 
    320006, -- nurse id
    44567890, -- booked by staff id
    "2026-11-19 10:15:00", -- scheduled appt time
    "Scheduled X Ray"
);
CALL book_appointment(
    00003, -- appt id
    10004, -- patient id
    11011019, -- doctor id 
    320006, -- nurse id
    44567890, -- booked by staff id
    "2026-11-24 15:30:00", -- scheduled appt time
    "Scheduled Ultrasound"
);
CALL book_appointment(
    00004, -- appt id
    10001, -- patient id
    11011019, -- doctor id 
    320006, -- nurse id
    44567890, -- booked by staff id
    "2026-11-23 13:45:00", -- scheduled appt time
    "Annual Checkup"
);
CALL cancel_appointment(3);
-- WALK INS 
CALL book_prospective_appointment(
    00005, -- appt id
    "Barney", 
    "Dinosaur", 
    "1992-04-06",
    11011019, -- doctor id
    320006, -- nurse id
    44567890, -- booked by receptionist staff id 
    "2026-11-26 09:00:00", -- walk in appt time
    "Walk-in Consultation"
);
CALL book_prospective_appointment(
    00006, -- appt id
    "Johnny", 
    "Test", 
    "1993-10-31",
    11011019, -- doctor id
    320006, -- nurse id
    44567890, -- booked by receptionist staff id 
    "2026-11-26 11:00:00", -- walk in appt time
    "Walk-in Consultation"
);
-- CHECK IN 
CALL log_visit_check_in(
    100001,  -- visit id
    00001, -- appt id
    44567890 -- checked in by receptionist staff id 
);
CALL log_visit_check_in(
    100002,  -- visit id
    00002, -- appt id
    44567890 -- checked in by receptionist staff id 
);
-- CHECK OUT
CALL log_visit_check_out(100001);

-- REMOTE ACCESS 
CALL grant_remote_access(
    44440, -- access id 
    8855123, -- remote worker staff id 
    10001,  -- patient id 
    90909, -- granted by sys admin staff id
    SHA2('TempAccess111',256), 
    "2026-11-19 09:00:00"
);
CALL grant_remote_access(
    31879, -- access id 
    8855123, -- remote worker staff id 
    10003,  -- patient id 
    90909, -- granted by sys admin staff id
    SHA2('TempAccess222',256), 
    "2026-12-31 23:59:00"
);
CALL revoke_remote_access(44440);

-- Sample patient charts linked to checked-in appointments for the clinical views.
INSERT INTO Medical_Record(patient_id, visit_id, clinician_staff_id, visit_date, height_cm, weight_kg,
    vitals_summary, diagnosis, treatment, notes)
VALUES
    (10001, 100001, 11011019, '2026-11-16', 170.00, 68.50, 'BP 118/76; pulse 72 bpm; temperature 36.8 C',
        'Routine checkup', 'Annual health review', 'Patient reports no acute concerns.'),
    (10003, 100002, 11011019, '2026-11-19', 145.00, 42.00, 'BP 106/68; pulse 88 bpm; temperature 36.7 C',
        'Imaging follow-up', 'X-ray completed', 'Follow up with the referring clinician.');

-- Additional fictional longitudinal charts make every seeded patient usable in the medical-records GUI.
-- These older records are not tied to an appointment; the current checked-in visits above remain linked.
INSERT INTO Medical_Record(patient_id, visit_id, clinician_staff_id, visit_date, height_cm, weight_kg,
    vitals_summary, diagnosis, treatment, notes)
VALUES
    (10001, NULL, 11011019, '2026-08-12', 169.50, 67.80, 'BP 116/74; pulse 70 bpm; temperature 36.7 C',
        'Minor ankle sprain, resolved', 'Rest and ice; gradual return to activity', 'No ongoing symptoms reported at follow-up.'),
    (10002, NULL, 11011019, '2026-10-03', 162.00, 58.40, 'BP 112/72; pulse 76 bpm; temperature 36.6 C',
        'Seasonal allergic rhinitis', 'Allergen avoidance reviewed; supportive care discussed', 'Fictional sample chart entry for GUI demonstration.'),
    (10002, NULL, 11011019, '2026-11-10', 162.00, 58.20, 'BP 110/70; pulse 74 bpm; temperature 36.5 C',
        'Allergy follow-up', 'Symptoms improved; continue current care plan', 'Follow-up entry for demonstrating multiple records per patient.'),
    (10003, NULL, 11011019, '2026-09-18', 144.50, 41.50, 'BP 104/66; pulse 84 bpm; temperature 36.8 C',
        'Preventive health visit', 'Routine health guidance provided', 'Fictional sample chart entry; guardian present.'),
    (10004, NULL, 11011019, '2026-10-28', 168.00, 62.00, 'BP 114/72; pulse 72 bpm; temperature 36.7 C',
        'Routine wellness visit', 'Preventive care reviewed', 'Fictional sample chart entry for GUI demonstration.'),
    (10005, NULL, 11011019, '2026-10-06', 165.00, 70.20, 'BP 120/78; pulse 74 bpm; temperature 36.6 C',
        'Routine wellness visit', 'Preventive care reviewed', 'Fictional sample chart entry for GUI demonstration.'),
    (10006, NULL, 11011019, '2026-09-22', 180.00, 78.00, 'BP 122/80; pulse 68 bpm; temperature 36.5 C',
        'Minor shoulder strain, improving', 'Activity modification and gentle mobility exercises discussed', 'Fictional sample chart entry; follow-up as needed.'),
    (10007, NULL, 11011019, '2026-08-30', 175.00, 82.00, 'BP 118/76; pulse 72 bpm; temperature 36.7 C',
        'Preventive health visit', 'Routine health guidance provided', 'Fictional sample chart entry for GUI demonstration.'),
    (10008, NULL, 11011019, '2026-10-15', 172.00, 74.00, 'BP 116/74; pulse 70 bpm; temperature 36.6 C',
        'Routine wellness visit', 'Preventive care reviewed', 'Fictional sample chart entry for GUI demonstration.');

-- Sample invoices for the receptionist billing view.
INSERT INTO Invoice(patient_id, appointment_id, created_by_staff_id, invoice_date, description, amount, status, due_date, paid_date, paid_by_staff_id)
VALUES
    (10001, 1, 44567890, CURRENT_DATE, 'Annual checkup', 75.00, 'UNPAID', DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY), NULL, NULL),
    (10003, 2, 44567890, CURRENT_DATE, 'X-ray appointment', 125.00, 'PAID', CURRENT_DATE, CURRENT_DATE, 44567890);
