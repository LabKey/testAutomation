ALTER TABLE vehicle.Colors ADD COLUMN CycleCol INT UNIQUE;
ALTER TABLE vehicle.Manufacturers ADD COLUMN CycleCol INT UNIQUE;

ALTER TABLE vehicle.Colors ADD CONSTRAINT FK_Manufacturers_CycleCol FOREIGN KEY (CycleCol) REFERENCES vehicle.Manufacturers (CycleCol);
ALTER TABLE vehicle.Manufacturers ADD CONSTRAINT FK_Colors_CycleCol FOREIGN KEY (CycleCol) REFERENCES vehicle.Colors (CycleCol);