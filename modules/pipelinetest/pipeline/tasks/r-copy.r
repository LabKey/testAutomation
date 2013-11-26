##
#  Copyright (c) 2013 LabKey Corporation
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
# echos the command line arguments
args = commandArgs(trailingOnly=TRUE)
print(args)

# reads the input file and prints the contents to stdout
lines = readLines(con="${input.txt}")

# print to stdout
cat("(stdout) contents of file: ${input.txt}\n")
for (line in lines) cat(line, "\n")
cat("\n")

# print to ${output.xxx}
cat(file="${output.xxx}", "(output) contents of file: ${input.txt}\n")
for (line in lines) cat(file="${output.xxx}", line, "\n")
cat(file="${output.xxx}", "\n")
