SELECT SpecimenEvent.labid.Label AS Location,
SpecimenEvent.labid.LdmsLabCode,
SpecimenEvent.ProtocolNumber,
SpecimenEvent.ParticipantId AS ParticipantId,
SpecimenEvent.SpecimenNumber,
SpecimenEvent.DrawTimestamp,
SpecimenEvent.SalReceiptDate,
SpecimenEvent.LabReceiptDate,
SpecimenEvent.DerivativeType AS DerivativeType,
SpecimenEvent.AdditiveType as AdditiveType,
SpecimenEvent.ShipFlag,
SpecimenEvent.ShipDate,
SpecimenEvent.ShipBatchNumber,
count (SpecimenEvent.VialId)as NumberOfVials
FROM SpecimenEvent
WHERE SpecimenEvent.ShipFlag =0 AND
SpecimenEvent.LabId.LdmsLabCode<>15.0 AND
SpecimenEvent.LabReceiptDate is null AND
SpecimenEvent.ProtocolNumber not in ('903', '026','027','039','040','041', '044','045','048','052','054','056','057','059','203','803')
Group by SpecimenEvent.ShipDate,
SpecimenEvent.ShipBatchNumber,
SpecimenEvent.ParticipantId,
SpecimenEvent.SpecimenNumber,
SpecimenEvent.DrawTimestamp,
SpecimenEvent.SalReceiptDate,
SpecimenEvent.LabReceiptDate,
SpecimenEvent.DerivativeType,
SpecimenEvent.Additivetype,
SpecimenEvent.ShipFlag,
SpecimenEvent.labid.Label,
SpecimenEvent.labid.LdmsLabCode,
SpecimenEvent.ProtocolNumber,
SpecimenEvent.LabId.LdmsLabCode
