package org.labkey.test.tests;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Specimen;
import org.labkey.test.components.BodyWebPart;
import org.labkey.test.components.IssueListDefDataRegion;
import org.labkey.test.components.html.Input;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@Category({DailyB.class})
public class CSISampleTest extends BaseWebDriverTest
{
    public static String FOLDER_NAME = "csi";
    public static String LIST_CELLLINES = "CellLines";
    public static String LIST_DEWARS="Dewars";
    public static String SAMPLESET_VIALGROUPS = "Vial Groups";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        CSISampleTest init = (CSISampleTest)getCurrentTest();

        init.doSetup();
    }

    @Test
    public void createVialGroup()
    {
        DataRegionTable sampleSetsTable = new DataRegionTable("SampleSet", getDriver());
        doAndWaitForPageToLoad(()->sampleSetsTable.findElement(Locator.linkWithText("Vial Groups")).click());

        clickButton("Create Vial Group");
        setFormElement(Locator.xpath("//input[@name='quf_Name']"), "Eddie's vials");
        selectOptionByText(Locator.xpath("//select[@name='quf_Workflow']"), "HeLa Study");
        selectOptionByText(Locator.xpath("//select[@name='quf_CellLine']"), "HeLa");
        setFormElement(Locator.xpath("//input[@name='quf_Passage']"),"1");
        clickButton("Submit");

        VialGroupDetailPane vialGroupDetailPane = new VialGroupDetailPane(getCurrentTest());
        vialGroupDetailPane.createVialGroup("BigBoy", "1", "1", "C","0","15");

        DataRegionTable dataRegionTable = vialGroupDetailPane.getDataTable();
        assertEquals(15L,dataRegionTable.getDataRowCount());

        for (int i=0; i<15; i++)
        {
            String index = Integer.toString(i);
            assertEquals("BigBoy", dataRegionTable.getDataAsText(i,"Dewar"));
            assertEquals("1", dataRegionTable.getDataAsText(i,"Rack"));
            assertEquals("1", dataRegionTable.getDataAsText(i,"Box"));
            assertEquals("C" + index, dataRegionTable.getDataAsText(i,"Slot"));
            assertEquals("1", dataRegionTable.getDataAsText(i,"Stored"));
            assertEquals(" ", dataRegionTable.getDataAsText(i, "Removed"));
            assertEquals(" ", dataRegionTable.getDataAsText(i, "Removed By"));
        }
    }

    @Test
    public void cannotRackVialsInUsedRackSpace()
    {
        DataRegionTable sampleSetsTable = new DataRegionTable("SampleSet", getDriver());
        doAndWaitForPageToLoad(()->sampleSetsTable.findElement(Locator.linkWithText("Vial Groups")).click());

        clickButton("Create Vial Group");
        setFormElement(Locator.xpath("//input[@name='quf_Name']"), "Joe's vials");
        selectOptionByText(Locator.xpath("//select[@name='quf_Workflow']"), "HeLa Study");
        selectOptionByText(Locator.xpath("//select[@name='quf_CellLine']"), "HeLa");
        setFormElement(Locator.xpath("//input[@name='quf_Passage']"),"1");
        clickButton("Submit");

        VialGroupDetailPane vialGroupDetailPane = new VialGroupDetailPane(getCurrentTest());
        vialGroupDetailPane.createVialGroup("BigBoy", "2", "2", "A","0","11");
        vialGroupDetailPane.createVialGroup("BigBoy", "2", "2", "A","10","10");
        assertTextPresent("There are existing vials in those locations.");

        // todo: verify conflict summary
    }

    @Test
    public void createVialGroupAndPullVials()
    {
        DataRegionTable sampleSetsTable = new DataRegionTable("SampleSet", getDriver());
        doAndWaitForPageToLoad(()->sampleSetsTable.findElement(Locator.linkWithText("Vial Groups")).click());

        clickButton("Create Vial Group");
        setFormElement(Locator.xpath("//input[@name='quf_Name']"), "Jeff's vials");
        selectOptionByText(Locator.xpath("//select[@name='quf_Workflow']"), "HeLa Study");
        selectOptionByText(Locator.xpath("//select[@name='quf_CellLine']"), "HeLa");
        setFormElement(Locator.xpath("//input[@name='quf_Passage']"),"1");
        clickButton("Submit");

        VialGroupDetailPane vialGroupDetailPane = new VialGroupDetailPane(getCurrentTest());
        vialGroupDetailPane.createVialGroup("LittleBoy", "1", "1", "B","0","10");

        DataRegionTable dataRegionTable = vialGroupDetailPane.getDataTable();
        assertEquals(10L,dataRegionTable.getDataRowCount());

        // pull all the vials
        dataRegionTable.checkAll();
        dataRegionTable.clickHeaderButtonByText("Pull Vial");
        sleep(1000); // give it a moment to refresh; todo: smarter wait

        // refresh
        dataRegionTable = vialGroupDetailPane.getDataTable();

        // confirm they're all no longer stored, removedBy == current user
        for (int i=0; i<10;i++)
        {
            assertEquals("0", dataRegionTable.getDataAsText(i,"Stored"));
            assertEquals(getCurrentUserName(), dataRegionTable.getDataAsText(i,"RemovedBy"));
        }
    }

    private void doSetup()
    {
        // create project
        _containerHelper.createProject(getProjectName(), "Custom");
        _containerHelper.createSubfolder(getProjectName(), "csi");
        _containerHelper.enableModules(Arrays.asList("csi_samples", "Issues"));

        // create issue list, call it "workflows"
        clickTab("Issues");
        clickButton("Manage Issue List Definitions");
        IssueListDefDataRegion issuesRegion = new IssueListDefDataRegion("query", getDriver());
        issuesRegion.createIssuesListDefinition("Workflows");
        clickAndWait(Locator.linkWithText("Workflows"), 1000);

        // admin-set singular/plural noun to 'workflow', set assignedTo field to 'all users', default to current user
        // todo: add users to 'all users' group
        issuesRegion.clickHeaderButtonByText("Admin");
        setFormElement(Locator.xpath("//input[@id='entrySingularName']"), "Workflow");
        setFormElement(Locator.xpath("//input[@id='entryPluralName']"), "Workflow");
        checkRadioButton(Locator.xpath("//label[contains(text(),'Specific Group')]/preceding-sibling::input[@name='assignedToMethod']"));;
        selectOptionByText(Locator.xpath("//select[@class='assigned-to-group']"), "Site:Users");
        checkRadioButton(Locator.xpath("//label[contains(text(),'Specific User')]/preceding-sibling::input[@name='assignedToUser']"));
        selectOptionByText(Locator.xpath("//select[@class='assigned-to-user']"), getCurrentUserName());
        clickButton("Save");

        // add an issue to the list
        clickButton("New Workflow");
        setFormElement(Locator.xpath("//input[@name='title']"), "HeLa Study");
        selectOptionByText(Locator.xpath("//select[@name='assignedTo']"), getCurrentUserName());
        selectOptionByText(Locator.xpath("//select[@name='type']"), "Operations");
        setFormElement(Locator.xpath("//textarea[@id='comment']"),"New study to isolate that one protein");
        clickButton("Save");
        clickTab("Portal");

        // configure the portal, add web parts
        clickTab("Portal");
        PortalHelper portalHelper = new PortalHelper(getDriver());
        portalHelper.addWebPart("Issues List");
        clickButton("Submit");
        portalHelper.addWebPart("Lists");

        createLists();
        createSampleSets();
    }

    private void createLists()
    {
        beginAt("/project/" + getProjectName() +"/"+ FOLDER_NAME + "/begin.view?");
        ListHelper listHelper = new ListHelper(getDriver());
        // cell lines
        listHelper.createList(FOLDER_NAME, LIST_CELLLINES, ListHelper.ListColumnType.AutoInteger, "CellLineId",
                new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String),
                new ListHelper.ListColumn("Description", "Description", ListHelper.ListColumnType.String));

        // dewars
        listHelper.createList(FOLDER_NAME, LIST_DEWARS, ListHelper.ListColumnType.AutoInteger, "DewarId",
                new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String),
                new ListHelper.ListColumn("Description", "Description", ListHelper.ListColumnType.String));
        clickButton("Done");
        clickTab("Portal");

        clickAndWait(Locator.linkWithText(LIST_CELLLINES));
        listHelper = new ListHelper(getDriver());
        log("adding rows to CellLines list");
        Map heLaRow = new HashMap<>();
        heLaRow.put("Name","HeLa");
        heLaRow.put("Description", "immortal cancer cell line");
        listHelper.insertNewRow(heLaRow);
        Map choRow = new HashMap<>();
        choRow.put("Name", "CHO-xyz");
        choRow.put("Description", "Everyone's favorite chinese hamster ovary cell line");
        listHelper.insertNewRow(choRow);
        clickTab("Portal");

        clickAndWait(Locator.linkWithText(LIST_DEWARS));
        listHelper = new ListHelper(getDriver());
        log("adding rows to Dewars list");
        Map bigBoyRow = new HashMap<>();
        bigBoyRow.put("Name","BigBoy");
        bigBoyRow.put("Description", "Liquid Hydrogen cooled vacuum flask");
        listHelper.insertNewRow(bigBoyRow);
        Map littleBeakerRow = new HashMap<>();
        littleBeakerRow.put("Name","LittleBoy");
        littleBeakerRow.put("Description", "Liquid Helium cooled vacuum flask");
        listHelper.insertNewRow(littleBeakerRow);
    }

    private void createSampleSets()
    {
        clickTab("Portal");

        PortalHelper portalHelper = new PortalHelper(getDriver());
        portalHelper.addWebPart("Sample Sets");

        log("adding vial groups sample set");
        String vialSetsData = "Name\tWorkflow\tCellLine\tPassage\n" +
                "ed\t1\t1\t1\n";
        DataRegionTable sampleSetsTable = new DataRegionTable("SampleSet", getDriver());
        sampleSetsTable.clickHeaderButtonByText("Import Sample Set");
        setFormElement(Locator.xpath("//input[@id='name']"), "Vial Groups");
        setFormElement(Locator.xpath("//textarea[@id='textbox']"), vialSetsData);

        // click/event outside textbox to get meta rows
        // set id row
        selectOptionByText(Locator.xpath("//select[@id='idCol1']"), "Name");
        // leave parent empty
        clickButton("Submit");

        log("editing fields in Vial Groups");
        ListHelper vialGroupsFieldEditHelper = new ListHelper(getDriver());
        vialGroupsFieldEditHelper.clickEditFields();
        vialGroupsFieldEditHelper.setColumnLabel(0, "parent");
        vialGroupsFieldEditHelper.setColumnType(0, new ListHelper.LookupInfo("/" + getProjectName() + "/" + FOLDER_NAME, "issues", "workflows" ));
        vialGroupsFieldEditHelper.setColumnType(1, new ListHelper.LookupInfo(null, "lists", "CellLines"));
        vialGroupsFieldEditHelper.setColumnType(2, ListHelper.ListColumnType.Integer);
        clickButton("Save");

        clickTab("Portal");

        log("adding vials sample set");
        String vialsData = "Name\tParent\tDewar\tRack\tBox\tSlot\tStored\tRemoved\tRemovedBy\tLocationPath\n" +
                "ed\t\t1\t1\t1\tA\t0\t2016-1-1\t1\there\n" +
                "billy\t\t1\t1\t1\tB\t0\t2016-1-1\t1\there\n";
        sampleSetsTable = new DataRegionTable("SampleSet", getDriver());
        sampleSetsTable.clickHeaderButtonByText("Import Sample Set");
        setFormElement(Locator.xpath("//input[@id='name']"), "Vials");
        setFormElement(Locator.xpath("//textarea[@id='textbox']"), vialsData);

        selectOptionByText(Locator.xpath("//select[@id='idCol1']"), "Name");
        selectOptionByText(Locator.xpath("//select[@id='parentCol']"), "Parent");
        clickButton("Submit");

        log("editing fields in Vials sample set");
        ListHelper fieldEditHelper = new ListHelper(getDriver());
        fieldEditHelper.clickEditFields();
        fieldEditHelper.setColumnLabel(0, "parent");
        fieldEditHelper.setColumnType(0, new ListHelper.LookupInfo(null, "samples", SAMPLESET_VIALGROUPS ));
        fieldEditHelper.setColumnType(1, new ListHelper.LookupInfo(null, "lists", LIST_DEWARS ));
        fieldEditHelper.setColumnType(7, ListHelper.ListColumnType.User);
        clickButton("Save");
        clickTab("Portal");
    }

    @Before
    public void preTest()
    {
        beginAt("/project/" + getProjectName() +"/"+ FOLDER_NAME + "/begin.view?");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return "CSISampleTest";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("issues", "csi_samples");
    }

    // wraps the vial group panel
    public class VialGroupDetailPane extends BodyWebPart
    {
        private static final String DEFAULT_TITLE = "Vial Group Detail";

        public VialGroupDetailPane(BaseWebDriverTest test)
        {
            this(test, DEFAULT_TITLE);
        }
        public VialGroupDetailPane(BaseWebDriverTest test, String title)
        {
            super(test, title);
        }

        public void createVialGroup(String dewarName, String rackNumber, String boxNumber, String rowLetter, String startIndex, String count)
        {
            selectOptionByText(elements().dewarSelect, dewarName);
            elements().rackInput.set(rackNumber);
            elements().boxInput.set(boxNumber);
            elements().rowInput.set(rowLetter);
            elements().startInput.set(startIndex);
            elements().countInput.set(count);
            elements().createButton.click();
        }

        public DataRegionTable getDataTable()
        {
            return new DataRegionTable(
                    new LazyWebElement(Locator.xpath("//table[contains(@lk-region-name, 'vials-')]"), this),
                    getDriver());
        }

        @Override
        protected Elements elements()
        {
            return new Elements();
        }

        private class Elements extends BodyWebPart.Elements
        {
            WebElement dewarSelect = new LazyWebElement(Locator.xpath("//select[@class='dewarList']"), this);
            Input rackInput = new Input(
                    new LazyWebElement(Locator.xpath("//input[@name='Rack']"),this),
                    getDriver());
            Input boxInput = new Input(
                    new LazyWebElement(Locator.xpath("//input[@name='Box']"),this),
                    getDriver());
            Input rowInput = new Input(
                    new LazyWebElement(Locator.xpath("//input[@name='Row']"),this),
                    getDriver());
            Input startInput = new Input(
                    new LazyWebElement(Locator.xpath("//input[@name='Start']"),this),
                    getDriver());
            Input countInput = new Input(
                    new LazyWebElement(Locator.xpath("//input[@name='Count']"),this),
                    getDriver());
            WebElement createButton = new LazyWebElement(Locator.xpath("//button[@class='createButton']"), this);
        }
    }
}
