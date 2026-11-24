SELECT ParticipantVisit.ParticipantId,
ParticipantVisit.Visit.Label AS VisitLabel,
ParticipantVisit.sequencenum AS SequenceNum,
ParticipantVisit."GA-1".GAdt AS EnrollmentDt,
"EIA Denny".DrawDt,
f.fiebig AS ConfFiebig,
"VL Denny".CalcResultModifier AS VirLdModifier,
"VL Denny".CalcResultValue AS VirLdValue,
"EIA Denny".QualResultValue AS QualResultEIA,
"WB Denny".Score AS QualResultWB,
"WB Denny".gp160.Value AS gp160,
"WB Denny".gp120.Value AS gp120,
"WB Denny".p68.Value AS p68,
"WB Denny".p66.Value AS p66,
"WB Denny".p65.Value AS p65,
"WB Denny".p5551.Value AS "p55/51",
"WB Denny".p55.Value AS p55,
"WB Denny".p52.Value AS p52,
"WB Denny".p51.Value AS p51,
"WB Denny".gp41.Value AS gp41,
"WB Denny".p40.Value AS p40,
"WB Denny".p34.Value AS p34,
"WB Denny".p31.Value AS p31,
"WB Denny".p25.Value AS p25,
"WB Denny".p24.Value AS p24,
"WB Denny".p18.Value AS p18,
"WB Denny".p17.Value AS p17
FROM ParticipantVisit
LEFT JOIN "VL Denny" ON ParticipantVisit.ParticipantId="VL Denny".ParticipantId AND ParticipantVisit.SequenceNum="VL Denny".SequenceNum
LEFT JOIN "EIA Denny" ON ParticipantVisit.ParticipantId="EIA Denny".ParticipantId AND ParticipantVisit.SequenceNum="EIA Denny".SequenceNum
LEFT JOIN "WB Denny" ON ParticipantVisit.ParticipantId="WB Denny".ParticipantId AND ParticipantVisit.SequenceNum="WB Denny".SequenceNum
LEFT JOIN study."Fiebig Stage" AS f ON ParticipantVisit.ParticipantId=f.ParticipantId AND ParticipantVisit.sequencenum=f.SequenceNum
WHERE ParticipantVisit.sequencenum<10000.0 AND
ParticipantVisit.sequencenum<>2001.0 AND
ParticipantVisit.sequencenum<>3001.0 AND
ParticipantVisit.sequencenum<>9999.0 AND
("VL Denny".CalcResultValue IS NOT NULL OR 
"EIA Denny".QualResultValue IS NOT NULL OR 
"WB Denny".Score IS NOT NULL)
ORDER BY ParticipantVisit.ParticipantId,ParticipantVisit.sequencenum
