##
#  Copyright (c) 2015-2019 LabKey Corporation
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
suppressMessages(library(Rlabkey));

setMaxSeverity <- function(level)
{
	# 0:NONE, 1:WARN, 2:ERROR
	value = 0;

	# Don't display warnings if severityLevel set to ERROR
	if(level == 2)
	{
		value = 2;
	}
	else if(labkey.transform.getRunPropertyValue(run.props, "severityLevel") != "ERROR" && level > run.error.level)
	{
		value = level;
	}
	value;
}

handleErrors <- function()
{
	if(run.error.level > 0)
	{
		fileConn<-file(runprop.output.file);
		if(run.error.level == 1)
		{
			writeLines(c(paste("maximumSeverity","WARN",sep="\t")), fileConn);
		}
		else
		{
			writeLines(c(paste("maximumSeverity","ERROR",sep="\t")), fileConn);
		}
		close(fileConn);

		# This file gets read and displayed directly as warnings or errors, depending on maximumSeverity level.
		fileConn<-file("errors.html");
		writeLines("<table border='1'>
                    	<tr>
                    		<th colspan='2'>There are errors in the input file</th>
                    	</tr>
                    	<tr>
                    		<td>Col1</td>
                    		<td>Col2</td>
                    	</tr>
                    	<tr>
                    		<td>test1</td>
                    		<td>test2</td>
                    	</tr>
                    </table>", fileConn);
		close(fileConn);
	}
}

run.error.level = 0;
run.props = labkey.transform.readRunPropertiesFile("${runInfo}");

# save the important run.props as separate variables
run.data.file = labkey.transform.getRunPropertyValue(run.props, "runDataFile");
run.output.file = run.props$val3[run.props$name == "runDataFile"];
runprop.output.file = labkey.transform.getRunPropertyValue(run.props, "transformedRunPropertiesFile");

# Logic here that might generate errors or warnings
# Set this to test different error levels
run.error.level = setMaxSeverity(2);

handleErrors();
