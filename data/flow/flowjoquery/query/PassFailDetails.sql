SELECT B.RowId AS FCSAnalysis,
B.Stim,
B.CD4_Count,
B.CD8_Count,
B.CD4_Resp,
B.CD8_Resp,
B.Run,
B.SampleOrder,
B.Sample,
CASE WHEN B.CD4_Count < B.Cutoff THEN 'LO_CD4' END AS LO_CD4,
CASE WHEN B.CD8_Count < B.Cutoff THEN 'LO_CD8' END AS LO_CD8,
CASE WHEN (B.CD4_Count >= 10000 AND B.CD8_Count >= 10000) THEN B.negctrl
WHEN (B.negctrl IS NOT NULL) THEN 'excluded' END AS negctrl,
B.sebctrl,
CASE WHEN (B.sebctrl IS NOT NULL AND (B.CD4_Resp < 1.2 OR B.CD8_Resp < 1.2)) THEN 'LO_SEB' END AS LO_SEB,
CASE WHEN (B.negctrl IS NOT NULL AND B.CD4_Count >= 10000 AND B.CD8_Count >= 10000) THEN B.CD4_Resp END AS negctrl_CD4_Resp,
CASE WHEN (B.negctrl IS NOT NULL AND B.CD4_Count >= 10000 AND B.CD8_Count >= 10000) THEN B.CD8_Resp END AS negctrl_CD8_Resp,
CASE WHEN (B.sebctrl IS NOT NULL) THEN B.CD4_Resp END AS sebctrl_CD4_Resp,
CASE WHEN (B.sebctrl IS NOT NULL) THEN B.CD8_Resp END AS sebctrl_CD8_Resp,
B.Run.Name||'-' ||B.SampleOrder AS Key,
FROM
(
SELECT A.RowId,
A.Run,
A.FCSFile.Sample,
A.FCSFile.Keyword."Sample Order" AS SampleOrder,
A.FCSFile.Keyword.Stim AS Stim,
CASE 
    WHEN (A.FCSFile.Keyword.Stim IN ('SEB', 'sebctrl', 'CMV')) THEN 0
    WHEN (A.FCSFile.Keyword.Stim IN ('Env1', 'Env2', 'Env3', 'ENV-1-PTEG', 'ENV-2-PTEG', 'ENV-3-PTEG')) THEN 5000
    WHEN (A.FCSFile.Keyword.Stim NOT IN ('negctrl', 'Neg Cont')) THEN 10000 END AS Cutoff,
CASE WHEN (A.FCSFile.Keyword.Stim IN ('negctrl', 'Neg Cont')) THEN 'negctrl' END AS negctrl,
CASE WHEN A.FCSFile.Keyword.Stim IN ('SEB', 'sebctrl') THEN 'sebctrl' END AS sebctrl,
A.Statistic."S/Lv/L/3+/4+:Count" AS CD4_Count,
A.Statistic."S/Lv/L/3+/8+:Count" AS CD8_Count,
A.Statistic."S/Lv/L/3+/4+/(IFNg+|IL2+):Freq_Of_Parent" AS CD4_Resp,
A.Statistic."S/Lv/L/3+/8+/(IFNg+|IL2+):Freq_Of_Parent" AS CD8_Resp
FROM FCSAnalyses AS A
WHERE A.FCSFile.Keyword.Stim NOT IN ('PBS','Comp')
) AS B