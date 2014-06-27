##
#  Copyright (c) 2014 LabKey Corporation
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
lines = readLines(con="${input.xxx}")

# filter-regex parameter.  to integer if possible
filter_regex <- "${filter-regex}"

# print lines that match the regex to ${output.yyy}
f = file(description="${output.yyy}", open="w")
cat(file=f, sep="\n", grep(filter_regex, lines, value=TRUE))

flush(con=f)
close(con=f)

