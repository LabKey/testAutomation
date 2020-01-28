##### example MDX query

>SELECT\
> [Measures].[RowCount] ON COLUMNS,\
> [Fact.Type].[Type].Members ON ROWS\
>FROM [Facts]

##### example MDX query (json format, experimental)

>{\
>"onCols" : {"level":"[Measures].[MeasuresLevel]"},\
>"onRows" : {"level":"[Fact.Type].[Type]"}\
>}

##### example CountDistinctQuery

>{\
>"countDistinctLevel" : "[Fact.Name].[Name]",\
>"onRows" : {"level":"[Fact.Type].[Type]"}\
>}


