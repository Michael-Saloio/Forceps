# Forceps Hospital Database System

## Fresh database

Run these scripts once, in order, from MySQL Workbench or the MySQL command-line client:

1. `sql/schema.sql`
2. `sql/procedures_triggers.sql`
3. `sql/medical_billing_procedures.sql`
4. `sql/audit_log_migration.sql`
5. `sql/seed_data.sql`

The seed script creates demo staff, patients, assigned appointments, check-ins, time-stamped vital-sign and treatment examples, sample medical records for all eight demo patients, and sample invoices. Its temporary remote-access grants expire relative to the time the seed runs. During a checked-in visit, a clinician can save structured vital signs and catalog-priced treatment events. Checking the visit out automatically creates one unpaid invoice with a $10 visit fee plus the treatment charges recorded for that visit. The printable invoice lists each treatment with its timestamp and charged-cost snapshot. After a receptionist checks a patient in, the app offers to show and print that patient's Code 39 wristband barcode; the barcode contains only the patient ID. The receptionist can reprint it later from Register Patient / Barcode. The appointment screen writes a printable HTML invoice to `Documents/Forceps/Invoices` and can open it for printing. Do not rerun the seed script on a populated database.

### Demo logins

After running the fresh-database scripts, sign in with the **account ID** (not the staff ID) and password below:

| Role | Account ID | Password |
| --- | ---: | --- |
| Doctor | 12345 | `password` |
| Nurse | 11221 | `password` |
| System Administrator | 31313 | `@Temp1000` |
| Remote Worker | 85132 | `Nintendo3D$` |
| Receptionist | 8456 | `Fri3ndship!` |

## Existing database migration

If the database already has Staff, User_account, Patient, Appointment, Nurse, and Visit, apply `sql/appointments_staff_migration.sql` once. It enables generated IDs for new records, including visits, and adds optional nurse assignment for existing appointments; new bookings through the application require a nurse.

If Medical_Record does not exist yet, run `sql/medical_billing.sql`. If an older Medical_Record table lacks the vital columns, apply `sql/medical_record_vitals_migration.sql` once. Then apply `sql/care_billing_migration.sql`; it safely adds row-version checks and the timestamped vital-sign/treatment tables, even if those columns or tables already exist. Run `sql/procedures_triggers.sql` and `sql/medical_billing_procedures.sql` afterward to replace the stored procedures and triggers. These procedure scripts can be rerun safely; the data seed cannot.

Existing appointments remain unassigned until a receptionist uses **Assign / change nurse**. Existing appointment rows with visit history are cancelled rather than deleted when removed, preserving the foreign-key history.

Apply `sql/audit_log_migration.sql` once to existing databases before using the System Administrator **Audit trail** tab. The screen supports filtering by staff, patient, action, and date; audit events are intended to be append-only.

## Local connection settings

The app defaults to `jdbc:mysql://localhost:3306/forceps` and the MySQL user `root`. Configure `FORCEPS_DB_URL`, `FORCEPS_DB_USER`, and `FORCEPS_DB_PASSWORD` in the environment used to launch the Java app. If `FORCEPS_DB_PASSWORD` is not set, it defaults to an empty password. Passwords should stay out of source control.

## Patient barcodes

The app prints Code 39 labels containing the numeric patient ID. A keyboard-wedge scanner types that ID into the clinical chart field; the field also accepts start/stop `*` characters some scanners return. A different barcode payload needs a database mapping before it can identify a patient.
