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
    protected void initializeStudyFolder(String... tabs)
    {
        super.initializeStudyFolder(tabs);

        log("** Adding new Sites to check null ExternalId (Issue 12074)");
        clickLinkContainingText("Manage Labs/Sites");
        setFormElement("newId", "100");
        setFormElement("newLabel", "Alice Lab");
        clickButton("Save");

        setFormElement("newId", "200");
        setFormElement("newLabel", "Bob's Lab");
        clickButton("Save");

        beginAt("/query/" + getProjectName() + "/" + getFolderName() + "/executeQuery.view?schemaName=study&query.queryName=Site&query.sort=RowId");
        DataRegionTable table = new DataRegionTable("query", this, false);
        assertEquals(2, table.getDataRowCount());
        assertEquals("Alice Lab", table.getDataAsText(0, "Label"));
        assertEquals("", table.getDataAsText(0, "External Id"));
        assertEquals("Bob's Lab", table.getDataAsText(1, "Label"));
        assertEquals("", table.getDataAsText(1, "External Id"));
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
        clickButton("Create Vials", 0);
        ExtHelper.waitForExtDialog(this, "Create Vials");

        // Check for 'Sally Lab' in ComboBox
        click(Locator.xpath("//input[@name='defaultLocationField']/../img"));
        waitForElement(Locator.xpath("//div[contains(@class, 'x-combo-list-item') and text()='Alice Lab']"), WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath("//div[contains(@class, 'x-combo-list-item') and text()='Alice Lab']"));
        assertFormElementEquals("defaultLocationField", "Alice Lab");

        setFormElement("maxCellPerVialField", "19e6");
        setFormElement("defaultLocationField", "Site A");
        //String btnId = selenium.getEval("this.browserbot.getCurrentWindow().Ext.MessageBox.getDialog().buttons[1].getId();");
        //click(Locator.id(btnId));
        clickButton("Create Vials");
        assertTextPresent("Each vial will have no more than 1.90e+07 cells.");
        assertTextNotPresent("B01");


        log("** test changing total cell counts updates vial count column");
        table = new DataRegionTable(getAssayName() + " Data", this, false);
        assertEquals("B02", table.getDataAsText(0, "Participant ID"));
        assertEquals(getFolderName() + " Study", table.getDataAsText(0, "Run Batch Target Study"));
        assertEquals("2", table.getDataAsText(0, "Vial Count"));
        assertEquals("Site A", getFormElement(Locator.name("siteLabel", 0)));
        setFormElement(Locator.name("siteLabel", 0), "Bob's Lab");

        setFormElement(Locator.name("viableCells", 0), "10000000");
        fireEvent(Locator.name("viableCells", 0), BaseSeleniumWebTest.SeleniumEvent.change);
        assertEquals("1", table.getDataAsText(0, "Vial Count"));

        setFormElement(Locator.name("viableCells", 0), "19000000");
        fireEvent(Locator.name("viableCells", 0), BaseSeleniumWebTest.SeleniumEvent.change);
        assertEquals("1", table.getDataAsText(0, "Vial Count"));

        setFormElement(Locator.name("viableCells", 0), "19000001");
        fireEvent(Locator.name("viableCells", 0), BaseSeleniumWebTest.SeleniumEvent.change);
        assertEquals("2", table.getDataAsText(0, "Vial Count"));

        setFormElement(Locator.name("viableCells", 0), "50000000");
        fireEvent(Locator.name("viableCells", 0), BaseSeleniumWebTest.SeleniumEvent.change);
        assertEquals("3", table.getDataAsText(0, "Vial Count"));

        pressTab(Locator.name("viableCells", 0).toString());

        clickButton("Save");

        log("** checking cell counts and specimen IDs");
        table = new DataRegionTable(getAssayName() + " Data", this);
        assertEquals("B02", table.getDataAsText(1, "Participant ID"));
        assertEquals("B02_1.0_0,B02_1.0_1,B02_1.0_2", table.getDataAsText(1, "Specimen IDs"));
        // Viable Cells value doesn't get updated, only Original Cells
        assertEquals("2.050E7", table.getDataAsText(1, "Viable Cells"));
        assertEquals("5.000E7", table.getDataAsText(1, "Original Cells"));
        assertEquals("3", table.getDataAsText(1, "Specimen Count"));

        assertEquals("B03", table.getDataAsText(2, "Participant ID"));
        assertEquals("B03_1.0_0,B03_1.0_1", table.getDataAsText(2, "Specimen IDs"));
        assertEquals("2.270E7", table.getDataAsText(2, "Viable Cells"));
        assertEquals("2.270E7", table.getDataAsText(2, "Original Cells"));
        assertEquals("2", table.getDataAsText(2, "Specimen Count"));

        
        log("** checking new site 'Site A' was added and no duplicate 'Bob's Lab' exist (Issue 12074)");
        pushLocation();
        beginAt("/query/" + getProjectName() + "/" + getFolderName() + "/executeQuery.view?schemaName=study&query.queryName=Site&query.sort=RowId");
        table = new DataRegionTable("query", this, false);
        assertEquals(4, table.getDataRowCount());
        assertEquals("Alice Lab", table.getDataAsText(0, "Label"));
        assertEquals("-1", table.getDataAsText(0, "External Id"));
        assertEquals("Bob's Lab", table.getDataAsText(1, "Label"));
        assertEquals("-2", table.getDataAsText(1, "External Id"));
        assertEquals("Not Specified", table.getDataAsText(2, "Label"));
        assertEquals("1", table.getDataAsText(2, "External Id"));
        assertEquals("Site A", table.getDataAsText(3, "Label"));
        assertEquals("2", table.getDataAsText(3, "External Id"));
        popLocation();


        log("** checking site and cells per vial");
        pushLocation();
        beginAt("/study-samples/" + getProjectName() + "/" + getFolderName() + "/samples.view?showVials=true");
        table = new DataRegionTable("SpecimenDetail", this);
        assertEquals("B02_1.0_0", table.getDataAsText(0, "Global Unique Id"));
        assertEquals("16,666,666.0", table.getDataAsText(0, "Volume"));
        assertEquals("Bob's Lab", table.getDataAsText(0, "Site Name"));

        assertEquals("B02_1.0_1", table.getDataAsText(1, "Global Unique Id"));
        assertEquals("16,666,666.0", table.getDataAsText(1, "Volume"));
        assertEquals("Bob's Lab", table.getDataAsText(1, "Site Name"));

        assertEquals("B02_1.0_2", table.getDataAsText(2, "Global Unique Id"));
        assertEquals("16,666,666.0", table.getDataAsText(2, "Volume"));
        assertEquals("Bob's Lab", table.getDataAsText(2, "Site Name"));

        assertEquals("B03_1.0_0", table.getDataAsText(3, "Global Unique Id"));
        assertEquals("11,349,723.0", table.getDataAsText(3, "Volume"));
        assertEquals("Site A", table.getDataAsText(3, "Site Name"));

        assertEquals("B03_1.0_1", table.getDataAsText(4, "Global Unique Id"));
        assertEquals("11,349,723.0", table.getDataAsText(4, "Volume"));
        assertEquals("Site A", table.getDataAsText(4, "Site Name"));


        log("** move two vials to 'Site C'");
        table.checkCheckbox(1);
        table.checkCheckbox(3);
        clickButton("Move Vials", 0);
        ExtHelper.waitForExtDialog(this, "Move Vials");
        setFormElement("changeLocationField", "Site C");
        clickButton("Move Vials", WAIT_FOR_JAVASCRIPT);

        assertEquals("B02_1.0_1", table.getDataAsText(1, "Global Unique Id"));
        assertEquals("Site C", table.getDataAsText(1, "Site Name"));

        assertEquals("B02_1.0_2", table.getDataAsText(2, "Global Unique Id"));
        assertEquals("Bob's Lab", table.getDataAsText(2, "Site Name"));
        
        assertEquals("B03_1.0_0", table.getDataAsText(3, "Global Unique Id"));
        assertEquals("Site C", table.getDataAsText(3, "Site Name"));
        popLocation();
        

        log("** Copy to study");
        clickLinkWithText("view runs");
        selenium.click(".toggle");
        clickButton("Copy to Study");
        clickButton("Next");
        assertTitleContains("Copy to " + getFolderName() + " Study");
        // UNDONE: assert first row has no specimen match
        clickButton("Copy to Study");
        assertTextPresent("B02");
        
    }

}
