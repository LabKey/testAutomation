#!/usr/local/bin/perl
use strict;
use warnings;


# Open the run properties file.  Run or upload set properties are not used by
# this script. We are only interested in the file paths for the run data and
# the error file.

open my $reportProps, '${runInfo}';

my $transformFileName = "unknown";
my $dataFileName = "unknown";

my %transformFiles;

# Parse the data file properties from reportProps and save the transformed data location
# in a map. It's possible for an assay to have more than one transform data file, although
# most will only have a single one.

while (my $line=<$reportProps>)
{
   chomp($line);
   my @row = split(/\t/, $line);
  
   if ($row[0] eq 'runDataFile')
   {
      $dataFileName = $row[1];

      # transformed data location is stored in column 4
     
      $transformFiles{$dataFileName} = $row[3];
   }
}

my $key;
my $value;
my $adjustM1 = 0;

# Read each line from the uploaded data file and insert new data (double the value in the M1 field)
# into an additional column named 'adjustedM1’. The additional column must already exist in the assay
# definition and be of the correct type.

while (($key, $value) = each(%transformFiles)) {

    open my $dataFile, $key or die "Can't open '$key': $!";
    open my $transformFile, '>', $value or die "Can't open '$value': $!";

    my $line=<$dataFile>;
    chomp($line);
    $line =~ s/\r*//g;
    print $transformFile $line, "\t", "adjustedM1", "\n";

    while (my $line=<$dataFile>)
    {
       $adjustM1 = substr($line, 27, 3) * 2;
       chomp($line);
       $line =~ s/\r*//g;
       print $transformFile $line, "\t", $adjustM1, "\n";
	 
    }

    close $dataFile;
    close $transformFile;
}