SELECT SpecimenEvent.Labid.Label AS Location,
SpecimenEvent.Labid.LdmsLabCode,
SpecimenEvent.ProtocolNumber,
SpecimenEvent.ParticipantId AS ParticipantId,
SpecimenEvent.VisitDescription,
SpecimenEvent.VisitValue,
SpecimenEvent.SpecimenNumber,
SpecimenEvent.DrawTimestamp,
SpecimenEvent.SalReceiptDate,
SpecimenEvent.LabReceiptDate,
SpecimenEvent.DerivativeType AS DerivativeType,
SpecimenEvent.AdditiveType as AdditiveType,
count (SpecimenEvent.VialId)as NumberOfVials
FROM SpecimenEvent
WHERE SpecimenEvent.VisitDescription not in ('Pos','Rdw') AND
SpecimenEvent.VisitValue = 99.9 AND
SpecimenEvent.ProtocolNumber not in ('903', '026','027','039','040','041', '044','045','048','052','054','056','057','059','203','803')
Group by SpecimenEvent.ParticipantId,
SpecimenEvent.VisitDescription,
SpecimenEvent.VisitValue,
SpecimenEvent.SpecimenNumber,
SpecimenEvent.DrawTimestamp,
SpecimenEvent.SalReceiptDate,
SpecimenEvent.LabReceiptDate,
SpecimenEvent.DerivativeType,
SpecimenEvent.AdditiveType,
SpecimenEvent.Labid.Label,
SpecimenEvent.Labid.LdmsLabCode,
SpecimenEvent.ProtocolNumber
