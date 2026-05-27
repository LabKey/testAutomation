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
library(lattice);
png(filename="${imgout:a}", width=900);
plot.new();
xyplot(systolicbloodpressure~ date| participantid, data=labkey.data,
 type="a", scales=list(draw=FALSE));
update(trellis.last.object(),
 strip = strip.custom(strip.names = FALSE, strip.levels = TRUE),
 main = "Systolic Pressure vs. Time, By Participant", 
 ylab="Systolic Pressure", xlab="");
dev.off();

png(filename="${imgout:b}", width=900);
plot.new();
xyplot(weight_kg ~ date| participantid, data=labkey.data,
 type="a", scales=list(draw=FALSE));
update(trellis.last.object(),
 strip = strip.custom(strip.names = FALSE, strip.levels = TRUE),
 main = "Weight vs. Time, By Participant", 
 ylab="Weight (kg)", xlab="");
dev.off();