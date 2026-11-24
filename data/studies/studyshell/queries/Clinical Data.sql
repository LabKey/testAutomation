SELECT ParticipantVisit.ParticipantId,
ParticipantVisit.Visit.Label AS VisitLabel,
ParticipantVisit.sequencenum AS SequenceNum,
 CASE 
WHEN(ParticipantVisit."IGE-1".IGEgroup IS NOT NULL)THEN(ParticipantVisit."IGE-1".IGEgroup)
ELSE(ParticipantVisit."GA-1".GAgroup)
END AS GroupAssigned,
ParticipantVisit."DM-1".DMage AS Age,
ParticipantVisit."DM-1".DMsex AS Gender,
d.Race,
d.Tribe,
ParticipantVisit."GA-1".GAdt AS EnrollmentDt,
 CASE 
WHEN(ssd.Date IS NOT NULL)THEN(ssd.Date)
WHEN(ParticipantVisit."SD-1".SDdt IS NOT NULL)THEN(ParticipantVisit."SD-1".SDdt)
WHEN(ParticipantVisit."STR-1".STRdt IS NOT NULL)THEN(ParticipantVisit."STR-1".STRdt)
WHEN(ParticipantVisit."HT-1".HTdt IS NOT NULL)THEN(ParticipantVisit."HT-1".HTdt)
WHEN(ParticipantVisit."HTR-1".HTRdt IS NOT NULL)THEN(ParticipantVisit."HTR-1".HTRdt)
ELSE(ParticipantVisit."SCL-1".SCLdt)
END AS SpecCollDt,
f.stage AS "Initial Fiebig Stage",
 CASE 
WHEN(ssd.VirLdValue IS NOT NULL)THEN(ssd.AltCollDtVirLd)
WHEN(ParticipantVisit."SD-1".SDnatqn1 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDnatdt2)
WHEN(ParticipantVisit."STR-1".STRnatqn IS NOT NULL)THEN(ParticipantVisit."STR-1".STRnatdt)
WHEN(ParticipantVisit."HT-1".HTnatqn1 IS NOT NULL)THEN(ParticipantVisit."HT-1".HTnatdt2)
ELSE(ParticipantVisit."HTR-1".HTRnatdt)
END AS AltCollDtVirLd,
 CASE 
WHEN(ssd.VirLdValue IS NOT NULL)THEN(ssd.VirLdModifier)
WHEN(ParticipantVisit."SD-1".SDnatqn1 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDnatop1)
WHEN(ParticipantVisit."STR-1".STRnatqn IS NOT NULL)THEN(ParticipantVisit."STR-1".STRnatop)
WHEN(ParticipantVisit."HT-1".HTnatqn1 IS NOT NULL)THEN(ParticipantVisit."HT-1".HTnatop1)
ELSE(ParticipantVisit."HTR-1".HTRnatop)
END AS VirLdModifier,
 CASE 
WHEN(ssd.VirLdValue IS NOT NULL)THEN(ssd.VirLdValue)
WHEN(ParticipantVisit."SD-1".SDnatqn1 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDnatqn1)
WHEN(ParticipantVisit."STR-1".STRnatqn IS NOT NULL)THEN(ParticipantVisit."STR-1".STRnatqn)
WHEN(ParticipantVisit."HT-1".HTnatqn1 IS NOT NULL)THEN(ParticipantVisit."HT-1".HTnatqn1)
ELSE(ParticipantVisit."HTR-1".HTRnatqn)
END AS VirLdValue,
 CASE 
WHEN(ssd.VirLdValue2 IS NOT NULL)THEN(ssd.AltCollDtVirLd2)
WHEN(ParticipantVisit."SD-1".SDnatqn2 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDnatdt3)
ELSE(ParticipantVisit."HT-1".HTnatdt3)
END AS AltCollDtVirLd2,
 CASE 
WHEN(ssd.VirLdValue2 IS NOT NULL)THEN(ssd.VirLdModifier2)
WHEN(ParticipantVisit."SD-1".SDnatqn2 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDnatop2)
ELSE(ParticipantVisit."HT-1".HTnatop2)
END AS VirLdModifier2,
 CASE 
WHEN(ssd.VirLdValue2 IS NOT NULL)THEN(ssd.VirLdValue2)
WHEN(ParticipantVisit."SD-1".SDnatqn2 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDnatqn2)
ELSE(ParticipantVisit."HT-1".HTnatqn2)
END AS VirLdValue2,
 CASE 
WHEN(ssd.VirLdValue IS NOT NULL)THEN(ssd.VLdxLab)
END AS ScrVrlLdSource,
 CASE 
WHEN(ParticipantVisit."STR-1".STRnatql IS NOT NULL)THEN(ParticipantVisit."STR-1".STRnatql)
WHEN(ParticipantVisit."SD-1".SDnatql IS NOT NULL)THEN(ParticipantVisit."SD-1".SDnatql)
WHEN(ParticipantVisit."HTR-1".HTRnatql IS NOT NULL)THEN(ParticipantVisit."HTR-1".HTRnatql)
ELSE(ParticipantVisit."HT-1".HTnatql)
END AS VirLdQual,
 CASE 
WHEN(ParticipantVisit."STR-1".STReia IS NOT NULL)THEN(ParticipantVisit."STR-1".STReiadt)
WHEN(ParticipantVisit."SD-1".SDeia1 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDeiadt1)
WHEN(ParticipantVisit."HTR-1".HTReia IS NOT NULL)THEN(ParticipantVisit."HTR-1".HTReiadt)
ELSE(ParticipantVisit."HT-1".HTeiadt1)
END AS AltCollDtStHIVAb1,
 CASE 
WHEN(ParticipantVisit."STR-1".STReia IS NOT NULL)THEN(ParticipantVisit."STR-1".STReia)
WHEN(ParticipantVisit."SD-1".SDeia1 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDeia1)
WHEN(ParticipantVisit."HTR-1".HTReia IS NOT NULL)THEN(ParticipantVisit."HTR-1".HTReia)
ELSE(ParticipantVisit."HT-1".HTeia1)
END AS StanHIVAb1,
 CASE 
WHEN(ParticipantVisit."SD-1".SDeia2 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDeiadt2)
ELSE(ParticipantVisit."HT-1".HTeiadt2)
END AS AltCollDtStHIVAb2,
 CASE 
WHEN(ParticipantVisit."SD-1".SDeia2 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDeia2)
ELSE(ParticipantVisit."HT-1".HTeia2)
END AS StanHIVAb2,
ParticipantVisit."LR-1".LRcd4dt AS AltCollDtAbsCD4,
ParticipantVisit."LR-1".LRcd4 AS AbsCD4,
ParticipantVisit."LR-1".LRlymdt AS AltCollDtLymphs,
ParticipantVisit."LR-1".LRlym AS Lymphs,
 CASE 
WHEN(ParticipantVisit."STR-1".STRhgb IS NOT NULL)THEN(ParticipantVisit."STR-1".STRhgbdt)
ELSE(ParticipantVisit."LR-1".LRhgbdt)
END AS AltCollDtHemo,
 CASE 
WHEN(ParticipantVisit."STR-1".STRhgb IS NOT NULL)THEN(ParticipantVisit."STR-1".STRhgb)
ELSE(ParticipantVisit."LR-1".LRhgb)
END AS Hemoglobin,
 CASE 
WHEN(ParticipantVisit."STR-1".STRwb IS NOT NULL)THEN(ParticipantVisit."STR-1".STRwbdt)
WHEN(ParticipantVisit."SD-1".SDwb1 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDwbdt1)
WHEN(ParticipantVisit."HTR-1".HTRwb IS NOT NULL)THEN(ParticipantVisit."HTR-1".HTRwbdt)
ELSE(ParticipantVisit."HT-1".HTwbdt1)
END AS AltCollDtWBTest1,
 CASE 
WHEN(ParticipantVisit."STR-1".STRwb IS NOT NULL)THEN(ParticipantVisit."STR-1".STRwb)
WHEN(ParticipantVisit."SD-1".SDwb1 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDwb1)
WHEN(ParticipantVisit."HTR-1".HTRwb IS NOT NULL)THEN(ParticipantVisit."HTR-1".HTRwb)
ELSE(ParticipantVisit."HT-1".HTwb1)
END AS WesternBlotTest1,
 CASE 
WHEN(ParticipantVisit."SD-1".SDwb2 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDwbdt2)
ELSE(ParticipantVisit."HT-1".HTwbdt2)
END AS AltCollDtWBTest2,
 CASE 
WHEN(ParticipantVisit."SD-1".SDwb2 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDwb2)
ELSE(ParticipantVisit."HT-1".HTwb2)
END AS WesternBlotTest2,
 CASE 
WHEN(ParticipantVisit."STR-1".STRrap IS NOT NULL)THEN(ParticipantVisit."STR-1".STRrapdt)
WHEN(ParticipantVisit."SD-1".SDrap1 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDrapdt1)
WHEN(ParticipantVisit."HTR-1".HTRrap IS NOT NULL)THEN(ParticipantVisit."HTR-1".HTRrapdt)
ELSE(ParticipantVisit."HT-1".HTrapdt1)
END AS AltCollDtRapid,
 CASE 
WHEN(ParticipantVisit."STR-1".STRrap IS NOT NULL)THEN(ParticipantVisit."STR-1".STRrap)
WHEN(ParticipantVisit."SD-1".SDrap1 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDrap1)
WHEN(ParticipantVisit."HTR-1".HTRrap IS NOT NULL)THEN(ParticipantVisit."HTR-1".HTRrap)
ELSE(ParticipantVisit."HT-1".HTrap1)
END AS RapidTest1,
 CASE 
WHEN(ParticipantVisit."STR-1".STR2rap IS NOT NULL)THEN(ParticipantVisit."STR-1".STR2rap)
WHEN(ParticipantVisit."SD-1".SDrap2 IS NOT NULL)THEN(ParticipantVisit."SD-1".SDrap2)
WHEN(ParticipantVisit."HTR-1".HTR2rap IS NOT NULL)THEN(ParticipantVisit."HTR-1".HTR2rap)
ELSE(ParticipantVisit."HT-1".HTrap2)
END AS RapidTest2,
 CASE 
WHEN(ParticipantVisit."STE-1".STEstis IS NOT NULL)THEN(ParticipantVisit."STE-1".STEdt)
ELSE(ParticipantVisit."STF-1".STFdt)
END AS FormCompDtSTIs,
 CASE 
WHEN(ParticipantVisit."STE-1".STEstis IS NOT NULL)THEN(ParticipantVisit."STE-1".STEstis)
ELSE(ParticipantVisit."STF-1".STFstis)
END AS AnySTIs,
 CASE 
WHEN(f.art IS NOT NULL)THEN(f.specdt)
ELSE(NULL)
END AS DtART,
f.art AS ART,
f.artdays AS ARTdays
FROM ParticipantVisit
LEFT JOIN study."Fiebig Stage" AS f ON ParticipantVisit.ParticipantId=f.ParticipantId AND ParticipantVisit.sequencenum=f.SequenceNum
LEFT JOIN study.Demographic AS d ON ParticipantVisit.ParticipantId=d.ParticipantId AND ParticipantVisit.sequencenum=d.SequenceNum
LEFT JOIN study."Supplemental Serostatus Data" AS ssd ON  ParticipantVisit.ParticipantId=ssd.ParticipantId AND ParticipantVisit.sequencenum=ssd.SequenceNum
WHERE ParticipantVisit.sequencenum<1000.0
AND ParticipantVisit.Visit.Label IS NOT NULL
ORDER BY ParticipantVisit.ParticipantId,ParticipantVisit.sequencenum
