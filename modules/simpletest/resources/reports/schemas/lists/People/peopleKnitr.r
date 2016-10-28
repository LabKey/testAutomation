#  Copyright (c) 2015-2016 LabKey Corporation
#
#  Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
##
```{r knitr}
library(knitr)

#TODO: This may cause a break with rmarkdown v2
opts_chunk$set(cache.path = file.path(labkey.file.root, "cache/testcache", labkey.user.email, ""))
```

```{r libs, cache = FALSE}
library("pheatmap")
```

```{r param-caching, cache=TRUE, cache.extra=digest::digest(labkey.data), echo=FALSE}
```


```{r data, cache = TRUE, dependson="param-caching"}
#mat <- matrix(rnorm(1e8, sd = 4), ncol = 1e4)
mat <- matrix(rnorm(100*100, sd = 4), ncol = 100)
head(labkey.data)
```


```{r heatmap, cache = TRUE, dependson="data"}
#pheatmap(mat[100,100], main = labkey.data$age[1], cluster_rows=FALSE, cluster_cols=FALSE, breaks=seq(-6,6,1))
plot(labkey.data[3])
```