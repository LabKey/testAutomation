/*
 * Copyright (c) 2011-2012 LabKey Corporation
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

package org.labkey.test.module;

import org.junit.Assert;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.test.Locator;
import org.labkey.test.tests.SimpleApiTest;
import org.labkey.test.util.AdvancedSqlTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EHRTestHelper;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LabModuleHelper;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Treygdor
 * Date: Mar 21, 2011
 * Time: 1:59:12 PM
 */
public class EHRStudyTest extends SimpleApiTest implements AdvancedSqlTest
{
    // Project/folder names are hard-coded into some links in the module.
    private static final String PROJECT_NAME = "EHR_TestProject";
    private static final String FOLDER_NAME = "EHR";
    private static final String CONTAINER_PATH = PROJECT_NAME + "/" + FOLDER_NAME;
    private static final String STUDY_ZIP = "/sampledata/study/EHR Study Anon.zip";

    //note: changed by BNB
    protected static final String PROJECT_ID = "640991"; // project with one participant
    protected static final String DUMMY_PROTOCOL = "dummyprotocol"; // need a protocol to create table entry
    protected static final String PROJECT_MEMBER_ID = "test2312318"; // PROJECT_ID's single participant
    protected static final String ROOM_ID = "6824778"; // room of PROJECT_MEMBER_ID
    protected static final String CAGE_ID = "4434662"; // cage of PROJECT_MEMBER_ID

    protected static final String AREA_ID = "A1/AB190"; // arbitrary area
    protected static final String PROTOCOL_PROJECT_ID = "795644"; // Project with exactly 3 members
    protected static final String PROTOCOL_ID = "protocol101";
    protected static final String[] PROTOCOL_MEMBER_IDS = {"test3997535", "test4551032", "test5904521"}; //{"test2008446", "test3804589", "test4551032", "test5904521", "test6390238"}; // Protocol members, sorted ASC alphabetically
    protected static final String[] MORE_ANIMAL_IDS = {"test1020148","test1099252","test1112911","test727088","test4564246"}; // Some more, distinct, Ids
    protected static final String DEAD_ANIMAL_ID = "test9118022";
    protected static final EHRUser DATA_ADMIN = new EHRUser("admin@ehrstudy.test", "EHR Administrators", EHRRole.DATA_ADMIN);
    protected static final EHRUser REQUESTER = new EHRUser("requester@ehrstudy.test", "EHR Requestors", EHRRole.REQUESTER);
    protected  static final EHRUser BASIC_SUBMITTER = new EHRUser("basicsubmitter@ehrstudy.test", "EHR Basic Submitters", EHRRole.BASIC_SUBMITTER);
    protected  static final EHRUser FULL_SUBMITTER = new EHRUser("fullsubmitter@ehrstudy.test", "EHR Full Submitters", EHRRole.FULL_SUBMITTER);
    protected  static final EHRUser REQUEST_ADMIN = new EHRUser("request_admin@ehrstudy.test", "EHR Request Admins", EHRRole.REQUEST_ADMIN);
    protected  static final EHRUser FULL_UPDATER = new EHRUser("full_updater@ehrstudy.test", "EHR Full Updaters", EHRRole.FULL_UPDATER);
    protected  static final String TASK_TITLE = "Test weight task";
    protected  static final String MPR_TASK_TITLE = "Test MPR task";

    protected  static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    //xpath fragment
    public static final String VISIBLE = "not(ancestor-or-self::*[contains(@style,'visibility: hidden') or contains(@class, 'x-hide-display')])";

    private EHRTestHelper _helper = new EHRTestHelper(this);
    public static class EHRUser
    {
        private final String _userId;
        private final String _groupName;
        private final EHRRole _role;
        
        public EHRUser(String userId, String groupName, EHRRole role)
        {
            _userId = userId;
            _groupName = groupName;
            _role = role;
        }
        
        public String getUser()
        {
            return _userId;
        }
        
        public String getGroup()
        {
            return _groupName;
        }
        
        public EHRRole getRole()
        {
            return _role;
        }
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/customModules/ehr";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    public boolean enableLinkCheck()
    {
        if ( super.enableLinkCheck() )
            log("EHR test has too many hard coded links and special actions to crawl effectively. Skipping crawl.");
        return false;
    }

    @Override
    public void validateQueries()
    {
        //NOTE: the queries are also validated as part of study import
        //also, validation takes place on the project root, while the EHR and required datasets are loaded into a subfolder
        log("Skipping query validation.");
    }

    @Override
    protected Pattern[] getIgnoredElements()
    {
        return new Pattern[] {
            Pattern.compile("qcstate", Pattern.CASE_INSENSITIVE),//qcstate IDs aren't predictable
            Pattern.compile("stacktrace", Pattern.CASE_INSENSITIVE)
        };
    }

    @Override
    protected File[] getTestFiles()
    {
        return new File[0];
    }

    @Override
    public void doCleanup(boolean afterTest)
    {
        long startTime = System.currentTimeMillis();
        try{deleteRecords();}catch(Throwable T){}
        try {deleteProject(PROJECT_NAME);} catch (Throwable t) { /*ignore*/ }
        if(isTextPresent(PROJECT_NAME))
        {
            log("Wait extra long for folder to finish deleting.");
            while (isTextPresent(PROJECT_NAME) && System.currentTimeMillis() - startTime < 300000) // 5 minutes max.
            {
                sleep(5000);
                refresh();
            }
            if (!isTextPresent(PROJECT_NAME)) log("Test Project deleted in " + (System.currentTimeMillis() - startTime) + "ms");
            else Assert.fail("Test Project not finished deleting after 5 minutes");
        }
        goToHome();
        try{deleteUser(DATA_ADMIN.getUser());}catch(Throwable T){}
        try{deleteUser(REQUESTER.getUser());}catch(Throwable T){}
        try{deleteUser(BASIC_SUBMITTER.getUser());}catch(Throwable T){}
        try{deleteUser(REQUEST_ADMIN.getUser());}catch(Throwable T){}
        try{deleteUser(FULL_UPDATER.getUser());}catch(Throwable T){}
        try{deleteUser(FULL_SUBMITTER.getUser());}catch(Throwable T){}
    }

    @Override
    public void runUITests()
    {
        initProject();
        setupEhrPermissions();
        defineQCStates();

        detailsPagesTest();
        viewsTest();
        animalHistoryTest();
        quickSearchTest();
        weightDataEntryTest();
        mprDataEntryTest();
    }

    private void initProject()
    {
        enableEmailRecorder();

        _containerHelper.createProject(PROJECT_NAME, "EHR");
        createSubfolder(PROJECT_NAME, PROJECT_NAME, FOLDER_NAME, "EHR", null);

        //set dummy values first, to test the admin UI
        String[] dummyProps = {"/" +  PROJECT_NAME, "EHRStudyContainer", "/fakeContainer"};
        setModuleProperties(Collections.singletonMap("EHR", Collections.singletonList(dummyProps)));

        String[] prop = {"/" + PROJECT_NAME, "EHRStudyContainer", "/" + CONTAINER_PATH};
        setModuleProperties(Collections.singletonMap("EHR", Collections.singletonList(prop)));

        clickLinkWithText(FOLDER_NAME);
        beginAt(getBaseURL()+"/ehr/"+CONTAINER_PATH+"/_initEHR.view");
        clickButton("Delete All", 0);
        waitForText("Delete Complete", 120000);
        clickButton("Populate All", 0);
        waitForText("Populate Complete", 120000);

        //these tables do not have a container field, so are not deleted when the test project is deleted
        clickButton("Delete Data From SNOMED", 0);
        waitForText("Delete Complete", 120000);
        clickButton("Populate SNOMED Table", 0);
        waitForText("Populate Complete", 120000);

        goToModule("Study");
        importStudyFromZip(new File(getLabKeyRoot() + STUDY_ZIP).getPath());
        try
        {
            deleteRecords();
            populateRecords();
        }
        catch (Throwable e)
        {
            //ignore for now
            log("There was an error");
        }

        log("Remove all webparts");
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);

        addWebPart("Quick Search");
    }

    private void populateRecords() throws Exception
    {
        log("Inserting initial records into EHR hard tables");

        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        //first ehr.protocol
        InsertRowsCommand insertCmd = new InsertRowsCommand("ehr", "protocol");
        Map<String,Object> rowMap = new HashMap<String,Object>();
        rowMap.put("protocol", PROTOCOL_ID);
        insertCmd.addRow(rowMap);
        rowMap = new HashMap<String,Object>();
        rowMap.put("protocol", DUMMY_PROTOCOL);
        insertCmd.addRow(rowMap);
        SaveRowsResponse saveResp = insertCmd.execute(cn, CONTAINER_PATH);

        //then ehr.project
        insertCmd = new InsertRowsCommand("ehr", "project");
        rowMap = new HashMap<String,Object>();
        rowMap.put("project", PROTOCOL_PROJECT_ID);
        rowMap.put("protocol", PROTOCOL_ID);
        insertCmd.addRow(rowMap);
        rowMap = new HashMap<String,Object>();
        rowMap.put("project", PROJECT_ID);
        rowMap.put("protocol", DUMMY_PROTOCOL);
        insertCmd.addRow(rowMap);
        saveResp = insertCmd.execute(cn, CONTAINER_PATH);
    }

    private void deleteRecords() throws Exception
    {
        log("Deleting initial records from EHR hard tables");

        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());

        //first ehr.protocol
        DeleteRowsCommand deleteCmd = new DeleteRowsCommand("ehr", "protocol");
        Map<String,Object> rowMap = new HashMap<String,Object>();
        rowMap.put("protocol", PROTOCOL_ID);
        deleteCmd.addRow(rowMap);
        rowMap = new HashMap<String,Object>();
        rowMap.put("protocol", DUMMY_PROTOCOL);
        deleteCmd.addRow(rowMap);
        SaveRowsResponse deleteResp = deleteCmd.execute(cn, CONTAINER_PATH);

        //then ehr.project
        deleteCmd = new DeleteRowsCommand("ehr", "project");
        rowMap = new HashMap<String,Object>();
        rowMap.put("project", PROTOCOL_PROJECT_ID);
        rowMap.put("protocol", PROTOCOL_ID);
        deleteCmd.addRow(rowMap);
        rowMap = new HashMap<String,Object>();
        rowMap.put("project", PROJECT_ID);
        rowMap.put("protocol", DUMMY_PROTOCOL);
        deleteCmd.addRow(rowMap);
        deleteResp = deleteCmd.execute(cn, CONTAINER_PATH);
    }

    /**
     * This test will hit a variety of EHR views and provides a very basic test of the UI on that page.  Initially it will
     * just look for JS errors and certain keywords, like 'error' or 'failed'.
     */
    private void detailsPagesTest()
    {
        String VIEW_TEXT = "Browse All";

        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitForPageToLoad();
        waitAndClick(Locator.linkWithText("Browse All Datasets"));
        waitForPageToLoad();

        waitForText("Biopsies");
        waitAndClick(LabModuleHelper.getNavPanelItem("Biopsies:", VIEW_TEXT));
        waitForPageToLoad();

        waitForText("details");
        DataRegionTable dr = new DataRegionTable("query", this);
        dr.clickLink(0, 0);
        //these are the sections we expect
        waitForText("Biopsy Details");
        waitForText("Morphologic Diagnoses");
        waitForText("Histology");
        assertNoErrorText();

        beginAt("/ehr/" + CONTAINER_PATH + "/datasets.view");
        waitForPageToLoad();
        waitForText("Necropsies");
        waitAndClick(LabModuleHelper.getNavPanelItem("Necropsies:", VIEW_TEXT));
        waitForPageToLoad();
        waitForText("details");
        dr = new DataRegionTable("query", this);
        dr.clickLink(0, 0);
        //these are the sections we expect
        waitForText("Necropsy Details");
        waitForText("Morphologic Diagnoses");
        waitForText("Histology");
        assertNoErrorText();

        beginAt("/ehr/" + CONTAINER_PATH + "/datasets.view");
        waitForPageToLoad();
        waitForText("Drug Administration");
        waitAndClick(LabModuleHelper.getNavPanelItem("Drug Administration:", VIEW_TEXT));
        waitForPageToLoad();
        waitForText("details");
        dr = new DataRegionTable("query", this);
        dr.clickLink(0, 0);
        //these are the sections we expect
        waitForText("Drug Details");
        waitForText("Clinical Remarks From ");
        assertNoErrorText();

        beginAt("/ehr/" + CONTAINER_PATH + "/datasets.view");
        waitForPageToLoad();
        waitForText("Housing");
        waitAndClick(LabModuleHelper.getNavPanelItem("Housing:", VIEW_TEXT));
        waitForPageToLoad();
        waitForText("details");
        dr = new DataRegionTable("query", this);
        dr.clickLink(1, "Room");
        waitForPageToLoad();
        //these are the sections we expect
        waitForText("Cage Details");
        waitForText("Animals Currently Housed");
        waitForText("Cage Observations For This Location");
        waitForText("All Animals Ever Housed");
        assertNoErrorText();

        beginAt("/ehr/" + CONTAINER_PATH + "/datasets.view");
        waitForPageToLoad();
        waitForText("Clinpath Runs");
        waitAndClick(LabModuleHelper.getNavPanelItem("Clinpath Runs:", VIEW_TEXT));
        waitForPageToLoad();
        waitForText("details");
        dr = new DataRegionTable("query", this);
        dr.clickLink(0, 0);
        //these are the sections we expect
        waitForText("Run Details");
        waitForText("Bacteriology Results");
        waitForText("Chemistry Results");
        waitForText("Immunology Results");
        waitForText("Hematology Results");
        waitForText("Hematology Morphology");
        waitForText("Parasitology Results");
        waitForText("Urinalysis Results");
        waitForText("Virology Results");
        assertNoErrorText();

        //TODO: clinical encounters details page




    }

    private void dataRegionButtonTest()
    {
        //TODO: check custom buttons

        //TODO: also check that delete, import, etc do not appear unless explicitly enabled
    }

    /**
     * This tests misc views that are not included in detailsPagesTest()
     */
    public void viewsTest()
    {
        //housing queries
        beginAt("/project/" + CONTAINER_PATH + "/begin.view");
        waitForPageToLoad();
        waitAndClick(Locator.linkWithText("Housing Queries"));
        waitForPageToLoad();
        waitForText("View:"); //a proxy for the search panel loading


        //animal queries
        beginAt("/project/" + CONTAINER_PATH + "/begin.view");
        waitForPageToLoad();
        waitAndClick(Locator.linkWithText("Animal Search"));
        waitForPageToLoad();
        waitForTextToDisappear("Loading");
        waitForText("View:"); //a proxy for the search panel loading
        assertNoErrorText();
        //TODO: test search plus specific queries

        //project, protocol queries
        beginAt("/project/" + CONTAINER_PATH + "/begin.view");
        waitForPageToLoad();
        waitAndClick(Locator.linkWithText("Protocol and Project Queries"));
        waitForPageToLoad();
        waitForTextToDisappear("Loading");
        waitForText("View:"); //a proxy for the search panel loading
        assertNoErrorText();
        //TODO: test search plus specific queries

        //population overview
        beginAt("/project/" + CONTAINER_PATH + "/begin.view");
        waitForPageToLoad();
        waitAndClick(Locator.linkWithText("Population Summary"));
        waitForPageToLoad();
        waitForText("Current Population Counts");
        waitForText("Population Changes");
        waitForText("Other Population Queries");
        //TODO: test R report plus other queries
        assertNoErrorText();
    }

    /**
     * This is designed to test several queries that are mainly used as reports.  We will insert and query specific data designed to test
     * problematic conditions
     */
    public void queriesTest()
    {

    }

    private void assertNoErrorText()
    {
        assertTextNotPresent("error", "Error", "ERROR", "failed", "Failed", "Invalid", "invalid");
    }

    private void animalHistoryTest()
    {
        String dataRegionName;
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);

        waitAndClick(Locator.linkWithText("Animal History"));
        waitForPageToLoad();

        log("Verify Single animal history");
        waitForElement(Locator.raw("subjectBox"));
        setFormElement("subjectBox", PROTOCOL_MEMBER_IDS[0]);
        refreshAnimalHistoryReport();
        waitForElement(Locator.linkWithText(PROTOCOL_MEMBER_IDS[0]), WAIT_FOR_JAVASCRIPT);

        //crawlReportTabs(); // TOO SLOW. TODO: Enable when performance is better.

        //NOTE: rendering the entire colony is slow, so instead of abstract we load a simpler report
        log("Verify Entire colony history");
        waitAndClick(Locator.ext4Radio("Entire Database"));
        _ext4Helper.clickTabContainingText("Demographics");
        waitForText("Rhesus"); //a proxy for the loading of the dataRegion
        dataRegionName = _helper.getAnimalHistoryDataRegionName("Demographics");
        Assert.assertEquals("Did not find the expected number of Animals", 44, getDataRegionRowCount(dataRegionName));

        log("Verify location based history");
        waitAndClick(Locator.ext4Radio("Current Location"));
        _ext4Helper.selectComboBoxItem("Area", AREA_ID);
        _ext4Helper.queryOne("#roomField", Ext4FieldRef.class).setValue(ROOM_ID);
        _ext4Helper.queryOne("#cageField", Ext4FieldRef.class).setValue(CAGE_ID);
        _ext4Helper.clickTabContainingText("Abstract");
        // No results expected due to anonymized cage info.
        waitForText("No records found", WAIT_FOR_JAVASCRIPT);

        log("Verify Project search");
        waitAndClick(Locator.ext4Radio("Multiple Animals"));
        waitAndClick(Locator.xpath("//a[text()='[Search By Project/Protocol]']"));
        waitForElement(Ext4Helper.ext4Window("Search By Project/Protocol"));
        _ext4Helper.selectComboBoxItem("Project", PROJECT_ID);
        clickButton("Submit", 0);
        waitForElement(Locator.ext4Button(PROJECT_MEMBER_ID + " (X)"), WAIT_FOR_JAVASCRIPT);
        refreshAnimalHistoryReport();
        waitForElement(Locator.linkWithText(PROJECT_MEMBER_ID), WAIT_FOR_JAVASCRIPT);

        log("Verify Protocol search");
        waitAndClick(Locator.ext4Radio("Multiple Animals"));
        waitAndClick(Locator.xpath("//a[text()='[Search By Project/Protocol]']"));
        waitForElement(Ext4Helper.ext4Window("Search By Project/Protocol"));
        _ext4Helper.selectComboBoxItem("Protocol", PROTOCOL_ID);
        clickButton("Submit", 0);
        waitForElement(Locator.ext4Button(PROTOCOL_MEMBER_IDS[0] + " (X)"), WAIT_FOR_JAVASCRIPT);

        // Check protocol search results.
        refreshAnimalHistoryReport();
        dataRegionName = _helper.getAnimalHistoryDataRegionName("Abstract");
        Assert.assertEquals("Did not find the expected number of Animals", PROTOCOL_MEMBER_IDS.length, getDataRegionRowCount(dataRegionName));
        assertLinkPresentWithText(PROTOCOL_MEMBER_IDS[0]);

        // Check animal count after removing one from search.
        waitAndClick(Locator.ext4Button(PROTOCOL_MEMBER_IDS[0] + " (X)"));
        waitForElementToDisappear(Locator.ext4Button(PROTOCOL_MEMBER_IDS[0] + " (X)"), WAIT_FOR_JAVASCRIPT);
        refreshAnimalHistoryReport();
        dataRegionName = _helper.getAnimalHistoryDataRegionName("Abstract");
        Assert.assertEquals("Did not find the expected number of Animals", PROTOCOL_MEMBER_IDS.length - 1, getDataRegionRowCount(dataRegionName));

        // Re-add animal.
        setFormElement("subjectBox",  PROTOCOL_MEMBER_IDS[0]);
        waitAndClick(Locator.ext4Button("Append -->"));
        waitForElement(Locator.button(PROTOCOL_MEMBER_IDS[0] + " (X)"), WAIT_FOR_JAVASCRIPT);
        refreshAnimalHistoryReport();
        dataRegionName = _helper.getAnimalHistoryDataRegionName("Abstract");
        waitForText(PROTOCOL_MEMBER_IDS[0]);
        Assert.assertEquals("Did not find the expected number of Animals", PROTOCOL_MEMBER_IDS.length, getDataRegionRowCount(dataRegionName));

        log("Verify custom actions");
        log("Return Distinct Values - no selections");
        clickMenuButtonAndContinue("More Actions", "Return Distinct Values");
        assertAlert("No records selected");

        log("Return Distinct Values");
        dataRegionName = _helper.getAnimalHistoryDataRegionName("Weight");
        checkAllOnPage(dataRegionName);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Return Distinct Values");
        _extHelper.waitForExtDialog("Return Distinct Values");
        _extHelper.selectComboBoxItem("Select Field:", "Animal Id");
        clickButton("Submit", 0);
        _extHelper.waitForExtDialog("Distinct Values");
        assertFormElementEquals("distinctValues", PROTOCOL_MEMBER_IDS[0]+"\n"+PROTOCOL_MEMBER_IDS[1]+"\n"+PROTOCOL_MEMBER_IDS[2]);
        clickButton("Close", 0);

        log("Return Distinct Values - filtered");
        waitForTextToDisappear("Loading...");
        setFilterAndWait(dataRegionName, "Id", "Does Not Equal", PROTOCOL_MEMBER_IDS[1], 0);
        waitForText("Filter: (Id <> " + PROTOCOL_MEMBER_IDS[1], WAIT_FOR_JAVASCRIPT);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Return Distinct Values");
        _extHelper.waitForExtDialog("Return Distinct Values");
        _extHelper.selectComboBoxItem("Select Field:", "Animal Id");
        clickButton("Submit", 0);
        _extHelper.waitForExtDialog("Distinct Values");
        assertFormElementEquals("distinctValues", PROTOCOL_MEMBER_IDS[0]+"\n"+PROTOCOL_MEMBER_IDS[2]);
        clickButton("Close", 0);

        log("Compare Weights - no selection");
        uncheckAllOnPage(dataRegionName);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Compare Weights");
        assertAlert("No records selected");

        log("Compare Weights - one selection");
        checkDataRegionCheckbox(dataRegionName, 0);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Compare Weights");
        _extHelper.waitForExtDialog("Weights");
        clickButton("OK", 0);

        log("Compare Weights - two selections");
        checkDataRegionCheckbox(dataRegionName, 1);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Compare Weights");
        _extHelper.waitForExtDialog("Weights");
        clickButton("OK", 0);

        log("Compare Weights - three selections");
        checkDataRegionCheckbox(dataRegionName, 2);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Compare Weights");
        _extHelper.waitForExtDialog("Error"); // After error dialog.
        clickButton("OK", 0);

        log("Jump to Other Dataset - no selection");
        uncheckAllOnPage(dataRegionName);
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Jump To Other Dataset");
        assertAlert("No records selected");

        log("Jump to Other Dataset - two selection");
        dataRegionName = _helper.getAnimalHistoryDataRegionName("Abstract");
        checkDataRegionCheckbox(dataRegionName, 0); // PROTOCOL_MEMBER_IDS[0]
        checkDataRegionCheckbox(dataRegionName, 2); // PROTOCOL_MEMBER_IDS[2]
        _extHelper.clickExtMenuButton(false, Locator.xpath("//table[@id='dataregion_"+dataRegionName+"']" +Locator.navButton("More Actions").getPath()), "Jump To Other Dataset");
        _extHelper.selectComboBoxItem("Dataset:", "Blood Draws");
        _extHelper.selectComboBoxItem("Filter On:", "Animal Id");
        clickButton("Submit");
        waitForElement(Locator.linkWithText(PROTOCOL_MEMBER_IDS[0]), WAIT_FOR_JAVASCRIPT);
        assertTextNotPresent(PROTOCOL_MEMBER_IDS[1]);

        log("Jump to History");
        checkDataRegionCheckbox("query", 0); // PROTOCOL_MEMBER_IDS[0]
        clickMenuButton("More Actions", "Jump To History");
        assertTitleContains("Animal History");
        waitAndClick(Locator.ext4Button("Append -->"));
        setFormElement("subjectBox", PROTOCOL_MEMBER_IDS[2]);
        waitAndClick(Locator.ext4Button("Append -->"));
        refreshAnimalHistoryReport();
        dataRegionName = _helper.getAnimalHistoryDataRegionName("Abstract");
        Assert.assertEquals("Did not find the expected number of Animals", 2, getDataRegionRowCount(dataRegionName));
        assertTextPresent(PROTOCOL_MEMBER_IDS[0], PROTOCOL_MEMBER_IDS[2]);

        log("Check subjectBox parsing");
        setFormElement("subjectBox",  MORE_ANIMAL_IDS[0]+","+MORE_ANIMAL_IDS[1]+";"+MORE_ANIMAL_IDS[2]+" "+MORE_ANIMAL_IDS[3]+"\n"+MORE_ANIMAL_IDS[4]);
        clickButton("  Replace -->", 0);
        refreshAnimalHistoryReport();
        dataRegionName = _helper.getAnimalHistoryDataRegionName("Abstract");
        Assert.assertEquals("Did not find the expected number of Animals", 5, getDataRegionRowCount(dataRegionName));
        assertTextNotPresent(PROTOCOL_MEMBER_IDS[1]);
        assertTextNotPresent(PROTOCOL_MEMBER_IDS[2]);
                                      
        clickButton("Clear", 0);
        refreshAnimalHistoryReport();
        assertAlert("Must Enter At Least 1 Animal ID");
        assertElementNotPresent(Locator.buttonContainingText("(X)"));
    }

    private void refreshAnimalHistoryReport()
    {
        waitForText("Abstract");
        clickButton("Refresh", 0);
    }

    private void quickSearchTest()
    {
        log("Quick Search - Show Animal");
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        setFormElement("animal", MORE_ANIMAL_IDS[0]);
        clickButton("Show Animal");
        assertTitleContains("Animal - "+MORE_ANIMAL_IDS[0]);

        log("Quick Search - Show Group");
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        _extHelper.selectComboBoxItem(Locator.xpath("//input[@name='animalGroup']/.."), "Alive, at Center");
        clickButton("Show Group");
        waitForText("1 - 36 of 36", WAIT_FOR_JAVASCRIPT);

        log("Quick Search - Show Project");
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        _extHelper.selectComboBoxItem(Locator.xpath("//input[@name='projectField']/.."), PROJECT_ID);
        clickButton("Show Project");
        waitForElement(Locator.linkWithText(PROJECT_ID), WAIT_FOR_JAVASCRIPT);

        log("Quick Search - Show Protocol");
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        _extHelper.selectComboBoxItem(Locator.xpath("//input[@name='protocolField']/.."), PROTOCOL_ID);
        clickButton("Show Protocol");
        waitForElement(Locator.linkWithText(PROTOCOL_ID), WAIT_FOR_JAVASCRIPT);

        log("Quick Search - Show Room");
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitForElement(Locator.linkWithText("Advanced Animal Search"), WAIT_FOR_JAVASCRIPT);
        setFormElement("room", ROOM_ID);
        clickButton("Show Room");
        waitForElement(Locator.linkWithText(PROJECT_MEMBER_ID), WAIT_FOR_JAVASCRIPT);
    }

    protected void setupEhrPermissions()
    {
        clickFolder(PROJECT_NAME);
        createUserAndNotify(DATA_ADMIN.getUser(), "");
        clickFolder(PROJECT_NAME);
        createUserAndNotify(REQUESTER.getUser(), "");
        clickFolder(PROJECT_NAME);
        createUserAndNotify(BASIC_SUBMITTER.getUser(), "");
        clickFolder(PROJECT_NAME);
        createUserAndNotify(FULL_SUBMITTER.getUser(), "");
        clickFolder(PROJECT_NAME);
        createUserAndNotify(FULL_UPDATER.getUser(), "");
        clickFolder(PROJECT_NAME);
        createUserAndNotify(REQUEST_ADMIN.getUser(), "");
        clickFolder(PROJECT_NAME);

        setInitialPassword(DATA_ADMIN.getUser(), PasswordUtil.getPassword());
        setInitialPassword(REQUESTER.getUser(), PasswordUtil.getPassword());
        setInitialPassword(BASIC_SUBMITTER.getUser(), PasswordUtil.getPassword());
        setInitialPassword(FULL_SUBMITTER.getUser(), PasswordUtil.getPassword());
        setInitialPassword(FULL_UPDATER.getUser(), PasswordUtil.getPassword());
        setInitialPassword(REQUEST_ADMIN.getUser(), PasswordUtil.getPassword());

        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        pushLocation();
        createPermissionsGroup(DATA_ADMIN.getGroup(), DATA_ADMIN.getUser());
        createPermissionsGroup(REQUESTER.getGroup(), REQUESTER.getUser());
        createPermissionsGroup(BASIC_SUBMITTER.getGroup(), BASIC_SUBMITTER.getUser());
        createPermissionsGroup(FULL_SUBMITTER.getGroup(), FULL_SUBMITTER.getUser());
        createPermissionsGroup(FULL_UPDATER.getGroup(), FULL_UPDATER.getUser());
        createPermissionsGroup(REQUEST_ADMIN.getGroup(), REQUEST_ADMIN.getUser());
        popLocation();
        enterPermissionsUI();
        uncheckInheritedPermissions();
        setPermissions(DATA_ADMIN.getGroup(), "Editor");
        setPermissions(REQUESTER.getGroup(), "Editor");
        setPermissions(BASIC_SUBMITTER.getGroup(), "Editor");
        setPermissions(FULL_SUBMITTER.getGroup(), "Editor");
        setPermissions(FULL_UPDATER.getGroup(), "Editor");
        setPermissions(REQUEST_ADMIN.getGroup(), "Editor");
        savePermissions();
        _ext4Helper.clickTabContainingText("Study Security");
        waitAndClickButton("Study Security");

        checkRadioButton(_helper.getAnimalHistoryRadioButtonLocator(DATA_ADMIN.getGroup(), "READOWN"));
        checkRadioButton(_helper.getAnimalHistoryRadioButtonLocator(REQUESTER.getGroup(), "READOWN"));
        checkRadioButton(_helper.getAnimalHistoryRadioButtonLocator(BASIC_SUBMITTER.getGroup(), "READOWN"));
        checkRadioButton(_helper.getAnimalHistoryRadioButtonLocator(FULL_SUBMITTER.getGroup(), "READOWN"));
        checkRadioButton(_helper.getAnimalHistoryRadioButtonLocator(FULL_UPDATER.getGroup(), "READOWN"));
        checkRadioButton(_helper.getAnimalHistoryRadioButtonLocator(REQUEST_ADMIN.getGroup(), "READOWN"));
        clickAndWait(Locator.id("groupUpdateButton"));

        //"set all to..." combo-boxes don't work through _selenium.
        log("Set per-dataset permissions individually");
        _helper.setPDP(DATA_ADMIN);
        _helper.setPDP(BASIC_SUBMITTER);
        _helper.setPDP(FULL_SUBMITTER);
        _helper.setPDP(FULL_UPDATER);
        _helper.setPDP(REQUESTER);
        _helper.setPDP(REQUEST_ADMIN);

        waitFor(new Checker(){
            public boolean check(){
                return "EHR Data Admin".equals(getSelectedOptionText(Locator.name("dataset.1061", 0))) &&
                       "EHR Basic Submitter".equals(getSelectedOptionText(Locator.name("dataset.1061", 1))) &&
                       "EHR Full Submitter".equals(getSelectedOptionText(Locator.name("dataset.1061", 2))) &&
                       "EHR Full Updater".equals(getSelectedOptionText(Locator.name("dataset.1061", 3))) &&
                       "EHR Request Admin".equals(getSelectedOptionText(Locator.name("dataset.1061", 4))) &&
                       "EHR Requestor".equals(getSelectedOptionText(Locator.name("dataset.1061", 5)));
            }
        }, "Per-dataset permission not set", WAIT_FOR_JAVASCRIPT);

        clickButton("Save");
    }

    private void weightDataEntryTest()
    {
        log("Test weight data entry");
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        saveLocation();
        impersonate(FULL_SUBMITTER.getUser());
        recallLocation();
        waitAndClick(Locator.linkWithText("Enter Data"));
        waitForPageToLoad();

        log("Create weight measurement task.");
        waitAndClick(Locator.linkWithText("Enter Weights"));
        waitForPageToLoad();
        waitForElement(Locator.name("title"), WAIT_FOR_JAVASCRIPT);
        setFormElement("title", TASK_TITLE);
        _extHelper.selectComboBoxItem("Assigned To:", BASIC_SUBMITTER.getGroup() + "\u00A0"); // appended with a nbsp (Alt+0160)

        log("Add blank weight entries");
        clickButton("Add Record", 0);
        waitForElement(Locator.xpath("//input[@name='Id' and not(contains(@class, 'disabled'))]"), WAIT_FOR_JAVASCRIPT);
        // Form input doesn't seem to be enabled yet, so wait
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        _extHelper.setExtFormElementByLabel("Id:", "noSuchAnimal");
        waitForText("Id not found", WAIT_FOR_JAVASCRIPT);
        _extHelper.setExtFormElementByLabel("Id:", DEAD_ANIMAL_ID);
        waitForText(DEAD_ANIMAL_ID, WAIT_FOR_JAVASCRIPT);

        waitForElement(Locator.button("Add Batch"), WAIT_FOR_JAVASCRIPT);
        clickButton("Add Batch", 0);
        _extHelper.waitForExtDialog("");
        _extHelper.setExtFormElementByLabel("", "Room(s):", ROOM_ID);
        _extHelper.clickExtButton("", "Submit", 0);
        waitForText(PROJECT_MEMBER_ID, WAIT_FOR_JAVASCRIPT);
        clickButton("Add Batch", 0);
        _extHelper.waitForExtDialog("");
        _extHelper.setExtFormElementByLabel("", "Id(s):", MORE_ANIMAL_IDS[0]+","+MORE_ANIMAL_IDS[1]+";"+MORE_ANIMAL_IDS[2]+" "+MORE_ANIMAL_IDS[3]+"\n"+MORE_ANIMAL_IDS[4]);
        _extHelper.clickExtButton("", "Submit", 0);
        waitForText(MORE_ANIMAL_IDS[0], WAIT_FOR_JAVASCRIPT);
        waitForText(MORE_ANIMAL_IDS[1], WAIT_FOR_JAVASCRIPT);
        waitForText(MORE_ANIMAL_IDS[2], WAIT_FOR_JAVASCRIPT);
        waitForText(MORE_ANIMAL_IDS[3], WAIT_FOR_JAVASCRIPT);
        waitForText(MORE_ANIMAL_IDS[4], WAIT_FOR_JAVASCRIPT);

        _helper.selectDataEntryRecord("weight", MORE_ANIMAL_IDS[0], true);
        _helper.selectDataEntryRecord("weight", MORE_ANIMAL_IDS[1], true);
        _helper.selectDataEntryRecord("weight", MORE_ANIMAL_IDS[2], true);
        clickButton("Delete Selected", 0);
        _extHelper.waitForExtDialog("Confirm");
        _extHelper.clickExtButton("Yes", 0);
        waitForElementToDisappear(Locator.tagWithText("div", PROTOCOL_MEMBER_IDS[0]), WAIT_FOR_JAVASCRIPT);
        waitForElementToDisappear(Locator.tagWithText("div", MORE_ANIMAL_IDS[0]), WAIT_FOR_JAVASCRIPT);
        waitForElementToDisappear(Locator.tagWithText("div", MORE_ANIMAL_IDS[1]), WAIT_FOR_JAVASCRIPT);

        //TODO: Test duplicate record
        _helper.selectDataEntryRecord("weight", MORE_ANIMAL_IDS[4], true);
        clickButton("Duplicate Selected", 0);
        _extHelper.waitForExtDialog("Duplicate Records");
        _extHelper.clickExtButton("Duplicate Records", "Submit", 0);
        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);

        clickButton("Save & Close");

        waitForText("No data to show.", WAIT_FOR_JAVASCRIPT);
        _extHelper.clickExtTab("All Tasks");
        waitForElement(Locator.xpath("//div[contains(@class, 'all-tasks-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);
        Assert.assertEquals("Incorrect number of task rows.", 1, selenium.getXpathCount("//div[contains(@class, 'all-tasks-marker') and "+Locator.NOT_HIDDEN+"]//tr[@class='labkey-alternate-row' or @class='labkey-row']"));
        _extHelper.clickExtTab("Tasks By Room");
        waitForElement(Locator.xpath("//div[contains(@class, 'room-tasks-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);
        Assert.assertEquals("Incorrect number of task rows.", 3, selenium.getXpathCount("//div[contains(@class, 'room-tasks-marker') and "+Locator.NOT_HIDDEN+"]//tr[@class='labkey-alternate-row' or @class='labkey-row']"));
        _extHelper.clickExtTab("Tasks By Id");
        waitForElement(Locator.xpath("//div[contains(@class, 'id-tasks-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);
        Assert.assertEquals("Incorrect number of task rows.", 3, selenium.getXpathCount("//div[contains(@class, 'id-tasks-marker') and "+Locator.NOT_HIDDEN+"]//tr[@class='labkey-alternate-row' or @class='labkey-row']"));

        stopImpersonating();

        log("Fulfil measurement task");
        impersonate(BASIC_SUBMITTER.getUser());
        recallLocation();
        waitAndClick(Locator.linkWithText("Enter Data"));
        waitForPageToLoad();
        waitForElement(Locator.xpath("//div[contains(@class, 'my-tasks-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);

        String href = getAttribute(Locator.linkWithText(TASK_TITLE), "href");
        beginAt(href); // Clicking link opens in another window.
        waitForElement(Locator.xpath("/*//*[contains(@class,'ehr-weight-records-grid')]"), WAIT_FOR_JAVASCRIPT);
        waitForTextToDisappear("Loading...", WAIT_FOR_JAVASCRIPT);
        _helper.selectDataEntryRecord("weight", MORE_ANIMAL_IDS[4], false);
        waitForElement(Locator.linkWithText(MORE_ANIMAL_IDS[4]), WAIT_FOR_JAVASCRIPT);
        clickButton("Delete Selected", 0); // Delete duplicate record. It has served its purpose.
        _extHelper.waitForExtDialog("Confirm");
        _extHelper.clickExtButton("Yes", 0);
        waitForText("No Animal Selected", WAIT_FOR_JAVASCRIPT);
        _helper.selectDataEntryRecord("weight", PROJECT_MEMBER_ID, false);
        _extHelper.setExtFormElementByLabel("Weight (kg):", "3.333");
        _helper.selectDataEntryRecord("weight", MORE_ANIMAL_IDS[3], false);
        _extHelper.setExtFormElementByLabel("Weight (kg):", "4.444");
        _helper.selectDataEntryRecord("weight", MORE_ANIMAL_IDS[4], false);
        _extHelper.setExtFormElementByLabel("Weight (kg):", "5.555");

        clickButton("Submit for Review", 0);
        _extHelper.waitForExtDialog("Submit For Review");
        _extHelper.selectComboBoxItem("Assign To:", DATA_ADMIN.getGroup());
        _extHelper.clickExtButton("Submit For Review", "Submit");

        sleep(1000); // Weird
        stopImpersonating();

        log("Verify Measurements");
        sleep(1000); // Weird 
        impersonate(DATA_ADMIN.getUser());
        recallLocation();
        waitAndClick(Locator.linkWithText("Enter Data"));
        waitForPageToLoad();
        waitForElement(Locator.xpath("//div[contains(@class, 'my-tasks-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);
        _extHelper.clickExtTab("Review Required");
        waitForElement(Locator.xpath("//div[contains(@class, 'review-requested-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);
        Assert.assertEquals("Incorrect number of task rows.", 1, selenium.getXpathCount("//div[contains(@class, 'review-requested-marker') and "+Locator.NOT_HIDDEN+"]//tr[@class='labkey-alternate-row' or @class='labkey-row']"));
        String href2 = getAttribute(Locator.linkWithText(TASK_TITLE), "href");
        beginAt(href2); // Clicking opens in a new window.
        waitForElement(Locator.xpath("/*//*[contains(@class,'ehr-weight-records-grid')]"), WAIT_FOR_JAVASCRIPT);
        clickButton("Validate", 0);
        waitForElement(Locator.xpath("//button[text() = 'Submit Final' and "+Locator.ENABLED+"]"), WAIT_FOR_JAVASCRIPT);
        clickButton("Submit Final", 0);
        _extHelper.waitForExtDialog("Finalize Form");
        _extHelper.clickExtButton("Finalize Form", "Yes");

        sleep(1000); // Weird
        stopImpersonating();
        sleep(1000); // Weird
        
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitAndClick(Locator.linkWithText("Browse All Datasets"));
        waitForPageToLoad();
        waitAndClick(LabModuleHelper.getNavPanelItem("Weight:", "Browse All"));
        waitForPageToLoad();

        setFilter("query", "date", "Equals", DATE_FORMAT.format(new Date()));
        assertTextPresent("3.333", "4.444", "5.555");
        assertTextPresent("Completed", 3);
    }

    private void mprDataEntryTest()
    {
        log("Test MPR data entry.");
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        saveLocation();
        impersonate(FULL_SUBMITTER.getUser());
        recallLocation();
        waitAndClick(Locator.linkWithText("Enter Data"));
        waitForPageToLoad();

        log("Create weight measurement task.");
        waitAndClick(Locator.linkWithText("Enter MPR"));
        waitForPageToLoad();
        // Wait for page to fully render.
        waitForText("Treatments", WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.name("Id"), WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.name("title"), WAIT_FOR_JAVASCRIPT);
        _extHelper.setExtFormElementByLabel("Id:", PROJECT_MEMBER_ID);
        waitForElement(Locator.linkWithText(PROJECT_MEMBER_ID), WAIT_FOR_JAVASCRIPT);
        setFormElement("title", MPR_TASK_TITLE);
        _extHelper.selectComboBoxItem("Assigned To:", BASIC_SUBMITTER.getGroup() + "\u00A0"); // appended with a nbsp (Alt+0160)

        sleep(1000);

        clickButton("Save & Close");

        waitForText("No data to show.", WAIT_FOR_JAVASCRIPT);
        _extHelper.clickExtTab("All Tasks");
        waitForElement(Locator.xpath("//div[contains(@class, 'all-tasks-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);
        Assert.assertEquals("Incorrect number of task rows.", 1, selenium.getXpathCount("//div[contains(@class, 'all-tasks-marker') and "+Locator.NOT_HIDDEN+"]//tr[@class='labkey-alternate-row' or @class='labkey-row']"));
        _extHelper.clickExtTab("Tasks By Room");
        waitForElement(Locator.xpath("//div[contains(@class, 'room-tasks-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);
        Assert.assertEquals("Incorrect number of task rows.", 1, selenium.getXpathCount("//div[contains(@class, 'room-tasks-marker') and "+Locator.NOT_HIDDEN+"]//tr[@class='labkey-alternate-row' or @class='labkey-row']"));
        _extHelper.clickExtTab("Tasks By Id");
        waitForElement(Locator.xpath("//div[contains(@class, 'id-tasks-marker') and "+Locator.NOT_HIDDEN+"]//table"), WAIT_FOR_JAVASCRIPT);
        Assert.assertEquals("Incorrect number of task rows.", 1, selenium.getXpathCount("//div[contains(@class, 'id-tasks-marker') and "+Locator.NOT_HIDDEN+"]//tr[@class='labkey-alternate-row' or @class='labkey-row']"));
        stopImpersonating();

        log("Fulfil MPR task");
        impersonate(BASIC_SUBMITTER.getUser());
        recallLocation();
        waitAndClick(Locator.linkWithText("Enter Data"));
        waitForPageToLoad();
        waitForElement(Locator.xpath("//div[contains(@class, 'my-tasks-marker') and "+VISIBLE+"]//table"), WAIT_FOR_JAVASCRIPT);
        String href = getAttribute(Locator.linkWithText(MPR_TASK_TITLE), "href");
        beginAt(href);

        // Wait for page to fully render.
        waitForText("Treatments", WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.name("Id"), WAIT_FOR_PAGE);
        waitForElement(Locator.name("title"), WAIT_FOR_JAVASCRIPT);
        waitForElement(Locator.xpath("/*//*[contains(@class,'ehr-drug_administration-records-grid')]"), WAIT_FOR_JAVASCRIPT);
        _extHelper.selectComboBoxItem("Project:", PROJECT_ID + " (" + DUMMY_PROTOCOL + ")\u00A0");
        _extHelper.selectComboBoxItem("Type:", "Physical Exam\u00A0");
        setFormElement("remark", "Bonjour");
        setFormElement("performedby", BASIC_SUBMITTER.getUser());

        log("Add treatments record.");
        waitForElement(Locator.xpath("/*//*[contains(@class,'ehr-drug_administration-records-grid')]"), WAIT_FOR_JAVASCRIPT);
        _helper.clickVisibleButton("Add Record");
        setFormElement(Locator.xpath("//div[./div/span[text()='Treatments & Procedures']]//input[@name='enddate']/..//input[contains(@id, 'date')]"), DATE_FORMAT.format(new Date()));
        _extHelper.selectComboBoxItem("Code:", "Antibiotic");
        _extHelper.selectComboBoxItem(Locator.xpath("//input[@name='code']/.."), "amoxicillin (c-54620)\u00a0");
        _extHelper.selectComboBoxItem("Route:", "oral\u00a0");
        setFormElement("concentration", "5");
        _extHelper.selectComboBoxItem(Locator.xpath("//input[@name='conc_units']/.."), "mg/tablet\u00a0");
        //TODO: assert units
        setFormElement("dosage", "2");
        click(Locator.xpath("//img["+VISIBLE+" and contains(@class, 'x-form-search-trigger')]"));
        waitForElement(Locator.xpath("//div[@class='x-form-invalid-msg']"), WAIT_FOR_JAVASCRIPT);
        _helper.setDataEntryField("Treatments & Procedures", "remark", "Yum");

        //TODO: Test more procedures.
//        log("Add blood draw record.");
//        _extHelper.clickExtTab(this, "Blood Draws");
//        waitForElement(Locator.xpath("//*["+VISIBLE+" and contains(@class,'ehr-blood_draws-records-grid')]"), WAIT_FOR_JAVASCRIPT);
//        clickVisibleButton("Add Record");
//
//        log("Add recovery observation");
//        _extHelper.clickExtTab(this, "Recovery Observations");
//        waitForElement(Locator.xpath("//*["+VISIBLE+" and contains(@class,'ehr-clinical_observations-records-grid')]"), WAIT_FOR_JAVASCRIPT);
//        clickVisibleButton("Add Record");
//
//        log("Add procedure code");
//        _extHelper.clickExtTab(this, "Procedure Codes");
//        waitForElement(Locator.xpath("//*["+VISIBLE+" and contains(@class,'ehr-procedure_codes-records-grid')]"), WAIT_FOR_JAVASCRIPT);
//        clickVisibleButton("Add Record");
//
//        log("Add housing record.");
//        _extHelper.clickExtTab(this, "Housing Moves/Restraint");
//        waitForElement(Locator.xpath("//*["+VISIBLE+" and contains(@class,'ehr-housing-records-grid')]"), WAIT_FOR_JAVASCRIPT);
//        clickVisibleButton("Add Record");
//
//        log("Add weight record.");
//        _extHelper.clickExtTab(this, "Weight");
//        waitForElement(Locator.xpath("//*["+VISIBLE+" and contains(@class,'ehr-weight-records-grid')]"), WAIT_FOR_JAVASCRIPT);
//        clickVisibleButton("Add Record");
//
//        log("Add charge");
//        _extHelper.clickExtTab(this, "Charges");
//        waitForElement(Locator.xpath("/*//*["+VISIBLE+" and not(contains(@class, 'x-hide-display')) and contains(@class,'ehr-charges-records-grid')]"), WAIT_FOR_JAVASCRIPT);
//        clickVisibleButton("Add Record");

        clickButton("Save & Close");

        stopImpersonating();
    }

    private void crawlReportTabs()
    {
        String tabs[] = {/*"-Assay", "MHC SSP Typing", "Viral Loads", */ //Bad queries on test server.
                         "-Assignments", "Active Assignments", "Assignment History",
                         "-Clin Path", "Bacteriology", "Chemistry:By Panel", "Clinpath Runs", "Hematology:By Panel", "Immunology:By Panel", "Parasitology", "Urinalysis:By Panel", "Viral Challenges", "Virology",
                         "-Clinical", "Abstract:Active Assignments", "Clinical Encounters", "Clinical Remarks", "Diarrhea Calendar", "Full History", "Full History Plus Obs", "Irregular Obs:Irregular Observations", "Problem List", "Procedure Codes", "Surgical History", "Tasks", "Treatment Orders", "Treatments", "Treatment Schedule", "Weights:Weight",
                         "-Colony Management", "Behavior Remarks", "Birth Records", "Housing - Active", "Housing History", "Inbreeding Coefficients", "Kinship", "Menses Calendar", "Menses Observations:Irregular Observations", "Pedigree:Offspring", /*"Pedigree Plot",*/ "Pregnancies", "TB Tests",
                         "-Pathology", "Biopsies", "Histology", "Morphologic Diagnosis", "Necropsies",
                         "-Physical Exam", "Alopecia", "Body Condition", "Dental Status", "Exams", "PE Findings", "Teeth", "Vitals",
                         "-Today At Center", "Irregular Observations", "Obs/Treatment:Obs/Treatments", "Problem List", /*"Today's History",*/ "Treatments - Morning", "Treatments - Afternoon", "Treatments - Evening", "Treatments - Master", "Unresolved Problem List", /*"Today's Blood Draws",*/
                         "-General", "Arrival/Departure:Arrivals", "Blood Draw History", "Charges", "Current Blood", "Deaths", "Demographics", "Major Events", "Notes", "Abstract:Active Assignments"};

        log("Check all Animal History report tabs");
        for (String tab : tabs)
        {
            if(tab.startsWith("-")) // High level tab
            {
                _extHelper.clickExtTab(tab.substring(1));
            }
            else
            {
                if(tab.contains(":"))
                {
                    _extHelper.clickExtTab(tab.split(":")[0]);
                    _helper.getAnimalHistoryDataRegionName(tab.split(":")[1]);
                }
                else
                {
                    _extHelper.clickExtTab(tab);
                    _helper.getAnimalHistoryDataRegionName(tab);
                }
            }
        }

        //Clear out lingering text on report pages
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        waitAndClick(Locator.linkWithText("Animal History"));
        waitForPageToLoad();
    }

    public static class Permission
    {
        EHRRole role;
        EHRQCState qcState;
        String action;
        public Permission(EHRRole role, EHRQCState qcState, String action)
        {
            this.role = role;
            this.qcState = qcState;
            this.action = action;
        }

        @Override
        public boolean equals(Object other)
        {
            return other.getClass().equals(Permission.class) &&
                this.role == ((Permission)other).role &&
                this.qcState == ((Permission)other).qcState &&
                this.action.equals(((Permission)other).action);
        }
    }

    public static enum EHRRole
    {
        DATA_ADMIN ("EHR Data Admin"),
        REQUESTER ("EHR Requestor"),
        BASIC_SUBMITTER ("EHR Basic Submitter"),
        FULL_SUBMITTER ("EHR Full Submitter"),
        FULL_UPDATER ("EHR Full Updater"),
        REQUEST_ADMIN ("EHR Request Admin");
        private final String name;
        private EHRRole (String name)
        {this.name = name;}
        public String toString()
        {return name;}
    }

    public static enum EHRQCState
    {
        ABNORMAL("Abnormal", "Value is abnormal", true, false, false),
        COMPLETED("Completed", "Data has been approved for public release", true, false, false),
        DELETE_REQUESTED("Delete Requested", "Records are requested to be deleted", true, true, false),
        IN_PROGRESS("In Progress", "Draft Record, not public", false, true, false),
        REQUEST_APPROVED("Request: Approved", "Request has been approved", true, true, true),
        REQUEST_COMPLETE("Request: Complete", "Request has been completed", true, false, true),
        REQUEST_DENIED("Request: Denied", "Request has been denied", true, false, true),
        REQUEST_PENDING("Request: Pending", "Part of a request that has not been approved", false, false, true),
        REVIEW_REQUIRED("Review Required", "Review is required prior to public release", false, false, false),
        SCHEDULED("Scheduled", "Record is scheduled, but not performed", true, true, false);

        public final String label;
        public final String description;
        public final boolean publicData;

        public final boolean draftData;
        public final boolean isRequest;

        EHRQCState(String label, String description, boolean publicData, boolean draftData, boolean isRequest)
        {
            this.label = label;
            this.description = description;
            this.publicData = publicData;
            this.draftData = draftData;
            this.isRequest = isRequest;
        }
    }

    protected void defineQCStates()
    {
        log("Define QC states for EHR study");
        clickFolder(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        goToManageStudy();
        clickLinkWithText("Manage Dataset QC States");

        for(EHRQCState qcState : EHRQCState.values())
        {
            setFormElement("newLabel", qcState.label);
            setFormElement("newDescription", qcState.description);
            if(!qcState.publicData) uncheckCheckbox("newPublicData");
            clickButton("Save");
        }

        setFormElement("showPrivateDataByDefault", "true");
        clickButton("Done");
    }
}
