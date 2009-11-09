/*
 * Copyright (c) 2009 LabKey Corporation
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
package org.labkey.test.drt;

import org.labkey.test.Locator;
import org.labkey.test.SortDirection;

import java.io.File;

/**
 * User: adam
 * Date: Apr 3, 2009
 * Time: 9:18:32 AM
 */
public class StudyTest extends StudyBaseTest
{
    protected static final String PROJECT_NAME = "ImportStudyVerifyProject";
    protected static final String FOLDER_NAME = "My Import Study";
    protected static final String ARCHIVE_TEMP_DIR = getSampleDataPath() + "drt_temp";
    protected static final String DEMOGRAPHICS_DESCRIPTION = "This is the demographics dataset, dammit.";
//    protected static final String DEMOGRAPHICS_DESCRIPTION = "This is the demographics dataset, dammit. Here are some ‘special symbols’ – they help test that we're roundtripping in UTF-8.";

    protected String _tsv = "participantid\tsequencenum\tvisitdate\tSampleId\tDateField\tNumberField\tTextField\treplace\n" +
        "1234\t1\t1/1/2006\t1234_A\t2/1/2006\t1.2\ttext\t\n" +
        "1234\t1\t1/1/2006\t1234_B\t2/1/2006\t1.2\ttext\t\n";

    protected static final String SPECIMEN_ARCHIVE_A = getSampleDataPath() + "specimens/sample_a.specimens";

    private SpecimenImporter _specimenImporter;

    // specimen comment constants
    private static final String PARTICIPANT_CMT_DATASET = "Participant Comments";
    private static final String PARTICIPANT_VISIT_CMT_DATASET = "Participant Visit Comments";
    private static final String COMMENT_FIELD_NAME = "comment";
    private static final String PARTICIPANT_COMMENT_LABEL = "participant comment";
    private static final String PARTICIPANT_VISIT_COMMENT_LABEL = "participant visit comment";
    
    protected void doCreateSteps()
    {
        importStudy();
        startSpecimenImport(2);

        // wait for study (but not specimens) to finish loading
        waitForPipelineJobsToComplete(1, "study import");
    }

    protected void doVerifySteps()
    {
        verifyStudyAndDatasets();
        waitForSpecimenImport();
        verifySpecimens();
        verifyParticipantComments();
    }

    protected void startSpecimenImport(int completeJobsExpected)
    {
        // Start importing the specimens as well.  We'll let this load in the background while executing the first set of
        // verification steps.  Doing this in parallel speeds up the test.
        _specimenImporter = new SpecimenImporter(new File(getPipelinePath()), new File(getLabKeyRoot(), SPECIMEN_ARCHIVE_A), new File(getLabKeyRoot(), ARCHIVE_TEMP_DIR), getFolderName(), completeJobsExpected);
        _specimenImporter.startImport();
    }

    protected void waitForSpecimenImport()
    {
        _specimenImporter.waitForComplete();
    }

    protected void verifyStudyAndDatasets()
    {
        verifyDemographics();
        verifyVisitMapPage();
        verifyManageDatasetsPage();
        verifyHiddenVisits();
        verifyCohorts();

        // configure QC state management before importing duplicate data
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage QC States");
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
        clickLinkWithText("verifyAssay");
        assertTextPresent("QC State");
        assertTextNotPresent("1234_B");
        clickMenuButton("QC State", "QCState:All data");
        clickButton("QC State", 0);
        assertTextPresent("unknown QC");
        assertTextPresent("1234_B");

        //Import duplicate data
        clickNavButton("Import Data");
        setFormElement("tsv", _tsv);
        clickNavButton("Import Data");
        assertTextPresent("Duplicates were found");
        //Now explicitly replace
        _tsv = "participantid\tsequencenum\tvisitdate\tSampleId\tDateField\tNumberField\tTextField\treplace\n" +
                "1234\t1\t1/1/2006\t1234_A\t2/1/2006\t5000\tnew text\tTRUE\n" +
                "1234\t1\t1/1/2006\t1234_B\t2/1/2006\t5000\tnew text\tTRUE\n";
        setFormElement("tsv", _tsv);
        clickNavButton("Import Data");
        assertTextPresent("5000.0");
        assertTextPresent("new text");
        assertTextPresent("QC State");
        assertTextPresent("unknown QC");
    }

    protected void verifySpecimens()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Blood (Whole)");
        clickMenuButton("Page Size", "Page Size:All");
        assertTextNotPresent("DRT000XX-01");
        assertTextPresent("GAA082NH-01");
        clickLinkWithText("Hide Vial Info");
        assertTextPresent("Total:");
        assertTextPresent("466");

        assertTextNotPresent("BAD");

        clickLinkWithText("Show Vial Info");
        clickLinkContainingText("history");
        // verify that we're correctly parsing frozen time, which is a date with a time portion only:
        assertTextPresent("15:30:00");
        assertTextPresent("2.0&nbsp;ML");
        assertTextNotPresent("Added Comments");
        // confirm collection location:
        assertTextPresent("KCMC, Moshi, Tanzania");
        // confirm historical locations:
        assertTextPresent("Contract Lab Services, Johannesburg, South Africa");
        assertTextPresent("Aurum Health KOSH Lab, Orkney, South Africa");

        clickLinkWithText("Specimens");
        clickNavButton("Search");
        setFilter("SpecimenDetail", "QualityControlFlag", "Equals", "true");
        setSort("SpecimenDetail", "GlobalUniqueId", SortDirection.ASC);
        assertTextPresent("AAA07XK5-02");
        assertTextPresent("Conflicts found: AdditiveTypeId, DerivativeTypeId, PrimaryTypeId");
        clickLinkContainingText("history");
        assertTextPresent("Blood (Whole)");
        assertTextPresent("Vaginal Swab");
        assertTextPresent("Vial is flagged for quality control");
        clickLinkWithText("update");
        setFormElement("qualityControlFlag", "false");
        setFormElement("comments", "Manually removed flag");
        clickNavButton("Save Changes");
        assertTextPresent("Manually removed flag");
        assertTextPresent("Conflicts found: AdditiveTypeId, DerivativeTypeId, PrimaryTypeId");
        assertTextNotPresent("Vial is flagged for quality control");
        clickLinkWithText("return to vial view");
        assertTextNotPresent("AAA07XK5-02");
        assertTextPresent("KBH00S5S-01");
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
        waitForElement(Locator.xpath("//input[@id='DatasetDesignerName']"), WAIT_FOR_GWT);

        // set the demographic data checkbox
        checkCheckbox(Locator.xpath("//input[@name='demographicData']"));

        // add a comment field
        setFormElement(Locator.id("ff_name" + 0),  COMMENT_FIELD_NAME);
        setFormElement(Locator.id("ff_label" + 0), PARTICIPANT_COMMENT_LABEL);
        selectOptionByText("ff_type" + 0, "Multi-Line Text");
        clickNavButton("Save");

        log("creating the participant/visit comment dataset");
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("Create New Dataset");

        setFormElement("typeName", PARTICIPANT_VISIT_CMT_DATASET);
        clickNavButton("Next");
        waitForElement(Locator.xpath("//input[@id='DatasetDesignerName']"), WAIT_FOR_GWT);

        // add a comment field
        setFormElement(Locator.id("ff_name" + 0),  COMMENT_FIELD_NAME);
        setFormElement(Locator.id("ff_label" + 0), PARTICIPANT_VISIT_COMMENT_LABEL);
        selectOptionByText("ff_type" + 0, "Multi-Line Text");
        clickNavButton("Save");

        log("configure comments");
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Comments");
        if (isTextPresent("Comments can only be configured for studies with editable datasets"))
        {
            log("configure editable datasets");
            clickLinkWithText("Manage Study");
            clickLinkWithText("Manage Security");
            selectOptionByText("securityString", "Basic security with editable datasets");
            waitForPageToLoad();
            clickNavButton("Update");

            log("configure comments");
            clickLinkWithText(getStudyLabel());
            clickLinkWithText("Manage Study");
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
        clickLinkWithText("Blood (Whole)");
        clickNavButton("Enable Comments/QC");
        log("manage participant comments directly");
        clickMenuButton("Comments and QC", "Comments:SetParticipant");

        clickNavButton("Insert New");
        setFormElement("quf_participantid", "999320812");
        setFormElement("quf_" + COMMENT_FIELD_NAME, "Participant Comment");
        clickNavButton("Submit");

        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Blood (Whole)");
        setFilter("SpecimenDetail", "ParticipantId", "Equals", "999320812");

        assertTextPresent("Participant Comment");
        clearAllFilters("SpecimenDetail", "ParticipantId");

        log("verify copying and moving vial comments");
        setFilter("SpecimenDetail", "GlobalUniqueId", "Equals", "AAA07XK5-01");
        selenium.click(".toggle");
        clickNavButton("Enable Comments/QC");
        clickMenuButton("Comments and QC", "Comments:Set");
        setFormElement("comments", "Vial Comment");
        clickNavButton("Save Changes");

        selenium.click(".toggle");
        clickMenuButton("Comments and QC", "Comments:Set");
        clickMenuButton("Copy or Move Comment(s)", "CopyPtid:999320812", "Comment:Copy", "Copy:ToParticipant");
        setFormElement("quf_" + COMMENT_FIELD_NAME, "Copied PTID Comment");
        clickNavButton("Submit");
        assertTextPresent("Copied PTID Comment");

        selenium.click(".toggle");
        clickMenuButton("Comments and QC", "Comments:Set");
        clickMenuButton("Copy or Move Comment(s)", "MovePtid:999320812", "Comment:Move", "Move:ToParticipant");
        selenium.getConfirmation();
        setFormElement("quf_" + COMMENT_FIELD_NAME, "Moved PTID Comment");
        clickNavButton("Submit");
        assertTextPresent("Moved PTID Comment");
        assertTextNotPresent("Participant Comment");
        assertTextNotPresent("Vial Comment");
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
        clickLinkWithText("Manage Datasets");

        clickLinkWithText("489");
        assertTextPresent("ESIdt");
        assertTextPresent("Form Completion Date");
        assertTableCellTextEquals("details", 5, 1, "false");     // "Demographics Data" should be false

        // Verify that "Demographics Data" is checked and description is set
        clickLinkWithText("Manage Datasets");
        clickLinkWithText("DEM-1: Demographics");
        assertTableCellTextEquals("details", 5, 1, "true");
        assertTableCellTextEquals("details", 8, 1, DEMOGRAPHICS_DESCRIPTION);

        // "Demographics Data" bit needs to be false for the rest of the test
        setDemographicsBit("DEM-1: Demographics", false);
    }

    private void verifyHiddenVisits()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Study Navigator");
        assertTextNotPresent("Screening Cycle");
        assertTextNotPresent("Cycle 1");
        assertTextPresent("Pre-exist Cond");
        clickLinkWithText("Show Hidden Data");
        assertTextPresent("Screening Cycle");
        assertTextPresent("Cycle 1");
        assertTextPresent("Pre-exist Cond");
    }

    private void verifyCohorts()
    {
        clickLinkWithText(getStudyLabel());
        clickLinkWithText("Study Navigator");
        clickLinkWithText("24");

        // verify that cohorts are working
        assertTextPresent("999320016");
        assertTextPresent("999320518");

        clickMenuButton("Cohorts", "Cohorts:PTID_CURRENT_Group 1");
        waitForPageToLoad();
        assertTextPresent("999320016");
        assertTextNotPresent("999320518");

        clickMenuButton("Cohorts", "Cohorts:PTID_CURRENT_Group 2");
        waitForPageToLoad();
        assertTextNotPresent("999320016");
        assertTextPresent("999320518");

        // verify that the participant view respects the cohort filter:
        setSort("Dataset", "ParticipantId", SortDirection.ASC);
        clickLinkWithText("999320518");
        clickLinkWithText("125: EVC-1: Enrollment Vaccination", false);
        assertTextNotPresent("Group 1");
        assertTextPresent("Group 2");
        clickLinkWithText("Next Participant >");
        assertTextNotPresent("Group 1");
        assertTextPresent("Group 2");
        clickLinkWithText("Next Participant >");
        assertTextNotPresent("Group 1");
        assertTextPresent("Group 2");
        clickLinkWithText("Next Participant >");
    }

    @Override
    protected void doCleanup() throws Exception
    {
        super.doCleanup();

        deleteDir(new File(getLabKeyRoot(), ARCHIVE_TEMP_DIR));
    }
}
