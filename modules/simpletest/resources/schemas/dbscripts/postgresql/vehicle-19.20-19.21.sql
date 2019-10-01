ALTER TABLE vehicle.Colors ADD COLUMN CycleCol INT UNIQUE;
ALTER TABLE vehicle.Colors ADD CONSTRAINT FK_Manufacturers_CycleCol FOREIGN KEY (CycleCol) REFERENCES vehicle.manufacturers (CycleCol);

ALTER TABLE vehicle.manufacturers ADD COLUMN CycleCol INT UNIQUE;
ALTER TABLE vehicle.manufacturers ADD CONSTRAINT FK_Colors_CycleCol FOREIGN KEY (CycleCol) REFERENCES vehicle.Colors (CycleCol);

-- CREATE TABLE vehicle.SelfReferencingPrimary
-- (
--     RowId INT NOT NULL,
--     refCol INT NOT NULL UNIQUE,
--
--     CONSTRAINT PK_selfReferencingPrimary PRIMARY KEY (RowId)
-- );
--
--
-- CREATE TABLE vehicle.SelfReferencingSecondary
-- (
--     RowId INT NOT NULL,
--     refCol INT NOT NULL UNIQUE,
--
--     CONSTRAINT PK_selfReferencingSecondary PRIMARY KEY (RowId),
--     CONSTRAINT FK_SelfReferencingPrimary_refCol FOREIGN KEY (refCol) REFERENCES vehicle.SelfReferencingPrimary (refCol)
-- );
--
-- ALTER TABLE vehicle.SelfReferencingPrimary ADD CONSTRAINT FK_SelfReferencingSecondary_refCol FOREIGN KEY (refCol) REFERENCES vehicle.SelfReferencingSecondary (refCol)

-- CREATE TABLE vehicle.SelfReferencing
-- (
--     RowId INT NOT NULL,
--     rowId2 INT NOT NULL UNIQUE,
--     refCol INT NOT NULL,
--
--     CONSTRAINT PK_selfReferencing PRIMARY KEY (RowId),
--     CONSTRAINT FK_SelfReferencing_refCol FOREIGN KEY (refCol) REFERENCES vehicle.SelfReferencing (RowId2)
-- );