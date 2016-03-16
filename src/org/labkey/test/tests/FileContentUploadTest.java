/*
 * Copyright (c) 2008-2015 LabKey Corporation
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.FileBrowserExtendedProperty;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SearchHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.labkey.test.util.FileBrowserHelper.BrowserAction;

@Category({BVT.class, FileBrowser.class})
public class FileContentUploadTest extends BaseWebDriverTest
{
    private final SearchHelper _searchHelper = new SearchHelper(this);

    private static final String FILE_DESCRIPTION = "FileContentTestFile";
    private static final String CUSTOM_PROPERTY_VALUE = "ExtendedProperty";
    private static final String CUSTOM_PROPERTY = "customProperty";
    protected static final String TEST_USER = "user_filecontent@filecontentupload.test";
    private static final String TEST_GROUP = "FileContentTestGroup";

    // Lookup list info
    private static final String LIST_NAME = "LookupList";
    private static final String COLUMN_NAME = "LookupColumn";
    private static final String LOOKUP_VALUE_1 = "Hydrogen";
    private static final String LOOKUP_VALUE_2 = "Helium";

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("filecontent");
    }

    @Override
    protected String getProjectName()
    {
        // Use a special exotic character in order to make sure we don't break
        // i18n. See https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=5369
        return "File Content T\u017Dst Project";
    }

    @BeforeClass
    public static void doSetup() throws Exception
    {
        FileContentUploadTest initTest = (FileContentUploadTest)getCurrentTest();

        initTest.doSetupSteps();
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
        deleteUsersIfPresent(TEST_USER);
    }

    private void doSetupSteps()
    {
        _searchHelper.initialize();

        _containerHelper.createProject(getProjectName(), null);
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Files");
        _permissionsHelper.createPermissionsGroup(TEST_GROUP, TEST_USER);
        _permissionsHelper.setPermissions(TEST_GROUP, "Editor");
        _permissionsHelper.exitPermissionsUI();
    }

    @Test
    public void testFileBrowser()
    {
        setupNotifications();
        setupCustomFileProperties();
        goToProjectHome();

        final File testFile = TestFileUtils.getSampleData("security/InlineFile.html");
        final String filename = testFile.getName();
        List<FileBrowserExtendedProperty> fileProperties = new ArrayList<>();
        fileProperties.add(new FileBrowserExtendedProperty(CUSTOM_PROPERTY, CUSTOM_PROPERTY_VALUE, false));
        fileProperties.add(new FileBrowserExtendedProperty("LookupColumn:", LOOKUP_VALUE_2, true));

        _fileBrowserHelper.uploadFile(testFile, FILE_DESCRIPTION, fileProperties, false);
        assertElementPresent(Locator.linkWithText(LOOKUP_VALUE_2));
        assertElementPresent(Locator.linkWithText(CUSTOM_PROPERTY_VALUE));
        assertAttributeEquals(Locator.linkWithText(CUSTOM_PROPERTY_VALUE), "href", "http://labkey.test/?a=" + CUSTOM_PROPERTY_VALUE + "&b=" + LOOKUP_VALUE_2);

        log("replace file");
        refresh();
        fileProperties = new ArrayList<>();
        fileProperties.add(new FileBrowserExtendedProperty(CUSTOM_PROPERTY, CUSTOM_PROPERTY_VALUE, false));
        fileProperties.add(new FileBrowserExtendedProperty("LookupColumn:", LOOKUP_VALUE_1, true));
        _fileBrowserHelper.uploadFile(testFile, FILE_DESCRIPTION, fileProperties, true);

        log("move file");
        final String folderName = "Test folder";
        _fileBrowserHelper.createFolder(folderName);
        _fileBrowserHelper.moveFile(filename, folderName);

        log("rename file");
        final String newFileName = "changedFilename.html";
        _fileBrowserHelper.renameFile(folderName + "/" + filename, newFileName);

        _searchHelper.enqueueSearchItem(newFileName, true, Locator.linkContainingText(newFileName));
        _searchHelper.enqueueSearchItem(FILE_DESCRIPTION, true, Locator.linkContainingText(newFileName));
        _searchHelper.enqueueSearchItem(CUSTOM_PROPERTY_VALUE, true, Locator.linkContainingText(newFileName));

        _searchHelper.verifySearchResults("/" + getProjectName() + "/@files/" + folderName, false);
        _searchHelper.assertNoSearchResult(filename);

        // Delete file.
        clickProject(getProjectName());
        _fileBrowserHelper.deleteFile(folderName + "/" + newFileName);
        waitForElementToDisappear(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(newFileName));
        _searchHelper.assertNoSearchResult(newFileName);
        _searchHelper.assertNoSearchResult(FILE_DESCRIPTION);
        _searchHelper.assertNoSearchResult(CUSTOM_PROPERTY_VALUE);

        clickProject(getProjectName());
        _fileBrowserHelper.clickFileBrowserButton(BrowserAction.AUDIT_HISTORY);
        assertTextPresent(
                "File uploaded to project: /" + getProjectName(),
                "annotations updated: " + CUSTOM_PROPERTY + "=" + CUSTOM_PROPERTY_VALUE,
                "File deleted from project: /" + getProjectName());

        beginAt(getBaseURL() + "/filecontent/" + EscapeUtil.encode(getProjectName()) + "/sendShortDigest.view");
        goToModule("Dumbster");
        addUrlParameter("reverse=true"); // List emails chronologically, in case of multiple notifications
        click(Locator.linkWithText("File Management Notification"));
        // All notifications might not appear in one digest
        if (isElementPresent(Locator.linkWithText("File Management Notification").index(1)))
            click(Locator.linkWithText("File Management Notification").index(1));
        assertTextPresentInThisOrder("File uploaded", "annotations updated", "File deleted");

        assertTextNotPresent(TEST_USER);  // User opted out of notifications
    }

    private void setupNotifications()
    {
        goToProjectHome();
        // Setup notification emails
        // as they are now digest based.
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Notifications"));


        _ext4Helper.selectComboBoxItem(MessagesLongTest.FILES_DEFAULT_COMBO, "15 minute digest");
        doAndWaitForPageSignal(() -> clickButton("Update", 0), "notificationSettingUpdate"); // signal in notifySettings.jsp
        waitForElementToDisappear(Ext4Helper.Locators.mask());


        // Change user setting TEST_USER -> No Email
        DataRegionTable table = new DataRegionTable("Users", this);
        table.checkCheckbox(table.getRow("Email", TEST_USER));
        shortWait().until(LabKeyExpectedConditions.elementIsEnabled(Locator.lkButton(MessagesLongTest.USERS_UPDATE_BUTTON)));
        _ext4Helper.clickExt4MenuButton(false, Locator.lkButton(MessagesLongTest.USERS_UPDATE_BUTTON), false, MessagesLongTest.FILES_MENU_ITEM);
        waitForElement(Ext4Helper.Locators.window("Update User Settings For Files"));
        _ext4Helper.selectComboBoxItem(MessagesLongTest.NEW_SETTING_LABEL, "No Email");
        clickButton(MessagesLongTest.POPUP_UPDATE_BUTTON, 0);
        doAndWaitForPageSignal(
                () -> waitAndClick(Ext4Helper.Locators.windowButton("Update Selected Users", "Yes")),
                DataRegionTable.SELECTION_SIGNAL);
        waitForElementToDisappear(Ext4Helper.Locators.window("Update Selected Users"));

        table = new DataRegionTable("Users", this);
        assertEquals("Failed to opt out of file notifications.", "No Email", table.getDataAsText(table.getRow("Email", TEST_USER), "File Settings"));

        enableEmailRecorder();
    }

    private void setupCustomFileProperties()
    {
        // Create list for lookup custom file property
        _listHelper.createList(getProjectName(), LIST_NAME, ListHelper.ListColumnType.String, COLUMN_NAME);
        _listHelper.uploadData(COLUMN_NAME+"\n"+LOOKUP_VALUE_1+"\n"+LOOKUP_VALUE_2);
        clickProject(getProjectName());
        // Setup custom file properties
        _fileBrowserHelper.goToAdminMenu();
        // Setup custom file actions

        waitAndClick(Ext4Helper.Locators.ext4CheckboxById("importAction"));

        waitAndClick(Ext4Helper.Locators.ext4Tab("File Properties"));
        click(Ext4Helper.Locators.ext4Radio("Use Custom File Properties"));
        clickButton("edit properties");

        waitForElement(Locator.name("ff_name0"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("ff_name0"), CUSTOM_PROPERTY);
        setFormElement(Locator.id("url"), "http://labkey.test/?a=${"+CUSTOM_PROPERTY+"}&b=${"+COLUMN_NAME+"}");
        _listHelper.addLookupField(null, 1, COLUMN_NAME, COLUMN_NAME, new ListHelper.LookupInfo(getProjectName(), "lists", LIST_NAME));
        clickButton("Save & Close");
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
