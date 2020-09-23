SELECT
  C.First, C.Second,
  C.FirstRowId, C.SecondRowId,
  C.Statistic, C.FirstValue, C.SecondValue,
  C.FirstValue - C.SecondValue AS Difference,
  ABS(C.FirstValue - C.SecondValue) AS AbsDifference,
  CASE WHEN ((C.FirstValue + C.SecondValue) > 0)
  THEN
      (100.0 * ABS(C.FirstValue - C.SecondValue) / ((C.FirstValue + C.SecondValue) / 2))
  END AS PercentDifference
FROM COMP AS C
