/*
 * Copyright (c) 2013-2016 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.pages.core.admin.CustomizeSitePage;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PermissionsHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Category({DailyA.class})
public class AdminConsoleTest extends BaseWebDriverTest
{
    protected static final String APP_ADMIN_USER = "app_admin_test_user@adminconsole.test";
    protected static final String APP_ADMIN_USER_PASS = PasswordUtil.getPassword();

    public String getProjectName()
    {
        return null;
    }

    @Test
    public void testRibbonBar()
    {
        goToAdminConsole();
        waitAndClick(Locator.linkContainingText("site settings"));
        waitForElement(Locator.name("showRibbonMessage"));

        WebElement el = Locator.name("ribbonMessageHtml").findElement(getDriver());
        el.clear();

        WebElement checkbox = Locator.checkboxByName("showRibbonMessage").findElement(getDriver());

        //only select if not already checked
        if (!("true".equals(checkbox.getAttribute("checked"))))
            click(Locator.checkboxByName("showRibbonMessage"));

        clickButton("Save");

        waitForElement(Locator.xpath("//div[contains(text(), 'Cannot enable the ribbon message without providing a message to show')]"));

        String linkText = "and also click this...";
        String html = "READ ME!!!  <a href='<%=contextPath%>/project/home/begin.view'>" + linkText + "</a>";

        //only check if not already checked
        checkbox = Locator.checkboxByName("showRibbonMessage").findElement(getDriver());
        if (!("true".equals(checkbox.getAttribute("checked"))))
            click(Locator.checkboxByName("showRibbonMessage"));

        setFormElement(Locator.name("ribbonMessageHtml"), html);
        clickButton("Save");

        waitForElement(Locator.linkContainingText("site settings"));

        Locator ribbon = Locator.xpath("//div[contains(@class, 'labkey-warning-messages')]//li[contains(text(), 'READ ME!!!')]");
        Locator ribbonLink = Locator.xpath("//div[contains(@class, 'labkey-warning-messages')]//li[contains(text(), 'READ ME!!!')]//..//a");

        assertElementPresent(ribbon);
        assertElementPresent(ribbonLink);

        el = getDriver().findElement(By.xpath("//div[contains(@class, 'labkey-warning-messages')]//..//a"));
        assertNotNull("Link not present in ribbon bar", el);

        String href = el.getAttribute("href");
        String expected = WebTestHelper.getBaseURL() + "/project/home/begin.view";
        assertEquals("Incorrect URL", expected, href);

        goToHome();
        impersonateRole("Reader");

        assertElementPresent(ribbon);
        assertElementPresent(ribbonLink);

        stopImpersonatingRole();

        goToAdminConsole();
        waitAndClick(Locator.linkContainingText("site settings"));
        waitForElement(Locator.name("showRibbonMessage"));
        click(Locator.checkboxByName("showRibbonMessage"));
        clickButton("Save");
        waitForElement(Locator.linkContainingText("site settings"));
        assertElementNotPresent(ribbon);
        assertElementNotPresent(ribbonLink);
    }

    @Test
    public void testAppAdminRole()
    {
       // log out as siteAdmin, log in as appAdmin
        signOut();
        signIn(APP_ADMIN_USER, APP_ADMIN_USER_PASS);
        clickButton("Submit");

        ShowAdminPage adminPage = goToAdminConsole();
        // verify expected UI present or absent

        //analytics settings
        URL url = getURL(); // will capture with redirect url
        clickAndWait(Locator.linkWithText("analytics settings"));
        clickButton("done");
        assertTrue("expect to return to admin console" ,url.toString().startsWith(getURL().toString()));
        url = getURL(); // will capture without redirect url

        //authentication
        clickAndWait(Locator.linkWithText("authentication"));
        assertNull("expect 'enable' links to be disabled for appAdmin", Locator.linkWithText("enable").findElementOrNull(getDriver()));
        assertNull("expect 'configure' links to be disabled for appAdmin", Locator.linkWithText("configure").findElementOrNull(getDriver()));
        clickAndWait(Locator.tagWithClass("a", "labkey-button").withChild(Locator.tagWithText("span", "Done")));
        assertEquals("expect to return to admin console" ,url, getURL());

        //change user properties
        assertNotNull(Locator.linkWithText("change user properties").findElementOrNull(getDriver()));

        //email customization
        clickAndWait(Locator.linkWithText("email customization"), WAIT_FOR_PAGE);
        clickAndWait(Locator.xpath("//a[@class='labkey-button' and ./span[contains(text(), 'Cancel')]]"));
        assertEquals(url, getURL());

        //folder types
        assertNotNull(Locator.linkWithText("folder types").findElementOrNull(getDriver()));

        //look and feel settings
        assertNotNull(Locator.linkWithText("look and feel settings").findElementOrNull(getDriver()));

        //missing value indicators
        assertNotNull(Locator.linkWithText("missing value indicators").findElementOrNull(getDriver()));

        //profiler
        assertNotNull(Locator.linkWithText("profiler").findElementOrNull(getDriver()));

        //project display order
        assertNotNull(Locator.linkWithText("project display order").findElementOrNull(getDriver()));

        //short urls
        assertNotNull(Locator.linkWithText("short urls").findElementOrNull(getDriver()));

        //site settings
        CustomizeSitePage customizeSitePage = adminPage.clickSiteSettings();
        Locator.xpath("//a[@class='labkey-button' and ./span[contains(text(),'Done')]]").findElement(getDriver()).click();

        //system maintenance
        clickAndWait(Locator.linkWithText("system maintenance"), WAIT_FOR_PAGE);
        Locator.xpath("//a[@class='labkey-button' and ./span[contains(text(),'Done')]]").findElement(getDriver()).click();

        // views and scripting
        clickAndWait(Locator.linkWithText("system maintenance"), WAIT_FOR_PAGE);
        assertNull(Locator.buttonContainingText("Edit").findElementOrNull(getDriver()));
        goBack();
        assertEquals(url, getURL());

        //audit log
        assertNotNull(Locator.linkWithText("audit log").findElementOrNull(getDriver()));

        //etl-all job histories
        assertNotNull(Locator.linkWithText("etl- all job histories").findElementOrNull(getDriver()));

        //etl run site scope etls
        assertNotNull(Locator.linkWithText("etl- run site scope etls").findElementOrNull(getDriver()));

        //full-text search
        assertNotNull(Locator.linkWithText("full-text search").findElementOrNull(getDriver()));

        //ms1
        assertNotNull(Locator.linkWithText("ms1").findElementOrNull(getDriver()));

        //pipeline
        assertNotNull(Locator.linkWithText("pipeline").findElementOrNull(getDriver()));

        //site-wide terms of use
        assertNotNull(Locator.linkWithText("site-wide terms of use").findElementOrNull(getDriver()));

        //actions
        assertNotNull(Locator.linkWithText("actions").findElementOrNull(getDriver()));

        //caches
        assertNotNull(Locator.linkWithText("caches").findElementOrNull(getDriver()));

        //credits
        assertNotNull(Locator.linkWithText("credits").findElementOrNull(getDriver()));

        //data sources
        assertNotNull(Locator.linkWithText("data sources").findElementOrNull(getDriver()));

        //dump heap
        assertNotNull(Locator.linkWithText("dump heap").findElementOrNull(getDriver()));

        //environment variables
        assertNotNull(Locator.linkWithText("environment variables").findElementOrNull(getDriver()));

        //memory usage
        assertNotNull(Locator.linkWithText("memory usage").findElementOrNull(getDriver()));

        //queries
        assertNotNull(Locator.linkWithText("queries").findElementOrNull(getDriver()));

        //reset site errors
        assertNotNull(Locator.linkWithText("reset site errors").findElementOrNull(getDriver()));

        //running threads
        assertNotNull(Locator.linkWithText("running threads").findElementOrNull(getDriver()));

        //site validation
        assertNotNull(Locator.linkWithText("site validation").findElementOrNull(getDriver()));

        //system properties
        assertNotNull(Locator.linkWithText("system properties").findElementOrNull(getDriver()));

        //test email configuration
        assertNotNull(Locator.linkWithText("test email configuration").findElementOrNull(getDriver()));

        //view all site errors
        assertNotNull(Locator.linkWithText("view all site errors").findElementOrNull(getDriver()));

        //view all site errors since reset
        assertNotNull(Locator.linkWithText("view all site errors since reset").findElementOrNull(getDriver()));

        //view primary site log file
        assertNotNull(Locator.linkWithText("view primary site log file").findElementOrNull(getDriver()));

        // log out as appAdmin
        signOut();
        // log in as siteAdmin again
        signIn();
    }

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
        removeTestUser();
    }

    private void createTestUser()
    {
        _userHelper.createUser(APP_ADMIN_USER, true, false);
        setInitialPassword(APP_ADMIN_USER, APP_ADMIN_USER_PASS);

        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addMemberToRole(APP_ADMIN_USER, "Application Admin", PermissionsHelper.MemberType.user, "/");
    }

    private void removeTestUser()
    {
        _userHelper.deleteUsers(false, APP_ADMIN_USER);
    }
}
