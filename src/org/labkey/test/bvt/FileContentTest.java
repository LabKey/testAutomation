/*
 * Copyright (c) 2008-2011 LabKey Corporation
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

package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class FileContentTest extends BaseSeleniumWebTest
{
    // Use a special exotic character in order to make sure we don't break
    // i18n. See https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=5369
    private static final String PROJECT_NAME = "File Content T\u017Dst Project";
    private static final String PROJECT_ENCODED = "File%20Content%20T%C3%A9st%20Project";
    private static final String FILE_DESCRIPTION = "FileContentTestFile";
    private static final String CUSTOM_PROPERTY_VALUE = "ExtendedProperty";
    private static final String CUSTOM_PROPERTY = "customProperty";
    private static final String TEST_USER = "user@filecontent.test";
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

    public boolean isFileUploadTest()
    {
        return true;
    }

    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
        deleteDir(getTestTempDir());
    }

    protected void doTestSteps() throws Exception
    {
        log("Create a new web part, upload file, log out and navigate to it");
        log("Note that we use a space and a non-ascii character in the project name, "+
            "so if this fails, check that tomcat's server.xml contains the following attribute " +
            "in its Connector element: URIEncoding=\"UTF-8\"");

        SearchHelper.initialize(this);

        createProject(PROJECT_NAME);
        createPermissionsGroup(TEST_GROUP, TEST_USER);
        setPermissions(TEST_GROUP, "Editor");
        exitPermissionsUI();

        assertFalse("ERROR: Add project with special characters failed; check that tomcat's server.xml contains the following attribute " +
            "in its Connector element: URIEncoding=\"UTF-8\"", isTextPresent("404: page not found"));

        clickLinkWithText("Project Settings");
        clickLinkWithText("Files");

        File dir = getTestTempDir();
        dir.mkdirs();
        setFormElement("rootPath", dir.getAbsolutePath());
        submit();

        clickLinkWithText(PROJECT_NAME);
        addWebPart("Files");

        if (isFileUploadAvailable())
        {
            // Setup notificaiton emails
            // as they are now digest based.
            clickAdminMenuItem("Manage Project", "Folder Settings");
            clickLinkWithText("Email Notifications");
            click(Locator.navButton("Update Settings"));
            // Set folder default
            ExtHelper.selectComboBoxItem(this, Locator.xpath("//div[./input[@name='defaultFileEmailOption']]"), "15 minute digest");
            clickButtonByIndex("Update Folder Default", 1, 0);
            ExtHelper.waitForExtDialog(this, "Update complete", WAIT_FOR_JAVASCRIPT);            
            waitForExtMaskToDisappear();
            // Change user setting TEST_USER -> No Email
            checkDataRegionCheckbox("Users", 1);
            ExtHelper.selectComboBoxItem(this, Locator.xpath("//div[./input[@name='fileEmailOption']]"), "No Email");
            clickButtonByIndex("Update Settings", 1, 0);
            ExtHelper.waitForExtDialog(this, "Update selected users", WAIT_FOR_JAVASCRIPT);
            clickNavButton("Yes", 0);
            waitForExtMaskToDisappear(); // Can't tell when ext mask is active (not style=display:block...
            sleep(200); // ...so we sleep

            waitForElement(Locator.xpath("//a/span[text() = 'Admin']"), WAIT_FOR_JAVASCRIPT);
            enableEmailRecorder();
            // Create list for lookup custom file property
            ListHelper.createList(this, PROJECT_NAME, LIST_NAME, ListHelper.ListColumnType.String, COLUMN_NAME);
            ListHelper.uploadData(this, PROJECT_NAME, LIST_NAME, COLUMN_NAME+"\n"+LOOKUP_VALUE_1+"\n"+LOOKUP_VALUE_2);
            clickLinkWithText(PROJECT_NAME);
            // Setup custom file properties
            clickButton("Admin", 0);
            ExtHelper.waitForExtDialog(this, "Manage File Browser Configuration", 5000);
            clickConfigTab(FileTab.file);
            checkRadioButton("fileOption", "useCustom");
            clickButton("Edit Properties...");
            waitForElement(Locator.name("ff_name0"), WAIT_FOR_JAVASCRIPT);
            setFormElement("ff_name0", CUSTOM_PROPERTY);
            addLookupField(null, 1, COLUMN_NAME, COLUMN_NAME, new ListHelper.LookupInfo(PROJECT_NAME, "lists", LIST_NAME));
            clickNavButton("Save & Close");

            waitForText("Last Modified", WAIT_FOR_JAVASCRIPT);
            sleep(1000); // Config button bar is broken without this wait.
            clickButton("Admin", 0);
            ExtHelper.waitForExtDialog(this, "Manage File Browser Configuration", 5000);

            // Setup custom file actions
            uncheckCheckbox("importAction");

            // enable custom file properties.
            clickConfigTab(FileTab.file);
            checkRadioButton("fileOption", "useCustom");

            clickConfigTab(FileTab.toolbar);

            // TODO: Add new button once 11342 is resolved.
            // dragAndDrop(Locator.xpath("//td[contains(@class, 'x-table-layout-cell')]//button[text()='Show History']"),
            //             Locator.xpath("//div[contains(@class, 'test-custom-toolbar')]"));
            click(Locator.xpath("//div[contains(@class, 'test-custom-toolbar')]//button[contains(@class, 'iconDownload')]"));
            click(Locator.xpath("//a[./span[text()='remove']]"));

            // Save settings.
            clickButton("Submit", 0);
            waitForExtMaskToDisappear();

            // Verify custom action buttons
            waitForElementToDisappear(Locator.xpath("//button[contains(@class, 'iconDownload')]"), WAIT_FOR_JAVASCRIPT);
            // TODO: Check added button once 11342 is resolved. 
            // assertElementPresent(Locator.xpath("//button[text()='Show History']"));

            clickButton("Upload Files",0);

            String filename = "InlineFile.html";
            String sampleRoot = getLabKeyRoot() + "/sampledata/security";
            File f = new File(sampleRoot, filename);
            setFormElement(Locator.xpath("//input[contains(@class, 'x-form-file') and @type='file']"), f.toString());
            setFormElement(Locator.xpath("//div[./label[text() = 'Description:']]/div/input[contains(@class, 'x-form-text')]"), FILE_DESCRIPTION);
            fireEvent(Locator.xpath("//div[./label[text() = 'Description:']]/div/input[contains(@class, 'x-form-text')]"), SeleniumEvent.blur);
            clickButton("Upload", 0);
            waitForExtMaskToDisappear();
            ExtHelper.waitForExtDialog(this, "Extended File Properties", WAIT_FOR_JAVASCRIPT);
            setFormElement(CUSTOM_PROPERTY, CUSTOM_PROPERTY_VALUE);
            click(Locator.xpath("//img[../../../label[contains(text(),'"+COLUMN_NAME+":')] ]"));
            waitForElement(Locator.xpath("//div[contains(@class, 'x-combo-list-item') and text() = '"+LOOKUP_VALUE_2+"']"), WAIT_FOR_JAVASCRIPT);
            click(Locator.xpath("//div[contains(@class, 'x-combo-list-item') and text() = '"+LOOKUP_VALUE_2+"']"));
            clickButton("Done", 0);
            waitForExtMaskToDisappear();
            
            waitForText(filename, WAIT_FOR_JAVASCRIPT);
            waitForText(FILE_DESCRIPTION, WAIT_FOR_JAVASCRIPT);
            // waitForText(CUSTOM_PROPERTY_VALUE, WAIT_FOR_JAVASCRIPT); // TODO: 11373: Custom file properties don't work on sqlserver
            // waitForText(LOOKUP_VALUE_2, WAIT_FOR_JAVASCRIPT);

            // Check custom actions as non-administrator.
            impersonate(TEST_USER);
            clickLinkWithText(PROJECT_NAME);
            waitForElementToDisappear(Locator.xpath("//button[text()='Import Data']"), WAIT_FOR_JAVASCRIPT);

            stopImpersonating();

            signOut();

            // Test that renderAs can be observed through a login
            beginAt("files/" + encode(PROJECT_NAME) + "/" + filename + "?renderAs=INLINE");
            assertTitleEquals("Sign In");

            log("Test renderAs through login and ensure that page is rendered inside of server UI");
            // If this succeeds, then page has been rendered in frame
            simpleSignIn();

            assertTextPresent("antidisestablishmentarianism");

            clickLinkWithText(PROJECT_NAME);

            SearchHelper.enqueueSearchItem(filename, true, Locator.linkContainingText(filename));
            //SearchHelper.enqueueSearchItem(FILE_DESCRIPTION, true, Locator.linkContainingText(filename)); // TODO: Blocked by Issue #11393
            SearchHelper.enqueueSearchItem(CUSTOM_PROPERTY_VALUE, true,  Locator.linkContainingText(filename));

            SearchHelper.verifySearchResults(this, "/" + PROJECT_NAME, false);

            // Delete file.
            clickLinkWithText(PROJECT_NAME);
            waitForText(filename, WAIT_FOR_JAVASCRIPT);
            // Wait a bit before trying to select the file
            sleep(2000);
            ExtHelper.clickFileBrowserFileCheckbox(this, filename);
            click(Locator.xpath("//button[contains(@class, 'iconDelete')]"));
            clickButton("Yes", 0);
            waitForElementToDisappear(Locator.xpath("//*[text()='"+filename+"']"), 5000);
                  
            clickButton("Audit History");
            assertTextPresent("file uploaded to folder: /" + PROJECT_NAME);
            assertTextPresent("annotations updated: "+CUSTOM_PROPERTY+"="+CUSTOM_PROPERTY_VALUE);
            assertTextPresent("file deleted from folder: /" + PROJECT_NAME);

            beginAt(getBaseURL()+"/filecontent/" + PROJECT_NAME + "/sendShortDigest.view");
            goToModule("Dumbster");
            assertTextNotPresent(TEST_USER);  // User opted out of notifications
            clickLinkWithText("File Management Notification", false);
            assertTextBefore("file uploaded", "annotations updated");
            assertTextBefore("annotations updated", "file deleted");
        }
    }

    private static String encode(String data) throws UnsupportedEncodingException
    {
        return URLEncoder.encode(data, "UTF-8").replace("+","%20");
    }

    private enum FileTab
    {action, file, toolbar, email}
    private void clickConfigTab(FileTab tab)
    {
        click(Locator.xpath("//li[contains(@id, '__"+tab+"Tab')]/a[contains(@class, 'x-tab-right')]"));
    }
}