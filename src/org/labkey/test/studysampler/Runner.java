/*
 * Copyright (c) 2010-2011 LabKey Corporation
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.commons.io.IOUtils;


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

    // List of subject IDs to include data for
    static Set<String> subjectIds;

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
            "surgeon",
            "source"
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
            BufferedReader reader = new BufferedReader(new FileReader("words.txt"));
            words = new ArrayList<String>();
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
        if (args.length != 2)
        {
            System.out.println("takes two arguments, the filesystem path to the root of the study export you want to" +
                    " sample from and the path to a file listing subjects who's data should be kept and anonymized. \n" +
                    "Ant usage:\nant studysampler -Dstudysampler.dir=/path/to/study -Dstudysampler.subjectfile=/path/to/file.txt");
            return;
        }
        File studyRoot = new File(args[0]);
        File destStudyRoot = new File(studyRoot.getParent(), studyRoot.getName() + " Anon");
        File subjectFile = new File(args[1]);
        System.out.println("Study directory: " + studyRoot);
        System.out.println("Sample file: " + subjectFile);

        loadSubjectList(subjectFile);

        // Get a list of files to be transformed
        Set<File> targets = new HashSet<File>();
        targets.add(new File(studyRoot + "/lists/project.tsv"));
        for (File f : new File(studyRoot + "/datasets").listFiles())
        {
            if (f.getName().endsWith("tsv"))
                targets.add(f);
        }

        // Get a list of all other study files
        Set<File> allFiles = new HashSet<File>();
        for (File f : listFilesRecursive(studyRoot) )
        {
            if(!targets.contains(f))
                allFiles.add(f);
        }

        AliasFactory aliaser = new AliasFactory();
        FillerFactory filler = new FillerFactory();

        for (File inFile : targets)
        {
            File outFile = new File(destStudyRoot, studyRoot.toURI().relativize(inFile.toURI()).getPath());
            outFile.getParentFile().mkdirs();
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
                if (idPosition != null && !subjectIds.contains(row[idPosition]))
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

            System.out.println("wrote " + wrote + " of " + read + " records from " + inFile + " to " + outFile);
        }

        for (File inFile : allFiles)
        {
            File outFile = new File(destStudyRoot, studyRoot.toURI().relativize(inFile.toURI()).getPath());
            outFile.getParentFile().mkdirs();

            FileInputStream input = new FileInputStream(inFile);
            FileOutputStream output = new FileOutputStream(outFile);

            int bytes = IOUtils.copy(input,output);

            output.close();
            input.close();

            System.out.println("wrote " + bytes + " bytes from " + inFile + " to " + outFile);
        }
    }

    static void loadSubjectList(File subjectListFile) throws IOException
    {
        BufferedReader reader;
        String line;
        subjectIds = newStringSet();

        reader = new BufferedReader(new FileReader(subjectListFile));
        while((line = reader.readLine()) != null){
            subjectIds.add(line);
        }

        reader.close();
    }

    static ArrayList<File> listFilesRecursive(File path)
    {
        File[] files = path.listFiles();
        ArrayList<File> allFiles = new ArrayList<File>();
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
        return new HashSet<String>(Arrays.asList(strings));
    }

}
