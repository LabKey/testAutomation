SELECT 
Participant.ParticipantId,
Participant.EnrollmentSiteId,
Participant.CurrentSiteId,
Participant.StartDate,
Participant.DataSet."GA-1"."seq101.01".GAgroup IS NOT NULL AS AcuteEnroll,
Participant.DataSet."GA-1".seq201.GAgroup IS NOT NULL AS NegativeEnroll,
Participant.DataSet."GA-1".seq301.GAgroup IS NOT NULL AS EstablishedEnroll
FROM Participant
