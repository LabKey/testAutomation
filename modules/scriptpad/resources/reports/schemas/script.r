# This sample code returns the query data in tab-separated values format, which LabKey then
# renders as HTML. Replace this code with your R script. See the Help tab for more details.
require("RJSONIO");

datafile <- paste(labkey.url.base, "input_data.tsv", sep="");
labkey.data <- read.table(datafile, header=TRUE, sep="\t", quote="\"", comment.char="")
png(filename="${imgout:foo.jpg}");
plot(labkey.data$diastolicbloodpressure, labkey.data$systolicbloodpressure, 
main="Diastolic vs. Systolic Pressures: All Visits", 
ylab="Systolic (mm Hg)", xlab="Diastolic (mm Hg)", ylim =c(60, 200));
abline(lsfit(labkey.data$diastolicbloodpressure, labkey.data$systolicbloodpressure));
dev.off();
print("I am console output - hear me roar!");
param1 <- labkey.url.params$param1;
param2 <- labkey.url.params$param2;

write(toJSON(list(myArray=c(1:10))), "${jsonout:myArray}");
write(toJSON(c(1:10)), "${jsonout:myAnonArray}");
write(toJSON(list(myNum=42)), "${jsonout:myNum}");
# note that this is a vector so it is persisted as [42] and decoded as an array.
write(toJSON(42), "${jsonout:myAnonNum}");
write(toJSON(list(myRecord = list(firstName = "Dax", lastName = "Hawkins", age = 41))), "${jsonout:myRecord}");
write(toJSON(list(firstName = "Dax", lastName = "Hawkins", age = 41)), "${jsonout:myAnonRecord}");

# give a sample of writing out everything in one file
# note that the anon array c(1:10) doesn't decode correctly
allParams <- list(myArray=c(1:10),c(1:10),mynum=42,42,myRecord=list(firstName="Dax",lastName="Hawkins",age=41),list(firstName="Dax",lastName="Hawkins",age=41));
write(toJSON(allParams), "${jsonout:allParams}");

print(param1);
print(param2);
