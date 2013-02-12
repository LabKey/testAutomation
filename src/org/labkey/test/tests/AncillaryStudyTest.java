/*
 * Copyright (c) 2011-2013 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.ChartHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.WikiHelper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
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
    private static final String[] DEPENDENT_DATASETS = {"EVC-1: Enrollment Vaccination", "FPX-1: Final Complete Physical Exam", "IV-1:Interim Visit", "TM-1: Termination"};
    private static final String PARTICIPANT_GROUP = "Ancillary Group";
    private static final String[] PTIDS = {"999320016", "999320518", "999320529", "999320533", "999320541", "999320557", "999320565", "999320576", "999320582", "999320590"};
    private static final String PARTICIPANT_GROUP_BAD = "Bad Ancillary Group";
    private static final String[] PTIDS_BAD = {"999320004", "999320007", "999320010", "999320016", "999320018", "999320021", "999320029", "999320033", "999320036","999320038"};
    private static final String SEQ_NUMBER = "1001"; //These should alphabetically precede all exesting sequence numbers.
    private static final String SEQ_NUMBER2 = "1002";
    private static final String UPDATED_DATASET_VAL = "Esperanto";
    private static final String EXTRA_DATASET_ROWS = "mouseId\tsequenceNum\n" + // Rows for APX-1: Abbreviated Physical Exam
                                                     PTIDS[0] + "\t"+SEQ_NUMBER+"\n" +
                                                     PTIDS_BAD[0] + "\t" + SEQ_NUMBER;
    private final File PROTOCOL_DOC = new File( getLabKeyRoot() + getStudySampleDataPath() + "/Protocol.txt");
    private final File PROTOCOL_DOC2 = new File( getLabKeyRoot() + getStudySampleDataPath() + "/Protocol2.txt");

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/study";
    }

    @Override
    public void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
        deleteDir(new File(getPipelinePath(), "export"));
    }

    @Override
    public void doCreateSteps()
    {
        importStudy();
        startSpecimenImport(2);
        waitForPipelineJobsToComplete(2, "study import", false);
        _studyHelper.createCustomParticipantGroup(PROJECT_NAME, getFolderName(), PARTICIPANT_GROUP, "Mouse", true, PTIDS);
        _studyHelper.createCustomParticipantGroup(PROJECT_NAME, getFolderName(), PARTICIPANT_GROUP_BAD, "Mouse", true, PTIDS_BAD);
        createAncillaryStudy();
    }

    private void createAncillaryStudy()
    {
        clickAndWait(Locator.linkWithText(PROJECT_NAME));
        clickFolder(getFolderName());
        clickTab("Manage");

        log("Create Special Emphasis Study.");
        clickButton("Create Ancillary Study", 0);
        
        //Wizard page 1 - location
        _extHelper.waitForExtDialog("Create Ancillary Study");
        clickAt(Locator.xpath("//label/span[text()='Protocol']"), "1,1");
        waitForElement(Locator.xpath("//div["+Locator.NOT_HIDDEN+" and @class='g-tip-header']//span[text()='Protocol Document']"), WAIT_FOR_JAVASCRIPT);
        setFormElement("studyName", getFolderName());
        setFormElement("studyDescription", STUDY_DESCRIPTION);
        Assert.assertTrue(PROTOCOL_DOC.exists());
        setFormElement("protocolDoc", PROTOCOL_DOC);
        clickButton("Change", 0);
        selenium.doubleClick(Locator.xpath(_extHelper.getExtDialogXPath("Create Ancillary Study") + "//span[string() = '"+PROJECT_NAME+"']").toString());
        clickButton("Next", 0);

        //Wizard page 2 - participant group
        Locator groupLocator = Locator.xpath("//span[contains(text(),  '" + PARTICIPANT_GROUP + "')]");
        waitForElement(groupLocator, WAIT_FOR_JAVASCRIPT);
        assertWizardError("Next", "You must select at least one Mouse group.");
        waitAndClick(groupLocator);

        log("Check participant group.");
        Locator.XPathLocator ptidLocator = Locator.xpath("//div[not(contains(@style, 'display: none;'))]/span[contains(@class, 'testParticipantGroups')]");
        waitForElement(ptidLocator, WAIT_FOR_JAVASCRIPT);
        Assert.assertEquals("Did not find expected number of participants", PTIDS.length, getXpathCount(ptidLocator));
        for (String ptid : PTIDS)
        {
            assertElementPresent(Locator.xpath("//div[not(contains(@style, 'display: none;'))]/span[@class='testParticipantGroups' and text() = '" + ptid + "']"));
        }

//        _extHelper.selectExt4GridItem(null, null, 0, "studyWizardParticipantList", false); //WebDriver
        selenium.getEval("selenium.selectExtGridItem(null, null, 0, 'studyWizardParticipantList', false)");

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
        clickButton("Add Selected", 0);
        sleep(1000); // wait for specimen Ids to appear in form.
*/
        clickButton("Next", 0);

        //Wizard page 3 - select datasets
        waitForElement(Locator.xpath("//div[contains(@class, 'studyWizardDatasetList')]"), WAIT_FOR_JAVASCRIPT);
        clickAt(Locator.xpath("//label/span[text()='Data Refresh']"), "1,1");
        waitForElement(Locator.xpath("//div["+Locator.NOT_HIDDEN+" and @class='g-tip-header']//span[text()='Data Refresh']"), WAIT_FOR_JAVASCRIPT);
        for(int i = 0; i < DATASETS.length; i++)
        {
            selenium.getEval("selenium.selectExtGridItem('Label', '"+DATASETS[i]+"', -1, 'studyWizardDatasetList', true)");
        }
        assertWizardError("Finish", "An error occurred trying to create the study: A study already exists in the destination folder.");

        clickButton("Previous", 0);
        clickButton("Previous", 0);
        setFormElement("studyName", STUDY_NAME);
        clickButton("Next", 0);
        clickButton("Next", 0);
        checkRadioButton("autoRefresh", "false");
        clickButton("Finish");

        waitForPipelineJobsToFinish(3);
        clickAndWait(Locator.linkWithText("Create Ancillary Study"));
    }

    @Override
    public void doVerifySteps()
    {
        assertTextPresent("Ancillary study created by AncillaryStudyTest");
        clickTab("Manage");
        assertTextPresent((DATASETS.length + DEPENDENT_DATASETS.length) + " Datasets");
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        for( String str : DATASETS )
        {
            assertLinkPresentWithText(str);
        }

        clickAndWait(Locator.linkWithText("Mice"));
        waitForText(PTIDS[0]);
        for( String str : PTIDS )
        {
            assertLinkPresentWithText(str);
        }
        assertTextPresent("10 mice");

        verifySpecimens(5, 44);
        verifyContainerPathFilter();
        verifyModifyParticipantGroup(STUDY_NAME);
        verifyModifyParticipantGroup(getFolderName());
        verifyModifyDataset();
        verifyProtocolDocument();
        verifyDatasets();
        verifySpecimens(4, 38); // Lose one specimen and associated vials due to mouse group modification
        verifyExportImport();
    }

    private void verifyModifyParticipantGroup(String study)
    {
        clickAndWait(Locator.linkWithText(study));
        log("Modify " + study + " participant group.");
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Mouse Groups"));
        waitForText(PARTICIPANT_GROUP);
        String csp = PTIDS[0];
        for( int i = 1; i < PTIDS.length - 1; i++ )
            csp += ","+PTIDS[i];
        _studyHelper.editCustomParticipantGroup(PARTICIPANT_GROUP, "Mouse", null, null, true, true, true, csp);

        log("Verify that modified participant group has no effect on ancillary study.");
        clickAndWait(Locator.linkWithText(STUDY_NAME));
        clickAndWait(Locator.linkWithText("Mice"));
        waitForText("Filter"); // Wait for participant list to appear.

        for( String str : PTIDS )
        {
            waitForElement(Locator.linkWithText(str), WAIT_FOR_JAVASCRIPT);
        }
    }

    private void verifyModifyDataset()
    {
        //INSERT
        log("Insert rows into source dataset");
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(DATASETS[0]));
        clickButton("Import Data");
        setFormElement(Locator.name("text"), EXTRA_DATASET_ROWS);
        clickButton("Submit", 0);
        waitAndClickButton("OK");

        log("Verify changes in Ancillary Study. (insert)");
        clickAndWait(Locator.linkWithText(STUDY_NAME));
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText(DATASETS[0]));
        clickButton("View Data");
        clickMenuButton("Views", "Edit Snapshot");
        clickButton("Update Snapshot", 0);
        assertConfirmation("Updating will replace all existing data with a new set of data. Continue?");
        waitForPageToLoad();
        DataRegionTable table = new DataRegionTable("Dataset", this, true, true);
        Assert.assertEquals("Dataset does not reflect changes in source study.", 21, table.getDataRowCount());
        assertTextPresent(SEQ_NUMBER + ".0");
        table.getColumnDataAsText("Sequence Num");

        //UPDATE
        log("Modify row in source dataset");
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(DATASETS[0]));
        Map nameAndValue = new HashMap(1);
        nameAndValue.put("quf_SequenceNum", SEQ_NUMBER2);
        (new ChartHelper(this)).editDrtRow(1, nameAndValue);

        log("Verify changes in Ancillary Study. (modify)");
        clickAndWait(Locator.linkWithText(STUDY_NAME));
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText(DATASETS[0]));
        clickButton("View Data");
        clickMenuButton("Views", "Edit Snapshot");
        clickButton("Update Snapshot", 0);
        assertConfirmation("Updating will replace all existing data with a new set of data. Continue?");
        waitForPageToLoad();
        table = new DataRegionTable("Dataset", this, true, true);
        Assert.assertEquals("Dataset does not reflect changes in source study.", 21, table.getDataRowCount());
        assertTextPresent(SEQ_NUMBER2 + ".0");
        assertTextNotPresent(SEQ_NUMBER + ".0");

        //DELETE
        log("Delete row from source dataset");
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(DATASETS[0]));
        checkCheckbox(".select", 1);
        clickButton("Delete");
        assertConfirmation("Delete selected row from this dataset?");

        log("Verify changes in Ancillary Study. (delete)");
        clickAndWait(Locator.linkWithText(STUDY_NAME));
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Datasets"));
        clickAndWait(Locator.linkWithText(DATASETS[0]));
        clickButton("View Data");
        clickMenuButton("Views", "Edit Snapshot");
        clickButton("Update Snapshot", 0);
        assertConfirmation("Updating will replace all existing data with a new set of data. Continue?");
        waitForPageToLoad();
        table = new DataRegionTable("Dataset", this, true, true);
        Assert.assertEquals("Dataset does not reflect changes in source study.", 20, table.getDataRowCount());
        assertTextNotPresent(SEQ_NUMBER + ".0", SEQ_NUMBER2 + ".0");
    }


    private void verifyProtocolDocument()
    {
        clickAndWait(Locator.linkWithText(STUDY_NAME));
        assertTextPresent(STUDY_DESCRIPTION);
        assertElementPresent(Locator.xpath("//a[contains(@href, 'name=" + PROTOCOL_DOC.getName() + "')]"));
        clickAndWait(Locator.xpath("//a[./img[@title='Edit']]"));

        waitForElement(Locator.name("Label"), WAIT_FOR_JAVASCRIPT);
        setFormElement("Label", "Extra " + STUDY_NAME);
        setFormElement("Description", "Extra " + STUDY_DESCRIPTION);
        click(Locator.linkWithText("Attach a file"));
        waitForElement(Locator.xpath("//div[contains(@class, 'protocolPanel')]//input[@type='file']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//div[contains(@class, 'protocolPanel')]//input[@type='file']"), PROTOCOL_DOC2.toString());
        clickButton("Submit");
        assertLinkPresentWithText(PROTOCOL_DOC.getName());
        assertLinkPresentWithText(PROTOCOL_DOC2.getName());
        assertTextPresent("Protocol documents:");
        assertTextPresent("Extra " + STUDY_NAME);
        assertTextPresent("Extra " + STUDY_DESCRIPTION);
    }

    private void verifyDatasets()
    {
        log("Verify Linked Datasets");
        clickFolder(getFolderName());
        clickAndWait(Locator.linkWithText(DEPENDENT_DATASETS[0]));
        clickAndWait(Locator.linkWithText("edit"));
        setFormElement(Locator.name("quf_formlang"), UPDATED_DATASET_VAL);
        clickButton("Submit");

        clickAndWait(Locator.linkWithText(STUDY_NAME));
        clickAndWait(Locator.linkWithText("Clinical and Assay Data"));
        for(String dataset : DATASETS)
        {
            waitForText(dataset);
        }
        for(String dataset : DEPENDENT_DATASETS)
        {
            assertLinkPresentWithText(dataset);
        }
        clickAndWait(Locator.linkWithText(DEPENDENT_DATASETS[0]));
        assertTextNotPresent(UPDATED_DATASET_VAL);
        clickMenuButton("Views", "Edit Snapshot");
        clickButton("Update Snapshot");
        assertConfirmation("Updating will replace all existing data with a new set of data. Continue?");
        assertTextPresent(UPDATED_DATASET_VAL);
    }

    private void verifySpecimens(int specimenCount, int vialCount)
    {
        log("Verify copied specimens");
        clickAndWait(Locator.linkWithText(STUDY_NAME));
        clickAndWait(Locator.linkWithText("Specimen Data"));
        clickAndWait(Locator.linkWithText("By Vial Group"));
        DataRegionTable table = new DataRegionTable("SpecimenSummary", this, false, true);
        Assert.assertEquals("Did not find expected number of specimens.", specimenCount, table.getDataRowCount() - 1); // n specimens + 1 total row
        Assert.assertEquals("Incorrect total vial count.", String.valueOf(vialCount), table.getDataAsText(specimenCount, "Vial Count"));
        clickAndWait(Locator.linkWithText("Specimen Data"));
        clickAndWait(Locator.linkWithText("By Individual Vial"));
        table = new DataRegionTable("SpecimenDetail", this, false, true);
        Assert.assertEquals("Did not find expected number of vials.", vialCount, table.getDataRowCount() - 1); // m vials + 1 total row

        log("Verify that Ancillary study doesn't support requests.");
        clickAndWait(Locator.linkWithText("Manage"));
        assertTextNotPresent("Specimen Repository Settings");
        assertTextNotPresent("Specimen Request Settings");
        assertTextPresent("NOTE: specimen repository and request settings are not available for ancillary or published studies.");
        assertElementNotPresent(Locator.linkWithText("Change Repository Type"));
        assertElementNotPresent(Locator.linkWithText("Manage Display and Behavior"));
        assertElementNotPresent(Locator.linkWithText("Manage Request Statuses"));
        assertElementNotPresent(Locator.linkWithText("Manage Actors and Groups"));
        assertElementNotPresent(Locator.linkWithText("Manage Default Requirements"));
        assertElementNotPresent(Locator.linkWithText("Manage New Request Form"));
        assertElementNotPresent(Locator.linkWithText("Manage Notifications"));
        assertElementNotPresent(Locator.linkWithText("Manage Requestability Rules"));
    }

    /**
     * Regression test for #17021. Requires Ancillary study.
     */
    public void verifyContainerPathFilter()
    {
        clickFolder(getFolderName());
        clickTab("Mice");
        addWebPart("Wiki");
        WikiHelper wh = new WikiHelper(this);
        wh.createWikiPage("17021", "17021 Regression", new File(getApiScriptFolder(), "filterTest.html"));
        setUpFacetedFilter("test17021", "PrimaryType", "Blood (Whole)");
        assertElementNotPresent(Locator.linkWithText("Semen"));
        clickButton("CANCEL",0);
    }

    private void verifyExportImport()
    {
        _studyHelper.exportStudy(STUDY_NAME);
        goToModule("Pipeline");
        clickButton("Process and Import Data");

        _extHelper.selectFileBrowserItem("export/study/participant_groups.xml");
        log("Verify protocol document in export");
        _extHelper.selectFileBrowserItem("export/study/protocolDocs/" + PROTOCOL_DOC.getName());
        assertTextPresent(PROTOCOL_DOC2.getName());

        _extHelper.selectFileBrowserItem("export/study/datasets/datasets_metadata.xml");
        assertTextPresent(".tsv", (DATASETS.length + DEPENDENT_DATASETS.length) * 3);
        assertTextPresent("dataset001.tsv", "dataset019.tsv", "dataset023.tsv", "dataset125.tsv",
                "dataset136.tsv", "dataset144.tsv", "dataset171.tsv", "dataset172.tsv", "dataset200.tsv",
                "dataset300.tsv", "dataset350.tsv", "dataset420.tsv", "dataset423.tsv", "dataset490.tsv");

        log("Verify reloading study");
        _extHelper.selectFileBrowserItem("export/study/study.xml");
        selectImportDataAction("Reload Study");
        waitForPipelineJobsToComplete(1, "study import", false);
    }

    private void assertWizardError(String button, String error)
    {
        clickButton(button, 0);
        _extHelper.waitForExtDialog("Error");
        assertTextPresent(error);
        clickButton("OK", 0);
        _extHelper.waitForExtDialogToDisappear("Error");
    }

    @Override
    public boolean isFileUploadTest()
    {
        return true;
    }

    public String getProjectName()
    {
        return PROJECT_NAME;
    }
}
