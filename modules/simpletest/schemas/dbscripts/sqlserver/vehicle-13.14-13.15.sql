/*
 * Copyright (c) 2013 LabKey Corporation
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


