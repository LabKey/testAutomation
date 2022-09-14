/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
package org.labkey.test.tests.filecontent;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.FileBrowserHelper.BrowserAction;
import org.labkey.test.util.PortalHelper;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@Category({Daily.class, FileBrowser.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class FileContentActionButtonsTest extends BaseWebDriverTest
{
    @BeforeClass
    public static void doSetup() throws Exception
    {
        FileContentActionButtonsTest initTest = (FileContentActionButtonsTest)getCurrentTest();

        initTest.doSetupSteps();
    }

    private void doSetupSteps()
    {
        _containerHelper.createProject(getProjectName(), null);
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Files");
    }

    @Before
    public void preTest()
    {
        goToProjectHome();

        // ensure that the file browser is in the default state
        resetToDefaultToolbar();
    }

    @Test
    public void testDefaultActions()
    {
        Collection<BrowserAction> buttonsWithText = new HashSet<>(Arrays.asList(
                BrowserAction.UPLOAD,
                BrowserAction.IMPORT_DATA,
                BrowserAction.AUDIT_HISTORY,
                BrowserAction.ADMIN)
        );

        // All icons are present by default
        for (BrowserAction action : BrowserAction.values())
        {
            assertElementPresent(action.getButtonIconLocator());
            if (buttonsWithText.contains(action))
                assertElementPresent(action.getButtonTextLocator());
            else
                assertElementNotPresent(action.getButtonTextLocator());
        }
    }

    @Test
    public void testEditorActions()
    {
        impersonateRoles("Editor");

        assertActionsAvailable(
                BrowserAction.FOLDER_TREE,
                BrowserAction.UP,
                BrowserAction.REFRESH,
                BrowserAction.NEW_FOLDER,
                BrowserAction.DOWNLOAD,
                BrowserAction.DELETE,
                BrowserAction.RENAME,
                BrowserAction.MOVE,
                BrowserAction.EDIT_PROPERTIES,
                BrowserAction.UPLOAD,
                BrowserAction.IMPORT_DATA,
                BrowserAction.EMAIL_SETTINGS
        );

        stopImpersonatingRole();
    }

    @Test
    public void testSubmitterReaderActions()
    {
        impersonateRoles("Submitter", "Reader");

        assertActionsAvailable(
                BrowserAction.FOLDER_TREE,
                BrowserAction.UP,
                BrowserAction.REFRESH,
                BrowserAction.NEW_FOLDER,
                BrowserAction.DOWNLOAD,
                BrowserAction.EDIT_PROPERTIES,
                BrowserAction.UPLOAD,
                BrowserAction.IMPORT_DATA,
                BrowserAction.EMAIL_SETTINGS
        );

        stopImpersonatingRole();
    }

    @Test
    public void testAuthorActions()
    {
        impersonateRoles("Author");

        assertActionsAvailable(
                BrowserAction.FOLDER_TREE,
                BrowserAction.UP,
                BrowserAction.REFRESH,
                BrowserAction.NEW_FOLDER,
                BrowserAction.DOWNLOAD,
                BrowserAction.EDIT_PROPERTIES,
                BrowserAction.UPLOAD,
                BrowserAction.IMPORT_DATA,
                BrowserAction.EMAIL_SETTINGS
        );

        stopImpersonatingRole();
    }

    @Test
    public void testReaderActions()
    {
        impersonateRoles("Reader");

        assertActionsAvailable(
                BrowserAction.FOLDER_TREE,
                BrowserAction.UP,
                BrowserAction.REFRESH,
                BrowserAction.DOWNLOAD,
                BrowserAction.EMAIL_SETTINGS
        );

        stopImpersonatingRole();
    }

    @Test
    public void testCustomizeToolbar()
    {
        _fileBrowserHelper.goToConfigureButtonsTab();
        _fileBrowserHelper.removeToolbarButton("refresh");
        click(Locator.xpath("//tr[@data-recordid='download']/td[2]")); // Unhide text for 'Download' button
        click(Locator.xpath("//tr[@data-recordid='download']/td[3]")); // Hide icon for 'Download' button
        click(Ext4Helper.Locators.ext4Button("submit"));
        waitForElementToDisappear(BrowserAction.REFRESH.getButtonIconLocator());
        waitForElementToDisappear(BrowserAction.DOWNLOAD.getButtonIconLocator());
        waitForElement(BrowserAction.DOWNLOAD.getButtonTextLocator());

        // Verify custom action buttons
        _fileBrowserHelper.goToConfigureButtonsTab();
        _fileBrowserHelper.removeToolbarButton("download");
        _fileBrowserHelper.addToolbarButton("refresh");
        click(Ext4Helper.Locators.ext4Button("submit"));
        waitForElementToDisappear(BrowserAction.DOWNLOAD.getButtonTextLocator());
        waitForElementToDisappear(BrowserAction.DOWNLOAD.getButtonIconLocator());
        waitForElement(BrowserAction.REFRESH.getButtonIconLocator());
        waitForElement(BrowserAction.REFRESH.getButtonTextLocator());

        resetToDefaultToolbar();
        waitForElement(BrowserAction.UP.getButtonIconLocator());
    }

    @Test
    public void testActionsWithSpecialCharactersInFileName()
    {
        goToProjectHome();
        goToModule("FileContent");
        Set<String> expectedFolders = new HashSet<>();
        String uploadFileName = "pdf_sample_with+%$@+%%+#-+=.pdf";
        String renamedFile = "pdf_sample_with+%$@+%%+#-+=_!@+%.pdf";
        String fileDescription = "sample pdf";
        String uploadFolderName = "fileTypes";

        String folderName1 = "test_~!@#$%()-_=+Folder~!@#$%()-_=+_Ž";
        log("Creating folder: " + folderName1);
        expectedFolders.add(folderName1);
        _fileBrowserHelper.createFolder(folderName1);

        String folderName2 = "+Folder~!@#$%()-_=+_";
        log("Creating folder: " + folderName2);
        expectedFolders.add(folderName2);
        _fileBrowserHelper.createFolder(folderName2);

        Set<String> folders = new HashSet<>(_fileBrowserHelper.getFileList());
        assertEquals("Expected folders not created", expectedFolders, folders);

        log("Uploading file '" + uploadFileName + "' to " + folderName1);
        _fileBrowserHelper.selectFileBrowserItem(folderName1 + "/");
        _fileBrowserHelper.uploadFile(TestFileUtils.getSampleData(uploadFolderName + "/" + uploadFileName));

        log("Moving file '" + uploadFileName + "' to " + folderName2);
        _fileBrowserHelper.moveFile("/" + folderName1 + "/" + uploadFileName, folderName2);

        log("Renaming file '" + uploadFileName + "' to '" + renamedFile + "'");
        _fileBrowserHelper.selectFileBrowserItem("/" + folderName2 + "/");
        _fileBrowserHelper.renameFile("/" + folderName2 + "/" + uploadFileName, renamedFile);

        log("Downloading file '" + renamedFile + "'");
        _fileBrowserHelper.selectFileBrowserItem("/" + folderName2 + "/" + renamedFile);
        File download = _fileBrowserHelper.downloadSelectedFiles();
        assertEquals(renamedFile, download.getName());

        log("Setting description " + fileDescription + " for '" + renamedFile + "'");
        _fileBrowserHelper.setDescription(renamedFile, fileDescription);
        assertEquals("Folder description is not as expected", fileDescription, _fileBrowserHelper.getFileDescription(renamedFile));

        log("Deleting '" + renamedFile + "'");
        _fileBrowserHelper.deleteFile(renamedFile);
        assertFalse("Unable to delete file '" + renamedFile + "'", _fileBrowserHelper.fileIsPresent(renamedFile));

        log("Drag and Drop file '" + uploadFileName + "'");
        _fileBrowserHelper.dragDropUpload(TestFileUtils.getSampleData(uploadFolderName + "/" + uploadFileName));
        assertEquals("File not uploaded via drag and drop", _fileBrowserHelper.getFileList().get(0), uploadFileName);

    }

    private void assertActionsAvailable(BrowserAction... actions)
    {
        assertEquals(Arrays.asList(actions), _fileBrowserHelper.getAvailableBrowserActions());
    }

    private void resetToDefaultToolbar()
    {
        _fileBrowserHelper.goToConfigureButtonsTab();
        clickButton("Reset to Default", 0);
        waitAndClick(Ext4Helper.Locators.windowButton("Confirm Reset", "Yes"));
        _ext4Helper.waitForMaskToDisappear();
    }

    @Nullable
    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("filecontent");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
