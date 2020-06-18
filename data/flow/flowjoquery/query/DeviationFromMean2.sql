SELECT
  Single.RowId,
  Single.FCSFile,
  Single.Run,
  Mean.NumberAveraged,
  CASE
    WHEN (ABS(Single."S/Lv:Freq_Of_Parent"-Mean."S/Lv:Freq_Of_Parent")/Mean.Statistic."S/Lv:Freq_Of_Parent">0.1) THEN('Live ')
    ELSE('')
  END|| CASE
    WHEN (ABS(Single."S/Lv/L/3+/4+:Freq_Of_Parent"-Mean."S/Lv/L/3+/4+:Freq_Of_Parent")/Mean.Statistic."S/Lv/L/3+/4+:Freq_Of_Parent">0.1) THEN('CD4 ')
    ELSE('')
  END|| CASE
    WHEN (ABS(Single."S/Lv/L/3+/8+:Freq_Of_Parent"-Mean."S/Lv/L/3+/8+:Freq_Of_Parent")/Mean."S/Lv/L/3+/8+:Freq_Of_Parent">0.1) THEN('CD8 ')
  ELSE('')
  END AS "Verdict",
  100*(Single."S/Lv:Freq_Of_Parent"-Mean."S/Lv:Freq_Of_Parent")/Mean."S/Lv:Freq_Of_Parent" AS "Live Deviation",
  100*(Single."S/Lv/L/3+/4+:Freq_Of_Parent"-Mean."S/Lv/L/3+/4+:Freq_Of_Parent")/Mean."S/Lv/L/3+/4+:Freq_Of_Parent" AS "CD4 Deviation",
  100*(Single."S/Lv/L/3+/8+:Freq_Of_Parent"-Mean."S/Lv/L/3+/8+:Freq_Of_Parent")/Mean."S/Lv/L/3+/8+:Freq_Of_Parent" AS "CD8 Deviation"
FROM
    (
    SELECT
      FCSAnalyses.RowId,
      FCSAnalyses.Run,
      FCSAnalyses.FCSFile.Keyword."Sample Order",
      FCSAnalyses.Statistic."S/Lv:Freq_Of_Parent",
      FCSAnalyses.Statistic."S/Lv/L/3+/4+:Freq_Of_Parent",
      FCSAnalyses.Statistic."S/Lv/L/3+/8+:Freq_Of_Parent",
    FROM FCSAnalyses
    WHERE FCSAnalyses.FCSFile.Keyword."Sample Order" NOT IN ('PBS', 'Comp')
    ) Single
INNER JOIN
    (
    SELECT
      FCSAnalyses.Run,
      COUNT(*) AS NumberAveraged,
      FCSAnalyses.FCSFile.Keyword."Sample Order",
      AVG(FCSAnalyses.Statistic."S/Lv:Freq_Of_Parent") AS "S/Lv:Freq_Of_Parent",
      AVG(FCSAnalyses.Statistic."S/Lv/L/3+/4+:Freq_Of_Parent") AS "S/Lv/L/3+/4+:Freq_Of_Parent",
      AVG(FCSAnalyses.Statistic."S/Lv/L/3+/8+:Freq_Of_Parent") AS "S/Lv/L/3+/8+:Freq_Of_Parent"
    FROM FCSAnalyses
    WHERE FCSAnalyses.FCSFile.Keyword."Sample Order" NOT IN ('PBS', 'Comp')
    GROUP BY FCSAnalyses.Run, FCSAnalyses.FCSFile.Keyword."Sample Order"
    ) Mean
ON Single.Run = Mean.Run AND Single."Sample Order" = Mean."Sample Order"