package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;

import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class LinkedReportTest extends BaseWebDriverTest
{
    private static final String REPORT_NAME = "Labkey linked report";
    private static final String LINK_REPORT_URL = "https://www.labkey.com";

    @Override
    protected @Nullable String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @BeforeClass
    public static void setupProject()
    {
        LinkedReportTest init = (LinkedReportTest) getCurrentTest();
        init._containerHelper.createProject(init.getProjectName());
    }

    /* Regression coverage : Issue 47004: Add Test Automation for LinkedReports  */

    @Test
    public void testLinkedReportToExternalURL()
    {
        goToProjectHome();
        goToManageViews().clickAddReport("Link Report");
        setFormElement(Locator.name("viewName"), REPORT_NAME);
        setFormElement(Locator.name("linkUrl"), LINK_REPORT_URL);
        clickButton("Save");
        waitForText("Manage Views");

        waitAndClick(Locator.linkWithText(REPORT_NAME));
        switchToWindow(1);
        waitForText("Bridge the gap between data and discovery.");
        Assert.assertTrue("Linked report navigated to incorrect external link", isElementPresent(Locator.linkWithText("Get a Demo")));
    }
}
