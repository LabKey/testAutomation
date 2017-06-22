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

/* vehicle-0.00-13.20.sql */

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

  CONSTRAINT PK_etltarget PRIMARY KEY (rowid),
  CONSTRAINT AK_etltarget UNIQUE (container,id),
  CONSTRAINT FK_etltarget_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);

DELETE FROM vehicle.etl_target;
ALTER TABLE vehicle.etl_target ADD _txTransformRunId INT NOT NULL;

CREATE TABLE vehicle.etl_target2(
  RowId INT IDENTITY(1,1),
  container entityid,
  created DATETIME,
  modified DATETIME,

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
  RowId INT NOT NULL,
  container entityid,
  created DATETIME,
  modified DATETIME,

  id VARCHAR(9),
  name VARCHAR(100),
  _txTransformRunId INT NOT NULL,

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
  CONSTRAINT PK_ownerbackup PRIMARY KEY (container,rowid)
);

ALTER TABLE vehicle.OwnerBackup ADD _txTransformRunId INT;

ALTER TABLE vehicle.etl_target DROP COLUMN _txTransformRunId;
ALTER TABLE vehicle.etl_target2 DROP COLUMN _txTransformRunId;
ALTER TABLE vehicle.OwnerBackup DROP COLUMN _txTransformRunId;

ALTER TABLE vehicle.etl_target ADD diTransformRunId INT;
ALTER TABLE vehicle.etl_target2 ADD diTransformRunId INT;
ALTER TABLE vehicle.OwnerBackup ADD diTransformRunId INT;

/* vehicle-13.20-13.30.sql */

ALTER TABLE vehicle.etl_source ADD TransformRun INT;

CREATE TABLE vehicle.Transfer
(
    RowId INT NOT NULL,
    TransferStart DATETIME NOT NULL,
	transferComplete DATETIME NULL,
	schemaName NVARCHAR(100) NOT NULL,
	description NVARCHAR(1000) NULL,
	log NTEXT NULL,
	status NVARCHAR(10) NULL
);

/* vehicle-13.30-14.10.sql */

/* vehicle-13.30-13.31.sql */

ALTER TABLE vehicle.Transfer ADD container dbo.ENTITYID NULL
GO

ALTER TABLE vehicle.Transfer  ADD CONSTRAINT FK_etltransfer_container FOREIGN KEY(container)
REFERENCES core.Containers (EntityId)
GO

-- =============================================
-- Author:		Tony Galuhn
-- Create date: 11/22/2013
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


CREATE PROCEDURE vehicle.etlMissingTransformRunId
AS
BEGIN
	SELECT 1
END

GO

/* vehicle-13.31-13.32.sql */

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.ROUTINES WHERE routine_schema = 'vehicle' AND routine_name = 'etlTest')
BEGIN
  DROP PROCEDURE vehicle.etlTest
END
GO

-- =============================================
-- Author:		Tony Galuhn
-- Create date: 11/22/2013 / modified 1/28/2014
-- Description:	sp for ETL testing
-- =============================================
CREATE PROCEDURE vehicle.etlTest
	@transformRunId int,
	@containerId varchar(100) = NULL OUTPUT,--entityId = NULL OUTPUT,
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

IF @runCount IS NULL
BEGIN
	SET @runCount = 1
END
ELSE
BEGIN
	SET @runCount += 1
END

IF @testMode = 1
BEGIN
	print 'Test print statement logging'
	SET @rowsInserted = 1
	SET @rowsDeleted = 2
	SET @rowsModified = 4
	SET @returnMsg = 'Test returnMsg logging'
	SELECT @previousFilterEndTimeStamp = getdate()
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
	IF @runCount > 1 AND (@previousFilterRunId IS NULL OR @previousFilterRunId >= @filterRunId)
	BEGIN
		SET @returnMsg = 'Required @filterRunId was not persisted from previous run.'
		RETURN 1
	END
	SET @previousFilterRunId = @filterRunId
END

IF @testMode = 7
BEGIN
	IF @runCount > 1 AND (@filterStartTimeStamp IS NULL AND @filterEndTimeStamp IS NULL)
	BEGIN
		SET @returnMsg = 'Required @filterStartTimeStamp or @filterEndTimeStamp were not persisted from previous run.'
		RETURN 1
	END
	SET @filterStartTimeStamp = COALESCE(@filterStartTimeStamp, getDate())
	SET @filterEndTimeStamp = COALESCE(@filterEndTimeStamp, getDate())
END

IF @testMode = 8
BEGIN
	IF @runCount > 1 AND ((@previousFilterStartTimeStamp IS NULL AND @previousFilterEndTimeStamp IS NULL)
							OR (@filterStartTimeStamp IS NULL AND @filterEndTimeStamp IS NULL))
	BEGIN
		SET @returnMsg = 'Required @filterStartTimeStamp or @filterEndTimeStamp were not persisted from previous run.'
		RETURN 1
	END
	SET @previousFilterStartTimeStamp = COALESCE(@filterStartTimeStamp, getDate())
	SET @previousFilterEndTimeStamp = COALESCE(@filterEndTimeStamp, getDate())
END

-- set value for persistence tests
IF @testInOutParam IS NOT NULL SET @testInOutParam = 'after'

RETURN 0

END

GO