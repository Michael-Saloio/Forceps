# Forceps Hospital Database System

## Database setup

For a fresh local database, run these scripts in order using MySQL Workbench or the MySQL command-line client:

1. `sql/schema.sql`
2. `sql/procedures_triggers.sql`
3. `sql/seed_data.sql` (run once; it inserts demonstration accounts, patients, appointments, and visits)

For an existing Forceps database that already has the `Patient`, `Staff`, `Appointment`, and `Visit` tables, run `sql/medical_billing.sql` to add the medical-record and invoice tables. Do not rerun `schema.sql` over an existing database.

The desktop app defaults to `jdbc:mysql://localhost:3306/forceps` and the MySQL user `root`. Set `FORCEPS_DB_URL`, `FORCEPS_DB_USER`, and `FORCEPS_DB_PASSWORD` in the environment to use different local connection settings. The password is not stored in source control. For example:

```sh
export FORCEPS_DB_URL='jdbc:mysql://localhost:3306/forceps'
export FORCEPS_DB_USER='root'
export FORCEPS_DB_PASSWORD='your-local-mysql-password'
```

Start the app from the same environment so Java receives those variables. The MySQL Connector/J jar is in `lib/`.

## Medical records and billing

Doctors and nurses can search records by patient ID, create records, and edit diagnosis, treatment, and notes. A record can be linked to a visit belonging to that patient. Receptionists can search invoices, create invoices with optional appointment links, edit unpaid invoices, record payments, and print invoices. Payment updates store the staff member and date for the audit trail.

The medical-record patient field supports keyboard-wedge barcode scanners: configure the barcode label to contain the numeric `patient_id`; the scanner types that value into the field and submits it. No extra barcode column is needed for that format. A barcode containing a different value needs a database mapping before it can be looked up.
