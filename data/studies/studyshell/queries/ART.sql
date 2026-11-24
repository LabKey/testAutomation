SELECT Demographics.ParticipantId,
i.IGEgroup AS "Init. Group Enr. Group",
g.GAgroup AS "Group Assign. Group",
 CASE 
WHEN(a.AL1med IS NOT NULL)THEN('Yes')
ELSE(NULL)
END AS "On ART"
FROM Demographics
LEFT JOIN "Antiretroviral Log" AS a ON Demographics.ParticipantId=a.ParticipantId AND a.SequenceNum=3001.0
LEFT JOIN "Initial Group Enrollment" AS i ON i.ParticipantId=Demographics.ParticipantId
LEFT JOIN (SELECT "Group Assignment".ParticipantId,
           "Group Assignment".GAgroup,
           "Group Assignment".GAdt,
           x."Last Enr. Date"
           FROM "Group Assignment"
           LEFT JOIN (SELECT "Group Assignment".ParticipantId,
                      max("Group Assignment".GAdt) AS "Last Enr. Date"
                      FROM "Group Assignment"
                      GROUP BY "Group Assignment".ParticipantId) AS x 
                      ON x.ParticipantId="Group Assignment".ParticipantId AND "Group Assignment".GAdt=x."Last Enr. Date"
           WHERE x."Last Enr. Date" IS NOT NULL) AS g 
           ON Demographics.ParticipantId=g.ParticipantId
