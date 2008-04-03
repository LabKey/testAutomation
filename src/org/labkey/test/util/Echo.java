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
