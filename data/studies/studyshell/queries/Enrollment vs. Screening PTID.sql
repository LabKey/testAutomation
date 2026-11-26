SELECT "Initial Group Enrollment".ParticipantId,
"Initial Group Enrollment".IGEscrid,
"Initial Group Enrollment".IGEgroup
FROM "Initial Group Enrollment"
WHERE "Initial Group Enrollment".IGEscrid IS NOT NULL
AND "Initial Group Enrollment".ParticipantId NOT LIKE '700%'
AND "Initial Group Enrollment".ParticipantId NOT LIKE '701%'
