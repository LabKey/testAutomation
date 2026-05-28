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
data_means <- aggregate(labkey.data, list(ParticipantID = 
 labkey.data$participantid), mean, na.rm = TRUE);
library(Cairo);
Cairo(file="${imgout:diastol_v_systol_means_figure.png}", type="png");
plot(data_means$diastolicbloodpressure, data_means$systolicbloodpressure, 
 main="Diastolic vs. Systolic Pressures: Mean Value For Each Participant", 
 ylab="Systolic (mm Hg)", xlab="Diastolic (mm Hg)", ylim =c(60, 200));
abline(lsfit(data_means$diastolicbloodpressure, data_means$systolicbloodpressure));
dev.off();