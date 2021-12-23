package org.labkey.test.util;

public class DeferredAssertionError extends AssertionError
{
    private final boolean tookScreenshots;

    public DeferredAssertionError(Object detailMessage, boolean tookScreenshots)
    {
        super(detailMessage);
        this.tookScreenshots = tookScreenshots;
    }

    public boolean isTookScreenshots()
    {
        return tookScreenshots;
    }
}
