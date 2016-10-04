/*
 * Copyright (c) 2012-2015 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.components.studydesigner.AssayScheduleWebpart;
import org.labkey.test.components.studydesigner.BaseManageVaccineDesignVisitPage;
import org.labkey.test.components.studydesigner.ManageAssaySchedulePage;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.AbstractContainerHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.UIAssayHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@Category({DailyA.class})
public class SpecimenProgressReportTest extends BaseWebDriverTest
{
    public static final String STUDY_PIPELINE_ROOT = TestFileUtils.getLabKeyRoot() + "/sampledata/specimenprogressreport";
    public AbstractContainerHelper _containerHelper = new APIContainerHelper(this);
    public PortalHelper _portalHelper = new PortalHelper(this);
    private static final String studyFolder = "study folder";
    private static final String assayFolder = "assay folder";
    int pipelineCount = 0;
    private static final String assay1 = "PCR";
    private static final String assay1File = "PCR Data.tsv";
    private static final String assay1XarPath = "/assays/PCR.xar";
    private static final String assay2 = "RNA";
    private static final String assay2File = "RNA Data.tsv";
    private static final String assay2File2 = "RNA Data 2.tsv";
    private static final String assay2XarPath = "/assays/RNA.xar";
    private static final String PROGRESS_REPORT_TABLE_ID = "ProgressReport";

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        //Issue 16247: tricky characters in project name cause alert when trying to add a lookup to a rho query in the folder
        return "Specimen Progress Report Test" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        this._containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Test
    public void testSteps() throws Exception
    {
        _assayHelper = new UIAssayHelper(this);
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), studyFolder, "Study");
        importFolderFromZip(TestFileUtils.getSampleData("specimenprogressreport/Study.folder.zip"));

        // set the label for the unscheduled visit
        goToManageStudy();
        clickAndWait(Locator.linkWithText("Manage Visits"));
        clickAndWait(Locator.xpath("//td[contains(text(),'999.0-999.9999')]/../td/a[contains(text(), 'edit')]"));
        setFormElement(Locator.name("label"), "SR");
        clickAndWait(Locator.linkWithText("Save"));

        // study folder specimen configuration
        manageSpecimenConfiguration();

        createAssayFolder();
        waitForElement(Locator.linkWithText("Specimen Report Study Folder Study specimens"));
        assertTextPresent("38 collections have occurred.",  "48 results from " + assay1 + " have been uploaded", "48 " + assay1 + " queries");
        assertTextNotPresent("Configuration error:",
                "You must first configure the assay(s) that you want to run reports from. Click on the customize menu for this web part and select the Assays that should be included in this report.");

        // verify setting an assay result as flagged for review (i.e. invalid)
        verifyAssayResultInvalid(assay1, assay1File);

        // verify setting the PCR additional grouping column
        verifyAdditionalGroupingColumn(assay1, "gene");

        // verify unscheduled visit ordering for the RNA assay
        verifyUnscheduledVisitDisplay(assay2);

        // verify RNA assay with ignored sampleminded data
        clickFolder(assayFolder);
        waitForElement(Locator.id(PROGRESS_REPORT_TABLE_ID));
        ignoreSampleMindedData(assay2);

        // verify upload with match on Specimen ID
        verifySpecimenIdDataUpload(assay2);

        verifyUnassayedSpecimenQuery(assay2);
    }

    @LogMethod
    private void manageSpecimenConfiguration()
    {
        clickFolder(studyFolder);
        _portalHelper.addWebPart("Assay Schedule");
        _portalHelper.addQueryWebPart("rho");
        _portalHelper.addQueryWebPart("study");

        // setup the visits and a map to their RowIds
        List<String> visitLabels = Arrays.asList("PT1", "0", "3", "5", "6", "8", "10", "11", "12", "13", "14", "15", "16", "17", "18", "20", "SR");
        Map<String, Integer> visitRowIdMap = new HashMap<>();
        for (String visitLabel : visitLabels)
            visitRowIdMap.put(visitLabel, getVisitRowId(visitLabel));

        // add the specimen configurations to the manage page and set the visits
        goToModule("rho");
        addSpecimenConfiguration("PCR", "R", "Main", "CEF-R Cryovial", 0);
        setSpecimenConfigurationVisits(visitRowIdMap, Arrays.asList("3", "5", "6", "8", "10", "11", "12", "13", "14", "15", "16", "17", "18", "20", "SR"), true, 0);
        addSpecimenConfiguration("PCR", "R", "Main", "UPR Micro Tube", 1);
        setSpecimenConfigurationVisits(visitRowIdMap, Arrays.asList("3", "5", "6", "8", "10", "11", "12", "13", "14", "15", "16", "17", "18", "20", "SR"), false, 1);
        addSpecimenConfiguration("RNA", "R", "Main", "TGE Cryovial", 2);
        setSpecimenConfigurationVisits(visitRowIdMap, Arrays.asList("PT1", "0", "6", "20", "SR"), false, 2);

        // set the assay plan and save
        ManageAssaySchedulePage managePage = new ManageAssaySchedulePage(this);
        String assayPlanTxt = "My assay plan " + TRICKY_CHARACTERS_FOR_PROJECT_NAMES + INJECT_CHARS_1 + INJECT_CHARS_2;
        managePage.setAssayPlan(assayPlanTxt);
        managePage.save();

        // verify display of assay schedule webpart
        AssayScheduleWebpart assayScheduleWebpart = new AssayScheduleWebpart(getDriver());
        assertEquals("Unexpected assay plan text", assayPlanTxt, assayScheduleWebpart.getAssayPlan());
        assertTextPresent("\u2713", 35);

        checkRhoQueryRowCount("AssaySpecimenVisit", 35);
        checkRhoQueryRowCount("AssaySpecimenMap", 35);
        checkRhoQueryRowCount("MissingSpecimen", 2);
        checkRhoQueryRowCount("MissingVisit", 3);
    }

    private Integer getVisitRowId(String visitLabel)
    {
        SelectRowsCommand command = new SelectRowsCommand("study", "Visit");
        if (StringUtils.isNumeric(visitLabel))
            command.setFilters(Arrays.asList(new Filter("SequenceNumMin", visitLabel)));
        else
            command.setFilters(Arrays.asList(new Filter("Label", visitLabel)));

        SelectRowsResponse response;
        try
        {
            response = command.execute(createDefaultConnection(true), getProjectName() + "/" + studyFolder);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }

        List<Map<String, Object>> rows = response.getRows();
        if (rows.size() == 1)
            return Integer.parseInt(rows.get(0).get("RowId").toString());

        return null;
    }

    private void checkRhoQueryRowCount(String name, int expectedCount)
    {
        clickFolder(studyFolder);
        clickAndWait(Locator.linkWithText(name));
        DataRegionTable drt = new DataRegionTable("query", this);
        assertEquals("Unexpected number of rows in the query", expectedCount, drt.getDataRowCount());
    }

    private void addSpecimenConfiguration(String assayName, String source, String location, String tubeType, int newRowIndex)
    {
        ManageAssaySchedulePage managePage = new ManageAssaySchedulePage(this);
        assertEquals("Unexpected assay schedule rows", newRowIndex, managePage.getAssayRowCount());

        managePage.addNewAssayRow(assayName, assayName + " " + tubeType, newRowIndex);
        managePage.setTextFieldValue("Source", source, newRowIndex);
        managePage.setComboFieldValue("LocationId", location, newRowIndex);
        managePage.setTextFieldValue("TubeType", tubeType, newRowIndex);
    }

    private void setSpecimenConfigurationVisits(Map<String, Integer> visitRowIdMap, List<String> visitLabels, boolean addAllVisits, int rowIndex)
    {
        ManageAssaySchedulePage managePage = new ManageAssaySchedulePage(this);

        if (addAllVisits)
            managePage.addAllExistingVisitColumns();

        for (String visitLabel : visitLabels)
        {
            BaseManageVaccineDesignVisitPage.Visit visit = new BaseManageVaccineDesignVisitPage.Visit(visitLabel);
            visit.setRowId(visitRowIdMap.get(visitLabel));
            managePage.selectVisit(visit, rowIndex);
        }
    }

    @LogMethod
    private void verifyAssayResultInvalid(String assayName, String runName)
    {
        clickFolder(assayFolder);
        waitForElement(Locator.id(PROGRESS_REPORT_TABLE_ID));
        assertEquals(0, getElementCount( Locator.xpath("//td[contains(@class, 'available')]")));
        assertEquals(24, getElementCount( Locator.xpath("//td[contains(@class, 'query')]")));
        assertEquals(2, getElementCount( Locator.xpath("//td[contains(@class, 'collected')]")));
        assertEquals(0, getElementCount(Locator.xpath("//td[contains(@class, 'invalid')]")));
        assertEquals(54, getElementCount(Locator.xpath("//td[contains(@class, 'expected')]")));
        assertEquals(3, getElementCount(Locator.xpath("//td[contains(@class, 'missing')]")));

        flagSpecimenForReview(assayName, null);

        waitForElement(Locator.id(PROGRESS_REPORT_TABLE_ID));
        assertEquals(1, getElementCount(Locator.xpath("//td[contains(@class, 'invalid')]")));

        // verify legend text and ordering
        assertTextPresentInThisOrder("specimen expected", "specimen received by lab", "specimen not collected",
                "specimen collected", "specimen received but invalid", "assay results available"); // "query" appears too many times on page!
    }

    @LogMethod
    private void verifyAdditionalGroupingColumn(String assayName, String groupCol)
    {
        clickFolder(assayFolder);
        waitForElement(Locator.id(PROGRESS_REPORT_TABLE_ID));
        waitForText("48 " + assayName + " queries");
        configureGroupingColumn(assayName, groupCol);
        waitForElement(Locator.id(PROGRESS_REPORT_TABLE_ID));
        waitAndClickAndWait(Locator.linkWithText("48 results from " + assayName + " have been uploaded."));
        assertTextPresent("Result reported with no corresponding specimen collected", 8);
        assertTextPresent("2 results found for this participant and visit combination", 4);
        assertTextPresent("This specimen type is not expected for this visit", 20);
    }

    @LogMethod
    private void verifyUnscheduledVisitDisplay(String assayName)
    {
        clickFolder(assayFolder);
        waitForElement(Locator.id(PROGRESS_REPORT_TABLE_ID));
        configureAssayProgressDashboard(assay2, "3");
        configureAssaySchema(assayName);

        flagSpecimenForReview(assayName, "2011-03-02");

        clickFolder(assayFolder);
        verifyProgressReport(assayName, false, true, false);

        clickAndWait(Locator.linkWithText("8 results from " + assayName + " have been uploaded."));
        assertTextPresent("Result reported with no corresponding specimen collected", 2);
        assertTextPresent("This specimen type is not expected for this visit", 1);
    }

    private void verifyProgressReport(String assayName, boolean ignoreSampleminded, boolean hasInvalid, boolean bySpecimenId)
    {
        int ignored = ignoreSampleminded ? 2 : 0;
        int invalid = hasInvalid ? 1 : 0;
        int query = bySpecimenId ? 0 : 3;
        int expected = bySpecimenId ? 0 : 2;

        waitForElement(Locator.id(PROGRESS_REPORT_TABLE_ID));
        _ext4Helper.selectRadioButtonById(assayName + "-boxLabelEl");
        waitForElement(Locator.id(PROGRESS_REPORT_TABLE_ID));
        assertTextPresentInThisOrder("SR1", "SR2");
        assertEquals(5 - invalid, getElementCount(Locator.xpath("//td[contains(@class, 'available')]")));
        assertEquals(query, getElementCount(Locator.xpath("//td[contains(@class, 'query')]")));
        assertEquals(4 + ignored, getElementCount(Locator.xpath("//td[contains(@class, 'collected')]")));
        assertEquals(2 - ignored, getElementCount(Locator.xpath("//td[contains(@class, 'received')]")));
        assertEquals(invalid, getElementCount(Locator.xpath("//td[contains(@class, 'invalid')]")));
        assertEquals(12 - expected, getElementCount(Locator.xpath("//td[contains(@class, 'expected')]")));
        assertEquals(3, getElementCount(Locator.xpath("//td[contains(@class, 'missing')]")));
    }

    private void verifySpecimenIdDataUpload(String assayName) throws CommandException, IOException
    {
        clickAndWait(Locator.linkWithText(assayName));
        new DataRegionTable("Runs", this).checkCheckbox(0);
        clickButton("Re-import run");
        clickButton("Next");
        checkRadioButton(Locator.radioButtonById("Fileupload"));
        setFormElement(Locator.name("__primaryFile__"), new File(STUDY_PIPELINE_ROOT + "/assays/" + assay2File2));
        clickButton("Save and Finish");

        clickFolder(assayFolder);
        verifyProgressReport(assayName, true, false, true);
    }

    private void verifyUnassayedSpecimenQuery(String assayName)
    {
        goToSchemaBrowser();
        selectQuery("rho", assayName + " Base Unassayed Specimens");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        DataRegionTable drt = new DataRegionTable("query", this);
        assertEquals(6, drt.getDataRowCount());
        assertTextPresent("specimen collected", 4);
        assertTextPresent("specimen received by lab", 2);
    }

    @LogMethod
    private void flagSpecimenForReview(String assayName, @Nullable String collectionDateFilterStr)
    {
        clickFolder(assayFolder);
        waitForElement(Locator.id(PROGRESS_REPORT_TABLE_ID));
        clickAndWait(Locator.linkWithText(assayName));
        clickAndWait(Locator.linkWithText("view results"));

        if (collectionDateFilterStr != null)
        {
            DataRegionTable drt = new DataRegionTable("Data", this);
            drt.setFilter("Date", "Equals", collectionDateFilterStr);
        }

        click(Locator.tagWithAttribute("img", "title", "Flag for review"));
        clickButton("OK", 0);
        waitForElement(Locator.tagWithAttribute("img", "title", "Flagged for review"));

        clickFolder(assayFolder);
        waitForElement(Locator.id(PROGRESS_REPORT_TABLE_ID));
    }

    @LogMethod
    private void createAssayFolder() throws CommandException, IOException
    {
        goToProjectHome();
        _containerHelper.createSubfolder(getProjectName(), assayFolder, "Assay");
        _containerHelper.enableModule("rho");

        _assayHelper.uploadXarFileAsAssayDesign(new File(STUDY_PIPELINE_ROOT + assay1XarPath), ++pipelineCount);
        _assayHelper.importAssay(assay1, new File(STUDY_PIPELINE_ROOT + "/assays/" + assay1File),  getProjectName() + "/" + assayFolder, Collections.singletonMap("ParticipantVisitResolver", "SampleInfo") );
        clickFolder(assayFolder);
        _assayHelper.uploadXarFileAsAssayDesign(new File(STUDY_PIPELINE_ROOT + assay2XarPath), ++pipelineCount);
        _assayHelper.importAssay(assay2, new File(STUDY_PIPELINE_ROOT + "/assays/" + assay2File),  getProjectName() + "/" + assayFolder, Collections.singletonMap("ParticipantVisitResolver", "SampleInfo") );


        clickFolder(assayFolder);
        _portalHelper.addWebPart("Assay Progress Dashboard");
        _portalHelper.addWebPart("Assay Progress Report");
        assertTextPresent("You must first configure the assay(s) that you want to run reports from. Click on the customize menu for this web part and select the Assays that should be included in this report", 2);

        configureAssayProgressDashboard(assay1, null);
        configureAssaySchema(assay1);
        clickFolder(assayFolder);
        waitForElement(Locator.id(PROGRESS_REPORT_TABLE_ID));
    }

    @LogMethod
    private void configureAssaySchema(String assayName)
    {
        ListHelper.LookupInfo lookupInfo = new ListHelper.LookupInfo("", "rho", assayName + " Query");
        _assayHelper.addAliasedFieldToMetadata("assay.General." + assayName, "Data", "RowId", "qcmessage", lookupInfo);
        clickButton("Save", 0);
        waitForText("Save successful.");
        clickButton("View Data");

        log("Set up data viewto include QC message");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("qcmessage/QueryMessage", "Query Message");
        _customizeViewsHelper.saveCustomView();
    }

    private void configureAssayProgressDashboard(String assayName, @Nullable String specimenTrimLength)
    {
        _portalHelper.clickWebpartMenuItem("Assay Progress Dashboard", "Customize");

        _ext4Helper.checkCheckbox(assayName);
        click(Locator.tagContainingText("label", "Specimen Report Study Folder Study"));
        if (specimenTrimLength != null)
            setFormElement(Locator.name("specimenTrimLength"), specimenTrimLength);

        clickButton("Save");
    }

    private void configureGroupingColumn(String label, String name)
    {
        _portalHelper.clickWebpartMenuItem("Assay Progress Dashboard", "Customize");
        setFormElement(Locator.xpath("//td/label[contains(text(),'" + label + "')]/../..//td/input[@name='groupingColumn']"), name);
        clickButton("Save");
    }

    private void ignoreSampleMindedData(String assayName)
    {
        _portalHelper.clickWebpartMenuItem("Assay Progress Report", "Customize");
        Locator ignoreSamplemindedCheckbox = Locator.css("table.ignoreSampleminded" + assayName + " input.x4-form-checkbox");
        Locator ignoreSamplemindedCheckboxChecked = Locator.css("table.x4-form-cb-checked.ignoreSampleminded" + assayName + " input.x4-form-checkbox");
        click(ignoreSamplemindedCheckbox);
        assertElementPresent(ignoreSamplemindedCheckboxChecked);
        clickButton("Save");

        verifyProgressReport(assay2, true, true, false);
    }
}
