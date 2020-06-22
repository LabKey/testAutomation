SELECT Single.RowId,
MAX(Single.FCSFile) AS FCSFile,
COUNT(Mean.RowId) AS NumberAveraged,
CASE 
WHEN(ABS(Single.Statistic."S/Lv:Freq_Of_Parent"-AVG(Mean.Statistic."S/Lv:Freq_Of_Parent"))/AVG(Mean.Statistic."S/Lv:Freq_Of_Parent")>0.1)THEN('Live ')
ELSE('')
END|| CASE 
WHEN(ABS(Single.Statistic."S/Lv/L/3+/4+:Freq_Of_Parent"-AVG(Mean.Statistic."S/Lv/L/3+/4+:Freq_Of_Parent"))/AVG(Mean.Statistic."S/Lv/L/3+/4+:Freq_Of_Parent")>0.1)THEN('CD4 ')
ELSE('')
END|| CASE 
WHEN(ABS(Single.Statistic."S/Lv/L/3+/8+:Freq_Of_Parent"-AVG(Mean.Statistic."S/Lv/L/3+/8+:Freq_Of_Parent"))/AVG(Mean.Statistic."S/Lv/L/3+/8+:Freq_Of_Parent")>0.1)THEN('CD8 ')
ELSE('')
END AS "Verdict",
100*(Single.Statistic."S/Lv:Freq_Of_Parent"-AVG(Mean.Statistic."S/Lv:Freq_Of_Parent"))/AVG(Mean.Statistic."S/Lv:Freq_Of_Parent") AS "Live Deviation",
100*(Single.Statistic."S/Lv/L/3+/4+:Freq_Of_Parent"-AVG(Mean.Statistic."S/Lv/L/3+/4+:Freq_Of_Parent"))/AVG(Mean.Statistic."S/Lv/L/3+/4+:Freq_Of_Parent") AS "CD4 Deviation",
100*(Single.Statistic."S/Lv/L/3+/8+:Freq_Of_Parent"-AVG(Mean.Statistic."S/Lv/L/3+/8+:Freq_Of_Parent"))/AVG(Mean.Statistic."S/Lv/L/3+/8+:Freq_Of_Parent")AS "CD8 Deviation"
FROM FCSAnalyses AS Single
INNER JOIN FCSAnalyses AS Mean ON Single.Run = Mean.Run AND Single.FCSFile.Keyword."Sample Order" = Mean.FCSFile.Keyword."Sample Order"
WHERE Single.FCSFile.Keyword."Sample Order" NOT IN ('PBS', 'Comp')
GROUP BY Single.RowId