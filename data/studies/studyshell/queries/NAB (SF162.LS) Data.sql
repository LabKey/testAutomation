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
NAB.CurveIC50 AS "NAb SF162.LS CurveIC50"
FROM ParticipantVisit
LEFT JOIN NAB ON ParticipantVisit.ParticipantId=NAB.ParticipantId AND ParticipantVisit.SequenceNum=NAB.SequenceNum AND (NAB.VirusName LIKE '%SF162%')
WHERE ParticipantVisit.sequencenum<10000.0 AND
ParticipantVisit.sequencenum<>2001.0 AND
ParticipantVisit.sequencenum<>3001.0 AND
ParticipantVisit.sequencenum<>9999.0 AND
NAB.CurveIC50 IS NOT NULL
ORDER BY ParticipantVisit.ParticipantId,ParticipantVisit.sequencenum
