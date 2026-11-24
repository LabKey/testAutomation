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
from (SELECT "HIV-1 Western Blot".ParticipantId,
      "HIV-1 Western Blot".SequenceNum,
      "HIV-1 Western Blot".DrawDt,
      "HIV-1 Western Blot".SpecimenID,
      s.ParticipantVisit.ParticipantId AS PTID,
      s.ParticipantVisit.sequencenum AS VISIT,
      s.ParticipantVisit.VisitDate AS DATE,
      CASE WHEN "HIV-1 Western Blot".ParticipantId=s.ParticipantVisit.ParticipantId THEN NULL ELSE true END AS PTIDdiff,
      CASE WHEN "HIV-1 Western Blot".SequenceNum=s.ParticipantVisit.sequencenum THEN NULL ELSE true END AS VISITdiff,
      CASE WHEN dayofmonth("HIV-1 Western Blot".DrawDt)=dayofmonth(s.ParticipantVisit.VisitDate)
          AND month("HIV-1 Western Blot".DrawDt)=month(s.ParticipantVisit.VisitDate)
          AND year("HIV-1 Western Blot".DrawDt)=year(s.ParticipantVisit.VisitDate) THEN NULL 
      ELSE true END AS DATEdiff,
      FROM "HIV-1 Western Blot"
      LEFT JOIN SpecimenDetail AS s ON s.GlobalUniqueId="HIV-1 Western Blot".SpecimenID
      WHERE "HIV-1 Western Blot".SpecimenID IS NOT NULL) as x
where x.PTIDdiff IS NOT NULL OR 
x.VISITdiff IS NOT NULL OR 
x.DATEdiff IS NOT NULL
