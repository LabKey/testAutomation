/*
 * Copyright (c) 2016-2017 LabKey Corporation
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

