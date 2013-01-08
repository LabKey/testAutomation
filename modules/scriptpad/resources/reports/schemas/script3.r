##
#  Copyright (c) 2012-2013 LabKey Corporation
# 
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
##
# This sample code returns the query data in tab-separated values format, which LabKey then
# renders as HTML. Replace this code with your R script. See the Help tab for more details.
datafile <- paste(labkey.url.base, "input_data.tsv", sep="");
labkey.data <- read.table(datafile, header=TRUE, sep="\t", quote="\"", comment.char="")
png(filename="${imgout:foo.jpg}");
plot(labkey.data$diastolicbloodpressure, labkey.data$systolicbloodpressure, 
main="Diastolic vs. Systolic Pressures: All Visits", 
ylab="Systolic (mm Hg)", xlab="Diastolic (mm Hg)", ylim =c(60, 200));
abline(lsfit(labkey.data$diastolicbloodpressure, labkey.data$systolicbloodpressure));
dev.off();
png(filename="${imgout:foo2.jpg}");
plot(labkey.data$diastolicbloodpressure, labkey.data$systolicbloodpressure,
main="Another image",
ylab="Systolic (mm Hg)", xlab="Diastolic (mm Hg)", ylim =c(60, 200));
dev.off();
write.csv(labkey.data, file = "${txtout:csvfile}");
write.table(labkey.data, file = "${fileout:tsvfileout}", sep = "t", qmethod = "double");
write.table(labkey.data, file = "${tsvout:tsvout}", sep = "t", qmethod = "double");
print("I am console output too - hear me roar!");