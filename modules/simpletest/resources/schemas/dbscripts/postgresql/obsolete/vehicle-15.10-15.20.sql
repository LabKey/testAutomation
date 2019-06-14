/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
/* vehicle-15.10-15.11.sql */

CREATE TABLE vehicle.etl_delete
(
  RowId SERIAL,
  container entityid,
  created TIMESTAMP,
  modified TIMESTAMP,

  id VARCHAR(9),
  name VARCHAR(100),
  TransformRun INT,
  rowversion SERIAL,
  CONSTRAINT PK_etldelete PRIMARY KEY (rowid),
  CONSTRAINT AK_etldelete UNIQUE (container,id),
  CONSTRAINT FK_etldelete_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);

ALTER TABLE vehicle.etl_source ADD COLUMN rowversion SERIAL;

ALTER TABLE vehicle.transfer ADD CONSTRAINT PK_transfer PRIMARY KEY (rowid);