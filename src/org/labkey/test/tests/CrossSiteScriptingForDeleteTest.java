package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.pages.reports.ManageViewsPage;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Collections;
import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class CrossSiteScriptingForDeleteTest extends BaseWebDriverTest
{
    protected static final String PROJECT_NAME = "CrossSiteScriptingForDeleteTest";
    protected static final String REPORT_NAME = BaseWebDriverTest.INJECT_CHARS_1;
    private static final String LINK_REPORT_URL = "/home/project-begin.view";

    @BeforeClass
    public static void initTest()
    {
        CrossSiteScriptingForDeleteTest init = (CrossSiteScriptingForDeleteTest) getCurrentTest();
        init.doSetup();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.singletonList("core");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    private void doSetup()
    {
        _containerHelper.createProject(PROJECT_NAME, null);

    }

    @Test
    public void verifyReportNameIsEncoded()
    {
        goToProjectHome();
        log("Adding the link report");
        goToManageViews().clickAddReport("Link Report");
        setFormElement(Locator.name("viewName"), REPORT_NAME);
        setFormElement(Locator.name("linkUrl"), WebTestHelper.getContextPath() + LINK_REPORT_URL);
        waitFor(()->getFormElement(Locator.name("linkUrl")).equals(WebTestHelper.getContextPath() + LINK_REPORT_URL),
                "link URL not set.", 500);
        waitAndClickAndWait(Locator.linkWithText("Save"));
        waitForText("Manage Views");

        log("Clicking on the report - No XSS");
        goToManageViews().selectReport(REPORT_NAME);
        switchToMainWindow();
        waitAndClick(Locator.linkContainingText("Delete Selected"));
        String deleteMsg = "Are you sure you want to delete the following?\n" +
                "\n  " + REPORT_NAME;

        log("Verifying the error message");
        confirmReportNameInMessage("Delete", deleteMsg, false);

        ManageViewsPage manageViewsPage = goToManageViews();
        manageViewsPage.editReport(REPORT_NAME);
        Window.Window(getDriver()).withTitle(REPORT_NAME)
                .waitFor()
                .clickButton("Delete", false);
        deleteMsg = "Are you sure you want to delete \"" + REPORT_NAME + "\"";

        log("Verifying the error message");
        confirmReportNameInMessage("Delete?", deleteMsg, true);

    }

    public void confirmReportNameInMessage(String windowTitle, String msg, boolean action)
    {
        Window window = new Window(windowTitle, getDriver());
        checker().verifyEquals("Invalid delete error message", msg, window.getBody());
        if (action)
            window.clickButton("Yes", true);
        else
            window.clickButton("Cancel", true);
    }

}
