select x.ParticipantId,
x.SequenceNum,
x.DrawDt,
x.SpecimenID,
x.PTID,
x.VISIT,
x.DATE,
x.PTIDdiff,
x.VISITdiff,
x.DATEdiff
from (SELECT "Viral Load".ParticipantId,
      "Viral Load".SequenceNum,
      "Viral Load".DrawDt,
      "Viral Load".SpecimenID,
      s.ParticipantVisit.ParticipantId AS PTID,
      s.ParticipantVisit.sequencenum AS VISIT,
      s.ParticipantVisit.VisitDate AS DATE,
      CASE WHEN "Viral Load".ParticipantId=s.ParticipantVisit.ParticipantId THEN NULL ELSE true END AS PTIDdiff,
      CASE WHEN "Viral Load".SequenceNum=s.ParticipantVisit.sequencenum THEN NULL ELSE true END AS VISITdiff,
      CASE WHEN dayofmonth("Viral Load".DrawDt)=dayofmonth(s.ParticipantVisit.VisitDate)
          AND month("Viral Load".DrawDt)=month(s.ParticipantVisit.VisitDate)
          AND year("Viral Load".DrawDt)=year(s.ParticipantVisit.VisitDate) THEN NULL 
      ELSE true END AS DATEdiff,
      FROM "Viral Load"
      LEFT JOIN SpecimenDetail AS s ON s.GlobalUniqueId="Viral Load".SpecimenID
      WHERE "Viral Load".SpecimenID IS NOT NULL) as x
where x.PTIDdiff IS NOT NULL OR 
x.VISITdiff IS NOT NULL OR 
x.DATEdiff IS NOT NULL
