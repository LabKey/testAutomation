SELECT CC.Name,
CC.Run,
 CASE 
WHEN(CC.statistic."comp:Freq_Of_Parent"<CCS.Property.MINPCT OR 100*CC.statistic."comp:Count" < 1000 * CC.statistic."comp:Freq_Of_Parent")THEN('FAIL')
ELSE('PASS')
END AS "Verdict",
CC.statistic."comp:Freq_Of_Parent",
case when (CC.statistic."comp:Freq_Of_Parent" = 0) then 999999999
else 
(100*CC.statistic."comp:Count"/CC.statistic."comp:Freq_Of_Parent") end AS "L Count",
CCS.Property.MINPCT
FROM CompensationControls AS CC
INNER JOIN Project.Samples.CompControls AS CCS ON CC.Name=CCS.Name