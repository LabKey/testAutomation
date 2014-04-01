/*
 * Copyright (c) 2009-2013 LabKey Corporation
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
package org.labkey.test.qc;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class JavaQC
{
    private static File _errorFile;

    public static void main(String[] args)
    {
        if (args.length != 1)
            throw new IllegalArgumentException("Input data file not passed in");

        File inputFile = new File(args[0]);
        if (inputFile.exists())
        {
            try {
                Map<String, String> props = parseInputData(inputFile);

                if (props.containsKey("errorsFile"))
                    _errorFile = new File(props.get("errorsFile"));

                writeError("A Generic Error Occurred", "runDataFile");
                writeError("A Second Generic Error Occurred", "runDataFile");
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        else
            throw new IllegalArgumentException("Input data file does not exist");
    }

    private static void writeError(String message, String prop) throws IOException
    {
        if (_errorFile != null)
        {
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
    private static Map<String, String> parseInputData(File inputData)
    {
        Map<String, String> props = new HashMap<>();

        try(BufferedReader br = new BufferedReader(new FileReader(inputData)))
        {
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
    }

}