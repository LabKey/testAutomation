/*
 * Copyright (c) 2011 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.StudyHelper;

/**
 * Created by IntelliJ IDEA.
 * User: Treygdor
 * Date: Sep 8, 2011
 * Time: 1:55:05 PM
 */
public class AncillaryStudyTest extends StudyBaseTest
{
    private static final String PROJECT_NAME = "AncillaryStudyTest Project";
    private static final String STUDY_NAME = "Special Emphasis Study";
    private static final String STUDY_DESCRIPTION = "Ancillary study created by AncillaryStudyTest.";
    private static final String[] DATASETS = {"APX-1: Abbreviated Physical Exam","AE-1:(VTN) AE Log","BRA-1: Behavioral Risk Assessment (Page 1)","BRA-2: Behavioral Risk Assessment (Page 2)","CM-1:(Ph I/II) Concomitant Medications Log","CPF-1: Follow-up Chemistry Panel","CPS-1: Screening Chemistry Panel","DEM-1: Demographics","DOV-1: Discontinuation of Vaccination","ECI-1: Eligibility Criteria"};
    private static final String PARTICIPANT_GROUP = "Ancillary Group";
    private static final String[] PTIDS = {"999320016", "999320518", "999320529", "999320533", "999320541", "999320557", "999320565", "999320576", "999320582", "999320590"};
    private static final String PARTICIPANT_GROUP_BAD = "Bad Ancillary Group";
    private static final String[] PTIDS_BAD = {"999320004", "999320007", "999320010", "999320016", "999320018", "999320021", "999320029", "999320033", "999320036","999320038"};
    private static final String SEQ_NUMBER = "1001"; //These should alphabetically precede all exesting sequence numbers.
    private static final String SEQ_NUMBER2 = "1002";
    private static final String EXTRA_DATASET_ROWS = "mouseId\tsequenceNum\n" + // Rows for APX-1: Abbreviated Physical Exam
                                                     PTIDS[0] + "\t"+SEQ_NUMBER+"\n" +
                                                     PTIDS_BAD[0] + "\t" + SEQ_NUMBER;

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/...";
    }

    @Override
    public void doCleanup()
    {
        // Delete any containers and users created by the test.
        try
        {
            deleteProject(PROJECT_NAME);
        }
        catch (Exception e)
        {
        }
    }

    @Override
    public void doCreateSteps()
    {
        importStudy();
        startSpecimenImport(2);
        waitForPipelineJobsToComplete(2, "study import", false);
        StudyHelper.createCustomParticipantGroup(this, PROJECT_NAME, getFolderName(), PARTICIPANT_GROUP, "Mouse", PTIDS);
        StudyHelper.createCustomParticipantGroup(this, PROJECT_NAME, getFolderName(), PARTICIPANT_GROUP_BAD, "Mouse", PTIDS_BAD);
        createAncillaryStudy();
    }

    private void createAncillaryStudy()
    {
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(getFolderName());
        clickLinkWithText("Manage Study");

        log("Create Special Emphasis Study.");
        clickNavButton("Create New Study", 0);
        
        //Wizard page 1 - location
        ExtHelper.waitForExtDialog(this, "Create New Study");
        setFormElement("studyName", getFolderName());
        setFormElement("studyDescription", STUDY_DESCRIPTION);
        selectStudyLocation();
        clickNavButton("Next", 0);

        //Wizard page 2 - participant group
        waitForElement(Locator.radioButtonByName("renderType"), WAIT_FOR_JAVASCRIPT);
        assertWizardError("Next", "You must select an existing group or create a new one.");
        waitAndClick(Locator.xpath("//div[contains(@class, 'testParticipantGroups')]//em[text()='Ancillary Group']"));

        log("Check participant group.");
        assertEquals("Did not find expected number of participants", PTIDS.length, getXpathCount(Locator.xpath("//div[contains(@class, 'testParticipantGroups')]//em[text() = '"+PTIDS[0]+"']/../../../dl")));
        for (String ptid : PTIDS)
        {
            assertElementPresent(Locator.xpath("//div[contains(@class, 'testParticipantGroups')]//em[text() = '"+ptid+"']"));
        }

        // kbl: commented out current wizard only allows existing participant groups or all participants (although this could change)
/*
        checkRadioButton("renderType", "new");
        waitForElement(Locator.xpath("//table[@id='dataregion_demoDataRegion']"), WAIT_FOR_JAVASCRIPT);
        assertWizardError("Next", "Mouse Category Label required.");
        setFormElement("categoryLabel", PARTICIPANT_GROUP);
        assertWizardError("Next", "One or more Mouse Identifiers required.");
        waitForElement(Locator.name(".toggle"), WAIT_FOR_JAVASCRIPT);
        sleep(100); // wait for specimen grid to be ready.
        checkAllOnPage("demoDataRegion");
        uncheckDataRegionCheckbox("demoDataRegion", 0);
        uncheckDataRegionCheckbox("demoDataRegion", 1);
        clickNavButton("Add Selected", 0);
        sleep(1000); // wait for specimen Ids to appear in form.
*/
        clickNavButton("Next", 0);

        //Wizard page 3 - select datasets
        waitForElement(Locator.xpath("//div[contains(@class, 'studyWizardDatasetList')]"), WAIT_FOR_JAVASCRIPT);
        assertWizardError("Finish", "You must select at least one dataset to create the new study from.");
        for(int i = 0; i < DATASETS.length; i++)
        {
            selenium.getEval("selenium.selectExtGridItem('Label', '"+DATASETS[i]+"', null, 'studyWizardDatasetList', true)");
        }
        assertWizardError("Finish", "An error occurred trying to create the study: A study already exists in the destination folder.");

        clickNavButton("Previous", 0);
        clickNavButton("Previous", 0);
        setFormElement("studyName", STUDY_NAME);
        selectStudyLocation();
        clickNavButton("Next", 0);
        clickNavButton("Next", 0);
        checkRadioButton("autoRefresh", "false");
        clickNavButton("Finish");
    }

    private void selectStudyLocation()
    {
        clickNavButton("Change", 0);
        ExtHelper.waitForExtDialog(this, "Choose Study Location");
        click(Locator.xpath(ExtHelper.getExtDialogXPath("Choose Study Location") + "//span[string() = '"+PROJECT_NAME+"']"));
        ExtHelper.clickExtButton(this, "Choose Study Location", "Select", 0);
        waitForElementToDisappear(Locator.xpath(ExtHelper.getExtDialogXPath("Choose Study Location")), WAIT_FOR_JAVASCRIPT);
    }

    @Override
    public void doVerifySteps()
    {
        assertTextPresent("10 Datasets");
        clickLinkWithText("Manage Datasets");
        for( String str : DATASETS )
        {
            assertLinkPresentWithText(str);
        }

        clickLinkWithText("Shortcuts");
        waitForText(PTIDS[0]);
        for( String str : PTIDS )
        {
            assertLinkPresentWithText(str);
        }
        assertTextPresent("10 mice");

        verifyModifyParticipantGroup(STUDY_NAME);
        verifyModifyParticipantGroup(getFolderName());
        verifyModifyDataset();
    }

    private void verifyModifyParticipantGroup(String study)
    {
        clickLinkWithText(study);
        log("Modify " + study + " participant group.");
        clickLinkWithText("Manage Study");
        clickLinkWithText("Manage Mouse Groups");
        waitForText(PARTICIPANT_GROUP);
        selenium.getEval("selenium.selectExtGridItem('label', '"+PARTICIPANT_GROUP+"', null, 'participantCategoriesGrid', null, false)");
        click(Locator.xpath("//*[text()='"+PARTICIPANT_GROUP+"']"));
        clickNavButton("Edit Selected", 0);
        ExtHelper.waitForExtDialog(this, "Define Mouse Group");
        waitForElement(Locator.id("dataregion_demoDataRegion"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        String csp = PTIDS[0];
        for( int i = 1; i < PTIDS.length - 1; i++ )
            csp += ","+PTIDS[i];
        setFormElement("categoryIdentifiers", csp);
        ExtHelper.clickExtButton(this, "Define Mouse Group", "Save", 0);
        waitForExtMaskToDisappear();

        log("Verify that modified participant group has no effect on ancillary study.");
        clickLinkWithText(STUDY_NAME);
        clickLinkWithText("Shortcuts");
        waitForText("Filter:"); // Wait for participant list to appear.

        for( String str : PTIDS )
        {
            waitForElement(Locator.linkWithText(str), WAIT_FOR_JAVASCRIPT);
        }
    }

    private void verifyModifyDataset()
    {
        //INSERT
        log("Insert rows into source dataset");
        clickLinkWithText(getFolderName());
        clickLinkWithText(DATASETS[0]);
        clickNavButton("Import Data");
        setFormElement(Locator.name("text"), EXTRA_DATASET_ROWS);
        clickNavButton("Submit", 0);
        waitAndClickNavButton("OK");

        log("Verify changes in Ancillary Study. (insert)");
        clickLinkWithText(STUDY_NAME);
        clickLinkWithText("Manage Datasets");
        clickLinkWithText(DATASETS[0]);
        clickNavButton("View Data");
        clickMenuButton("Views", "Edit Snapshot");
        clickNavButton("Update Snapshot", 0);
        assertConfirmation("Updating will replace all existing data with a new set of data. Continue?");
        waitForPageToLoad();
        DataRegionTable table = new DataRegionTable("Dataset", this, true, true);
        assertEquals("Dataset does not reflect changes in source study.", 21, table.getDataRowCount());
        assertTextPresent(SEQ_NUMBER + ".0");
        table.getColumnDataAsText("Sequence Num");

        //UPDATE
        log("Modify row in source dataset");
        clickLinkWithText(getFolderName());
        clickLinkWithText(DATASETS[0]);
        clickLinkWithText("edit", 1);
        setFormElement(Locator.name("quf_SequenceNum"), SEQ_NUMBER2);
        clickNavButton("Submit");

        log("Verify changes in Ancillary Study. (modify)");
        clickLinkWithText(STUDY_NAME);
        clickLinkWithText("Manage Datasets");
        clickLinkWithText(DATASETS[0]);
        clickNavButton("View Data");
        clickMenuButton("Views", "Edit Snapshot");
        clickNavButton("Update Snapshot", 0);
        assertConfirmation("Updating will replace all existing data with a new set of data. Continue?");
        waitForPageToLoad();
        table = new DataRegionTable("Dataset", this, true, true);
        assertEquals("Dataset does not reflect changes in source study.", 21, table.getDataRowCount());
        assertTextPresent(SEQ_NUMBER2 + ".0");
        assertTextNotPresent(SEQ_NUMBER + ".0");

        //DELETE
        log("Delete row from source dataset");
        clickLinkWithText(getFolderName());
        clickLinkWithText(DATASETS[0]);
        checkCheckbox(".select", 1);
        clickNavButton("Delete");
        assertConfirmation("Delete selected row from this dataset?");

        log("Verify changes in Ancillary Study. (delete)");
        clickLinkWithText(STUDY_NAME);
        clickLinkWithText("Manage Datasets");
        clickLinkWithText(DATASETS[0]);
        clickNavButton("View Data");
        clickMenuButton("Views", "Edit Snapshot");
        clickNavButton("Update Snapshot", 0);
        assertConfirmation("Updating will replace all existing data with a new set of data. Continue?");
        waitForPageToLoad();
        table = new DataRegionTable("Dataset", this, true, true);
        assertEquals("Dataset does not reflect changes in source study.", 20, table.getDataRowCount());
        assertTextNotPresent(SEQ_NUMBER + ".0", SEQ_NUMBER2 + ".0");
    }
        
    private void assertWizardError(String button, String error)
    {
        clickNavButton(button, 0);
        waitForText(error);
        clickNavButton("OK", 0);
        waitForTextToDisappear(error);
    }

    public String getProjectName()
    {
        return PROJECT_NAME;
    }
}
