package org.labkey.test.logging;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TestLogLayout extends Layout
{
    private static final int MAX_INDENT = 20;
    private static final int INDENT_STEP = 2;

    private static int currentIndent = 0;

    public static void reset()
    {
        currentIndent = 0;
    }

    public static void increaseIndent()
    {
        currentIndent += INDENT_STEP;
    }

    public static void decreaseIndent()
    {
        if (currentIndent > 0)
            currentIndent -= INDENT_STEP;
    }

    private static String getIndentString()
    {
        return StringUtils.repeat(' ', Math.min(currentIndent, MAX_INDENT));
    }

    @Override
    public String format(LoggingEvent event)
    {
        String message = event.getRenderedMessage();
        if (message == null)
        {
            return "";
        }
        else
        {
            String d = new SimpleDateFormat("HH:mm:ss,SSS").format(new Date()); // Include time with log entry.  Use format that matches labkey log.
            return d + " " + event.getThreadName() + " " + getIndentString() + message;
        }
    }

    @Override
    public boolean ignoresThrowable()
    {
        return true;
    }

    @Override
    public void activateOptions()
    {
        // No options
    }
}
