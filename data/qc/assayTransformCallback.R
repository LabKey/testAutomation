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
# A CallbackStatus of "report" or "fail" first records the script's working directory in the TransformWorkDirs list; "fail" then rejects the run.
# "edit" moves the Sync Callback Assay results of participant EditedByScript to visit 2, editing the run while its script runs.
suppressMessages(library(Rlabkey));

labkey.setDefaults(apiKey = "${apikey}");

run.props = labkey.transform.readRunPropertiesFile("${runInfo}");
sleep.seconds = as.numeric(labkey.transform.getRunPropertyValue(run.props, "SleepSeconds"));
runprop.output.file = labkey.transform.getRunPropertyValue(run.props, "transformedRunPropertiesFile");
errors.file = labkey.transform.getRunPropertyValue(run.props, "errorsFile");
status = labkey.transform.getRunPropertyValue(run.props, "CallbackStatus");

if (status %in% c("report", "fail"))
    labkey.insertRows(baseUrl = "${baseServerURL}", folderPath = "${containerPath}", schemaName = "lists", queryName = "TransformWorkDirs", toInsert = data.frame(WorkDir = dirname("${runInfo}")));

if (identical(status, "edit")) {
    results = labkey.selectRows(baseUrl = "${baseServerURL}", folderPath = "${containerPath}", schemaName = "assay.General.Sync Callback Assay", queryName = "Data", colSelect = "RowId", colFilter = makeFilter(c("ParticipantID", "EQUAL", "EditedByScript")), colNameOpt = "fieldname");
    if (nrow(results) == 0)
        stop("No Sync Callback Assay result for participant EditedByScript to edit");
    labkey.updateRows(baseUrl = "${baseServerURL}", folderPath = "${containerPath}", schemaName = "assay.General.Sync Callback Assay", queryName = "Data", toUpdate = data.frame(RowId = results$RowId, VisitID = 2));
}

Sys.sleep(sleep.seconds);

rows = labkey.selectRows(baseUrl = "${baseServerURL}", folderPath = "${containerPath}", schemaName = "exp", queryName = "Data", colSelect = "RowId");

if (identical(status, "fail")) {
    writeLines(paste("error", "", "Transform script rejected the run", sep = "\t"), errors.file);
} else {
    writeLines(c(paste("CallbackStatus", "succeeded", sep = "\t"), paste("runComments", "set by transform", sep = "\t")), runprop.output.file);
}
