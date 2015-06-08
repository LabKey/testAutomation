/*
 * Copyright (c) 2010-2015 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.studysampler;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import org.apache.commons.io.IOUtils;
import org.labkey.api.reader.Readers;
import org.labkey.api.writer.PrintWriters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


/**
 * Given a study export, create a new study export
 * where data for only a subset of participants and retained
 * and values of certain fields are replaced with new values
 *
 * Designed for Wisconsin EHR data.
 */
public class Runner
{

    // List of subject IDs to include data for
    // Defined at: https://www.labkey.org/_webdav/O%27Connor/EHR%20Test%20Support/%40files/subjects.txt 
    static Set<String> subjectIds;

    // the name of the field where the above id is found
    static final String idField = "id";

    // the name of the field where snomed codes are found
    static final String snomedField = "code";

    static final int MAX_ROWS = 10000;

    // list of field names where the values should be aliased to something new
    static final Set<String> aliasFields = newStringSet(
            idField,
            "project",
            "account",
            "dam",
            "sire",
            "cage",
            "userid",
            "room",
            "roomcage"
    );

    // list of field names that should be overwritten with filler content
    static final Set<String> wipeFields = newStringSet(
            "remark",
            "clinremark",
            "description",
            "surgeon",
            "source",
            //project.tsv
            "reqname",
            //protocol.tsv
            "inves",
            //blood draws (1008)
            "done_by",
            "done_for",
            //necropsies (1022)
            "pathologist",
            "assistant",
            "caseno",
            //departure (1013)
            "destination",
            "authorize",
            //demographics (1012)
            "origin",
            //rhesaux.tsv
            "name"
    );

    static Random random; // Initialized with a constant seed defined in subjects file
    private static final String ANIMAL_PREFIX = "test";

    // to create and retrieve aliases for values during the run

    static class AliasFactory
    {
        private Map<String, String> _aliases = new HashMap<>();

        String get(String key)
        {
            return get(key, "");
        }

        String get(String key, String prefix)
        {
            if (!_aliases.containsKey(key))
            {
                String rand;
                do
                {
                    rand = Integer.toString(random.nextInt(9999999));
                }
                while(_aliases.containsValue(rand)); // ensure unique ids

                _aliases.put(key, prefix + rand);
            }

            return _aliases.get(key);
        }

    }

    static class FillerFactory
    {
        List<String> words;

        FillerFactory() throws IOException
        {
            BufferedReader reader = Readers.getReader(new File("words.txt"));
            words = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null)
            {
                words.add(line);
            }

            reader.close();
        }

        String get(int size)
        {
            StringBuilder sb = new StringBuilder();
            String delim = "";
            while (sb.length() < size)
            {
                sb.append(delim + words.get(random.nextInt(words.size())));
                delim = " ";
            }
            return sb.toString();
        }
    }

    public static void main(String[] args) throws IOException
    {
        long startTime = System.currentTimeMillis();

        if (args.length != 2)
        {
            System.out.println("Takes two arguments: absolute/path/to/study absolute/path/to/subjectslist.txt");
            return;
        }
        File studyRoot = new File(args[0]);
        File destStudyRoot = new File(studyRoot.getParent(), studyRoot.getName() + " Anon");
        File subjectFile = new File(args[1]);
        System.out.println("Study directory: " + studyRoot);
        System.out.println("Sample file: " + subjectFile);

        AliasFactory aliaser = new AliasFactory();
        FillerFactory filler = new FillerFactory();
        loadSubjectList(subjectFile, aliaser);

        // List of lists to be skipped
        Set<File> clearTargets = new HashSet<>();

        //NOTE: this list has been removed from the EHR, so this line is no longer needed
//        clearTargets.add(new File(studyRoot + "/lists/deleted_records.tsv")); // contains scattered subject ids

        // Get a set of lists to be minimized
        Set<File> minimizeTargets = new HashSet<>();
        for (File f : new File(studyRoot + "/lists").listFiles())
        {
            if (!clearTargets.contains(f) && f.getName().contains("snomed"))
                minimizeTargets.add(f);
        }

        // Get a set of files to be transformed
        Set<File> anonymizeTargets = new HashSet<>();
        for (File f : new File(studyRoot + "/datasets").listFiles())
        {
            if (f.getName().endsWith("tsv") && !minimizeTargets.contains(f) && !clearTargets.contains(f))
                anonymizeTargets.add(f);
        }
        for (File f : new File(studyRoot + "/lists").listFiles())
        {
            if (f.getName().endsWith("tsv") && !minimizeTargets.contains(f) && !clearTargets.contains(f))
                anonymizeTargets.add(f);
        }

        // Get a set of all other study files
        Set<File> allFiles = new HashSet<>();
        for (File f : listFilesRecursive(studyRoot) )
        {
            if(!anonymizeTargets.contains(f) && !minimizeTargets.contains(f) && !clearTargets.contains(f))
                allFiles.add(f);
        }

        ArrayList<String> usedSnomeds = new ArrayList<>();

        System.out.println("\nAnonymize lists and datasets");

        for (File inFile : anonymizeTargets)
        {
            File outFile = new File(destStudyRoot, studyRoot.toURI().relativize(inFile.toURI()).getPath());
            outFile.getParentFile().mkdirs();
            CSVReader reader = new CSVReader(Readers.getReader(inFile), '\t');
            CSVWriter writer = new CSVWriter(PrintWriters.getPrintWriter(outFile), '\t');

            // read (and write) the field names.
            String[] row = reader.readNext();
            writer.writeNext(row);

            Integer idPosition = null;
            Integer snomedPosition = null;
            Set<Integer> wipePositions = new HashSet<>();
            Set<Integer> aliasPositions = new HashSet<>();

            for (int i = 0; i < row.length; i++)
            {
                if (aliasFields.contains(row[i].toLowerCase()))
                    aliasPositions.add(i);

                if (idField.equals(row[i].toLowerCase()))
                    idPosition = i;
                if (snomedField.equals(row[i].toLowerCase()))
                    snomedPosition = i;
                else if (wipeFields.contains(row[i].toLowerCase()))
                    wipePositions.add(i);
            }

            int columnCount = row.length;
            int read = 0;
            int wrote = 0;

            while ((row = reader.readNext()) != null && (wrote < MAX_ROWS || MAX_ROWS == -1))
            {
                read++;

                if (row.length != columnCount)
                {
                    System.out.println(inFile.getName() + ". Malformed row ["+read+"]. Skipping.");
                    continue;
                }

                if (idPosition != null && !subjectIds.contains(row[idPosition]))
                    continue;

                for (Integer aliasPos : aliasPositions)
                    if (!row[aliasPos].isEmpty())
                        row[aliasPos] = aliaser.get(row[aliasPos]);


                for (Integer wipePos : wipePositions)
                    if (!row[wipePos].isEmpty())
                        row[wipePos] = filler.get(row[wipePos].length());

                if (snomedPosition != null)
                    usedSnomeds.add(row[snomedPosition]);

                writer.writeNext(row);
                wrote++;
            }

            writer.close();
            reader.close();

            System.out.println("wrote " + wrote + " of " + read + " records from " + inFile + " to " + outFile);
        }

        System.out.println("\nPrune snomed lists");
        for(File inFile : minimizeTargets)
        {
            File outFile = new File(destStudyRoot, studyRoot.toURI().relativize(inFile.toURI()).getPath());
            outFile.getParentFile().mkdirs();

            CSVReader reader = new CSVReader(Readers.getReader(inFile), '\t');
            CSVWriter writer = new CSVWriter(PrintWriters.getPrintWriter(outFile), '\t'); // Excel format escapes quotes with quotes; manually remove such rows.

            // read (and write) the field names.
            String[] row = reader.readNext();
            writer.writeNext(row);

            Integer snomedPosition = null;

            for (int i = 0; i < row.length; i++)
            {
                if (snomedField.equals(row[i].toLowerCase()))
                    snomedPosition = i;
            }

            int columnCount = row.length;
            int read = 0;
            int wrote = 0;

            while ((row = reader.readNext()) != null && wrote < MAX_ROWS)
            {
                read++;

                if (row.length != columnCount)
                {
                    System.out.println(inFile.getName() + ". Malformed row ["+read+"]. Skipping.");
                    continue;
                }

                if (usedSnomeds.contains(row[snomedPosition]))
                {
                    writer.writeNext(row);
                    wrote++;
                }
            }

            writer.close();
            reader.close();

            System.out.println("wrote " + wrote + " of " + read + " records from " + inFile + " to " + outFile);
        }

        System.out.println("\nCreate empty files.");

        for (File inFile : clearTargets)
        {
            File outFile = new File(destStudyRoot, studyRoot.toURI().relativize(inFile.toURI()).getPath());
            outFile.getParentFile().mkdirs();

            try (BufferedReader input = Readers.getReader(inFile);
                 PrintWriter output = PrintWriters.getPrintWriter(outFile))
            {
                //Copy column headers only.
                String line = input.readLine();
                output.write(line);
            }

            System.out.println("wrote " + outFile.length() + " bytes from " + inFile + " to " + outFile);
        }

        System.out.println("\nCopy remaining study files");
        for (File inFile : allFiles)
        {
            File outFile = new File(destStudyRoot, studyRoot.toURI().relativize(inFile.toURI()).getPath());
            outFile.getParentFile().mkdirs();

            int bytes;

            try (FileInputStream input = new FileInputStream(inFile); FileOutputStream output = new FileOutputStream(outFile))
            {
                bytes = IOUtils.copy(input, output);
            }

            System.out.println("wrote " + bytes + " bytes from " + inFile + " to " + outFile);
        }

        System.out.println("Elapsed: " + (int)((System.currentTimeMillis() - startTime)/1000) + " seconds.");
    }

    static void loadSubjectList(File subjectListFile, AliasFactory aliaser) throws IOException
    {
        String line;
        subjectIds = newStringSet();

        try (BufferedReader reader = Readers.getReader(subjectListFile))
        {
            random = new Random(Long.parseLong(reader.readLine())); // First line is seed.

            while (!(line = reader.readLine()).equals(""))
            {
                subjectIds.add(line);
                aliaser.get(line, ANIMAL_PREFIX); // Pre-generate aliases to ensure consistency.
            }
            // Animal IDs separated from non animal IDs by a single blank line.
            while ((line = reader.readLine()) != null)
            {
                subjectIds.add(line);
                aliaser.get(line); // Pre-generate aliases to ensure consistency (non-animal id).
            }
        }
    }

    static ArrayList<File> listFilesRecursive(File path)
    {
        File[] files = path.listFiles();
        ArrayList<File> allFiles = new ArrayList<>();
        for (File file : files)
        {
            if ( file.isDirectory() )
                allFiles.addAll(listFilesRecursive(file));
            else // file.isFile()
                allFiles.add(file);
        }
        return allFiles;
    }

    static Set<String> newStringSet(String... strings)
    {
        return new HashSet<>(Arrays.asList(strings));
    }

}
