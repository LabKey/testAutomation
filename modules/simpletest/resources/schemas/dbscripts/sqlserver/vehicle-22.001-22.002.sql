IF COL_LENGTH('vehicle.Vehicles', 'TriggerScriptContainer') IS NULL
BEGIN
ALTER TABLE vehicle.Vehicles ADD TriggerScriptContainer ENTITYID;
END;