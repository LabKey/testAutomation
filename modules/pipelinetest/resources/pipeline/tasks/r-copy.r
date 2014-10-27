##
#  Copyright (c) 2013-2014 LabKey Corporation
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

# reads the input file and prints the contents to stdout
lines = readLines(con="${input.txt}")

# skip-lines parameter. convert to integer if possible
skipLines = as.integer("${skip-lines}")
if (is.na(skipLines)) {
    skipLines = 0
}

# lines in the file
lineCount = NROW(lines)

if (skipLines > lineCount) {
    cat("start index larger than number of lines")
} else {
    # start index
    start = skipLines + 1

    # print to stdout
    cat("(stdout) contents of file: ${input.txt}\n")
    for (i in start:lineCount) {
        cat(sep="", lines[i], "\n")
    }

    # print to ${output.xxx}
    f = file(description="${output.xxx}", open="w")
    cat(file=f, "# (output) contents of file: ${input.txt}\n")
    for (i in start:lineCount) {
        cat(file=f, sep="", lines[i], "\n")
    }
    flush(con=f)
    close(con=f)
}

# write output properties as a tab-separated name/value file
# containing the number of lines in the input file
outProps = file(description="${pipeline, taskOutputParams}", open="w")
cat(file=outProps, sep="", "name\tvalue\n")
cat(file=outProps, sep="", "lineCount\t", lineCount, "\n")
flush(con=outProps)
close(con=outProps)

