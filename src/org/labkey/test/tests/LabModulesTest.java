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
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.AdvancedSqlTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LabModuleHelper;
import org.labkey.test.util.UIContainerHelper;

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
public class LabModulesTest extends BaseSeleniumWebTest implements AdvancedSqlTest
{
    protected LabModuleHelper _helper = new LabModuleHelper(this);
    private int _oligosTotal = 0;
    private int _samplesTotal = 0;
    private int _peptideTotal = 0;

    private String IMPORT_DATA_TEXT = "Import Data";

    @Override
    protected String getProjectName()
    {
        return "LaboratoryVerifyProject" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    }

    public LabModulesTest()
    {
        setContainerHelper(new UIContainerHelper(this));
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        setUpTest();
        goToProjectHome();
        overviewUITest();
        labToolsWebpartTest();
        workbookCreationTest();
        dnaOligosTableTest();
        samplesTableTest();
        urlGenerationTest();
        peptideTableTest();
        searchPanelTest();
        queryMetadataTest();
    }

    private void setUpTest() throws Exception
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

    private List<Pair<String, String>> getAssaysToCreate()
    {
        List<Pair<String, String>> assays = new ArrayList<Pair<String, String>>();
        assays.add(Pair.of("Immunophenotyping", "TruCount"));
        assays.add(Pair.of("SSP Typing", "MHC_SSP"));
        assays.add(Pair.of("Viral Loads", "Viral_Load"));

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

    private void labToolsWebpartTest()
    {
        waitAndClick(Locator.xpath("//div[contains(@class, 'tool-icon')]//span[text() = 'Import Data']"));
        assertElementPresent(Ext4Helper.ext4MenuItem("Sequence"));
        for(Pair<String, String> pair : getAssaysToCreate())
        {
            assertElementPresent(Ext4Helper.ext4MenuItem(pair.getValue()));
        }

        waitAndClick(Locator.xpath("//div[contains(@class, 'tool-icon')]//span[text() = 'Import Samples']"));
        for (String s : getSampleItems())
        {
            assertElementPresent(Ext4Helper.ext4MenuItem(s));
        }

        //verify settings hidden for readers
        assertElementPresent(Locator.xpath("//div[contains(@class, 'tool-icon') and contains(@class, 'x4-icon-text-left')]//span[text() = 'Settings']"));
        impersonateRole("Reader");
        _helper.goToLabHome();
        assertElementNotPresent(Locator.xpath("//div[contains(@class, 'tool-icon') and contains(@class, 'x4-icon-text-left')]//span[text() = 'Settings']"));
        stopImpersonatingRole();
        goToProjectHome();

    }

    private void workbookCreationTest()
    {
        _helper.goToLabHome();

        _helper.clickNavPanelItem("View and Edit Workbooks:", "Create New Workbook");
        waitForElement(Ext4Helper.ext4Window("Create Workbook"));
        assertTextNotPresent("Add To Existing Workbook");
        waitForElement(Locator.ext4Button("Close"));
        click(Locator.ext4Button("Close"));
        assertElementNotPresent(Ext4Helper.ext4Window("Create Workbook"));

        String workbookTitle = "NewWorkbook_" + INJECT_CHARS_1;
        String workbookDescription = "I am a workbook.  I am trying to inject javascript into your page.  " + INJECT_CHARS_1 + INJECT_CHARS_2;
        _helper.createWorkbook(workbookTitle, workbookDescription);

        //verify correct name and correct webparts present
        assertElementPresent(_helper.webpartTitle("Lab Tools"));
        assertElementPresent(_helper.webpartTitle("Files"));
        assertElementPresent(_helper.webpartTitle("Experiment Runs"));

        //we expect insert from within the workbook to go straight to the import page (unlike the top-level folder, which gives a dialog)
        waitAndClick(Locator.xpath("//div[contains(@class, 'tool-icon')]//span[text() = 'Import Samples']"));
        for (String s : getSampleItems())
        {
            assertElementPresent(Ext4Helper.ext4MenuItem(s));
        }
        //NOTE: we are in a workbook here
        _ext4Helper.clickExt4MenuItem("DNA_Oligos");
        waitForPageToLoad();
        waitForElement(Locator.name("name"));
        waitForElement(Locator.name("purification"));

        setText("name", "TestPrimer20");
        setText("sequence", "ATGATGATGGGGG");
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

        setText("name", "TestPrimer1");
        setText("sequence", "ABCDQ");
        setText("oligo_type", "Type1");
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
        setText("text", "Name\tSequence\nTestPrimer1\tatg\nTestPrimer2\tABCDEFG");
        click(Locator.ext4Button("Upload"));

        waitForElement(Ext4Helper.ext4Window("Error"));
        waitForText(errorMsg);
        clickButton("OK", 0);

        waitForText("There were errors in the upload:");

        assertTextPresent("Row 2:");
        assertTextPresent(errorMsg);

        String sequence = "tggGg gGAAAAgg";
        setFormElement("text", "Name\tSequence\nTestPrimer1\tatg\nTestPrimer2\t" + sequence);
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
        _helper.setFormField("samplespecies", "Species");

        //verify drop down menus show correct text by spot checking several drop-downs
        //NOTE: trailing spaces are added by ext template
        _ext4Helper.selectComboBoxItem("Sample Type", "Cell Line");
        _ext4Helper.selectComboBoxItem("Sample Source", "DNA");
        _ext4Helper.selectComboBoxItem("Additive", "EDTA");
        _ext4Helper.selectComboBoxItem("Molecule Type", "vRNA");

        assertElementNotPresent(Ext4Helper.invalidField());
        fireEvent(Locator.name("samplename"), SeleniumEvent.blur);
        clickButton("Submit", 0);

        //test error conditions in trigger script
        waitForElement(Ext4Helper.ext4Window("Error"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Must enter either a location or freezer");
        clickButton("OK", 0);
        waitForElement(Ext4Helper.invalidField());

        //TODO: why doesnt this work?
//        _helper.setFormField("box_row", "-100");
//        _helper.setFormField("freezer", "Freezer1");
//        sleep(50);
//        clickButton("Submit", 0);
//
//        waitForElement(Ext4Helper.ext4Window("Error"));
//        assertTextPresent("Cannot have a negative value for row");
//        clickButton("OK", 0);


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
        waitForText("Back");
        assertTextPresent("Sample1", "DNA", "Whole Blood", "Freezer:");

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
        Locator locator = Locator.xpath("//div[contains(@class, 'tool-icon')]//span[text() = 'Import Samples']");
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
        modules.add("FlowAssays");
        modules.add("GenotypeAssays");
        modules.add("SequenceAnalysis");
        modules.add("SSP_Assay");
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
    protected void doCleanup(boolean afterTest) throws Exception
    {
        try
        {
            deleteProject(getProjectName());
        }
        catch (Throwable t)
        {
            //ignore
        }
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return null;
    }

}
