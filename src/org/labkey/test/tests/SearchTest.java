/*
 * Copyright (c) 2010-2012 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.SearchHelper;
import org.labkey.test.util.WikiHelper;

import java.io.File;
import java.util.HashMap;

/**
 * User: Trey Chadick
 * Date: Apr 27, 2010
 * Time: 9:10:47 AM
 */

public class SearchTest extends StudyTest
{
    private final SearchHelper _searchHelper = new SearchHelper(this);
    
    private static final String PROJECT_NAME = "SearchTest Project";
    private static final String FOLDER_A = "Folder Apple";
    private static final String FOLDER_B = "Folder Banana"; // Folder move destination
    private static final String FOLDER_C = "Folder Cherry"; // Folder rename name.
    private static final String GROUP_NAME = "Test Group";
    private static final String USER1 = "user1_searchtest@search.test";
    private static final String USER1_DISPLAY_NAME = displayNameFromEmail(USER1);

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
    protected String getFolderName()
    {
        return FOLDER_NAME;
    }

    protected void doCreateSteps()
    {
        _searchHelper.deleteIndex();
        addSearchableStudy(); // Must come first;  Creates project.
        addSearchableLists();
        addSearchableContainers();
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

    String fullySearchableList = "List1";       //index both number and text colums
    String textOnlySearchableList = "List2";    //index text columns only
    String metaOnlySearchable = "MetaDataSet";  //index metadata only
    String customizedIndexingList =  "CustomIndexing";  //index one text column but not another
    String listToDelete = "List To Delete";
    String listIndexAsWhole = "Indexed as one doc";

    private void addSearchableLists()
    {
        clickTab("Overview");
        addWebPart("Lists");
        _listHelper.importListArchive(FOLDER_A, new File(getLabKeyRoot() + getStudySampleDataPath() + "/searchTest.lists.zip"));


        clickLinkWithText(listToDelete);
        _listHelper.deleteList();

        _searchHelper.enqueueSearchItem("BoarQPine");
        _searchHelper.enqueueSearchItem("Panda", Locator.bodyLinkWithText("List " + fullySearchableList));
        _searchHelper.enqueueSearchItem("2003-01-02", Locator.bodyLinkWithText("List " + fullySearchableList));
        _searchHelper.enqueueSearchItem("12345", Locator.bodyLinkWithText("List " + fullySearchableList));  //Issue 15419
        _searchHelper.enqueueSearchItem("Owlbear", Locator.bodyLinkWithText("List " + textOnlySearchableList));
        _searchHelper.enqueueSearchItem("54321");
        _searchHelper.enqueueSearchItem(metaOnlySearchable, Locator.bodyLinkWithText("List " + metaOnlySearchable));
        _searchHelper.enqueueSearchItem("Turtleduck", Locator.bodyLinkWithText("List " + metaOnlySearchable)); //this phrase is present in the metadata-only file
        _searchHelper.enqueueSearchItem("Cat", Locator.bodyLinkWithText("List " + customizedIndexingList));
        _searchHelper.enqueueSearchItem("Garfield");
    }

    protected void doVerifySteps()
    {
        sleep(10000); // wait for indexer
        _searchHelper.verifySearchResults("/" + getProjectName() + "/" + getFolderName(), false);
        renameFolder(getProjectName(), getFolderName(), FOLDER_C, false);
        FOLDER_NAME = FOLDER_C;
        sleep(10000); // wait for indexer
        _searchHelper.verifySearchResults("/" + getProjectName() + "/" + getFolderName(), false);
        moveFolder(getProjectName(), getFolderName(), FOLDER_B, false);
        alterListsAndReSearch();
        _searchHelper.verifySearchResults("/" + getProjectName() + "/" + FOLDER_B + "/" + getFolderName(), false);

        verifySyntaxErrorMessages();

        _testDone = true;
    }

    private void alterListsAndReSearch()
    {
        clickLinkWithText(FOLDER_C);
        clickLinkWithText(listIndexAsWhole);
        HashMap<String, String> data = new HashMap<String, String>();
        String newAnimal = "Zebra Seal";
        data.put("Animal", newAnimal);
        _listHelper.insertNewRow(data);
        goBack();

        _searchHelper.enqueueSearchItem(newAnimal, Locator.linkContainingText(listIndexAsWhole));
//        _searchHelper.verifySearchResults(this, "/" + getProjectName() + "/" + getFolderName(), false);
        log("TODO");
    }

    public void runApiTests() throws Exception
    {
        /* No API tests */
    }
    
    private void verifySyntaxErrorMessages()
    {
        _searchHelper.searchFor("age()");
        checkSyntaxErrorMessage("Error: Can't parse 'age()': Problem character is highlighted", "These characters have special meaning within search queries:", "You can escape special characters using \\ before the character or you can enclose the query string in double quotes.", "For more information, visit the search syntax documentation.");
        _searchHelper.searchFor("incomplete(");
        checkSyntaxErrorMessage("Error: Can't parse 'incomplete(': Query string is incomplete", "These characters have special meaning within search queries:");
        _searchHelper.searchFor("this AND OR");
        checkSyntaxErrorMessage("Error: Can't parse 'this AND OR': Problem character is highlighted", "Boolean operators AND, OR, and NOT have special meaning within search queries");
    }

    private void checkSyntaxErrorMessage(String... expectedPhrases)
    {
        // We want our nice, custom error messages to appear
        assertTextPresent(expectedPhrases);

        // Various phrases that appear in the standard Lucene system error message
        assertTextNotPresent("Cannot parse");
        assertTextNotPresent("encountered");
        assertTextNotPresent("Was expecting");
        assertTextNotPresent("<NOT>");
        assertTextNotPresent("<OR>");
        assertTextNotPresent("<AND>");
        assertTextNotPresent("<EOF>");
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        super.doCleanup(afterTest);
        if (afterTest)
        {
            sleep(5000); // wait for index to update.
            _searchHelper.verifyNoSearchResults();
        }
    }

    private void addSearchableContainers()
    {
        clickFolder(getProjectName());
        createSubfolder(getProjectName(), getProjectName(), FOLDER_B, "None", null);
    }

    private void addSearchableStudy()
    {
        super.doCreateSteps(); // import study and specimens

        _searchHelper.enqueueSearchItem("999320016", Locator.linkContainingText("999320016"));
        _searchHelper.enqueueSearchItem("Urinalysis", Locator.linkContainingText("URF-1"),
                                                     Locator.linkContainingText("URF-2"),
                                                     Locator.linkContainingText("URS-1"));
    }

    private void addSearchableReports()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("DEM-1: Demographics");

        clickMenuButton("Views", "Create", "Crosstab View");
        selectOptionByValue("rowField",  "DEMsex");
        selectOptionByValue("colField", "DEMsexor");
        selectOptionByValue("statField", "MouseId");
        clickButton("Submit");

        String[] row3 = new String[] {"Male", "2", "9", "3", "14"};
        assertTableRowsEqual("report", 3, new String[][] {row3});

        setFormElement("label", REPORT_NAME);
        clickButton("Save");

        // create new grid view report:
        clickMenuButton("Views", "Manage Views");
        clickMenuButton("Create", "Grid View");
        setFormElement("label", GRID_VIEW_NAME);
        selectOptionByText("params", "ECI-1: Eligibility Criteria");
        clickButton("Create View");

        // create new external report
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("DEM-1: Demographics");
        clickMenuButton("Views", "Create", "Advanced View");
        selectOptionByText("queryName", "DEM-1: Demographics");
        String java = System.getProperty("java.home") + "/bin/java";
        setFormElement("commandLine", java + " -cp " + getLabKeyRoot() + "/server/test/build/classes org.labkey.test.util.Echo ${DATA_FILE} ${REPORT_FILE}");
        clickButton("Submit");
        assertTextPresent("Female");
        setFormElement("commandLine", java + " -cp " + getLabKeyRoot() + "/server/test/build/classes org.labkey.test.util.Echo ${DATA_FILE}");
        selectOptionByValue("fileExtension", "tsv");
        clickButton("Submit");
        assertTextPresent("Female");
        setFormElement("label", "tsv");
        selectOptionByText("showWithDataset", "DEM-1: Demographics");
        clickButton("Save");
    }

    private void addSearchableWiki()
    {
        WikiHelper _wikiHelper = new WikiHelper(this);
        
        clickFolder(getFolderName());
        addWebPart("Wiki");
        _wikiHelper.createWikiPage("RADEOX", WIKI_NAME, WIKI_TITLE, WIKI_CONTENT, new File(getLabKeyRoot() + "/server/module.template.properties"));
        addWebPart("Wiki");
        //Issue 9454: Don't index option for wiki page
        _wikiHelper.createWikiPage("RADEOX", WIKI_NAME+"UNSEARCHABLE", WIKI_TITLE, WIKI_CONTENT, false, null);


        _searchHelper.enqueueSearchItem(WIKI_NAME, Locator.linkWithText(WIKI_TITLE));
        _searchHelper.enqueueSearchItem(WIKI_NAME + "UNSEARCHABLE");
        _searchHelper.enqueueSearchItem(WIKI_TITLE, Locator.linkWithText(WIKI_TITLE));
        _searchHelper.enqueueSearchItem(WIKI_CONTENT, Locator.linkWithText(WIKI_TITLE));
        _searchHelper.enqueueSearchItem("moduleDependencies", Locator.linkWithText("\"module.template.properties\" attached to page \"" + WIKI_TITLE + "\"")); // some text from attached file
    }

    private void addSearchableIssues()
    {
        createPermissionsGroup(GROUP_NAME, USER1);
        clickButton("Save and Finish");
        clickFolder(getFolderName());
        addWebPart("Issues Summary");

        // Setup issues options.
        clickLinkWithText("Issues Summary");
        clickButton("Admin");

        // Add Area
        IssuesTest.addKeywordsAndVerify(this, "area", "Area", "Area51");

        // Add Type
        IssuesTest.addKeywordsAndVerify(this, "type", "Type", "UFO");

        // Create new issue.
        clickButton("Back to Issues");
        clickButton("New Issue");
        setFormElement("title", ISSUE_TITLE);
        selectOptionByText("type", "UFO");
        selectOptionByText("area", "Area51");
        selectOptionByText("priority", "1");
        setFormElement("comment", ISSUE_BODY);
        selectOptionByText("assignedTo", USER1_DISPLAY_NAME);
        if (isFileUploadAvailable())
        {
            clickLinkWithText("Attach a file", false);
            File file = new File(getLabKeyRoot() + "/common.properties");
            setFormElement("formFiles[00]", file);
        }
        clickButton("Save");

        _searchHelper.enqueueSearchItem(ISSUE_TITLE, Locator.linkContainingText(ISSUE_TITLE));
        _searchHelper.enqueueSearchItem(ISSUE_BODY, Locator.linkContainingText(ISSUE_TITLE));
        _searchHelper.enqueueSearchItem(USER1_DISPLAY_NAME, Locator.linkContainingText(ISSUE_TITLE));
        _searchHelper.enqueueSearchItem("Area51", Locator.linkContainingText(ISSUE_TITLE));
        _searchHelper.enqueueSearchItem("UFO", Locator.linkContainingText(ISSUE_TITLE));
        //_searchHelper.enqueueSearchItem("Override", Locator.linkWithText("\"common.properties\" attached to issue \"" + ISSUE_TITLE + "\"")); // some text from attached file
    }

    private void addSearchableMessages()
    {
        clickFolder(getFolderName());
        addWebPart("Messages");
        clickWebpartMenuItem("Messages", "New");
        setFormElement("title", MESSAGE_TITLE);
        setFormElement("body", MESSAGE_BODY);
        if (isFileUploadAvailable())
        {
            clickLinkWithText("Attach a file", false);
            File file = new File(getLabKeyRoot() + "/sampledata/dataloading/excel/fruits.tsv");
            setFormElement("formFiles[0]", file);
        }
        clickButton("Submit");

        _searchHelper.enqueueSearchItem(MESSAGE_TITLE, Locator.linkContainingText(MESSAGE_TITLE));
        _searchHelper.enqueueSearchItem(MESSAGE_BODY, Locator.linkContainingText(MESSAGE_TITLE));
        _searchHelper.enqueueSearchItem("persimmon", Locator.linkContainingText("\"fruits.tsv\" attached to message \"" + MESSAGE_TITLE + "\"")); // some text from attached file
    }

    private void addSearchableFiles()
    {
        clickFolder(getFolderName());
        goToModule("FileContent");
        File file = new File(getLabKeyRoot() + "/sampledata/security", "InlineFile.html");
        File MLfile = new File(getLabKeyRoot() + "/sampledata/mzxml", "test_nocompression.mzXML");

        uploadFile(file);
        uploadFile(MLfile);

        _searchHelper.enqueueSearchItem("antidisestablishmentarianism", true, Locator.linkWithText(file.getName()));
        _searchHelper.enqueueSearchItem("ThermoFinnigan", true, Locator.linkWithText(MLfile.getName()));
    }

    private void uploadFile(File f)
    {
        waitFor(new Checker() {
            public boolean check()
            {
                return getFormElement(Locator.xpath("//label[./span[text() = 'Choose a File:']]//..//input[contains(@class, 'x-form-file-text')]")).equals("");
            }
        }, "Upload field did not clear after upload.", WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//label[./span[text() = 'Choose a File:']]//..//input[@class = 'x-form-file']"), f.toString());
        clickButton("Upload", 0);
        waitForText(f.getName(), 10000);
    }
}
