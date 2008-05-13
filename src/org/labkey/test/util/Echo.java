/*
 * Copyright (c) 2006-2007 LabKey Corporation
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

package org.labkey.test.util;

import java.io.*;

public class Echo
{
    public static void main(String[] args) throws Exception
    {
        PrintStream out = null;
        InputStream in = null;
        try
        {
            if (args.length > 2)
            {
                System.err.println("java Echo [infile] [outfile]");
                System.exit(1);
            }

            if (args.length == 0)
                in = System.in;
            else
                in = new FileInputStream(new File(args[0]));

            if (args.length == 1)
                out = System.out;
            else
                out = new PrintStream(new FileOutputStream(new File(args[1])));


            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String s;
            while (null != (s = reader.readLine()))
                out.println(s);
        }
        finally
        {
            if (args.length > 0 && null != in)
                in.close();
            if (args.length > 1 && null != out)
                out.close();
        }
    }

}
