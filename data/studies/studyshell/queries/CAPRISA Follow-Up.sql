SELECT i.ParticipantId,
min(i.GAdt) AS "Enrollment Date",
l.LastSpecCollDate AS "Last Spec Coll Date",
TIMESTAMPDIFF('SQL_TSI_DAY',min(i.GAdt), l.LastSpecCollDate) AS "Date Difference (days)"
FROM "Group Assignment" AS i
LEFT JOIN "Last Spec Coll Visit" as l ON i.ParticipantId = l.ParticipantId
WHERE i.ParticipantId LIKE '705%'
GROUP BY i.ParticipantId, l.LastSpecCollDate
