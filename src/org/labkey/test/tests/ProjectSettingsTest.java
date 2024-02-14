/*
 * Copyright (c) 2012-2019 LabKey Corporation
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
import org.labkey.test.categories.Daily;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.html.SiteNavBar;
import org.labkey.test.pages.core.admin.LookAndFeelSettingsPage;
import org.labkey.test.pages.core.admin.ProjectSettingsPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class ProjectSettingsTest extends BaseWebDriverTest
{
    private static final String INJECT_CHARS = "<script>alert(\"8(\");</script>";
    private static final String DATE_TIME_FORMAT_INJECTION = "yyyy-MM-dd HH:mm'" + INJECT_CHARS + "'";

    @Override
    //this project will remain unaltered and copy every property from the site.
    protected String getProjectName()
    {
        return "Site Settings Test";
    }

    protected LookAndFeelSettingsPage goToSiteLookAndFeel()
    {
        goToAdminConsole().goToSettingsSection().clickLookAndFeelSettings();
        return new LookAndFeelSettingsPage(getDriver());
    }

    protected void checkHelpLinks(String projectName, boolean supportLinkPresent, boolean helpLinkPresent)
    {
        if(projectName!=null)
            clickProject(projectName);

        BootstrapMenu menu = new SiteNavBar(getDriver()).userMenu();
        menu.expand();    // the support and help links are now on the user menu
        List<WebElement> visibleLinks = menu.findVisibleMenuItems();
        checker().verifyEquals("Support link state unexpected.",
                supportLinkPresent, visibleLinks.stream().anyMatch((a)-> a.getText().equals("Support")));
        checker().verifyEquals("Help link state unexpected.",
                helpLinkPresent, visibleLinks.stream().anyMatch((a)-> a.getText().equals("LabKey Documentation")));

        checker().screenShotIfNewError("Menu_Error");
    }

    private void resetSiteSettings()
    {
        LookAndFeelSettingsPage settingsPage = LookAndFeelSettingsPage.beginAt(this);
        settingsPage.reset();
    }

    private void resetProjectSettings()
    {
        ProjectSettingsPage settingsPage = ProjectSettingsPage.beginAt(this, getProjectName());
        settingsPage.reset();
    }

    @BeforeClass
    public static void setupProject()
    {
        ProjectSettingsTest init = (ProjectSettingsTest)getCurrentTest();
        init.setUpTest();
    }

    protected void setUpTest()
    {

        // Make sure the site settings are reset.
        resetSiteSettings();

        _containerHelper.createProject(getProjectName(), null);
        checkHelpLinks(null, true, true);
        checkHelpLinks(getProjectName(), true, true);

    }

    @Test
    public void testSiteSettingOverride()
    {
        goToProjectHome();

        log("Assert both locators are present in the project.");
        checkHelpLinks(getProjectName(), true, true);

        log("Change global settings to exclude help and report a problem link.");
        LookAndFeelSettingsPage settingsPage = goToSiteLookAndFeel();
        settingsPage.setHelpMenu(false);
        settingsPage.setSupportLink("");
        settingsPage.save();

        log("Go to the project settings page and validate that various settings match the site settings.");
        ProjectSettingsPage projectSettingsPage = ProjectSettingsPage.beginAt(this, getProjectName());

        checker().verifyFalse("Help menu should not be checked.",
                projectSettingsPage.getHelpMenu());

        checker().verifyTrue("Support link should be empty.",
                projectSettingsPage.getSupportLink().isEmpty());

        log("Validate help and report links are missing from the menu.");
        checkHelpLinks(getProjectName(), false, false);

        log("Change settings in folder/project re-enable help and report options.");
        projectSettingsPage = ProjectSettingsPage.beginAt(this, getProjectName());
        projectSettingsPage.setHelpMenu(true);
        projectSettingsPage.setSupportLink("${contextPath}/home/support/project-begin.view");
        projectSettingsPage.save();

        log("Assert help link and report link are present in the menu in the project.");
        checkHelpLinks(null, true, true);

        resetSiteSettings();
        resetProjectSettings();
    }

    @Test
    public void testInjection()
    {
        goToProjectHome();
        goToProjectSettings();
        setFormElement(Locator.name("defaultDateTimeFormat"), DATE_TIME_FORMAT_INJECTION);
        clickButtonContainingText("Save");
        _listHelper.createList(getProjectName(), "IceCream", ListHelper.ListColumnType.AutoInteger, "IceCreamID",
                new ListHelper.ListColumn("IceCreamDate", "", ListHelper.ListColumnType.DateAndTime, ""));
        goToProjectHome();
        clickAndWait(Locator.linkWithText("IceCream"));
        Map<String, String> testRow = new HashMap<>();
        String testDate = "1800-05-10 10:32";
        testRow.put("IceCreamDate", testDate);
        _listHelper.insertNewRow(testRow);
        DataRegionTable list = new DataRegionTable("query", getDriver());
        String attemptedInjection = list.getDataAsText(0, 0);
        assertEquals("Wrong list data from injection attempt", testDate + INJECT_CHARS, attemptedInjection);
    }

    @Test
    public void testTimeAndDateFields()
    {
        goToProjectHome();
        goToProjectSettings();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        goToSiteLookAndFeel();
        checkCheckbox(Locator.checkboxByName("helpMenuEnabled"));
        clickButtonContainingText("Save");

        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("core");
    }

    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
