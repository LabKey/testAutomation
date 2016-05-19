/*
 * Copyright (c) 2011-2015 LabKey Corporation
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
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Specimen;
import org.labkey.test.util.DataRegionTable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

@Category({DailyB.class, Specimen.class})
public class CreateVialsTest extends AbstractViabilityTest
{
    public static final String PROJECT_NAME = "CreateVialsTest";
    public static final String FOLDER_NAME = "Viability Folder";
    private static final String ASSAY_NAME = "Guava Assay";

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("letvin");
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
    public void validateQueries(boolean validateSubfolders)
    {
        // Disable the Letvin module so the study queries don't cause the query validation to fail.
        _containerHelper.disableModules("Letvin");

        super.validateQueries(true);
    }

    @Override
    protected void initializeStudyFolder(String... tabs)
    {
        super.initializeStudyFolder(tabs);

        clickTab("Manage");
        log("** Adding new Sites to check null ExternalId (Issue 12074)");
        clickAndWait(Locator.linkContainingText("Manage Locations"));
        clickAndWait(Locator.linkContainingText("Insert New"));
        setFormElement(Locator.name("quf_LdmsLabCode"), "100");
        setFormElement(Locator.name("quf_Label"), "Alice Lab");
        clickButton("Submit");

        clickAndWait(Locator.linkContainingText("Insert New"));
        setFormElement(Locator.name("quf_LdmsLabCode"), "200");
        setFormElement(Locator.name("quf_Label"), "Bob's Lab");
        clickButton("Submit");

        beginAt("/query/" + getProjectName() + "/" + getFolderName() + "/executeQuery.view?schemaName=study&query.queryName=Site&query.sort=RowId");
        DataRegionTable table = new DataRegionTable("query", getDriver());
        assertEquals(2, table.getDataRowCount());
        assertEquals("Alice Lab", table.getDataAsText(0, "Label"));
        assertEquals(" ", table.getDataAsText(0, "External Id"));
        assertEquals("Bob's Lab", table.getDataAsText(1, "Label"));
        assertEquals(" ", table.getDataAsText(1, "External Id"));
    }

    @Test
    public void runUITests()
    {
        // Create study with 'Letvin' module activated
        initializeStudyFolder("Letvin");
        createViabilityAssay();
        setupPipeline();

        
        log("** Upload run without a TargetStudy set and try to create vials.");
        uploadViabilityRun("/sampledata/viability/122810.EP5.CSV", "run1", false);
        doAndWaitForPageToLoad(() ->
        {
            clickButton("Save and Finish", 0);
            acceptAlert();
        });
        clickAndWait(Locator.linkContainingText("run1"));

        // Add the hidden 'TargetStudy' column on the results table and the actual 'TargetStudy' column from batch
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addCustomizeViewColumn("TargetStudy");
        _customizeViewsHelper.addCustomizeViewColumn(new String[] { "Run", "Batch", "TargetStudy" }, "BatchTargetStudy");
        _customizeViewsHelper.setColumnProperties("Run/Batch/TargetStudy", "BatchTargetStudy", Collections.emptyList());
        _customizeViewsHelper.saveCustomView();

        DataRegionTable table = new DataRegionTable("Data", getDriver());
        assertEquals(" ", table.getDataAsText(0, "Target Study"));
        assertEquals(" ", table.getDataAsText(0, "BatchTargetStudy"));

        table.checkAllOnPage();
        clickButton("Create Vials", 0);
        assertExtMsgBox("Error", "ParticipantID 'B01' missing TargetStudy.");
        clickButton("OK", 0);

        // Delete run
        clickAndWait(Locator.linkWithText("view runs"));
        new DataRegionTable("Runs", getDriver()).checkAll();
        clickButton("Delete");
        clickButton("Confirm Delete");


        log("** Upload run again but this time set a TargetStudy, visit ids, and a single specimen id on the first row");
        uploadViabilityRun("/sampledata/viability/122810.EP5.CSV", "run2", true);
        setFormElement(Locator.name("_pool_B01_0_VisitID"), "1.0");
        click(Locator.checkboxById("_pool_B01_0_VisitIDCheckBox"));
        addSpecimenIds("_pool_B01_0_SpecimenIDs", "vial1");
        doAndWaitForPageToLoad(() ->
        {
            clickButton("Save and Finish", 0);
            acceptAlert();
        });

        clickAndWait(Locator.linkContainingText("run2"));
        table = new DataRegionTable("Data", getDriver());
        assertEquals(getFolderName() + " Study", table.getDataAsText(0, "Target Study"));
        assertEquals(getFolderName() + " Study", table.getDataAsText(0, "BatchTargetStudy"));

        table.checkAllOnPage();
        clickButton("Create Vials", 0);
        assertExtMsgBox("Error", "ParticipantID 'B01' has SpecimenIDs.");
        clickButton("OK", 0);

        // uncheck the row with the specimen id and go to create vials page.
        table.uncheckCheckbox(0);
        clickButton("Create Vials", 0);
        _extHelper.waitForExtDialog("Create Vials");

        // Check for 'Sally Lab' in ComboBox
        click(Locator.xpath("//input[@name='defaultLocationField']/../img"));
        waitForElement(Locator.xpath("//div[contains(@class, 'x-combo-list-item') and text()='Alice Lab']"), WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath("//div[contains(@class, 'x-combo-list-item') and text()='Alice Lab']"));
        assertEquals("Alice Lab", getFormElement(Locator.name("defaultLocationField")));

        setFormElement(Locator.name("maxCellPerVialField"), "10e6");
        setFormElement(Locator.name("defaultUsedCellsField"), "1e5");
        setFormElement(Locator.name("defaultLocationField"), "Site A");
        _extHelper.clickExtButton("Create Vials");
        assertTextPresent("Each vial will have no more than 1.00e+07 cells.");
        assertElementNotPresent(Locator.tagWithText("td", "B01"));


        log("** test changing cell counts updates used/remaining columns and vial count column");
        table = new DataRegionTable("Data", getDriver());
        assertEquals("B02", table.getDataAsText(0, "Participant ID"));
        assertEquals(getFolderName() + " Study", table.getDataAsText(0, "Target Study"));
        assertEquals("2.050E7", table.getDataAsText(0, "Original Viable Cells"));
        assertEquals("100000", getFormElement(Locator.name("usedCells")));
        assertEquals("20404212", getFormElement(Locator.name("viableCells")));
        assertEquals("3", table.getDataAsText(0, "Vial Count"));
        assertEquals("Site A", getFormElement(Locator.name("siteLabel")));
        setFormElement(Locator.name("siteLabel"), "Bob's Lab");

        setFormElement(Locator.name("usedCells"), "1");
        fireEvent(Locator.name("usedCells"), SeleniumEvent.change);
        assertEquals("20504211", getFormElement(Locator.name("viableCells")));
        assertEquals("3", table.getDataAsText(0, "Vial Count"));

        setFormElement(Locator.name("viableCells"), "10000000");
        fireEvent(Locator.name("viableCells"), SeleniumEvent.change);
        assertEquals("10504212", getFormElement(Locator.name("usedCells")));
        assertEquals("1", table.getDataAsText(0, "Vial Count"));

        setFormElement(Locator.name("viableCells"), "20000000");
        fireEvent(Locator.name("viableCells"), SeleniumEvent.change);
        assertEquals("504212", getFormElement(Locator.name("usedCells")));
        assertEquals("2", table.getDataAsText(0, "Vial Count"));

        setFormElement(Locator.name("viableCells"), "50000000");
        fireEvent(Locator.name("viableCells"), SeleniumEvent.change);
        assertEquals("-29495788", getFormElement(Locator.name("usedCells")));
        assertEquals("5", table.getDataAsText(0, "Vial Count"));

        pressTab(Locator.name("viableCells"));

        clickButton("Save", 0);
        assertAlert("All used cell fields must be greater than or equal to zero.");

        setFormElement(Locator.name("viableCells"), "20000001");
        fireEvent(Locator.name("viableCells"), SeleniumEvent.change);
        assertEquals("504211", getFormElement(Locator.name("usedCells")));
        assertEquals("3", table.getDataAsText(0, "Vial Count"));

        clickButton("Save");


        log("** checking cell counts and specimen IDs");
        table = new DataRegionTable("Data", getDriver());
        assertEquals("B02", table.getDataAsText(1, "Participant ID"));
        assertEquals("B02_1.0_0,B02_1.0_1,B02_1.0_2", table.getDataAsText(1, "Specimen IDs"));
        // Viable Cells value doesn't get updated, only Original Cells
        assertEquals("2.050E7", table.getDataAsText(1, "Viable Cells"));
        assertEquals("2.000E7", table.getDataAsText(1, "Original Cells"));
        assertEquals("3", table.getDataAsText(1, "Specimen Count"));

        assertEquals("B03", table.getDataAsText(2, "Participant ID"));
        assertEquals("B03_1.0_0,B03_1.0_1,B03_1.0_2", table.getDataAsText(2, "Specimen IDs"));
        assertEquals("2.270E7", table.getDataAsText(2, "Viable Cells"));
        assertEquals("2.260E7", table.getDataAsText(2, "Original Cells"));
        assertEquals("3", table.getDataAsText(2, "Specimen Count"));

        
        log("** checking new site 'Site A' was added and no duplicate 'Bob's Lab' exist (Issue 12074)");
        pushLocation();
        beginAt("/query/" + getProjectName() + "/" + getFolderName() + "/executeQuery.view?schemaName=study&query.queryName=Site&query.sort=RowId");
        table = new DataRegionTable("query", getDriver());
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
        table = new DataRegionTable("SpecimenDetail", getDriver());
        assertEquals("B02_1.0_0", table.getDataAsText(0, "Global Unique Id"));
        assertEquals("6,666,667.0", table.getDataAsText(0, "Volume"));
        assertEquals("Bob's Lab", table.getDataAsText(0, "Site Name"));

        assertEquals("B02_1.0_1", table.getDataAsText(1, "Global Unique Id"));
        assertEquals("6,666,667.0", table.getDataAsText(1, "Volume"));
        assertEquals("Bob's Lab", table.getDataAsText(1, "Site Name"));

        assertEquals("B02_1.0_2", table.getDataAsText(2, "Global Unique Id"));
        assertEquals("6,666,667.0", table.getDataAsText(2, "Volume"));
        assertEquals("Bob's Lab", table.getDataAsText(2, "Site Name"));

        assertEquals("B03_1.0_0", table.getDataAsText(3, "Global Unique Id"));
        assertEquals("7,533,149.0", table.getDataAsText(3, "Volume"));
        assertEquals("Site A", table.getDataAsText(3, "Site Name"));

        assertEquals("B03_1.0_1", table.getDataAsText(4, "Global Unique Id"));
        assertEquals("7,533,149.0", table.getDataAsText(4, "Volume"));
        assertEquals("Site A", table.getDataAsText(4, "Site Name"));

        log("** move two vials to 'Site C'");
        table.checkCheckbox(1);
        table.checkCheckbox(3);
        clickButton("Move Vials", 0);
        _extHelper.waitForExtDialog("Move Vials");
        waitForFormElementToNotEqual(Locator.id("changeLocationField"), "");
        setFormElement(Locator.id("changeLocationField"), "Site C");
        pressTab(Locator.id("changeLocationField"));
        _extHelper.clickExtButton("Move Vials", WAIT_FOR_JAVASCRIPT);

        assertEquals("B02_1.0_1", table.getDataAsText(1, "Global Unique Id"));
        assertEquals("Site C", table.getDataAsText(1, "Site Name"));

        assertEquals("B02_1.0_2", table.getDataAsText(2, "Global Unique Id"));
        assertEquals("Bob's Lab", table.getDataAsText(2, "Site Name"));
        
        assertEquals("B03_1.0_0", table.getDataAsText(3, "Global Unique Id"));
        assertEquals("Site C", table.getDataAsText(3, "Site Name"));
        popLocation();
        

        log("** Copy to study");
        clickAndWait(Locator.linkWithText("view runs"));
        click(Locator.name(".toggle"));
        clickButton("Copy to Study");
        clickButton("Next");
        assertTitleContains("Copy to " + getFolderName() + " Study");
        // UNDONE: assert first row has no specimen match
        clickButton("Copy to Study");
        assertTextPresent("B02");

    }

}
