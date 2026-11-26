SELECT Clinical.ParticipantId,
Clinical.sequencenum AS SequenceNum,
Clinical.sequencenum.Label AS Visit,
Clinical.VisitDate,
Clinical.Day,
Clinical.LRcd4 AS CD4,
Clinical.LRlym AS Lymphocytes,
Clinical.LRhgb AS Hemoglobin,
CASE WHEN Clinical.HTR2rap IS NULL THEN Clinical.STR2rap ELSE Clinical.HTR2rap END AS RapidTest2,
CASE WHEN Clinical.HTReia IS NULL THEN 
    CASE WHEN Clinical.STReia IS NULL THEN Clinical.HTeia1 ELSE Clinical.STReia END
ElSE Clinical.HTReia END AS StandardHIVAntibody,
CASE WHEN Clinical.HTRnatql IS NULL THEN Clinical.STRnatql ELSE Clinical.HTRnatql END AS ViralLoadQualitative,
CASE WHEN Clinical.HTRnatop IS NULL THEN Clinical.STRnatop ELSE Clinical.HTRnatop END AS ViralLoadOp,
CASE WHEN Clinical.HTRnatqn IS NULL THEN Clinical.STRnatqn ELSE Clinical.HTRnatqn END AS ViralLoad,
CASE WHEN Clinical.HTRrap IS NULL THEN Clinical.STRrap ELSE Clinical.HTRrap END AS RapidTest1,
CASE WHEN Clinical.HTRwb IS NULL THEN Clinical.STRwb ELSE Clinical.HTRwb END AS WesternBlot,
Clinical.FUal AS StartStopART,
FROM Clinical Order By Clinical.ParticipantId, Clinical.SequenceNum
