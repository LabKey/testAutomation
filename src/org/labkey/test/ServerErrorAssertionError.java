package org.labkey.test;

/**
 * Indicates that an error was found in the server's error log
 */
public class ServerErrorAssertionError extends AssertionError
{
    public ServerErrorAssertionError(String detailMessage)
    {
        super(detailMessage);
    }
}
