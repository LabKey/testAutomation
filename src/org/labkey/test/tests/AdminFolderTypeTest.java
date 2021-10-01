package org.labkey.test.tests;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.pages.core.admin.FolderTypePage;

import java.util.List;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 2)
public class AdminFolderTypeTest extends BaseWebDriverTest
{
    @Test
    public void testDefaultFolderTypeSetting()
    {
        goToProjectHome();
        String newDefaultFolder = "Targeted MS";

        log("Setting a new default folder type");
        FolderTypePage folderTypePage = goToAdminConsole().clickFolderType();
        String oldDefaultFolder = folderTypePage.getDefaultFolder();
        folderTypePage.setDefaultFolder(newDefaultFolder).clickSave();

        folderTypePage = goToAdminConsole().clickFolderType();
        checker().verifyEquals("Incorrect default Folder type selected", newDefaultFolder, folderTypePage.getDefaultFolder());

        log("Verifying the default folder type while project creation");
        goToCreateProject();
        checker().verifyTrue("Incorrect default folder type selected",
                RadioButton.RadioButton().withLabel("Panorama").find(getDriver()).isSelected());

        log("Rollback to the old default folder type");
        folderTypePage = goToAdminConsole().clickFolderType();
        folderTypePage.setDefaultFolder(oldDefaultFolder).clickSave();
    }

    @Test
    public void testEnableAndDisableFolderSetting()
    {
        goToProjectHome();
        String folderName = "Empty custom folder";

        log("Verifying by default folder is enabled");
        FolderTypePage folderTypePage = goToAdminConsole().clickFolderType();
        checker().verifyTrue(folderName + " should have been enabled", folderTypePage.isEnabled(folderName));

        goToCreateProject();
        checker().verifyTrue(folderName + " project is not enabled", isElementPresent(Locator.tagWithText("label",folderName)));

        log("Disabling the folder");
        folderTypePage = goToAdminConsole().clickFolderType();
        folderTypePage.disableFolder(folderName).clickSave();

        log("Verifying folder is disabled");
        folderTypePage = goToAdminConsole().clickFolderType();
        checker().verifyFalse(folderName + " should have been disabled", folderTypePage.isEnabled(folderName));

        goToCreateProject();
        checker().verifyFalse(folderName + " project is not disabled", isElementPresent(Locator.tagWithText("label",folderName)));
    }

    @Override
    protected String getProjectName() { return "Admin Folder Type Test Project"; }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
