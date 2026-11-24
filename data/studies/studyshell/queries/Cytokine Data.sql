SELECT ParticipantVisit.ParticipantId,
ParticipantVisit.Visit.Label AS VisitLabel,
ParticipantVisit.sequencenum AS SequenceNum,
ParticipantVisit."GA-1".GAdt AS EnrollmentDt,
 CASE 
WHEN(ParticipantVisit."SD-1".SDdt IS NOT NULL)THEN(ParticipantVisit."SD-1".SDdt)
WHEN(ParticipantVisit."STR-1".STRdt IS NOT NULL)THEN(ParticipantVisit."STR-1".STRdt)
WHEN(ParticipantVisit."HTR-1".HTRdt IS NOT NULL)THEN(ParticipantVisit."HTR-1".HTRdt)
WHEN(ParticipantVisit."HT-1".HTdt IS NOT NULL)THEN(ParticipantVisit."HT-1".HTdt)
ELSE(ParticipantVisit."SCL-1".SCLdt)
END AS SpecCollDt,
Cytokine.AnalyteName,
Cytokine.ResultSign,
Cytokine.ResultValue,
Cytokine.Units,
Cytokine.KitMfr,
Cytokine.LowestConc
FROM ParticipantVisit
LEFT JOIN Cytokine ON ParticipantVisit.ParticipantId=Cytokine.ParticipantId AND ParticipantVisit.SequenceNum=Cytokine.SequenceNum
WHERE ParticipantVisit.sequencenum<10000.0 AND
ParticipantVisit.sequencenum<>2001.0 AND
ParticipantVisit.sequencenum<>3001.0 AND
ParticipantVisit.sequencenum<>9999.0 AND
Cytokine.AnalyteName IS NOT NULL
ORDER BY ParticipantVisit.ParticipantId,Cytokine.AnalyteName,ParticipantVisit.sequencenum
