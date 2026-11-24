SELECT SpecimenSummary.Visit,
SpecimenSummary.PrimaryType,
SpecimenSummary.DerivativeType,
SpecimenSummary.Clinic,
 SUM(SpecimenSummary.TotalVolume) AS TotalVolume,
 SUM(SpecimenSummary.AvailableVolume) AS AvailableVolume,
 MIN(SpecimenSummary.VolumeUnits) AS VolumeUnits,
 SUM(SpecimenSummary.VialCount) AS VialCount,
 SUM(SpecimenSummary.LockedInRequestCount) AS LockedInRequestCount,
 SUM(SpecimenSummary.AtRepositoryCount) AS AtRepositoryCount,
 SUM(SpecimenSummary.AvailableCount) AS AvailableCount,
SpecimenSummary.Container
FROM SpecimenSummary
GROUP BY SpecimenSummary.Visit,SpecimenSummary.PrimaryType,
SpecimenSummary.DerivativeType,SpecimenSummary.Clinic,
SpecimenSummary.Container
