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

CREATE SCHEMA vehicle;
GO

CREATE TABLE vehicle.Colors
(
    Name NVARCHAR(30) NOT NULL,
    Hex TEXT,

    CONSTRAINT PK_Colors PRIMARY KEY (Name)
);

ALTER TABLE vehicle.Colors ADD TriggerScriptProperty NVARCHAR(100);

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

ALTER TABLE vehicle.Models ADD InitialReleaseYear INT;
ALTER TABLE vehicle.Models ADD ThumbnailImage NVARCHAR(60);
ALTER TABLE vehicle.Models ADD Image NVARCHAR(60);
ALTER TABLE vehicle.Models ADD PopupImage NVARCHAR(60);

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

-- add container constraint
ALTER TABLE vehicle.Vehicles ADD CONSTRAINT FK_Vehicles_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId);
ALTER TABLE vehicle.Vehicles ADD TriggerScriptContainer ENTITYID;

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

ALTER TABLE vehicle.FirstFKTable ADD CONSTRAINT FK_SecondFKTable_StartCycleCol FOREIGN KEY (StartCycleCol) REFERENCES vehicle.SecondFKTable (StartCycleCol);

CREATE TABLE vehicle.ThirdFKTable
(
    RowId INT IDENTITY(1,1),
    CycleCol INT NOT NULL UNIQUE,

    CONSTRAINT PK_ThirdFKTable PRIMARY KEY (RowId)
);

ALTER TABLE vehicle.SecondFKTable ADD CONSTRAINT FK_ThirdFKTable_CycleCol FOREIGN KEY (CycleCol) REFERENCES vehicle.ThirdFKTable (CycleCol);
ALTER TABLE vehicle.ThirdFKTable ADD CONSTRAINT FK_SecondFKTable_CycleCol FOREIGN KEY (CycleCol) REFERENCES vehicle.SecondFKTable (CycleCol);
