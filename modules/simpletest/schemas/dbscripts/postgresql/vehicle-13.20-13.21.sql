/*
 * Copyright (c) 2009-2012 LabKey Corporation
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