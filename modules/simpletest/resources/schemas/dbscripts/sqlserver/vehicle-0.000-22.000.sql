/*
 * Copyright (c) 2019 LabKey Corporation
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
GO

CREATE TABLE vehicle.Colors
(
    Name NVARCHAR(30) NOT NULL,
    Hex TEXT,

    CONSTRAINT PK_Colors PRIMARY KEY (Name)
);

CREATE TABLE vehicle.Manufacturers
(
    RowId INT IDENTITY(1,1),
    Name NVARCHAR(255) NOT NULL,

    CONSTRAINT PK_Manufacturers PRIMARY KEY (RowId)
);

CREATE TABLE vehicle.Models
(
    RowId INT IDENTITY(1,1),
    ManufacturerId INT NOT NULL,
    Name NVARCHAR(255) NOT NULL,

    CONSTRAINT PK_Models PRIMARY KEY (RowId),
    CONSTRAINT FK_Models_Manufacturers FOREIGN KEY (ManufacturerId) REFERENCES vehicle.Manufacturers(RowId)
);

CREATE TABLE vehicle.Vehicles
(
    RowId INT IDENTITY(1,1) NOT NULL,
    Container ENTITYID NOT NULL,
    CreatedBy USERID NOT NULL,
    Created DATETIME NOT NULL,
    ModifiedBy USERID NOT NULL,
    Modified DATETIME NOT NULL,

    ModelId INT NOT NULL,
    Color NVARCHAR(30) NOT NULL,

    ModelYear INT NOT NULL,
    Milage INT NOT NULL,
    LastService DATETIME NOT NULL,

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
  rowid int identity(1,1),
  name varchar(100),
  container entityid,
  parentTest int,
  vehicleId int,
  result bit,

  CONSTRAINT PK_emissiontest PRIMARY KEY (rowid),
  CONSTRAINT FK_emissiontest_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);

/* vehicle-13.10-13.20.sql */

CREATE TABLE vehicle.etl_source(
  RowId INT IDENTITY(1,1),
  container entityid,
  created DATETIME,
  modified DATETIME,

  id VARCHAR(9),
  name VARCHAR(100),
  TransformRun INT,
  rowversion rowversion,

CONSTRAINT PK_etlsource PRIMARY KEY (rowid),
  CONSTRAINT AK_etlsource UNIQUE (container,id),
  CONSTRAINT FK_etlsource_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);


CREATE TABLE vehicle.etl_target(
  RowId INT IDENTITY(1,1),
  container entityid,
  created DATETIME,
  modified DATETIME,

  id VARCHAR(9),
  name VARCHAR(100),
  diTransformRunId INT,

  CONSTRAINT PK_etltarget PRIMARY KEY (rowid),
  CONSTRAINT AK_etltarget UNIQUE (container,id),
  CONSTRAINT FK_etltarget_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);

CREATE TABLE vehicle.etl_target2
(
  RowId INT NOT NULL,
  container entityid,
  created DATETIME,
  modified DATETIME,

  id VARCHAR(9),
  name VARCHAR(100),
  diTransformRunId INT,

  CONSTRAINT PK_etltarget2 PRIMARY KEY (rowid),
  CONSTRAINT AK_etltarget2 UNIQUE (container,id),
  CONSTRAINT FK_etltarget2_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);

CREATE TABLE vehicle.Owner
(
  RowId INT IDENTITY(1,1),
  container ENTITYID,
  created DATETIME,
  createdby USERID,
  modified DATETIME,
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
  birth_date DATETIME,
  CONSTRAINT PK_owner PRIMARY KEY (container,rowid)
);


CREATE TABLE vehicle.OwnerBackup
(
  RowId INT IDENTITY(1,1),
  container ENTITYID,
  created DATETIME,
  createdby USERID,
  modified DATETIME,
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
  birth_date DATETIME,
  diTransformRunId INT,

  CONSTRAINT PK_ownerbackup PRIMARY KEY (container,rowid)
);

/* vehicle-13.20-13.30.sql */

CREATE TABLE vehicle.Transfer
(
    RowId INT NOT NULL,
    TransferStart DATETIME NOT NULL,
	transferComplete DATETIME NULL,
	schemaName NVARCHAR(100) NOT NULL,
	description NVARCHAR(1000) NULL,
	log NTEXT NULL,
	status NVARCHAR(10) NULL,
	container dbo.ENTITYID NULL
);

/* vehicle-13.30-14.10.sql */

ALTER TABLE vehicle.Transfer  ADD CONSTRAINT FK_etltransfer_container FOREIGN KEY(container)
	REFERENCES core.Containers (EntityId)
ALTER TABLE vehicle.transfer ADD CONSTRAINT PK_transfer PRIMARY KEY (rowid);
GO


CREATE PROCEDURE vehicle.etlMissingTransformRunId
AS
BEGIN
	SELECT 1
END

GO

/* vehicle-15.00-15.01.sql */

CREATE PROCEDURE [vehicle].[etlTestResultSet]
 	@transformRunId int,
 	@containerId varchar(100) = NULL OUTPUT,
 	@rowsInserted int = 0 OUTPUT,
 	@rowsDeleted int = 0 OUTPUT,
 	@rowsModified int = 0 OUTPUT,
 	@returnMsg varchar(100) = 'default message' OUTPUT,
 	@debug varchar(1000) = '',
 	@filterRunId int = null,
 	@filterStartTimeStamp datetime = null OUTPUT,
 	@filterEndTimeStamp datetime = null OUTPUT,
 	@testMode int,
 	@testInOutParam varchar(10) = null OUTPUT,
 	@runCount int = 1 OUTPUT,
 	@previousFilterRunId int = null OUTPUT,
 	@previousFilterStartTimeStamp datetime = null OUTPUT,
 	@previousFilterEndTimeStamp datetime = null OUTPUT
 AS
 BEGIN

 IF @testMode = 9
 BEGIN
 	SELECT * FROM vehicle.etl_source WHERE container = @containerId
 END

 IF @testInOutParam IS NOT NULL SET @testInOutParam = 'after'

 RETURN 0

 END

 GO

/* vehicle-15.10-15.11.sql */

CREATE TABLE vehicle.etl_delete
(
  RowId INT IDENTITY(1,1),
  container entityid,
  created DATETIME,
  modified DATETIME,

  id VARCHAR(9),
  name VARCHAR(100),
  TransformRun INT,
  rowversion rowversion,
  CONSTRAINT PK_etldelete PRIMARY KEY (rowid),
  CONSTRAINT AK_etldelete UNIQUE (container,id),
  CONSTRAINT FK_etldelete_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);
GO

/* vehicle-15.20-15.21.sql */

-- =============================================
-- Author:		Tony Galuhn
-- Create date: 11/22/2013 / modified 1/28/2014
-- Description:	sp for ETL testing
-- =============================================
CREATE PROCEDURE vehicle.etlTest
	@transformRunId int,
	@containerId entityId = NULL OUTPUT,
	@rowsInserted int = 0 OUTPUT,
	@rowsDeleted int = 0 OUTPUT,
	@rowsModified int = 0 OUTPUT,
	@returnMsg varchar(100) = 'default message' OUTPUT,
	@debug varchar(1000) = '',
	@filterRunId int = null,
	@filterStartTimeStamp datetime = null,
	@filterEndTimeStamp datetime = null,
	@testMode int,
	@testInOutParam varchar(10) = null OUTPUT,
	@runCount int = 1 OUTPUT,
	@previousFilterRunId int = null OUTPUT,
	@previousFilterStartTimeStamp datetime = null OUTPUT,
	@previousFilterEndTimeStamp datetime = null OUTPUT
AS
BEGIN

/*
	Test modes
	1	normal operation
	2	return code > 0
	3	raise error
	4	input/output parameter persistence
	5	override of persisted input/output parameter
	6	Run filter strategy, require @filterRunId. Test persistence.
	7	Modified since filter strategy, require @filterStartTimeStamp & @filterEndTimeStamp. Test persistence.

*/

IF @testMode IS NULL
BEGIN
	SET @returnMsg = 'No testMode set'
	RETURN 1
END

IF @testMode = 1
BEGIN
	print 'Test print statement logging'
	SET @rowsInserted = 1
	SET @rowsDeleted = 2
	SET @rowsModified = 4
	SET @returnMsg = 'Test returnMsg logging'
	RETURN 0
END

IF @testMode = 2 RETURN 1

IF @testMode = 3
BEGIN
	SET @returnMsg = 'Intentional SQL Exception From Inside Proc'
	RAISERROR(@returnMsg, 11, 1)
END

IF @testMode = 4 AND @testInOutParam != 'after' AND @runCount > 1
BEGIN
	SET @returnMsg = 'Expected value "after" for @testInOutParam on run count = ' + convert(varchar, @runCount) + ', but was ' + @testInOutParam
	RETURN 1
END

IF @testMode = 5 AND @testInOutParam != 'before' AND @runCount > 1
BEGIN
	SET @returnMsg = 'Expected value "before" for @testInOutParam on run count = ' + convert(varchar, @runCount) + ', but was ' + @testInOutParam
	RETURN 1
END

IF @testMode = 6
BEGIN
	IF @filterRunId IS NULL
	BEGIN
		SET @returnMsg = 'Required @filterRunId value not supplied'
		RETURN 1
	END
	IF @runCount > 1 AND (@previousFilterRunId IS NULL OR @previousFilterRunId <= @filterRunId)
	BEGIN
		SET @returnMsg = 'Required @filterRunId was not persisted from previous run.'
		RETURN 1
	END
	SET @previousFilterRunId = @filterRunId
END

IF @testMode = 7
BEGIN
	IF @runCount > 1 AND (@previousFilterStartTimeStamp IS NULL OR @previousFilterEndTimeStamp IS NULL
							OR @previousFilterStartTimeStamp <= @filterStartTimeStamp OR @previousFilterEndTimeStamp <= @filterEndTimeStamp)
	BEGIN
		SET @returnMsg = 'Required @filterStartTimeStamp or @filterEndTimeStamp were not persisted from previous run.'
		RETURN 1
	END
	SET @previousFilterStartTimeStamp = @filterStartTimeStamp
	SET @previousFilterEndTimeStamp = @filterEndTimeStamp
END

-- set value for persistence tests
IF @testInOutParam IS NOT NULL SET @testInOutParam = 'after'

RETURN 0

END
GO

/* vehicle-16.20-16.30.sql */

-- For testing merge, need to have the PK on this table not be IDENTITY
DROP TABLE vehicle.etl_target;
CREATE TABLE vehicle.etl_target(
  RowId INT NOT NULL,
  container entityid,
  created DATETIME,
  modified DATETIME,
  id VARCHAR(9),
  name VARCHAR(100),
  diTransformRunId INT,

  CONSTRAINT PK_etltarget PRIMARY KEY (rowid),
  CONSTRAINT AK_etltarget UNIQUE (container,id),
  CONSTRAINT FK_etltarget_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);


CREATE TABLE vehicle.etl_180column_source(
  RowId int IDENTITY(1,1) NOT NULL,
  container dbo.ENTITYID NULL,
  created datetime NULL,
  modified datetime NULL,
  field5 INT NULL,
  field6 INT NULL,
  field7 INT NULL,
  field8 INT NULL,
  field9 INT NULL,
  field10 INT NULL,
  field11 INT NULL,
  field12 INT NULL,
  field13 INT NULL,
  field14 INT NULL,
  field15 INT NULL,
  field16 INT NULL,
  field17 INT NULL,
  field18 INT NULL,
  field19 INT NULL,
  field20 INT NULL,
  field21 INT NULL,
  field22 INT NULL,
  field23 INT NULL,
  field24 INT NULL,
  field25 INT NULL,
  field26 INT NULL,
  field27 INT NULL,
  field28 INT NULL,
  field29 INT NULL,
  field30 INT NULL,
  field31 INT NULL,
  field32 INT NULL,
  field33 INT NULL,
  field34 INT NULL,
  field35 INT NULL,
  field36 INT NULL,
  field37 INT NULL,
  field38 INT NULL,
  field39 INT NULL,
  field40 INT NULL,
  field41 INT NULL,
  field42 INT NULL,
  field43 INT NULL,
  field44 INT NULL,
  field45 INT NULL,
  field46 INT NULL,
  field47 INT NULL,
  field48 INT NULL,
  field49 INT NULL,
  field50 INT NULL,
  field51 INT NULL,
  field52 INT NULL,
  field53 INT NULL,
  field54 INT NULL,
  field55 INT NULL,
  field56 INT NULL,
  field57 INT NULL,
  field58 INT NULL,
  field59 INT NULL,
  field60 INT NULL,
  field61 INT NULL,
  field62 INT NULL,
  field63 INT NULL,
  field64 INT NULL,
  field65 INT NULL,
  field66 INT NULL,
  field67 INT NULL,
  field68 INT NULL,
  field69 INT NULL,
  field70 INT NULL,
  field71 INT NULL,
  field72 INT NULL,
  field73 INT NULL,
  field74 INT NULL,
  field75 INT NULL,
  field76 INT NULL,
  field77 INT NULL,
  field78 INT NULL,
  field79 INT NULL,
  field80 INT NULL,
  field81 INT NULL,
  field82 INT NULL,
  field83 INT NULL,
  field84 INT NULL,
  field85 INT NULL,
  field86 INT NULL,
  field87 INT NULL,
  field88 INT NULL,
  field89 INT NULL,
  field90 INT NULL,
  field91 INT NULL,
  field92 INT NULL,
  field93 INT NULL,
  field94 INT NULL,
  field95 INT NULL,
  field96 INT NULL,
  field97 INT NULL,
  field98 INT NULL,
  field99 INT NULL,
  field100 INT NULL,
  field101 INT NULL,
  field102 INT NULL,
  field103 INT NULL,
  field104 INT NULL,
  field105 INT NULL,
  field106 INT NULL,
  field107 INT NULL,
  field108 INT NULL,
  field109 INT NULL,
  field110 INT NULL,
  field111 INT NULL,
  field112 INT NULL,
  field113 INT NULL,
  field114 INT NULL,
  field115 INT NULL,
  field116 INT NULL,
  field117 INT NULL,
  field118 INT NULL,
  field119 INT NULL,
  field120 INT NULL,
  field121 INT NULL,
  field122 INT NULL,
  field123 INT NULL,
  field124 INT NULL,
  field125 INT NULL,
  field126 INT NULL,
  field127 INT NULL,
  field128 INT NULL,
  field129 INT NULL,
  field130 INT NULL,
  field131 INT NULL,
  field132 INT NULL,
  field133 INT NULL,
  field134 INT NULL,
  field135 INT NULL,
  field136 INT NULL,
  field137 INT NULL,
  field138 INT NULL,
  field139 INT NULL,
  field140 INT NULL,
  field141 INT NULL,
  field142 INT NULL,
  field143 INT NULL,
  field144 INT NULL,
  field145 INT NULL,
  field146 INT NULL,
  field147 INT NULL,
  field148 INT NULL,
  field149 INT NULL,
  field150 INT NULL,
  field151 INT NULL,
  field152 INT NULL,
  field153 INT NULL,
  field154 INT NULL,
  field155 INT NULL,
  field156 INT NULL,
  field157 INT NULL,
  field158 INT NULL,
  field159 INT NULL,
  field160 INT NULL,
  field161 INT NULL,
  field162 INT NULL,
  field163 INT NULL,
  field164 INT NULL,
  field165 INT NULL,
  field166 INT NULL,
  field167 INT NULL,
  field168 INT NULL,
  field169 INT NULL,
  field170 INT NULL,
  field171 INT NULL,
  field172 INT NULL,
  field173 INT NULL,
  field174 INT NULL,
  field175 INT NULL,
  field176 INT NULL,
  field177 INT NULL,
  field178 INT NULL,
  field179 INT NULL,
  field180 INT NULL,

  CONSTRAINT PK_etl_180column_source PRIMARY KEY (RowId),
  CONSTRAINT FK_etl_180column_source_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);

CREATE TABLE vehicle.etl_180column_target(
  RowId int NOT NULL,
  container dbo.ENTITYID NULL,
  created datetime NULL,
  modified datetime NULL,
  field5 INT NULL,
  field6 INT NULL,
  field7 INT NULL,
  field8 INT NULL,
  field9 INT NULL,
  field10 INT NULL,
  field11 INT NULL,
  field12 INT NULL,
  field13 INT NULL,
  field14 INT NULL,
  field15 INT NULL,
  field16 INT NULL,
  field17 INT NULL,
  field18 INT NULL,
  field19 INT NULL,
  field20 INT NULL,
  field21 INT NULL,
  field22 INT NULL,
  field23 INT NULL,
  field24 INT NULL,
  field25 INT NULL,
  field26 INT NULL,
  field27 INT NULL,
  field28 INT NULL,
  field29 INT NULL,
  field30 INT NULL,
  field31 INT NULL,
  field32 INT NULL,
  field33 INT NULL,
  field34 INT NULL,
  field35 INT NULL,
  field36 INT NULL,
  field37 INT NULL,
  field38 INT NULL,
  field39 INT NULL,
  field40 INT NULL,
  field41 INT NULL,
  field42 INT NULL,
  field43 INT NULL,
  field44 INT NULL,
  field45 INT NULL,
  field46 INT NULL,
  field47 INT NULL,
  field48 INT NULL,
  field49 INT NULL,
  field50 INT NULL,
  field51 INT NULL,
  field52 INT NULL,
  field53 INT NULL,
  field54 INT NULL,
  field55 INT NULL,
  field56 INT NULL,
  field57 INT NULL,
  field58 INT NULL,
  field59 INT NULL,
  field60 INT NULL,
  field61 INT NULL,
  field62 INT NULL,
  field63 INT NULL,
  field64 INT NULL,
  field65 INT NULL,
  field66 INT NULL,
  field67 INT NULL,
  field68 INT NULL,
  field69 INT NULL,
  field70 INT NULL,
  field71 INT NULL,
  field72 INT NULL,
  field73 INT NULL,
  field74 INT NULL,
  field75 INT NULL,
  field76 INT NULL,
  field77 INT NULL,
  field78 INT NULL,
  field79 INT NULL,
  field80 INT NULL,
  field81 INT NULL,
  field82 INT NULL,
  field83 INT NULL,
  field84 INT NULL,
  field85 INT NULL,
  field86 INT NULL,
  field87 INT NULL,
  field88 INT NULL,
  field89 INT NULL,
  field90 INT NULL,
  field91 INT NULL,
  field92 INT NULL,
  field93 INT NULL,
  field94 INT NULL,
  field95 INT NULL,
  field96 INT NULL,
  field97 INT NULL,
  field98 INT NULL,
  field99 INT NULL,
  field100 INT NULL,
  field101 INT NULL,
  field102 INT NULL,
  field103 INT NULL,
  field104 INT NULL,
  field105 INT NULL,
  field106 INT NULL,
  field107 INT NULL,
  field108 INT NULL,
  field109 INT NULL,
  field110 INT NULL,
  field111 INT NULL,
  field112 INT NULL,
  field113 INT NULL,
  field114 INT NULL,
  field115 INT NULL,
  field116 INT NULL,
  field117 INT NULL,
  field118 INT NULL,
  field119 INT NULL,
  field120 INT NULL,
  field121 INT NULL,
  field122 INT NULL,
  field123 INT NULL,
  field124 INT NULL,
  field125 INT NULL,
  field126 INT NULL,
  field127 INT NULL,
  field128 INT NULL,
  field129 INT NULL,
  field130 INT NULL,
  field131 INT NULL,
  field132 INT NULL,
  field133 INT NULL,
  field134 INT NULL,
  field135 INT NULL,
  field136 INT NULL,
  field137 INT NULL,
  field138 INT NULL,
  field139 INT NULL,
  field140 INT NULL,
  field141 INT NULL,
  field142 INT NULL,
  field143 INT NULL,
  field144 INT NULL,
  field145 INT NULL,
  field146 INT NULL,
  field147 INT NULL,
  field148 INT NULL,
  field149 INT NULL,
  field150 INT NULL,
  field151 INT NULL,
  field152 INT NULL,
  field153 INT NULL,
  field154 INT NULL,
  field155 INT NULL,
  field156 INT NULL,
  field157 INT NULL,
  field158 INT NULL,
  field159 INT NULL,
  field160 INT NULL,
  field161 INT NULL,
  field162 INT NULL,
  field163 INT NULL,
  field164 INT NULL,
  field165 INT NULL,
  field166 INT NULL,
  field167 INT NULL,
  field168 INT NULL,
  field169 INT NULL,
  field170 INT NULL,
  field171 INT NULL,
  field172 INT NULL,
  field173 INT NULL,
  field174 INT NULL,
  field175 INT NULL,
  field176 INT NULL,
  field177 INT NULL,
  field178 INT NULL,
  field179 INT NULL,
  field180 INT NULL,

  CONSTRAINT PK_etl_180column_target PRIMARY KEY (RowId),
  CONSTRAINT FK_etl_180column_target_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);

-- Undo change to this table from the 16.20-16.21 script. Make the merge test use etl_target2 instead
DROP TABLE vehicle.etl_target;
CREATE TABLE vehicle.etl_target(
  RowId INT IDENTITY(1,1),
  container entityid,
  created DATETIME,
  modified DATETIME,
  id VARCHAR(9),
  name VARCHAR(100),
  diTransformRunId INT,

  CONSTRAINT PK_etltarget PRIMARY KEY (rowid),
  CONSTRAINT AK_etltarget UNIQUE (container,id),
  CONSTRAINT FK_etltarget_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);

TRUNCATE TABLE vehicle.etl_target2;
ALTER TABLE vehicle.etl_target2 DROP CONSTRAINT PK_etltarget2;
ALTER TABLE vehicle.etl_target2 DROP CONSTRAINT AK_etltarget2;
ALTER TABLE vehicle.etl_target2 ALTER COLUMN container entityid NOT NULL;
GO
ALTER TABLE vehicle.etl_target2 ADD CONSTRAINT AK_etltarget2 UNIQUE (Container, id);
ALTER TABLE vehicle.etl_target2 ADD CONSTRAINT PK_etltarget2 PRIMARY KEY (RowId, container);

TRUNCATE TABLE vehicle.etl_180column_target;
ALTER TABLE vehicle.etl_180column_target DROP CONSTRAINT PK_etl_180column_target;
ALTER TABLE vehicle.etl_180column_target ALTER COLUMN container entityid NOT NULL;
GO
ALTER TABLE vehicle.etl_180column_target ADD CONSTRAINT PK_etl_180column_target PRIMARY KEY (RowId, container);

/* vehicle-16.30-17.10.sql */

ALTER TABLE vehicle.Models
    ADD InitialReleaseYear INT;

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

/* vehicle-17.10-17.20.sql */

ALTER TABLE vehicle.Colors ADD TriggerScriptProperty NVARCHAR(100);

/* vehicle-18.20-18.30.sql */

ALTER TABLE vehicle.Models ADD ThumbnailImage NVARCHAR(60);
ALTER TABLE vehicle.Models ADD Image NVARCHAR(60);
ALTER TABLE vehicle.Models ADD PopupImage NVARCHAR(60);

/* vehicle-19.20-19.30.sql */

CREATE TABLE vehicle.FirstFKTable
(
    RowId INT IDENTITY(1,1),
    StartCycleCol INT NOT NULL UNIQUE,
    CONSTRAINT PK_FirstFKTable PRIMARY KEY (RowId)
);

CREATE TABLE vehicle.SecondFKTable
(
    RowId INT IDENTITY(1,1),
    StartCycleCol INT NOT NULL UNIQUE,
    CycleCol INT NOT NULL UNIQUE,

    CONSTRAINT PK_SecondFKTable PRIMARY KEY (RowId)
);

CREATE TABLE vehicle.ThirdFKTable
(
    RowId INT IDENTITY(1,1),
    CycleCol INT NOT NULL UNIQUE,

    CONSTRAINT PK_ThirdFKTable PRIMARY KEY (RowId)
);

ALTER TABLE vehicle.FirstFKTable ADD CONSTRAINT FK_SecondFKTable_StartCycleCol FOREIGN KEY (StartCycleCol) REFERENCES vehicle.SecondFKTable (StartCycleCol);
ALTER TABLE vehicle.SecondFKTable ADD CONSTRAINT FK_ThirdFKTable_CycleCol FOREIGN KEY (CycleCol) REFERENCES vehicle.ThirdFKTable (CycleCol);
ALTER TABLE vehicle.ThirdFKTable ADD CONSTRAINT FK_SecondFKTable_CycleCol FOREIGN KEY (CycleCol) REFERENCES vehicle.SecondFKTable (CycleCol);

/* 21.xxx SQL scripts */

ALTER TABLE vehicle.Vehicles ADD TriggerScriptContainer ENTITYID;