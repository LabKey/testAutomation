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
		writeLines("Inline warning from R transform.<br><a href=\"http://www.labkey.test\">Warning link</a>", fileConn);
		close(fileConn);
		
		# These two files are just to verify files are available to be downloaded and reviewed
		fileConn<-file("test1.txt");
		writeLines("This is test file 1 (R).", fileConn);
		close(fileConn);
		
		fileConn<-file("test2.tsv");
		writeLines("This is test file 2 (R).", fileConn);
		close(fileConn);

		write("stderr warning", stderr())
        write("stdout warning", stdout())
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
run.error.level = setMaxSeverity(1);

handleErrors();
