

readRunPropertiesFile <- function()
{
    # set up a data frame to store the run properties
    properties = data.frame(NA, NA, NA, NA);
    colnames(properties) = c("name", "val1", "val2", "val3");

    #read in the run properties from the TSV
    lines = readLines("${runInfo}");

    # each line has a run property with the name, val1, val2, etc.
    for (i in 1:length(lines))
    {
        # split the line into the various parts (tab separated)
        parts = strsplit(lines[i], split="\t")[[1]];

        # if the line does not have 4 parts, add NA's as needed
        if (length(parts) < 4)
        {
            for (j in 1:4)
            {
                if (is.na(parts[j]))
                {
                    parts[j] = NA;
                }
            }
        }

        # add the parts for the given run property to the properties data frame
        properties[i,] = parts;
    }

    properties
}

getRunPropertyValue <- function(colName)
{
    value = NA;
    if (any(run.props$name == colName))
    {
        value = run.props$val1[run.props$name == colName];

        # return NA for an empty string
        if (nchar(value) == 0)
        {
            value = NA;
        }
    }
    value;
}

setMaxSeverity <- function(level)
{
	# 0:NONE, 1:WARN, 2:ERROR
	value = 0;
	
	# Don't display warnings if severityLevel set to ERROR
	if(level == 2)
	{
		value = 2;
	}
	else if(getRunPropertyValue("severityLevel") != "ERROR" && level > run.error.level)
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
run.props = readRunPropertiesFile();

# save the important run.props as separate variables
run.data.file = getRunPropertyValue("runDataFile");
run.output.file = run.props$val3[run.props$name == "runDataFile"];
runprop.output.file = getRunPropertyValue("transformedRunPropertiesFile");

# Logic here that might generate errors or warnings
# Set this to test different error levels
run.error.level = setMaxSeverity(1);

handleErrors();
