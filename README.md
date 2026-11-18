# Forceps Hospital Database System

## Medical records and billing

Clinical staff can search, add, and edit patient records. Records retain their clinician and can be linked to a visit. Receptionists can search, create, and edit unpaid invoices, optionally link an invoice to an appointment, and mark it paid while recording the staff member who handled the payment.

For a fresh database, run `sql/schema.sql`, followed by `sql/procedures_triggers.sql` and `sql/seed_data.sql`. The schema includes the `Medical_Record` and `Invoice` tables. For an existing database that already has Patient, Staff, Visit, and Appointment, run `sql/medical_billing.sql` to add these tables.

Configure the MySQL connection in `src/database/DatabaseConnection.java` for the local environment before starting the application.
