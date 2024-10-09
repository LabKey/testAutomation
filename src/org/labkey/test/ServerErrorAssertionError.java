package org.labkey.test;

public class ServerErrorAssertionError extends AssertionError
{
    public ServerErrorAssertionError(Object detailMessage)
    {
        super(detailMessage);
    }
}
