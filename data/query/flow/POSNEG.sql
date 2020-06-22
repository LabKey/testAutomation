SELECT q.s AS FCSAnalysis,
 MAX(q.s.Statistic."S/Lv/L/3+/4+/(IFNg+|IL2+):Freq_Of_Parent")- AVG(q.BG.Statistic."S/Lv/L/3+/4+/(IFNg+|IL2+):Freq_Of_Parent") AS "4+ POS BG ADJ",
 MAX(q.s.Statistic."S/Lv/L/3+/8+/(IFNg+|IL2+):Freq_Of_Parent")- AVG(q.BG.Statistic."S/Lv/L/3+/8+/(IFNg+|IL2+):Freq_Of_Parent") AS "8+ POS BG ADJ",
 CASE 
WHEN( MAX(q.s.Statistic."S/Lv/L/3+/4+/(IFNg+|IL2+):Freq_Of_Parent")- AVG(q.BG.Statistic."S/Lv/L/3+/4+/(IFNg+|IL2+):Freq_Of_Parent")>0.05 AND  
MAX(q.s.Statistic."S/Lv/L/3+/4+/(IFNg+|IL2+):Freq_Of_Parent")>4* AVG(q.BG.Statistic."S/Lv/L/3+/4+/(IFNg+|IL2+):Freq_Of_Parent") OR  
MAX(q.s.Statistic."S/Lv/L/3+/8+/(IFNg+|IL2+):Freq_Of_Parent")- AVG(q.BG.Statistic."S/Lv/L/3+/8+/(IFNg+|IL2+):Freq_Of_Parent")>0.05 AND  
MAX(q.s.Statistic."S/Lv/L/3+/8+/(IFNg+|IL2+):Freq_Of_Parent")>4* AVG(q.BG.Statistic."S/Lv/L/3+/8+/(IFNg+|IL2+):Freq_Of_Parent"))THEN('POSITIVE')
ELSE('NEGATIVE')
END AS "Verdict",
 CASE 
WHEN( MAX(q.s.Statistic."S/Lv/L/3+/4+/(IFNg+|IL2+):Freq_Of_Parent")- AVG(q.BG.Statistic."S/Lv/L/3+/4+/(IFNg+|IL2+):Freq_Of_Parent")>0.05 AND  MAX(q.s.Statistic."S/Lv/L/3+/4+/(IFNg+|IL2+):Freq_Of_Parent")>4* AVG(q.BG.Statistic."S/Lv/L/3+/4+/(IFNg+|IL2+):Freq_Of_Parent"))THEN('POSITIVE CD4')
ELSE('NEGATIVE CD4')
END AS "CD4 Verdict",
 CASE 
WHEN( MAX(q.s.Statistic."S/Lv/L/3+/8+/(IFNg+|IL2+):Freq_Of_Parent")- AVG(q.BG.Statistic."S/Lv/L/3+/8+/(IFNg+|IL2+):Freq_Of_Parent")>0.05 AND  MAX(q.s.Statistic."S/Lv/L/3+/8+/(IFNg+|IL2+):Freq_Of_Parent")>4* AVG(q.BG.Statistic."S/Lv/L/3+/8+/(IFNg+|IL2+):Freq_Of_Parent"))THEN('POSITIVE CD8')
ELSE('NEGATIVE CD8')
END AS "CD8 Verdict",
 MAX(q.s.Statistic."S/Lv/L/3+/4+/(IFNg+|IL2+):Freq_Of_Parent") AS "4+/(IFNg+|IL2+):%P",
 AVG(q.BG.Statistic."S/Lv/L/3+/4+/(IFNg+|IL2+):Freq_Of_Parent") AS "BG 4+/(IFNg+|IL2+):%P",
 MAX(q.s.Statistic."S/Lv/L/3+/8+/(IFNg+|IL2+):Freq_Of_Parent") AS "8+/(IFNg+|IL2+):%P",
 AVG(q.BG.Statistic."S/Lv/L/3+/8+/(IFNg+|IL2+):Freq_Of_Parent") AS "BG 8+/(IFNg+|IL2+):%P",
MIN(q.BG) AS BG1,
MAX(q.BG) AS BG2,
COUNT(q.BG) AS BGCount,

FROM 
(SELECT Sample.RowId AS s,
BG.RowId AS BG 
FROM 
(SELECT FCSAnalyses.RowId,
FCSAnalyses.Run,
FCSAnalyses.FCSFile.Keyword."Sample Order" AS "Sample Order"
FROM FCSAnalyses) AS Sample INNER JOIN
(SELECT FCSAnalyses.RowId AS RowId,
FCSAnalyses.Run AS Run,
FCSAnalyses.FCSFile.Keyword."Sample Order" AS "Sample Order"
FROM FCSAnalyses WHERE FCSAnalyses.FCSFile.Keyword.Stim IN ('Neg Cont', 'negctrl')) AS BG ON Sample.Run = BG.Run AND Sample."Sample Order" = BG."Sample Order"
) AS q
WHERE q.s.FCSFile.Keyword."Sample Order" = q.BG.FCSFile.Keyword."Sample Order"
GROUP BY q.s
