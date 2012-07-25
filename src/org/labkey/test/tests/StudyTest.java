/*
 * Copyright (c) 2009-2012 LabKey Corporation
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

import com.thoughtworks.selenium.SeleniumException;
import org.apache.commons.lang3.StringUtils;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PasswordUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.labkey.test.util.PasswordUtil.getUsername;

/**
 * User: adam
 * Date: Apr 3, 2009
 * Time: 9:18:32 AM
 */
public class StudyTest extends StudyBaseTest
{
    protected boolean quickTest = true;
    protected static final String DEMOGRAPHICS_DESCRIPTION = "This is the demographics dataset, dammit. Here are some \u2018special symbols\u2019 - they help test that we're roundtripping in UTF-8.";
    protected static final String DEMOGRAPHICS_TITLE = "DEM-1: Demographics";

    protected String _tsv = "participantid\tsequencenum\tvisitdate\tSampleId\tDateField\tNumberField\tTextField\treplace\taliasedColumn\n" +
        "1234\t1\t1/1/2006\t1234_A\t2/1/2006\t1.2\ttext\t\taliasedData\n" +
        "1234\t1\t1/1/2006\t1234_B\t2/1/2006\t1.2\ttext\t\taliasedData\n";

    // specimen comment constants
    private static final String PARTICIPANT_CMT_DATASET = "Mouse Comments";
    private static final String PARTICIPANT_VISIT_CMT_DATASET = "Mouse Visit Comments";
    private static final String COMMENT_FIELD_NAME = "comment";
    private static final String PARTICIPANT_COMMENT_LABEL = "mouse comment";
    private static final String PARTICIPANT_VISIT_COMMENT_LABEL = "mouse visit comment";

    protected static final String VISIT_IMPORT_MAPPING = "Name\tSequenceNum\n" +
        "Cycle 10\t10\n" +
        "Vaccine 1\t201\n" +
        "Vaccination 1\t201\n" +
        "Soc Imp Log #%{S.3.2}\t5500\n" +
        "ConMeds Log #%{S.3.2}\t9002\n" +
        "All Done\t9999";

    public static final String APPEARS_AFTER_PICKER_LOAD = "Add Selected";


    //lists created in participant picker tests must be cleaned up afterwards
    LinkedList<String> persistingLists  = new LinkedList<String>();
    private String Study001 = "Study 001";
    private String authorUser = "author@study.test";
    private String specimenUrl = null;

    protected File[] getTestFiles()
    {
        return new File[]{new File(getLabKeyRoot() + "/server/test/data/api/study-api.xml")};
    }

    protected void doCreateSteps()
    {
        enableEmailRecorder();
        importStudy();
        startSpecimenImport(2);

        // wait for study (but not specimens) to finish loading
        waitForPipelineJobsToComplete(1, "study import", false);
    }

    protected void doCleanup() throws Exception //child class cleanup method throws Exception
    {
        try{emptyParticipantPickerList();}catch(Throwable t){ /* Ignore */ }
        deleteUser(authorUser, false);
        super.doCleanup();
    }

    protected void emptyParticipantPickerList()
    {

        goToManageParticipantClassificationPage(PROJECT_NAME, STUDY_NAME, SUBJECT_NOUN);
        while(persistingLists.size()!=0)
        {
            deleteListTest(persistingLists.pop());
        }
    }

    protected void doVerifySteps()
    {
        doVerifyStepsSetDepth(false);
    }

    protected void doVerifyStepsSetDepth(boolean quickTest)
    {
        this.quickTest = quickTest;
        manageSubjectClassificationTest();
        emptyParticipantPickerList(); // Delete participant lists to avoid interfering with api test.
        verifyStudyAndDatasets();
        if(!quickTest)
        {
            waitForSpecimenImport();
            verifySpecimens();
            verifyParticipantComments();
            verifyParticipantReports();
            verifyPermissionsRestrictions();
        }
    }

    private void verifyPermissionsRestrictions()
    {
        createUserWithPermissions(authorUser, null, "Author");
        impersonate(authorUser);
        beginAt(specimenUrl);
        clickButton("Request Options", 0);
        assertElementNotPresent(Locator.tagWithText("span", "Create New Request"));
        stopImpersonating();

    }

    private void verifyParticipantReports()
    {
        clickLinkWithText(getFolderName());
        addWebPart("Study Data Tools");
        clickLinkWithImage("/labkey/study/tools/participant_report.png");
        clickButton("Choose Measures", 0);
        ExtHelper.waitForExtDialog(this, "Add Measure");
        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);

        String textToFilter = "AE-1:(VTN) AE Log";
        waitForText(textToFilter);
        assertTextPresent(textToFilter, 27);
        assertTextPresent("Abbrevi", 79);

        log("filter participant results down");
        Locator filterSearchText = Locator.xpath("//input[@name='filterSearch']");
        selenium.type(filterSearchText.toXpath(), "a");
        selenium.type(filterSearchText.toXpath(), "abbrev");
        setFormElement(Locator.xpath("//input[@type='text']"), "abbrevi");
        fireEvent(filterSearchText, SeleniumEvent.change);
        sleep(1000);
        assertTextPresent("Abbrevi", 79);
        assertTextNotPresent(textToFilter);

        log("select some records and include them in a report");
        ExtHelper.clickX4GridPanelCheckbox(this, 4, "measuresGridPanel", true);
        ExtHelper.clickX4GridPanelCheckbox(this, 40, "measuresGridPanel", true);
        ExtHelper.clickX4GridPanelCheckbox(this, 20, "measuresGridPanel", true);
        ExtHelper.clickExtButton(this, "Select", 0);
        waitForExtMaskToDisappear();

        log("Verify report page looks as expected");
        String reportName = "Foo";
        String reportDescription = "Desc";
        ExtHelper.setExtFormElementByLabel(this, "Report Name", reportName);
        ExtHelper.setExtFormElementByLabel(this, "Report Description", reportDescription);
        clickButton("Save", 0);
        waitForText(reportName);
        assertTextPresent(reportName, 2);

    }

    protected static final String SUBJECT_NOUN = "Mouse";
    protected static final String SUBJECT_NOUN_PLURAL = "Mice";
    protected static final String PROJECT_NAME = "StudyVerifyProject";
    protected static final String STUDY_NAME = "My Study";
    protected static final String LABEL_FIELD = "groupLabel";
    protected static final String ID_FIELD = "categoryIdentifiers";


    /**
     * This is a test of the participant picker/classification creation UI.
     */
    protected void manageSubjectClassificationTest()
    {

        if(!quickTest)
        {
            //verify/create the right data
            goToManageParticipantClassificationPage(PROJECT_NAME, STUDY_NAME, SUBJECT_NOUN);

            //issue 12487
            assertTextPresent("Manage " + SUBJECT_NOUN + " Groups");

            //nav trail check
            assertTextNotPresent("Manage Study > ");

            String allList = "all list12345";
            String filteredList = "Filtered list";

            cancelCreateClassificationList();

            String pIDs = createListWithAddAll(allList, false);
            persistingLists.add(allList);

            refresh();
            editClassificationList(allList, pIDs);

            //Issue 12485
            createListWithAddAll(filteredList, true);
            persistingLists.add(filteredList);

            String changedList = changeListName(filteredList);
            persistingLists.add(changedList);
            persistingLists.remove(filteredList);
            deleteListTest(allList);
            persistingLists.remove(allList);

            attemptCreateExpectError("1", "does not exist in this study.", "bad List ");
            String id = pIDs.substring(0, pIDs.indexOf(","));
            attemptCreateExpectError(id + ", " + id, "Duplicates are not allowed in a group", "Bad List 2");
        }

        // test creating a participant group directly from a data grid
        waitForElement(Locator.linkContainingText(STUDY_NAME));
        clickLinkWithText(STUDY_NAME);
        clickLinkWithText("47 datasets");
        clickLinkWithText("DEM-1: Demographics");


        // verify warn on no selection
        if(!isQuickTest)
        {
            //nav trail check
            clickLinkContainingText("999320016");
            assertTextPresent("Dataset: DEM-1: Demographics, All Visits >  ");
            clickLinkContainingText("Dataset:");

            ExtHelper.clickMenuButton(this, false, SUBJECT_NOUN + " Groups", "Create " + SUBJECT_NOUN + " Group", "From Selected " + SUBJECT_NOUN_PLURAL);
            ExtHelper.waitForExtDialog(this, "Selection Error");
            assertTextPresent("At least one " + SUBJECT_NOUN + " must be selected");
            clickButtonContainingText("OK", 0);
            waitForExtMaskToDisappear();

        }

        DataRegionTable table = new DataRegionTable("Dataset", this, true, true);
        for (int i=0; i < 5; i++)
            table.checkCheckbox(i);

        // verify the selected list of identifiers is passed to the participant group wizard
        String[] selectedIDs = new String[]{"999320016","999320518","999320529","999320541","999320533"};
        ExtHelper.clickMenuButton(this, false, SUBJECT_NOUN + " Groups", "Create " + SUBJECT_NOUN + " Group", "From Selected " + SUBJECT_NOUN_PLURAL);
        ExtHelper.waitForExtDialog(this, "Define " + SUBJECT_NOUN + " Group");
        verifySubjectIDsInWizard(selectedIDs);

        // save the new group and use it
        setFormElement(LABEL_FIELD, "Participant Group from Grid");
        clickButtonContainingText("Save", 0);
        waitForExtMaskToDisappear();

        if(!quickTest)
        {
            // the dataregion get's ajaxed into place, wait until the new group appears in the menu
            Locator menu = Locator.navButton(SUBJECT_NOUN + " Groups");
            waitForElement(menu, WAIT_FOR_JAVASCRIPT);
            Locator menuItem = Locator.menuItem("Participant Group from Grid");
            for (int i = 0; i < 10; i++)
            {
                try{
                    click(menu);
                }
                catch(SeleniumException e){
                    /* Ignore. This button is unpredictable. */
                }
                if (isElementPresent(menuItem))
                    break;
                else
                    sleep(1000);
            }
            clickAndWait(menuItem);
            for (String identifier : selectedIDs)
                assertTextPresent(identifier);
        }
    }

    private void verifySubjectIDsInWizard(String[] ids)
    {
        Locator textArea = Locator.xpath("//textarea[@id='categoryIdentifiers']");
        waitForElement(textArea, WAIT_FOR_JAVASCRIPT);
        String subjectIDs = getFormElement(textArea);
        Set<String> identifiers = new HashSet<String>();

        for (String subjectId : subjectIDs.split(","))
            identifiers.add(subjectId);

        // validate...
        if (!identifiers.containsAll(Arrays.asList(ids)))
            fail("The Participant Group wizard did not contain the subject IDs : " + ids);
    }

    /** verify that we can change a list's name
     * pre-conditions: list with name listName exists
     * post-conditions: list now named lCHANGEstName
     * @param listName
     * @return new name of list
     */
    private String changeListName(String listName)
    {
        String newListName = listName.substring(0, 1) + "CHANGE" + listName.substring(2);
        selectListName(listName);
        clickButtonContainingText("Edit Selected", APPEARS_AFTER_PICKER_LOAD);

        setFormElement(LABEL_FIELD, newListName);

        clickButtonContainingText("Save", 0);

        waitForTextToDisappear(listName, 2*defaultWaitForPage);
        assertTextPresent(newListName);
        return newListName;
    }

    /**
     * verify that we can delete a list and its name no longer appears in classification list
     * pre-conditions:  list listName exists
     * post-conditions:  list listName does not exist
     * @param listName list to delete
     */
    private void deleteListTest(String listName)
    {
        selectListName(listName);

        clickButtonContainingText("Delete Selected", 0);

        //make sure we can change our minds
        ExtHelper.waitForExtDialog(this, "Delete Group");
        clickButtonContainingText("No", 0);
        waitForExtMaskToDisappear();
        assertTextPresent(listName);


        clickButtonContainingText("Delete Selected", 0);
        ExtHelper.waitForExtDialog(this, "Delete Group");
        clickButtonContainingText("Yes", 0);
        waitForExtMaskToDisappear();
        waitForTextToDisappear(listName);

    }

    /** verify that attempting to create a list with the expected name and list of IDs causes
     * the error specified by expectedError
     *
     * @param ids IDs to enter in classification list
     * @param expectedError error message to expect
     * @param listName name to enter in classification label
     */
    private void attemptCreateExpectError(String ids, String expectedError, String listName)
    {
        createStudy();

        setFormElement(LABEL_FIELD, listName);
        setFormElement(ID_FIELD, ids);
        clickButtonContainingText("Save", 0);
        waitForText(expectedError, 5*defaultWaitForPage);
        clickButtonContainingText("OK", 0);
        clickButtonContainingText("Cancel", 0);
        assertTextNotPresent(listName);
    }

    /**
     * verify that an already created list contains the pIDs we expect it to and can be changed.
     * pre-conditions:  listName exists with the specified IDs
     * post-conditions:  listName exists, with the same IDs, minus the first one
     *
     * @param listName
     * @param pIDs
     */
    private void editClassificationList(String listName, String pIDs)
    {
        selectListName(listName);

        clickButtonContainingText("Edit Selected", APPEARS_AFTER_PICKER_LOAD);
        String newPids = getFormElement(ID_FIELD);
        assertSetsEqual(pIDs, newPids, ", *");
        log("IDs present after opening list: " + newPids);

        //remove first element
        newPids = pIDs.substring(pIDs.indexOf(",")+2);
        setFormElement(ID_FIELD, newPids);
        log("edit list of IDs to: " + newPids);

        //save, close, reopen, verify change
        ExtHelper.waitForExtDialog(this, "Define Mouse Group");
        clickButtonContainingText("Save", 0);
        waitForExtMaskToDisappear();
        selectListName(listName);
        clickButtonContainingText("Edit Selected", APPEARS_AFTER_PICKER_LOAD);


        String pidsAfterEdit =   getFormElement(ID_FIELD);
        log("pids after edit: " + pidsAfterEdit);


        assertEquals(newPids, pidsAfterEdit );

        clickButtonContainingText("Cancel", 0);
    }

    // select the list name from the main classification page
    private void selectListName(String listName)
    {

        Locator report = Locator.tagContainingText("div", listName);

        // select the report and click the delete button
        waitForElement(report, 10000);
        selenium.mouseDownAt(report.toString(), "1,1");
        selenium.clickAt(report.toString(), "1, 1");
    }

    /**
     * very basic test of ability to enter and exit clist creation screen
     *
     * pre-condition:  at participant classification main screen
     * post-condition:  no change
     */
    private void cancelCreateClassificationList()
    {
        createStudy();
        clickButtonContainingText("Cancel", 0);
    }

    /**preconditions: at participant picker main page
     * post-conditions:  at screen for creating new PP list
     */
    private void createStudy()
    {
//        clickButtonContainingText("Create", 0);
        //Issue 12505:  uncomment "wait for cancel", comment out
//        waitForText("Cancel", defaultWaitForPage);
//        waitForText("Add Selected", defaultWaitForPage);
        clickButtonContainingText("Create", "Add Selected");
    }


    /** create list using add all
     *
     * @param listName name of list to create
     * @param filtered should list be filtered?  If so, only participants with DEMasian=0 will be included
     * @return ids in new list
     */
    private String createListWithAddAll(String listName, boolean filtered)
    {
        createStudy();
        setFormElement(LABEL_FIELD, listName);
        DataRegionTable table = new DataRegionTable("demoDataRegion", this, true);

        if(filtered)
        {
            table.setFilter("DEMasian", "Equals", "0", 0);
            waitForText("Filter", WAIT_FOR_JAVASCRIPT);
        }

        clickButtonContainingText("Add All", 0);

        List<String> idsInColumn = table.getColumnDataAsText("Mouse Id");
        String idsInForm = getFormElement(ID_FIELD);
        assertIDListsMatch(idsInColumn, idsInForm);

        clickButtonContainingText("Save", 0);

        ExtHelper.waitForLoadingMaskToDisappear(this, WAIT_FOR_JAVASCRIPT);
        waitForText(listName, WAIT_FOR_JAVASCRIPT);
        return idsInForm;
    }

    /**
     * Compare list of IDs extracted from a column to those entered in
     * the form.  They should be identical.
     * @param idsInColumn
     * @param idsInForm
     */
    private void assertIDListsMatch(List<String> idsInColumn, String idsInForm)
    {
        //assert same size
        int columnCount = idsInColumn.size()-2; //the first entry in column count is the name
        int formCount = idsInForm.length() - idsInForm.replace(",", "").length() - 1; //number of commas + 1 = number of entries
        assertEquals(columnCount, formCount);
    }

    private void goToManageParticipantClassificationPage(String projectName, String studyName, String subjectNoun)
    {
        //else
        sleep(1000);
        goToManageStudyPage(projectName, studyName);
        clickManageSubjectCategory(subjectNoun);
    }



    protected void verifyStudyAndDatasets()
    {
        verifyDemographics();
        verifyVisitMapPage();
        verifyManageDatasetsPage();


        if(quickTest)
            return;

        verifyHiddenVisits();
        verifyVisitImportMapping();
        verifyCohorts();

        // configure QC state management before importing duplicate data
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Dataset QC States");
        setFormElement("newLabel", "unknown QC");
        setFormElement("newDescription", "Unknown data is neither clean nor dirty.");
        clickCheckboxById("dirty_public");
        clickCheckbox("newPublicData");
        clickNavButton("Save");
        selectOptionByText("defaultDirectEntryQCState", "unknown QC");
        selectOptionByText("showPrivateDataByDefault", "Public data");
        clickNavButton("Save");

        // return to dataset import page
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("47 datasets");
        clickLinkWithText("verifyAssay");
        assertTextPresent("QC State");
        assertTextNotPresent("1234_B");
        clickMenuButton("QC State", "All data");
        clickButton("QC State", 0);
        assertTextPresent("unknown QC");
        assertTextPresent("1234_B");

        //Import duplicate data
        clickNavButton("Import Data");
        setFormElement("text", _tsv);
        ListHelper.submitImportTsv_error(this, "Duplicates were found");
        //Now explicitly replace, using 'mouseid' instead of 'participantid'
        _tsv = "mouseid\tsequencenum\tvisitdate\tSampleId\tDateField\tNumberField\tTextField\treplace\n" +
                "1234\t1\t1/1/2006\t1234_A\t2/1/2006\t5000\tnew text\tTRUE\n" +
                "1234\t1\t1/1/2006\t1234_B\t2/1/2006\t5000\tnew text\tTRUE\n";
        ListHelper.submitTsvData(this, _tsv);
        assertTextPresent("5000.0");
        assertTextPresent("new text");
        assertTextPresent("QC State");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "QCState", "QC State");
        CustomizeViewsHelper.applyCustomView(this);
        assertTextPresent("unknown QC");

        // Test Bad Field Names -- #13607
        clickNavButton("Manage Dataset");
        clickNavButton("Edit Definition");
        waitAndClick(WAIT_FOR_JAVASCRIPT, Locator.navButton("Add Field"), 0);
        int newFieldIndex = getXpathCount(Locator.xpath("//input[starts-with(@name, 'ff_name')]")) - 1;
        ListHelper.setColumnName(this, getPropertyXPath("Dataset Fields"), newFieldIndex, "Bad Name");
        clickNavButton("Save");
        clickNavButton("View Data");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "Bad Name", "Bad Name");
        CustomizeViewsHelper.applyCustomView(this);
        clickMenuButton("QC State", "All data");
        clickLinkWithText("edit", 0);
        setFormElement(Locator.input("quf_Bad Name"), "Updatable Value");
        clickButton("Submit");
        assertTextPresent("Updatable Value");
        clickLinkWithText("edit", 0);
        assertFormElementEquals(Locator.input("quf_Bad Name"), "Updatable Value");
        setFormElement(Locator.input("quf_Bad Name"), "Updatable Value11");
        clickButton("Submit");
        assertTextPresent("Updatable Value11");
    }

    protected void verifySpecimens()
    {
        clickLinkWithText(getStudyLabel());
        addWebPart("Specimens");
        waitForText("Blood (Whole)");
        clickLinkWithText("Blood (Whole)");
        specimenUrl = getCurrentRelativeURL();


        log("verify presence of \"create new request\" button");
        clickButton("Request Options", 0);
        assertElementPresent(Locator.tagWithText("span", "Create New Request"));

        //TODO:  move this to specimen test
//        log("verify presence of create");
//        clickMenuButton("Page Size", "Show All");
//        assertTextNotPresent("DRT000XX-01");
//        assertTextPresent("GAA082NH-01");
//        clickLinkWithText("Group vials");
//        assertTextPresent("Total:");
//        assertTextPresent("466");
//
//        assertTextNotPresent("BAD");
//
//        clickLinkWithText("Show individual vials");
//        clickLinkContainingText("history");
//        // verify that we're correctly parsing frozen time, which is a date with a time portion only:
//        assertTextPresent("15:30:00");
//        assertTextPresent("2.0&nbsp;ML");
//        assertTextNotPresent("Added Comments");
//        // confirm collection location:
//        assertTextPresent("KCMC, Moshi, Tanzania");
//        // confirm historical locations:
//        assertTextPresent("Contract Lab Services, Johannesburg, South Africa");
//        assertTextPresent("Aurum Health KOSH Lab, Orkney, South Africa");
//
//        clickLinkWithText("Specimen Overview");
//        clickLinkWithText("By Individual Vial");
//        DataRegionTable table = new DataRegionTable("SpecimenDetail", this);
//        table.setFilter("QualityControlFlag", "Equals", "true");
//        table.setSort("GlobalUniqueId", SortDirection.ASC);
//        assertEquals("AAA07XK5-02", table.getDataAsText(0, "Global Unique Id"));
//        assertEquals("Conflicts found: AdditiveTypeId, DerivativeTypeId, PrimaryTypeId", table.getDataAsText(0, "Quality Control Comments"));
//        assertEquals("", table.getDataAsText(0, "Primary Type"));
//        assertEquals("", table.getDataAsText(0, "Additive Type"));
//
//        assertEquals("ABH00LT8-01", table.getDataAsText(2, "Global Unique Id"));
//        assertEquals("Conflicts found: VolumeUnits", table.getDataAsText(2, "Quality Control Comments"));
//        assertEquals("", table.getDataAsText(2, "Volume Units"));
//
//        clickLinkContainingText("history");
//        assertTextPresent("Blood (Whole)");
//        assertTextPresent("Vaginal Swab");
//        assertTextPresent("Vial is flagged for quality control");
//        clickLinkWithText("update");
//        setFormElement("qualityControlFlag", "false");
//        setFormElement("comments", "Manually removed flag");
//        clickNavButton("Save Changes");
//        assertTextPresent("Manually removed flag");
//        assertTextPresent("Conflicts found: AdditiveTypeId, DerivativeTypeId, PrimaryTypeId");
//        assertTextNotPresent("Vial is flagged for quality control");
//        clickLinkWithText("return to vial view");
//        assertTextNotPresent("AAA07XK5-02");
//        assertTextPresent("KBH00S5S-01");
    }

    private void verifyParticipantComments()
    {
        log("creating the participant/visit comment dataset");
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Create New Dataset");

        setFormElement("typeName", PARTICIPANT_CMT_DATASET);
        clickNavButton("Next");
        waitForElement(Locator.xpath("//input[@id='DatasetDesignerName']"), WAIT_FOR_JAVASCRIPT);

        // set the demographic data checkbox
        checkCheckbox(Locator.xpath("//input[@name='demographicData']"));

        // add a comment field
        ListHelper.setColumnName(this, 0, COMMENT_FIELD_NAME);
        ListHelper.setColumnLabel(this, 0, PARTICIPANT_COMMENT_LABEL);
        ListHelper.setColumnType(this, 0, ListHelper.ListColumnType.MutliLine);
        clickNavButton("Save");

        log("creating the participant/visit comment dataset");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Create New Dataset");

        setFormElement("typeName", PARTICIPANT_VISIT_CMT_DATASET);
        clickNavButton("Next");
        waitForElement(Locator.xpath("//input[@id='DatasetDesignerName']"), WAIT_FOR_JAVASCRIPT);

        // add a comment field
        ListHelper.setColumnName(this, 0, COMMENT_FIELD_NAME);
        ListHelper.setColumnLabel(this, 0, PARTICIPANT_VISIT_COMMENT_LABEL);
        ListHelper.setColumnType(this, 0, ListHelper.ListColumnType.MutliLine);
        clickNavButton("Save");

        log("configure comments");
        clickTab("Manage");
        clickLinkWithText("Manage Comments");
        if (isTextPresent("Comments can only be configured for studies with editable datasets"))
        {
            log("configure editable datasets");
            clickTab("Manage");
            clickLinkWithText("Manage Security");
            selectOptionByText("securityString", "Basic security with editable datasets");
            waitForPageToLoad();

            log("configure comments");
            clickLinkWithText(getStudyLabel());
            clickTab("Manage");
            clickLinkWithText("Manage Comments");
        }
        selectOptionByText("participantCommentDataSetId", PARTICIPANT_CMT_DATASET);
        waitForPageToLoad();
        selectOptionByText("participantCommentProperty", PARTICIPANT_COMMENT_LABEL);

        selectOptionByText("participantVisitCommentDataSetId", PARTICIPANT_VISIT_CMT_DATASET);
        waitForPageToLoad();
        selectOptionByText("participantVisitCommentProperty", PARTICIPANT_VISIT_COMMENT_LABEL);
        clickNavButton("Save");

        clickLinkWithText(getStudyLabel());
        waitForText("Blood (Whole)");
        clickLinkWithText("Blood (Whole)");
        clickNavButton("Enable Comments/QC");
        log("manage participant comments directly");
        clickMenuButton("Comments and QC", "Manage Mouse Comments");

        int datasetAuditEventCount = getDatasetAuditEventCount(); //inserting a new event should increase this by 1;
        clickNavButton("Insert New");
        setFormElement("quf_MouseId", "999320812");
        setFormElement("quf_" + COMMENT_FIELD_NAME, "Mouse Comment");
        clickNavButton("Submit");
        //Issue 14894: Datasets no longer audit row insertion
        verifyAuditEventAdded(datasetAuditEventCount);

        clickLinkWithText(getStudyLabel());
        waitForText("Blood (Whole)");
        clickLinkWithText("Blood (Whole)");
        setFilter("SpecimenDetail", "MouseId", "Equals", "999320812");

        assertTextPresent("Mouse Comment");
        clearAllFilters("SpecimenDetail", "MouseId");

        log("verify copying and moving vial comments");
        setFilter("SpecimenDetail", "GlobalUniqueId", "Equals", "AAA07XK5-01");
        selenium.click(".toggle");
        clickNavButton("Enable Comments/QC");
        clickMenuButton("Comments and QC", "Set Vial Comment or QC State for Selected");
        setFormElement("comments", "Vial Comment");
        clickNavButton("Save Changes");

        selenium.click(".toggle");
        clickMenuButton("Comments and QC", "Set Vial Comment or QC State for Selected");
        clickMenuButton("Copy or Move Comment(s)", "Copy", "To Mouse", "999320812");
        setFormElement("quf_" + COMMENT_FIELD_NAME, "Copied PTID Comment");
        clickNavButton("Submit");
        assertTextPresent("Copied PTID Comment");

        selenium.click(".toggle");
        clickMenuButton("Comments and QC", "Set Vial Comment or QC State for Selected");
        clickMenuButtonAndContinue("Copy or Move Comment(s)", "Move", "To Mouse", "999320812");
        getConfirmationAndWait();
        setFormElement("quf_" + COMMENT_FIELD_NAME, "Moved PTID Comment");
        clickNavButton("Submit");
        assertTextPresent("Moved PTID Comment");
        assertTextNotPresent("Mouse Comment");
        assertTextNotPresent("Vial Comment");
    }

    private void verifyAuditEventAdded(int previousCount)
    {
        log("Verify there is exactly one new DatasetAuditEvent, and it refers to the insertion of a new record");
        SelectRowsResponse selectResp = getDatasetAuditLog();
        List<Map<String,Object>> rows = selectResp.getRows();
        assertEquals("Unexpected size of datasetAuditEvent log", previousCount + 1, rows.size());
        assertEquals("A new dataset record was inserted", rows.get(rows.size()-1).get("Comment"));

    }

    private SelectRowsResponse getDatasetAuditLog()
    {

        SelectRowsCommand selectCmd = new SelectRowsCommand("auditLog", "DatasetAuditEvent");

        selectCmd.setMaxRows(-1);
        selectCmd.setContainerFilter(ContainerFilter.CurrentAndSubfolders);
        selectCmd.setColumns(Arrays.asList("*"));
        Connection cn = new Connection(getBaseURL(), getUsername(), PasswordUtil.getPassword());
        SelectRowsResponse selectResp = null;
        try
        {
            selectResp = selectCmd.execute(cn,  "/" +  getProjectName());
        }
        catch (Exception e)
        {
            fail("Error when attempting to verify audit trail: " + e.getMessage());
        }
        return selectResp;

    }

    private int getDatasetAuditEventCount()
    {
        SelectRowsResponse selectResp = getDatasetAuditLog();
        List<Map<String,Object>> rows = selectResp.getRows();
        return rows.size();
    }

    private void verifyDemographics()
    {
        clickLinkWithText(getFolderName());
        clickLinkWithText("Study Navigator");
        clickLinkWithText("24");
        assertTextPresent(DEMOGRAPHICS_DESCRIPTION);
        assertTextPresent("Male");
        assertTextPresent("African American or Black");
        clickLinkWithText("999320016");
        clickLinkWithText("125: EVC-1: Enrollment Vaccination", false);
        assertTextPresent("right deltoid");
        
        verifyDemoCustomizeOptions();
        verifyDatasetExport();
    }

    private void verifyDatasetExport()
    {
        pushLocation();
        exportDataRegion("Script", "R");
        goToAuditLog();
        selectOptionByText("view", "Query events");
        waitForPageToLoad();

        DataRegionTable auditTable =  new DataRegionTable("audit", this);
        String[][] columnAndValues = new String[][] {{"Created By", getDisplayName()},
                {"Project", PROJECT_NAME}, {"Container", STUDY_NAME}, {"SchemaName", "study"},
                {"QueryName", "DEM-1: Demographics"}, {"Comment", "Exported to script type r"}};
        for(String[] columnAndValue : columnAndValues)
        {
            log("Checking column: "+ columnAndValue[0]);
            assertEquals(columnAndValue[1], auditTable.getDataAsText(0, columnAndValue[0]));
        }
        clickLinkContainingText("details");

        popLocation();
    }

    private void verifyDemoCustomizeOptions()
    {
        log("verify demographic data set not present");
        clickLinkContainingText(DEMOGRAPHICS_TITLE);
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        assertFalse(CustomizeViewsHelper.isColumnPresent(this, "MouseVisit/DEM-1"));
    }

    protected void verifyVisitMapPage()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Visits");

        // test optional/required/not associated
        clickLinkWithText("edit", 0);
        selectOption("dataSetStatus", 0, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 1, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 2, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 3, "OPTIONAL");
        selectOption("dataSetStatus", 4, "OPTIONAL");
        selectOption("dataSetStatus", 5, "OPTIONAL");
        selectOption("dataSetStatus", 6, "REQUIRED");
        selectOption("dataSetStatus", 7, "REQUIRED");
        selectOption("dataSetStatus", 8, "REQUIRED");
        clickNavButton("Save");
        clickLinkWithText("edit", 0);
        selectOption("dataSetStatus", 0, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 1, "OPTIONAL");
        selectOption("dataSetStatus", 2, "REQUIRED");
        selectOption("dataSetStatus", 3, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 4, "OPTIONAL");
        selectOption("dataSetStatus", 5, "REQUIRED");
        selectOption("dataSetStatus", 6, "NOT_ASSOCIATED");
        selectOption("dataSetStatus", 7, "OPTIONAL");
        selectOption("dataSetStatus", 8, "REQUIRED");
        clickNavButton("Save");
        clickLinkWithText("edit", 0);
        assertSelectOption("dataSetStatus", 0, "NOT_ASSOCIATED");
        assertSelectOption("dataSetStatus", 1, "OPTIONAL");
        assertSelectOption("dataSetStatus", 2, "REQUIRED");
        assertSelectOption("dataSetStatus", 3, "NOT_ASSOCIATED");
        assertSelectOption("dataSetStatus", 4, "OPTIONAL");
        assertSelectOption("dataSetStatus", 5, "REQUIRED");
        assertSelectOption("dataSetStatus", 6, "NOT_ASSOCIATED");
        assertSelectOption("dataSetStatus", 7, "OPTIONAL");
        assertSelectOption("dataSetStatus", 8, "REQUIRED");
    }

    protected void verifyManageDatasetsPage()
    {
        clickLinkWithText(getFolderName());
        clickTab("Manage");
        clickLinkWithText("Manage Datasets");

        clickLinkWithText("489");
        assertTextPresent("ESIdt");
        assertTextPresent("Form Completion Date");
        assertTableCellTextEquals("details", 4, 1, "false");     // "Demographics Data" should be false

        // Verify that "Demographics Data" is checked and description is set
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("DEM-1: Demographics");
        assertTableCellTextEquals("details", 4, 1, "true");
        assertTableCellTextEquals("details", 3, 3, DEMOGRAPHICS_DESCRIPTION);

        // "Demographics Data" bit needs to be false for the rest of the test
        setDemographicsBit("DEM-1: Demographics", false);

        log("verify ");
        clickButtonContainingText("View Data");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        assertTrue(CustomizeViewsHelper.isColumnPresent(this, "MouseVisit/DEM-1"));
    }

    private void verifyHiddenVisits()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Study Navigator");
        assertTextNotPresent("Screening Cycle");
        assertTextNotPresent("Cycle 1");
        assertTextPresent("Pre-exist Cond");
        clickLinkWithText("Show All Datasets");
        assertTextPresent("Screening Cycle");
        assertTextPresent("Cycle 1");
        assertTextPresent("Pre-exist Cond");
    }

    private void verifyVisitImportMapping()
    {
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Visits");
        clickLinkWithText("Visit Import Mapping");
        assertTableRowsEqual("customMapping", 2, VISIT_IMPORT_MAPPING.replace("SequenceNum", "Sequence Number Mapping"));

        assertEquals("Incorrect number of gray cells", 60, countTableCells(null, true));
        assertEquals("Incorrect number of non-gray \"Int. Vis. %{S.1.1} .%{S.2.1}\" cells", 1, countTableCells("Int. Vis. %{S.1.1} .%{S.2.1}", false));
        assertEquals("Incorrect number of gray \"Int. Vis. %{S.1.1} .%{S.2.1}\" cells", 18, countTableCells("Int. Vis. %{S.1.1} .%{S.2.1}", true));
        assertEquals("Incorrect number of non-gray \"Soc Imp Log #%{S.3.2}\" cells", 1, countTableCells("Soc Imp Log #%{S.3.2}", false));
        assertEquals("Incorrect number of gray \"Soc Imp Log #%{S.3.2}\" cells", 1, countTableCells("Soc Imp Log #%{S.3.2}", true));
        assertEquals("Incorrect number of non-gray \"ConMeds Log #%{S.3.2}\" cells", 1, countTableCells("ConMeds Log #%{S.3.2}", false));
        assertEquals("Incorrect number of gray \"ConMeds Log #%{S.3.2}\" cells", 1, countTableCells("ConMeds Log #%{S.3.2}", true));

        // Replace custom visit mapping and verify
        String replaceMapping = "Name\tSequenceNum\nBarBar\t4839\nFoofoo\t9732";
        clickLinkWithText("Replace Custom Mapping");
        setLongTextField("tsv", replaceMapping);
        clickNavButton("Submit");
        assertTableRowsEqual("customMapping", 2, replaceMapping.replace("SequenceNum", "Sequence Number Mapping"));
        assertTextNotPresent("Cycle 10");
        assertTextNotPresent("All Done");

        assertEquals("Incorrect number of gray cells", 54, countTableCells(null, true));
        assertEquals("Incorrect number of non-gray \"Int. Vis. %{S.1.1} .%{S.2.1}\" cells", 1, countTableCells("Int. Vis. %{S.1.1} .%{S.2.1}", false));
        assertEquals("Incorrect number of gray \"Int. Vis. %{S.1.1} .%{S.2.1}\" cells", 18, countTableCells("Int. Vis. %{S.1.1} .%{S.2.1}", true));
        assertEquals("Incorrect number of non-gray \"Soc Imp Log #%{S.3.2}\" cells", 1, countTableCells("Soc Imp Log #%{S.3.2}", false));
        assertEquals("Incorrect number of gray \"Soc Imp Log #%{S.3.2}\" cells", 0, countTableCells("Soc Imp Log #%{S.3.2}", true));
        assertEquals("Incorrect number of non-gray \"ConMeds Log #%{S.3.2}\" cells", 1, countTableCells("ConMeds Log #%{S.3.2}", false));
        assertEquals("Incorrect number of gray \"ConMeds Log #%{S.3.2}\" cells", 0, countTableCells("ConMeds Log #%{S.3.2}", true));

        clickLinkWithText(getFolderName());
        clickLinkWithText("47 datasets");
        clickLinkWithText("Types");
        log("Verifying sequence numbers and visit names imported correctly");

        DataRegionTable table = new DataRegionTable("Dataset", this, true, true);
        List<String> sequenceNums = table.getColumnDataAsText("Sequence Num");
        assertEquals("Incorrect number of rows in Types dataset", 48, sequenceNums.size());

        int sn101 = 0;
        int sn201 = 0;

        for (String seqNum : sequenceNums)
        {
            // Use startsWith because StudyTest and StudyExportTest have different default format strings
            if (seqNum.startsWith("101.0"))
                sn101++;
            else if (seqNum.startsWith("201.0"))
                sn201++;
            else
                fail("Unexpected sequence number: " + seqNum);
        }

        assertEquals("Incorrect count for sequence number 101.0", 24, sn101);
        assertEquals("Incorrect count for sequence number 201.0", 24, sn201);
    }

    // Either param can be null
    private int countTableCells(String text, Boolean grayed)
    {
        List<String> parts = new LinkedList<String>();

        if (null != text)
            parts.add("contains(text(), '" + text + "')");

        if (null != grayed)
        {
            if (grayed)
                parts.add("contains(@class, 'labkey-mv')");
            else
                parts.add("not(contains(@class, 'labkey-mv'))");
        }

        String path = "//td[" + StringUtils.join(parts, " and ") + "]";
        return getXpathCount(Locator.xpath(path));
    }

    private void verifyCohorts()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Study Navigator");
        clickLinkWithText("24");

        // verify that cohorts are working
        assertTextPresent("999320016");
        assertTextPresent("999320518");

        clickMenuButton("Mouse Groups", "Cohorts", "Group 1");
        assertTextPresent("999320016");
        assertTextNotPresent("999320518");

        clickMenuButton("Mouse Groups", "Cohorts", "Group 2");
        assertTextNotPresent("999320016");
        assertTextPresent("999320518");

        // verify that the participant view respects the cohort filter:
        setSort("Dataset", "MouseId", SortDirection.ASC);
        clickLinkWithText("999320518");
        clickLinkWithText("125: EVC-1: Enrollment Vaccination", false);
        assertTextNotPresent("Group 1");
        assertTextPresent("Group 2");
        clickLinkWithText("Next Mouse");
        assertTextNotPresent("Group 1");
        assertTextPresent("Group 2");
        clickLinkWithText("Next Mouse");
        assertTextNotPresent("Group 1");
        assertTextPresent("Group 2");
        clickLinkWithText("Next Mouse");
    }
}
