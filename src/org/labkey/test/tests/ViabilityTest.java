/*
 * Copyright (c) 2009-2017 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.categories.Assays;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.AssayDesignerPage;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.TextSearcher;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class, Assays.class})
public class ViabilityTest extends AbstractViabilityTest
{
    private static final String ASSAY_NAME = "Guava Assay";
    private static final String STUDY2_NAME = "Study2 Folder";
    private static final String SAVE_AND_FINISH = "Save and Finish";
    private static final String SAVE_AND_IMPORT = "Save and Import Another Run";

    @Override
    protected String getProjectName()
    {
        return "Viability";
    }

    @Override
    protected String getFolderName()
    {
        return "Viability Folder";
    }

    @Override
    protected String getAssayName()
    {
        return ASSAY_NAME;
    }

    @Test
    public void runUITests() throws Exception
    {
        // setup a scripting engine to run a java transform script
        prepareProgrammaticQC();

        initializeStudyFolder();
        importSpecimens();
        createViabilityAssay();
        setupPipeline();


        runUploadTest();
        runReRunTest();
        runResultSpecimenLookupTest();
        runTargetStudyTest();
        runTransformTest();
    }

    protected void runUploadTest()
    {
        uploadViabilityRun("/sampledata/viability/small.VIA.csv", true);

        log("** Check form field values");
        assertFormElementEquals(Locator.name("_pool_1604505335_0_ParticipantID"), "160450533");
        assertFormElementEquals(Locator.name("_pool_1604505335_0_VisitID"), "5.0");
        assertFormElementEquals(Locator.name("_pool_1604505335_0_TotalCells"), "3.700E7");
        assertFormElementEquals(Locator.name("_pool_1604505335_0_ViableCells"), "3.127E7");
        assertFormElementEquals(Locator.name("_pool_1604505335_0_Viability"), "84.5%");
        assertNotChecked(Locator.checkboxByName("_pool_1604505335_0_Unreliable"));
        assertFormElementEquals(Locator.name("_pool_1604505335_0_IntValue"), "");

        clickButton(SAVE_AND_FINISH, 0);
        String alertText = cancelAlert();
        assertTrue(alertText.contains("Save anyway"));
        assertElementNotPresent(Locator.lkButtonDisabled(SAVE_AND_FINISH));

        clickButton(SAVE_AND_IMPORT, 0);
        alertText = cancelAlert();
        assertTrue(alertText.contains("Save anyway"));
        assertElementNotPresent(Locator.lkButtonDisabled(SAVE_AND_IMPORT));

        log("** Insert specimen IDs");
        addSpecimenIds("_pool_1604505335_0_SpecimenIDs", "vial2", "vial3", "vial1", "foobar");
        addSpecimenIds("_pool_1594020325_1_SpecimenIDs", "vial1");
        addSpecimenIds("_pool_161400006105_2_SpecimenIDs", "vial2");
        addSpecimenIds("_pool_161400006115_3_SpecimenIDs", "vial3");
        addSpecimenIds("_pool_1614016435_4_SpecimenIDs", "xyzzy");

        log("** Set Unreliable flag and IntValue");
        checkCheckbox(Locator.checkboxByName("_pool_1604505335_0_Unreliable"));
        setFormElement(Locator.xpath("//input[@name='_pool_1604505335_0_IntValue'][1]"), "300");

        doAndWaitForPageToLoad(() ->
        {
            clickButton(SAVE_AND_FINISH, 0);
            String actualConfirmation = acceptAlert();
            log("** Got confirmation: " + actualConfirmation);
        });


        setSelectedFields("/" + getProjectName() + "/" + getFolderName(), "assay.Viability." + getAssayName(), "Data", null,
                new String[] { "Run", "ParticipantID", "VisitID", "PoolID",
                        "TotalCells", "ViableCells", "Viability", "OriginalCells", "Recovery",
                        "SpecimenIDs", "SpecimenCount", "SpecimenMatchCount", "SpecimenMatches", "Unreliable", "IntValue"});

        clickAndWait(Locator.linkContainingText(".VIA.csv")); // run name (small.VIA.csv or small-XXX.VIA.csv)
        DataRegionTable table = new DataRegionTable("Data", this);
        String runName = table.getDataAsText(0, "Run");
        assertTrue(runName.contains("small") && runName.contains(".VIA.csv"));
        assertEquals("160450533", table.getDataAsText(0, "Participant ID"));
        assertEquals("5.0", table.getDataAsText(0, "Visit ID"));
        assertEquals("160450533-5", table.getDataAsText(0, "Pool ID"));
        assertEquals("3.700E7", table.getDataAsText(0, "Total Cells"));
        assertEquals("3.127E7", table.getDataAsText(0, "Viable Cells"));
        assertEquals("84.5%", table.getDataAsText(0, "Viability"));
        assertEquals("6.000E7", table.getDataAsText(0, "Original Cells"));

        assertEquals("foobar,vial1,vial2,vial3", table.getDataAsText(0, "Specimen IDs"));
        assertEquals("vial1,vial2,vial3", table.getDataAsText(0, "SpecimenMatches"));
        assertEquals("4", table.getDataAsText(0, "SpecimenCount"));
        assertEquals("3", table.getDataAsText(0, "SpecimenMatchCount"));
        assertEquals("52.11%", table.getDataAsText(0, "Recovery"));
        assertEquals("true", table.getDataAsText(0, "Unreliable?"));
        assertEquals("300", table.getDataAsText(0, "IntValue"));

        assertEquals("vial1", table.getDataAsText(1, "Specimen IDs"));
        assertEquals("1", table.getDataAsText(1, "SpecimenCount"));
        assertEquals("1", table.getDataAsText(1, "SpecimenMatchCount"));
        assertEquals("115.67%", table.getDataAsText(1, "Recovery"));

        assertEquals("161400006", table.getDataAsText(2, "Participant ID"));
        assertEquals("5.0", table.getDataAsText(2, "Visit ID"));
        assertEquals("161400006.10-5", table.getDataAsText(2, "Pool ID"));
        assertEquals("vial2", table.getDataAsText(2, "Specimen IDs"));
        assertEquals("1", table.getDataAsText(2, "SpecimenCount"));
        assertEquals("1", table.getDataAsText(2, "SpecimenMatchCount"));
        assertEquals("105.78%", table.getDataAsText(2, "Recovery"));

        assertEquals("161400006", table.getDataAsText(3, "Participant ID"));
        assertEquals("5.0", table.getDataAsText(3, "Visit ID"));
        assertEquals("161400006.11-5", table.getDataAsText(3, "Pool ID"));

        assertEquals("xyzzy", table.getDataAsText(4, "Specimen IDs"));
        assertEquals("1", table.getDataAsText(4, "SpecimenCount"));
        assertEquals("0", table.getDataAsText(4, "SpecimenMatchCount"));
        assertEquals(" ", table.getDataAsText(4, "Recovery"));

        assertEquals(" ", table.getDataAsText(5, "Specimen IDs"));
        assertEquals("0", table.getDataAsText(5, "SpecimenCount"));
        assertEquals(" ", table.getDataAsText(5, "SpecimenMatchCount"));
        assertEquals(" ", table.getDataAsText(5, "Recovery"));
    }

    protected void runReRunTest()
    {
        log("** Test Day2 re-run scenario");
        final String runName = "re-run scenario";
        reuploadViabilityRun("/sampledata/viability/small.VIA.csv", runName);

        // Check the 'SpecimenIDs' and 'IntValue' field is copied on re-run
        assertFormElementEquals(Locator.xpath("//input[@name='_pool_1604505335_0_SpecimenIDs']").index(0), "foobar");
        assertFormElementEquals(Locator.xpath("//input[@name='_pool_1604505335_0_SpecimenIDs']").index(1), "vial1");
        assertFormElementEquals(Locator.xpath("//input[@name='_pool_1604505335_0_SpecimenIDs']").index(2), "vial2");
        assertFormElementEquals(Locator.xpath("//input[@name='_pool_1604505335_0_SpecimenIDs']").index(3), "vial3");
        assertFormElementEquals(Locator.xpath("//input[@name='_pool_1614016435_4_SpecimenIDs']").index(0), "xyzzy");
        assertFormElementEquals(Locator.xpath("//input[@name='_pool_1604505335_0_IntValue']"), "300");

        // Check the 'Unreliable' field isn't copied on re-run
        assertNotChecked(Locator.checkboxByName("_pool_1604505335_0_Unreliable"));

        doAndWaitForPageToLoad(() ->
        {
            clickButton(SAVE_AND_FINISH, 0);
            String actualConfirmation = acceptAlert();
            log("** Got confirmation: " + actualConfirmation);
        });

        log(".. checking re-runs are placed in the same Run Group");
        DataRegionTable runsTable = new DataRegionTable("Runs", this);
        assertEquals(2, runsTable.getDataRowCount());
        String runGroupName = runsTable.getDataAsText(0, "Run Groups");
        assertEquals(runGroupName, runsTable.getDataAsText(1, "Run Groups"));
        clickAndWait(runsTable.link(0, "Run Groups"));

        // Run Group name should be "Assay Name-XXX" where XXX is the run group rowid
        String runGroupRowId = getUrlParam("rowId", true);
        assertEquals(getAssayName() + "-" + runGroupRowId, runGroupName);
        assertTextPresent("Re-importing any Viability run in this run group will place the new run in this same run group.");
        runsTable = new DataRegionTable("Runs", this);
        assertEquals(2, runsTable.getDataRowCount());

        log(".. checking appropriate fields are copied in re-run");
        clickAndWait(Locator.linkWithText(runName));

        DataRegionTable dataTable = new DataRegionTable("Data", this);
        assertEquals(" ", dataTable.getDataAsText(0, "Unreliable?"));
        assertEquals("300", dataTable.getDataAsText(0, "IntValue"));
    }

    protected void runResultSpecimenLookupTest()
    {
        log("** Checking ResultSpecimens lookups");
        beginAt("/query/" + getProjectName() + "/" + getFolderName() + "/executeQuery.view?schemaName=assay&query.queryName=" + getAssayName() + " ResultSpecimens");
        DataRegionTable table = new DataRegionTable("query", this);
        assertTextPresent(new TextSearcher(table.getComponentElement()::getText), "foobar", "vial1", "xyzzy", "160450533-5", "161400006.11-5");

        setSelectedFields("/" + getProjectName() + "/" + getFolderName(), "assay", getAssayName() + " ResultSpecimens", null,
                new String[] { "ResultID", "ResultID/Recovery", "Specimen", "SpecimenIndex", "SpecimenID/Volume", "SpecimenID/Specimen/VolumeUnits"/*, "SpecimenID/AssayMatch"*/});
        // WONT_FIX: Issue 24688: Viability/Guava assay - bad SQL for specimen lookup AssayMatch column
        table = new DataRegionTable("query", this);
        assertTextNotPresent(new TextSearcher(table.getComponentElement()::getText), "foobar");
        assertTextPresent(new TextSearcher(table.getComponentElement()::getText), "161400006.11-5", "105.78%", "20,000,000.0", "CEL");
    }

    protected void runTransformTest()
    {
        // add the transform script to the assay
        log("** Uploading Viability Runs with a transform script");

        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText(getAssayName()));
        _assayHelper.clickEditAssayDesign(true);

        AssayDesignerPage assayDesigner = new AssayDesignerPage(getDriver());
        assayDesigner.addTransformScript(new File(TestFileUtils.getLabKeyRoot(), "/sampledata/qc/transform.jar"));
        assayDesigner.saveAndClose();

        final String runName = "transformed assayId";
        uploadViabilityRun("/sampledata/viability/small.VIA.csv", runName, false);

        log("** Check form field values");
        assertFormElementEquals(Locator.name("_pool_1604505335_0_ParticipantID"), "160450533");
        assertFormElementEquals(Locator.name("_pool_1604505335_0_VisitID"), "5.0");
        assertFormElementEquals(Locator.name("_pool_1604505335_0_TotalCells"), "3.700E7");
        assertFormElementEquals(Locator.name("_pool_1604505335_0_ViableCells"), "3.127E7");
        assertFormElementEquals(Locator.name("_pool_1604505335_0_Viability"), "84.5%");

        log("** Insert specimen IDs");
        addSpecimenIds("_pool_1604505335_0_SpecimenIDs", "vial2", "vial3", "vial1", "foobar");
        addSpecimenIds("_pool_1594020325_1_SpecimenIDs", "vial1");
        addSpecimenIds("_pool_161400006105_2_SpecimenIDs", "vial2");
        addSpecimenIds("_pool_161400006115_3_SpecimenIDs", "vial3");
        addSpecimenIds("_pool_1614016435_4_SpecimenIDs", "xyzzy");

        doAndWaitForPageToLoad(() ->
        {
            clickButton(SAVE_AND_FINISH, 0);
            String actualConfirmation = acceptAlert();
            log("** Got confirmation: " + actualConfirmation);
        });

        // verify the description error was generated by the transform script
        clickAndWait(Locator.linkWithText("transformed assayId"));

        DataRegionTable table = new DataRegionTable("Data", this);
        List<String> specimenIDColumnValues = table.getColumnDataAsText("Specimen IDs");
        for (String s : specimenIDColumnValues)
        {
            assertEquals("Specimen not Transformed", "Transformed", s);
        }
    }

    protected void runTargetStudyTest()
    {
        log("** Create Study2");
        _containerHelper.createSubfolder(getProjectName(), getProjectName(), STUDY2_NAME, "Study", null, true);
        clickButton("Create Study");
        clickButton("Create Study");

        log("** Import specimens2");
        // create a 'xyzzy' vial id
        importSpecimens(STUDY2_NAME, "/sampledata/viability/specimens2.txt");

        log("** Test Target Study as Result Domain Field");

        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText(getAssayName()));
        _assayHelper.clickEditAssayDesign(true);
        waitForElement(Locator.lkButton("Add Script"));

        // remove TargetStudy field from the Batch domain and add it to the Result domain.
        _listHelper.deleteField("Batch Fields", 0);
        _listHelper.addField("Guava Assay Result Fields", "TargetStudy", "Target Study", ListHelper.ListColumnType.String);
        clickButton("Save & Close");

        navigateToFolder(getProjectName(), getFolderName());
        clickAndWait(Locator.linkWithText(getAssayName()));
        clickButton("Import Data");

        final String runName = "result-level target study";
        uploadViabilityRun("/sampledata/viability/small.VIA.csv", runName, false);

        log("** Test 'same' checkbox for TargetStudy");
        String targetStudyOptionText = "/" + getProjectName() + "/" + getFolderName() + " (" + getFolderName() + " Study)";
        selectOptionByText(Locator.name("_pool_1604505335_0_TargetStudy"), targetStudyOptionText);
        assertEquals("[None]", getSelectedOptionText(Locator.name("_pool_1594020325_1_TargetStudy")));
        checkCheckbox(Locator.checkboxById("_pool_1604505335_0_TargetStudyCheckBox"));
        assertEquals("Target study didn't propogate with 'Same' checkbox.",
                getSelectedOptionValue(Locator.name("_pool_1604505335_0_TargetStudy")),
                getSelectedOptionValue(Locator.name("_pool_1594020325_1_TargetStudy")));
        assertEquals("Target study didn't propogate with 'Same' checkbox.",
                getSelectedOptionValue(Locator.name("_pool_1604505335_0_TargetStudy")),
                getSelectedOptionValue(Locator.name("_pool_161400006115_3_TargetStudy")));
        uncheckCheckbox(Locator.checkboxById("_pool_1604505335_0_TargetStudyCheckBox"));

        // clear TargetStudy for 'vial2' and set the TargetStudy for 'vial3' and 'xyzzy'
        selectOptionByText(Locator.name("_pool_161400006105_2_TargetStudy"), "[None]");
        selectOptionByText(Locator.name("_pool_161400006115_3_TargetStudy"), "/" + getProjectName() + "/" + STUDY2_NAME + " (" + STUDY2_NAME + " Study)");
        selectOptionByText(Locator.name("_pool_1614016435_4_TargetStudy"), "/" + getProjectName() + "/" + STUDY2_NAME + " (" + STUDY2_NAME + " Study)");

        log("** Insert specimen IDs");
        addSpecimenIds("_pool_1604505335_0_SpecimenIDs", "vial2", "vial3", "vial1", "foobar");
        addSpecimenIds("_pool_1594020325_1_SpecimenIDs", "vial1");
        addSpecimenIds("_pool_161400006105_2_SpecimenIDs", "vial2");
        addSpecimenIds("_pool_161400006115_3_SpecimenIDs", "vial3");
        addSpecimenIds("_pool_1614016435_4_SpecimenIDs", "xyzzy");

        doAndWaitForPageToLoad(() ->
        {
            clickButton(SAVE_AND_FINISH, 0);
            String actualConfirmation = acceptAlert();
            log("** Got confirmation: " + actualConfirmation);
        });

        clickAndWait(Locator.linkWithText(runName));

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("TargetStudy", "Target Study");
        _customizeViewsHelper.saveDefaultView();

        DataRegionTable table = new DataRegionTable("Data", this);
        assertEquals("foobar,vial1,vial2,vial3", table.getDataAsText(0, "Specimen IDs"));
        assertEquals("4", table.getDataAsText(0, "SpecimenCount"));
        assertEquals("3", table.getDataAsText(0, "SpecimenMatchCount"));
        assertEquals("52.11%", table.getDataAsText(0, "Recovery"));
        assertEquals(getFolderName() + " Study", table.getDataAsText(0, "TargetStudy"));

        assertEquals("vial2", table.getDataAsText(2, "Specimen IDs"));
        assertEquals("1", table.getDataAsText(2, "SpecimenCount"));
        assertEquals("0", table.getDataAsText(2, "SpecimenMatchCount"));
        assertEquals(" ", table.getDataAsText(2, "Recovery"));
        assertEquals(" ", table.getDataAsText(2, "TargetStudy"));

        assertEquals("vial3", table.getDataAsText(3, "Specimen IDs"));
        assertEquals("1", table.getDataAsText(3, "SpecimenCount"));
        assertEquals("0", table.getDataAsText(3, "SpecimenMatchCount"));
        assertEquals(" ", table.getDataAsText(3, "Recovery"));
        assertEquals(STUDY2_NAME + " Study", table.getDataAsText(3, "TargetStudy"));

        assertEquals("xyzzy", table.getDataAsText(4, "Specimen IDs"));
        assertEquals("1", table.getDataAsText(4, "SpecimenCount"));
        assertEquals("1", table.getDataAsText(4, "SpecimenMatchCount"));
        assertEquals("88.88%", table.getDataAsText(4, "Recovery"));
        assertEquals(STUDY2_NAME + " Study", table.getDataAsText(4, "TargetStudy"));

        // UNDONE: participant/visit resolver test
        // UNDONE: copy-to-study
    }
}
