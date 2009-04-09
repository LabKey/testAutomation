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
public class AssayValidator
{
    private String _email;
    private String _password;
    private File _errorFile;
    private Map<String, String> _runProperties;
    private List<String> _errors = new ArrayList<String>();

    private static final String HOST_NAME = "http://localhost:8080/labkey";
    private static final String HOST = "localhost:8080";

    public static void main(String[] args)
    {
        if (args.length < 3)
            throw new IllegalArgumentException("Input data file not passed in");

        File runProperties = new File(args[0]);
        if (runProperties.exists())
        {
            AssayValidator qc = new AssayValidator();

            qc.runQC(runProperties, args[1], args[2]);
        }
        else
            throw new IllegalArgumentException("Input data file does not exist");
    }

    public void runQC(File inputFile, String username, String password)
    {
        try {
            _email = username;
            _password = password;
            _runProperties = parseRunProperties(inputFile);

            if (_runProperties.containsKey("errorsFile"))
                _errorFile = new File(_runProperties.get("errorsFile"));

            if (_runProperties.containsKey("runDataFile"))
            {
                List<Map<String, String>> dataMap = parseRunData(new File(_runProperties.get("runDataFile")));
                Map<String, String> ptidMap = new HashMap<String, String>();

                // check for ptid duplicates
                for (Map<String, String> row : dataMap)
                {
                    String ptid = row.get("participantid");
                    if (!ptidMap.containsKey(ptid))
                        ptidMap.put(ptid, ptid);
                    else
                        writeError("A duplicate PTID was discovered : " + ptid, "runDataFile");
                }

                // add a log entry for this run
                //setCredentials(HOST);
                insertLog();
            }
            else
                writeError("Unable to locate the runDataFile", "runDataFile");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void insertLog() throws Exception
    {
        Connection con = new Connection(HOST_NAME, _email, _password);
        InsertRowsCommand cmd = new InsertRowsCommand("lists", "QC Log");

        Map<String, Object> row = new HashMap<String,Object>();
        row.put("Date", new Date());
        row.put("Container", _runProperties.get("containerPath"));
        row.put("AssayId", _runProperties.get("assayId"));
        row.put("AssayName", _runProperties.get("assayName"));
        row.put("User", _runProperties.get("userName"));
        row.put("Comments", "Programmatic QC was run and " + _errors.size() + " errors were found");

        cmd.addRow(row);
        cmd.execute(con, _runProperties.get("containerPath"));
    }

    private void writeError(String message, String prop) throws IOException
    {
        if (_errorFile != null)
        {
            _errors.add(message);
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(_errorFile, true)));

            StringBuilder sb = new StringBuilder();
            sb.append("error\t");
            sb.append(prop);
            sb.append('\t');
            sb.append(message);
            sb.append('\n');

            pw.write(sb.toString());
            pw.close();
        }
        else
            throw new RuntimeException("Errors file does not exist");
    }

    private Map<String, String> parseRunProperties(File runProperties)
    {
        BufferedReader br = null;
        Map<String, String> props = new HashMap<String, String>();

        try {
            br = new BufferedReader(new FileReader(runProperties));
            String l;
            while ((l = br.readLine()) != null)
            {
                System.out.println(l);
                String[] parts = l.split("\t");
                props.put(parts[0], parts[1]);
            }
            return props;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }
        finally
        {
            if (br != null)
                try {br.close();} catch(IOException ioe) {}
        }
    }

    /**
     * Parse the tab-delimitted input data file
     */
    private List<Map<String, String>> parseRunData(File data)
    {
        BufferedReader br = null;
        Map<Integer, String> columnMap = new HashMap<Integer, String>();
        List<Map<String, String>> dataMap = new ArrayList<Map<String, String>>();

        try {
            br = new BufferedReader(new FileReader(data));
            String l;
            boolean isHeader = true;
            while ((l = br.readLine()) != null)
            {
                if (isHeader)
                {
                    int i=0;
                    for (String col : l.split("\t"))
                        columnMap.put(i++, col.toLowerCase());
                    isHeader = false;
                }
                dataMap.add(parseDataRow(l, columnMap));
            }
            return dataMap;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }
        finally
        {
            if (br != null)
                try {br.close();} catch(IOException ioe) {}
        }
    }

    private Map<String, String> parseDataRow(String row, Map<Integer, String> columnMap)
    {
        Map<String, String> props = new HashMap<String, String>();
        int i=0;
        for (String col : row.split("\t"))
        {
            props.put(columnMap.get(i), col);
            i++;
        }
        return props;
    }

    private void setCredentials(String host) throws IOException
    {
        NetrcFileParser parser = new NetrcFileParser();
        NetrcFileParser.NetrcEntry entry = parser.getEntry(host);

        if (null != entry)
        {
            _email = entry.getLogin();
            _password = entry.getPassword();
        }
    }
}