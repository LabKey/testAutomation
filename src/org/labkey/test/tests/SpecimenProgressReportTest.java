/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.APIContainerHelper;
import org.labkey.test.util.AbstractContainerHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelperWD;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.UIAssayHelper;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * User: elvan
 * Date: 9/11/12
 * Time: 2:42 PM
 */
@Category({DailyA.class})
public class SpecimenProgressReportTest extends BaseWebDriverTest
{
    public static final String STUDY_PIPELINE_ROOT = getLabKeyRoot() + "/sampledata/specimenprogressreport";
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
    private static final String assay2XarPath = "/assays/RNA.xar";
    private Locator.XPathLocator tableLoc = Locator.xpath("//table[@id='dataregion_ProgressReport']");

    @Override
    public String getAssociatedModuleDirectory()
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
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        _assayHelper = new UIAssayHelper(this);
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.createSubfolder(getProjectName(), studyFolder, "Study");
        importFolderFromZip(new File(STUDY_PIPELINE_ROOT, "Study.folder.zip"));

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
        assertTextPresent("34 collections have occurred.",  "48 results from " + assay1 + " have been uploaded", "48 " + assay1 + " queries");
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
        waitForElement(tableLoc);
        ignoreSampleMindedData(assay2);
        verifyProgressReport(assay2, true);
        verifyUnassayedSpecimenQuery(assay2);
    }

    @LogMethod
    private void manageSpecimenConfiguration()
    {
        clickFolder(studyFolder);
        _portalHelper.addWebPart("Assay Schedule");
        _portalHelper.addQueryWebPart("rho");
        _portalHelper.addQueryWebPart("study");

        // lookup the locationId for the Main site from the study.Locations table
        goToSchemaBrowser();
        selectQuery("study", "Location");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        DataRegionTable drt = new DataRegionTable("query", this);
        _customizeViewsHelper.openCustomizeViewPanel();
        //TODO: why can't we get the value from the RowId column when it is first in the data region?
        _customizeViewsHelper.removeCustomizeViewColumn("RowId");
        _customizeViewsHelper.addCustomizeViewColumn("RowId");
        _customizeViewsHelper.applyCustomView();
        int locationId = Integer.parseInt(drt.getDataAsText(drt.getRow("Label", "Main"), "RowId"));

        // add the specimen configurations to the manage page
        goToModule("rho");
        addSpecimenConfiguration("PCR", "R", locationId, "CEF-R Cryovial", false);
        addSpecimenConfiguration("PCR", "R", locationId, "UPR Micro Tube", true);
        addSpecimenConfiguration("RNA", "R", locationId, "TGE Cryovial", true);
        sleep(1000); // give the store a second to save the configurations

        // lookup the config IDs to use in setting the visits
        clickFolder(studyFolder);
        clickAndWait(Locator.linkWithText("AssaySpecimen"));
        drt = new DataRegionTable("query", this);
        String pcr1RowId = drt.getDataAsText(drt.getRow("TubeType", "CEF-R Cryovial"), "RowId");
        String pcr2RowId = drt.getDataAsText(drt.getRow("TubeType", "UPR Micro Tube"), "RowId");
        String rnaRowId = drt.getDataAsText(drt.getRow("TubeType", "TGE Cryovial"), "RowId");
        // set the specimen configuration visits (by checking the checkboxes on the manage page
        goToManageStudy();
        clickAndWait(Locator.linkWithText("Manage Assay Schedule"));
        waitForElement(Locator.css("#AssaySpecimenVisitPanel table.x4-grid-table"));
        setSpecimenConfigurationVisit(pcr1RowId, new String[]{"3", "5", "6", "8", "10", "11", "12", "13", "14", "15", "16", "17", "18", "20", "SR"});
        setSpecimenConfigurationVisit(pcr2RowId, new String[]{"3", "5", "6", "8", "10", "11", "12", "13", "14", "15", "16", "17", "18", "20", "SR"});
        setSpecimenConfigurationVisit(rnaRowId, new String[]{"PT1", "0", "6", "20", "SR"});
        sleep(1000); // give the store a second to save the configurations
        // set the assay plan
        String assayPlanTxt = "My assay plan " + TRICKY_CHARACTERS_FOR_PROJECT_NAMES + INJECT_CHARS_1 + INJECT_CHARS_2;
        setFormElement(Locator.name("assayPlan"), assayPlanTxt);
        clickButton("Save");

        checkRhoQueryRowCount("AssaySpecimenVisit", 35);
        checkRhoQueryRowCount("AssaySpecimenMap", 35);
        checkRhoQueryRowCount("MissingSpecimen", 2);
        checkRhoQueryRowCount("MissingVisit", 3);

        // verify display of assay schedule webpart
        clickAndWait(Locator.linkWithText("Overview"));
        waitForElement(Locator.tagWithClass("table", "study-vaccine-design"));
        assertTextPresent(assayPlanTxt);
        assertTextPresent("\u2713", 35);
    }

    private void checkRhoQueryRowCount(String name, int expectedCount)
    {
        clickFolder(studyFolder);
        clickAndWait(Locator.linkWithText(name));
        waitForElement(Locator.id("dataregion_query"));
        DataRegionTable drt = new DataRegionTable("query", this);
        assertEquals("Unexpected number of rows in the query", expectedCount, drt.getDataRowCount());
    }

    private void addSpecimenConfiguration(String assayName, String source, int locationId, String tubeType, boolean expectRows)
    {
        Locator.XPathLocator configGridRow = Locator.xpath("id('AssaySpecimenConfigGrid')//table").withClass("x4-grid-table").append("/tbody/tr");
        if (expectRows)
            waitForElement(configGridRow);
        else
            waitForText("No assay configurations");
        int expectedRowIndex = getElementCount(configGridRow);
        clickButton("Insert New", 0);
        waitForElement(Locator.name("AssayName"));
        setFormElement(Locator.name("AssayName"), assayName);
        setFormElement(Locator.name("Description"), assayName + " " + tubeType);
        setFormElement(Locator.name("Source"), source);
        setFormElement(Locator.name("LocationId"), String.valueOf(locationId));
        setFormElement(Locator.name("TubeType"), tubeType);
        clickButton("Submit");
        waitForElement(configGridRow.index(expectedRowIndex));
    }

    private void setSpecimenConfigurationVisit(String scRowId, String[] labels)
    {
        for (String label : labels)
        {
            checkCheckbox(Locator.name("sc" + scRowId + "v" + label));
        }
    }

    @LogMethod
    private void verifyAssayResultInvalid(String assayName, String runName)
    {
        clickFolder(assayFolder);
        waitForElement(tableLoc);
        assertEquals(0, getElementCount( Locator.xpath("//td[contains(@class, 'available')]")));
        assertEquals(24, getElementCount( Locator.xpath("//td[contains(@class, 'query')]")));
        assertEquals(2, getElementCount( Locator.xpath("//td[contains(@class, 'collected')]")));
        assertEquals(0, getElementCount(Locator.xpath("//td[contains(@class, 'invalid')]")));
        assertEquals(54, getElementCount(Locator.xpath("//td[contains(@class, 'expected')]")));
        assertEquals(3, getElementCount(Locator.xpath("//td[contains(@class, 'missing')]")));

        flagSpecimenForReview(assayName, null);

        waitForElement(tableLoc);
        assertEquals(1, getElementCount(Locator.xpath("//td[contains(@class, 'invalid')]")));

        // verify legend text and ordering
        assertTextPresentInThisOrder("specimen expected", "specimen received by lab", "specimen not collected",
                "specimen collected", "specimen received but invalid", "assay results available"); // "query" appears too many times on page!
    }

    @LogMethod
    private void verifyAdditionalGroupingColumn(String assayName, String groupCol)
    {
        clickFolder(assayFolder);
        waitForElement(tableLoc);
        waitForText("48 " + assayName + " queries");
        configureGroupingColumn(assayName, groupCol);
        waitForElement(tableLoc);
        clickAndWait(Locator.linkWithText("48 results from " + assayName + " have been uploaded."));
        assertTextPresent("Result reported with no corresponding specimen collected", 8);
        assertTextPresent("2 results found for this participant and visit combination", 4);
        assertTextPresent("This specimen type is not expected for this visit", 20);
    }

    @LogMethod
    private void verifyUnscheduledVisitDisplay(String assayName)
    {
        clickFolder(assayFolder);
        waitForElement(tableLoc);
        configureAssayProgressDashboard(assay2);
        configureAssaySchema(assayName);

        flagSpecimenForReview(assayName, "2011-03-02");

        clickFolder(assayFolder);
        verifyProgressReport(assayName, false);

        clickAndWait(Locator.linkWithText("8 results from " + assayName + " have been uploaded."));
        assertTextPresent("Result reported with no corresponding specimen collected", 2);
        assertTextPresent("This specimen type is not expected for this visit", 1);
    }

    private void verifyProgressReport(String assayName, boolean ignoreSampleminded)
    {
        int ignored = ignoreSampleminded ? 2 : 0;

        waitForElement(tableLoc);
        _ext4Helper.selectRadioButtonById(assayName + "-boxLabelEl");
        waitForElement(tableLoc);
        assertTextPresentInThisOrder("SR1", "SR2");
        assertEquals(4, getElementCount(Locator.xpath("//td[contains(@class, 'available')]")));
        assertEquals(3, getElementCount(Locator.xpath("//td[contains(@class, 'query')]")));
        assertEquals(2 + ignored, getElementCount(Locator.xpath("//td[contains(@class, 'collected')]")));
        assertEquals(2 - ignored, getElementCount(Locator.xpath("//td[contains(@class, 'received')]")));
        assertEquals(1, getElementCount(Locator.xpath("//td[contains(@class, 'invalid')]")));
        assertEquals(10, getElementCount(Locator.xpath("//td[contains(@class, 'expected')]")));
        assertEquals(3, getElementCount(Locator.xpath("//td[contains(@class, 'missing')]")));
    }

    private void verifyUnassayedSpecimenQuery(String assayName)
    {
        goToSchemaBrowser();
        selectQuery("rho", assayName + " Base Unassayed Specimens");
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
        DataRegionTable drt = new DataRegionTable("query", this);
        assertEquals(4, drt.getDataRowCount());
        assertTextPresent("specimen collected", 2);
        assertTextPresent("specimen received by lab", 2);
    }

    @LogMethod
    private void flagSpecimenForReview(String assayName, @Nullable String collectionDateFilterStr)
    {
        clickFolder(assayFolder);
        waitForElement(tableLoc);
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
        waitForElement(tableLoc);
    }

    @LogMethod
    private void createAssayFolder() throws CommandException, IOException
    {
        goToProjectHome();
        _containerHelper.createSubfolder(getProjectName(), assayFolder, "Assay");
        enableModule("rho", false);

        _assayHelper.uploadXarFileAsAssayDesign(STUDY_PIPELINE_ROOT + assay1XarPath, ++pipelineCount, assay1);
        _assayHelper.importAssay(assay1, new File(STUDY_PIPELINE_ROOT + "/assays/" + assay1File),  getProjectName() + "/" + assayFolder, Collections.<String, Object>singletonMap("ParticipantVisitResolver", "SampleInfo") );
        clickFolder(assayFolder);
        _assayHelper.uploadXarFileAsAssayDesign(STUDY_PIPELINE_ROOT + assay2XarPath, ++pipelineCount, assay2);
        _assayHelper.importAssay(assay2, new File(STUDY_PIPELINE_ROOT + "/assays/" + assay2File),  getProjectName() + "/" + assayFolder, Collections.<String, Object>singletonMap("ParticipantVisitResolver", "SampleInfo") );


        clickFolder(assayFolder);
        _portalHelper.addWebPart("Assay Progress Dashboard");
        _portalHelper.addWebPart("Assay Progress Report");
        assertTextPresent("You must first configure the assay(s) that you want to run reports from. Click on the customize menu for this web part and select the Assays that should be included in this report", 2);

        configureAssayProgressDashboard(assay1);
        configureAssaySchema(assay1);
        clickFolder(assayFolder);
        waitForElement(tableLoc);
    }

    @LogMethod
    private void configureAssaySchema(String assayName)
    {
        ListHelperWD.LookupInfo lookupInfo = new ListHelperWD.LookupInfo("", "rho", assayName + " Query");
        _assayHelper.addAliasedFieldToMetadata("assay.General." + assayName, "Data", "RowId", "qcmessage", lookupInfo);
        clickButton("Save", 0);
        waitForText("Save successful.");
        clickButton("View Data");

        log("Set up data viewto include QC message");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("qcmessage/QueryMessage", "Query Message");
        _customizeViewsHelper.saveCustomView();
    }

    private void configureAssayProgressDashboard(String assayName)
    {
        _portalHelper.clickWebpartMenuItem("Assay Progress Dashboard", "Customize");
        _extHelper.checkCheckbox(assayName);
        click(Locator.tagContainingText("label", "Specimen Report Study Folder Study"));
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
    }
}
