/*
 * Copyright (c) 2009-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* vehicle-0.00-1.00.sql */

CREATE SCHEMA vehicle;

CREATE TABLE vehicle.Colors
(
    Name VARCHAR(30) NOT NULL,
    Hex TEXT,

    CONSTRAINT PK_Colors PRIMARY KEY (Name)
);

CREATE TABLE vehicle.Manufacturers
(
    RowId SERIAL NOT NULL,
    Name VARCHAR(255) NOT NULL,

    CONSTRAINT PK_Manufacturers PRIMARY KEY (RowId)
);

CREATE TABLE vehicle.Models
(
    RowId SERIAL NOT NULL,
    ManufacturerId INT NOT NULL,
    Name VARCHAR(255) NOT NULL,

    CONSTRAINT PK_Models PRIMARY KEY (RowId),
    CONSTRAINT FK_Models_Manufacturers FOREIGN KEY (ManufacturerId) REFERENCES vehicle.Manufacturers(RowId)
);

CREATE TABLE vehicle.Vehicles
(
    RowId SERIAL NOT NULL,
    Container ENTITYID NOT NULL,
    CreatedBy USERID NOT NULL,
    Created TIMESTAMP NOT NULL,
    ModifiedBy USERID NOT NULL,
    Modified TIMESTAMP NOT NULL,

    ModelId INT NOT NULL,
    Color VARCHAR(30) NOT NULL,

    ModelYear INT NOT NULL,
    Milage INT NOT NULL,
    LastService TIMESTAMP NOT NULL,

    CONSTRAINT PK_Vehicles PRIMARY KEY (RowId),
    CONSTRAINT FK_Vehicles_Models FOREIGN KEY (ModelId) REFERENCES vehicle.Models(RowId),
    CONSTRAINT FK_Vehicles_Colors FOREIGN KEY (Color) REFERENCES vehicle.Colors(Name)
);

/* vehicle-11.30-12.10.sql */

-- add container constraint
ALTER TABLE vehicle.Vehicles
    ADD CONSTRAINT FK_Vehicles_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId);

/* vehicle-12.30-13.10.sql */

CREATE TABLE vehicle.emissiontest (
  rowid SERIAL,
  name varchar(100),
  container entityid,
  parentTest int,
  vehicleId int,
  result boolean,

  CONSTRAINT PK_emissiontest PRIMARY KEY (rowid),
  CONSTRAINT FK_emissiontest_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);

/* vehicle-13.10-13.20.sql */

CREATE TABLE vehicle.etl_source
(
  rowid SERIAL,
  container entityid,
  created TIMESTAMP,
  modified TIMESTAMP,

  id VARCHAR(9),
  name VARCHAR(100),

  CONSTRAINT PK_etlsource PRIMARY KEY (rowid),
  CONSTRAINT AK_etlsource UNIQUE (container,id),
  CONSTRAINT FK_etlsource_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);


CREATE TABLE vehicle.etl_target
(
  rowid SERIAL,
  container entityid,
  created TIMESTAMP,
  modified TIMESTAMP,

  id VARCHAR(9),
  name VARCHAR(100),

  CONSTRAINT PK_etltarget PRIMARY KEY (rowid),
  CONSTRAINT AK_etltarget UNIQUE (container,id),
  CONSTRAINT FK_etltarget_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);

DELETE FROM vehicle.etl_target;
ALTER TABLE vehicle.etl_target ADD COLUMN _txTransformRunId INT NOT NULL;

CREATE TABLE vehicle.etl_target2
(
  rowid SERIAL,
  container entityid,
  created TIMESTAMP,
  modified TIMESTAMP,

  id VARCHAR(9),
  name VARCHAR(100),
  _txTransformRunId INT NOT NULL,

  CONSTRAINT PK_etltarget2 PRIMARY KEY (rowid),
  CONSTRAINT AK_etltarget2 UNIQUE (container,id),
  CONSTRAINT FK_etltarget2_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);

DROP TABLE vehicle.etl_target2;

CREATE TABLE vehicle.etl_target2
(
  rowid INT NOT NULL,
  container entityid,
  created TIMESTAMP,
  modified TIMESTAMP,

  id VARCHAR(9),
  name VARCHAR(100),
  _txTransformRunId INT NOT NULL,

  CONSTRAINT PK_etltarget2 PRIMARY KEY (rowid),
  CONSTRAINT AK_etltarget2 UNIQUE (container,id),
  CONSTRAINT FK_etltarget2_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);

CREATE TABLE vehicle.Owner
(
  RowId SERIAL,
  container ENTITYID,
  created TIMESTAMP,
  createdby USERID,
  modified TIMESTAMP,
  modifiedby USERID,

  first_name VARCHAR(100),
  last_name VARCHAR(100),
  email VARCHAR(100),
  gender VARCHAR(10),
  race VARCHAR(100),
  city VARCHAR(100),
  state VARCHAR(100),
  zip VARCHAR(10),
  text VARCHAR(1000),
  birth_date TIMESTAMP,
  CONSTRAINT PK_owner PRIMARY KEY (container,rowid)
);

CREATE TABLE vehicle.OwnerBackup
(
  RowId SERIAL,
  container ENTITYID,
  created TIMESTAMP,
  createdby USERID,
  modified TIMESTAMP,
  modifiedby USERID,

  first_name VARCHAR(100),
  last_name VARCHAR(100),
  email VARCHAR(100),
  gender VARCHAR(10),
  race VARCHAR(100),
  city VARCHAR(100),
  state VARCHAR(100),
  zip VARCHAR(10),
  text VARCHAR(1000),
  birth_date TIMESTAMP,
  CONSTRAINT PK_ownerbackup PRIMARY KEY (container,rowid)
);

ALTER TABLE vehicle.OwnerBackup ADD COLUMN _txTransformRunId INT;

ALTER TABLE vehicle.etl_target DROP COLUMN _txTransformRunId;
ALTER TABLE vehicle.etl_target2 DROP COLUMN _txTransformRunId;
ALTER TABLE vehicle.OwnerBackup DROP COLUMN _txTransformRunId;

ALTER TABLE vehicle.etl_target ADD COLUMN diTransformRunId INT;
ALTER TABLE vehicle.etl_target2 ADD COLUMN diTransformRunId INT;
ALTER TABLE vehicle.OwnerBackup ADD COLUMN diTransformRunId INT;

/* vehicle-13.20-13.30.sql */

ALTER TABLE vehicle.etl_source ADD COLUMN TransformRun INT;

CREATE TABLE vehicle.transfer
(
    RowId Int NOT NULL,
    TransferStart TIMESTAMP NOT NULL,
    TransferComplete TIMESTAMP NULL,
    SchemaName VARCHAR(100),
    Description VARCHAR(1000) NULL,
    Log VARCHAR,
    status VARCHAR(10) NULL
);

/* vehicle-13.30-14.10.sql */

-- It is intentional that there are no procedures in this script corresponding to the SQL Server version.
-- Stored Proc ETL's are not yet supported in Postgres and the test is skipped.

ALTER TABLE vehicle.Transfer Add COLUMN container ENTITYID NULL;

ALTER TABLE vehicle.Transfer ADD CONSTRAINT FK_etltransfer_container FOREIGN KEY (container) REFERENCES core.Containers (EntityId);

-- It is intentional that there are no procedures in this script corresponding to the SQL Server version.
-- Stored Proc ETL's are not yet supported in Postgres and the test is skipped.

-- Function: vehicle.etltest(integer, integer, entityid, integer, integer, integer, character varying, character varying, integer, timestamp without time zone, timestamp without time zone, character varying, integer, integer, timestamp without time zone, timestamp without time zone)

-- DROP FUNCTION vehicle.etltest(integer, integer, entityid, integer, integer, integer, character varying, character varying, integer, timestamp without time zone, timestamp without time zone, character varying, integer, integer, timestamp without time zone, timestamp without time zone);

CREATE OR REPLACE FUNCTION vehicle.etltest
(IN transformrunid integer
, IN containerid entityid DEFAULT NULL::character varying
, INOUT rowsinserted integer DEFAULT 0
, INOUT rowsdeleted integer DEFAULT 0
, INOUT rowsmodified integer DEFAULT 0
, INOUT returnmsg character varying DEFAULT 'default message'::character varying
, IN debug character varying DEFAULT ''::character varying
, IN filterrunid integer DEFAULT NULL::integer
, INOUT filterstarttimestamp timestamp without time zone DEFAULT NULL::timestamp without time zone
, INOUT filterendtimestamp timestamp without time zone DEFAULT NULL::timestamp without time zone
, INOUT previousfilterrunid integer DEFAULT (-1)
, INOUT previousfilterstarttimestamp timestamp without time zone DEFAULT NULL::timestamp without time zone
, INOUT previousfilterendtimestamp timestamp without time zone DEFAULT NULL::timestamp without time zone
--, INOUT procVersion decimal DEFAULT 0
, IN testmode integer DEFAULT (-1)
, INOUT testinoutparam character varying DEFAULT ''::character varying
, INOUT runcount integer DEFAULT 1
, OUT return_status integer)
  RETURNS record AS
$BODY$

/*
	Test modes
	1	normal operation
	2	return code > 0
	3	raise error
	4	input/output parameter persistence
	5	override of persisted input/output parameter
	6	Run filter strategy, require filterRunId. Test persistence.
  7 Modified since filter strategy, no source, require filterStartTimeStamp & filterEndTimeStamp,
		populated from output of previous run
  8	Modified since filter strategy with source, require filterStartTimeStamp & filterEndTimeStamp
		populated from the filter strategy IncrementalStartTime & IncrementalEndTime

*/
BEGIN

IF runCount IS NULL
THEN
	runCount := 1;
ELSE
	runCount := runCount + 1;
END IF;

IF testMode = 1
THEN
	RAISE NOTICE '%', 'Test print statement logging';
	rowsInserted := 1;
	rowsDeleted := 2;
	rowsModified := 4;
	returnMsg := 'Test returnMsg logging';
	return_status := 0;
	RETURN;
END IF;

IF testMode = 2 THEN return_status := 1; RETURN; END IF;

IF testMode = 3
THEN
	returnMsg := 'Intentional SQL Exception From Inside Proc';
	RAISE EXCEPTION '%', returnMsg;
END IF;

IF testMode = 4 AND testInOutParam != 'after' AND runCount > 1
THEN
	returnMsg := 'Expected value "after" for testInOutParam on run count = ' || runCount || ', but was ' || testInOutParam;
	return_status := 1;
	RETURN;
END IF;

IF testMode = 5 AND testInOutParam != 'before' AND runCount > 1
THEN
	returnMsg := 'Expected value "before" for testInOutParam on run count = ' || runCount || ', but was ' || testInOutParam;
	return_status := 1;
	RETURN;
END IF;

IF testMode = 6
THEN
	IF filterRunId IS NULL
	THEN
		returnMsg := 'Required filterRunId value not supplied';
		return_status := 1;
		RETURN;
	END IF;
	IF runCount > 1 AND (previousFilterRunId IS NULL OR previousFilterRunId >= filterRunId)
	THEN
		returnMsg := 'Required filterRunId was not persisted from previous run.';
		return_status := 1;
		RETURN;
	END IF;
	previousFilterRunId := filterRunId;
END IF;

IF testMode = 7
THEN
	IF runCount > 1 AND (filterStartTimeStamp IS NULL AND filterEndTimeStamp IS NULL)
	THEN
		returnMsg := 'Required filterStartTimeStamp or filterEndTimeStamp were not persisted from previous run.';
		return_status := 1;
		RETURN;
	END IF;
	filterStartTimeStamp := localtimestamp;
	filterEndTimeStamp := localtimestamp;
END IF;

IF testMode = 8
THEN
	IF runCount > 1 AND ((previousFilterStartTimeStamp IS NULL AND previousFilterEndTimeStamp IS NULL)
							OR (filterStartTimeStamp IS NULL AND filterEndTimeStamp IS NULL))
	THEN
		returnMsg := 'Required filterStartTimeStamp or filterEndTimeStamp were not persisted from previous run.';
		return_status := 1;
		RETURN;
	END IF;
	previousFilterStartTimeStamp := coalesce(filterStartTimeStamp, localtimestamp);
	previousFilterEndTimeStamp := coalesce(filterEndTimeStamp, localtimestamp);
END IF;


-- set value for persistence tests
IF testInOutParam != ''
THEN
	testInOutParam := 'after';
END IF;

return_status := 0;
RETURN;

END;
$BODY$
  LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION vehicle.etlMissingTransformRunId()
RETURNS RECORD
AS
$BODY$
BEGIN
	SELECT 1;
END;
$BODY$ LANGUAGE plpgsql;

/* vehicle-15.00-15.01.sql */

CREATE OR REPLACE FUNCTION vehicle.etltestresultset(transformrunid integer, containerid entityid DEFAULT NULL::character varying, debug character varying DEFAULT ''::character varying, filterrunid integer DEFAULT NULL::integer, filterstarttimestamp timestamp without time zone DEFAULT NULL::timestamp without time zone, filterendtimestamp timestamp without time zone DEFAULT NULL::timestamp without time zone, previousfilterrunid integer DEFAULT (-1), previousfilterstarttimestamp timestamp without time zone DEFAULT NULL::timestamp without time zone, previousfilterendtimestamp timestamp without time zone DEFAULT NULL::timestamp without time zone, testmode integer DEFAULT (-1))
  RETURNS refcursor AS
$BODY$

DECLARE
      ref refcursor;                                                     -- Declare a cursor variable
    BEGIN
      OPEN ref FOR SELECT * FROM vehicle.etl_source where container = containerid;   -- Open a cursor
      RETURN ref;                                                       -- Return the cursor to the caller
    END;

$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;