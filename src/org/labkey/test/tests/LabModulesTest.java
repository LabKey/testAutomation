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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    @Override
    protected String getProjectName()
    {
        return "LaboratoryVerifyProject" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES; //INJECT_CHARS_1;
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
//        samplesWebpartTest();
    }

    private void setUpTest() throws Exception
    {
        _containerHelper.createProject(getProjectName(), "Laboratory Folder");
        enableModules(getEnabledModules(), true);

        //insert initial values into tables
        waitForElement(Locator.xpath("//img[@src='" + getContextPath() + "/study/tools/settings.png']"));
        clickLink(Locator.xpath("//img[@src='" + getContextPath() + "/study/tools/settings.png']"));
        clickLinkWithText("Initialize Module");
        click(Locator.extButton("Delete All"));
        waitForText("Delete Complete");
        click(Locator.extButton("Populate All"));
        waitForText("Insert Complete");

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

        _helper.verifyNavPanelRowItemPresent("Sequence Data:");

        for(Pair<String, String> pair : getAssaysToCreate())
        {
            _helper.verifyNavPanelRowItemPresent(pair.getValue() + " Data:");
            assertElementNotPresent(LabModuleHelper.getNavPanelItem(pair.getValue() + " Data:", "Import Data"));
        }

        _helper.verifyNavPanelRowItemPresent("DNA_Oligos:");
        assertElementNotPresent(LabModuleHelper.getNavPanelItem("DNA_Oligos:", "Import Data"));

        _helper.verifyNavPanelRowItemPresent("Peptides:");
        assertElementNotPresent(LabModuleHelper.getNavPanelItem("Peptides:", "Import Data"));

        _helper.verifyNavPanelRowItemPresent("Samples:");
        assertElementNotPresent(LabModuleHelper.getNavPanelItem("Samples:", "Import Data"));

        //now try UI will normal permissions
        stopImpersonatingRole();
        _helper.goToLabHome();

        _helper.clickNavPanelItem("Sequence Data:", "Import Data");
        assertElementPresent(Ext4Helper.ext4MenuItem("Import Sequence Files"));
        Ext4Helper.clickExt4MenuItem(this, "Import Readsets");
        waitForElement(Ext4Helper.ext4Window("Import Sequence Data"));
        waitForElement(Locator.ext4Button("Close"));
        click(Locator.ext4Button("Close"));

    }

    private void labToolsWebpartTest()
    {
        click(Locator.xpath("//div[contains(@class, 'tool-icon')]//span[text() = 'Import Data']"));
        assertElementPresent(Ext4Helper.ext4MenuItem("Sequence Data"));
        for(Pair<String, String> pair : getAssaysToCreate())
        {
            assertElementPresent(Ext4Helper.ext4MenuItem(pair.getValue()));
        }

        click(Locator.xpath("//div[contains(@class, 'tool-icon')]//span[text() = 'Import Samples']"));
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
        Ext4Helper.clickExt4MenuItem(this, "DNA_Oligos");
        waitForPageToLoad();
        waitForElement(Locator.name("name"));

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

        _helper.clickNavPanelItem("DNA_Oligos:", "Import Data");
        waitForElement(Locator.xpath("//input[contains(@class, 'x4-form-text')]"));

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

        Ext4Helper.clickTabContainingText(this, "Import Spreadsheet");
        waitForText("Copy/Paste Data");
        setText("text", "Name\tSequence\nTestPrimer1\tatg\nTestPrimer2\tABCDEFG");
        click(Locator.ext4Button("Upload"));

        Ext4Helper.waitForMaskToDisappear(this);
        waitForText("There were errors in the upload:");

        waitForElement(Ext4Helper.ext4Window("Error"));
        assertTextPresent(errorMsg);
        clickButton("OK", 0);

        assertTextPresent("Row 2:");
        assertTextPresent(errorMsg);

        String sequence = "tggGg gGAAAAgg";
        setFormElement("text", "Name\tSequence\nTestPrimer1\tatg\nTestPrimer2\t" + sequence);
        clickButton("Upload", 0);
        _oligosTotal += 2;

        //TODO: import more data

        Ext4Helper.waitForMaskToDisappear(this);
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

        _helper.clickNavPanelItem("Samples:", "Import Data");
        waitForElement(Locator.xpath("//input[contains(@class, 'x4-form-text')]"));

        setText("samplename", "SampleName");
        setText("samplespecies", "Species");

        //verify drop down menus show correct text by spot checking several drop-downs
        //NOTE: trailing spaces are added by ext template
        Ext4Helper.selectComboBoxItem(this, "Sample Type", "Cell Line");
        Ext4Helper.selectComboBoxItem(this, "Sample Source", "DNA");
        Ext4Helper.selectComboBoxItem(this, "Additive", "EDTA");
        Ext4Helper.selectComboBoxItem(this, "Molecule Type", "vRNA");

        assertElementNotPresent(Ext4Helper.invalidField());
        fireEvent(Locator.name("samplename"), SeleniumEvent.blur);
        clickButton("Submit", 0);

        //test error conditions in trigger script
        waitForElement(Ext4Helper.ext4Window("Error"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Must enter either a location or freezer");
        clickButton("OK", 0);
        waitForElement(Ext4Helper.invalidField());

        //TODO: why doesnt this work?
//        setText("box_row", "-100");
//        setText("freezer", "Freezer1");
//        sleep(50);
//        clickButton("Submit", 0);
//
//        waitForElement(Ext4Helper.ext4Window("Error"));
//        assertTextPresent("Cannot have a negative value for row");
//        clickButton("OK", 0);


        //test presence of UI to download multiple templates
        Ext4Helper.clickTabContainingText(this, "Import Spreadsheet");
        waitForText("Copy/Paste Data");

        //we only care that these items are present
        Ext4Helper.selectComboBoxItem(this, "Choose Template", "Default Template");
        Ext4Helper.selectComboBoxItem(this, "Choose Template", "Cells Template");
        Ext4Helper.selectComboBoxItem(this, "Choose Template", "DNA Samples Template");
    }

    /**
     * This is designed to be a general test of custom URLs, and also should verify that URLs in
     * dataRegions use the correct container when you display rows from multiple containers in the same grid.
     */
    private void urlGenerationTest() throws UnsupportedEncodingException
    {
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
            String container = getProjectName() + "/workbook-" + workbookIds[i];

//TODO: most of these links are broken due to Issue 15829
//            //details link
//            String href = URLDecoder.decode(getAttribute(Locator.linkWithText("details", rowNum), "href"), "UTF-8");
//            Assert.assertTrue("Expected [details] link to go to " + getProjectName() + " container, href=" + href,
//                    href.contains("/query/" + getProjectName() + "/recordDetails.view?"));
//
//            //update link
//            href = URLDecoder.decode(getAttribute(Locator.linkWithText("edit", rowNum), "href"), "UTF-8");
//            Assert.assertTrue("Expected [edit] link to go to " + getProjectName() + " container, href=" + href,
//                    href.contains("/query/" + container + "/manageRecord.view?"));
//
//            //sample source
//            href = URLDecoder.decode(getAttribute(Locator.linkWithText("Blood", rowNum), "href"), "UTF-8");
//            Assert.assertTrue("Expected sample source column URL to go to " + getProjectName() + " container, href=" + href,
//                    href.contains("/query/" + container + "/detailsQueryRow.view?schemaName=laboratory&query.queryName=sample_source?"));
//
//            //sample type
//            href = URLDecoder.decode(getAttribute(Locator.linkWithText("DNA", rowNum), "href"), "UTF-8");
//            Assert.assertTrue("Expected sample type column URL to go to " + getProjectName() + " container, href=" + href,
//                    href.contains("/query/" + container + "/detailsQueryRow.view?schemaName=laboratory&query.queryName=sample_type?"));
//
//            //container column
//            href = URLDecoder.decode(getAttribute(Locator.linkWithText("Workbook" + i), "href"), "UTF-8");
//            Assert.assertTrue("Expected container column to go to " + getProjectName() + " container, href=" + href,
//                    href.contains("/project/" + container + "/start.view?"));
//
            i++;
        }

        //TODO: click through on DetailsURL and test detailsPanel
    }

    private  void insertDummySampleRow(String suffix)
    {
        Locator locator = Locator.xpath("//div[contains(@class, 'tool-icon')]//span[text() = 'Import Samples']");
        waitForElement(locator);
        click(locator);
        Ext4Helper.clickExt4MenuItem(this, "Samples");
        waitForPageToLoad();
        waitForElement(Locator.xpath("//input[contains(@class, 'x4-form-text')]"));
        setText("samplename", "Sample" + suffix);
        setText("freezer", "freezer_" + _helper.getRandomInt());

        Ext4Helper.selectComboBoxItem(this, "Sample Type", "DNA");
        Ext4Helper.selectComboBoxItem(this, "Sample Source", "Blood");

        sleep(150); //there's a buffer when committing changes
        clickButton("Submit", 0);

        waitForElement(Ext4Helper.ext4Window("Success"));
        assertTextPresent("Your upload was successful");
        _samplesTotal++;
        clickButton("OK", 0);
        waitForPageToLoad();

        _helper.goToLabHome();

//        try
//        {
//            Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
//            InsertRowsCommand insertCmd = new InsertRowsCommand("laboratory", "samples");
//            Map<String,Object> rowMap = new HashMap<String,Object>();
//            rowMap.put("samplename", "Sample" + suffix);
//            rowMap.put("freezer", "freezer");
//            rowMap.put("sampletype", "DNA");
//            rowMap.put("samplesource", "Blood");
//
//            insertCmd.addRow(rowMap);
//            insertCmd.execute(cn, containerPath);
//        }
//        catch (CommandException e)
//        {
//            throw new RuntimeException(e);
//        }
//        catch (IOException e)
//        {
//            throw new RuntimeException(e);
//        }
    }

    private void peptideTableTest()
    {
        log("Testing Peptide Table");
        _helper.goToLabHome();

        _helper.clickNavPanelItem("Peptides:", "Import Data");
        waitForElement(Locator.xpath("//input[contains(@class, 'x4-form-text')]"));

        String sequence = "Sv LFpT LLF";
        String name = "Peptide 1"; //spaces should get replaced with '_' on import

        setText("sequence", sequence + "123");
        setText("name", name);
        sleep(150); //there's a buffer when committing changes
        clickButton("Submit", 0);

        //test error conditions in trigger script
        String errorMsg = "Sequence can only contain valid amino acid characters: ARNDCQEGHILKMFPSTWYV*";
        waitForElement(Ext4Helper.ext4Window("Error"));
        assertTextPresent(errorMsg);
        clickButton("OK", 0);

        setText("sequence", sequence);
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
        sleep(50);
        setText("name", "TestPrimer");
        click(Locator.ext4Button("Submit"));
        waitForPageToLoad();
        DataRegionTable table = new DataRegionTable("query", this);
        Assert.assertEquals("Wrong number of rows found", _oligosTotal, table.getDataRowCount());

        //TODO: test different operators
        //also verify correct options show up on drop down menus


        //TODO: also verify records imported into workbook show up here.  verify lookups / view
    }

//    private void samplesWebpartTest()
//    {
//        log("Testing samples webpart");
//
//        clickTab("Materials");
//        waitForPageToLoad();
//        waitForText("Samples and Materials:");
//        String msg = "Sample type missing or sample count incorrect";
//
//        Assert.assertTrue(msg, isTextPresent("DNA_Oligos" + (_oligosTotal > 0 ? " (" + _oligosTotal + ")" : "") + ":"));
//        Assert.assertTrue(msg, isTextPresent("Peptides" + (_peptideTotal > 0 ? " (" + _peptideTotal + ")" : "") + ":"));
//        Assert.assertTrue(msg, isTextPresent("Samples" + (_samplesTotal > 0 ? " (" + _samplesTotal + ")" : "") + ":"));
//    }

    private void queryMetadataTest()
    {
        //TODO: test URL generation, shownInInsertView, etc.
    }

    protected List<String> getEnabledModules()
    {
        List<String> modules = new ArrayList<String>();
        modules.add("Immunophenotype_Assay");
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
    protected void doCleanup() throws Exception
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
