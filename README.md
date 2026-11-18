# Forceps Hospital Database System

## Medical records and billing

Clinical staff can search records by patient ID and add a visit date, diagnosis, treatment, and notes. Each record is linked to the logged-in clinician. Receptionists can search invoices, create an invoice for a patient, and mark an unpaid invoice as paid. Invoice creation and payment are linked to the logged-in staff member.

For a fresh database, run `sql/schema.sql`, followed by `sql/procedures_triggers.sql` and `sql/seed_data.sql`. The schema includes the `Medical_Record` and `Invoice` tables. For a database that already has the patient, staff, and appointment tables, run `sql/medical_billing.sql` to add the two tables.

Configure the MySQL connection in `src/database/DatabaseConnection.java` for the local environment before starting the application.
