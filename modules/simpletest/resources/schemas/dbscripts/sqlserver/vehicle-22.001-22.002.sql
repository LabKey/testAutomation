IF COL_LENGTH('vehicle.Vehicles', 'TriggerScriptContainer') IS NULL
BEGIN
ALTER TABLE vehicle.Vehicles ADD COLUMN IF NOT EXISTS TriggerScriptContainer ENTITYID;
END;