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

import org.hamcrest.CoreMatchers;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.components.DomainDesignerPage;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.tests.MessagesLongTest;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.FileBrowserExtendedProperty;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SearchHelper;
import org.labkey.test.util.Timer;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.labkey.test.components.ext4.Window.Window;
import static org.labkey.test.util.FileBrowserHelper.BrowserAction;

@Category({Daily.class, FileBrowser.class})
@BaseWebDriverTest.ClassTimeout(minutes = 6)
public class FileContentUploadTest extends BaseWebDriverTest
{
    private static final String FILE_DESCRIPTION = "FileContentTestFile";
    private static final String CUSTOM_PROPERTY_VALUE = "ExtendedProperty";
    private static final String CUSTOM_PROPERTY = "customProperty";
    protected static final String TEST_USER = "user_filecontent@filecontentupload.test";
    private static final String TEST_GROUP = "FileContentTestGroup";

    private static final String subfolderName = "Subfolder1";

    // Lookup list info
    private static final String LIST_NAME = "LookupList";
    private static final String COLUMN_NAME = "LookupColumn";
    private static final String LOOKUP_VALUE_1 = "Hydrogen";
    private static final String LOOKUP_VALUE_2 = "Helium";

    @Override
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
    public static void doSetup()
    {
        FileContentUploadTest initTest = (FileContentUploadTest)getCurrentTest();

        initTest.doSetupSteps();
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(false, TEST_USER);
    }

    private void doSetupSteps()
    {
        _containerHelper.createProject(getProjectName(), null);
        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addWebPart("Files");
        ApiPermissionsHelper permissionsHelper = new ApiPermissionsHelper(this);
        permissionsHelper.createPermissionsGroup(TEST_GROUP, TEST_USER);
        permissionsHelper.setPermissions(TEST_GROUP, "Editor");

        _containerHelper.createSubfolder(getProjectName(), subfolderName);
        clickFolder(subfolderName);
        portalHelper.addWebPart("Files");
    }

    protected SearchHelper getSearchHelper()
    {
        return new SearchHelper(this);
    }

    @Test
    public void testFileBrowser()
    {
        sendFileDigest();

        SearchHelper _searchHelper = getSearchHelper();
        _searchHelper.initialize();

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

        _searchHelper.enqueueSearchItem(filename); // No results for old file name
        _searchHelper.enqueueSearchItem(newFileName, folderName, Locator.linkContainingText(newFileName));
        _searchHelper.enqueueSearchItem(FILE_DESCRIPTION, folderName, Locator.linkContainingText(newFileName));
        _searchHelper.enqueueSearchItem(CUSTOM_PROPERTY_VALUE, folderName, Locator.linkContainingText(newFileName));

        _searchHelper.verifySearchResults(getProjectName(), "searchAfterRename");

        // Delete file.
        clickProject(getProjectName());
        _fileBrowserHelper.deleteFile(folderName + "/" + newFileName);
        waitForElementToDisappear(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(newFileName));
        _searchHelper.enqueueSearchItem(newFileName);
        _searchHelper.enqueueSearchItem(FILE_DESCRIPTION);
        _searchHelper.enqueueSearchItem(CUSTOM_PROPERTY_VALUE);

        _searchHelper.verifySearchResults(getProjectName(), "searchAfterDelete");

        clickProject(getProjectName());
        _fileBrowserHelper.clickFileBrowserButton(BrowserAction.AUDIT_HISTORY);
        assertTextPresent(
                "File uploaded to project: /" + getProjectName(),
                "annotations updated: " + CUSTOM_PROPERTY + "=" + CUSTOM_PROPERTY_VALUE,
                "File deleted from project: /" + getProjectName());

        sendFileDigest();
        goToModule("Dumbster");
        addUrlParameter("reverse=true"); // List emails chronologically, in case of multiple notifications

        Timer timer = new Timer(Duration.ofSeconds(15));
        Locator.XPathLocator notificationLoc = Locator.linkWithText("File Management Notification");
        do
        {
            sleep(1000);
            sendFileDigest();
            refresh();
        } while (!timer.isTimedOut() && !isElementPresent(notificationLoc));
        click(notificationLoc);
        // All notifications might not appear in one digest
        if (isElementPresent(notificationLoc.index(1)))
            click(notificationLoc.index(1));
        assertTextPresentInThisOrder("File uploaded", "annotations updated", "File deleted");

        assertTextNotPresent(TEST_USER);  // User opted out of notifications
    }

    @Test
    public void testAbsoluteFilePath() throws Exception
    {
        log("Check Absolute File Path in File Browser");

        final String s = File.separator;

        new ApiPermissionsHelper(this)
                .setSiteAdminRoleUserPermissions(PasswordUtil.getUsername(), "See Absolute File Paths");

        navigateToFolder(getProjectName(), subfolderName);
        final File testFile = TestFileUtils.getSampleData("security/InlineFile2.html");
        final String filename = testFile.getName();
        _fileBrowserHelper.uploadFile(testFile, FILE_DESCRIPTION, Collections.emptyList(), false);

        _fileBrowserHelper.goToAdminMenu();
        _fileBrowserHelper.goToConfigureButtonsTab();
        _fileBrowserHelper.unhideGridColumn(FileBrowserHelper.ABSOLUTE_FILE_PATH_COLUMN_ID);
        click(Ext4Helper.Locators.ext4Button("submit"));
        WebElement columnHeader = waitForElement(Locator.byClass("x4-column-header").withText("Absolute File Path").notHidden());

        String absolutePath = FileBrowserHelper.Locators.gridRowWithNodeId(filename)
                .append(Locator.byClass("x4-grid-cell").last()).findElement(getDriver()).getText();
        assertThat("Absolute file path for file", absolutePath, CoreMatchers.containsString(s + "@files" + s + filename));

        log("Check Absolute File Path in WebDav");
        URL webdavURL = new URL(WebTestHelper.getBaseURL() + "/_webdav" + getCurrentContainerPath() + "/@files");
        goToURL(webdavURL, 5000);
        waitForText("WebDav URL");
        absolutePath = Locator.byClass("fb-details")
                .append(Locator.tagWithText("th", "Absolute Path:").followingSibling("td"))
                .findElement(getDriver()).getText();

        assertThat("Absolute file path", absolutePath, CoreMatchers.containsString(s + "@files"));
    }

    @LogMethod(quiet = true)
    private void sendFileDigest()
    {
        invokeApiAction(getProjectName(), "filecontent", "sendShortDigest", "Failed to send file digest");
    }

    /**
     * Issue 36008: Folder name containing '%' character is not uploaded with the correct encoding in file browser
     */
    @Test
    public void testFolderNameCharacters()
    {
        goToProjectHome();
        goToModule("FileContent");
        List<String> stringsToCheck = folderSubstringsToVerify();
        Set<String> expectedFolders = new HashSet<>();

        for (String check : stringsToCheck)
        {
            String folderName = "test_" + check + "_tset";
            log("Creating folder: " + folderName);
            expectedFolders.add(folderName);
            _fileBrowserHelper.createFolder(folderName);
        }

        Set<String> folders = new HashSet<>(_fileBrowserHelper.getFileList());
        assertEquals("Didn't create expected folders", expectedFolders, folders);
    }

    @Test
    public void testDrop()
    {
        goToProjectHome();
        final File testFile = TestFileUtils.getSampleData("security/InlineFile_drop.html");

        log("Dropping the file object in drop zone");
        _fileBrowserHelper.dragDropUpload(testFile.getAbsoluteFile());

        log("Verifying file is uploaded");
        waitForElement(Locator.tagWithText("span", testFile.getName()));
        assertElementPresent(Locator.tagWithText("span", testFile.getName()));
    }

    @NotNull
    protected List<String> folderSubstringsToVerify()
    {
        return Arrays.asList("#", "%", "[", "]", "{", "}", "+", "%ab", "%20", "\u2603", "&", "'", "\u00E4\u00F6\u00FC\u00C5");
    }

    @LogMethod
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
        table.checkCheckbox(table.getRowIndex("Email", TEST_USER));
        shortWait().until(LabKeyExpectedConditions.elementIsEnabled(Locator.lkButton(MessagesLongTest.USERS_UPDATE_BUTTON)));
        table.clickHeaderMenu(MessagesLongTest.USERS_UPDATE_BUTTON, false, MessagesLongTest.FILES_MENU_ITEM);
        final Window window = Window(getDriver()).withTitle("Update user settings for files").waitFor();
        ComboBox.ComboBox(getDriver()).withLabel(MessagesLongTest.NEW_SETTING_LABEL).find(window).selectComboBoxItem("No Email");
        window.clickButton(MessagesLongTest.POPUP_UPDATE_BUTTON, true);
        table.doAndWaitForUpdate(() -> Window(getDriver()).withTitle("Update selected users").waitFor().
                clickButton("Yes"));

        assertEquals("Failed to opt out of file notifications.", "No Email", table.getDataAsText(table.getRowIndex("Email", TEST_USER), "File Settings"));

        enableEmailRecorder();
    }

    private void setupCustomFileProperties()
    {
        // Create list for lookup custom file property
        _listHelper.createList(getProjectName(), LIST_NAME, ListHelper.ListColumnType.String, COLUMN_NAME);
        _listHelper.goToList(LIST_NAME);
        _listHelper.uploadData(COLUMN_NAME+"\n"+LOOKUP_VALUE_1+"\n"+LOOKUP_VALUE_2);
        clickProject(getProjectName());
        // Setup custom file properties
        DomainDesignerPage editor = _fileBrowserHelper.goToEditProperties();

        DomainFieldRow row = editor.fieldsPanel().addField(CUSTOM_PROPERTY);
        row.setName(CUSTOM_PROPERTY);
        row.setUrl("http://labkey.test/?a=${" + CUSTOM_PROPERTY + "}&b=${" + COLUMN_NAME + "}");

        row = editor.fieldsPanel().addField(COLUMN_NAME);
        row.setLabel(COLUMN_NAME);
        row.setLookup(new FieldDefinition.LookupInfo(getProjectName(), "lists", LIST_NAME).setTableType(FieldDefinition.ColumnType.String));
        editor.clickFinish();
    }



    @Override
    public BrowserType bestBrowser()
    {
        return BrowserType.FIREFOX;
    }
}
