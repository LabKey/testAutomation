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
import org.apache.logging.log4j.core.LoggerContext;
import org.jetbrains.annotations.NotNull;
import org.labkey.test.TestFileUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class TestLogger
{
    static
    {
        // 'testAutomation/resources' isn't included in classpath. Need to load config file explicitly
        // TODO: Fixed in next gradle plugin release (>1.32.0)
        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        File file = new File(TestFileUtils.getTestRoot(), "resources/log4j2.xml");
        context.setConfigLocation(file.toURI());
    }

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

    private static String getIndentString()
    {
        return StringUtils.repeat(' ', Math.min(currentIndent, MAX_INDENT));
    }

    /**
     * Temporary workaround to make sure log4j2.xml gets loaded for all loggers
     * TODO: Remove once gradlePlugin v1.32.1 is released
     */
    public static Logger getLogger(final Class<?> clazz)
    {
        return LogManager.getLogger(clazz);
    }

    public static void setTestLogContext(String testLogContext)
    {
        TestLogger.testLogContext = testLogContext;
        updateThreadContext();
    }

    private static Logger getLog()
    {
        updateThreadContext();

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
        ThreadContext.put("testLogIndent", getIndentString());
    }

    public static void debug(String msg, Throwable t)
    {
        getLog().debug(msg, t);
    }

    public static void debug(String msg)
    {
        getLog().debug(msg);
    }

    public static void info(String str, Throwable t)
    {
        getLog().info(str, t);
    }

    public static void info(String str)
    {
        getLog().info(str);
    }

    public static void warn(String str, Throwable t)
    {
        getLog().warn(str, t);
    }

    public static void warn(String str)
    {
        getLog().warn(str);
    }

    public static void error(String str, Throwable t)
    {
        getLog().error(str, t);
    }

    public static void error(String str)
    {
        getLog().error(str);
    }

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
