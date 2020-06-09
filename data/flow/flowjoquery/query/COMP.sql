SELECT
  A.Name AS First,
  B.Name AS Second,
  A.RowId AS FirstRowId,
  B.RowId AS SecondRowId,
  S.Name AS Statistic,
  A.Statistic(S.Name) AS FirstValue,
  B.Statistic(S.Name) AS SecondValue,
FROM FCSAnalyses AS A
  INNER JOIN Folder.flow.FCSAnalyses AS B
    ON A.RowId <> B.RowId
    -- AND A.Name = B.Name
    AND A.FCSFile = B.FCSFile
  INNER JOIN Statistics AS S ON 1=1

