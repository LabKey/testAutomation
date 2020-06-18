SELECT "Physical Exam + AverageTemp".ParticipantId,
"Physical Exam + AverageTemp".SequenceNum,
"Physical Exam + AverageTemp".Date,
"Physical Exam + AverageTemp".Day,
"Physical Exam + AverageTemp".Weight_kg,
"Physical Exam + AverageTemp".Temp_C,
"Physical Exam + AverageTemp".SystolicBloodPressure,
"Physical Exam + AverageTemp".DiastolicBloodPressure,
"Physical Exam + AverageTemp".Pulse,
"Physical Exam + AverageTemp".Respirations,
"Physical Exam + AverageTemp".Signature,
"Physical Exam + AverageTemp".Pregnancy,
"Physical Exam + AverageTemp".Language,
"Physical Exam + AverageTemp".AverageTemp,
ROUND("Physical Exam + AverageTemp".Temp_C
-"Physical Exam + AverageTemp".AverageTemp,1) AS TempDelta
FROM "Physical Exam + AverageTemp"
