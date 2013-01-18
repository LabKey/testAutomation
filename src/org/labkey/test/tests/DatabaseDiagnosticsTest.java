package org.labkey.test.tests;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.LogMethod;

/**
 * User: tchadick
 * Date: 1/15/13
 * Time: 12:32 PM
 */
public class DatabaseDiagnosticsTest extends BaseWebDriverTest
{
    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        validateDomainsTest();
        databaseCheckTest();
    }

    @LogMethod
    private void validateDomainsTest()
    {
        goToAdmin();

        clickAndWait(Locator.linkWithText("Check Database"));

        click(Locator.linkWithText("Validate"));

        waitForElement(Locator.id("StatusFiles"));
        if (isElementPresent(Locator.linkWithText("RUNNING")))
            click(Locator.linkWithText("RUNNING"));
        else
            click(Locator.linkWithText("COMPLETE"));

        waitForText("Check complete");
        assertTextPresent("Check complete, 0 errors found");
    }

    @LogMethod
    private void databaseCheckTest()
    {
        goToAdmin();
        clickAndWait(Locator.linkWithText("Check Database"));
        clickAndWait(Locator.linkWithText("Do Database Check"));
        waitForText("Database Consistency checker complete", 60000);
        assertTextNotPresent("ERROR");
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }
}
