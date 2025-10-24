library(Rlabkey);

run.props = labkey.transform.readRunPropertiesFile("${runInfo}");
run.data.file = labkey.transform.getRunPropertyValue(run.props, "runDataFile");
run.output.file = run.props$val3[run.props$name == "runDataFile"];

run.data = read.delim(run.data.file, header=TRUE, sep="\t", quote="", check.names=FALSE);
run.data$TransformType = "${transformOperation}";
write.table(run.data, file=run.output.file, sep="\t", na="", row.names=FALSE, quote=FALSE);