-- One-time migration for an existing Forceps database.
-- Fresh installations already receive these columns from sql/schema.sql.
USE forceps;

ALTER TABLE Medical_Record
    ADD COLUMN height_cm DECIMAL(5,2) NULL AFTER visit_date,
    ADD COLUMN weight_kg DECIMAL(6,2) NULL AFTER height_cm,
    ADD COLUMN vitals_summary VARCHAR(500) NULL AFTER weight_kg,
    ADD COLUMN remote_notes TEXT NULL AFTER notes,
    ADD COLUMN row_version INT NOT NULL DEFAULT 0 AFTER remote_notes,
    ADD CONSTRAINT chk_medical_record_height
        CHECK (height_cm IS NULL OR (height_cm > 0 AND height_cm <= 300)),
    ADD CONSTRAINT chk_medical_record_weight
        CHECK (weight_kg IS NULL OR (weight_kg > 0 AND weight_kg <= 1000));
