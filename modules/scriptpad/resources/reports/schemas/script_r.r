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
#
# This sample code shows the new JSON output parameter type.
# renders as HTML. Replace this code with your R script. See the Help tab for more details.
#

#
# setup: load data
#
print('Setup script time');
ptm <- proc.time();
if (!exists("sharedSession")) {
    print('running setup code ...');
    require("RJSONIO");
    print('taking our sweet, sweet time');
    Sys.sleep(15);
    library(Cairo)
    datafile <- paste(labkey.url.base, "input_data.tsv", sep="");
    labkey.data <- read.table(datafile, header=TRUE, sep="\t", quote="\"", comment.char="")
    sharedSession <- 1;
}
print(proc.time() - ptm);

#
# plot 1
#
png(filename="${imgout:Blood Pressure/Single.jpg}");
plot(labkey.data$diastolicbloodpressure, labkey.data$systolicbloodpressure, 
main="Diastolic vs. Systolic Pressures: All Visits", 
ylab="Systolic (mm Hg)", xlab="Diastolic (mm Hg)", ylim =c(60, 200));
abline(lsfit(labkey.data$diastolicbloodpressure, labkey.data$systolicbloodpressure));
dev.off();

#
# plot 2
#
data_means <- aggregate(labkey.data, list(ParticipantID =
 labkey.data$participantid), mean, na.rm = TRUE);
Cairo(file="${imgout:Blood Pressure/Multiple.png}", type="png")
op <- par(mfcol = c(2, 2)) # 2 x 2 pictures on one plot
c11 <- plot(data_means$diastolicbloodpressure, data_means$weight_kg, ,
 xlab="Diastolic Blood Pressure (mm Hg)", ylab="Weight (kg)",
 mfg=c(1, 1))
abline(lsfit(data_means$diastolicbloodpressure, data_means$weight_kg))
c21 <- plot(data_means$diastolicbloodpressure, data_means$systolicbloodpressure, ,
 xlab="Diastolic Blood Pressure (mm Hg)",
 ylab="Systolic Blood Pressure (mm Hg)", mfg= c(2, 1))
abline(lsfit(data_means$diastolicbloodpressure, data_means$systolicbloodpressure))
c21 <- plot(data_means$diastolicbloodpressure, data_means$pulse, ,
 xlab="Diastolic Blood Pressure (mm Hg)",
 ylab="Pulse Rate (Beats/Minute)", mfg= c(1, 2))
abline(lsfit(data_means$diastolicbloodpressure, data_means$pulse))
c21 <- plot(data_means$diastolicbloodpressure, data_means$temp_c, ,
 xlab="Diastolic Blood Pressure (mm Hg)",
 ylab="Temperature (Degrees C)", mfg= c(2, 2))
abline(lsfit(data_means$diastolicbloodpressure, data_means$temp_c))
par(op); #Restore graphics parameters
dev.off();

#
# csv
#
write.csv(labkey.data, file = "${txtout:csvfile}");

#
# JSON output parameter examples
#

# named array
write(toJSON(list(myArray=c(1:10))), "${jsonout:myArray}");

# unnamed array
write(toJSON(c(1:10)), "${jsonout:myAnonArray}");

# scalar
write(toJSON(list(myNum=42)), "${jsonout:myNum}");

# unnamed scalar
write(toJSON(42), "${jsonout:myAnonNum}");

# record
write(toJSON(list(myRecord = list(firstName = "Dax", lastName = "Hawkins", favoriteNumber = 42))), "${jsonout:myRecord}");

# unnamed record
write(toJSON(list(firstName = "Dax", lastName = "Hawkins", favoriteNumber = 42)), "${jsonout:myAnonRecord}");

# combine outputs into one output parameter
allParams <- list(myArray=c(1:10), myNum=42,myRecord=list(firstName="Dax",lastName="Hawkins",favoriteNumber=42));
write(toJSON(allParams), "${jsonout:combineParams}");

#
# console output
#
param1 <- paste("param1: ", labkey.url.params$param1);
param2 <- paste("param2: ", labkey.url.params$param2);
print("Console output text");
print (param1);
print(param2);
