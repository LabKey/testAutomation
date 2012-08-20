/*
 * Copyright (c) 2008-2012 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PipelineHelper;
import org.labkey.test.util.SearchHelper;

import java.io.File;

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

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    public boolean isFileUploadTest()
    {
        return true;
    }


    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
        deleteUser(TEST_USER);
        deleteDir(getTestTempDir());
    }

    protected void doTestSteps() throws Exception
    {
        log("Create a new web part, upload file, log out and navigate to it");
        log("Note that we use a space and a non-ascii character in the project name, "+
            "so if this fails, check that tomcat's server.xml contains the following attribute " +
            "in its Connector element: URIEncoding=\"UTF-8\"");

        SearchHelper.initialize(this);

        PipelineHelper pipelineHelper = new PipelineHelper(this);

        _containerHelper.createProject(PROJECT_NAME, null);
        createPermissionsGroup(TEST_GROUP, TEST_USER);
        setPermissions(TEST_GROUP, "Editor");
        exitPermissionsUI();

        goToProjectSettings();
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
            goToFolderManagement();
            clickLinkWithText("Notifications");
            click(Locator.navButton("Update Settings"));
            // Set folder default
            ExtHelper.selectComboBoxItem(this, Locator.xpath("//div[./input[@name='defaultFileEmailOption']]"), "15 minute digest");
            click(Locator.xpath("//div[starts-with(@id, 'PanelButtonContent') and contains(@id, 'files')]//button[text()='Update Folder Default']"));
            ExtHelper.waitForExtDialog(this, "Update complete", WAIT_FOR_JAVASCRIPT);            
            waitForExtMaskToDisappear();
            // Change user setting TEST_USER -> No Email
            DataRegionTable table = new DataRegionTable("Users", this);
            checkDataRegionCheckbox("Users", table.getRow("Email", TEST_USER));
            ExtHelper.selectComboBoxItem(this, Locator.xpath("//div[./input[@name='fileEmailOption']]"), "No Email");
            click(Locator.xpath("//div[starts-with(@id, 'PanelButtonContent') and contains(@id, 'files')]//button[text()='Update Settings']"));
            waitAndClickNavButton("Yes");
            waitForPageToLoad();
            Assert.assertEquals("Failed to opt out of file notifications.", "No Email", table.getDataAsText(table.getRow("Email", TEST_USER), "File Settings"));

            waitForElement(Locator.xpath("//a/span[text() = 'Admin']"), WAIT_FOR_JAVASCRIPT);
            enableEmailRecorder();
            // Create list for lookup custom file property
            ListHelper.createList(this, PROJECT_NAME, LIST_NAME, ListHelper.ListColumnType.String, COLUMN_NAME);
            ListHelper.uploadData(this, PROJECT_NAME, LIST_NAME, COLUMN_NAME+"\n"+LOOKUP_VALUE_1+"\n"+LOOKUP_VALUE_2);
            clickLinkWithText(PROJECT_NAME);
            // Setup custom file properties
            ExtHelper.waitForFileGridReady(this);
            ExtHelper.waitForFileAdminEnabled(this);
            pipelineHelper.goToAdminMenu();
            // Setup custom file actions
            uncheckCheckbox("importAction");


            ExtHelper.clickExtTab(this, "File Properties");
            checkRadioButton("fileOption", "useCustom");
            clickButton("Edit Properties...");
            waitForElement(Locator.name("ff_name0"), WAIT_FOR_JAVASCRIPT);
            setFormElement("ff_name0", CUSTOM_PROPERTY);
            setFormElement("url", "http://labkey.test/?a=${"+CUSTOM_PROPERTY+"}&b=${"+COLUMN_NAME+"}");
            addLookupField(null, 1, COLUMN_NAME, COLUMN_NAME, new ListHelper.LookupInfo(PROJECT_NAME, "lists", LIST_NAME));
            clickNavButton("Save & Close");

            waitForText("Last Modified", WAIT_FOR_JAVASCRIPT);
            ExtHelper.waitForFileGridReady(this);
            ExtHelper.waitForFileAdminEnabled(this);
            pipelineHelper.goToAdminMenu();

            // enable custom file properties.
            ExtHelper.clickExtTab(this, "File Properties");
            checkRadioButton("fileOption", "useCustom");

            // Modify toolbar.
            ExtHelper.clickExtTab(this, "Toolbar and Grid Settings");
            waitForText("Configure Grid columns and Toolbar");

            waitForText("Import Data");
            waitForText("file1.xls");
            Locator folderBtn = Locator.xpath("//div[contains(@class, 'test-custom-toolbar')]//button[contains(@class, 'iconFolderNew')]");
            waitForElement(folderBtn, WAIT_FOR_JAVASCRIPT);
            click(folderBtn);
            click(Locator.xpath("//a[./span[text()='remove']]")); // Remove upload button
            click(Locator.xpath("//div[contains(@class, 'test-custom-toolbar')]//button[contains(@class, 'iconUp')]"));
            click(Locator.xpath("//a[./span[text()='show/hide text']]")); // Add text to 'Parent Folder' button

            // Save settings.
            clickButton("Submit", 0);
            waitForExtMaskToDisappear();

            // Verify custom action buttons
            waitForElementToDisappear(Locator.xpath("//button[contains(@class, 'iconFolderNew')]"), WAIT_FOR_JAVASCRIPT);
            assertElementPresent(Locator.xpath("//button[text()='Parent Folder']"));


            //TODO: Re-add new folder button to test adding new button. Fails on TeamCity
            // Re-add upload button
//            clickButton("Admin", 0);
//            ExtHelper.waitForExtDialog(this, "Manage File Browser Configuration", 5000);
//            clickButton("Submit", 0);
            pipelineHelper.addCreateFolderButton();
            waitForExtMaskToDisappear();
            waitForElement(Locator.xpath("//button[contains(@class, 'iconFolderNew')]"), WAIT_FOR_JAVASCRIPT);
            
            String filename = "InlineFile.html";
            String sampleRoot = getLabKeyRoot() + "/sampledata/security";
            File f = new File(sampleRoot, filename);
            pipelineHelper.uploadFile(f, FILE_DESCRIPTION, CUSTOM_PROPERTY_VALUE, LOOKUP_VALUE_2);

            assertLinkPresentWithText(LOOKUP_VALUE_2);
            assertLinkPresentWithText(CUSTOM_PROPERTY_VALUE);
            assertAttributeEquals(Locator.linkWithText(CUSTOM_PROPERTY_VALUE), "href", "http://labkey.test/?a="+CUSTOM_PROPERTY_VALUE+"&b="+LOOKUP_VALUE_2);

            log("rename file");
            String newFileName = "changedFilename.html";
            pipelineHelper.renameFile(filename, newFileName);
            filename = newFileName;

            log("move file");
            String folderName = "Test folder";
            pipelineHelper.createFolder(folderName);
            pipelineHelper.moveFile(newFileName, folderName);



            // Check custom actions as non-administrator.
            impersonate(TEST_USER);
            clickLinkWithText(PROJECT_NAME);
            waitForElementToDisappear(Locator.xpath("//button[text()='Import Data']"), WAIT_FOR_JAVASCRIPT);

            stopImpersonating();

            signOut();

            // Test that renderAs can be observed through a login
            beginAt("files/" + EscapeUtil.encode(PROJECT_NAME)+"/%40files/" + EscapeUtil.encode(folderName) + "/" + filename + "?renderAs=INLINE");
            assertTitleEquals("Sign In");

            log("Test renderAs through login and ensure that page is rendered inside of server UI");
            // If this succeeds, then page has been rendered in frame
            simpleSignIn();

            assertTextPresent("antidisestablishmentarianism");

            clickLinkWithText(PROJECT_NAME);

            SearchHelper.enqueueSearchItem(filename, true, Locator.linkContainingText(filename));
            SearchHelper.enqueueSearchItem(FILE_DESCRIPTION, true, Locator.linkContainingText(filename));
            SearchHelper.enqueueSearchItem(CUSTOM_PROPERTY_VALUE, true,  Locator.linkContainingText(filename));

            SearchHelper.verifySearchResults(this, "/" + PROJECT_NAME + "/@files/" + folderName, false);

            // Delete file.
            clickLinkWithText(PROJECT_NAME);
            ExtHelper.selectFileBrowserItem(this, folderName + "/" + filename);
            click(Locator.css("button.iconDelete"));
            clickButton("Yes", 0);
            waitForElementToDisappear(Locator.xpath("//*[text()='"+filename+"']"), 5000);
                  
            clickButton("Audit History");
            assertTextPresent("File uploaded to project: /" + PROJECT_NAME);
            assertTextPresent("annotations updated: "+CUSTOM_PROPERTY+"="+CUSTOM_PROPERTY_VALUE);
            assertTextPresent("File deleted from project: /" + PROJECT_NAME);

            beginAt(getBaseURL()+"/filecontent/" + PROJECT_NAME + "/sendShortDigest.view");
            goToModule("Dumbster");
            assertTextNotPresent(TEST_USER);  // User opted out of notifications
            clickLinkWithText("File Management Notification", false);
            assertTextBefore("File uploaded", "annotations updated");
            assertTextBefore("annotations updated", "File deleted");

            validateLabAuditTrail();
        }
    }
}
