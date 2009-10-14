/*
 * Copyright (c) 2007-2009 LabKey Corporation
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
import org.labkey.test.util.DataRegionTable;

import java.io.File;

/**
 * User: kevink
 * Date: Sep 30, 2009
 */
public class ViabilityTest extends AbstractAssayTest
{
    public static final String PROJECT_NAME = "\u2603 Viability ?";
    public static final String FOLDER_NAME = "Viability Folder";
    private static final String ASSAY_NAME = "\u262D Guava Assay";


    public String getAssociatedModuleDirectory()
    {
        return "viability";
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

    protected void doTestSteps() throws Exception
    {
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
        clickLinkWithText("Manage Assays");
        clickNavButton("New Assay Design");
        checkRadioButton("providerName", "Viability");
        clickNavButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_GWT);

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

        setFormElement("uploadedFile", new File(getLabKeyRoot() + "/sampledata/viability/small.VIA.CSV"));
        clickNavButton("Next");

        log("** Check form field values");
        assertFormElementEquals("_pool_1604505335_0_ParticipantID", "160450533");
        assertFormElementEquals("_pool_1604505335_0_VisitID", "5.0");
        assertFormElementEquals("_pool_1604505335_0_TotalCells", "3.700E7");
        assertFormElementEquals("_pool_1604505335_0_ViableCells", "3.127E7");
        assertFormElementEquals("_pool_1604505335_0_Viability", "84.5%");

        log("** Insert specimen IDs");
        addSpecimenIds("_pool_1604505335_0_SpecimenIDs", "vial1", "vial2", "vial3");
        addSpecimenIds("_pool_1594020325_1_SpecimenIDs", "vial1");
        addSpecimenIds("_pool_1614000065_2_SpecimenIDs", "vial2");
        addSpecimenIds("_pool_1614016435_3_SpecimenIDs", "xyzzy"); // specimen doesn't exist

        clickNavButton("Save and Finish");
        // XXX: should say "sample number 33" but checkRunUploadForm() doesn't work under selenium for some reason
        String expectConfirmation = "Missing SpecimenIDs value for sample number 1.  Save anyway?";
        String actualConfirmation = selenium.getConfirmation();
        log("** Got confirmation: " + actualConfirmation);
        assertEquals(expectConfirmation, actualConfirmation);

        clickLinkWithText("small.VIA.CSV"); // run name
        DataRegionTable table = new DataRegionTable(ASSAY_NAME + " Data", this);
        assertEquals("small.VIA.CSV", table.getDataAsText(0, "Run"));
        assertEquals("160450533", table.getDataAsText(0, "Participant ID"));
        assertEquals("5.0", table.getDataAsText(0, "Visit ID"));
        assertEquals("160450533-5", table.getDataAsText(0, "Pool ID"));
        assertEquals("3.700E7", table.getDataAsText(0, "Total Cells"));
        assertEquals("3.127E7", table.getDataAsText(0, "Viable Cells"));
        assertEquals("84.5%", table.getDataAsText(0, "Viability"));
        assertEquals("6.000E7", table.getDataAsText(0, "Original Cells"));
        assertEquals("3", table.getDataAsText(0, "Specimen ID Count"));

        assertEquals("vial1,vial2,vial3", table.getDataAsText(0, "Specimen IDs"));
        assertEquals("3", table.getDataAsText(0, "SpecimenIDCount"));
        assertEquals("52.11%", table.getDataAsText(0, "Recovery"));

        assertEquals("vial1", table.getDataAsText(1, "Specimen IDs"));
        assertEquals("115.67%", table.getDataAsText(1, "Recovery"));

        assertEquals("vial2", table.getDataAsText(2, "Specimen IDs"));
        assertEquals("105.78%", table.getDataAsText(2, "Recovery"));

        assertEquals("xyzzy", table.getDataAsText(3, "Specimen IDs"));
        assertEquals("", table.getDataAsText(3, "Recovery"));
        
        assertEquals("", table.getDataAsText(4, "Specimen IDs"));
        assertEquals("", table.getDataAsText(4, "Recovery"));

//        beginAt("/query/" + PROJECT_NAME + "/" + FOLDER_NAME + "/executeQuery.view?schema=assay&query.queryName=" + ASSAY_NAME + " ResultSpecimens");
//        setSelectedFields("/" + PROJECT_NAME + "/" + FOLDER_NAME, "assay", ASSAY_NAME + " ResultSpecimens", null, new String[] { "Result", "Specimen", "SpecimenIndex", "Specimen/Volume", "Result/Recovert" });

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
        selenium.keyPress(xpath, "\\9"); // press tab
    }
}
