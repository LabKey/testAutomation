SELECT FCSAnalyses.RowId,
FCSAnalyses.Run.Name||'-' ||FCSAnalyses.FCSFile.Keyword."Sample Order" AS Key,
FCSAnalyses.FCSFile.Keyword.Stim AS Stim,
CASE WHEN (FCSAnalyses.FCSFile.Keyword.Stim NOT IN ('SEB', 'sebctrl', 'CMV')) THEN
FCSAnalyses.Statistic."S/Lv/L/3+/4+:Count" END AS CD4_Count,
CASE WHEN (FCSAnalyses.FCSFile.Keyword.Stim NOT IN ('SEB', 'sebctrl', 'CMV')) THEN
FCSAnalyses.Statistic."S/Lv/L/3+/8+:Count" END AS CD8_Count,
FCSAnalyses.Statistic."S/Lv/L/3+/4+/(IFNg+|IL2+):Freq_Of_Parent" AS CD4_Resp,
FCSAnalyses.Statistic."S/Lv/L/3+/8+/(IFNg+|IL2+):Freq_Of_Parent" AS CD8_Resp
FROM FCSAnalyses
WHERE FCSAnalyses.FCSFile.Keyword.Stim NOT IN ('PBS','Comp')
