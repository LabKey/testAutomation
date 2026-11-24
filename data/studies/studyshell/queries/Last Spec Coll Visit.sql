SELECT "Specimen Collection".ParticipantId,
"Specimen Collection".SequenceNum,
"Specimen Collection".ParticipantVisit.Visit.Label,
"Specimen Collection".SCLdt AS LastSpecCollDate,
CASE WHEN mod("Specimen Collection".SequenceNum,100) >= 9 THEN 'Yes' ELSE NULL END AS PastWeek24
FROM "Specimen Collection"
LEFT JOIN (SELECT "Specimen Collection".ParticipantId,
           max("Specimen Collection".SCLdt) AS "LastCollectionDate"
           FROM "Specimen Collection"
           GROUP BY "Specimen Collection".ParticipantId) AS x
           ON x.ParticipantId = "Specimen Collection".ParticipantId AND x.LastCollectionDate = "Specimen Collection".SCLdt
WHERE x.LastCollectionDate IS NOT NULL
ORDER BY "Specimen Collection".SequenceNum
