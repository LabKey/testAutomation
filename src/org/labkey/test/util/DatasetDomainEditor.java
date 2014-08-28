package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.pages.DomainEditor;

public class DatasetDomainEditor extends DomainEditor
{
    public DatasetDomainEditor(BaseWebDriverTest test)
    {
        super(test);
    }

    public void waitForReady()
    {
        super.waitForReady();
        _test.waitForElement(Locator.name("description"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }
}
