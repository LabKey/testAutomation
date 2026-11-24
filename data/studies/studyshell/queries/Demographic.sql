SELECT ParticipantVisit.ParticipantId,
ParticipantVisit.sequencenum AS SequenceNum,
 CASE 
WHEN(ParticipantVisit."IGE-1".IGEgroup IS NOT NULL)THEN(ParticipantVisit."IGE-1".IGEgroup)
ELSE(ParticipantVisit."GA-1".GAgroup)
END AS GroupAssigned,
ParticipantVisit."DM-1".DMage AS Age,
ParticipantVisit."DM-1".DMsex AS Gender,
 CASE 
WHEN(ParticipantVisit."DMU-1".DMUwhite='1')THEN('White')
WHEN(ParticipantVisit."DMU-1".DMUblack='1')THEN('Black/African American')
WHEN(ParticipantVisit."DMU-1".DMUasian='1')THEN('Asian')
WHEN(ParticipantVisit."DMU-1".DMUhawpi='1')THEN('Native Hawaiian/Pacific Islander')
WHEN(ParticipantVisit."DMU-1".DMUnatam='1')THEN('American Indian/Alaskan Native')
WHEN(ParticipantVisit."DMS-1".DMSwhite='1')THEN('White')
WHEN(ParticipantVisit."DMS-1".DMSblack='1')THEN('Black')
WHEN(ParticipantVisit."DMS-1".DMScolo='1')THEN('Coloured')
WHEN(ParticipantVisit."DMS-1".DMSind='1')THEN('Indian')
ELSE(ParticipantVisit."DMU-1".DMUracox)
END AS Race,
 CASE 
WHEN(ParticipantVisit."DMM-1".DMMchewa='1')THEN('Chewa')
WHEN(ParticipantVisit."DMM-1".DMMyao='1')THEN('Yao')
WHEN(ParticipantVisit."DMM-1".DMMtumbu='1')THEN('Tumbuka')
WHEN(ParticipantVisit."DMM-1".DMMlomwe='1')THEN('Lomwe')
WHEN(ParticipantVisit."DMM-1".DMMsena='1')THEN('Sena')
WHEN(ParticipantVisit."DMM-1".DMMtonga='1')THEN('Tonga')
WHEN(ParticipantVisit."DMM-1".DMMothx IS NOT NULL)THEN(ParticipantVisit."DMM-1".DMMothx)
WHEN(ParticipantVisit."DMT-1".DMTchagg='1')THEN('Chagga')
WHEN(ParticipantVisit."DMT-1".DMTpare='1')THEN('Pare')
WHEN(ParticipantVisit."DMT-1".DMTmasai='1')THEN('Masai')
WHEN(ParticipantVisit."DMT-1".DMTmuaru='1')THEN('Muarusha')
WHEN(ParticipantVisit."DMT-1".DMTmeru='1')THEN('Meru')
ELSE(ParticipantVisit."DMT-1".DMTothx)
END AS Tribe
FROM ParticipantVisit
WHERE ParticipantVisit."DM-1".DMsex IS NOT NULL
ORDER BY ParticipantVisit.ParticipantId
