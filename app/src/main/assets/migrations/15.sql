ALTER TABLE openaps_temp_basals ADD COLUMN integration TEXT;
ALTER TABLE openaps_temp_basals DELETE COLUMN ns_upload_id;
ALTER TABLE Treatments ADD COLUMN integration TEXT;
ALTER TABLE Treatments DELETE COLUMN ns_upload_id;