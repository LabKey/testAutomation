package org.labkey.test.drt;

import org.labkey.test.BaseSeleniumWebTest;

/**
 * User: brittp
 * Date: Nov 30, 2005
 * Time: 10:53:59 PM
 */
public class JUnitTest extends BaseSeleniumWebTest
{
    public String getAssociatedModuleDirectory()
    {
        return "none";
    }

    protected void doCleanup() throws Exception
    {
        // nothing to clean up
    }

    protected void doTestSteps()
    {
        beginAt("/Junit/begin.view");
        log("Run tests");
        //Wait up to 5 minutes!
        clickNavButton("Run All", 1000 * 60 * 5);
        assertTextPresent("SUCCESS");
    }
}
