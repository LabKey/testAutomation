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
package org.labkey.test.util;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;

import java.util.List;

import static org.junit.Assert.fail;

public class UIContainerHelper extends AbstractContainerHelper
{
    public UIContainerHelper(BaseWebDriverTest test)
    {
        super(test);
    }

    @Override
    @LogMethod
    public void doCreateFolder(String parentPath, String child, String foldertype)
    {
        String[] ancestors = parentPath.split("/");
        createSubfolder(ancestors[0], ancestors[ancestors.length - 1], child, foldertype, null);
    }

    @Override
    @LogMethod
    protected void doCreateProject(String projectName, String folderType)
    {
        doCreateProject(projectName, folderType, null);
    }

    @LogMethod
    public void createProjectFromTemplate(String projectName, String templateFolder)
    {
        doCreateProject(projectName, "Create From Template Folder", templateFolder);
    }

    @LogMethod
    private void doCreateProject(String projectName, String folderType, String templateFolder)
    {
        _test.log("Creating project with name " + projectName);
        _test.ensureAdminMode();
        _test.goToCreateProject();
        _test.waitForElement(Locator.name("name"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.setFormElement(Locator.name("name"), projectName);

        if (null != folderType && !folderType.equals("None"))
        {
            _test.click(Locator.xpath("//td[./label[text()='" + folderType + "']]/input"));
            if (folderType.equals("Create From Template Folder"))
            {
                _test._ext4Helper.waitForMaskToDisappear();
                _test._ext4Helper.selectComboBoxItem(Locator.xpath("//div")
                        .withClass("labkey-wizard-header")
                        .withText("Choose Template Folder:").append("/following-sibling::table[contains(@id, 'combobox')]"), templateFolder);
                _test._ext4Helper.checkCheckbox("Include Subfolders");
            }
        }
        else
        {
            _test.click(Locator.xpath("//td[./label[text()='Custom']]/input"));
            _test.waitForElementToBeVisible(Locator.input("defaultModule")); // wait for module-choice flyout
        }

        _test.waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Next"));

        List<String> errors = _test.getTexts(Locators.labkeyError.findElements(_test.getDriver()));
        if (!errors.isEmpty())
            fail("Unexpected error(s) during project creation: " + errors);

        //second page of the wizard
        _test.waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Next"));

        //third page of wizard
        if (_test.isElementPresent(Ext4Helper.Locators.ext4Button("Finish")))
        {
            _test.waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Finish"));
        }
        else
        {
            _test.waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Next"));

            // There may be additional steps based on the folder type. In that case return so the caller can set
            // the information for that wizard step.
            return;
        }

        // block until the project exists
        long startTime = System.currentTimeMillis();
        _test.log("Wait extra long for folder to finish deleting.");
        while (!projectLinkExists(projectName) && System.currentTimeMillis() - startTime < LabKeySiteWrapper.WAIT_FOR_JAVASCRIPT)
        {
            WebDriverWrapper.sleep(5000);
            _test.refresh();
        }
    }

    @LogMethod
    @Override
    protected void doDeleteProject(String project, boolean failIfNotFound, int wait)
    {
        _test.projectMenu().open();

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

        long startTime = System.currentTimeMillis();
        // confirm delete:
        _test.log("Starting delete of project '" + project + "'...");
        _test.clickButton("Yes, delete all", wait);

        if (projectLinkExists(project))
        {
            _test.log("Wait extra long for folder to finish deleting.");
            while (projectLinkExists(project) && System.currentTimeMillis() - startTime < wait)
            {
                WebDriverWrapper.sleep(5000);
                _test.refresh();
            }
        }

        if (!projectLinkExists(project))
            _test.log(project + " deleted in " + (System.currentTimeMillis() - startTime) + "ms");
        else
            fail(project + " not finished deleting after " + (System.currentTimeMillis() - startTime) + " ms");
        _test.projectMenu().close();
    }

    private boolean projectLinkExists(String project) // use the presence of start-menu links to know
    {
        return _test.projectMenu().projectLinkExists(project);
    }
}
