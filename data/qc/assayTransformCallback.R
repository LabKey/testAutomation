##
#  Copyright (c) 2026 LabKey Corporation
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
# Sleeps for the SleepSeconds run property, then queries exp.Data through the API before letting the import finish.
suppressMessages(library(Rlabkey));

labkey.setDefaults(apiKey = "${apikey}");

run.props = labkey.transform.readRunPropertiesFile("${runInfo}");
sleep.seconds = as.numeric(labkey.transform.getRunPropertyValue(run.props, "SleepSeconds"));
runprop.output.file = labkey.transform.getRunPropertyValue(run.props, "transformedRunPropertiesFile");

Sys.sleep(sleep.seconds);

rows = labkey.selectRows(baseUrl = "${baseServerURL}", folderPath = "${containerPath}", schemaName = "exp", queryName = "Data", colSelect = "RowId");

writeLines(c(paste("CallbackStatus", "succeeded", sep = "\t"), paste("runComments", "set by transform", sep = "\t")), runprop.output.file);
