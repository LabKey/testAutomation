/*
 * Copyright (c) 2008-2014 LabKey Corporation
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
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.FileBrowser;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.FileBrowserExtendedProperty;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.SearchHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@Category({BVT.class, FileBrowser.class})
public class FileContentTest extends BaseWebDriverTest
{
    private final SearchHelper _searchHelper = new SearchHelper(this);

    // Use a special exotic character in order to make sure we don't break
    // i18n. See https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=5369
    protected static final String PROJECT_NAME = "File Content T\u017Dst Project";
    private static final String FILE_DESCRIPTION = "FileContentTestFile";
    private static final String CUSTOM_PROPERTY_VALUE = "ExtendedProperty";
    private static final String CUSTOM_PROPERTY = "customProperty";
    protected static final String TEST_USER = "user_filecontent@filecontent.test";
    private static final String TEST_GROUP = "FileContentTestGroup";

    // Lookup list info
    private static final String LIST_NAME = "LookupList";
    private static final String COLUMN_NAME = "LookupColumn";
    private static final String LOOKUP_VALUE_1 = "Hydrogen";
    private static final String LOOKUP_VALUE_2 = "Helium";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/filecontent";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }


    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(PROJECT_NAME, afterTest);
        deleteUsers(afterTest, TEST_USER);
        deleteDir(getTestTempDir());
    }

    @Test
    public void testSteps()
    {
        log("Create a new web part, upload file, log out and navigate to it");
        log("Note that we use a space and a non-ascii character in the project name, "+
            "so if this fails, check that tomcat's server.xml contains the following attribute " +
            "in its Connector element: URIEncoding=\"UTF-8\"");

        _searchHelper.initialize();

        _containerHelper.createProject(PROJECT_NAME, null);
        createPermissionsGroup(TEST_GROUP, TEST_USER);
        setPermissions(TEST_GROUP, "Editor");
        exitPermissionsUI();

        addWebPart("Files");

        // Setup notificaiton emails
        // as they are now digest based.
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Notifications"));
        click(Locator.navButton("Update Settings"));
        shortWait().until(LabKeyExpectedConditions.animationIsDone(Locator.css(".labkey-ribbon > div")));
        // Set folder default
        _extHelper.selectComboBoxItem(Locator.xpath("//div[./input[@name='defaultFileEmailOption']]"), "15 minute digest");
        click(Locator.xpath("//div[starts-with(@id, 'PanelButtonContent') and contains(@id, 'files')]//button[text()='Update Folder Default']"));
        _extHelper.waitForExtDialog("Update complete", WAIT_FOR_JAVASCRIPT);
        _extHelper.waitForExt3MaskToDisappear(WAIT_FOR_JAVASCRIPT);
        // Change user setting TEST_USER -> No Email
        DataRegionTable table = new DataRegionTable("Users", this);
        checkDataRegionCheckbox("Users", table.getRow("Email", TEST_USER));
        _extHelper.selectComboBoxItem(Locator.xpath("//div[./input[@name='fileEmailOption']]"), "No Email");
        click(Locator.xpath("//div[starts-with(@id, 'PanelButtonContent') and contains(@id, 'files')]//button[text()='Update Settings']"));
        _extHelper.waitForExtDialog("Update selected users");
        _extHelper.clickExtButton("Update selected users", "Yes");
        assertEquals("Failed to opt out of file notifications.", "No Email", table.getDataAsText(table.getRow("Email", TEST_USER), "File Settings"));

        waitForElement(Locator.xpath("//a/span[text() = 'Admin']"), WAIT_FOR_JAVASCRIPT);
        enableEmailRecorder();
        // Create list for lookup custom file property
        _listHelper.createList(PROJECT_NAME, LIST_NAME, ListHelper.ListColumnType.String, COLUMN_NAME);
        _listHelper.uploadData(PROJECT_NAME, LIST_NAME, COLUMN_NAME+"\n"+LOOKUP_VALUE_1+"\n"+LOOKUP_VALUE_2);
        clickProject(PROJECT_NAME);
        // Setup custom file properties
        _fileBrowserHelper.waitForFileGridReady();
        _fileBrowserHelper.goToAdminMenu();
        // Setup custom file actions

        waitAndClick(Locator.ext4CheckboxById("importAction"));

        _ext4Helper.clickExtTab("File Properties");
        click(Locator.ext4Radio("Use Custom File Properties"));
        clickButton("edit properties");

        waitForElement(Locator.name("ff_name0"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("ff_name0"), CUSTOM_PROPERTY);
        setFormElement(Locator.id("url"), "http://labkey.test/?a=${"+CUSTOM_PROPERTY+"}&b=${"+COLUMN_NAME+"}");
        _listHelper.addLookupField(null, 1, COLUMN_NAME, COLUMN_NAME, new ListHelper.LookupInfo(PROJECT_NAME, "lists", LIST_NAME));
        clickButton("Save & Close");

        waitForText("Last Modified", WAIT_FOR_JAVASCRIPT);
        _fileBrowserHelper.waitForFileGridReady();
        _fileBrowserHelper.goToAdminMenu();

        // enable custom file properties.
        _ext4Helper.clickExtTab("File Properties");
        click(Locator.ext4Radio("Use Custom File Properties"));

        // Modify toolbar.
        _fileBrowserHelper.goToConfigureButtonsTab();
        waitForText("Import Data");
        _fileBrowserHelper.removeToolbarButton("createDirectory");
        click(Locator.xpath("//tr[@data-recordid='parentFolder']/td[2]")); // Add text to 'Parent Folder' button
        click(Locator.ext4Button("submit"));

        // Verify custom action buttons
        waitForElementToDisappear(Locator.xpath("//span[contains(@class, 'iconFolderNew')]"));
        waitForElement(Locator.xpath("//span[text()='Parent Folder']"));

        _fileBrowserHelper.goToConfigureButtonsTab();
        _fileBrowserHelper.removeToolbarButton("importData");
        _fileBrowserHelper.addToolbarButton("createDirectory");
        click(Locator.ext4Button("submit"));
        waitForElementToDisappear(Locator.xpath("//span[contains(@class, 'iconDBCommit')]"));
        waitForElement(Locator.xpath("//span[contains(@class, 'iconFolderNew')]"));

        String filename = "InlineFile.html";
        String sampleRoot = getLabKeyRoot() + "/sampledata/security";
        File f = new File(sampleRoot, filename);
        List<FileBrowserExtendedProperty> fileProperties = new ArrayList<>();
        fileProperties.add(new FileBrowserExtendedProperty(CUSTOM_PROPERTY, CUSTOM_PROPERTY_VALUE, false));
        fileProperties.add(new FileBrowserExtendedProperty("LookupColumn:", LOOKUP_VALUE_2, true));

        _fileBrowserHelper.uploadFile(f, FILE_DESCRIPTION, fileProperties, false);
        assertElementPresent(Locator.linkWithText(LOOKUP_VALUE_2));
        assertElementPresent(Locator.linkWithText(CUSTOM_PROPERTY_VALUE));
        assertAttributeEquals(Locator.linkWithText(CUSTOM_PROPERTY_VALUE), "href", "http://labkey.test/?a="+CUSTOM_PROPERTY_VALUE+"&b="+LOOKUP_VALUE_2);

        log("replace file");
        refresh();
        fileProperties = new ArrayList<>();
        fileProperties.add(new FileBrowserExtendedProperty(CUSTOM_PROPERTY, CUSTOM_PROPERTY_VALUE, false));
        fileProperties.add(new FileBrowserExtendedProperty("LookupColumn:", LOOKUP_VALUE_1, true));
        _fileBrowserHelper.uploadFile(f, FILE_DESCRIPTION, fileProperties, true);

        log("rename file");
        String newFileName = "changedFilename.html";
        _fileBrowserHelper.renameFile(filename, newFileName);
        filename = newFileName;

        log("move file");
        String folderName = "Test folder";
        _fileBrowserHelper.createFolder(folderName);
        _fileBrowserHelper.moveFile(filename, folderName);

        // Check custom actions as non-administrator.
        impersonate(TEST_USER);
        clickProject(PROJECT_NAME);
        waitForElementToDisappear(Locator.xpath("//span[text()='Import Data']"), WAIT_FOR_JAVASCRIPT);

        stopImpersonating();

        signOut();

        // Test that renderAs can be observed through a login
        beginAt("files/" + EscapeUtil.encode(PROJECT_NAME)+"/%40files/" + EscapeUtil.encode(folderName) + "/" + filename + "?renderAs=INLINE");
        assertTitleEquals("Sign In");

        log("Test renderAs through login and ensure that page is rendered inside of server UI");
        // If this succeeds, then page has been rendered in frame
        simpleSignIn();

        assertTextPresent("antidisestablishmentarianism");

        clickProject(PROJECT_NAME);

        _searchHelper.enqueueSearchItem(filename, true, Locator.linkContainingText(filename));
        _searchHelper.enqueueSearchItem(FILE_DESCRIPTION, true, Locator.linkContainingText(filename));
        _searchHelper.enqueueSearchItem(CUSTOM_PROPERTY_VALUE, true,  Locator.linkContainingText(filename));

        _searchHelper.verifySearchResults("/" + PROJECT_NAME + "/@files/" + folderName, false);

        // Delete file.
        clickProject(PROJECT_NAME);
        _fileBrowserHelper.selectFileBrowserItem(folderName + "/" + filename);
        _fileBrowserHelper.clickFileBrowserButton(FileBrowserHelper.BrowserAction.DELETE);
        clickButton("Yes", 0);
        waitForElementToDisappear(Locator.css(".labkey-filecontent-grid div.x4-grid-cell-inner").withText(filename));

        _fileBrowserHelper.clickFileBrowserButton("Audit History");
        assertTextPresent("File uploaded to project: /" + PROJECT_NAME);
        assertTextPresent("annotations updated: "+CUSTOM_PROPERTY+"="+CUSTOM_PROPERTY_VALUE);
        assertTextPresent("File deleted from project: /" + PROJECT_NAME);

        beginAt(getBaseURL()+"/filecontent/" + EscapeUtil.encode(PROJECT_NAME) + "/sendShortDigest.view");
        goToModule("Dumbster");
        assertTextNotPresent(TEST_USER);  // User opted out of notifications

        // All notifications might not appear in one digest
        if (isElementPresent(Locator.linkWithText("File Management Notification").index(1)))
        {
            click(Locator.linkWithText("File Management Notification"));
            click(Locator.linkWithText("File Management Notification").index(1));
            assertTextPresentInThisOrder("File deleted", "File uploaded", "annotations updated"); // Deletion notification in most recent notification; Upload and update in the older notification
        }
        else // All notifications in one email
        {
            click(Locator.linkWithText("File Management Notification"));
            assertTextPresentInThisOrder("File uploaded", "annotations updated", "File deleted");
        }
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
