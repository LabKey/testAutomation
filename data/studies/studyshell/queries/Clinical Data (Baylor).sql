SELECT ClinicalData.ParticipantId,
ClinicalData.VisitLabel,
ClinicalData.SequenceNum,
ClinicalData.GroupAssigned,
ClinicalData.Age,
ClinicalData.Gender,
ClinicalData.Race,
ClinicalData.Tribe,
CASE WHEN ClinicalData.VirLdModifier2 IS NOT NULL THEN ClinicalData.VirLdModifier2 
ELSE ClinicalData.VirLdModifier END AS VirLdModifier,
CASE WHEN ClinicalData.VirLdValue2 IS NOT NULL THEN ClinicalData.VirLdValue2 
ELSE ClinicalData.VirLdValue END AS VirLdValue,
ClinicalData.AbsCD4,
ClinicalData.ART
FROM ClinicalData
WHERE ClinicalData.SequenceNum<>3001.0 AND
ClinicalData.SequenceNum<>3002.0
ORDER BY ClinicalData.ParticipantId,ClinicalData.SequenceNum
