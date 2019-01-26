package org.labkey.serverapi.writer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class PrintWriters
{
    /**
     * Create a standard PrintWriter, with standard character set and buffering, for output to an OutputStream
     *
     * @param out OutputStream destination for the new PrintWriter
     * @return A standard, buffered PrintWriter targeting the OutputStream
     */
    public static PrintWriter getPrintWriter(OutputStream out)
    {
        return new StandardPrintWriter(out);
    }

    /**
     * Create a standard PrintWriter, with standard character set and buffering, for output to a File
     *
     * @param file File destination for the new PrintWriter
     * @return A standard, buffered PrintWriter targeting the File
     */
    public static PrintWriter getPrintWriter(File file) throws FileNotFoundException
    {
        return new StandardPrintWriter(file);
    }


    // Use factory methods above, unless you really need to subclass
    public static class StandardPrintWriter extends PrintWriter
    {
        public StandardPrintWriter(OutputStream out)
        {
            super(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)));
        }

        public StandardPrintWriter(File file) throws FileNotFoundException
        {
            super(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)));
        }
    }
}
