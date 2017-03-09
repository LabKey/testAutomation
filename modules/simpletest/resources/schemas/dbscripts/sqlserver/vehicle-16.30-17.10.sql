/* vehicle-16.30-16.31.sql */

ALTER TABLE vehicle.Models
    ADD InitialReleaseYear INT;

/* vehicle-16.31-16.32.sql */

-- Drop tables / procedures for ETL testing from this schema; they've been moved to the etltest schema.
DROP TABLE vehicle.etl_source;
DROP TABLE vehicle.etl_target;
DROP TABLE vehicle.etl_target2;
DROP TABLE vehicle.transfer;
DROP TABLE vehicle.etl_180column_source;
DROP TABLE vehicle.etl_180column_target;
DROP TABLE vehicle.etl_delete;

DROP PROCEDURE vehicle.etlTest;
DROP PROCEDURE vehicle.etlTestResultSet;
-- This procedure wasn't being used
DROP PROCEDURE vehicle.etlMissingTransformRunId

-- These two tables seem to have been introduced for ETL testing, but were never used
DROP TABLE vehicle.owner;
DROP TABLE vehicle.ownerbackup;