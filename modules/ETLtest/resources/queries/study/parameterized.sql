PARAMETERS
(
myParam VARCHAR DEFAULT 'DefaultSubjectParam'
)
SELECT "ETL Source".ParticipantId,
  "ETL Source".date,
  id,
  CASE WHEN name != 'Subject 3' THEN name
  ELSE myParam END AS name
FROM "ETL Source"