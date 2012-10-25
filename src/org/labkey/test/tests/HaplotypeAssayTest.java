/*
 * Copyright (c) 2012 LabKey Corporation
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

import junit.framework.Assert;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;

import java.io.File;
import java.util.List;

/**
 * User: cnathe
 * Date: 10/23/12
 */
public class HaplotypeAssayTest extends GenotypingTest
{
    private static final String PROJECT_NAME = "HaplotypeAssayVerifyProject";
    private static final String ASSAY_NAME = "HaplotypeAssay" + TRICKY_CHARACTERS_NO_QUOTES;
    private static final File FIRST_RUN_FILE = new File(getSampledataPath(), "genotyping/haplotypeAssay/firstRunData.txt");
    private static final File SECOND_RUN_FILE = new File(getSampledataPath(), "genotyping/haplotypeAssay/secondRunData.txt");
    private static final File ERROR_RUN_FILE = new File(getSampledataPath(), "genotyping/haplotypeAssay/errorRunData.txt");

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setUp2();  // from GenotypingTest
        configureExtensibleTables();
        setupHaplotypeAssay();
        verifyAssayUploadErrors();
        verifyFirstRun();
        verifySecondRun();
        verifyExtraHaplotypeAssignment();
        verifyCustomerReport();
    }

    @Override
    protected void doCleanup() throws Exception
    {
        deleteProject(getProjectName());
    }

    private void configureExtensibleTables()
    {
        log("Configure extensible Animal table");
        goToProjectHome();
        clickLink("adminSettings");
        clickLink("configureAnimal");
        waitForText("No fields have been defined.");
        _listHelper.addField("Field Properties", 0, "animalStrTest", "Animal String Test", ListHelper.ListColumnType.String);
        _listHelper.addField("Field Properties", 1, "animalIntTest", "Animal Integer Test", ListHelper.ListColumnType.Integer);
        clickButton("Save");
        clickLinkWithText("Animal");
        assertTextPresent("Animal String Test");
        assertTextPresent("Animal Integer Test");

        log("Configure extensible Haplotype table");
        goToProjectHome();
        clickLink("adminSettings");
        clickLink("configureHaplotype");
        waitForText("No fields have been defined.");
        _listHelper.addField("Field Properties", 0, "haplotypeStrTest", "Haplotype String Test", ListHelper.ListColumnType.String);
        _listHelper.addField("Field Properties", 1, "haplotypeIntTest", "Haplotype Integer Test", ListHelper.ListColumnType.Integer);
        clickButton("Save");
        clickLinkWithText("Haplotype");
        assertTextPresent("Haplotype String Test");
        assertTextPresent("Haplotype Integer Test");
    }

    private void setupHaplotypeAssay()
    {
        log("Setting up Haplotype assay");
        goToProjectHome();
        goToManageAssays();
        clickButton("New Assay Design");
        checkRadioButton("providerName", "Haplotype");
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.id("AssayDesignerName"), ASSAY_NAME);
        checkCheckbox(Locator.name("editableRunProperties"));

        clickButton("Save", 0);
        waitForText("Save successful.", WAIT_FOR_JAVASCRIPT);
    }

    private void verifyAssayUploadErrors()
    {
        log("Test errors with Haplotype assay upload");
        goToHaplotypeAssayImport();
        clickButton("Save and Finish");
        waitForText("Data contained zero data rows");
        setFormElement(Locator.name("data"), getFileContents(ERROR_RUN_FILE));
        clickButton("Save and Finish");
        waitForText("Column header mapping missing for: Lab Animal ID");
        _ext4Helper.selectComboBoxItem("Lab Animal ID", "OC ID");
        clickButton("Save and Finish");
        waitForText("Column header mapping missing for: Customer Animal ID");
        clickButton("Cancel");
    }

    private void verifyFirstRun()
    {
        importRun("first run", FIRST_RUN_FILE);

        log("Verify Haplotype Assignment data for the first run");
        goToAssayRun("first run");

        // add the Animal/CustomerAnimalId column so we can verify that as well
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("AnimalId/CustomerAnimalId");
        _customizeViewsHelper.saveCustomView();

        DataRegionTable drt = new DataRegionTable("Data", this);
        verifyColumnDataValues(drt, "Animal", "ID-1,ID-2,ID-3,ID-4,ID-5");
        verifyColumnDataValues(drt, "TotalReads", "1000,2000,3000,4000,5000");
        verifyColumnDataValues(drt, "IdentifiedReads", "300,1000,600,2500,3250");
        verifyColumnDataValues(drt, "%Unknown", "70.0,50.0,80.0,37.5,35.0");
        verifyColumnDataValues(drt, "Mamu-AHaplotype1", "A001,A023,A001,A004,A002a");
        verifyColumnDataValues(drt, "Mamu-AHaplotype2", "A023,A025,A001,A023,A002a");
        verifyColumnDataValues(drt, "Mamu-BHaplotype1", "B015c,B012b,B001c,B012b,B002");
        verifyColumnDataValues(drt, "Mamu-BHaplotype2", "B025a,B017a,B017a,B012b,B002");
        verifyColumnDataValues(drt, "Enabled", "true,true,true,true,true");
        verifyColumnDataValues(drt, "CustomerAnimalId", "x123,x234,x345,x456,x567");

        // verify concatenated haplotype strings
        assertTextPresent("A001,A023,B015c,B025a");
        assertTextPresent("A023,A025,B012b,B017a");
        assertTextPresent("A001,A001,B001c,B017a");
        assertTextPresent("A004,A023,B012b,B012b");
        assertTextPresent("A002a,A002a,B002,B002");

        // verify that the animal and haplotype rows were properly inserted
        goToQuery("Animal");
        drt = new DataRegionTable("query", this);
        Assert.assertEquals("Unexpected number of Animal records", 5, drt.getDataRowCount());
        verifyColumnDataValues(drt, "LabAnimalId", "ID-1,ID-2,ID-3,ID-4,ID-5");
        verifyColumnDataValues(drt, "CustomerAnimalId", "x123,x234,x345,x456,x567");

        verifyHaplotypeRecordsByType(11, 5, 6);
    }

    private void verifySecondRun()
    {
        importRun("second run", SECOND_RUN_FILE);

        log("Verify Haplotype Assignment data for the second run");
        goToAssayRun("second run");

        DataRegionTable drt = new DataRegionTable("Data", this);
        verifyColumnDataValues(drt, "Animal", "ID-4,ID-5,ID-6,ID-7");
        verifyColumnDataValues(drt, "TotalReads", "4000,5000,6000,7000");
        verifyColumnDataValues(drt, "IdentifiedReads", "2500,3250,3000,3500");
        verifyColumnDataValues(drt, "%Unknown", "37.5,35.0,50.0,50.0");
        verifyColumnDataValues(drt, "Mamu-AHaplotype1", "A001,,A033,A004"); // note: ,, in str to test record without any haplotype assignments
        verifyColumnDataValues(drt, "Mamu-AHaplotype2", "A023,,A033,A004");
        verifyColumnDataValues(drt, "Mamu-BHaplotype1", "B015c,,B012b,B033");
        verifyColumnDataValues(drt, "Mamu-BHaplotype2", "B025a,,B012b,B033");
        verifyColumnDataValues(drt, "Enabled", "true,true,true,true");
        verifyColumnDataValues(drt, "CustomerAnimalId", "x456,x567,x678,x789");

        // verify concatenated haplotype strings
        assertTextPresent("A001,A023,B015c,B025a");
        assertTextPresent("A033,A033,B012b,B012b");
        assertTextPresent("A004,B033,B033");   // record with only 3 haplotype assignments

        // verify that the animal and haplotype rows were properly inserted
        goToQuery("Animal");
        drt = new DataRegionTable("query", this);
        Assert.assertEquals("Unexpected number of Animal records", 7, drt.getDataRowCount());
        verifyColumnDataValues(drt, "LabAnimalId", "ID-1,ID-2,ID-3,ID-4,ID-5,ID-6,ID-7");
        verifyColumnDataValues(drt, "CustomerAnimalId", "x123,x234,x345,x456,x567,x678,x789");

        verifyHaplotypeRecordsByType(13, 6, 7);
    }

    private void verifyExtraHaplotypeAssignment()
    {
        log("Verify Animal Haplotype Assignment with > 4 assignments");
        goToProjectHome();
        goToAssayRun("first run");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.addCustomizeViewColumn("RowId");
        _customizeViewsHelper.saveCustomView();
        DataRegionTable drt = new DataRegionTable("Data", this);
        String animalAnalysisId = drt.getDataAsText(4, "RowId"); // row index 4 is ID-5
        goToQuery("AnimalHaplotypeAssignment");
        // ADD: animal ID-5, haplotype A001
        clickButton("Insert New");
        selectOptionByText(Locator.name("quf_HaplotypeId"), "A001");
        selectOptionByText(Locator.name("quf_AnimalAnalysisId"), animalAnalysisId);
        clickButton("Submit");
        // ADD: animal ID-5, haplotype B002
        clickButton("Insert New");
        selectOptionByText(Locator.name("quf_HaplotypeId"), "B002");
        selectOptionByText(Locator.name("quf_AnimalAnalysisId"), animalAnalysisId);
        clickButton("Submit");
        // verify the calculated columns in the assay results view for concatenated haplotype, etc.
        goToProjectHome();
        goToAssayRun("first run");
        drt = new DataRegionTable("Data", this);
        drt.setFilter("AnimalId", "Equals", "ID-5");
        verifyColumnDataValues(drt, "Mamu-AHaplotype1", "A001");
        verifyColumnDataValues(drt, "Mamu-AHaplotype2", "A002a");
        verifyColumnDataValues(drt, "Mamu-BHaplotype1", "B002");
        verifyColumnDataValues(drt, "Mamu-BHaplotype2", "B002");
        assertTextPresent("A001,A002a,A002a,B002,B002,B002");
        drt.clearFilter("AnimalId");
    }

    private void verifyCustomerReport()
    {
        log("Verify Haplotype Assignment customer report");
        goToProjectHome();
        goToAssayRun("first run");
        clickLinkWithText("produce customer report");
        waitForText("Search for animal IDs by:");
        assertTextPresent("Show report column headers as:");
        assertTextPresent("Enter the animal IDs separated by whitespace, comma, or semicolon:");

        // test a single ID
        Locator dr = Locator.id("dataregion_report");
        setFormElement(Locator.name("idsTextArea"), "ID-3");
        sleep(500); // entering text enables the submit button
        clickButton("Submit", 0);
        waitForElement(dr);
        DataRegionTable drt = new DataRegionTable("report", this);
        assertTextPresentInThisOrder("A001", "B001c", "B017a");
        verifyColumnDataValues(drt, "ID-3", "2,1,1");
        _ext4Helper.selectComboBoxItem("Show report column headers as", "CustomerAnimalID");
        clickButton("Submit", 0);
        waitForText("x345");
        drt = new DataRegionTable("report", this);
        assertTextPresentInThisOrder("A001","B001c","B017a");
        verifyColumnDataValues(drt, "x345", "2,1,1");

        // test with IDs that only have one result
        _ext4Helper.selectComboBoxItem("Search for animal IDs by", "CustomerAnimalID");
        _ext4Helper.selectComboBoxItem("Show report column headers as", "LabAnimalID");
        setFormElement(Locator.name("idsTextArea"), "x123,x234;x345 x678 x789");
        clickButton("Submit", 0);
        waitForText("1 - 11 of 11");
        assertTextPresentInThisOrder("ID-1", "ID-2", "ID-3", "ID-6", "ID-7");
        drt = new DataRegionTable("report", this);
        drt.setFilter("ID-1::Counts", "Equals", "1", 0);
        waitForText("ID-1::Counts = 1");
        assertTextPresentInThisOrder("A001","A023","B015c","B025a");
        assertTextNotPresent("A004");
        drt.clearFilter("ID-1::Counts", 0);
        drt.setFilter("ID-6::Counts", "Equals", "2", 0);
        waitForText("ID-6::Counts = 2");
        assertTextPresentInThisOrder("A033","B012b");
        assertTextNotPresent("A001");
        drt.clearFilter("ID-6::Counts", 0);

        // test with IDs that have duplicate reocrds
        _ext4Helper.selectComboBoxItem("Search for animal IDs by", "LabAnimalID");
        _ext4Helper.selectComboBoxItem("Show report column headers as", "LabAnimalID");
        setFormElement(Locator.name("idsTextArea"), "ID-4,ID-5");
        clickButton("Submit", 0);
        waitForText("1 - 8 of 8");
        waitForText("Warning: multiple enabled assay results were found for the following IDs: ID-4 (2), ID-5 (2)");
        drt = new DataRegionTable("report", this);
        verifyColumnDataValues(drt, "ID-4", "1,,1,2,,2,1,1");

        // test disabling a run and clearing a duplicate
        goToManageAssays();
        clickLinkWithText(ASSAY_NAME);
        drt = new DataRegionTable("Runs", this);
        drt.setFilter("Name", "Equals", "second run");
        clickLinkWithText("edit");
        uncheckCheckbox(Locator.name("quf_enabled"));
        clickButton("Submit");
        clickLinkWithText("produce customer report");
        waitForText("Enter the animal IDs separated by whitespace, comma, or semicolon:");
        dr = Locator.id("dataregion_report");
        setFormElement(Locator.name("idsTextArea"), "ID-4");
        sleep(500); // entering text enables the submit button
        clickButton("Submit", 0);
        waitForElement(dr);
        assertTextPresentInThisOrder("A004","A023","B012b");
        assertTextNotPresent("A001", "B015c", "B025a");
    }

    private void goToQuery(String queryName)
    {
        goToSchemaBrowser();
        selectQuery("genotyping", queryName);
        waitForText("view data");
        clickLinkContainingText("view data");
    }

    private void verifyHaplotypeRecordsByType(int total, int typeACount, int typeBCount)
    {
        goToQuery("Haplotype");
        drt = new DataRegionTable("query", this);
        Assert.assertEquals("Unexpected number of Haplotype records", total, drt.getDataRowCount());
        drt.setFilter("Type", "Equals", "Mamu-A");
        Assert.assertEquals("Unexpected number of filtered Haplotype records", typeACount, drt.getDataRowCount());
        drt.clearFilter("Type");
        drt.setFilter("Type", "Equals", "Mamu-B");
        Assert.assertEquals("Unexpected number of filtered Haplotype records", typeBCount, drt.getDataRowCount());
        drt.clearFilter("Type");
    }

    private void verifyColumnDataValues(DataRegionTable drt, String colName, String valueStr)
    {
        Assert.assertEquals("Unexpected values in " + colName + " column", valueStr, listToConcatString(drt.getColumnDataAsText(colName)));
    }

    private String listToConcatString(List<String> list)
    {
        String str = "";
        String sep = "";
        for (String s : list)
        {
            str += sep + s;
            sep = ",";
        }
        return str;
    }

    private void importRun(String assayId, File dataFile)
    {
        log("Importing Haplotype Run: " + assayId);
        goToHaplotypeAssayImport();
        setFormElement(Locator.name("name"), assayId);
        checkCheckbox("enabled");
        setDataAndColumnHeaderProperties(dataFile);
        clickButton("Save and Finish");
        waitForText(ASSAY_NAME + " Runs");
        assertLinkPresentWithText(assayId);
    }

    private void setDataAndColumnHeaderProperties(File dataFile)
    {
        // adding text to the data text area triggers the events to enable the comboboxes and load their stores
        Locator cb = Locator.xpath("//table[contains(@class,'item-disabled')]//label[text() = 'Mamu-B Haplotype 2:']");
        setFormElement(Locator.name("data"), getFileContents(dataFile));
        waitForElementToDisappear(cb, WAIT_FOR_JAVASCRIPT);
        _ext4Helper.selectComboBoxItem("Lab Animal ID", "OC ID");
        _ext4Helper.selectComboBoxItem("Customer Animal ID", "Animal ID");
        _ext4Helper.selectComboBoxItem("Total # Reads Evaluated", "# Reads Merged");
        _ext4Helper.selectComboBoxItem("Total # Reads Identified", "# Reads Identified");
    }

    private void goToAssayRun(String assayId)
    {
        log("Navigating to Haplotype assay run");
        goToProjectHome();
        goToManageAssays();
        clickLinkWithText(ASSAY_NAME);
        clickLinkWithText(assayId);
        waitForText(ASSAY_NAME + " Results");
    }

    private void goToHaplotypeAssayImport()
    {
        log("Navigating to Haplotype Assay Import");
        goToProjectHome();
        goToManageAssays();
        clickLinkWithText(ASSAY_NAME);
        clickButton("Import Data");
        waitForText("Copy/Paste the header rows into the text area below:");
        waitForText("Match the column headers from the tab-delimited data with the key fields:");
    }
}
