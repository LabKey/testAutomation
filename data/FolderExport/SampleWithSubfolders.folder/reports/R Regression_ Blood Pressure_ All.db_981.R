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
Cairo(file="${imgout:diastol_v_systol_figure.png}", type="png");
plot(labkey.data$diastolicbloodpressure, labkey.data$systolicbloodpressure, 
 main="Diastolic vs. Systolic Pressures: All Visits", 
 ylab="Systolic (mm Hg)", xlab="Diastolic (mm Hg)", ylim =c(60, 200));
abline(lsfit(labkey.data$diastolicbloodpressure, labkey.data$systolicbloodpressure));
dev.off();

png(filename="${imgout:diastol_v_systol_figure2.png}");
plot(labkey.data$diastolicbloodpressure, labkey.data$apxbpsys, 
 main="Diastolic vs. Systolic Pressures: All Visits", 
 ylab="Systolic (mm Hg)", xlab="Diastolic (mm Hg)", ylim =c(60, 200));
abline(lsfit(labkey.data$diastolicbloodpressure, labkey.data$systolicbloodpressure));
dev.off();