SELECT ClinicalData.ParticipantId,
ClinicalData.SequenceNum,
ClinicalData.specCollDt,
g.GAdt AS EnrollmentDt,
a.AL1stdt AS ArtStartDt,
t.TMtmdt AS TerminationDt,
f.stage AS FiebigAtEnrollment,
v.fellay_st,
v.fellay_sp,
ClinicalData.virLdModifier,
ClinicalData.virLdValue,
ClinicalData.virLdModifier2,
ClinicalData.virLdValue2
FROM ClinicalData
LEFT JOIN study."Group Assignment" AS g ON ClinicalData.ParticipantId=g.ParticipantId AND g.GAgroup='Group 1: Acute HIV-1'
LEFT JOIN study."Antiretroviral Log" AS a ON ClinicalData.ParticipantId=a.ParticipantId AND a.SequenceNum=3001
LEFT JOIN study.Termination AS t ON ClinicalData.ParticipantId=t.ParticipantId
LEFT JOIN study."Fiebig Stage" AS f ON ClinicalData.ParticipantId=f.ParticipantId AND f.SequenceNum=101
LEFT JOIN study."Viral Load Set Point" AS v ON ClinicalData.ParticipantId=v.ParticipantId
WHERE ClinicalData.specCollDt IS NOT NULL AND
g.GAdt IS NOT NULL AND 
ClinicalData.ParticipantId<>'700010607' AND 
ClinicalData.ParticipantId<>'700010700' AND 
ClinicalData.ParticipantId<>'704010195'
ORDER BY ClinicalData.ParticipantId, ClinicalData.specCollDt
