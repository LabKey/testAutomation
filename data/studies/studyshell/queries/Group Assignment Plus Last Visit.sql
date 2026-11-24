SELECT "Group Assignment".ParticipantId,
"Group Assignment".SequenceNum,
"Group Assignment".GAdt,
"Group Assignment".GAgroup,
l.PastWeek24
FROM "Group Assignment"
LEFT JOIN "Last Spec Coll Visit" as l ON "Group Assignment".ParticipantId = l.ParticipantId
