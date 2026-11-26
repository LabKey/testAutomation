SELECT DISTINCT year(SpecimenSummary.DrawTimestamp) AS Year
FROM SpecimenSummary
WHERE SpecimenSummary.DrawTimestamp > convert('2009-01-01',SQL_DATE)
