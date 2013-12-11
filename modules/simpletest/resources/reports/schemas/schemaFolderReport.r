##
#  Copyright (c) 2012-2013 LabKey Corporation
#
#  Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
##
cat('Hello, ', labkey.url.params$greeter, '!\n', sep='')

cat("{ \"a\": [1,2,3], \"", labkey.url.params$greeter, "\": \"Hello\" }\n", sep="", file="${jsonout:hello.json}")

