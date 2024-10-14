package org.labkey.test;

/**
 * Indicates that a client-side error was found in the server's error log
 * Requires the 'javascriptErrorServerLogging' optional feature to be enabled on the server
 */
public class ClientErrorAssertionError extends ServerErrorAssertionError
{
    public ClientErrorAssertionError(String detailMessage)
    {
        super(detailMessage);
    }
}
