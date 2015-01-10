package org.labkey.test.pages;

import org.labkey.test.BaseWebDriverTest;

/**
 * Placeholder
 * This class should contain most basic page interaction functionality
 * {@link org.labkey.test.BaseWebDriverTest} currently does this
 */
public class LabKeyPage
{
    protected BaseWebDriverTest _test;

    public LabKeyPage(BaseWebDriverTest test)
    {
        _test = test;
        waitForPage();
    }

    protected void waitForPage() {}
}
