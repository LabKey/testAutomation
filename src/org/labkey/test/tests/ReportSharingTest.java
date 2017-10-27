package org.labkey.test.tests;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.InDevelopment;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;

import java.util.Arrays;
import java.util.List;

@Category({InDevelopment.class})
public class ReportSharingTest extends BaseWebDriverTest
{
    private static final String USER_NON_EDITOR = "labkey_non_editor@reportsharing.test";
    private static final String USER_EDITOR = "labkey_user@reportsharing.test";
    private static final String USER_DEV = "labkey_dev@reportsharing.test";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(true,USER_DEV,USER_EDITOR,USER_NON_EDITOR);
        _containerHelper.deleteProject(getProjectName(),afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        ReportSharingTest init = (ReportSharingTest) getCurrentTest();

        init.doSetup();

    }

    private void doSetup()
    {

        _containerHelper.createProject(getProjectName(), null);

        _userHelper.createUser(USER_EDITOR);
        _userHelper.createUser(USER_DEV);
        _userHelper.createUser(USER_NON_EDITOR);

        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addUserToProjGroup(USER_EDITOR, getProjectName(), "Users");
        apiPermissionsHelper.setPermissions("Users", "Editor");
        apiPermissionsHelper.setUserPermissions(USER_EDITOR,"Reader");
        apiPermissionsHelper.setUserPermissions(USER_NON_EDITOR,"Submitter");
        apiPermissionsHelper.addUserToSiteGroup(USER_DEV, "Site Administrators");

    }

    @Before
    public void preTest() throws Exception
    {
        goToProjectHome();

    }

    @Test
    public void testSharingWithDevelopers() throws Exception
    {
        // go to core.Containers view
        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("core","Containers");

        //create R report
        table.goToReport("Create R Report");
        findButton("Save").click();
        _extHelper.waitForExtDialog("Save Report", WAIT_FOR_JAVASCRIPT);

        final Window saveWindow = Window.Window(getDriver()).withTitle("Save Report").waitFor();
        String REPORT_NAME = "shared report";
        setFormElement(Locator.xpath("//input[contains(@class, 'x4-form-field')]").findElement(saveWindow), REPORT_NAME);
        saveWindow.clickButton("OK");

        // open new report from view
        goToSchemaBrowser();
        table = viewQueryData("core","Containers");
        table.goToReport(REPORT_NAME);

        // click share button, add recipient to list and submit
        click(Locator.byClass("fa-share"));
        setFormElement(Locator.textarea("recipientList"),USER_DEV);
        clickAndWait(Locator.linkContainingText("Submit"));

        // confirm recipient has been added as a specific user
        assertChecked(Locator.tagWithText("td",USER_DEV).followingSibling("td").childTag("input"));
        clickAndWait(Locator.linkContainingText("Save"));

        //confirm recipient can Save As
        impersonate(USER_DEV);
        goToProjectHome();
        goToSchemaBrowser();
        table = viewQueryData("core","Containers");
        table.goToReport(REPORT_NAME);
        click(Locator.linkContainingText("Source"));
        scrollIntoView(Locator.linkContainingText("Save As"));
        click(Locator.linkContainingText("Save As"));
        _extHelper.waitForExtDialog("Save Report As", WAIT_FOR_JAVASCRIPT);
        final Window saveAsWindow = Window.Window(getDriver()).withTitle("Save Report As").waitFor();
        String REPORT_NAME_SAVED_AS = "shared report saved";
        setFormElement(Locator.xpath("//input[contains(@class, 'x4-form-field')]").findElement(saveAsWindow), REPORT_NAME_SAVED_AS);
        saveAsWindow.clickButton("OK");

        //confirm recipient can share report saved from shared
        goToSchemaBrowser();
        table = viewQueryData("core","Containers");
        table.goToReport(REPORT_NAME_SAVED_AS);
        click(Locator.byClass("fa-share"));
        setFormElement(Locator.textarea("recipientList"),USER_EDITOR);
        clickAndWait(Locator.linkContainingText("Submit"));

        //confirm recipient is included in Users list and checked
        assertChecked(Locator.tagWithText("td",USER_EDITOR).followingSibling("td").childTag("input"));
    }

    @Test
    public void testSharingWithNonEditor() throws Exception
    {
        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("core","Containers");
        table.goToReport("Create R Report");
        findButton("Save").click();
        _extHelper.waitForExtDialog("Save Report", WAIT_FOR_JAVASCRIPT);

        final Window saveWindow = Window.Window(getDriver()).withTitle("Save Report").waitFor();
        String REPORT_NAME_FOR_FAIL_SHARE = "shared report fail";
        setFormElement(Locator.xpath("//input[contains(@class, 'x4-form-field')]").findElement(saveWindow), REPORT_NAME_FOR_FAIL_SHARE);
        saveWindow.clickButton("OK");

        goToSchemaBrowser();
        table = viewQueryData("core","Containers");
        table.goToReport(REPORT_NAME_FOR_FAIL_SHARE);
        click(Locator.byClass("fa-share"));
        setFormElement(Locator.textarea("recipientList"),USER_NON_EDITOR);
        clickAndWait(Locator.linkContainingText("Submit"));
        assertTextPresent("User does not have permissions to this container: labkey_non_editor@reportsharing.test");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "ReportSharingTest Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }

}