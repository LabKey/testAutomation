/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
import org.labkey.test.Locators;
import org.labkey.test.TestProperties;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.html.SiteNavBar;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@Category({DailyB.class})
public class ProjectSettingsTest extends BaseWebDriverTest
{
    private static final Locator helpMenuLinkDev =  Locator.tagWithText("span", "Help (default)");
    private static final Locator helpMenuLinkProduction =  Locator.tagWithText("span", "Help");
    private static final String INJECT_CHARS = "<script>alert(\"8(\");</script>";
    private static final String DATE_TIME_FORMAT_INJECTION = "yyyy-MM-dd HH:mm'" + INJECT_CHARS + "'";
    private Locator helpMenuLink = TestProperties.isDevModeEnabled() ? helpMenuLinkDev : helpMenuLinkProduction;

    @Override
    //this project will remain unaltered and copy every property from the site.
    protected String getProjectName()
    {
        return "Copycat Project";
    }

    protected void goToSiteLookAndFeel()
    {
        goToAdminConsole().goToAdminConsoleLinksSection().clickLookAndFeelSettings();
    }

    //this project's properties will be altered and so should not copy site properties
    protected String getProjectAlteredName()
    {
        return "Independent Project";
    }

    protected void checkHelpLinks(String projectName, boolean supportLinkPresent, boolean helpLinkPresent)
    {
        if(projectName!=null)
            clickProject(projectName);

        BootstrapMenu menu = new SiteNavBar(getDriver()).userMenu();
        menu.expand();    // the support and help links are now on the user menu
        List<WebElement> visibleLinks = menu.findVisibleMenuItems();
        assertEquals("Support link state unexpected.", supportLinkPresent, visibleLinks.stream().anyMatch((a)-> a.getText().equals("Support")));
        assertEquals("Help link state unexpected.", helpLinkPresent, visibleLinks.stream().anyMatch((a)-> a.getText().equals("LabKey Documentation")));
    }

    @BeforeClass
    public static void setupProject()
    {
        ProjectSettingsTest init = (ProjectSettingsTest)getCurrentTest();
        init.setUpTest();
    }

    protected void setUpTest()
    {
        _containerHelper.createProject(getProjectName(), null);
        checkHelpLinks(null, true, true);
        _containerHelper.createProject(getProjectAlteredName(), null);
        checkHelpLinks(null, true, true);

        goToProjectSettings(getProjectAlteredName());
        setFormElement(Locator.name("reportAProblemPath"), "");
        clickButton("Save");
        assertElementNotPresent(Locators.labkeyError);

        checkHelpLinks(getProjectAlteredName(), false, true);
//        assertElementNotPresent("Support link still present after removing link from settings", supportLink);
    }

    @Test
    public void testSteps()
    {
        //assert both locators are present in clone project
        goToProjectHome();
        checkHelpLinks(null, true, true);

        //change global settings to exclude help link
        goToSiteLookAndFeel();
        click(Locator.checkboxByName("enableHelpMenu"));
        clickButtonContainingText("Save");


        //assert help link missing in proj 1, present in proj 2
        checkHelpLinks(getProjectName(), true, false);
        checkHelpLinks(getProjectAlteredName(), false, true);

        //change proj 2 to exclude both help and support
        goToProjectSettings(getProjectAlteredName());
        uncheckCheckbox(Locator.checkboxByName("enableHelpMenu"));
        clickButtonContainingText("Save");

        //assert help link itself gone
        assertElementNotPresent(helpMenuLink);
    }

    @Test
    public void testInjection()
    {
        goToProjectHome();
        goToProjectSettings();
        setFormElement(Locator.name("defaultDateTimeFormat"), DATE_TIME_FORMAT_INJECTION);
        clickButtonContainingText("Save");
        _listHelper.createList(getProjectName(), "IceCream", ListHelper.ListColumnType.AutoInteger, "IceCreamID",
                new ListHelper.ListColumn("IceCreamDate", "", ListHelper.ListColumnType.DateTime, ""));
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

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        goToSiteLookAndFeel();
        checkCheckbox(Locator.checkboxByName("enableHelpMenu"));
        clickButtonContainingText("Save");

        _containerHelper.deleteProject(getProjectName(), afterTest);
        _containerHelper.deleteProject(getProjectAlteredName(), afterTest);
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
