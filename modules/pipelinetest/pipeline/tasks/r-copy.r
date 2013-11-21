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
