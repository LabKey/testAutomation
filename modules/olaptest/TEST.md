### Test notes

**FactCubeNotEnabled** should not allow mdx queries (text or json) because it does not have the EnableMondrain annotation.

        <Annotation name="EnableMondrian">TRUE</Annotation>

**FactCubeContainer** also disallows mdx queries because we only handle container filters in the CountDistinct api.  Even
though only one cube in the schema has a container column, we don't parse MDX queries to know which cube is being used.
_Note: that this could be changed to be per cube in the JSON case._

**FactCube** this cube should allow mdx queries (both text and json) as well as CountDistinct the api.


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


