/*
 * Copyright (c) 2020-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT "Physical Exam".ParticipantId,
"Physical Exam".SequenceNum,
"Physical Exam".Date,
"Physical Exam".Day,
"Physical Exam".Weight_kg,
"Physical Exam".Temp_C,
"Physical Exam".SystolicBloodPressure,
"Physical Exam".DiastolicBloodPressure,
"Physical Exam".Pulse,
"Physical Exam".Respirations,
"Physical Exam".Signature,
"Physical Exam".Pregnancy,
"Physical Exam".Language,
AverageTempPerParticipant.AverageTemp,
FROM "Physical Exam"
INNER JOIN AverageTempPerParticipant 
ON "Physical Exam".ParticipantID=AverageTempPerParticipant.ParticipantID
