package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.reports.ManageViewsPage;

import java.util.Collections;
import java.util.List;

public class CrossSiteScriptingForDeleteTest extends BaseWebDriverTest
{
    protected static final String PROJECT_NAME = "CrossSiteScriptingForDeleteTest";
    protected static final String REPORT_NAME = "<span onclick='alert(\"fooled you!\")'>mouseover me, it's totally safe</span>";
    private static final String LINK_REPORT_URL = "/project/home/begin.view";

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
        goToManageViews();

        log("Adding the link report");
        new BootstrapMenu.BootstrapMenuFinder(getDriver()).withButtonTextContaining("Add Report").find()
                .clickSubMenu(true, "Link Report");

        setFormElement(Locator.name("viewName"), REPORT_NAME);
        setFormElement(Locator.name("linkUrl"), WebTestHelper.getContextPath() + LINK_REPORT_URL);
        clickButton("Save");
        waitForText("Manage Views");

        log("Clicking on the report - No XSS");
        click(Locator.linkWithText(REPORT_NAME));
        switchToMainWindow();
        waitAndClick(Locator.linkContainingText("Delete Selected"));
        String deleteMsg = "Are you sure you want to delete the following?\n" +
                "\n" +
                "  <span onclick='alert(\"fooled you!\")'>mouseover me, it's totally safe</span>";

        log("Verifying the error message");
        confirmReportNameInMessage("Delete", deleteMsg, false);

        ManageViewsPage manageViewsPage = goToManageViews();
        manageViewsPage.editReport(REPORT_NAME);
        Window.Window(getDriver()).withTitle(REPORT_NAME)
                .waitFor()
                .clickButton("Delete", false);
        deleteMsg = "Are you sure you want to delete \"<span onclick='alert(\"fooled you!\")'>mouseover me, it's totally safe</span>\"";

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
