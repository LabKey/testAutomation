ALTER TABLE vehicle.Colors ADD COLUMN CycleCol INT UNIQUE;
ALTER TABLE vehicle.Colors ADD CONSTRAINT FK_Manufacturers_CycleCol FOREIGN KEY (CycleCol) REFERENCES vehicle.manufacturers (CycleCol);

ALTER TABLE vehicle.manufacturers ADD COLUMN CycleCol INT UNIQUE;
ALTER TABLE vehicle.manufacturers ADD CONSTRAINT FK_Colors_CycleCol FOREIGN KEY (CycleCol) REFERENCES vehicle.Colors (CycleCol);