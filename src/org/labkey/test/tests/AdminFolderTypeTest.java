/*
 * Copyright (c) 2021-2026 LabKey Corporation
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
import org.labkey.test.categories.Daily;
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.pages.core.admin.FolderTypePages;

import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class AdminFolderTypeTest extends BaseWebDriverTest
{
    @Test
    public void testDefaultFolderTypeSetting()
    {
        String newDefaultFolder = "Assay";

        log(String.format("Setting a new default folder type to '%s'.", newDefaultFolder));
        FolderTypePages folderTypePage = goToAdminConsole().clickFolderType();
        String oldDefaultFolder = folderTypePage.getDefaultFolderType();
        folderTypePage.setDefaultFolderType(newDefaultFolder).clickSave();

        folderTypePage = goToAdminConsole().clickFolderType();
        checker().verifyEquals("Incorrect default Folder type selected", newDefaultFolder, folderTypePage.getDefaultFolderType());

        log("Verifying the default folder type while project creation");
        goToCreateProject();
        waitForElement(Locator.tagWithText("label", "Collaboration"));
        checker().verifyTrue("Incorrect default folder type selected",
                RadioButton.RadioButton().withLabel(newDefaultFolder).find(getDriver()).isSelected());

        log(String.format("Rollback to the old default folder type '%s'.", oldDefaultFolder));
        folderTypePage = goToAdminConsole().clickFolderType();
        folderTypePage.setDefaultFolderType(oldDefaultFolder).clickSave();
    }

    @Test
    public void testEnableAndDisableFolderTypeSetting()
    {
        String folderTypeName = "Empty custom folder";

        log("Verifying by default folder is enabled");
        FolderTypePages folderTypePage = goToAdminConsole().clickFolderType();
        checker().verifyTrue(folderTypeName + " should have been enabled", folderTypePage.isEnabled(folderTypeName));

        goToCreateProject();
        waitForElement(Locator.tagWithText("label", "Collaboration"));
        checker().verifyTrue(folderTypeName + " project is not visible", isElementPresent(Locator.tagWithText("label",folderTypeName)));

        log("Disabling the folder type " + folderTypeName);
        folderTypePage = goToAdminConsole().clickFolderType();
        folderTypePage.disableFolderType(folderTypeName).clickSave();

        log("Verifying folder type " + folderTypeName + " is disabled");
        folderTypePage = goToAdminConsole().clickFolderType();
        checker().verifyFalse(folderTypeName + " should have been disabled", folderTypePage.isEnabled(folderTypeName));

        goToCreateProject();
        waitForElement(Locator.tagWithText("label", "Collaboration"));
        checker().verifyFalse(folderTypeName + " project is not hidden", isElementPresent(Locator.tagWithText("label",folderTypeName)));

        /* Test coverage for Issue 44995: Filter disabled folder types from folder management admin page */

        goToHome();
        goToFolderManagement().goToFolderTypeTab();
        checker().verifyFalse("Disabled folder " + folderTypeName + " should not be present at Folder Management --> Folder type",
               isElementPresent(Locator.radioButtonByNameAndValue("folderType", folderTypeName)));

        log("Enabling the folder type " + folderTypeName);
        folderTypePage = goToAdminConsole().clickFolderType();
        folderTypePage.enableFolderType(folderTypeName).clickSave();
    }

    @Override
    protected String getProjectName() { return null; }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
