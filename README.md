# Forceps Hospital Database System

## Fresh database

Run these scripts once, in order, from MySQL Workbench or the MySQL command-line client:

1. `sql/schema.sql`
2. `sql/procedures_triggers.sql`
3. `sql/medical_billing_procedures.sql`
4. `sql/seed_data.sql`

The seed script creates demo staff, patients, assigned appointments, check-ins, sample medical records, and sample invoices. Checking a visit out automatically creates one unpaid $10 `Visit completion fee` invoice for that appointment. Reception can find it in the Billing view and print it or save it as a PDF. Do not rerun the seed script on a populated database.

## Existing database migration

If the database already has Staff, User_account, Patient, Appointment, Nurse, and Visit, apply `sql/appointments_staff_migration.sql` once. It enables generated IDs and adds optional nurse assignment for existing appointments; new bookings through the application require a nurse.

If Medical_Record does not exist yet, run `sql/medical_billing.sql`. If it already exists, run `sql/medical_record_vitals_migration.sql` once instead. Then run `sql/procedures_triggers.sql` and `sql/medical_billing_procedures.sql` to replace the existing procedures with the updated versions. These two procedure scripts can be rerun safely; the data seed cannot.

Existing appointments remain unassigned until a receptionist uses **Assign / change nurse**. Existing appointment rows with visit history are cancelled rather than deleted when removed, preserving the foreign-key history.

## Local connection settings

The app defaults to `jdbc:mysql://localhost:3306/forceps` and the MySQL user `root`. Configure `FORCEPS_DB_URL`, `FORCEPS_DB_USER`, and `FORCEPS_DB_PASSWORD` in the environment used to launch the Java app. Passwords should stay out of source control.

## Patient barcodes

The app prints Code 39 labels containing the numeric patient ID. A keyboard-wedge scanner types that ID into the clinical chart field; the field also accepts start/stop `*` characters some scanners return. A different barcode payload needs a database mapping before it can identify a patient.
