-- Drop tables / functions for ETL testing from this schema; they've been moved to the etltest schema.
DROP TABLE vehicle.etl_source;
DROP TABLE vehicle.etl_target;
DROP TABLE vehicle.etl_target2;
DROP TABLE vehicle.transfer;
DROP TABLE vehicle.etl_180column_source;
DROP TABLE vehicle.etl_180column_target;
DROP TABLE vehicle.etl_delete;

DROP FUNCTION vehicle.etltest(integer, entityid, integer, integer, integer, character varying, character varying, integer, timestamp without time zone, timestamp without time zone, integer, timestamp without time zone, timestamp without time zone, integer, character varying, integer);
DROP FUNCTION vehicle.etltestresultset(integer, entityid, character varying, integer, timestamp without time zone, timestamp without time zone, integer, timestamp without time zone, timestamp without time zone, integer);
-- This function wasn't being used
DROP FUNCTION vehicle.etlmissingtransformrunid();

-- These two tables seem to have been introduced for ETL testing, but were never used
DROP TABLE vehicle.owner;
DROP TABLE vehicle.ownerbackup;
