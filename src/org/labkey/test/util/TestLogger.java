/*
 * Copyright (c) 2012-2019 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.TestProperties;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestLogger
{
    private static final int indentStep = 2;
    private static int currentIndent = 0;
    private static boolean suppressLogging = false;

    private static final int MAX_INDENT = 20;

    public static void resetLogger()
    {
        currentIndent = 0;
        suppressLogging = false;
    }

    public static void increaseIndent()
    {
        currentIndent += indentStep;
    }

    public static void decreaseIndent()
    {
        if (currentIndent > 0)
            currentIndent -= indentStep;
    }

    public static void suppressLogging(boolean suppress)
    {
        suppressLogging = suppress;
    }

    private static String getIndentString()
    {
        return StringUtils.repeat(' ', Math.min(currentIndent, MAX_INDENT));
    }

    public static void debug(String msg)
    {
        // TODO: Log at debug level if/when we convert to Log4J or similar
        if (TestProperties.isDebugLoggingEnabled())
            log("DEBUG: " + msg, System.out);
    }

    public static void warn(String str, Throwable t)
    {
        log("WARNING: " + str, System.out, t);
    }

    public static void warn(String str)
    {
        log("WARNING: " + str, System.out);
    }

    public static void error(String str)
    {
        log(str, System.err);
    }

    public static void error(String str, Throwable t)
    {
        log(str, System.err, t);
    }

    public static void log(String str)
    {
        if (!suppressLogging)
        {
            log(str, System.out);
        }
    }

    private static void log(String str, PrintStream out, Throwable t)
    {
        log(str, out);
        t.printStackTrace(out);
    }

    private static void log(String str, PrintStream out)
    {
        String d = new SimpleDateFormat("HH:mm:ss,SSS").format(new Date()); // Include time with log entry.  Use format that matches labkey log.
        out.println(d + " " + getIndentString() + str);
    }
}
