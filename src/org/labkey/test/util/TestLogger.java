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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class TestLogger
{
    private static final Logger LOG = LogManager.getLogger(TestLogger.class);
    private static final Logger NO_OP = LogManager.getLogger("NoOpLogger");

    private static final int indentStep = 2;
    private static final int MAX_INDENT = 20;

    private static int currentIndent = 0;
    private static boolean suppressLogging = false;
    private static String testLogContext = "";

    public static void resetLogger()
    {
        currentIndent = 0;
        suppressLogging = false;
        updateThreadContext();
    }

    public static void increaseIndent()
    {
        currentIndent += indentStep;
        updateThreadContext();
    }

    public static void decreaseIndent()
    {
        if (currentIndent > 0)
            currentIndent -= indentStep;
        updateThreadContext();
    }

    public static void suppressLogging(boolean suppress)
    {
        suppressLogging = suppress;
    }

    public static void setTestLogContext(String testLogContext)
    {
        TestLogger.testLogContext = testLogContext;
        updateThreadContext();
    }

    @Contract (pure = true)
    public static Logger log()
    {
        updateThreadContext(); // Just to be safe

        if (suppressLogging)
        {
            return NO_OP;
        }
        else
        {
            return LOG;
        }
    }

    private static void updateThreadContext()
    {
        ThreadContext.put("testLogContext", testLogContext);
        ThreadContext.put("testLogIndent", StringUtils.repeat(' ', Math.min(currentIndent, MAX_INDENT)));
    }

    public static void debug(String message, Throwable t)
    {
        log().debug(message, t);
    }

    public static void debug(String message)
    {
        log().debug(message);
    }

    public static void info(String message, Throwable t)
    {
        log().info(message, t);
    }

    public static void info(String message)
    {
        log().info(message);
    }

    public static void warn(String message, Throwable t)
    {
        log().warn(message, t);
    }

    public static void warn(String message)
    {
        log().warn(message);
    }

    public static void error(String message, Throwable t)
    {
        log().error(message, t);
    }

    public static void error(String message)
    {
        log().error(message);
    }

    /**
     * @deprecated Use a specific log level. Usually {@link #info(String)}
     */
    @Deprecated
    public static void log(String str)
    {
        info(str);
    }

    /**
     * Format an elapsed time to be suitable for log messages.
     * Over one minute:
     *  " &lt;1m 25s&gt;"
     * Over one minute:
     *  " &lt;8.059s&gt;"
     * Less than on second:
     *  " &lt;125ms&gt;"
     * @param milliseconds Elapsed time in milliseconds
     * @return Formatted time
     */
    @NotNull
    public static String formatElapsedTime(long milliseconds)
    {
        long minutesPart = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long secondsPart = TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                TimeUnit.MINUTES.toSeconds(minutesPart);
        long millisecondsPart = milliseconds - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(milliseconds));

        StringBuilder elapsedStr = new StringBuilder(" <");
        if (minutesPart == 0 && secondsPart == 0) // milliseconds only
        {
            elapsedStr.append(millisecondsPart);
            elapsedStr.append("ms");
        }
        else
        {
            if (minutesPart > 0)
            {
                elapsedStr.append(minutesPart).append("m ");
            }
            elapsedStr.append(secondsPart);
            if (minutesPart == 0)
            {
                String millisecondsStr = String.valueOf(millisecondsPart);
                String padding = StringUtils.repeat("0", 3 - millisecondsStr.length());
                elapsedStr.append(".").append(padding).append(millisecondsPart);
            }
            elapsedStr.append("s");
        }
        elapsedStr.append(">");
        return elapsedStr.toString();
    }
}
