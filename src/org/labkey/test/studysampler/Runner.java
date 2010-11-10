/*
 * Copyright (c) 2010 LabKey Corporation
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


/**
 * Created by IntelliJ IDEA.
 * User: newton
 * Date: Oct 27, 2010
 * Time: 11:09:34 AM
 *
 * Given a study export, create a new study export
 * where data for only a subset of participants and retained
 * and values of certain fields are replaced with new values
 *
 * Designed for Wisconsin EHR data.
 */
public class Runner
{

    // List of participant IDs to include data for
    static final Set<String> sampleIds = newStringSet(
        /*

        Insert list of participant IDs here.

         */
    );

    // the name of the field where the above id is found
    static final String idField = "id";

    // list of field names where the values should be aliased to something new
    static final Set<String> aliasFields = newStringSet(
            idField,
            "project",
            "account"
    );

    // list of field names that should be overwritten with filler content
    static final Set<String> wipeFields = newStringSet(
            "remark",
            "clinremark",
            "description",
            "inves",
            "surgeon"
    );

    static Random random = new Random();

    // to create and retrieve aliases for values during the run

    static class AliasFactory
    {
        private Map<String, String> _aliases = new HashMap<String, String>();

        String get(String key)
        {
            if (!_aliases.containsKey(key))
                _aliases.put(key, random.nextInt(999999) + "");

            return _aliases.get(key);
        }

    }

    static class FillerFactory
    {
        List<String> words;

        FillerFactory() throws IOException
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(Runner.class.getClassLoader().getResourceAsStream("words.txt")));
            words = new ArrayList<String>();
            String line = null;
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
        if (args.length != 1)
        {
            System.out.println("takes one argument, the filesystem path to the root of the study export you want to" +
                    " sample from. If you are running with ant do:\n\nant studysampler -Dstudysampler.dir=/path/to/study");
            return;
        }
        String studyRoot = args[0];
        System.out.println("Study directory: " + studyRoot);

        Set<File> targets = new HashSet<File>();
        targets.add(new File(studyRoot + "/lists/project.tsv"));
        for (File f : new File(studyRoot + "/datasets").listFiles())
        {
            if (f.getName().endsWith("tsv"))
                targets.add(f);
        }

        AliasFactory aliaser = new AliasFactory();
        FillerFactory filler = new FillerFactory();

        for (File inFile : targets)
        {
            File outFile = new File(inFile.getAbsolutePath() + ".tmp");
            CSVReader reader = new CSVReader(new FileReader(inFile.getAbsolutePath()), '\t');
            CSVWriter writer = new CSVWriter(new FileWriter(outFile), '\t');

            // read (and write) the field names.
            String[] row = reader.readNext();
            writer.writeNext(row);

            Integer idPosition = null;
            Set<Integer> wipePositions = new HashSet<Integer>();
            Set<Integer> aliasPositions = new HashSet<Integer>();

            for (int i = 0; i < row.length; i++)
            {
                if (aliasFields.contains(row[i].toLowerCase()))
                    aliasPositions.add(i);

                if (idField.equals(row[i].toLowerCase()))
                    idPosition = i;
                else if (wipeFields.contains(row[i].toLowerCase()))
                    wipePositions.add(i);
            }

            int read = 0;
            int wrote = 0;

            while ((row = reader.readNext()) != null)
            {
                read++;
                if (idPosition != null && !sampleIds.contains(row[idPosition]))
                    continue;

                for (Integer aliasPos : aliasPositions)
                    if (!row[aliasPos].isEmpty())
                        row[aliasPos] = aliaser.get(row[aliasPos]);


                for (Integer wipePos : wipePositions)
                    if (!row[wipePos].isEmpty())
                        row[wipePos] = filler.get(row[wipePos].length());

                writer.writeNext(row);
                wrote++;
            }

            writer.close();
            reader.close();
            outFile.renameTo(inFile);

            System.out.println("wrote " + wrote + " of " + read + " records in " + inFile);

        }

    }


    static Set<String> newStringSet(String... strings)
    {
        return new HashSet<String>(Arrays.asList(strings));
    }

}
