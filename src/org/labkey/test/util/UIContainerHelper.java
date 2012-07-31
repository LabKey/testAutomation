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
package org.labkey.test.util;

import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

/**
 * User: jeckels
 * Date: Jul 20, 2012
 */
public class UIContainerHelper extends AbstractContainerHelper
{
    public UIContainerHelper(BaseSeleniumWebTest test)
    {
        super(test);
    }

    @Override
    protected void doCreateProject(String projectName, String folderType)
    {
        _test.log("Creating project with name " + projectName);
        _test.ensureAdminMode();
        if (_test.isLinkPresentWithText(projectName))
            Assert.fail("Cannot create project; A link with text " + projectName + " already exists.  " +
                    "This project may already exist, or its name appears elsewhere in the UI.");
        _test.goToCreateProject();
        _test.waitForElement(Locator.name("name"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.setText("name", projectName);

        if (null != folderType && !folderType.equals("None"))
            _test.click(Locator.xpath("//td[./label[text()='"+folderType+"']]/input"));
        else
            _test.click(Locator.xpath("//td[./label[text()='Custom']]/input"));

        _test.waitAndClick(Locator.xpath("//button[./span[text()='Next']]"));
        _test.waitForPageToLoad();

        //second page of the wizard
        _test.waitAndClick(Locator.xpath("//button[./span[text()='Next']]"));
        _test.waitForPageToLoad();

        //third page of wizard
        _test.waitAndClick(Locator.xpath("//button[./span[text()='Finish']]"));
        _test.waitForPageToLoad();
        
    }
}
