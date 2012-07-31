package org.labkey.test.tests;

import org.apache.commons.lang3.tuple.Pair;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LabModuleHelper;

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
public class LabModulesTest extends BaseSeleniumWebTest
{
    private String pipelineLoc =  getLabKeyRoot() + "/sampledata/sequence";
    private LabModuleHelper _helper = new LabModuleHelper(this);
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
        overviewUITest();
        labToolsWebpartTest();
        workbookCreationTest();
        dnaOligosTableTest();
        samplesTableTest();
        peptideTableTest();
        searchPanelTest();
        queryMetadataTest();
        samplesWebpartTest();
    }

    private void setUpTest() throws Exception
    {
        _containerHelper.createProject(getProjectName(), "Lab");
        enableModules(getEnabledModules(), true);

        //TODO: if we have a better folder type this is not needed
        addWebPart("Laboratory Home");
        addWebPart("Lab Tools");

        waitForElement(Locator.xpath("//img[@src='" + getContextPath() + "/study/tools/settings.png']"));
        clickLink(Locator.xpath("//img[@src='" + getContextPath() + "/study/tools/settings.png']"));
        clickLinkWithText("Initialize Module");

        click(Locator.extButton("Delete All"));
        waitForText("Delete Complete");
        click(Locator.extButton("Populate All"));
        waitForText("Insert Complete");

        clickTab("Materials");
        addWebPart("Samples and Materials");

        setupAssays();
        goToProjectHome();
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
            assertElementNotPresent(_helper.getNavPanelItem(pair.getValue() + " Data:", "Import Data"));
        }

        _helper.verifyNavPanelRowItemPresent("DNA_Oligos:");
        assertElementNotPresent(_helper.getNavPanelItem("DNA_Oligos:", "Import Data"));

        _helper.verifyNavPanelRowItemPresent("Peptides:");
        assertElementNotPresent(_helper.getNavPanelItem("Peptides:", "Import Data"));

        _helper.verifyNavPanelRowItemPresent("Samples:");
        assertElementNotPresent(_helper.getNavPanelItem("Samples:", "Import Data"));

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

        clickTab("Workbooks");
        clickButton("Create New Workbook", 0);
        waitForElement(Ext4Helper.ext4Window("Create Workbook"));
        String workbookTitle = "NewWorkbook_" + INJECT_CHARS_1;
        String workbookDescription = "I am a workbook.  I am trying to inject javascript into your page.  " + INJECT_CHARS_1 + INJECT_CHARS_2;
        setText("title", workbookTitle);
        setText("description", workbookDescription);
        clickButton("Submit");
        waitForPageToLoad();

        //verify correct name and correct webparts present
        assertElementPresent(_helper.webpartTitle("Lab Tools"));
        assertElementPresent(_helper.webpartTitle("Files"));
        assertElementPresent(_helper.webpartTitle("Experiment Runs"));

        //TODO: try inserts.  b/c we're in the workbook we should not get the import dialog


        goToProjectHome();
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
        _oligosTotal = 2;

        //TODO: import more data

        Ext4Helper.waitForMaskToDisappear(this);
        waitForElement(Ext4Helper.ext4Window("Success"));
        assertTextPresent("Success! 2 rows inserted.");
        clickButton("OK");

        //verify row imported
        _helper.goToLabHome();
        _helper.clickNavPanelItem("DNA_Oligos:", "Browse All");
        waitForPageToLoad();

        assertTrue("Sequence was not formatted properly on import", isTextPresent(sequence.toUpperCase().replaceAll(" ", "")));
        assertFalse("Sequence was not formatted properly on import", isTextPresent(sequence));
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
        clickButton("Submit", 0);

        //test error conditions in trigger script
        waitForElement(Ext4Helper.ext4Window("Error"));
        assertTextPresent("Must enter either a location or freezer");
        clickButton("OK", 0);
        waitForElement(Ext4Helper.invalidField());

        //TODO
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

        //TODO: test details URL / detailsPanel
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

        assertTrue("Sequence was not formatted properly on import", isTextPresent(sequence.toUpperCase().replaceAll(" ", "")));
        assertFalse("Sequence was not formatted properly on import", isTextPresent(sequence));
        assertTrue("MW not set correctly", isTextPresent("1036.1"));
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
        assertTrue("Wrong number of rows found", table.getDataRowCount() == _oligosTotal);

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

        assertTrue(msg, isTextPresent("DNA_Oligos" + (_oligosTotal > 0 ? " (" + _oligosTotal + ")" : "") + ":"));
        assertTrue(msg, isTextPresent("Peptides" + (_peptideTotal > 0 ? " (" + _peptideTotal + ")" : "") + ":"));
        assertTrue(msg, isTextPresent("Samples" + (_samplesTotal > 0 ? " (" + _samplesTotal + ")" : "") + ":"));
    }

    private void queryMetadataTest()
    {
        //TODO: test URL generation, shownInInsertView, etc.
    }

    protected List<String> getEnabledModules()
    {
        List<String> modules = new ArrayList<String>();
        modules.add("Laboratory");
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
