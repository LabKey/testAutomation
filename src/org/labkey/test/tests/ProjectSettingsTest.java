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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestProperties;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Category({DailyB.class})
public class ProjectSettingsTest extends BaseWebDriverTest
{
    private static final Locator supportLink = Locator.xpath("//a[contains(@href, 'support')]/span[text()='Support']");
    private static final Locator helpLink = Locator.xpath("//a[@target='labkeyHelp']/span[contains(text(), 'LabKey Documentation')]");
    private static final Locator helpMenuLinkDev =  Locator.tagWithText("span", "Help (default)");
    private static final Locator helpMenuLinkProduction =  Locator.tagWithText("span", "Help");
    private Locator helpMenuLink = TestProperties.isDevModeEnabled() ? helpMenuLinkDev : helpMenuLinkProduction;

    @Override
    //this project will remain unaltered and copy every property from the site.
    protected String getProjectName()
    {
        return "Copycat Project";
    }

    protected void goToSiteLookAndFeel()
    {
        goToAdmin();
        clickAndWait(Locator.linkContainingText("look and feel settings"));
    }

    //this project's properties will be altered and so should not copy site properties
    protected String getProjectAltertedName()
    {
        return "Independent Project";
    }

    protected void checkHelpLinks(String projectName, boolean supportLinkPresent, boolean helpLinkPresent)
    {
        if(projectName!=null)
            clickProject(projectName);
        click(helpMenuLink);
        assertEquals("Support link state unexpected.", supportLinkPresent, isElementPresent(supportLink));
        assertEquals("Help link state unexpected.", helpLinkPresent, isElementPresent(helpLink));
    }

    protected void setUpTest()
    {

        _containerHelper.createProject(getProjectName(), null);
        checkHelpLinks(null, true, true);
        _containerHelper.createProject(getProjectAltertedName(), null);
        checkHelpLinks(null, true, true);

        goToProjectSettings(getProjectAltertedName());
        setFormElement(Locator.name("reportAProblemPath"), "");
        clickButton("Save");
        assertElementNotPresent(Locators.labkeyError);

        checkHelpLinks(getProjectAltertedName(), false, true);
//        assertElementNotPresent("Support link still present after removing link from settings", supportLink);
    }

    @Test
    public void testSteps()
    {
        setUpTest();

        //assert both locators are present in clone project
        goToProjectHome();
        click(helpMenuLink);
        checkHelpLinks(null, true, true);

        //change global settings to exclude help link
        goToSiteLookAndFeel();
        click(Locator.checkboxByName("enableHelpMenu"));
        clickButtonContainingText("Save");


        //assert help link missing in proj 1, present in proj 2
        checkHelpLinks(getProjectName(), true, false);
        checkHelpLinks(getProjectAltertedName(), false, true);

        //change proj 2 to exclude both help and support
        goToProjectSettings(getProjectAltertedName());
        uncheckCheckbox(Locator.checkboxByName("enableHelpMenu"));
        clickButtonContainingText("Save");

        //assert help link itself gone
        assertElementNotPresent(helpMenuLink);
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        goToSiteLookAndFeel();
        checkCheckbox(Locator.checkboxByName("enableHelpMenu"));
        clickButtonContainingText("Save");

        deleteProject(getProjectName(), afterTest);
        deleteProject(getProjectAltertedName(), afterTest);
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
