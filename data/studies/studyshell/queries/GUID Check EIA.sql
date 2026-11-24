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
from (SELECT EIA.ParticipantId,
      EIA.SequenceNum,
      EIA.DrawDt,
      EIA.SpecimenID,
      s.ParticipantVisit.ParticipantId AS PTID,
      s.ParticipantVisit.sequencenum AS VISIT,
      s.ParticipantVisit.VisitDate AS DATE,
      CASE WHEN EIA.ParticipantId=s.ParticipantVisit.ParticipantId THEN NULL ELSE true END AS PTIDdiff,
      CASE WHEN EIA.SequenceNum=s.ParticipantVisit.sequencenum THEN NULL ELSE true END AS VISITdiff,
      CASE WHEN dayofmonth(EIA.DrawDt)=dayofmonth(s.ParticipantVisit.VisitDate)
          AND month(EIA.DrawDt)=month(s.ParticipantVisit.VisitDate)
          AND year(EIA.DrawDt)=year(s.ParticipantVisit.VisitDate) THEN NULL 
      ELSE true END AS DATEdiff,
      FROM EIA
      LEFT JOIN SpecimenDetail AS s ON s.GlobalUniqueId=EIA.SpecimenID
      WHERE EIA.SpecimenID IS NOT NULL) as x
where x.PTIDdiff IS NOT NULL OR 
x.VISITdiff IS NOT NULL OR 
x.DATEdiff IS NOT NULL
