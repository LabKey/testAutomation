all.lines = readLines("${runInfo}")
runDataFile.line = grep("^runDataFile", all.lines, value = TRUE);
runDataFile.props <- strsplit(runDataFile.line, "\t")[[1]]
run.data.file = runDataFile.props[2];
run.output.file = runDataFile.props[4];

run.data = read.delim(run.data.file, header=TRUE, sep="\t", quote="", check.names=FALSE);
run.data$TransformType = "${transformOperation}";
write.table(run.data, file=run.output.file, sep="\t", na="", row.names=FALSE, quote=FALSE);