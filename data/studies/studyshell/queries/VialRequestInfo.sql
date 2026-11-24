SELECT 
VialRequest.Vial.ParticipantId as ptid,
VialRequest.Vial.Visit.SequenceNumMin as visitvalue,
VialRequest.Vial.Visit.Label as visitlabel,
VialRequest.Request.Destination.RowId as siteid,
VialRequest.Request.Destination.Label as lab,
COUNT(VialRequest.Vial) AS vialcount,
VialRequest.Vial.PrimaryType.Description AS primarytype,
VialRequest.Vial.DerivativeType.Description AS derivative,
VialRequest.Vial.AdditiveType.Description AS additive
FROM VialRequest 
WHERE VialRequest.Request.Status.SpecimensLocked
GROUP BY 
VialRequest.Vial.ParticipantId,
VialRequest.Vial.Visit.SequenceNumMin,
VialRequest.Vial.Visit.Label,
VialRequest.Request.Destination.RowId,
VialRequest.Request.Destination.Label,
VialRequest.Vial.PrimaryType.Description,
VialRequest.Vial.DerivativeType.Description,
VialRequest.Vial.AdditiveType.Description
