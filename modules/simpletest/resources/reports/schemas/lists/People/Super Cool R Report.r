##
#  Copyright (c) 2013-2019 LabKey Corporation
# 
#  Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
##

# ${input_data:inputTsv}
        grid <- read.table("inputTsv", header=TRUE, sep="\t")
        grid$name

# ${imgout:labkeyl.png}
        png(filename="labkeyl.png")
        plot(as.factor(labkey.data$name), labkey.data$age, main="Age")
        dev.off()

filename <- paste("do_render", ".gct");
write.table(labkey.data, file = filename, sep = "\t", qmethod = "double", col.names=NA)
filename <- paste("dont_render", ".gct");
write.table(labkey.data, file = filename, sep = "\t", qmethod = "double", col.names=NA)

# ${fileout:regex(.*?(do_.*\.gct))}