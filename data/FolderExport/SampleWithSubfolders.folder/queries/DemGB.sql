SELECT COUNT(Demographics.ParticipantId) AS "Count",
Demographics.Gender,
Demographics.Country,
FROM Demographics
GROUP BY Country, Gender
