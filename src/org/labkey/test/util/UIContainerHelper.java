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
package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.components.html.ProjectMenu;

import static org.junit.Assert.*;

public class UIContainerHelper extends AbstractContainerHelper
{
    public UIContainerHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    @Override
    @LogMethod
    public void createSubfolder(String parentPath, String child, String foldertype)
    {
        String[] ancestors = parentPath.split("/");
        createSubfolder(ancestors[0], ancestors[ancestors.length - 1], child, foldertype, null);
    }

    @Override
    @LogMethod
    protected void doCreateProject(String projectName, String folderType)
    {
        _test.log("Creating project with name " + projectName);
        _test.ensureAdminMode();
        if (projectLinkExists(projectName))
            fail("Cannot create project; A link with text " + projectName + " already exists.  " +
                    "This project may already exist, or its name appears elsewhere in the UI.");
        _test.goToCreateProject();
        _test.waitForElement(Locator.name("name"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.setFormElement(Locator.name("name"), projectName);

        if (null != folderType && !folderType.equals("None"))
            _test.click(Locator.xpath("//td[./label[text()='"+folderType+"']]/input"));
        else
            _test.click(Locator.xpath("//td[./label[text()='Custom']]/input"));

        _test.waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Next"));

        //second page of the wizard
        _test.waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Next"));

        //third page of wizard
        if (_test.isElementPresent(Ext4Helper.Locators.ext4Button("Finish")))
        {
            _test.waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Finish"));
        }
        else
        {
            // There may be additional steps based on
            _test.waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Next"));
        }

        // block until the project exists
        long startTime = System.currentTimeMillis();
        _test.log("Wait extra long for folder to finish deleting.");
        while (!projectLinkExists(projectName) && System.currentTimeMillis() - startTime < LabKeySiteWrapper.WAIT_FOR_JAVASCRIPT)
        {
            _test.sleep(5000);
            _test.refresh();
        }
    }

    @Override //TODO :  this will be necessary for full interconversion between UIcontainer and APIContainer,
    //but at the moment it's unnecessary, and complicated because the two don't have the same capabilities.
    protected void doCreateFolder(String projectName, String path, String folderType)
    {
        throw new UnsupportedOperationException("Use APIContainerHelper to create a sub-folder.");
    }

    @LogMethod
    @Override
    protected void doDeleteProject(String project, boolean failIfNotFound, int wait)
    {
        _test.openProjectMenu();

        if (!projectLinkExists(project))
        {
            if (failIfNotFound)
            {
                fail("Project \""+ project + "\" not found");
            }
            else
            {
                _test.log("No need to delete: project \""+ project + "\" not found");
                _test.goToHome();
                return;
            }
        }
        _test.clickProject(project);
        _test.goToFolderManagement();
        _test.waitForElement(Ext4Helper.Locators.folderManagementTreeSelectedNode(project));

        _test.clickButton("Delete");

        // in case there are sub-folders
        if (_test.isButtonPresent("Delete All Folders"))
        {
            _test.clickButton("Delete All Folders");
        }

        long startTime = System.currentTimeMillis();
        // confirm delete:
        _test.log("Starting delete of project '" + project + "'...");
        _test.clickButton("Delete", wait);

        if (projectLinkExists( project))
        {
            _test.log("Wait extra long for folder to finish deleting.");
            while (projectLinkExists(project) && System.currentTimeMillis() - startTime < wait)
            {
                _test.sleep(5000);
                _test.refresh();
            }
        }

        if (!projectLinkExists(project))
            _test.log(project + " deleted in " + (System.currentTimeMillis() - startTime) + "ms");
        else
            fail(project + " not finished deleting after " + (System.currentTimeMillis() - startTime) + " ms");

        // verify that we're not on an error page with a check for a project link:
        _test.openProjectMenu();
       assertFalse(projectLinkExists(project));
    }

    private boolean projectLinkExists(String project) // use the presence of start-menu links to know
    {
        if (LabKeySiteWrapper.IS_BOOTSTRAP_LAYOUT)
        {
            return new ProjectMenu(_test.getDriver()).projectLinkExists(project);
        }
        else
        {
            return _test.isElementPresent(Locator.linkWithText(project));
        }
    }
}
