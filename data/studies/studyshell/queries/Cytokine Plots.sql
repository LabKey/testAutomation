SELECT ParticipantVisit.ParticipantId,
ParticipantVisit.sequencenum AS VisitLabel,
ParticipantVisit.sequencenum AS SequenceNum,
ParticipantVisit."GA-1".GAdt AS EnrollmentDt,
 CASE 
WHEN(ParticipantVisit."SD-1".SDdt IS NOT NULL)THEN(ParticipantVisit."SD-1".SDdt)
WHEN(ParticipantVisit."STR-1".STRdt IS NOT NULL)THEN(ParticipantVisit."STR-1".STRdt)
WHEN(ParticipantVisit."HTR-1".HTRdt IS NOT NULL)THEN(ParticipantVisit."HTR-1".HTRdt)
WHEN(ParticipantVisit."HT-1".HTdt IS NOT NULL)THEN(ParticipantVisit."HT-1".HTdt)
ELSE(ParticipantVisit."SCL-1".SCLdt)
END AS SpecCollDt, 
 CASE
WHEN(Cytokine.AnalyteName IS NOT NULL)THEN(Cytokine.AnalyteName)
ELSE('NONE')
END AS AnalyteName,
Cytokine.ResultSign,
Cytokine.ResultValue,
Cytokine.Units,
Cytokine.KitMfr,
Cytokine.LowestConc,
 CASE
WHEN(ClinicalData.VirLdValue2 IS NOT NULL)THEN(ClinicalData.AltCollDtVirLd2)
ELSE(ClinicalData.AltCollDtVirLd)
END AS AltCollDtVirLd,
 CASE
WHEN(ClinicalData.VirLdValue2 IS NOT NULL)THEN(ClinicalData.VirLdModifier2)
ELSE(ClinicalData.VirLdModifier)
END AS VirLdModifier,
 CASE
WHEN(ClinicalData.VirLdValue2 IS NOT NULL)THEN(ClinicalData.VirLdValue2)
ELSE(ClinicalData.VirLdValue)
END AS VirLdValue,
ParticipantVisit."AL-1".AL1stdt AS ARTMed1Start,
ParticipantVisit."AL-1".AL1spdt AS ARTMed1Stop,
ParticipantVisit."AL-1".AL2stdt AS ARTMed2Start,
ParticipantVisit."AL-1".AL2spdt AS ARTMed2Stop,
ParticipantVisit."AL-1".AL3stdt AS ARTMed3Start,
ParticipantVisit."AL-1".AL3spdt AS ARTMed3Stop,
ParticipantVisit."AL-1".AL4stdt AS ARTMed4Start,
ParticipantVisit."AL-1".AL4spdt AS ARTMed4Stop,
ParticipantVisit."AL-1".AL5stdt AS ARTMed5Start,
ParticipantVisit."AL-1".AL5spdt AS ARTMed5Stop
FROM ParticipantVisit
LEFT JOIN ClinicalData ON ParticipantVisit.ParticipantId=ClinicalData.ParticipantId AND ParticipantVisit.SequenceNum=ClinicalData.SequenceNum
LEFT JOIN Cytokine ON ParticipantVisit.ParticipantId=Cytokine.ParticipantId AND ParticipantVisit.SequenceNum=Cytokine.SequenceNum
WHERE ParticipantVisit.sequencenum<10000.0 AND
ParticipantVisit.sequencenum<>2001.0 AND
ParticipantVisit.sequencenum<>9999.0 AND 
ParticipantVisit.ParticipantId in (SELECT Cytokine.ParticipantId FROM Cytokine)
