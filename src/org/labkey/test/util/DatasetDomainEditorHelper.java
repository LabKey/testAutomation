package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

public class DatasetDomainEditorHelper extends DomainEditorHelper
{
    public DatasetDomainEditorHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    public void waitForReady()
    {
        _test.waitForElement(Locator.lkButton("Cancel"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForElement(Locator.name("description"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.waitForElement(Locator.lkButton("Add Field"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }
}
