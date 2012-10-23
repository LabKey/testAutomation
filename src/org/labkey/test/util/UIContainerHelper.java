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
    @LogMethod
    public void createSubfolder(String project, String child, String foldertype)
    {
        _test.createSubfolder(project, project, child, foldertype, null);
    }

    @Override
    @LogMethod
    protected void doCreateProject(String projectName, String folderType)
    {
        _test.log("Creating project with name " + projectName);
        _test.ensureAdminMode();
        if (_test.isLinkPresentWithText(projectName))
            Assert.fail("Cannot create project; A link with text " + projectName + " already exists.  " +
                    "This project may already exist, or its name appears elsewhere in the UI.");
        _test.goToCreateProject();
        _test.waitForElement(Locator.name("name"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.setFormElement(Locator.name("name"), projectName);

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

    @Override //TODO :  this will be necessary for full interconversion between UIcontainer and APIContainer,
    //but at the moment it's unnecessary, and complicated because the two don't have the same capabilities.
    protected void doCreateFolder(String projectName, String folderType, String path)
    {
//        _test.createSubfolder();
    }

    @LogMethod
    @Override
    public void deleteProject(String project, boolean failIfNotFound, int wait)
    {
            if(!_test.isTextPresent(project))
                _test.goToHome();

            _test.click(Locator.id("expandCollapse-projectsMenu"));
            if(!_test.isLinkPresentWithText(project))
                if(failIfNotFound)
                    Assert.fail("Project "+ project + " not found");
                else
                    return;

            _test.clickLinkWithText(project);
            //Delete even if terms of use is required
            if (_test.isElementPresent(Locator.name("approvedTermsOfUse")))
            {
                _test.clickCheckbox("approvedTermsOfUse");
                _test.clickButton("Agree");
            }
            _test.ensureAdminMode();
            _test.goToFolderManagement();
            _test.waitForExt4FolderTreeNode(project, 10000);
            _test.clickButton("Delete");
            // in case there are sub-folders
            if (_test.isNavButtonPresent("Delete All Folders"))
            {
                _test.clickButton("Delete All Folders");
            }
            long startTime = System.currentTimeMillis();
            // confirm delete:
            _test.log("Starting delete of project '" + project + "'...");
            _test.clickButton("Delete", _test.longWaitForPage);

            if(_test.isLinkPresentWithText(project))
            {
                _test.log("Wait extra long for folder to finish deleting.");
                while (_test.isLinkPresentWithText(project) && System.currentTimeMillis() - startTime < wait)
                {
                    _test.sleep(5000);
                    _test.refresh();
                }
            }
            if (!_test.isLinkPresentWithText(project))
                _test.log(project + " deleted in " + (System.currentTimeMillis() - startTime) + "ms");
            else Assert.fail(project + " not finished deleting after " + (System.currentTimeMillis() - startTime) + " ms");

            // verify that we're not on an error page with a check for a project link:
            _test.assertLinkNotPresentWithText(project);
    }
}
