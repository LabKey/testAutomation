##
#  Copyright (c) 2020-2026 LabKey Corporation
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
library(Cairo);
data_means <- aggregate(labkey.data, list(ParticipantID = 
 labkey.data$participantid), mean, na.rm = TRUE);
Cairo(file="${imgout:multiplot.png}", type="png")
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