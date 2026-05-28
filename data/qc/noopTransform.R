##
#  Copyright (c) 2025-2026 LabKey Corporation
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
all.lines = readLines("${runInfo}")
runDataFile.line = grep("^runDataFile", all.lines, value = TRUE);
runDataFile.props <- strsplit(runDataFile.line, "\t")[[1]]
run.data.file = runDataFile.props[2];
run.output.file = runDataFile.props[4];

run.data = read.delim(run.data.file, header=TRUE, sep="\t", quote="", check.names=FALSE);
run.data$TransformType = "${transformOperation}";
write.table(run.data, file=run.output.file, sep="\t", na="", row.names=FALSE, quote=TRUE, qmethod="double");