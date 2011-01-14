/*
 * Copyright (c) 2010 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.drt.StudyTest;
import org.labkey.test.util.ListHelper;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: Trey Chadick
 * Date: Apr 27, 2010
 * Time: 9:10:47 AM
 */

public class SearchTest extends StudyTest
{
    private static final String PROJECT_NAME = "SearchTest Project";
    private static final String FOLDER_A = "Folder Apple";
    private static final String FOLDER_B = "Folder Banana"; // Folder move destination
    private static final String FOLDER_C = "Folder Cherry"; // Folder rename name.
    private static final String GROUP_NAME = "Test Group";
    private static final String USER1 = "user1@search.test";

    private static final String WIKI_NAME = "Brie";
    private static final String WIKI_TITLE = "Roquefort";
    private static final String WIKI_CONTENT = "Stilton";

    private static final String ISSUE_TITLE = "Sedimentary";
    private static final String ISSUE_BODY = "Igneous";

    private static final String MESSAGE_TITLE = "King";
    private static final String MESSAGE_BODY = "Queen";

    private String FOLDER_NAME = FOLDER_A;
    private boolean _testDone = false;
    private static final String GRID_VIEW_NAME = "DRT Eligibility Query";
    private static final String REPORT_NAME = "TestReport";

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/search";
    }

    @Override
    public boolean isFileUploadTest()
    {
        return true;
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected String getFolderName()
    {
        return FOLDER_NAME;
    }

    protected void doCreateSteps()
    {
        SearchHelper.deleteIndex(this);
        addSearchableStudy(); // Must come first;  Creates project.
        addSearchableContainers();
        addSearchableList();
        //addSearchableReports(); // Reports not currently indexed.
        addSearchableWiki();
        addSearchableIssues();
        //addSearchableMessages();
        if (isFileUploadAvailable())
        {
            // TODO: enable once files move with their container
            addSearchableFiles();
        }
    }

    protected void doVerifySteps()
    {
        sleep(10000); // wait for indexer
        SearchHelper.verifySearchResults(this, "/" + getProjectName() + "/" + getFolderName(), false);
        renameFolder(getProjectName(), getFolderName(), FOLDER_C, false);
        FOLDER_NAME = FOLDER_C;
        sleep(10000); // wait for indexer
        SearchHelper.verifySearchResults(this, "/" + getProjectName() + "/" + getFolderName(), false);
        moveFolder(getProjectName(), getFolderName(), FOLDER_B, false);
        SearchHelper.verifySearchResults(this, "/" + getProjectName() + "/" + FOLDER_B + "/" + getFolderName(), false);

        _testDone = true;
    }

    @Override
    protected void doCleanup() throws Exception
    {
        try {deleteProject(getProjectName());} catch (Throwable t) {}
        if(_testDone)
        {
            sleep(5000); // wait for index to update.
            SearchHelper.verifyNoSearchResults(this);
        }
        super.doCleanup();
    }

    private void addSearchableContainers()
    {
        clickLinkWithText(getProjectName());
        createSubfolder(getProjectName(), getProjectName(), FOLDER_B, "None", null);
    }

    private void addSearchableStudy()
    {
        super.doCreateSteps(); // import study and specimens

        // Enable dumbster to prevent errors caused by undeliverable notification emails.
        enableModule(getProjectName(), "Dumbster");
        addWebPart("Mail Record");
        uncheckCheckbox("emailRecordOn");
        checkCheckbox("emailRecordOn");

        SearchHelper.enqueueSearchItem("999320016", Locator.linkContainingText("999320016"));
        SearchHelper.enqueueSearchItem("Urinalysis", Locator.linkContainingText("URF-1"),
                                                     Locator.linkContainingText("URF-2"),
                                                     Locator.linkContainingText("URS-1"));
    }

    private void addSearchableList()
    {
        clickLinkWithText(getFolderName());
        ListHelper.importListArchive(this, getFolderName(), new File(getLabKeyRoot(), "/sampledata/rlabkey/listArchive.zip"));

        SearchHelper.enqueueSearchItem("AllTypes*", //Locator.linkWithText("List AllTypes"), // TODO: enable when stemming/wildcard issue is resolved.
                                                    Locator.linkWithText("List AllTypesCategories"),
                                                    Locator.linkWithText("List AllTypesCategoryGroups"),
                                                    Locator.linkWithText("List AllTypesComments"));
    }

    private void addSearchableReports()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("DEM-1: Demographics");

        clickMenuButton("Views", "Create", "Crosstab View");
        selectOptionByValue("rowField",  "DEMsex");
        selectOptionByValue("colField", "DEMsexor");
        selectOptionByValue("statField", "MouseId");
        clickNavButton("Submit");

        String[] row3 = new String[] {"Male", "2", "9", "3", "14"};
        assertTableRowsEqual("report", 3, new String[][] {row3});

        setFormElement("label", REPORT_NAME);
        clickNavButton("Save");

        // create new grid view report:
        clickMenuButton("Views", "Manage Views");
        clickExtMenuButton("Create", "Grid View");
        setFormElement("label", GRID_VIEW_NAME);
        selectOptionByText("params", "ECI-1: Eligibility Criteria");
        clickNavButton("Create View");

        // create new external report
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("DEM-1: Demographics");
        clickMenuButton("Views", "Create", "Advanced View");
        selectOptionByText("queryName", "DEM-1: Demographics");
        String java = System.getProperty("java.home") + "/bin/java";
        setFormElement("commandLine", java + " -cp " + getLabKeyRoot() + "/server/test/build/classes org.labkey.test.util.Echo ${DATA_FILE} ${REPORT_FILE}");
        clickNavButton("Submit");
        assertTextPresent("Female");
        setFormElement("commandLine", java + " -cp " + getLabKeyRoot() + "/server/test/build/classes org.labkey.test.util.Echo ${DATA_FILE}");
        selectOptionByValue("fileExtension", "tsv");
        clickNavButton("Submit");
        assertTextPresent("Female");
        setFormElement("label", "tsv");
        selectOptionByText("showWithDataset", "DEM-1: Demographics");
        clickNavButton("Save");
    }

    private void addSearchableWiki()
    {
        clickLinkWithText(getFolderName());
        addWebPart("Wiki");
        createNewWikiPage("RADEOX");

        setFormElement("name", WIKI_NAME);
        setFormElement("title", WIKI_TITLE);
        setFormElement("body", WIKI_CONTENT);
        if (isFileUploadAvailable())
        {
            File file = new File(getLabKeyRoot() + "/server/version.properties");
            setFormElement("formFiles[0]", file);
        }
        saveWikiPage();


        SearchHelper.enqueueSearchItem(WIKI_NAME, Locator.linkWithText(WIKI_TITLE));
        SearchHelper.enqueueSearchItem(WIKI_TITLE, Locator.linkWithText(WIKI_TITLE));
        SearchHelper.enqueueSearchItem(WIKI_CONTENT, Locator.linkWithText(WIKI_TITLE));
        SearchHelper.enqueueSearchItem("Master version", Locator.linkWithText("\"version.properties\" attached to page \"" + WIKI_TITLE + "\"")); // some text from attached file
    }

    private void addSearchableIssues()
    {
        createPermissionsGroup(GROUP_NAME, USER1);
        clickNavButton("Save and Finish");
        clickLinkWithText(getFolderName());
        addWebPart("Issues Summary");

        // Setup issues options.
        clickLinkWithText("Issues Summary");
        clickNavButton("Admin");

        // Add Area
        setFormElement(Locator.formElement("addArea", "keyword"), "Area51");
        clickNavButton("Add Area");

        // Add Type
        setFormElement(Locator.formElement("addType", "keyword"), "UFO");
        clickNavButton("Add Type");

        // Create new issue.
        clickNavButton("Back to Issues");
        clickNavButton("New Issue");
        setFormElement("title", ISSUE_TITLE);
        selectOptionByText("type", "UFO");
        selectOptionByText("area", "Area51");
        selectOptionByText("priority", "1");
        setFormElement("comment", ISSUE_BODY);
        selectOptionByText("assignedTo", USER1);
        if (isFileUploadAvailable())
        {
            clickLinkWithText("Attach a file", false);
            File file = new File(getLabKeyRoot() + "/common.properties");
            setFormElement("formFiles[0]", file);
        }
        clickNavButton("Submit");

        SearchHelper.enqueueSearchItem(ISSUE_TITLE, Locator.linkContainingText(ISSUE_TITLE));
        SearchHelper.enqueueSearchItem(ISSUE_BODY, Locator.linkContainingText(ISSUE_TITLE));
        SearchHelper.enqueueSearchItem(USER1, Locator.linkContainingText(ISSUE_TITLE));
        SearchHelper.enqueueSearchItem("Area51", Locator.linkContainingText(ISSUE_TITLE));
        SearchHelper.enqueueSearchItem("UFO", Locator.linkContainingText(ISSUE_TITLE));
        //SearchHelper.enqueueSearchItem("Override", Locator.linkWithText("\"common.properties\" attached to issue \"" + ISSUE_TITLE + "\"")); // some text from attached file
    }

    private void addSearchableMessages()
    {
        clickLinkWithText(getFolderName());
        addWebPart("Messages");
        clickLinkWithText("new message");
        setFormElement("title", MESSAGE_TITLE);
        setFormElement("body", MESSAGE_BODY);
        if (isFileUploadAvailable())
        {
            clickLinkWithText("Attach a file", false);
            File file = new File(getLabKeyRoot() + "/sampledata/dataloading/excel/fruits.tsv");
            setFormElement("formFiles[0]", file);
        }
        clickNavButton("Submit");

        SearchHelper.enqueueSearchItem(MESSAGE_TITLE, Locator.linkContainingText(MESSAGE_TITLE));
        SearchHelper.enqueueSearchItem(MESSAGE_BODY, Locator.linkContainingText(MESSAGE_TITLE));
        SearchHelper.enqueueSearchItem("persimmon", Locator.linkContainingText("\"fruits.tsv\" attached to message \"" + MESSAGE_TITLE + "\"")); // some text from attached file
    }

    private void addSearchableFiles()
    {
        clickLinkWithText(getFolderName());
        goToModule("FileContent");
        File file = new File(getLabKeyRoot() + "/sampledata/security", "InlineFile.html");
        File MLfile = new File(getLabKeyRoot() + "/sampledata/mzxml", "test_nocompression.mzXML");

        uploadFile(file);
        uploadFile(MLfile);

        SearchHelper.enqueueSearchItem("antidisestablishmentarianism", true, Locator.linkWithText(file.getName()));
        SearchHelper.enqueueSearchItem("ThermoFinnigan", true, Locator.linkWithText(MLfile.getName()));
    }

    private void uploadFile(File f)
    {
        waitFor(new Checker() {
            public boolean check()
            {
                return getFormElement(Locator.xpath("//label[text() = 'Choose a file:']//..//input[contains(@class, 'x-form-file-text')]")).equals("");
            }
        }, "Upload field did not clear after upload.", WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//label[text() = 'Choose a file:']//..//input[@class = 'x-form-file']"), f.toString());
        clickNavButton("Upload", 0);
        waitForText(f.getName(), 10000);
    }
}
