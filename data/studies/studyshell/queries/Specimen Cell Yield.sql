SELECT SpecimenEvent.DrawTimestamp,
round(sum(SpecimenEvent.Volume) / max(SpecimenEvent.PrimaryVolume) / 1000000,1) as CellYield,
SpecimenEvent.ParticipantId,
SpecimenEvent.VisitValue AS Visit,
sum(SpecimenEvent.Volume) AS TotalVolume,
max(SpecimenEvent.PrimaryVolume) AS PrimaryVolume,
SpecimenEvent.PrimaryType.Description as PrimaryType,
SpecimenEvent.DerivativeType.Description as DerivativeType,
SpecimenEvent.AdditiveType.Description as AdditiveType,
SpecimenEvent.LabId.LdmsLabCode as ProcessingLocation,
SpecimenEvent.ProcessedByInitials
FROM SpecimenEvent
WHERE SpecimenEvent.PrimaryType.Description='Blood (Whole)' AND
SpecimenEvent.DerivativeType.Description='PBMC Cells, Viable' AND
SpecimenEvent.VolumeUnits='CEL' AND 
SpecimenEvent.PrimaryVolumeUnits='ML' AND 
--SpecimenEvent.LabId = SpecimenEvent.VialId.ProcessingLocation AND 
SpecimenEvent.DrawTimestamp>convert('2009-01-01', SQL_DATE)
GROUP BY SpecimenEvent.DrawTimestamp,
SpecimenEvent.ParticipantId,
SpecimenEvent.VisitValue,
SpecimenEvent.PrimaryType.Description,
SpecimenEvent.DerivativeType.Description,
SpecimenEvent.AdditiveType.Description,
SpecimenEvent.LabId.LdmsLabCode,
SpecimenEvent.ProcessedByInitials
