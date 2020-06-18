SELECT "Physical Exam".ParticipantId,
"Physical Exam".SequenceNum,
"Physical Exam".Date,
"Physical Exam".Day,
"Physical Exam".Weight_kg,
"Physical Exam".Temp_C AS Temp,
"Physical Exam".SystolicBloodPressure-"Physical Exam".DiastolicBloodPressure AS PulsePressure,
"Physical Exam".DiastolicBloodPressure,
"Physical Exam".Pulse,
"Physical Exam".Respirations,
"Physical Exam".Signature,
"Physical Exam".Pregnancy,
"Physical Exam".Language
FROM "Physical Exam"
WHERE "Physical Exam".ParticipantId.ParticipantId='249318596'
