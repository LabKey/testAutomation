package org.labkey.test.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: tchadick
 * Date: 11/6/12
 * Time: 2:49 PM
 */
public class TestLogger
{
    private static final int indentStep = 2;
    private static int currentIndent = 0;

    private static final int MAX_INDENT = 20;
    private static final int MIN_INDENT = 0;

    public static void increaseIndent()
    {
        currentIndent += indentStep;
    }

    public static void decreaseIndent()
    {
        currentIndent -= indentStep;
    }

    private static String getIndentString()
    {
        String indentStr = "";
        for (int i = 0; i < currentIndent && i < MAX_INDENT; i++)
            indentStr += " ";
        return indentStr;
    }

    public static void log(String str)
    {
        String d = new SimpleDateFormat("HH:mm:ss,SSS").format(new Date()); // Include time with log entry.  Use format that matches labkey log.
        System.out.println(d + " " + getIndentString() + str);
    }
}
