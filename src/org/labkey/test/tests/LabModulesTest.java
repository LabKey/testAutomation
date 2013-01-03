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
import org.apache.commons.lang3.tuple.Pair;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.AdvancedSqlTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LabModuleHelper;
import org.labkey.test.util.UIContainerHelper;
import org.labkey.test.util.ext4cmp.Ext4FieldRefWD;
import org.openqa.selenium.Alert;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: bbimber
 * Date: 5/27/12
 * Time: 8:33 PM
 */


/**
 * Contains a series of tests designed to test the UI in the laboratory module.
 * Also contains considerable coverage of Ext4 components and the client API
 */
public class LabModulesTest extends BaseWebDriverTest implements AdvancedSqlTest
{
    protected LabModuleHelper _helper = new LabModuleHelper(this);
    protected String PROJECT_NAME = "LaboratoryVerifyProject" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    private String VIRAL_LOAD_ASSAYNAME = "Viral Load Test";

    private int _oligosTotal = 0;
    private int _samplesTotal = 0;
    private int _peptideTotal = 0;

    protected static final String IMPORT_DATA_TEXT = "Import Data";

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    public LabModulesTest()
    {
        setContainerHelper(new UIContainerHelper(this));
    }

    @Override
    public void validateQueries(boolean validateSubfolders)
    {
        super.validateQueries(false); // too may subfolders
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setUpTest();
        overviewUITest();
        reportsTest();
        settingsTest();
        defaultAssayImportMethodTest();

        labToolsWebpartTest();
        workbookCreationTest();
        dnaOligosTableTest();
        samplesTableTest();
        urlGenerationTest();
        peptideTableTest();
        searchPanelTest();
        queryMetadataTest();
    }

    protected void setUpTest() throws Exception
    {
        _containerHelper.createProject(getProjectName(), "Laboratory Folder");
        enableModules(getEnabledModules(), true);

        setupAssays();
    }

    protected void setupAssays()
    {
        for(Pair<String, String> pair : getAssaysToCreate())
        {
            _helper.defineAssay(pair.getKey(), pair.getValue());
        }
    }

    protected List<Pair<String, String>> getAssaysToCreate()
    {
        List<Pair<String, String>> assays = new ArrayList<Pair<String, String>>();
        assays.add(Pair.of("Immunophenotyping", "TruCount Test"));
        assays.add(Pair.of("ICS", "ICS Test"));
        assays.add(Pair.of("SSP Typing", "SSP Test"));
        assays.add(Pair.of("Viral Loads", VIRAL_LOAD_ASSAYNAME));
        assays.add(Pair.of("ELISPOT_Assay", "ELISPOT Test"));
        assays.add(Pair.of("Electrochemiluminescence Assay", "Electrochemiluminescence Assay Test"));
        assays.add(Pair.of("Genotype Assay", "Genotyping Assay Test"));

        return assays;
    }

    private void overviewUITest()
    {
        log("Testing Overview UI");
        goToProjectHome();

        //verify import not visible to reader
        impersonateRole("Reader");
        _helper.goToLabHome();

        _helper.verifyNavPanelRowItemPresent("Sequence:");

        for(Pair<String, String> pair : getAssaysToCreate())
        {
            _helper.verifyNavPanelRowItemPresent(pair.getValue() + ":");
            assertElementNotPresent(LabModuleHelper.getNavPanelItem(pair.getValue() + ":", IMPORT_DATA_TEXT));
        }

        _helper.verifyNavPanelRowItemPresent("DNA_Oligos:");
        assertElementNotPresent(LabModuleHelper.getNavPanelItem("DNA_Oligos:", IMPORT_DATA_TEXT));

        _helper.verifyNavPanelRowItemPresent("Peptides:");
        assertElementNotPresent(LabModuleHelper.getNavPanelItem("Peptides:", IMPORT_DATA_TEXT));

        _helper.verifyNavPanelRowItemPresent("Samples:");
        assertElementNotPresent(LabModuleHelper.getNavPanelItem("Samples:", IMPORT_DATA_TEXT));

        //now try UI will normal permissions
        stopImpersonatingRole();
        _helper.goToLabHome();

        _helper.clickNavPanelItem("Sequence:", IMPORT_DATA_TEXT);
        assertElementPresent(Ext4Helper.ext4MenuItem("Create Readsets"));
        assertElementPresent(Ext4Helper.ext4MenuItem("Upload Raw Data"));

        _ext4Helper.clickExt4MenuItem("Create Readsets");
        waitForElement(Ext4Helper.ext4Window("Create Readsets"));
        waitForElement(Locator.ext4Button("Close"));
        click(Locator.ext4Button("Close"));

    }

    private void settingsTest()
    {
        log("Testing Settings");
        _helper.goToLabHome();
        waitAndClick(_helper.toolIcon("Settings"));
        waitForPageToLoad();
        waitForText("Reference Sequences"); //proxy for page load

        assertElementPresent(LabModuleHelper.getNavPanelRow("Assay Types:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Instruments:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Peptide Pools:"));

        assertElementPresent(LabModuleHelper.getNavPanelRow("Diluents:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Roche E411 Tests:"));

        assertElementPresent(LabModuleHelper.getNavPanelRow("Cell Populations:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Units:"));

        assertElementPresent(LabModuleHelper.getNavPanelRow("Reference AA Features:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Reference NT Features:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Virus Strains:"));

        assertElementPresent(LabModuleHelper.getNavPanelRow("Allowable Cell Types:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Allowable Genders:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Allowable Sample Types:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Manage Freezers:"));

        assertElementPresent(LabModuleHelper.getNavPanelRow("Allowable Barcodes:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("DNA Loci:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Haplotype Definitions:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Input Material Types:"));

        assertElementPresent(LabModuleHelper.getNavPanelRow("ABI7500 Detectors:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Fluors:"));
        assertElementPresent(LabModuleHelper.getNavPanelRow("Techniques:"));

        waitAndClick(Locator.linkContainingText("Control Item Visibility"));
        waitForPageToLoad();
        waitForText("Sequence"); //proxy for page load
        waitForText("TruCount"); //proxy for page load

        log("Disabling items");

        int i = 1;
        for (Pair<String, String> pair : getAssaysToCreate())
        {
            Ext4FieldRefWD.getForBoxLabel(this, pair.getValue()).setValue(false);
            sleep(40); //wait for listener to act
            Assert.assertFalse("Radio was not toggled", (Boolean)Ext4FieldRefWD.getForBoxLabel(this, pair.getValue() + ": Raw Data").getValue());

            if (i == 1)
                Ext4FieldRefWD.getForBoxLabel(this, pair.getValue()).setValue(true);

            i++;
        }

        //sequence
        Ext4FieldRefWD.getForBoxLabel(this, "Sequence").setValue(false);
        sleep(40); //wait for listener to act
        Assert.assertFalse("Radio was not toggled", (Boolean)Ext4FieldRefWD.getForBoxLabel(this, "Browse Sequence Data").getValue());


        //samples
        Ext4FieldRefWD.getForBoxLabel(this, "Samples").setValue(false);
        sleep(40); //wait for listener to act
        Assert.assertFalse("Radio was not toggled", (Boolean)Ext4FieldRefWD.getForBoxLabel(this, "Freezer Summary").getValue());
        Assert.assertFalse("Radio was not toggled", (Boolean)Ext4FieldRefWD.getForBoxLabel(this, "View All Samples").getValue());

        //oligos
        Ext4FieldRefWD.getForBoxLabel(this, "DNA_Oligos").setValue(false);
        sleep(40); //wait for listener to act
        Assert.assertFalse("Radio was not toggled", (Boolean)Ext4FieldRefWD.getForBoxLabel(this, "View All DNA Oligos").getValue());

        //peptides
        Ext4FieldRefWD.getForBoxLabel(this, "Peptides").setValue(false);
        sleep(40); //wait for listener to act
        Assert.assertFalse("Radio was not toggled", (Boolean)Ext4FieldRefWD.getForBoxLabel(this, "View All Peptides").getValue());

        click(Locator.ext4Button("Submit"));
        waitForElement(Ext4Helper.ext4Window("Success"));
        click(Locator.ext4Button("OK"));

        //should redirect to lab home
        waitForText("Types of Data:");

        assertElementNotPresent(LabModuleHelper.getNavPanelRow("Sequence:"));
        assertElementNotPresent(LabModuleHelper.getNavPanelRow("Samples:"));
        assertElementNotPresent(LabModuleHelper.getNavPanelRow("DNA_Oligos:"));
        assertElementNotPresent(LabModuleHelper.getNavPanelRow("Peptides:"));
        i = 1;
        for (Pair<String, String> pair : getAssaysToCreate())
        {
            if (i == 1)
                assertElementPresent(LabModuleHelper.getNavPanelRow(pair.getValue() + ":"));
            else
                assertElementNotPresent(LabModuleHelper.getNavPanelRow(pair.getValue() + ":"));

            i++;
        }

        //also verify reports
        clickTab("Reports");
        waitForText("Raw Data");
        i = 1;
        for (Pair<String, String> pair : getAssaysToCreate())
        {
            if (i == 1)
                assertElementPresent(Locator.linkContainingText(pair.getValue() + ": Raw Data"));
            else
                assertElementNotPresent(Locator.linkContainingText(pair.getValue() + ": Raw Data"));

            i++;
        }
        assertElementPresent(Locator.linkContainingText("TruCount Test: Results Pivoted"));

        assertElementNotPresent(Locator.linkContainingText("View All")); //covers samples, peptides, oligos
        assertElementNotPresent(Locator.linkContainingText("Browse Sequence Data"));

        //restore defaults
        clickTab("Settings");
        waitForPageToLoad();
        waitAndClick(Locator.linkContainingText("Control Item Visibility"));
        waitForPageToLoad();
        waitForText("Sequence"); //proxy for page load
        waitForText("TruCount"); //proxy for page load

        for (Pair<String, String> pair : getAssaysToCreate())
        {
            Ext4FieldRefWD.getForBoxLabel(this, pair.getValue()).setValue(true);
        }
        Ext4FieldRefWD.getForBoxLabel(this, "Sequence").setValue(true);
        Ext4FieldRefWD.getForBoxLabel(this, "Samples").setValue(true);
        Ext4FieldRefWD.getForBoxLabel(this, "DNA_Oligos").setValue(true);
        Ext4FieldRefWD.getForBoxLabel(this, "Peptides").setValue(true);

        click(Locator.ext4Button("Submit"));
        waitForElement(Ext4Helper.ext4Window("Success"));
        click(Locator.ext4Button("OK"));
    }

    private void reportsTest()
    {
        _helper.goToLabHome();
        clickTab("Reports");

        //TODO: also verify link targets

        waitForElement(Locator.linkContainingText("Browse Sequence Data")); //proxy for page load

        for (Pair<String, String> pair : getAssaysToCreate())
        {
            assertElementPresent(Locator.linkContainingText(pair.getValue() + ": Raw Data"));
        }
        assertElementPresent(Locator.linkContainingText("TruCount Test: Results Pivoted"));
        assertElementPresent(Locator.linkContainingText("SSP Test: SSP_Summary"));
        assertElementPresent(Locator.linkContainingText("SSP Test: SSP_Pivot"));
        assertElementPresent(Locator.linkContainingText("ICS Test: Results Pivoted"));
        assertElementPresent(Locator.linkContainingText(VIRAL_LOAD_ASSAYNAME + ": Viral_Load_Summary"));

        assertElementPresent(Locator.linkContainingText("View All DNA Oligos"));
        assertElementPresent(Locator.linkContainingText("View All Peptides"));
        assertElementPresent(Locator.linkContainingText("View All Samples"));
        assertElementPresent(Locator.linkContainingText("Freezer Summary"));

        assertElementPresent(Locator.linkContainingText("Browse Sequence Data"));

    }

    private void labToolsWebpartTest()
    {
        log("testing lab tools webpart");
        _helper.goToLabHome();

        waitAndClick(_helper.toolIcon("Import Data"));
        assertElementPresent(Ext4Helper.ext4MenuItem("Sequence"));
        for(Pair<String, String> pair : getAssaysToCreate())
        {
            assertElementPresent(Ext4Helper.ext4MenuItem(pair.getValue()));
        }

        waitAndClick(_helper.toolIcon("Import Samples"));
        for (String s : getSampleItems())
        {
            assertElementPresent(Ext4Helper.ext4MenuItem(s));
        }

        //verify settings hidden for readers
        Locator settings = _helper.toolIcon("Settings");
        assertElementPresent(settings);
        impersonateRole("Reader");
        _helper.goToLabHome();
        assertElementNotPresent(settings);
        stopImpersonatingRole();
        goToProjectHome();
    }

    private void workbookCreationTest()
    {
        _helper.goToLabHome();

        _helper.clickNavPanelItem("View and Edit Workbooks:", "Create New Workbook");
        waitForElement(Ext4Helper.ext4Window("Create Workbook"));
        assertElementNotPresent(Locator.ext4Radio("Add To Existing Workbook"));
        waitForElement(Locator.ext4Button("Close"));
        click(Locator.ext4Button("Close"));
        assertElementNotPresent(Ext4Helper.ext4Window("Create Workbook"));

        String workbookTitle = "NewWorkbook_" + INJECT_CHARS_1;
        String workbookDescription = "I am a workbook.  I am trying to inject javascript into your page.  " + INJECT_CHARS_1 + INJECT_CHARS_2;
        _helper.createWorkbook(workbookTitle, workbookDescription);

        //verify correct name and correct webparts present
        assertElementPresent(_helper.webpartTitle("Lab Tools"));
        assertElementPresent(_helper.webpartTitle("Files"));
        assertElementPresent(_helper.webpartTitle("Workbook Summary"));

        //we expect insert from within the workbook to go straight to the import page (unlike the top-level folder, which gives a dialog)
        waitAndClick(_helper.toolIcon("Import Samples"));
        for (String s : getSampleItems())
        {
            assertElementPresent(Ext4Helper.ext4MenuItem(s));
        }
        //NOTE: we are in a workbook here
        _ext4Helper.clickExt4MenuItem("DNA_Oligos");
        waitForPageToLoad();
        waitForElement(Locator.name("name"));
        waitForElement(Locator.name("purification"));

        setFormElement(Locator.name("name"), "TestPrimer20");
        setFormElement(Locator.name("sequence"), "ATGATGATGGGGG");
        sleep(150); //there's a buffer when committing changes
        clickButton("Submit", 0);

        waitForElement(Ext4Helper.ext4Window("Success"));
        assertTextPresent("Your upload was successful");
        _oligosTotal++;
        clickButton("OK", 0);
        waitForPageToLoad();

        _helper.goToLabHome();
    }

    private void dnaOligosTableTest()
    {
        log("Testing DNA Oligos Table");
        _helper.goToLabHome();

        _helper.clickNavPanelItem("DNA_Oligos:", IMPORT_DATA_TEXT);
        waitForElement(Ext4Helper.ext4Window(IMPORT_DATA_TEXT));
        waitAndClick(Locator.ext4Button("Submit"));

        waitForElement(Locator.name("purification"));

        setFormElement(Locator.name("name"), "TestPrimer1");
        setFormElement(Locator.name("sequence"), "ABCDQ");
        setFormElement(Locator.name("oligo_type"), "Type1");
        sleep(150); //there's a buffer when committing changes
        clickButton("Submit", 0);

        //TODO: test field metadata, shownInInsertView, etc.
        //eventually test import views

        String errorMsg = "Sequence can only contain valid bases: ATGCN or IUPAC bases: RYSWKMBDHV";
        waitForElement(Ext4Helper.ext4Window("Error"));
        assertTextPresent(errorMsg);
        clickButton("OK", 0);

        _ext4Helper.clickTabContainingText("Import Spreadsheet");
        waitForText("Copy/Paste Data");
        setFormElementJS(Locator.name("text"), "Name\tSequence\nTestPrimer1\tatg\nTestPrimer2\tABCDEFG");
        click(Locator.ext4Button("Upload"));

        waitForElement(Ext4Helper.ext4Window("Error"));
        waitForText(errorMsg);
        clickButton("OK", 0);

        waitForText("There were errors in the upload:");

        assertTextPresent("Row 2:");
        assertTextPresent(errorMsg);

        String sequence = "tggGg gGAAAAgg";
        setFormElementJS(Locator.name("text"), "Name\tSequence\nTestPrimer1\tatg\nTestPrimer2\t" + sequence);
        clickButton("Upload", 0);
        _oligosTotal += 2;

        //TODO: import more data

        waitForElement(Ext4Helper.ext4Window("Success"));
        assertTextPresent("Success! 2 rows inserted.");
        clickButton("OK");

        //verify row imported
        _helper.goToLabHome();
        _helper.clickNavPanelItem("DNA_Oligos:", "Browse All");
        waitForPageToLoad();

        Assert.assertTrue("Sequence was not formatted properly on import", isTextPresent(sequence.toUpperCase().replaceAll(" ", "")));
        Assert.assertFalse("Sequence was not formatted properly on import", isTextPresent(sequence));
    }

    private void samplesTableTest()
    {
        log("Testing Samples Table");
        _helper.goToLabHome();

        _helper.clickNavPanelItem("Samples:", IMPORT_DATA_TEXT);
        waitForElement(Ext4Helper.ext4Window(IMPORT_DATA_TEXT));
        waitAndClick(Locator.ext4Button("Submit"));

        waitForElement(Locator.name("samplespecies"));

        verifyFreezerColOrder();

        _helper.setFormField("samplename", "SampleName");

        //verify drop down menus show correct text by spot checking several drop-downs
        //NOTE: trailing spaces are added by ext template
        _ext4Helper.selectComboBoxItem("Sample Type", "Cell Line");
        _ext4Helper.selectComboBoxItem("Sample Source", "DNA");
        _ext4Helper.selectComboBoxItem("Additive", "EDTA");
        _ext4Helper.selectComboBoxItem("Molecule Type", "vRNA");

        _helper.setFormField("samplespecies", "Species");

        assertElementNotPresent(Ext4Helper.invalidField());
        clickButton("Submit", 0);

        //test error conditions in trigger script
        waitForElement(Ext4Helper.ext4Window("Error"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Must enter either a location or freezer");
        clickButton("OK", 0);
        waitForElement(Ext4Helper.invalidField());

        _helper.setFormField("box_row", "-100");
        _helper.setFormField("location", "Location1");
        clickButton("Submit", 0);

        waitForElement(Ext4Helper.ext4Window("Error"));
        waitForText("Cannot have a negative value for box_row");
        clickButton("OK", 0);

        //test presence of UI to download multiple templates
        _ext4Helper.clickTabContainingText("Import Spreadsheet");
        waitForText("Copy/Paste Data");

        //we only care that these items are present
        _ext4Helper.selectComboBoxItem("Choose Template", "Default Template");
        _ext4Helper.selectComboBoxItem("Choose Template", "Cells Template");
        _ext4Helper.selectComboBoxItem("Choose Template", "DNA Samples Template");
    }

    /**
     * This is designed to be a general test of custom URLs, and also should verify that URLs in
     * dataRegions use the correct container when you display rows from multiple containers in the same grid.
     */
    private void urlGenerationTest() throws UnsupportedEncodingException
    {
        log("Testing DataRegion URL generation");
        _helper.goToLabHome();

        //insert dummy data:
        String[] workbookIds = new String[3];
        Integer i = 0;
        int max = 3;
        while (i < max)
        {
            String id = _helper.createWorkbook("Workbook" + i, "Description");
            workbookIds[i] = id;
            insertDummySampleRow(i.toString());
            i++;
        }

        _helper.goToLabHome();
        _helper.clickNavPanelItem("Samples:", "Browse All");
        waitForPageToLoad();
        DataRegionTable dr = new DataRegionTable("query", this);

        i = 0;
        while (i < max)
        {
            //first record for each workbook
            int rowNum = dr.getRow("Folder", "Workbook" + i);
            String workbook = getProjectName() + "/workbook-" + workbookIds[i];

            //NOTE: these URLs should point to the workbook where the record was created, not the current folder

            //details link
            String href = URLDecoder.decode(getAttribute(Locator.linkWithText("details", rowNum), "href"), "UTF-8");
            Assert.assertTrue("Expected [details] link to go to the container: " + workbook + ", href was: " + href,
                    href.contains(workbook));

            //update link
            href = URLDecoder.decode(getAttribute(Locator.linkWithText("edit", rowNum), "href"), "UTF-8");
            Assert.assertTrue("Expected [edit] link to go to the container: " + workbook + ", href was: " + href,
                    href.contains("/query/" + workbook + "/manageRecord.view?"));

            //sample source: this is the current container
            href = URLDecoder.decode(getAttribute(Locator.linkWithText("Whole Blood", rowNum), "href"), "UTF-8");
            Assert.assertTrue("Expected sample source column URL to go to the container: " + getProjectName() + ", href was: " + href,
                    href.contains("/query/" + getProjectName() + "/detailsQueryRow.view?schemaName=laboratory&query.queryName=sample_type&type=Whole Blood"));

            //sample type
            href = URLDecoder.decode(getAttribute(Locator.linkWithText("DNA", rowNum), "href"), "UTF-8");
            Assert.assertTrue("Expected sample type column URL to go to the container: " + getProjectName() + ", href was: " + href,
                    href.contains("/query/" + getProjectName() + "/detailsQueryRow.view?schemaName=laboratory&query.queryName=sample_type&type=DNA"));

            //container column
            href = URLDecoder.decode(getAttribute(Locator.linkWithText("Workbook" + i), "href"), "UTF-8");
            Assert.assertTrue("Expected container column to go to the container: " + workbook + ", href was:" + href,
                    href.contains("/project/" + workbook + "/start.view?"));

            i++;
        }

        //Test DetailsPanel:
        log("Testing details panel");
        dr.clickLink(1, 1);
        waitForPageToLoad();
        //waitForText("Back");
        assertTextPresent("Sample1", "DNA", "Whole Blood", "Freezer");

        verifyFreezerColOrder();
    }

    private void verifyFreezerColOrder()
    {
        //NOTE: we expect these columns to respect the order defined in the schema XML file
        log("Verifying freezer column order");
        assertTextBefore("Location", "Freezer");
        assertTextBefore("Freezer", "Cane");
        assertTextBefore("Cane", "Box");
        assertTextBefore("Box", "Row");
        assertTextBefore("Row", "Column");
        assertTextBefore("Column", "Parent Sample");
    }

    private  void insertDummySampleRow(String suffix)
    {
        Locator locator = _helper.toolIcon("Import Samples");
        waitForElement(locator);
        click(locator);
        //NOTE: we are in a workbook
        _ext4Helper.clickExt4MenuItem("Samples");
        waitForPageToLoad();

        waitForElement(Locator.name("freezer"));
        _helper.setFormField("samplename", "Sample" + suffix);
        _helper.setFormField("freezer", "freezer_" + _helper.getRandomInt());

        _ext4Helper.selectComboBoxItem("Sample Type", "DNA");
        _ext4Helper.selectComboBoxItem("Sample Source", "Whole Blood");

        sleep(150); //there's a buffer when committing changes
        clickButton("Submit", 0);

        waitForElement(Ext4Helper.ext4Window("Success"));
        assertTextPresent("Your upload was successful");
        _samplesTotal++;
        clickButton("OK", 0);
        waitForPageToLoad();

        _helper.goToLabHome();
    }

    private void peptideTableTest()
    {
        log("Testing Peptide Table");
        _helper.goToLabHome();

        _helper.clickNavPanelItem("Peptides:", IMPORT_DATA_TEXT);
        waitForElement(Ext4Helper.ext4Window(IMPORT_DATA_TEXT));
        waitAndClick(Locator.ext4Button("Submit"));

        waitForElement(Locator.name("sequence"));

        String sequence = "Sv LFpT LLF";
        String name = "Peptide 1"; //spaces should get replaced with '_' on import

        _helper.setFormField("sequence", sequence + "123");
        _helper.setFormField("name", name);
        sleep(150); //there's a buffer when committing changes
        clickButton("Submit", 0);

        //test error conditions in trigger script
        String errorMsg = "Sequence can only contain valid amino acid characters: ARNDCQEGHILKMFPSTWYV*";
        waitForElement(Ext4Helper.ext4Window("Error"));
        assertTextPresent(errorMsg);
        clickButton("OK", 0);

        _helper.setFormField("sequence", sequence);
        sleep(150); //there's a buffer when committing changes
        clickButton("Submit", 0);

        waitForElement(Ext4Helper.ext4Window("Success"));
        assertTextPresent("Your upload was successful");
        _peptideTotal = 1;
        clickButton("OK", 0);
        waitForPageToLoad();

        _helper.goToLabHome();
        _helper.clickNavPanelItem("Peptides:", "Browse All");
        waitForPageToLoad();

        Assert.assertTrue("Sequence was not formatted properly on import", isTextPresent(sequence.toUpperCase().replaceAll(" ", "")));
        Assert.assertFalse("Sequence was not formatted properly on import", isTextPresent(sequence));
        Assert.assertTrue("MW not set correctly", isTextPresent("1036.1"));
    }

    private void searchPanelTest()
    {
        _helper.goToLabHome();
        _helper.clickNavPanelItem("DNA_Oligos:", "Search");
        waitForPageToLoad();
        waitForTextToDisappear("Loading...");
        waitForElement(Locator.name("name"));
        sleep(50);
        _helper.setFormField("name", "TestPrimer");
        click(Locator.ext4Button("Submit"));
        waitForPageToLoad();
        DataRegionTable table = new DataRegionTable("query", this);
        Assert.assertEquals("Wrong number of rows found", _oligosTotal, table.getDataRowCount());

        //TODO: test different operators
        //also verify correct options show up on drop down menus


        //TODO: also verify records imported into workbook show up here.  verify lookups / view
    }

    private void samplesWebpartTest()
    {
        log("Testing samples webpart");

        clickTab("Materials");
        waitForPageToLoad();
        waitForText("Samples and Materials:");
        String msg = "Sample type missing or sample count incorrect";

        Assert.assertTrue(msg, isTextPresent("DNA_Oligos" + (_oligosTotal > 0 ? " (" + _oligosTotal + ")" : "") + ":"));
        Assert.assertTrue(msg, isTextPresent("Peptides" + (_peptideTotal > 0 ? " (" + _peptideTotal + ")" : "") + ":"));
        Assert.assertTrue(msg, isTextPresent("Samples" + (_samplesTotal > 0 ? " (" + _samplesTotal + ")" : "") + ":"));
    }

    private void queryMetadataTest()
    {
        //TODO: test URL generation, shownInInsertView, etc.
    }

    protected List<String> getEnabledModules()
    {
        List<String> modules = new ArrayList<String>();
        modules.add("Electrochemiluminescence");
        modules.add("ELISPOT_Assay");
        modules.add("FlowAssays");
        modules.add("GenotypeAssays");
        modules.add("SequenceAnalysis");
        modules.add("Viral_Load_Assay");
        return modules;
    }

    private List<String> getSampleItems()
    {
        List<String> list = new ArrayList<String>();
        list.add("DNA_Oligos");
        list.add("Peptides");
        list.add("Samples");
        return list;
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }

    private void defaultAssayImportMethodTest()
    {
        log("verifying ability to set default import method");
        _helper.goToLabHome();
        click(Locator.xpath("//a//span[text() = 'Settings']"));
        waitForPageToLoad();
        waitForText("Set Assay Defaults");
        _helper.clickNavPanelItem("Set Assay Defaults");
        waitForPageToLoad();
        String defaultVal = "LC480";
        _helper.waitForField(VIRAL_LOAD_ASSAYNAME);
        Ext4FieldRefWD.getForLabel(this, VIRAL_LOAD_ASSAYNAME).setValue(defaultVal);
        waitAndClick(Locator.ext4Button("Submit"));

        waitForElement(Ext4Helper.ext4Window("Success"));
        waitAndClick(Locator.ext4Button("OK"));
        waitForPageToLoad();
        waitForText("Types of Data");
        _helper.goToAssayResultImport(VIRAL_LOAD_ASSAYNAME);
        _helper.waitForField("Source Material");
        Boolean state = (Boolean)Ext4FieldRefWD.getForBoxLabel(this, defaultVal).getValue();
        Assert.assertTrue("Default method not correct", state);
        beginAt(getProjectUrl());
        Alert alert = _driver.switchTo().alert();
        alert.accept();
        waitForText(LabModuleHelper.LAB_HOME_TEXT);
    }

    @Override
    public boolean skipViewCheck()
    {
        //the module contains an R report tied to a specific assay name, so view check fails when an assay of that name isnt present
        //when module-based assays can supply reports this should be corrected
        return true;
    }
}
