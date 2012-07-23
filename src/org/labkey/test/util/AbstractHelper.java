package org.labkey.test.util;

import org.labkey.test.BaseSeleniumWebTest;

/**
 * User: jeckels
 * Date: Jul 20, 2012
 */
public abstract class AbstractHelper
{
    protected final BaseSeleniumWebTest _test;

    public AbstractHelper(BaseSeleniumWebTest test)
    {
        _test = test;
    }
}
