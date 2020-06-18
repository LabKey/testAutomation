SELECT "Physical Exam".ParticipantID, ROUND(AVG("Physical Exam".Temp_C), 1) AS AverageTemp,
FROM "Physical Exam"
GROUP BY "Physical Exam".ParticipantID
