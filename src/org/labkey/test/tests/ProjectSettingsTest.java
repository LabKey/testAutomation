/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

/**
 * Created by IntelliJ IDEA.
 * User: elvan
 * Date: 3/14/12
 * Time: 12:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProjectSettingsTest extends BaseSeleniumWebTest
{
    Locator supportLink = Locator.xpath("//a[contains(@href, 'support')]/span[text()='Support']");
    Locator helpLink = Locator.xpath("//a[@target='labkeyHelp']/span[contains(text(), 'LabKey Help')]");
    Locator helpMenuLink =  Locator.tagWithText("span", "Help (default)");

    @Override
    //this project will remain unaltered and copy every property from the site.
    protected String getProjectName()
    {
        return "Copycat Project";  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected void goToSiteLookAndFeel()
    {
        goToAdmin();
        clickLinkContainingText("look and feel settings");
    }

    //this project's properties will be altered and so should not copy site properties
    protected String getProjectAltertedName()
    {
        return "Independent Project";
    }

    protected void checkHelpLinks(String projectName, boolean supportLinkPresent, boolean helpLinkPresent)
    {
        if(projectName!=null)
            clickLinkWithText(projectName);
        clickAndWait(helpMenuLink, 0);
        assertEquals("Support link state unexpected.", supportLinkPresent, isElementPresent(supportLink));
        assertEquals("Help link state unexpected.", helpLinkPresent, isElementPresent(helpLink));
    }

    protected void setUpTest()
    {

        createProject(getProjectName());
        checkHelpLinks(null, true, true);
        createProject(getProjectAltertedName());
        checkHelpLinks(null, true, true);

        goToProjectSettings(getProjectAltertedName());
        setFormElement("reportAProblemPath", "");
        clickButton("Save");

        checkHelpLinks(getProjectAltertedName(), false, true);
//        assertElementNotPresent("Support link still present after removing link from settings", supportLink);
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setUpTest();

        //assert both locators are present in clone project
        goToProjectHome();
        clickAndWait(Locator.tagWithText("span", "Help (default)"), 0);
        checkHelpLinks(null, true, true);

        //change global settings to exclude help link
        goToSiteLookAndFeel();
        clickCheckbox("enableHelpMenu");
        clickButtonContainingText("Save");


        //assert help link missing in proj 1, present in proj 2
        checkHelpLinks(getProjectName(), true, false);
        checkHelpLinks(getProjectAltertedName(), false, true);

        //change proj 2 to exclude both help and suport
        goToProjectSettings(getProjectAltertedName());
        uncheckCheckbox("enableHelpMenu");
        clickButtonContainingText("Save");

        //assert help link itself gone
        assertElementNotPresent(helpMenuLink);
    }

    @Override
    protected void doCleanup() throws Exception
    {
        goToSiteLookAndFeel();
        checkCheckbox("enableHelpMenu");
        clickButtonContainingText("Save");

        deleteProject(getProjectName());
        deleteProject(getProjectAltertedName(), false);
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
