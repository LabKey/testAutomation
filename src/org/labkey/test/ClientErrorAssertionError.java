package org.labkey.test;

public class ClientErrorAssertionError extends AssertionError
{
    public ClientErrorAssertionError(Object detailMessage)
    {
        super(detailMessage);
    }
}
