SELECT FCSAnalyses.FCSFile.Sample.Property.PROTOCOL AS PROTOCOL,
'FH' AS LABID,
FCSAnalyses.FCSFile.Run AS ASSAYID,
NULL AS SPECID,
FCSAnalyses.FCSFile.Sample.Property.PTID,
FCSAnalyses.FCSFile.Sample.Property.VISITNO,
NULL AS VISITDAY,
FCSAnalyses.FCSFile.Sample.Property.DRAWDT,
FCSAnalyses.FCSFile.Sample.Property.TESTDT,
'Y' AS ASSAYRUN,
'?' AS RELIABLE,
FCSAnalyses.FCSFile.Sample.Property.PLT_TEMPLATE,
FCSAnalyses.FCSFile.Keyword.Plate AS PLATE,
FCSAnalyses.FCSFile.Sample.Property.SAMP_ORD,
FCSAnalyses.FCSFile.Keyword."WELL ID" AS WELL_ID,
FCSAnalyses.Statistic."Count" AS COLLECTCT,
FCSAnalyses.Statistic."S:Count" AS SINGLETCT,
FCSAnalyses.Statistic."S/Lv:Count" AS LIVECT,
FCSAnalyses.Statistic."S/Lv/L:Count" AS LYMPHCT,
FCSAnalyses.Statistic."S/Lv/L/3+:Count" AS CD3CT,
Subsets.Property.TCELLSUB,
FCSAnalyses.Statistic(Subsets.Property.STAT_TCELLSUB) AS NSUB,
FCSAnalyses.FCSFile.Keyword.Stim AS ANTIGEN,
Subsets.Property.CYTOKINE,
FCSAnalyses.Statistic(Subsets.Property.STAT_CYTNUM) AS CYTNUM,
FCSAnalyses.FCSFile.Keyword.Replicate AS NREPL,
NULL AS PREFRZCT,
FCSAnalyses.FCSFile.Sample.Property.VIABL1,
FCSAnalyses.FCSFile.Sample.Property.RECOVR1,
FCSAnalyses.FCSFile.Sample.Property.VIABL2,
FCSAnalyses.FCSFile.Sample.Property.RECOVR2,
1 AS METHOD,
'8' AS COLORS,
'S' AS STAIN,
'?' AS REPLACE,
NULL AS MODDT,
 CASE 
WHEN(FCSAnalyses.Flag.Comment IS NULL)THEN('')
ELSE('(Analysis:)'||FCSAnalyses.Flag.Comment)
END|| CASE 
WHEN(FCSAnalyses.FCSFile.Flag.Comment IS NULL)THEN('')
ELSE('(File:)'||FCSAnalyses.FCSFile.Flag.Comment)
END|| CASE 
WHEN(FCSAnalyses.FCSFile.Run.Flag.Comment IS NULL)THEN('')
ELSE('(Run:)'||FCSAnalyses.FCSFile.Run.Flag.Comment)
END|| CASE 
WHEN(FCSAnalyses.FCSFile.Sample.Flag.Comment IS NULL)THEN('')
ELSE('(Sample:)'||FCSAnalyses.FCSFile.Sample.Flag.Comment)
END AS Comments
FROM FCSAnalyses
INNER JOIN Project.Samples.Subsets AS Subsets ON 1=1
WHERE FCSAnalyses.FCSFile.Keyword."Sample Order" NOT IN ('PBS','Comp')
