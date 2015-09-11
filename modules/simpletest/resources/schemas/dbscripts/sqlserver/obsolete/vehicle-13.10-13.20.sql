/*
 * Copyright (c) 2013-2015 LabKey Corporation
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

/* vehicle-13.10-13.11.sql */

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

/* vehicle-13.11-13.12.sql */

DELETE FROM vehicle.etl_target;
ALTER TABLE vehicle.etl_target ADD _txTransformRunId INT NOT NULL;

/* vehicle-13.12-13.13.sql */

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

/* vehicle-13.13-13.14.sql */

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

/* vehicle-13.14-13.15.sql */

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

/* vehicle-13.15-13.16.sql */

ALTER TABLE vehicle.OwnerBackup ADD _txTransformRunId INT;

/* vehicle-13.16-13.17.sql */

ALTER TABLE vehicle.etl_target DROP COLUMN _txTransformRunId;
ALTER TABLE vehicle.etl_target2 DROP COLUMN _txTransformRunId;
ALTER TABLE vehicle.OwnerBackup DROP COLUMN _txTransformRunId;

ALTER TABLE vehicle.etl_target ADD diTransformRunId INT;
ALTER TABLE vehicle.etl_target2 ADD diTransformRunId INT;
ALTER TABLE vehicle.OwnerBackup ADD diTransformRunId INT;
