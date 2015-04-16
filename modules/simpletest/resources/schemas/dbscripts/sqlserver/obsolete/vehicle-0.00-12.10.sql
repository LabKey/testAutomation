/*
 * Copyright (c) 2009-2015 LabKey Corporation
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
