/*
 * Copyright (c) 2013-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimpleGetCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.core.login.LoginConfigRow;
import org.labkey.test.pages.core.login.LoginConfigurePage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class AdminConsoleTest extends BaseWebDriverTest
{
    protected static final String APP_ADMIN_USER = "app_admin_test_user@adminconsole.test";

    @Override
    public String getProjectName()
    {
        return null;
    }

    @Test
    public void testServerHttpHeaderSetting()
    {
        goToAdminConsole().clickSiteSettings();
        waitForElement(Locator.name("includeServerHttpHeader"));
        WebElement checkbox = Locator.checkboxByName("includeServerHttpHeader").findElement(getDriver());

        boolean originalValue = "true".equals(checkbox.getAttribute("checked"));

        // Try with the setting on
        if (!originalValue)
            click(Locator.checkboxByName("includeServerHttpHeader"));
        clickButton("Save");

        String serverHeader = getServerHeader();
        assertTrue("Expected to get a Server header, but got " + serverHeader, serverHeader != null && serverHeader.startsWith("LabKey/"));

        // Try with the setting off
        goToAdminConsole().clickSiteSettings();
        waitForElement(Locator.name("includeServerHttpHeader"));
        click(Locator.checkboxByName("includeServerHttpHeader"));
        clickButton("Save");

        serverHeader = getServerHeader();
        assertNull("Expected to get no Server header, but got " + serverHeader, serverHeader);

        if (originalValue)
        {
            // Turn the setting back on
            goToAdminConsole().clickSiteSettings();
            waitForElement(Locator.name("includeServerHttpHeader"));
            click(Locator.checkboxByName("includeServerHttpHeader"));
            clickButton("Save");
        }
    }

    private static class GetServerHeaderCommand extends SimpleGetCommand
    {
        private String _server;
        public GetServerHeaderCommand()
        {
            super("project", "begin");
        }

        @Override
        protected Response _execute(Connection connection, String folderPath) throws CommandException, IOException
        {
            Response response = super._execute(connection, folderPath);
            _server = response.getHeaderValue("Server");
            return response;
        }
    }

    @LogMethod(quiet = true)
    private String getServerHeader()
    {
        Connection cn = createDefaultConnection();
        try
        {
            GetServerHeaderCommand command = new GetServerHeaderCommand();
            command.execute(cn, "/home");
            return command._server;
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to get Server HTTP response header", e);
        }
    }
        @Test
    public void testRibbonBar()
    {
        goToAdminConsole().clickSiteSettings();
        waitForElement(Locator.name("showRibbonMessage"));
        Locator.name("ribbonMessage").findElement(getDriver()).clear();

        WebElement checkbox = Locator.checkboxByName("showRibbonMessage").findElement(getDriver());

        //only select if not already checked
        if (!("true".equals(checkbox.getAttribute("checked"))))
            click(Locator.checkboxByName("showRibbonMessage"));

        clickButton("Save");

        waitForElement(Locator.xpath("//div[contains(text(), 'Cannot enable the ribbon message without providing a message to show')]"));

        String linkText = "and also click this...";
        String html = "READ ME!!!  <a href='<%=contextPath%>" + "/home/project-begin.view'>" + linkText + "</a>";

        //only check if not already checked
        checkbox = Locator.checkboxByName("showRibbonMessage").findElement(getDriver());
        if (!("true".equals(checkbox.getAttribute("checked"))))
            click(Locator.checkboxByName("showRibbonMessage"));

        setFormElement(Locator.name("ribbonMessage"), html);
        clickButton("Save");

        Locator ribbon = Locator.tagWithClass("div", "alert alert-warning").containing("READ ME!!!");
        waitForElement(ribbon);

        Locator ribbonLink = Locator.tagWithClassContaining("div", "alert").append(Locator.linkContainingText("and also click this..."));
        assertElementPresent(ribbonLink);
        String href = ribbonLink.findElement(getDriver()).getAttribute("href");
        String expected = WebTestHelper.getBaseURL() + "/home/project-begin.view";
        assertEquals("Incorrect URL", expected, href);

        goToHome();
        impersonateRole("Reader");
        assertElementPresent(ribbon);
        assertElementPresent(ribbonLink);
        stopImpersonating();

        goToAdminConsole().clickSiteSettings();
        waitForElement(Locator.name("showRibbonMessage"));
        click(Locator.checkboxByName("showRibbonMessage"));
        clickButton("Save");
        assertElementNotPresent(ribbon);
        assertElementNotPresent(ribbonLink);
    }

    @Test
    public void testAppAdminRole()
    {
        Locator siteAdminLoc = Locator.pageHeader("Site Administration");
        
        // log out as siteAdmin, log in as appAdmin
        signOut();
        signIn(APP_ADMIN_USER);

        // verify that all the following links are visible to AppAdmin:
        goToAdminConsole().goToSettingsSection();
        List<String> expectedLinkTexts = new ArrayList<>(Arrays.asList("change user properties",
                "experimental features",
                "deprecated features",
                "folder types",
                "look and feel settings",
                "missing value indicators",
                "profiler",
                "project display order",
                "short urls",
                "audit log",
                "full-text search",
                "pipeline",
                "site-wide terms of use",
                "actions",
                "caches",
                "credits",
                "data sources",
                "dump heap",
                "memory usage",
                "queries",
                "reset site errors",
                "running threads",
                "site validation",
                "view all site errors",
                "view all site errors since reset",
                "view primary site log file"));
        if (_containerHelper.getAllModules().contains("dataintegration"))
            expectedLinkTexts.addAll(Arrays.asList("etl - all job histories", "etl - run site scope etls"));

        expectedLinkTexts.removeIf(linkText -> isElementPresent(Locator.linkWithText(linkText)));
        assertTrue("Missing expected admin console links: " + expectedLinkTexts, expectedLinkTexts.isEmpty());

        // confirm that NONE of the following are visible to AppAdmin:
        List<String> notShownLinks = Arrays.asList(
                "files",
                "flow cytometry",
                "mascot server",
                "ldap sync admin",
                "notification service",
                "ms2",
                "check database",
                "loggers",
                "sql scripts",
                "environment variables",
                "system properties");
        for (String linkText: notShownLinks)
        {
            assertElementNotPresent(Locator.linkWithText(linkText));
        }

        //analytics settings
        goToAdminConsole().clickAnalyticsSettings();
        assertElementNotPresent(Locator.button("submit"));
        clickButton("done");
        assertElementPresent("expect to return to admin console", siteAdminLoc, 1);

        //authentication
        LoginConfigurePage configurePage = goToAdminConsole().clickAuthentication();
        List<LoginConfigRow> configRows = configurePage.getPrimaryConfigurations();
        assertFalse("expect 'edit' links not to be available for auth configs", configRows.stream().anyMatch(a-> a.canEdit()));
        assertFalse("expect 'add configuration' menu to be absent for AppAdmin", configurePage.canAddConfiguration());
        clickButton("Done");
        assertElementPresent("expect to return to admin console", siteAdminLoc, 1);

        //email customization
        goToAdminConsole().clickEmailCustomization();
        clickButton("Cancel");
        assertElementPresent("expect to return to admin console", siteAdminLoc, 1);

        //site settings
        goToAdminConsole().clickSiteSettings();
        clickButton("Done");
        assertElementPresent("expect to return to admin console", siteAdminLoc, 1);

        //system maintenance
        goToAdminConsole().clickSystemMaintenance();
        clickButton("Done");
        assertElementPresent("expect to return to admin console", siteAdminLoc, 1);

        // views and scripting
        goToAdminConsole().clickViewsAndScripting();
        assertNull(Locator.buttonContainingText("Edit").findElementOrNull(getDriver()));
        clickButton("Done");
        assertElementPresent("expect to return to admin console", siteAdminLoc, 1);
    }

    @Test
    public void testConfigureReturnURL()
    {
        String host = "google.com";
        goToAdminConsole().clickExternalRedirectHosts();

        log("Verifying host cannot be blank ");
        clickButton("Save");
        assertElementPresent(Locator.css(".labkey-error").withText("External redirect host name must not be blank."));

        log("Setting the host URL");
        setFormElement(Locator.name("newExternalRedirectHost"), host);
        clickButton("Save");

        log("Verifying url got added correctly");
        assertEquals(host, getFormElement(Locator.name("existingExternalHost1")));

        log("Verifying cannot be duplicate");
        setFormElement(Locator.name("newExternalRedirectHost"), host);
        clickButton("Save");
        assertElementPresent(Locator.css(".labkey-error").withText("'" + host + "' already exists. Duplicate hosts not allowed."));

    }

    /*
       Test coverage : Issue 46587: Add test for display of credits page
       https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=46587
    */
    @Test
    public void testAdminConsoleCredits()
    {
        goToAdminConsole().clickCredits();
        log("Verifying the page is properly loaded");
        assertTextPresent("JAR Files Distributed with the API Module");
    }



    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("admin");
    }

    @Override
    public void checkQueries()
    {

    }

    @Override
    public void checkViews()
    {

    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        AdminConsoleTest initTest = (AdminConsoleTest)getCurrentTest();
        initTest.createTestUser();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, APP_ADMIN_USER);
    }

    private void createTestUser()
    {
        _userHelper.createUser(APP_ADMIN_USER, true, false);
        setInitialPassword(APP_ADMIN_USER);

        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addMemberToRole(APP_ADMIN_USER, "Application Admin", PermissionsHelper.MemberType.user, "/");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
