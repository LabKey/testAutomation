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

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;

import java.io.File;
import java.util.List;

/**
 * User: kevink
 * Date: Sep 30, 2009
 */
public class ViabilityTest extends AbstractViabilityTest
{
    public static final String PROJECT_NAME = "Viability";
    public static final String FOLDER_NAME = "Viability Folder";
    private static final String ASSAY_NAME = "Guava Assay";
    private static final String STUDY2_NAME = "Study2 Folder";


    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected String getFolderName()
    {
        return FOLDER_NAME;
    }

    @Override
    protected String getAssayName()
    {
        return ASSAY_NAME;
    }

    protected void runUITests() throws Exception
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
        assertFormElementEquals("_pool_1604505335_0_ParticipantID", "160450533");
        assertFormElementEquals("_pool_1604505335_0_VisitID", "5.0");
        assertFormElementEquals("_pool_1604505335_0_TotalCells", "3.700E7");
        assertFormElementEquals("_pool_1604505335_0_ViableCells", "3.127E7");
        assertFormElementEquals("_pool_1604505335_0_Viability", "84.5%");
        assertFormElementEquals("_pool_1604505335_0_Unreliable", "off");
        assertFormElementEquals("_pool_1604505335_0_IntValue", "");

        log("** Insert specimen IDs");
        addSpecimenIds("_pool_1604505335_0_SpecimenIDs", "vial2", "vial3", "vial1", "foobar");
        addSpecimenIds("_pool_1594020325_1_SpecimenIDs", "vial1");
        addSpecimenIds("_pool_161400006105_2_SpecimenIDs", "vial2");
        addSpecimenIds("_pool_161400006115_3_SpecimenIDs", "vial3");
        addSpecimenIds("_pool_1614016435_4_SpecimenIDs", "xyzzy");

        log("** Set Unreliable flag and IntValue");
        checkCheckbox("_pool_1604505335_0_Unreliable", 0);
        setFormElement("//input[@name='_pool_1604505335_0_IntValue'][1]", "300");

        clickNavButton("Save and Finish", 0);
        String expectConfirmation = "Some values are missing for the following pools:\n\n" +
                "  Sample number 33: SpecimenIDs\n" +
                "  Sample number 34: SpecimenIDs\n\n" +
                "Save anyway?";
        String actualConfirmation = getConfirmationAndWait();
        log("** Got confirmation: " + actualConfirmation);

        //This is a test for Issue 10054, which has been resolved as won't fix.
        //Assert.assertEquals(expectConfirmation, actualConfirmation);

        setSelectedFields("/" + getProjectName() + "/" + getFolderName(), "assay", getAssayName() + " Data", null,
                new String[] { "Run", "ParticipantID", "VisitID", "PoolID",
                        "TotalCells", "ViableCells", "Viability", "OriginalCells", "Recovery",
                        "SpecimenIDs", "SpecimenCount", "SpecimenMatchCount", "SpecimenMatches", "Unreliable", "IntValue"});

        clickLinkContainingText(".VIA.csv"); // run name (small.VIA.csv or small-XXX.VIA.csv)
        DataRegionTable table = new DataRegionTable(getAssayName() + " Data", this);
        String runName = table.getDataAsText(0, "Run");
        Assert.assertTrue(runName.contains("small") && runName.contains(".VIA.csv"));
        Assert.assertEquals("160450533", table.getDataAsText(0, "Participant ID"));
        Assert.assertEquals("5.0", table.getDataAsText(0, "Visit ID"));
        Assert.assertEquals("160450533-5", table.getDataAsText(0, "Pool ID"));
        Assert.assertEquals("3.700E7", table.getDataAsText(0, "Total Cells"));
        Assert.assertEquals("3.127E7", table.getDataAsText(0, "Viable Cells"));
        Assert.assertEquals("84.5%", table.getDataAsText(0, "Viability"));
        Assert.assertEquals("6.000E7", table.getDataAsText(0, "Original Cells"));

        Assert.assertEquals("foobar,vial1,vial2,vial3", table.getDataAsText(0, "Specimen IDs"));
        if (isGroupConcatSupported())
            Assert.assertEquals("vial1,vial2,vial3", table.getDataAsText(0, "SpecimenMatches"));
        else
            Assert.assertEquals("", table.getDataAsText(0, "SpecimenMatches"));
        Assert.assertEquals("4", table.getDataAsText(0, "SpecimenCount"));
        Assert.assertEquals("3", table.getDataAsText(0, "SpecimenMatchCount"));
        Assert.assertEquals("52.11%", table.getDataAsText(0, "Recovery"));
        Assert.assertEquals("true", table.getDataAsText(0, "Unreliable?"));
        Assert.assertEquals("300", table.getDataAsText(0, "IntValue"));

        Assert.assertEquals("vial1", table.getDataAsText(1, "Specimen IDs"));
        Assert.assertEquals("1", table.getDataAsText(1, "SpecimenCount"));
        Assert.assertEquals("1", table.getDataAsText(1, "SpecimenMatchCount"));
        Assert.assertEquals("115.67%", table.getDataAsText(1, "Recovery"));

        Assert.assertEquals("161400006", table.getDataAsText(2, "Participant ID"));
        Assert.assertEquals("5.0", table.getDataAsText(2, "Visit ID"));
        Assert.assertEquals("161400006.10-5", table.getDataAsText(2, "Pool ID"));
        Assert.assertEquals("vial2", table.getDataAsText(2, "Specimen IDs"));
        Assert.assertEquals("1", table.getDataAsText(2, "SpecimenCount"));
        Assert.assertEquals("1", table.getDataAsText(2, "SpecimenMatchCount"));
        Assert.assertEquals("105.78%", table.getDataAsText(2, "Recovery"));

        Assert.assertEquals("161400006", table.getDataAsText(3, "Participant ID"));
        Assert.assertEquals("5.0", table.getDataAsText(3, "Visit ID"));
        Assert.assertEquals("161400006.11-5", table.getDataAsText(3, "Pool ID"));

        Assert.assertEquals("xyzzy", table.getDataAsText(4, "Specimen IDs"));
        Assert.assertEquals("1", table.getDataAsText(4, "SpecimenCount"));
        Assert.assertEquals("0", table.getDataAsText(4, "SpecimenMatchCount"));
        Assert.assertEquals("", table.getDataAsText(4, "Recovery"));

        Assert.assertEquals("", table.getDataAsText(5, "Specimen IDs"));
        Assert.assertEquals("0", table.getDataAsText(5, "SpecimenCount"));
        Assert.assertEquals("", table.getDataAsText(5, "SpecimenMatchCount"));
        Assert.assertEquals("", table.getDataAsText(5, "Recovery"));
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

        clickNavButton("Save and Finish", 0);
        String actualConfirmation = getConfirmationAndWait();
        log("** Got confirmation: " + actualConfirmation);

        log(".. checking re-runs are placed in the same Run Group");
        DataRegionTable runsTable = new DataRegionTable(getAssayName() + " Runs", this);
        Assert.assertEquals(2, runsTable.getDataRowCount());
        String runGroupName = runsTable.getDataAsText(0, "Run Groups");
        Assert.assertEquals(runGroupName, runsTable.getDataAsText(1, "Run Groups"));
        runsTable.clickLink(0, "Run Groups");

        // Run Group name should be "Assay Name-XXX" where XXX is the run group rowid
        String runGroupRowId = getUrlParam(selenium.getLocation(), "rowId", true);
        Assert.assertEquals(getAssayName() + "-" + runGroupRowId, runGroupName);
        assertTextPresent("Re-importing any Viability run in this run group will place the new run in this same run group.");
        runsTable = new DataRegionTable(getAssayName() + " Runs", this);
        Assert.assertEquals(2, runsTable.getDataRowCount());

        log(".. checking appropriate fields are copied in re-run");
        clickLinkWithText(runName);

        DataRegionTable dataTable = new DataRegionTable(getAssayName() + " Data", this);
        Assert.assertEquals("", dataTable.getDataAsText(0, "Unreliable?"));
        Assert.assertEquals("300", dataTable.getDataAsText(0, "IntValue"));
    }

    protected void runResultSpecimenLookupTest()
    {
        log("** Checking ResultSpecimens lookups");
        beginAt("/query/" + getProjectName() + "/" + getFolderName() + "/executeQuery.view?schemaName=assay&query.queryName=" + getAssayName() + " ResultSpecimens");
        assertTextPresent("foobar", "vial1", "xyzzy", "160450533-5", "161400006.11-5");

        setSelectedFields("/" + getProjectName() + "/" + getFolderName(), "assay", getAssayName() + " ResultSpecimens", null,
                new String[] { "ResultID", "ResultID/Recovery", "Specimen", "SpecimenIndex", "SpecimenID/Volume", "SpecimenID/Specimen/VolumeUnits"});
        assertTextNotPresent("foobar");
        assertTextPresent("161400006.11-5", "105.78%", "20,000,000.0", "CEL");
    }

    protected void runTransformTest()
    {
        // add the transform script to the assay
        log("** Uploading Viability Runs with a transform script");

        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickLinkWithText(getAssayName());
        click(Locator.linkWithText("manage assay design"));
        clickLinkWithText("edit assay design", false);
        getConfirmationAndWait();

        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), "/sampledata/qc/transform.jar"), 0);
        clickNavButton("Save & Close");

        final String runName = "transformed assayId";
        uploadViabilityRun("/sampledata/viability/small.VIA.csv", runName, false);

        log("** Check form field values");
        assertFormElementEquals("_pool_1604505335_0_ParticipantID", "160450533");
        assertFormElementEquals("_pool_1604505335_0_VisitID", "5.0");
        assertFormElementEquals("_pool_1604505335_0_TotalCells", "3.700E7");
        assertFormElementEquals("_pool_1604505335_0_ViableCells", "3.127E7");
        assertFormElementEquals("_pool_1604505335_0_Viability", "84.5%");

        log("** Insert specimen IDs");
        addSpecimenIds("_pool_1604505335_0_SpecimenIDs", "vial2", "vial3", "vial1", "foobar");
        addSpecimenIds("_pool_1594020325_1_SpecimenIDs", "vial1");
        addSpecimenIds("_pool_161400006105_2_SpecimenIDs", "vial2");
        addSpecimenIds("_pool_161400006115_3_SpecimenIDs", "vial3");
        addSpecimenIds("_pool_1614016435_4_SpecimenIDs", "xyzzy");

        clickNavButton("Save and Finish", 0);
        String expectConfirmation = "Some values are missing for the following pools:\n\n" +
                "  Sample number 33: SpecimenIDs\n" +
                "  Sample number 34: SpecimenIDs\n\n" +
                "Save anyway?";
        String actualConfirmation = getConfirmationAndWait();
        log("** Got confirmation: " + actualConfirmation);

        // verify the description error was generated by the transform script
        clickLinkWithText("transformed assayId");

        DataRegionTable table = new DataRegionTable(getAssayName() + " Data", this);
        List<String> specimenIDColumnValues = table.getColumnDataAsText("Specimen IDs");
        for (String s : specimenIDColumnValues)
        {
            Assert.assertTrue("Specimen not Transformed", s.equals("Transformed"));
        }
    }

    protected void runTargetStudyTest()
    {
        log("** Create Study2");
        createSubfolder(getProjectName(), getProjectName(), STUDY2_NAME, "Study", null, true);
        clickNavButton("Create Study");
        clickNavButton("Create Study");

        log("** Import specimens2");
        // create a 'xyzzy' vial id
        importSpecimens(STUDY2_NAME, "/sampledata/viability/specimens2.txt");

        log("** Test Target Study as Result Domain Field");

        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickLinkWithText(getAssayName());
        click(Locator.linkWithText("manage assay design"));
        clickLinkWithText("edit assay design", false);
        getConfirmationAndWait();
        waitForElement(Locator.navButton("Add Script"));

        // remove TargetStudy field from the Batch domain and add it to the Result domain.
        deleteField("Batch Fields", 0);
        addField("Result Fields", 13, "TargetStudy", "Target Study", ListHelper.ListColumnType.String);
        clickNavButton("Save & Close");

        clickLinkWithText(getProjectName());
        clickLinkWithText(getFolderName());
        clickLinkWithText(getAssayName());
        clickNavButton("Import Data");

        final String runName = "result-level target study";
        uploadViabilityRun("/sampledata/viability/small.VIA.csv", runName, false);

        log("** Test 'same' checkbox for TargetStudy");
        String targetStudyOptionText = "/" + getProjectName() + "/" + getFolderName() + " (" + getFolderName() + " Study)";
        selectOptionByText("_pool_1604505335_0_TargetStudy", targetStudyOptionText);
        Assert.assertEquals("[None]", getSelectedOptionText("_pool_1594020325_1_TargetStudy"));
        clickCheckboxById("_pool_1604505335_0_TargetStudyCheckBox");
        assertOptionEquals("_pool_1594020325_1_TargetStudy", targetStudyOptionText);
        assertOptionEquals("_pool_161400006115_3_TargetStudy", targetStudyOptionText);
        clickCheckboxById("_pool_1604505335_0_TargetStudyCheckBox");
        
        // clear TargetStudy for 'vial2' and set the TargetStudy for 'vial3' and 'xyzzy'
        selectOptionByText("_pool_161400006105_2_TargetStudy", "[None]");
        selectOptionByText("_pool_161400006115_3_TargetStudy", "/" + getProjectName() + "/" + STUDY2_NAME + " (" + STUDY2_NAME + " Study)");
        selectOptionByText("_pool_1614016435_4_TargetStudy", "/" + getProjectName() + "/" + STUDY2_NAME + " (" + STUDY2_NAME + " Study)");

        log("** Insert specimen IDs");
        addSpecimenIds("_pool_1604505335_0_SpecimenIDs", "vial2", "vial3", "vial1", "foobar");
        addSpecimenIds("_pool_1594020325_1_SpecimenIDs", "vial1");
        addSpecimenIds("_pool_161400006105_2_SpecimenIDs", "vial2");
        addSpecimenIds("_pool_161400006115_3_SpecimenIDs", "vial3");
        addSpecimenIds("_pool_1614016435_4_SpecimenIDs", "xyzzy");

        clickNavButton("Save and Finish", 0);
        String expectConfirmation = "Some values are missing for the following pools:\n\n" +
                "  Sample number 33: SpecimenIDs\n" +
                "  Sample number 34: SpecimenIDs\n\n" +
                "Save anyway?";
        String actualConfirmation = getConfirmationAndWait();
        log("** Got confirmation: " + actualConfirmation);

        //TODO: uncomment once Issue 10054 is resolved.
        //Assert.assertEquals(expectConfirmation, actualConfirmation);

        clickLinkWithText(runName);

        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "TargetStudy", "Target Study");
        CustomizeViewsHelper.saveDefaultView(this);

        DataRegionTable table = new DataRegionTable(getAssayName() + " Data", this);
        Assert.assertEquals("foobar,vial1,vial2,vial3", table.getDataAsText(0, "Specimen IDs"));
        Assert.assertEquals("4", table.getDataAsText(0, "SpecimenCount"));
        Assert.assertEquals("3", table.getDataAsText(0, "SpecimenMatchCount"));
        Assert.assertEquals("52.11%", table.getDataAsText(0, "Recovery"));
        Assert.assertEquals(getFolderName() + " Study", table.getDataAsText(0, "TargetStudy"));

        Assert.assertEquals("vial2", table.getDataAsText(2, "Specimen IDs"));
        Assert.assertEquals("1", table.getDataAsText(2, "SpecimenCount"));
        Assert.assertEquals("0", table.getDataAsText(2, "SpecimenMatchCount"));
        Assert.assertEquals("", table.getDataAsText(2, "Recovery"));
        Assert.assertEquals("", table.getDataAsText(2, "TargetStudy"));

        Assert.assertEquals("vial3", table.getDataAsText(3, "Specimen IDs"));
        Assert.assertEquals("1", table.getDataAsText(3, "SpecimenCount"));
        Assert.assertEquals("0", table.getDataAsText(3, "SpecimenMatchCount"));
        Assert.assertEquals("", table.getDataAsText(3, "Recovery"));
        Assert.assertEquals(STUDY2_NAME + " Study", table.getDataAsText(3, "TargetStudy"));

        Assert.assertEquals("xyzzy", table.getDataAsText(4, "Specimen IDs"));
        Assert.assertEquals("1", table.getDataAsText(4, "SpecimenCount"));
        Assert.assertEquals("1", table.getDataAsText(4, "SpecimenMatchCount"));
        Assert.assertEquals("88.88%", table.getDataAsText(4, "Recovery"));
        Assert.assertEquals(STUDY2_NAME + " Study", table.getDataAsText(4, "TargetStudy"));

        // UNDONE: participant/visit resolver test
        // UNDONE: copy-to-study
    }
}
