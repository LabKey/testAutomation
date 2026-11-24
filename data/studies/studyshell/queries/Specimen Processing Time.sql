SELECT SpecimenEvent.DrawTimestamp,
CASE WHEN SpecimenEvent.ProcessingTime > convert('1970-01-01',SQL_DATE) AND SpecimenEvent.FrozenTime > convert('1970-01-01',SQL_DATE) THEN 
((hour(SpecimenEvent.FrozenTime)*60 + minute(SpecimenEvent.FrozenTime)) - (hour(SpecimenEvent.ProcessingTime)*60 + minute(SpecimenEvent.ProcessingTime))) 
ELSE NULL END AS NumMinutesDiff,
SpecimenEvent.ParticipantId,
SpecimenEvent.VisitValue as Visit,
SpecimenEvent.PrimaryType.Description as PrimaryType,
SpecimenEvent.DerivativeType.Description as DerivativeType,
SpecimenEvent.AdditiveType.Description as AdditiveType,
SpecimenEvent.LabId.LdmsLabCode as ProcessingLocation,
SpecimenEvent.ProcessedByInitials,
SpecimenEvent.FrozenTime,
SpecimenEvent.ProcessingTime
FROM SpecimenEvent
WHERE SpecimenEvent.PrimaryType.Description='Blood (Whole)' AND
SpecimenEvent.DerivativeType.Description='PBMC Cells, Viable' AND 
SpecimenEvent.DrawTimestamp>convert('2009-01-01',SQL_DATE)
GROUP BY SpecimenEvent.ParticipantId,
SpecimenEvent.VisitValue,
SpecimenEvent.PrimaryType.Description,
SpecimenEvent.DerivativeType.Description,
SpecimenEvent.AdditiveType.Description,
SpecimenEvent.LabId.LdmsLabCode,
SpecimenEvent.ProcessedByInitials,
SpecimenEvent.DrawTimestamp,
SpecimenEvent.FrozenTime,
SpecimenEvent.ProcessingTime
