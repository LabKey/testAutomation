SELECT
PVInt_One.PandaId,
PVInt_One._key AS RENAMED_KEY,
-- NOTE: We specifically don't select SequenceNum so it will be pulled in auto-magically as a suggested column.
--PVInt_One.SequenceNum,
PVInt_One."PVInt_One Datum",
PVInt_One.DataSets
FROM PVInt_One
