/*
 * Copyright (c) 2009 LabKey Corporation
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
package org.labkey;

import org.labkey.remoteapi.query.*;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.sas.NetrcFileParser;

import java.io.*;
import java.util.*;

/*
* User: Karl Lum
* Date: Mar 4, 2009
* Time: 9:17:41 AM
*/
public class AssayValidator extends AbstractAssayValidator
{
    public static void main(String[] args) throws Exception
    {
        if (args.length < 4)
            throw new IllegalArgumentException("Input data file not passed in");

        File runProperties = new File(args[0]);
        if (runProperties.exists())
        {
            AssayValidator qc = new AssayValidator();

            qc.runQC(runProperties, args[1], args[2], args[3]);
        }
        else
            throw new IllegalArgumentException("Input data file does not exist");
    }

    public void runQC(File inputFile, String username, String password, String host) throws Exception
    {
        setEmail(username);
        setPassword(password);
        setHost(host);
        parseRunProperties(inputFile);

        if (getRunProperties().containsKey(Props.runDataFile.name()))
        {
            List<Map<String, String>> dataMap = parseRunData(new File(getRunProperties().get(Props.runDataFile.name())));
            Map<String, String> ptidMap = new HashMap<String, String>();
            Map<String, String> animalMap = new HashMap<String, String>();

            for (Map<String, String> row : dataMap)
            {
                // check for ptid duplicates
                String ptid = row.get("participantid");
                if (!ptidMap.containsKey(ptid))
                    ptidMap.put(ptid, ptid);
                else
                    writeError("A duplicate PTID was discovered : " + ptid, "runDataFile");

                // if the data contains a transformed column, make sure it contains a required value
                if (row.containsKey("animal"))
                    animalMap.put(row.get("animal"), row.get("animal"));
            }

            if (!animalMap.isEmpty())
            {
                if (!animalMap.containsKey("goat"))
                    writeError("The animal column must contain a goat", "runDataFile");
            }
            // add a log entry for this run
            //setCredentials(HOST);
            insertLog("Programmatic QC was run and " + getErrors().size() + " errors were found");
        }
        else
            writeError("Unable to locate the runDataFile", "runDataFile");
    }
}