SELECT SpecimenEvent.LabId.LdmsLabCode,
SpecimenEvent.Labid.Label AS Location,
SpecimenEvent.ProtocolNumber,
SpecimenEvent.ParticipantId AS Ptid,
SpecimenEvent.VisitValue,
SpecimenEvent.SpecimenNumber,
SpecimenEvent.DrawTimestamp,
SpecimenEvent.SalReceiptDate,
SpecimenEvent.DerivativeType AS DerivativeType,
SpecimenEvent.AdditiveType as AdditiveType,
SpecimenEvent.Comments,
count (SpecimenEvent.VialId)as NumberOfVials
FROM SpecimenEvent
WHERE SpecimenEvent.LabId.LdmsLabCode<>15.0 AND 
SpecimenEvent.DrawTimestamp < SpecimenEvent.SalReceiptDate AND
SpecimenEvent.ProtocolNumber not in ('903', '026','027','039','040','041', '044','045','048','052','054','056','057','059','203','803')
Group by SpecimenEvent.LabId.LdmsLabCode,
SpecimenEvent.Labid.Label,
SpecimenEvent.ProtocolNumber,
SpecimenEvent.ParticipantId,
SpecimenEvent.VisitValue,
SpecimenEvent.SpecimenNumber,
SpecimenEvent.DrawTimestamp,
SpecimenEvent.SalReceiptDate,
SpecimenEvent.DerivativeType,
SpecimenEvent.AdditiveType,
SpecimenEvent.Comments
ORDER BY SpecimenEvent.LabId.LdmsLabCode,SpecimenEvent.ProtocolNumber
