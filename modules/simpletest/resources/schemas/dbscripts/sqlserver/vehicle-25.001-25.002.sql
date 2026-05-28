/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
DROP TABLE IF EXISTS vehicle.OwnedVehicles;
DROP TABLE IF EXISTS vehicle.Owners;

CREATE TABLE vehicle.Owners
(
    RowId BIGINT IDENTITY(2147483648,1) NOT NULL,
    Name VARCHAR(100) NOT NULL,

    CONSTRAINT PK_Owners PRIMARY KEY (RowId)
);

CREATE TABLE vehicle.OwnedVehicles
(
    RowId BIGINT IDENTITY(4294967296,1) NOT NULL,
    Owner BIGINT NOT NULL,
    Vehicle INT NOT NULL,

    CONSTRAINT PK_OwnedVehicles PRIMARY KEY (RowId)
);