/*
 * Copyright (c) 2013-2017 LabKey Corporation
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
package org.labkey.test.tests.flow;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Flow;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PipelineStatusTable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test checks the flow specimen foreign key behavior from flow.FCSFiles and flow.FCSAnalyses.
 */
@Category({DailyA.class, Flow.class})
public class FlowSpecimenTest extends BaseFlowTest
{
    public static final String STUDY_FOLDER = "KoStudy";

    public static final String PTID = "P5216";
    public static final String DATE = "2012-09-12";

    public static final String SPECIMEN_DATA =
           "Vial Id\tDraw Date\tParticipant\tVolume\tUnits\tSpecimen Type\tDerivative Type\tAdditive Type\n" +
           "Sample_002\t" + DATE + "\t" + PTID + "\t100\tml\t\t\t\n" +
           "Sample_003\t11/13/12\tP7312\t200\tml\t\t\t";

    @BeforeClass
    public static void initFlowFolders()
    {
        FlowSpecimenTest initTest = (FlowSpecimenTest)getCurrentTest();
        initTest.initializeStudyFolder();
    }

    @LogMethod
    private void initializeStudyFolder()
    {
        log("** Initialize Study Folder");
        _containerHelper.createSubfolder(getProjectName(), getProjectName(), STUDY_FOLDER, "Study", null);
        clickButton("Create Study");
        //use date-based study
        click(Locator.xpath("(//input[@name='timepointType'])[1]"));
        setFormElement(Locator.xpath("//input[@name='startDate']"), "2012-01-01");
        clickButton("Create Study");

        log("** Import specimens");
        clickTab("Specimen Data");
        waitAndClickAndWait(Locator.linkWithText("Import Specimens"));
        setFormElementJS(Locator.id("tsv"), SPECIMEN_DATA);
        clickButton("Submit");
        assertTextPresent("Specimens uploaded successfully");
    }

    @Test
    public void _doTestSteps()
    {
        importFCS31File();

        importFCSFiles();

        verifyFCSFileSpecimenFK();

        importFlowAnalysis();

        // Issue 16945: flow specimen FK doesn't work for 'fake' FCS file wells created during FlowJo import
        verifyFCSAnalysisSpecimenFK();

        copyFlowResultsToStudy();

        // Issue 16945: flow specimen FK doesn't work for 'fake' FCS file wells created during FlowJo import
        verifyFlowDatasetSpecimenFK();
    }

    @LogMethod
    protected void importFCSFiles()
    {
        log("** Import microFCS directory, set TargetStudy");
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("Browse for more FCS files to be imported"));
        _fileBrowserHelper.selectFileBrowserItem("flowjoquery/microFCS");
        _fileBrowserHelper.selectImportDataAction("Import Directory of FCS Files");
        selectOptionByText(Locator.id("targetStudy"), "/" + getProjectName() + "/" + STUDY_FOLDER + " (" + STUDY_FOLDER + " Study)");
        clickButton("Import Selected Runs");
        waitForPipeline(getContainerPath());

        log("** Verify Target Study is set on FCSFile run");
        beginAt("/flow-run/" + getContainerPath() + "/showRuns.view");
        DataRegionTable table = new DataRegionTable("query", this);
        assertEquals(STUDY_FOLDER + " Study", table.getDataAsText(0, "Target Study"));
        table.clickRowDetails(0);
        assertElementPresent(Locator.linkWithText(STUDY_FOLDER + " Study"));

        log("** Set ICS protocol metadata");
        setProtocolMetadata("Keyword $SRC", null, null, null, false);
    }

    @LogMethod
    protected void importFCS31File()
    {
        log("** Import FCS files of supported, and not, versions");
        goToFlowDashboard();
        clickAndWait(Locator.linkWithText("Browse for FCS files to be imported"));
        _fileBrowserHelper.selectFileBrowserItem("version");
        _fileBrowserHelper.selectImportDataAction("Import Directory of FCS Files");
        clickButton("Import Selected Runs");
        waitForPipeline(getContainerPath());
        verifyUploadReport(
                "Reading keywords from file Fake_2_0.fcs",
                "Reading keywords from file Fake_3_0.fcs",
                "Reading keywords from file Fake_3_1.fcs",
                "WARN : The FCS version FCS3.2 is not supported for file Fake_3_2.fcs. Supported versions are FCS2.0,FCS3.0,FCS3.1.");

    }

    @LogMethod
    private void verifyUploadReport(String... reportText)
    {
        waitForPipeline("/" + getProjectName() + "/" + getFolderName());
        goToFlowDashboard();
        waitForPipeline("/" + getProjectName() + "/" + getFolderName());
        clickAndWait(Locator.linkContainingText("Show Jobs"));
        PipelineStatusTable statusTable = new PipelineStatusTable(this);
        statusTable.clickStatusLink(0);
        assertTextPresent(reportText);
    }
    @LogMethod
    protected void importFlowAnalysis()
    {
        log("** Import workspace analysis");
        importAnalysis(getContainerPath(),
                "/flowjoquery/microFCS/microFCS.xml",
                SelectFCSFileOption.Previous,
                null,
                "microFCS",
                false,
                true);
    }

    @LogMethod
    private void copyFlowResultsToStudy()
    {
        // Copy the sample wells to the STUDY_FOLDER
        beginAt("/flow" + getContainerPath() + "/query.view?schemaName=flow&query.queryName=FCSAnalyses");
        click(Locator.checkboxByName(".toggle"));
        clickButton("Copy to Study");
        selectOptionByText(Locator.name("targetStudy"), "/" + getProjectName() + "/" + STUDY_FOLDER + " (" + STUDY_FOLDER + " Study)");
        clickButton("Next");
        assertTitleContains("Copy to " + STUDY_FOLDER + " Study: Verify Results");
        // verify specimen information is filled in for '118795.fcs' FCS file
        assertEquals(PTID, getFormElement(Locator.name("participantId").index(0)));
        assertEquals(DATE, getFormElement(Locator.name("date").index(0)));
        clickButton("Copy to Study");

        assertTitleContains("Dataset: Flow");
        assertTrue("Expected go to STUDY_FOLDER container", getCurrentRelativeURL().contains("/" + STUDY_FOLDER));
        // PTID and Date from specimen vial 'Sample_002' from specimen repository
        assertTextPresent(PTID, DATE);
    }

    @LogMethod
    protected void verifyFCSFileSpecimenFK()
    {
        log("** Verify specimen FK from flow.FCSFile table");
        beginAt("/flow" + getContainerPath() + "/query.view?schemaName=flow&query.queryName=FCSFiles");
        verifySpecimenFK("");
    }

    @LogMethod
    protected void verifyFCSAnalysisSpecimenFK()
    {
        log("** Verify specimen FK from flow.FCSAnalysis table");
        beginAt("/flow" + getContainerPath() + "/query.view?schemaName=flow&query.queryName=FCSAnalyses");
        verifySpecimenFK("FCSFile/");
    }

    @LogMethod
    protected void verifyFlowDatasetSpecimenFK()
    {
        log("** Verify specimen FK from flow dataset");
        beginAt("/study/" + getProjectName() + "/" + STUDY_FOLDER + "/dataset.view?datasetId=5001");
        verifySpecimenFK("FCSFile/");
    }

    @LogMethod
    protected void verifySpecimenFK(String lookupPrefix)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addColumn(lookupPrefix + "SpecimenID");
        _customizeViewsHelper.addColumn(lookupPrefix + "SpecimenID/GlobalUniqueId");
        _customizeViewsHelper.addColumn(lookupPrefix + "SpecimenID/Volume");
        _customizeViewsHelper.addColumn(lookupPrefix + "SpecimenID/Specimen/SequenceNum");
        _customizeViewsHelper.saveCustomView();

        // verify the specimen columns are present
        DataRegionTable table = new DataRegionTable(getDriver().getCurrentUrl().contains("dataset.view") ? "Dataset" : "query", this);
        int row = table.getRowIndex("Name", "118795.fcs");
        assertEquals("Sample_002", table.getDataAsText(row, "Specimen ID"));
        assertEquals("Sample_002", table.getDataAsText(row, "Specimen Global Unique Id"));
        assertEquals("100.0", table.getDataAsText(row, "Specimen Volume"));
        assertEquals("20120912", table.getDataAsText(row, "Sequence Num"));
    }

}
