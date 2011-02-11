/*
 * Copyright (c) 2009-2011 LabKey Corporation
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

package org.labkey.test.bvt;

import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;

import java.io.File;

/**
 * User: kevink
 * Date: Sep 30, 2009
 */
public class ViabilityTest extends AbstractQCAssayTest
{
    public static final String PROJECT_NAME = "Viability";
    public static final String FOLDER_NAME = "Viability Folder";
    private static final String ASSAY_NAME = "Guava Assay";
    private static final String STUDY2_NAME = "Study2 Folder";


    public String getAssociatedModuleDirectory()
    {
        return "server/modules/viability";
    }

    @Override
    protected boolean isDatabaseSupported(DatabaseInfo info)
    {
        return info.productName.equals("PostgreSQL") ||
               (info.productName.equals("Microsoft SQL Server") && !info.productVersion.startsWith("08.00"));
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected String getFolderName()
    {
        return FOLDER_NAME;
    }

    protected void doCleanup() throws Exception
    {
        try
        {
            deleteProject(getProjectName());
            deleteEngine();
        }
        catch(Throwable T) {}

        deleteDir(getTestTempDir());
    }

    protected void initializeFolder()
    {
        if (!isLinkPresentWithText(getProjectName()))
            createProject(getProjectName());
        createSubfolder(getProjectName(), getProjectName(), getFolderName(), "Study", null, true);
    }

    protected void runUITests() throws Exception
    {
        // setup a scripting engine to run a java transform script
        prepareProgrammaticQC();

        log("** Create Study");
        initializeFolder();
        clickNavButton("Create Study");
        clickNavButton("Create Study");

        log("** Import specimens");
        clickLinkWithText(getFolderName());
        clickLinkWithText("By Specimen");
        clickNavButton("Import Specimens");
        setLongTextField("tsv", getFileContents("/sampledata/viability/specimens.txt"));
        submit();

        log("** Create viability assay");
        clickLinkWithText(getFolderName());
        addWebPart("Assay List");
        clickNavButton("Manage Assays");
        clickNavButton("New Assay Design");
        checkRadioButton("providerName", "Viability");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        selenium.type("//input[@id='AssayDesignerName']", ASSAY_NAME);

        sleep(1000);
        clickNavButton("Save", 0);
        waitForText("Save successful.", 20000);

        log("** Setting pipeline root");
        setupPipeline(getProjectName());

        log("** Upload guava run");
        clickLinkWithText(getFolderName());
        clickLinkWithText(ASSAY_NAME);
        clickNavButton("Import Data");
        selectOptionByText("targetStudy", "/" + getProjectName() + "/" + getFolderName() + " (" + getFolderName() + " Study)");
        clickNavButton("Next");

        File guavaFile = new File(getLabKeyRoot() + "/sampledata/viability/small.VIA.csv");
        assertTrue("Upload file doesn't exist: " + guavaFile, guavaFile.exists());
        setFormElement("__primaryFile__", guavaFile);
        clickNavButton("Next", 8000);

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

        //TODO: uncomment once Issue 10054 is resolved.
        //assertEquals(expectConfirmation, actualConfirmation);

        setSelectedFields("/" + PROJECT_NAME + "/" + FOLDER_NAME, "assay", ASSAY_NAME + " Data", null,
                new String[] { "Run", "ParticipantID", "VisitID", "PoolID",
                        "TotalCells", "ViableCells", "Viability", "OriginalCells", "Recovery",
                        "SpecimenIDs", "SpecimenCount", "SpecimenMatchCount", "SpecimenMatches"});

        clickLinkWithText("small.VIA.csv"); // run name
        DataRegionTable table = new DataRegionTable(ASSAY_NAME + " Data", this);
        assertEquals("small.VIA.csv", table.getDataAsText(0, "Run"));
        assertEquals("160450533", table.getDataAsText(0, "Participant ID"));
        assertEquals("5.0", table.getDataAsText(0, "Visit ID"));
        assertEquals("160450533-5", table.getDataAsText(0, "Pool ID"));
        assertEquals("3.700E7", table.getDataAsText(0, "Total Cells"));
        assertEquals("3.127E7", table.getDataAsText(0, "Viable Cells"));
        assertEquals("84.5%", table.getDataAsText(0, "Viability"));
        assertEquals("6.000E7", table.getDataAsText(0, "Original Cells"));

        assertEquals("foobar,vial1,vial2,vial3", table.getDataAsText(0, "Specimen IDs"));
        //assertEquals("vial1,vial2,vial3", table.getDataAsText(0, "SpecimenMatches")); // enable when SqlServer supports SpecimenMatches column
        assertEquals("4", table.getDataAsText(0, "SpecimenCount"));
        assertEquals("3", table.getDataAsText(0, "SpecimenMatchCount"));
        assertEquals("52.11%", table.getDataAsText(0, "Recovery"));

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
        assertEquals("", table.getDataAsText(4, "SpecimenMatchCount"));
        assertEquals("", table.getDataAsText(4, "Recovery"));
        
        assertEquals("", table.getDataAsText(5, "Specimen IDs"));
        assertEquals("0", table.getDataAsText(5, "SpecimenCount"));
        assertEquals("", table.getDataAsText(5, "SpecimenMatchCount"));
        assertEquals("", table.getDataAsText(5, "Recovery"));

        log("** Checking ResultSpecimens lookups");
        beginAt("/query/" + PROJECT_NAME + "/" + FOLDER_NAME + "/executeQuery.view?schemaName=assay&query.queryName=" + ASSAY_NAME + " ResultSpecimens");
        assertTextPresent("foobar", "vial1", "xyzzy", "160450533-5", "161400006.11-5");

        setSelectedFields("/" + PROJECT_NAME + "/" + FOLDER_NAME, "assay", ASSAY_NAME + " ResultSpecimens", null,
                new String[] { "ResultID", "ResultID/Recovery", "Specimen", "SpecimenIndex", "SpecimenID/Volume", "SpecimenID/Specimen/VolumeUnits"});
        assertTextNotPresent("foobar");
        assertTextPresent("161400006.11-5", "105.78%", "20,000,000.0", "CEL");

        runTargetStudyTest();
        runTransformTest();

    }

    public void addSpecimenIds(String id, String... values)
    {
        for (int i = 0; i < values.length; i++)
        {
            String value = values[i];
            addSpecimenId(id, value, i+1);
        }
    }

    public void addSpecimenId(String id, String value, int index)
    {
        String xpath = "//input[@name='" + id + "'][" + index + "]";
        setFormElement(xpath, value);
        pressTab(xpath);
    }

    protected void runTransformTest()
    {
        // add the transform script to the assay
        log("Uploading Viability Runs with a transform script");

        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText(ASSAY_NAME);
        click(Locator.linkWithText("manage assay design"));
        clickLinkWithText("edit assay design", false);
        getConfirmationAndWait();
        waitForElement(Locator.xpath("//input[@id='AssayDesignerTransformScript']"), WAIT_FOR_JAVASCRIPT);

        addTransformScript(new File(WebTestHelper.getLabKeyRoot(), "/sampledata/qc/transform.jar"));
        clickNavButton("Save & Close");

        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText(ASSAY_NAME);
        clickNavButton("Import Data");

        setFormElement("name", "transformed assayId");
        File guavaFile = new File(getLabKeyRoot() + "/sampledata/viability/small.VIA.csv");
        assertTrue("Upload file doesn't exist: " + guavaFile, guavaFile.exists());
        setFormElement("__primaryFile__", guavaFile);
        clickNavButton("Next", 8000);

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
        //TODO: uncomment once Issue 10054 is resolved.
        //assertEquals(expectConfirmation, actualConfirmation);

        // verify the description error was generated by the transform script
        clickLinkWithText("transformed assayId");
        for(int i = 2; i <= 7; i++)
        {
            assertTableCellTextEquals("dataregion_" + ASSAY_NAME + " Data",  i, "Specimen IDs", "Transformed");
        }
    }

    protected void runTargetStudyTest()
    {
        log("** Create Study2");
        createSubfolder(getProjectName(), getProjectName(), STUDY2_NAME, "Study", null, true);
        clickNavButton("Create Study");
        clickNavButton("Create Study");

        log("** Import specimens2");
        clickLinkWithText(STUDY2_NAME);
        clickLinkWithText("By Specimen");
        clickNavButton("Import Specimens");
        // create a 'xyzzy' vial id
        setLongTextField("tsv", getFileContents("/sampledata/viability/specimens2.txt"));
        submit();

        log("** Test Target Study as Result Domain Field");

        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText(ASSAY_NAME);
        click(Locator.linkWithText("manage assay design"));
        clickLinkWithText("edit assay design", false);
        getConfirmationAndWait();
        waitForElement(Locator.xpath("//input[@id='AssayDesignerTransformScript']"), WAIT_FOR_JAVASCRIPT);

        // remove TargetStudy field from the Batch domain and add it to the Result domain.
        deleteField("Batch Fields", 0);
        addField("Result Fields", 11, "TargetStudy", "Target Study", ListHelper.ListColumnType.String);
        clickNavButton("Save & Close");

        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(FOLDER_NAME);
        clickLinkWithText(ASSAY_NAME);
        clickNavButton("Import Data");

        log("** Upload guava run");
        clickLinkWithText(getFolderName());
        clickLinkWithText(ASSAY_NAME);
        clickNavButton("Import Data");
        assertTextNotPresent("Target Study");

        File guavaFile = new File(getLabKeyRoot() + "/sampledata/viability/small.VIA.csv");
        assertTrue("Upload file doesn't exist: " + guavaFile, guavaFile.exists());
        setFormElement("__primaryFile__", guavaFile);
        clickNavButton("Next", 8000);

        log("** Test 'same' checkbox for TargetStudy");
        String targetStudyOptionText = "/" + getProjectName() + "/" + getFolderName() + " (" + getFolderName() + " Study)";
        selectOptionByText("_pool_1604505335_0_TargetStudy", targetStudyOptionText);
        assertEquals("[None]", getSelectedOptionText("_pool_1594020325_1_TargetStudy"));
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
        //assertEquals(expectConfirmation, actualConfirmation);

        clickLinkWithText("small-1.VIA.csv"); // run name

        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, "TargetStudy", "Target Study");
        CustomizeViewsHelper.saveDefaultView(this);

        DataRegionTable table = new DataRegionTable(ASSAY_NAME + " Data", this);
        assertEquals("foobar,vial1,vial2,vial3", table.getDataAsText(0, "Specimen IDs"));
        assertEquals("4", table.getDataAsText(0, "SpecimenCount"));
        assertEquals("3", table.getDataAsText(0, "SpecimenMatchCount"));
        assertEquals("52.11%", table.getDataAsText(0, "Recovery"));
        assertEquals(FOLDER_NAME + " Study", table.getDataAsText(0, "TargetStudy"));

        assertEquals("vial2", table.getDataAsText(2, "Specimen IDs"));
        assertEquals("1", table.getDataAsText(2, "SpecimenCount"));
        assertEquals("", table.getDataAsText(2, "SpecimenMatchCount"));
        assertEquals("", table.getDataAsText(2, "Recovery"));
        assertEquals("", table.getDataAsText(2, "TargetStudy"));

        assertEquals("vial3", table.getDataAsText(3, "Specimen IDs"));
        assertEquals("1", table.getDataAsText(3, "SpecimenCount"));
        assertEquals("", table.getDataAsText(3, "SpecimenMatchCount"));
        assertEquals("", table.getDataAsText(3, "Recovery"));
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
