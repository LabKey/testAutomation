SELECT "CD4 & VL Data".ParticipantId,
c.age AS Age,
c.gender AS Gender,
CASE WHEN c.race IS NULL THEN c.tribe ELSE c.race END AS RaceOrTribe,
CASE WHEN min("CD4 & VL Data".VirLdValue2) < min("CD4 & VL Data".VirLdValue) 
 THEN min("CD4 & VL Data".VirLdValue2) 
 ELSE min("CD4 & VL Data".VirLdValue) 
 END AS MinVL,
CASE WHEN max("CD4 & VL Data".VirLdValue2) > max("CD4 & VL Data".VirLdValue) 
 THEN max("CD4 & VL Data".VirLdValue2) 
 ELSE max("CD4 & VL Data".VirLdValue) 
 END AS MaxVL,
min("CD4 & VL Data".AbsCD4) as MinCD4,
max("CD4 & VL Data".AbsCD4) as MaxCD4
FROM "CD4 & VL Data"
LEFT JOIN "CD4 & VL Data" AS c ON "CD4 & VL Data".ParticipantId = c.ParticipantId AND c.age IS NOT NULL
GROUP BY "CD4 & VL Data".ParticipantId,c.age,c.gender,c.race,c.tribe
