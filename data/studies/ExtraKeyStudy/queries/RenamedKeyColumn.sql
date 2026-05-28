/*
 * Copyright (c) 2020-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
PVInt_One.PandaId,
PVInt_One._key AS RENAMED_KEY,
-- NOTE: We specifically don't select SequenceNum so it will be pulled in auto-magically as a suggested column.
--PVInt_One.SequenceNum,
PVInt_One."PVInt_One Datum",
PVInt_One.DataSets
FROM PVInt_One
