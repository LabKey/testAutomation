/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: cnathe
 * Date: 10/23/12
 */
public class HaplotypeAssayTest extends GenotypingTest
{
    private static final String PROJECT_NAME = "HaplotypeAssayVerifyProject";
    private static final String ASSAY_NAME = "HaplotypeAssay";// + TRICKY_CHARACTERS_NO_QUOTES;
    private static final File FIRST_RUN_FILE = new File(getSampledataPath(), "genotyping/haplotypeAssay/firstRunData.txt");
    private static final File SECOND_RUN_FILE = new File(getSampledataPath(), "genotyping/haplotypeAssay/secondRunData.txt");
    private static final File ERROR_RUN_FILE = new File(getSampledataPath(), "genotyping/haplotypeAssay/errorRunData.txt");
    private static final File DRB_RUN_FILE = new File(getSampledataPath(), "genotyping/haplotypeAssay/dbrRunData.txt");
    public static final String DBR_ASSAY = "DBR assay";
    public static final String DRB_RUN = "drb run";

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
        verifyAssignmentReport();
        verifyDuplicateRecords();
        verifyAribitraryHaplotypeAssay();

    }

    @LogMethod
    private void verifyAribitraryHaplotypeAssay()
    {
        setupHaplotypeAssay(DBR_ASSAY,  new String[][] {{"DRBHaplotype", "DRB Haplotype" }});
        importRun(DRB_RUN, DBR_ASSAY, DRB_RUN_FILE);

        clickAndWait(Locator.linkWithText(DRB_RUN));
        DataRegionTable drt = new DataRegionTable("Data", this);
//        String[] DR?B1 =
        verifyColumnDataValues(drt, "Mamu-AHaplotype1", "A001,A023,A001,A004,A002a");
        verifyColumnDataValues(drt, "Mamu-AHaplotype2", "A023,A025,A001,A023,A002a");
        verifyColumnDataValues(drt, "DRB Haplotype 1", "D015c,D012b,D001c,D012b,D002");
        verifyColumnDataValues(drt, "DRB Haplotype 2", "D025a,D017a,D017a,D012b,D002");
    }
 
    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }

    @LogMethod
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
        clickAndWait(Locator.linkWithText("Animal"));
        _customizeViewsHelper.openCustomizeViewPanel();
        waitForText("Animal String Test");
        assertTextPresent("Animal Integer Test");

        log("Configure extensible Haplotype table");
        goToProjectHome();
        clickLink("adminSettings");
        clickLink("configureHaplotype");
        waitForText("No fields have been defined.");
        _listHelper.addField("Field Properties", 0, "haplotypeStrTest", "Haplotype String Test", ListHelper.ListColumnType.String);
        _listHelper.addField("Field Properties", 1, "haplotypeIntTest", "Haplotype Integer Test", ListHelper.ListColumnType.Integer);
        clickButton("Save");
        clickAndWait(Locator.linkWithText("Haplotype"));
        _customizeViewsHelper.openCustomizeViewPanel(); //TODO:  should this be necessary?
        assertTextPresent("Haplotype String Test");
        assertTextPresent("Haplotype Integer Test");
    }

    private void setupHaplotypeAssay()
    {
        setupHaplotypeAssay(ASSAY_NAME, null);
    }

    @LogMethod
    private void setupHaplotypeAssay(String name, String[][] extraHaplotypes)
    {
        log("Setting up Haplotype assay");
        goToProjectHome();
        goToManageAssays();
        clickButton("New Assay Design");
        checkRadioButton("providerName", "Haplotype");
        clickButton("Next");
        waitForElement(Locator.xpath("//input[@id='AssayDesignerName']"), WAIT_FOR_JAVASCRIPT);

        if(extraHaplotypes!=null)
        {
            int columnIndex = 9;
            for(String[] haplotype : extraHaplotypes)
            {
                addRunField(haplotype[0] + "1", haplotype[1] + " 1", columnIndex++, ListHelper.ListColumnType.String);
                click(Locator.xpath("(//span[@id='propertyShownInInsert']/input)[2]"));
                click(Locator.xpath("(//span[@id='propertyShownInUpdate']/input)[2]"));

                addRunField(haplotype[0] + "2", haplotype[1]  + " 2", columnIndex++, ListHelper.ListColumnType.String);
                click(Locator.xpath("(//span[@id='propertyShownInInsert']/input)[2]"));
                click(Locator.xpath("(//span[@id='propertyShownInUpdate']/input)[2]"));
            }
        }

        setFormElement(Locator.id("AssayDesignerName"), name);
        fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), SeleniumEvent.blur);
        checkCheckbox(Locator.name("editableRunProperties"));

        clickButton("Save", 0);
        waitForText("Save successful.", WAIT_FOR_JAVASCRIPT);

    }

    @LogMethod
    private void verifyAssayUploadErrors()
    {
        log("Test errors with Haplotype assay upload");
        goToAssayImport(ASSAY_NAME);
        clickButton("Save and Finish");
        waitForText("Data contained zero data rows");
        setFormElement(Locator.name("data"), getFileContents(ERROR_RUN_FILE));
        sleep(1000);
        clickButton("Save and Finish");
        waitForText("Column header mapping missing for: Lab Animal ID");
        waitForElementToDisappear(Locator.xpath("//table[contains(@class,'item-disabled')]//label[text() = 'Mamu-B Haplotype 2 *:']"), WAIT_FOR_JAVASCRIPT);
        _ext4Helper.selectComboBoxItem("Lab Animal ID *:", "OC ID");
        clickButton("Save and Finish");
        waitForText("Column header mapping missing for: Total # Reads Evaluated");
        clickButton("Cancel");
    }

    @LogMethod
    private void verifyFirstRun()
    {
        importRun("first run", ASSAY_NAME, FIRST_RUN_FILE);

        log("Verify Haplotype Assignment data for the first run");
        goToAssayRun("first run");

        // add the Animal/ClientAnimalId column so we can verify that as well
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addCustomizeViewColumn("AnimalId/ClientAnimalId");
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
        verifyColumnDataValues(drt, "ClientAnimalId", "x123,x234,x345,x456,x567");

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
        verifyColumnDataValues(drt, "Lab Animal Id", "ID-1,ID-2,ID-3,ID-4,ID-5");
        verifyColumnDataValues(drt, "Client Animal Id", "x123,x234,x345,x456,x567");

        verifyHaplotypeRecordsByType(11, 5, 6);
    }

    @LogMethod
    private void verifySecondRun()
    {
        importRun("second run", ASSAY_NAME, SECOND_RUN_FILE);

        log("Verify Haplotype Assignment data for the second run");
        goToAssayRun("second run");

        DataRegionTable drt = new DataRegionTable("Data", this);
        verifyColumnDataValues(drt, "Animal", "ID-4,ID-5,ID-6,ID-7,ID-8,ID-9");
        verifyColumnDataValues(drt, "TotalReads", "4000,5000,6000,7000,,0");
        verifyColumnDataValues(drt, "IdentifiedReads", "2500,3250,3000,3500,,1");
        verifyColumnDataValues(drt, "%Unknown", "37.5,35.0,50.0,50.0,,");
        verifyColumnDataValues(drt, "Mamu-AHaplotype1", "A001,,A033,A004,A004,A004");
        verifyColumnDataValues(drt, "Mamu-AHaplotype2", "A023,,A033,A004,A004,A004");
        verifyColumnDataValues(drt, "Mamu-BHaplotype1", "B015c,,B012b,B033,B033,B033");
        verifyColumnDataValues(drt, "Mamu-BHaplotype2", "B025a,,B012b,B033,B033,B033");
        verifyColumnDataValues(drt, "Enabled", "true,true,true,true,true,true");
        verifyColumnDataValues(drt, "ClientAnimalId", "x456,x567,x678,x789,x888,x999");

        // verify concatenated haplotype strings
        assertTextPresent("A001,A023,B015c,B025a");
        assertTextPresent("A033,A033,B012b,B012b");
        assertTextPresent("A004,B033,B033");   // record with only 3 haplotype assignments

        // verify that the animal and haplotype rows were properly inserted
        goToQuery("Animal");
        drt = new DataRegionTable("query", this);
        Assert.assertEquals("Unexpected number of Animal records", 9, drt.getDataRowCount());
        verifyColumnDataValues(drt, "LabAnimalId", "ID-1,ID-2,ID-3,ID-4,ID-5,ID-6,ID-7,ID-8,ID-9");
        verifyColumnDataValues(drt, "ClientAnimalId", "x123,x234,x345,x456,x567,x678,x789,x888,x999");

        verifyHaplotypeRecordsByType(13, 6, 7);
    }

    @LogMethod
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
        waitForText("1 - 39 of 39");
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
        //TODO: 17077: Concatenated Halplotypes appear out of order
//        assertTextPresent("A001,A002a,A002a,B002,B002,B002");
        drt.clearFilter("AnimalId");
    }

    @LogMethod
    private void verifyAssignmentReport()
    {
        log("Verify Haplotype Assignment Report");
        goToProjectHome();
        goToAssayRun("first run");
        clickButton("Produce Report");
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
        _ext4Helper.selectComboBoxItem("Show report column headers as:", "Client Animal ID");
        clickButton("Submit", 0);
        waitForText("x345");
        drt = new DataRegionTable("report", this);
        assertTextPresentInThisOrder("A001","B001c","B017a");
        verifyColumnDataValues(drt, "x345", "2,1,1");

        // test with IDs that only have one result
        _ext4Helper.selectComboBoxItem("Search for animal IDs by:", "Client Animal ID");
        _ext4Helper.selectComboBoxItem("Show report column headers as:", "Lab Animal ID");
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
        _ext4Helper.selectComboBoxItem("Search for animal IDs by:", "Lab Animal ID");
        _ext4Helper.selectComboBoxItem("Show report column headers as:", "Lab Animal ID");
        setFormElement(Locator.name("idsTextArea"), "ID-4,ID-5");
        clickButton("Submit", 0);
        waitForText("1 - 8 of 8");
        waitForText("Warning: multiple enabled assay results were found for the following IDs: ID-4 (2), ID-5 (2)");
        drt = new DataRegionTable("report", this);
        verifyColumnDataValues(drt, "ID-4", "1,,1,2,,2,1,1");
        verifyColumnDataValues(drt, "ID-5", "1,2,,,3,,,");
    }

    @LogMethod
    private void verifyDuplicateRecords()
    {
        // verify that the two duplicates show up on the duplicates report
        goToAssayRun("first run");
        clickAndWait(Locator.linkWithText("view duplicates"));
        waitForText("# Active Assignments");
        assertLinkPresentWithText("ID-4");
        assertLinkPresentWithText("ID-5");

        // test editing a run/animal record to clear a duplicate for ID-4
        goToAssayRun("first run");
        drt = new DataRegionTable("Data", this);
        drt.setFilter("AnimalId", "Equals", "ID-4");
        clickAndWait(Locator.linkWithText("edit"));
        waitForText("mamuB Haplotype", 2, WAIT_FOR_JAVASCRIPT);
        _ext4Helper.uncheckCheckbox("Enabled:");
        clickButton("Submit");
        drt = new DataRegionTable("Data", this);
        verifyColumnDataValues(drt, "Enabled", "false");
        setReportId("ID-4");
        assertTextNotPresent("Warning: multiple enabled assay results were found for the following IDs");
        assertTextPresentInThisOrder("A001","A023","B015c","B025a");
        assertTextNotPresent("A004", "B012b");

        // test disabling a run and clearing the other duplicate for ID-5
        goToManageAssays();
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        drt = new DataRegionTable("Runs", this);
        drt.setFilter("Name", "Equals", "second run");
        clickAndWait(Locator.linkWithText("edit"));
        uncheckCheckbox(Locator.name("quf_enabled"));
        clickButton("Submit");
        goToAssayRun("second run");
        setReportId("ID-5");
        assertTextNotPresent("Warning: multiple enabled assay results were found for the following IDs");
        assertTextPresentInThisOrder("A001","A002a","B002");

        // verify that the duplicates report is now clear
        clickAndWait(Locator.linkWithText("view duplicates"));
        waitForText("# Active Assignments");
        assertTextNotPresent("ID-4", "ID-5");
    }

    private void setReportId(String id)
    {
        // this method assumes that we are already viewing the Assay results grid
        drt = new DataRegionTable("Data", this);
        drt.setFilter("AnimalId", "Equals", id);
        checkCheckbox(".select");
        clickButton("Produce Report");
        waitForText("Enter the animal IDs separated by whitespace, comma, or semicolon:");
        Locator dr = Locator.id("dataregion_report");
        waitForElement(dr);
    }

    private void goToQuery(String queryName)
    {
        goToSchemaBrowser();
        selectQuery("genotyping", queryName);
        waitForText("view data");
        clickAndWait(Locator.linkContainingText("view data"));
    }

    @LogMethod
    private void verifyHaplotypeRecordsByType(int total, int typeACount, int typeBCount)
    {
        goToQuery("Haplotype");
        drt = new DataRegionTable("query", this);
        Assert.assertEquals("Unexpected number of Haplotype records", total, drt.getDataRowCount());
        drt.setFilter("Type", "Equals", "mamuA");
        Assert.assertEquals("Unexpected number of filtered Haplotype records", typeACount, drt.getDataRowCount());
        drt.clearFilter("Type");
        drt.setFilter("Type", "Equals", "mamuB");
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

    @LogMethod
    private void importRun(String assayId, String assayName, File dataFile)
    {
        log("Importing Haplotype Run: " + assayId);
        goToAssayImport(assayName);
        setFormElement(Locator.name("name"), assayId);
        checkCheckbox("enabled");
        setDataAndColumnHeaderProperties(dataFile);
        sleep(3500); //TODO
        clickButton("Save and Finish");
        waitForText(assayName + " Runs");
        assertLinkPresentWithText(assayId);
    }

    @LogMethod
    private void setDataAndColumnHeaderProperties(File dataFile)
    {
        // adding text to the data text area triggers the events to enable the comboboxes and load their stores
        Locator cb = Locator.xpath("//table[contains(@class,'disabled')]//label[text() = 'Mamu-B Haplotype 2:']");
        if (!isElementPresent(cb))
            Assert.fail("The Haplotype column header mapping comboboxes should be disbabled until the data is pasted in.");

        setFormElement(Locator.name("data"), getFileContents(dataFile));
        waitForElementToDisappear(cb, WAIT_FOR_JAVASCRIPT);
        _ext4Helper.selectComboBoxItem("Lab Animal ID *:", "OC ID");
        _ext4Helper.selectComboBoxItem("Client Animal ID:", "Animal ID");
        _ext4Helper.selectComboBoxItem("Total # Reads Evaluated *:", "# Reads Merged");
        _ext4Helper.selectComboBoxItem("Total # Reads Identified *:", "# Reads Identified");
    }

    private void goToAssayRun(String assayId)
    {
        log("Navigating to Haplotype assay run");
        goToProjectHome();
        goToManageAssays();
        clickAndWait(Locator.linkWithText(ASSAY_NAME));
        clickAndWait(Locator.linkWithText(assayId));
        waitForText(ASSAY_NAME + " Results");
    }

    private void goToAssayImport(String assayName)
    {
        log("Navigating to Haplotype Assay Import");
        goToProjectHome();
        goToManageAssays();
        clickAndWait(Locator.linkWithText(assayName));
        clickButton("Import Data");
        waitForText("Copy/Paste the header rows into the text area below:");
        waitForText("Match the column headers from the tab-delimited data with the key fields:");
    }
}
