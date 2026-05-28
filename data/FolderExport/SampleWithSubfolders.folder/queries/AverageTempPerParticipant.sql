/*
 * Copyright (c) 2020-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT "Physical Exam".ParticipantID, ROUND(AVG("Physical Exam".Temp_C), 1) AS AverageTemp,
FROM "Physical Exam"
GROUP BY "Physical Exam".ParticipantID
