/*
 * Copyright (c) 2009-2016 LabKey Corporation
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