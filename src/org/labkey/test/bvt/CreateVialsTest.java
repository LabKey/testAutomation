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

package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;

/**
 * User: kevink
 * Date: Feb 23, 2011
 */
public class CreateVialsTest extends AbstractViabilityTest
{
    public static final String PROJECT_NAME = "CreateVialsTest";
    public static final String FOLDER_NAME = "Viability Folder";
    private static final String ASSAY_NAME = "Guava Assay";

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "customModules/letvin";
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

    @Override
    protected void doCleanup() throws Exception
    {
        try
        {
            deleteProject(getProjectName());
        }
        catch(Throwable T) {}

        deleteDir(getTestTempDir());
    }

    @Override
    protected void runUITests() throws Exception
    {
        // Create study with 'Letvin' module activated
        initializeStudyFolder("Letvin");
        createViabilityAssay();
        setupPipeline();

        
        log("** Upload run without a TargetStudy set and try to create vials.");
        uploadViabilityRun("/sampledata/viability/122810.EP5.CSV", "run1", false);
        clickButton("Save and Finish");
        String confirmation = getConfirmationAndWait();
        clickLinkContainingText("run1");

        DataRegionTable table = new DataRegionTable(getAssayName() + " Data", this);
        table.checkAllOnPage();
        clickButton("Create Vials", 0);
        assertExtMsgBox("Error", "ParticipantID 'B01' missing TargetStudy.");
        clickButton("OK", 0);

        // Delete run
        clickLinkWithText("view runs");
        checkAllOnPage(getAssayName() + " Runs");
        clickButton("Delete"); clickButton("Confirm Delete");


        log("** Upload run again but this time set a TargetStudy, visit ids, and a single specimen id on the first row");
        uploadViabilityRun("/sampledata/viability/122810.EP5.CSV", "run2", true);
        setFormElement("_pool_B01_0_VisitID", "1.0");
        clickCheckboxById("_pool_B01_0_VisitIDCheckBox");
        addSpecimenIds("_pool_B01_0_SpecimenIDs", "vial1");
        clickButton("Save and Finish");
        confirmation = getConfirmationAndWait();

        clickLinkContainingText("run2");
        table = new DataRegionTable(getAssayName() + " Data", this);

        table.checkAllOnPage();
        clickButton("Create Vials", 0);
        assertExtMsgBox("Error", "ParticipantID 'B01' has SpecimenIDs.");
        clickButton("OK", 0);

        // uncheck the row with the specimen id and go to create vials page.
        table.uncheckCheckbox(0);
        clickButton("Create Vials");
        assertTextNotPresent("B01");
        assertTextPresent("B02");


        log("** test changing total cell counts updates vial count column");
        table = new DataRegionTable(getAssayName() + " Data", this);
        assertEquals("2", table.getDataAsText(0, "Vial Count"));

        setFormElement(Locator.name("totalCells", 0), "10000000");
        fireEvent(Locator.name("totalCells", 0), BaseSeleniumWebTest.SeleniumEvent.blur);
        assertEquals("1", table.getDataAsText(0, "Vial Count"));

        setFormElement(Locator.name("totalCells", 0), "40000000");
        fireEvent(Locator.name("totalCells", 0), BaseSeleniumWebTest.SeleniumEvent.blur);
        assertEquals("2", table.getDataAsText(0, "Vial Count"));

        setFormElement(Locator.name("totalCells", 0), "50000000");
        fireEvent(Locator.name("totalCells", 0), BaseSeleniumWebTest.SeleniumEvent.blur);
        assertEquals("3", table.getDataAsText(0, "Vial Count"));

        clickButton("Save");

        table = new DataRegionTable(getAssayName() + " Data", this);
        assertEquals("5.000E7", table.getDataAsText(1, "Original Cells"));
        assertEquals("3", table.getDataAsText(1, "Specimen Count"));


        // Uncomment when 11574 is fixed
        //log("** Copy to study");
        //clickLinkWithText("view runs");
        //selenium.click(".toggle");
        //clickButton("Copy to Study");
        //clickButton("Next");
        //assertTitleContains("Copy to " + getFolderName() + " Study");
        //// UNDONE: assert first row has no specimen match
        //clickButton("Copy to Study");
        //assertTextPresent("B02");
    }

}
