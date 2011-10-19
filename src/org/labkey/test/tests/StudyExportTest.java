/*
 * Copyright (c) 2007-2011 LabKey Corporation
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
import org.labkey.test.util.ExtHelper;
import org.labkey.test.tests.StudyManualTest;
import org.labkey.test.util.ListHelper;

import java.io.File;

/**
 * User: brittp
 * Date: Dec 8, 2006
 * Time: 4:30:24 PM
 */
public class StudyExportTest extends StudyManualTest
{
    private static final String SPECIMEN_ARCHIVE_B = "/sampledata/study/specimens/sample_b.specimens";
    private static final String DEMOGRAPHICS_DATASET = "DEM-1: Demographics";
    private static final String TEST_ADD_ENTRY = "999000000";

    private final String DATASET_DATA_FILE = getLabKeyRoot() + "/sampledata/dataLoading/excel/dataset_data.xls";
    private static final String HIDDEN_DATASET = "URS-1: Screening Urinalysis";
    private static final String MODIFIED_DATASET = "Quality Control Report"; // Empty dataset.
    private static final String REORDERED_DATASET1 = "LLS-1: Screening Local Lab Results (Page 1)";
    private static final String REORDERED_DATASET2 = "LLS-2: Screening Local Lab Results (Page 2)";
    private static final String CATEGORY = "Test Category";
    private static final String DATE_FORMAT = "dd/mm hh:mma";
    private static final String NUMBER_FORMAT = "00.00";
    private static final String MODIFIED_PARTICIPANT = "999321033";
    private static final String GROUP_2 = "Group 2";
    private static final String COLUMN_DESC = "Test Column Description";
    private static final String MODIFIED_VISIT = "Cycle 2";

    @Override
    protected void doCreateSteps()
    {
        // manually create a study and load a specimen archive
        log("Creating study manually");
        createStudyManually();

        // import the specimens and wait for both datasets & specimens to load
        SpecimenImporter specimenImporter = new SpecimenImporter(new File(getPipelinePath()), new File(getLabKeyRoot(), SPECIMEN_ARCHIVE_A), new File(getLabKeyRoot(), ARCHIVE_TEMP_DIR), getFolderName(), 2);
        specimenImporter.importAndWaitForComplete();

        // export manually created study to individual files using "legacy" formats
        exportStudy(false, false);

        // delete manually created study
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickNavButton("Delete Study");
        checkCheckbox("confirm");
        clickNavButton("Delete", WAIT_FOR_PAGE * 2); // TODO: Shorten wait (Issue 12731)

        log("Importing exported study (legacy formats)");
        clickNavButton("Import Study");
        clickNavButton("Import Study Using Pipeline");
        waitAndClick(Locator.fileTreeByName("export"));
        ExtHelper.waitForImportDataEnabled(this);
        ExtHelper.clickFileBrowserFileCheckbox(this, "study.xml");

        selectImportDataAction("Import Study");

        // wait for study & specimen load to complete
        waitForPipelineJobsToComplete(3, "study and specimen import (legacy formats)", false);

        // delete "export" directory
        deleteDir(new File(getPipelinePath() + "export"));

        // change settings that aren't roundtripped using "legacy" formats
        setDemographicsDescription();
        createCustomAssays();
        setFormatStrings();
        setManualCohorts();
        modifyVisits();
        importCustomVisitMapping();
        changeDatasetOrder("16");
        setDatasetCategory(MODIFIED_DATASET, CATEGORY);
        hideDataset(HIDDEN_DATASET);
        modifyDatasetColumn(MODIFIED_DATASET);

        ListHelper.importListArchive(this, getFolderName(), new File(getLabKeyRoot(), "/sampledata/rlabkey/listArchive.zip"));

        // export new study to zip file using "xml" formats
        exportStudy(true, true);

        // delete the study
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickNavButton("Delete Study");
        checkCheckbox("confirm");
        clickNavButton("Delete", WAIT_FOR_PAGE *2); // TODO: Shorten wait (Issue 12731)

        log("Importing exported study (xml formats)");
        clickNavButton("Import Study");
        clickNavButton("Import Study Using Pipeline");
        waitAndClick(Locator.fileTreeByName("export"));
        ExtHelper.selectAllFileBrowserFiles(this);

        selectImportDataAction("Import Study");

        // wait for study & specimen load
        waitForPipelineJobsToComplete(4, "study and specimen import (xml formats)", false);

        // TODO: Move this earlier (after legacy format import) once issue 10074 is resolved. 
        setDemographicsBit();
    }


    private void exportStudy(boolean useXmlFormat, boolean zipFile)
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickNavButton("Export Study");

        assertTextPresent("Visit Map", "Cohort Settings", "QC State Settings", "CRF Datasets", "Assay Datasets", "Specimens", "Participant Comment Settings", "Queries", "Custom Views", "Reports", "Lists");

        checkRadioButton("format", useXmlFormat ? "new" : "old");
        checkRadioButton("location", zipFile ? "1" : "0");  // zip file vs. individual files
        clickNavButton("Export");
    }


    @Override
    protected void waitForSpecimenImport()
    {
        // specimen import is already complete
    }

    @Override
    protected void verifyStudyAndDatasets()
    {
        super.verifyStudyAndDatasets();

        // verify reordered, categorized, & hidden datasets.
        clickLinkWithText(getFolderName());
        assertTextBefore(REORDERED_DATASET2, REORDERED_DATASET1);
        assertLinkNotPresentWithText(HIDDEN_DATASET);
        assertTextBefore(CATEGORY, MODIFIED_DATASET);

        // verify format strings
        clickLinkWithText("Manage Datasets");
        assertFormElementEquals(Locator.id("dateFormat"), DATE_FORMAT);
        assertFormElementEquals(Locator.id("numberFormat"), NUMBER_FORMAT);

        // verify dataset category on dataset management page
        assertTextPresent(CATEGORY, 1);
        assertElementContains(Locator.xpath("//tr[./td/a[text() = '" + MODIFIED_DATASET + "']]/td[4]"), CATEGORY);

        // verify dataset columns
        clickLinkWithText(MODIFIED_DATASET);
        assertChecked(Locator.xpath("//tr[9]/td[6]/input"));
        assertElementContains(Locator.xpath("//tr[9]/td[7]"), COLUMN_DESC);
        assertTextPresent(CATEGORY);

        // TODO: verify lookup

        // verify manual cohorts
        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Cohorts");
        assertFormElementEquals(Locator.id("manualCohortAssignmentEnabled"), "on");
        clickLinkWithText(getFolderName());
        clickLinkWithText(DEMOGRAPHICS_DATASET);
        clickMenuButton("Mouse Groups", "Cohorts", GROUP_2);
        clickMenuButton("QC State", "All data");
        assertTextPresent(MODIFIED_PARTICIPANT);

        // verify visit display order
        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Visits");
        assertTextBefore("Cycle 3", MODIFIED_VISIT);

        // verify visit modifications
        editVisit(MODIFIED_VISIT);
        assertFormElementEquals("dataSetStatus", "OPTIONAL");
        assertOptionEquals("cohortId", GROUP_2);
    }

    @Override
    protected void verifySpecimens()
    {
        super.verifySpecimens();

        // verify that we correctly warn when specimen tracking hasn't been configured
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Create New Request");
        assertTextPresent("Specimen management is not configured for this study");

        // configure specimen tracking
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Request Statuses");
        setFormElement("newLabel", "New Request");
        clickNavButton("Save");
        setFormElement("newLabel", "Pending Approval");
        clickNavButton("Save");
        setFormElement("newLabel", "Complete");
        clickNavButton("Done");
        clickLinkWithText("Manage Actors and Groups");
        setFormElement("newLabel", "Institutional Review Board");
        selectOptionByText("newPerSite", "Multiple Per Study (Location Affiliated)");
        clickNavButton("Save");
        setFormElement("newLabel", "Scientific Leadership Group");
        selectOptionByText("newPerSite", "One Per Study");
        clickNavButton("Save");
        clickLinkWithText("Update Members");
        clickLinkWithText("FHCRC - Seattle");
        assertTextPresent("Institutional Review Board, FHCRC - Seattle");
        assertTextPresent("This group currently has no members.");
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Default Requirements");
        selectOptionByText("providerActor", "Institutional Review Board");
        setFormElement("providerDescription", "To be deleted");
        clickNavButtonByIndex("Add Requirement", 1);
        assertTextPresent("To be deleted");
        clickLinkWithText("Delete");
        assertTextNotPresent("To be deleted");
        selectOptionByText("providerActor", "Institutional Review Board");
        setFormElement("providerDescription", "Providing lab approval");
        clickNavButtonByIndex("Add Requirement", 1);
        selectOptionByText("receiverActor", "Institutional Review Board");
        setFormElement("receiverDescription", "Receiving lab approval");
        clickNavButtonByIndex("Add Requirement", 2);
        selectOptionByText("generalActor", "Scientific Leadership Group");
        setFormElement("generalDescription", "SLG Request Approval");
        clickNavButtonByIndex("Add Requirement", 3);
        clickLinkWithText("manage study");

        // create specimen request
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Study Navigator");

        assertLinkNotPresentWithText("24");
        selectOptionByText("QCState", "All data");
        waitForPageToLoad();

        clickLinkWithText("24");
        checkCheckbox(Locator.checkboxByName(".toggle"));
        clickNavButton("View Specimens");
        assertLinkPresentWithText("999320016");
        assertLinkPresentWithText("999320518");
        clickLinkWithText("Show Vial Info");
        assertLinkPresentWithText("999320016");
        checkCheckbox(Locator.checkboxByName(".toggle"));
        clickMenuButton("Request Options", "Create New Request");
        assertTextPresent("HAQ0003Y-09");
        assertTextPresent("BAQ00051-09");
        assertTextNotPresent("KAQ0003Q-01");
        selectOptionByText("destinationSite", "Duke University");
        setFormElements("textarea", "inputs", new String[] { "An Assay Plan", "Duke University, NC", "My comments" });
        clickNavButton("Create and View Details");

        assertTextPresent("This request has not been submitted");
        assertNavButtonPresent("Cancel Request");
        assertNavButtonPresent("Submit Request");
        clickLinkWithText("Specimen Requests");

        assertNavButtonPresent("Submit");
        assertNavButtonPresent("Cancel");
        assertNavButtonPresent("Details");
        assertTextPresent("Not Yet Submitted");
        clickNavButton("Submit", 0);
        getConfirmationAndWait();
        clickLinkWithText("Specimen Requests");
        assertNavButtonNotPresent("Submit");
        assertNavButtonPresent("Details");
        assertTextPresent("New Request");

        // test auto-fill:
        clickNavButton("Create New Request");
        String inputs = selenium.getValue("inputs");
        System.out.println(inputs);
        assertFormElementNotEquals(Locator.dom("document.forms[1].inputs[1]"), "Duke University, NC");
        selectOptionByText("destinationSite", "Duke University");
        assertFormElementEquals(Locator.dom("document.forms[1].inputs[1]"), "Duke University, NC");
        clickNavButton("Cancel");

        // manage new request
        clickNavButton("Details");
        assertTextNotPresent("Complete");
        assertTextNotPresent("WARNING: Missing Specimens");
        assertTextPresent("New Request");
        assertTextNotPresent("Pending Approval");
        clickLinkWithText("Update Request");
        selectOptionByText("status", "Pending Approval");
        setFormElement("comments", "Request is now pending.");
        clickNavButton("Save Changes and Send Notifications");
        assertTextNotPresent("New Request");
        assertTextPresent("Pending Approval");
        clickLinkWithText("Details", 0);
        assertTextPresent("Duke University");
        assertTextPresent("Providing lab approval");
        checkCheckbox("complete");
        setFormElement("comment", "Approval granted.");
        if (isFileUploadAvailable())
            setFormElement("formFiles[0]", new File(getLabKeyRoot() + VISIT_MAP).getPath());
        else
            log("File upload skipped.");
        clickNavButton("Save Changes and Send Notifications");
        assertTextPresent("Complete");

        clickLinkWithText("Details", 1);
        clickNavButton("Delete Requirement");
        assertTextNotPresent("Receiving lab approval");

        clickLinkWithText("Originating Location Specimen Lists");
        assertTextPresent("WARNING: The requirements for this request are incomplete");
        assertTextPresent("KCMC, Moshi, Tanzania");
        clickNavButton("Cancel");

        clickLinkWithText("View History");
        assertTextPresent("Request is now pending.");
        assertTextPresent("Approval granted.");
        assertTextPresent("Institutional Review Board (Duke University), Receiving lab approval");
        if (isFileUploadAvailable())
            assertTextPresent(VISIT_MAP.substring(VISIT_MAP.lastIndexOf("/") + 1));

        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        enterPermissionsUI();
        clickNavButton("Study Security");

        // enable advanced study security
        selectOptionByValue("securityString", "ADVANCED_READ");
        waitForPageToLoad(30000);

        click(Locator.xpath("//td[.='Users']/..//input[@value='READ']"));
        clickAndWait(Locator.id("groupUpdateButton"));

        // set the QC state 
        clickLinkWithText(getFolderName());
        clickLinkWithText(DEMOGRAPHICS_DATASET);
        clickMenuButton("QC State", "All data");
        checkAllOnPage("Dataset");
        clickMenuButton("QC State", "Update state of selected rows");
        selectOptionByText("newState", "clean");
        setFormElement("comments", "This data is clean.");
        clickNavButton("Update Status");
        clickMenuButton("QC State", "clean");

        // test specimen comments
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Plasma, Unknown Processing");
        clickNavButton("Enable Comments/QC");
        checkAllOnPage("SpecimenDetail");
        clickMenuButton("Comments and QC", "Set Vial Comment or QC State for Selected");
        setFormElement("comments", "These vials are very important.");
        clickNavButton("Save Changes");
        assertTextPresent("These vials are very important.", 25);
        setFilter("SpecimenDetail", "MouseId", "Equals", "999320824");
        checkAllOnPage("SpecimenDetail");
        clickMenuButtonAndContinue("Comments and QC", "Clear Vial Comments for Selected");
        getConfirmationAndWait();
        assertTextNotPresent("These vials are very important.");
        clearFilter("SpecimenDetail", "MouseId");
        assertTextPresent("These vials are very important.", 23);
        clickMenuButton("Comments and QC", "Exit Comments and QC mode");

        // import second archive, verify that that data is merged:
        SpecimenImporter importer = new SpecimenImporter(new File(getPipelinePath()), new File(getLabKeyRoot(), SPECIMEN_ARCHIVE_B), new File(getLabKeyRoot(), ARCHIVE_TEMP_DIR), getStudyLabel(), 5);
        importer.importAndWaitForComplete();

        // verify that comments remain after second specimen load
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Plasma, Unknown Processing");
        assertTextPresent("These vials are very important.", 2);

        // check to see that data in the specimen archive was merged correctly:
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("By Vial");
        clickMenuButton("Page Size", "Show All");
        assertTextPresent("DRT000XX-01");
        clickLinkWithText("Search");
        clickLinkWithText("Search by specimen");

//        WARNING: Using getFormElementNameByTableCaption() is dangerous... if muliple values are returned their
//        order is unpredictable, since they come back in keyset order.  The code below breaks under Java 6.
//
//        String[] globalUniqueIDCompareElems = getFormElementNameByTableCaption("Specimen Number", 0, 1);
//        String[] globalUniqueIDValueElems = getFormElementNameByTableCaption("Specimen Number", 0, 2);
//        String[] participantIDFormElems = getFormElementNameByTableCaption("Participant Id", 0, 1);
//        setFormElement(globalUniqueIDCompareElems[1], "CONTAINS");
//        setFormElement(globalUniqueIDValueElems[0], "1416");
//        setFormElement(participantIDFormElems[2], "999320528");

        // Hard-code the element names, since code above is unpredictable
        selectOptionByValue("searchParams[0].value", "999320528");
        selectOptionByValue("searchParams[1].value", "Enroll/Vacc #1");

        clickNavButton("Search");
        assertTextPresent("999320528");
        clickLinkWithText("Show Vial Info");
        // if our search worked, we'll only have six vials:
        assertTextPresent("[history]", 6);
        assertLinkPresentWithTextCount("999320528", 6);
        assertTextNotPresent("DRT000XX-01");
        clickLinkWithText("[history]");
        assertTextPresent("GAA082NH-01");
        assertTextPresent("BAD");
        assertTextPresent("1.0&nbsp;ML");
        assertTextPresent("Added Comments");
        assertTextPresent("Johannesburg, South Africa");

        clickLinkWithText(getStudyLabel());
        clickLinkWithText("View Existing Requests");
        clickNavButton("Details");
        assertTextPresent("WARNING: Missing Specimens");
        clickNavButton("Delete missing specimens", 0);
        getConfirmationAndWait();
        assertTextNotPresent("WARNING: Missing Specimens");
        assertTextPresent("Duke University");
        assertTextPresent("An Assay Plan");
        assertTextPresent("Providing lab approval");
        assertTextPresent("HAQ0003Y-09");
        assertTextPresent("BAQ00051-09");
        assertTextNotPresent("BAQ00051-10");
        assertTextPresent("BAQ00051-11");

        log("Test editing rows in a dataset");
        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());

        enterPermissionsUI();
        clickNavButton("Study Security");

        selectOptionByValue("securityString", "BASIC_WRITE");
        waitForPageToLoad(30000);

        clickLinkWithText(getFolderName());
        clickLinkWithText("DEM-1: Demographics");

        clickLinkWithText("edit");
        setFormElement("quf_DEMbdt", "2001-11-11");
        clickNavButton("Submit");
        clickMenuButton("QC State", "unknown QC");
        assertTextPresent("2001-11-11");

        log("Test adding a row to a dataset");
        clickNavButton("Insert New");
        clickNavButton("Submit");
        assertTextPresent("This field is required");
        setFormElement("quf_MouseId", TEST_ADD_ENTRY);
        setFormElement("quf_SequenceNum", "123");
        clickNavButton("Submit");
        clickMenuButton("QC State", "All data");
        assertTextPresent(TEST_ADD_ENTRY);

        // Make sure that we can view its participant page immediately
        pushLocation();
        clickLinkWithText(TEST_ADD_ENTRY);
        assertTextPresent("Mouse - " + TEST_ADD_ENTRY);
        assertTextPresent("DEM-1: Demographics");
        popLocation();

        log("Test deleting rows in a dataset");
        checkCheckbox(Locator.raw("//input[contains(@value, '999320529')]"));
        clickNavButton("Delete", 0);
        getConfirmationAndWait();
        assertTextNotPresent("999320529");

        // configure QC state management to show all data by default so the next steps don't have to keep changing the state:
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Dataset QC States");
        selectOptionByText("showPrivateDataByDefault", "All data");
        clickNavButton("Save");

        // Test creating and importing a dataset from an excel file
        doTestDatasetImport();
    }

    protected boolean comparePaths(String path1, String path2)
    {
        String[] parseWith = { "/", "\\\\" };
        for (String parser1 : parseWith)
        {
            String[] path1Split = path1.split(parser1);
            for  (String parser2 : parseWith)
            {
                String[] path2Split = path2.split(parser2);
                if (path1Split.length == path2Split.length)
                {
                    int index = 0;
                    while (path1Split[index].compareTo(path2Split[index]) == 0)
                    {
                        index++;
                        if (index > path2Split.length - 1)
                            return true;
                    }
                }
            }
        }
        return false;
    }

    private void changeDatasetOrder(String value)
    {
        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Change Display Order");
        selectOptionByValue("items", value);
        clickNavButton("Move Down", 0);
        clickNavButton("Save");
    }

    protected void hideDataset(String dataset)
    {
        clickLinkWithText(getFolderName());
        setVisibleBit(dataset, false);
    }

    protected void setDatasetCategory(String dataset, String category)
    {
        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText(dataset);
        clickNavButton("Edit Definition");
        waitForElement(Locator.name("dsCategory"), WAIT_FOR_PAGE);
        setFormElement("dsCategory", category);
        clickNavButton("Save");
    }

    private void modifyVisits()
    {
        hideSceeningVisit();
        clickLinkWithText("Change Visit Order");
        checkCheckbox("explicitDisplayOrder");
        selectOptionByText("displayOrderItems", MODIFIED_VISIT);
        clickNavButton("Move Down", 0);
        clickNavButton("Save");
        editVisit(MODIFIED_VISIT);
        selectOption("dataSetStatus", 0, "OPTIONAL");
        selectOptionByText("cohortId", GROUP_2);
        clickNavButton("Save");
        
    }

    private void modifyDatasetColumn(String dataset)
    {
        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText(dataset);
        clickNavButton("Edit Definition");
        waitForElement(Locator.name("ff_name0"), WAIT_FOR_PAGE);
        click(Locator.name("ff_name0"));
        click(Locator.xpath("//span[contains(@class,'x-tab-strip-text') and text()='Advanced']"));
        waitForElement(Locator.name("mvEnabled"), WAIT_FOR_JAVASCRIPT);
        checkCheckbox("mvEnabled");
        setFormElement(Locator.id("propertyDescription"), COLUMN_DESC);
        // TODO: add lookups for current & other folders
        clickNavButton("Save");
    }

    private void setFormatStrings()
    {
        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Datasets");
        setText("dateFormat", DATE_FORMAT);
        setText("numberFormat", NUMBER_FORMAT);
        clickNavButton("Submit");
    }

    private void setManualCohorts()
    {
        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Cohorts");
        clickRadioButtonById("manualCohortAssignmentEnabled");
        waitForPageToLoad();
        setParticipantCohort(MODIFIED_PARTICIPANT, GROUP_2);
        clickNavButton("Save");
    }

    private void setParticipantCohort(String ptid, String cohort)
    {
        selectOptionByText(Locator.xpath("//tr[./td = '" + ptid + "']//select"), cohort);
    }

    protected void editVisit(String visit)
    {
        clickAndWait(Locator.xpath("//table[@id='visits']//tr[./th[text() = '" + visit + "']]/td/a[text() = 'edit']"));
    }

    protected void doTestDatasetImport()
    {
        if (!isFileUploadAvailable())
            return;

        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Create New Dataset");
        setFormElement("typeName", "fileImportDataset");
        clickCheckbox("fileImport");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@name='uploadFormElement']"), WAIT_FOR_JAVASCRIPT);

        File datasetFile = new File(DATASET_DATA_FILE);
        setFormElement("uploadFormElement", datasetFile);

        waitForElement(Locator.xpath("//span[@id='button_Import']"), WAIT_FOR_JAVASCRIPT);

        Locator.XPathLocator mouseId = Locator.xpath("//label[contains(@class, 'x-form-item-label') and text() ='MouseId:']/../div/div");
        ExtHelper.selectGWTComboBoxItem(this, mouseId, "name");
        Locator.XPathLocator sequenceNum = Locator.xpath("//label[contains(@class, 'x-form-item-label') and text() ='Sequence Num:']/../div/div");
        ExtHelper.selectGWTComboBoxItem(this, sequenceNum, "visit number");

        waitAndClickNavButton("Import");
        waitForPageToLoad();

        assertTextPresent("kevin");
        assertTextPresent("chimpanzee");
    }

    @Override
    protected void doCleanup() throws Exception
    {
        super.doCleanup();

        deleteDir(new File(getPipelinePath() + "export"));
    }
}
