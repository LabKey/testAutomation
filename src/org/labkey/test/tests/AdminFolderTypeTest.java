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
        checker().fatal().verifyEquals("Incorrect default Folder type selected", newDefaultFolder, folderTypePage.getDefaultFolderType());

        log("Verifying the default folder type while project creation");
        goToCreateProject();
        waitForElement(Locator.tagWithText("label", "Collaboration"));
        checker().fatal().verifyTrue("Incorrect default folder type selected",
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
        checker().fatal().verifyTrue(folderTypeName + " should have been enabled", folderTypePage.isEnabled(folderTypeName));

        goToCreateProject();
        waitForElement(Locator.tagWithText("label", "Collaboration"));
        checker().fatal().verifyTrue(folderTypeName + " project is not visible", isElementPresent(Locator.tagWithText("label",folderTypeName)));

        log("Disabling the folder type " + folderTypeName);
        folderTypePage = goToAdminConsole().clickFolderType();
        folderTypePage.disableFolderType(folderTypeName).clickSave();

        log("Verifying folder type " + folderTypeName + " is disabled");
        folderTypePage = goToAdminConsole().clickFolderType();
        checker().fatal().verifyFalse(folderTypeName + " should have been disabled", folderTypePage.isEnabled(folderTypeName));

        goToCreateProject();
        waitForElement(Locator.tagWithText("label", "Collaboration"));
        checker().fatal().verifyFalse(folderTypeName + " project is not hidden", isElementPresent(Locator.tagWithText("label",folderTypeName)));

        /* Test coverage for Issue 44995: Filter disabled folder types from folder management admin page */

        goToHome();
        goToFolderManagement().goToFolderTypeTab();
        checker().fatal().verifyFalse("Disabled folder " + folderTypeName + " should not be present at Folder Management --> Folder type",
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
