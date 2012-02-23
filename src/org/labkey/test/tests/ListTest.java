/*
 * Copyright (c) 2007-2012 LabKey Corporation
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
import org.labkey.test.SortDirection;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.CustomizeViewsHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.ListHelper.ListColumn;
import org.labkey.test.util.ListHelper.LookupInfo;
import static org.labkey.test.util.ListHelper.ListColumnType.*;

import java.io.File;
import java.util.List;
import java.util.Arrays;

/**
 * User: ulberge
 * Date: Jul 13, 2007
 */
public class ListTest extends BaseSeleniumWebTest
{
    protected final static String PROJECT_NAME = "ListVerifyProject" + TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    private final static String PROJECT_NAME2 = "OtherListVerifyProject";
    protected final static String LIST_NAME = TRICKY_CHARACTERS_NO_QUOTES + "Colors";
    protected final static ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.String;
    protected final static String LIST_KEY_NAME = "Key";
    protected final static String LIST_KEY_NAME2 = "Color";
    protected final static String LIST_DESCRIPTION = "A list of colors and what they are like";
    protected final static String FAKE_COL1_NAME = "FakeName";
    protected final static String ALIASED_KEY_NAME = "Material";
    protected final static String HIDDEN_TEXT = "Hidden";
    protected ListColumn _listCol1Fake = new ListColumn(FAKE_COL1_NAME, FAKE_COL1_NAME, ListHelper.ListColumnType.String, "What the color is like");
    protected ListColumn _listCol1 = new ListColumn("Desc", "Description", ListHelper.ListColumnType.String, "What the color is like");
    protected final ListColumn _listCol2 = new ListColumn("Month", "Month to Wear", ListHelper.ListColumnType.DateTime, "When to wear the color", "M");
    protected final ListColumn _listCol3 = new ListColumn("JewelTone", "Jewel Tone", ListHelper.ListColumnType.Boolean, "Am I a jewel tone?");
    protected final ListColumn _listCol4 = new ListColumn("Good", "Quality", ListHelper.ListColumnType.Integer, "How nice the color is");
    protected final ListColumn _listCol5 = new ListColumn("HiddenColumn", HIDDEN_TEXT, ListHelper.ListColumnType.String, "I should be hidden!");
    protected final ListColumn _listCol6 = new ListColumn("AliasedColumn", "Element", ListHelper.ListColumnType.String, "I show aliased data.");
    protected final static String[][] TEST_DATA = {
            { "Blue", "Green", "Red", "Yellow" },
            { "Light", "Mellow", "Robust", "Zany" },
            { "true", "false", "true", "false"},
            { "1", "4", "3", "2" },
            { "10", "9", "8", "7"},
            { "Water", "Earth", "Fire", "Air"}};
    private final static String[] CONVERTED_MONTHS = { "2000-01-01", "2000-04-04", "2000-03-03", "2000-02-02" };
    private final static String LIST_ROW1 = TEST_DATA[0][0] + "\t" + TEST_DATA[1][0] + "\t" + TEST_DATA[2][0] + "\t" + CONVERTED_MONTHS[0];
    private final static String LIST_ROW2 = TEST_DATA[0][1] + "\t" + TEST_DATA[1][1] + "\t" + TEST_DATA[2][1] + "\t" + CONVERTED_MONTHS[1];
    private final static String LIST_ROW3 = TEST_DATA[0][2] + "\t" + TEST_DATA[1][2] + "\t" + TEST_DATA[2][2] + "\t" + CONVERTED_MONTHS[2];
    private final String LIST_DATA = LIST_KEY_NAME2 + "\t" + FAKE_COL1_NAME +
            "\t" + _listCol3.getName() + "\t" + _listCol2.getName() + "\n" + LIST_ROW1 + "\n" + LIST_ROW2 + "\n" + LIST_ROW3;
    private final String LIST_DATA2 =
            LIST_KEY_NAME2 + "\t" + _listCol4.getName() + "\t" + ALIASED_KEY_NAME + "\t" + _listCol5.getName() + "\n" +
            TEST_DATA[0][0] + "\t" + TEST_DATA[4][0] + "\t" + TEST_DATA[5][0] + "\t" + HIDDEN_TEXT + "\n" +
            TEST_DATA[0][1] + "\t" + TEST_DATA[4][1] + "\t" + TEST_DATA[5][1] + "\t" + HIDDEN_TEXT + "\n" +
            TEST_DATA[0][2] + "\t" + TEST_DATA[4][2] + "\t" + TEST_DATA[5][2] + "\t" + HIDDEN_TEXT;
    private final String TEST_FAIL2 = LIST_KEY_NAME2 + "\t" + FAKE_COL1_NAME + "\t" + _listCol2.getName() + "\n" +
            LIST_ROW1 + "\t" + "String";
    private final static String TEST_FAIL = "testfail";
    private final static String TEST_VIEW = "list_view";
    private final static String LIST2_NAME = TRICKY_CHARACTERS_NO_QUOTES + "Cars";
    protected final static ListHelper.ListColumnType LIST2_KEY_TYPE = ListHelper.ListColumnType.String;
    protected final static String LIST2_KEY_NAME = "Car";

    protected final ListColumn _list2Col1 = new ListColumn(LIST_KEY_NAME2, LIST_KEY_NAME2, LIST2_KEY_TYPE, "The color of the car", new LookupInfo(null, "lists", LIST_NAME));
    private final static String LIST2_KEY = "Car1";
    private final static String LIST2_FOREIGN_KEY = "Blue";
    private final static String LIST2_KEY2 = "Car2";
    private final static String LIST2_FOREIGN_KEY2 = "Green";
    private final static String LIST2_FOREIGN_KEY_OUTSIDE = "Guy";
    private final static String LIST2_KEY3 = "Car3";
    private final static String LIST2_FOREIGN_KEY3 = "Red";
    private final static String LIST2_KEY4 = "Car4";
    private final static String LIST2_FOREIGN_KEY4 = "Brown";
    private final static String LIST3_NAME = "Owners";
    private final static ListHelper.ListColumnType LIST3_KEY_TYPE = ListHelper.ListColumnType.String;
    private final static String LIST3_KEY_NAME = "Owner";
    private final ListColumn _list3Col2 = new ListColumn("Wealth", "Wealth", ListHelper.ListColumnType.String, "");
    protected final ListColumn _list3Col1 = new ListColumn(LIST3_KEY_NAME, LIST3_KEY_NAME, LIST3_KEY_TYPE, "Who owns the car", new LookupInfo("/" + PROJECT_NAME2, "lists", LIST3_NAME));
    private final static String LIST3_COL2 = "Rich";
    private final String LIST2_DATA = LIST2_KEY_NAME + "\t" + _list2Col1.getName()  + "\t" + LIST3_KEY_NAME
            + "\n" + LIST2_KEY + "\t" + LIST2_FOREIGN_KEY + "\n" + LIST2_KEY2  + "\t" + LIST2_FOREIGN_KEY2 + "\t" +
            LIST2_FOREIGN_KEY_OUTSIDE + "\n" + LIST2_KEY3  + "\t" + LIST2_FOREIGN_KEY3 + "\n" + LIST2_KEY4  + "\t" +
            LIST2_FOREIGN_KEY4;
    private final String LIST3_DATA = LIST3_KEY_NAME + "\t" + _list3Col2.getName() + "\n" + LIST2_FOREIGN_KEY_OUTSIDE + "\t" +
            LIST3_COL2;
    public static final String LIST_AUDIT_EVENT = "List events";

    private final String EXCEL_DATA_FILE = getLabKeyRoot() + "/sampledata/dataLoading/excel/fruits.xls";
    private final String TSV_DATA_FILE = getLabKeyRoot() + "/sampledata/dataLoading/excel/fruits.tsv";
    private final String TSV_LIST_NAME = "Fruits from TSV";

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/list";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected void doCleanup()
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
        try {deleteProject(PROJECT_NAME2); } catch (Throwable t) {}
    }

    @Override
    protected boolean isFileUploadTest()
    {
        return true;
    }

    protected void setUpListFinish()
    {
        log("Add data to existing rows");
        clickImportData();
        setFormElement("text", LIST_DATA2);
        submitImportTsv();
    }

    protected void setUpList(String projectName)
    {

        log("Setup project and list module");
        createProject(projectName);

        log("Add list -- " + LIST_NAME);
        ListHelper.createList(this, projectName, LIST_NAME, LIST_KEY_TYPE, LIST_KEY_NAME, _listCol1Fake, _listCol2, _listCol3);

        log("Add description and test edit");
        clickEditDesign();
        setFormElement("ff_description", LIST_DESCRIPTION);
        setColumnName(0, LIST_KEY_NAME2);
        clickSave();

        log("Check that edit list definition worked");
        assertTextPresent(LIST_KEY_NAME2);
        assertTextPresent(LIST_DESCRIPTION);

        log("Test upload data");
        clickImportData();
        submitImportTsv("Form contains no data");
        setFormElement("text", TEST_FAIL);
        submitImportTsv("could not be matched to a field");
        assertTextPresent(TEST_FAIL);
        setFormElement("text", TEST_FAIL2);
        submitImportTsv("must be of type");
        setFormElement("text", LIST_DATA);
        submitImportTsv();

        log("Check upload worked correctly");
        assertTextPresent(_listCol2.getLabel());
        assertTextPresent(TEST_DATA[0][0]);
        assertTextPresent(TEST_DATA[1][1]);
        assertTextPresent(TEST_DATA[3][2]);

        DataRegionTable table = new DataRegionTable("query", this);
        assertEquals("true",  table.getDataAsText(0, _listCol3.getLabel()));
        assertEquals("false", table.getDataAsText(1, _listCol3.getLabel()));
        assertEquals("true",  table.getDataAsText(2, _listCol3.getLabel()));

        log("Test check/uncheck of checkboxes");
        // Second row (Green)
        clickLinkWithText("edit", 1);
        setFormElement("quf_" + _listCol2.getName(), CONVERTED_MONTHS[1]);  // Has a funny format -- need to post converted date
        checkCheckbox("quf_JewelTone");
        submit();
        // Third row (Red)
        clickLinkWithText("edit", 2);
        setFormElement("quf_" + _listCol2.getName(), CONVERTED_MONTHS[2]);  // Has a funny format -- need to post converted date
        uncheckCheckbox("quf_JewelTone");
        submit();

        table = new DataRegionTable("query", this);
        assertEquals("true",  table.getDataAsText(0, _listCol3.getLabel()));
        assertEquals("true",  table.getDataAsText(1, _listCol3.getLabel()));
        assertEquals("false", table.getDataAsText(2, _listCol3.getLabel()));

        log("Test edit and adding new field with imported data present");
        clickTab("List");
        clickLinkWithText("view design");
        clickEditDesign();
        setColumnName(1,_listCol1.getName());
        setColumnLabel(1, _listCol1.getLabel());
        clickNavButton("Add Field", 0);
        setColumnName(4,_listCol4.getName());
        setColumnLabel(4, _listCol4.getLabel());
        setColumnType(4, _listCol4.getType());
        setFormElement(Locator.id("propertyDescription"), _listCol4.getDescription());


        // Create "Hidden Field" and remove from all views.
        clickNavButton("Add Field", 0);
        setColumnName(5, _listCol5.getName());
        setColumnLabel(5,_listCol5.getLabel());
        setColumnType(5,_listCol5.getType());
        uncheckCheckbox(Locator.raw("//span[@id='propertyShownInGrid']/input"));
        uncheckCheckbox(Locator.raw("//span[@id='propertyShownInInsert']/input"));
        uncheckCheckbox(Locator.raw("//span[@id='propertyShownInUpdate']/input"));
        uncheckCheckbox(Locator.raw("//span[@id='propertyShownInDetail']/input"));

        clickNavButton("Add Field", 0);
        setColumnName(6, _listCol6.getName());
        setColumnLabel(6,_listCol6.getLabel());
        setColumnType(6,_listCol6.getType());
        selectPropertyTab("Advanced");
        waitForElement(Locator.id("importAliases"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.id("importAliases"), ALIASED_KEY_NAME);

        mouseClick(Locator.id("partdown_2").toString());

        clickSave();

        log("Check new field was added correctly");
        assertTextPresent(_listCol4.getName());

        log("Set title field of 'Colors' to 'Desc'");
        clickEditDesign();
        selectOptionByText("ff_titleColumn", "Desc");
        clickDone();

        clickLinkWithText("view data");
        assertTextPresent(TEST_DATA[0][0]);
        assertTextPresent(TEST_DATA[1][1]);
        assertTextPresent(TEST_DATA[3][2]);

        assertTextNotPresent(HIDDEN_TEXT); // Hidden from Grid view.
        if(!getBrowserType().contains("iexplore"))
            assertTextBefore(_listCol3.getLabel(), _listCol2.getLabel()); // Columns swapped. Doesn't work in IE

        setUpListFinish();

        log("Check that data was added correctly");
        assertTextPresent(TEST_DATA[0][0]);
        assertTextPresent(TEST_DATA[1][1]);
        assertTextPresent(TEST_DATA[3][2]);
        assertTextPresent(TEST_DATA[4][0]);
        assertTextPresent(TEST_DATA[4][1]);
        assertTextPresent(TEST_DATA[4][2]);
        assertTextPresent(TEST_DATA[5][0]);
        assertTextPresent(TEST_DATA[5][1]);
        assertTextPresent(TEST_DATA[5][2]);

        log("Check that hidden column is hidden.");
        clickLinkWithText("details");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from details view.
        if(!getBrowserType().contains("iexplore"))
            assertTextBefore(_listCol3.getLabel(), _listCol2.getLabel());
        clickNavButton("Edit");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from update view.
        if(!getBrowserType().contains("iexplore"))
            assertTextBefore(_listCol3.getLabel(), _listCol2.getLabel());
        clickNavButton("Cancel");
        clickNavButton("Show Grid");

        log("Test inserting new row");
        clickNavButton("Insert New");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from insert view.
        if(!getBrowserType().contains("iexplore"))
            assertTextBefore(_listCol3.getLabel(), _listCol2.getLabel());
        String html = selenium.getHtmlSource();
        assertTrue("Description \"" + _listCol1.getDescription() + "\" not present.", html.contains(_listCol1.getDescription()));
        assertTrue("Description \"" + _listCol3.getDescription() + "\" not present.", html.contains(_listCol3.getDescription()));
        setFormElement("quf_" + _listCol1.getName(), TEST_DATA[1][3]);
        setFormElement("quf_" + _listCol2.getName(), "wrong type");
        // Jewel Tone checkbox is left blank -- we'll make sure it's posted as false below
        setFormElement("quf_" + _listCol4.getName(), TEST_DATA[4][3]);
        submit();
        assertTextPresent("This field is required");
        setFormElement("quf_" + LIST_KEY_NAME2, TEST_DATA[0][3]);
        submit();
        assertTextPresent("Could not convert");
        setFormElement("quf_" + _listCol2.getName(), CONVERTED_MONTHS[3]);
        submit();

        log("Check new row was added");
        assertTextPresent(TEST_DATA[0][3]);
        assertTextPresent(TEST_DATA[1][3]);
        assertTextPresent(TEST_DATA[2][3]);
        assertTextPresent(TEST_DATA[3][3]);
        table = new DataRegionTable("query", this);
        assertEquals("false", table.getDataAsText(2, _listCol3.getLabel()));
        assertEquals("false", table.getDataAsText(3, _listCol3.getLabel()));

        log("Check hidden field is hidden only where specified.");
        dataregionToEditDesign();

        setColumnName(5,_listCol5.getName()); // Select Hidden field.
        checkCheckbox(Locator.raw("//span[@id='propertyShownInGrid']/input"));
        mouseClick(Locator.id("partdown_2").toString());
        clickDone();

        log("Check that hidden column is hidden.");
//        clickLinkWithText("view data");
        assertTextPresent(HIDDEN_TEXT); // Not hidden from grid view.
        clickLinkWithText("details");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from details view.
        assertTextBefore(_listCol2.getLabel(), _listCol3.getLabel());
        clickNavButton("Edit");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from update view.
        assertTextBefore(_listCol2.getLabel(), _listCol3.getLabel());
        clickNavButton("Cancel");
        clickNavButton("Show Grid");
        clickNavButton("Insert New");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from insert view.
        assertTextBefore(_listCol2.getLabel(), _listCol3.getLabel());
        clickNavButton("Cancel");

        dataregionToEditDesign();

        setColumnName(5,_listCol5.getName()); // Select Hidden field.
        uncheckCheckbox(Locator.raw("//span[@id='propertyShownInGrid']/input"));
        checkCheckbox(Locator.raw("//span[@id='propertyShownInInsert']/input"));
        clickDone();

//        clickLinkWithText("view data");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from grid view.
        clickLinkWithText("details");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from details view.
        clickNavButton("Edit");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from update view.
        clickNavButton("Cancel");
        clickNavButton("Show Grid");
        clickNavButton("Insert New");
        assertTextPresent(HIDDEN_TEXT); // Not hidden from insert view.
        clickNavButton("Cancel");

        dataregionToEditDesign();

        setColumnName(5,_listCol5.getName()); // Select Hidden field.
        uncheckCheckbox(Locator.raw("//span[@id='propertyShownInInsert']/input"));
        checkCheckbox(Locator.raw("//span[@id='propertyShownInUpdate']/input"));
        clickDone();

//        clickLinkWithText("view data");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from grid view.
        clickLinkWithText("details");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from details view.
        clickNavButton("Edit");
        assertTextPresent(HIDDEN_TEXT); // Not hidden from update view.
        clickNavButton("Cancel");
        clickNavButton("Show Grid");
        clickNavButton("Insert New");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from insert view.
        clickNavButton("Cancel");

        dataregionToEditDesign();

        setColumnName(5,_listCol5.getName()); // Select Hidden field.
        uncheckCheckbox(Locator.raw("//span[@id='propertyShownInUpdate']/input"));
        checkCheckbox(Locator.raw("//span[@id='propertyShownInDetail']/input"));
        clickDone();

//        clickLinkWithText("view data");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from grid view.
        clickLinkWithText("details");
        assertTextPresent(HIDDEN_TEXT); // Not hidden from details view.
        clickNavButton("Edit");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from update view.
        clickNavButton("Cancel");
        clickNavButton("Show Grid");
        clickNavButton("Insert New");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from insert view.
        clickNavButton("Cancel");
    }

    protected void doTestSteps()
    {
        setUpList(PROJECT_NAME);

        log("Test Sort and Filter in Data View");
        setSort("query", _listCol1.getName(), SortDirection.ASC);
        assertTextBefore(TEST_DATA[0][0], TEST_DATA[0][1]);

        clearSortTest();

        setFilter("query", _listCol4.getName(), "Is Greater Than", "7");
        assertTextNotPresent(TEST_DATA[0][3]);

        log("Test Customize View");
        clickButton("Clear All");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.removeCustomizeViewColumn(this, _listCol4.getName());
        CustomizeViewsHelper.addCustomizeViewFilter(this, _listCol4.getName(), _listCol4.getLabel(), "Is Less Than", "10");
        CustomizeViewsHelper.addCustomizeViewSort(this, _listCol2.getName(), _listCol2.getLabel(), "Ascending");
        CustomizeViewsHelper.saveCustomView(this, TEST_VIEW);

        log("Check Customize View worked");
        assertTextPresent(TEST_DATA[0][3]);
        assertTextPresentInThisOrder(TEST_DATA[0][3], TEST_DATA[0][2], TEST_DATA[0][1]);
        assertTextNotPresent(TEST_DATA[0][0]);
        assertTextNotPresent(_listCol4.getLabel());

        log("4725: Check Customize View can't remove all fields");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.removeCustomizeViewColumn(this, LIST_KEY_NAME2);
        CustomizeViewsHelper.removeCustomizeViewColumn(this, _listCol1.getName());
        CustomizeViewsHelper.removeCustomizeViewColumn(this, _listCol2.getName());
        CustomizeViewsHelper.removeCustomizeViewColumn(this, _listCol3.getName());
        CustomizeViewsHelper.removeCustomizeViewColumn(this, _listCol6.getName());
        CustomizeViewsHelper.applyCustomView(this, 0);
        assertAlert("You must select at least one field to display in the grid.");
        CustomizeViewsHelper.closeCustomizeViewPanel(this);

        log("Test Export");
        addUrlParameter("exportAsWebPage=true");
        pushLocation();
        waitForElement(Locator.navButton("Export"), WAIT_FOR_JAVASCRIPT);
        clickExportToText();
        assertTextPresent(TEST_DATA[0][3]);
        assertTextPresentInThisOrder(TEST_DATA[0][3], TEST_DATA[0][2], TEST_DATA[0][1]);
        assertTextNotPresent(TEST_DATA[0][0]);
        assertTextNotPresent(_listCol4.getLabel());
        popLocation();

        filterTest();

        clickLinkWithText(getProjectName());

        log("Test that sort only affects one web part");
        setSort("qwp2", _listCol4.getName(), SortDirection.ASC);
        String source = selenium.getHtmlSource();
        int index;
        assertTrue(source.indexOf(TEST_DATA[1][2]) < (index = source.indexOf(TEST_DATA[1][1])) &&
                source.indexOf(TEST_DATA[1][1], index) < source.indexOf(TEST_DATA[1][2], index));

        log("Test list history");
        clickLinkWithText("manage lists");
        clickLinkWithText("view history");
        assertTextPresent(":History");
        assertTextPresent("modified", 10);
        assertTextPresent("Bulk inserted", 2);
        assertTextPresent("A new list record was inserted", 1);
        assertTextPresent("created", 1);
        assertEquals("details Links", 3, countLinksWithText("details"));
        assertEquals("Project Links", 14 + 3, countLinksWithText(PROJECT_NAME)); // Table links + header & sidebar links
        assertEquals("List Links", 14 + 1, countLinksWithText(LIST_NAME)); // Table links + header link
        clickLinkWithText("details");
        assertTextPresent("List Item Details");
        assertTextNotPresent("No details available for this event.");
        assertTextNotPresent("Unable to find the audit history detail for this event");

        clickNavButton("Done");
        clickLinkWithText(PROJECT_NAME, 3);

        log("Test single list web part");
        addWebPart("List - Single");
        setText("title", "This is my single list web part title");
        submit();
        assertTextPresent("Import Data");
        assertTextPresent("View Design");
        clickAndWait(Locator.linkWithSpan("This is my single list web part title"), WAIT_FOR_PAGE);
        assertTextPresent("Colors");
        assertTextPresent("Views");

        log("Create second project");
        createProject(PROJECT_NAME2);

        log("Add List -- " + LIST3_NAME);
        ListHelper.createList(this, PROJECT_NAME2, LIST3_NAME, LIST3_KEY_TYPE, LIST3_KEY_NAME, _list3Col2);
        assertTextPresent("<AUTO> (Owner)");

        log("Upload data to second list");
        ListHelper.uploadData(this, PROJECT_NAME2, LIST3_NAME, LIST3_DATA);

        log("Navigate back to first project");
        log("Add list -- " + LIST2_NAME);
        ListHelper.createList(this, PROJECT_NAME, LIST2_NAME, LIST2_KEY_TYPE, LIST2_KEY_NAME, _list2Col1, _list3Col1);

        log("Upload data to second list");
        ListHelper.uploadData(this, PROJECT_NAME, LIST2_NAME, LIST2_DATA);

        log("Check that upload worked");
        assertTextPresent(LIST2_KEY);
        assertTextPresent(LIST2_KEY2);
        assertTextPresent(LIST2_KEY3);
        assertTextPresent(LIST2_KEY4);

        log("Check that reference worked");
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        CustomizeViewsHelper.addCustomizeViewColumn(this, _list2Col1.getName() + "/" +  _listCol1.getName(), _list2Col1.getLabel() + " " +  _listCol1.getLabel());
        CustomizeViewsHelper.addCustomizeViewColumn(this, _list2Col1.getName() + "/" +  _listCol2.getName(), _list2Col1.getLabel() + " " +  _listCol2.getLabel());
        CustomizeViewsHelper.addCustomizeViewColumn(this, _list2Col1.getName() + "/" +  _listCol4.getName(), _list2Col1.getLabel() + " " + _listCol4.getLabel());
        CustomizeViewsHelper.addCustomizeViewFilter(this, _list2Col1.getName() + "/" +  _listCol4.getName(),  _listCol4.getLabel(), "Is Less Than", "10");
        CustomizeViewsHelper.addCustomizeViewSort(this, _list2Col1.getName() + "/" +  _listCol4.getName(),  _listCol4.getLabel(), "Ascending");
        CustomizeViewsHelper.addCustomizeViewColumn(this, _list3Col1.getName() + "/" +  _list3Col1.getName(), _list3Col1.getLabel() + " " +  _list3Col1.getLabel());
        CustomizeViewsHelper.addCustomizeViewColumn(this, _list3Col1.getName() + "/" +  _list3Col2.getName(), _list3Col1.getLabel() + " " +  _list3Col2.getLabel());
        CustomizeViewsHelper.saveCustomView(this, TEST_VIEW);

        log("Check adding referenced fields worked");
        waitForText(_listCol1.getLabel(), WAIT_FOR_JAVASCRIPT);
        assertTextPresent(_listCol1.getLabel());
        assertTextPresent(_listCol2.getLabel());
        assertTextPresent(_listCol4.getLabel());
        assertTextPresent(LIST2_FOREIGN_KEY_OUTSIDE);
        assertTextPresent(LIST3_COL2);
        assertTextNotPresent(LIST2_KEY);
        assertTextBefore(LIST2_KEY3, LIST2_KEY2);
        assertTextNotPresent(LIST2_KEY4);

        log("Test export");
        addUrlParameter("exportAsWebPage=true");
        pushLocation();
        waitForElement(Locator.navButton("Export"), WAIT_FOR_JAVASCRIPT);
        clickExportToText();
        assertTextPresent(LIST_KEY_NAME2.toLowerCase() + _listCol1.getName());
        assertTextPresent(LIST_KEY_NAME2.toLowerCase() + _listCol2.getName());
        assertTextPresent(LIST_KEY_NAME2.toLowerCase() + _listCol4.getName());
        assertTextPresent(LIST2_FOREIGN_KEY_OUTSIDE);
        assertTextPresent(LIST3_COL2);
        assertTextNotPresent(LIST2_KEY);
        assertTextBefore(LIST2_KEY3, LIST2_KEY2);
        assertTextNotPresent(LIST2_KEY4);
        popLocation();

        log("Test edit row");
        clickLinkWithText("edit", 0);
        selectOptionByText("quf_Color", TEST_DATA[1][1]);
        selectOptionByText("quf_Owner", LIST2_FOREIGN_KEY_OUTSIDE);
        submit();

        clickMenuButton("Views", "default");
        assertTextPresent(TEST_DATA[1][1], 2);

        log("Test deleting rows");
        checkCheckbox(".toggle");
        selenium.chooseOkOnNextConfirmation();
        clickButton("Delete", 0);
        assertEquals(selenium.getConfirmation(), "Are you sure you want to delete the selected rows?");
        waitForPageToLoad();
        assertTextNotPresent(LIST2_KEY);
        assertTextNotPresent(LIST2_KEY2);
        assertTextNotPresent(LIST2_KEY3);
        assertTextNotPresent(LIST2_KEY4);

        log("Get URL to test exporting deleted list.");
        clickTab("List");
        clickAndWait(Locator.raw("//td[contains(text(), '" + LIST_NAME + "')]/..//a[text()='view data']"));
        clickNavButton("Export", 0);
        ExtHelper.clickSideTab(this, "Text");
        String exportUrl = getAttribute(Locator.xpath(Locator.navButton("Export to Text").getPath() + "/..") , "href");
        clickLinkWithText("View Design");

        log("Test deleting data");
        clickDeleteList();
        clickNavButton("OK");

        log("Test that deletion happened");
        assertTextNotPresent(LIST_NAME);
        clickLinkWithText("view data");
        pushLocation();
        CustomizeViewsHelper.openCustomizeViewPanel(this);
        assertElementNotPresent(Locator.xpath("//div[contains(@class, 'x-tree-node') and @*='" + LIST_KEY_NAME + "']"));
        assertElementPresent(Locator.xpath("//div[contains(@class, 'x-tree-node') and @*='" + LIST3_KEY_NAME + "']"));
        popLocation();
        clickLinkWithText(PROJECT_NAME);
        assertTextPresent("query not found");

        log("Test exporting a nonexistent list returns a 404");
        selenium.open(WebTestHelper.getBaseURL() + exportUrl.substring(WebTestHelper.getContextPath().length()));
        assertEquals("Incorrect response code", 404, getResponseCode());
        assertTextPresent("Query '" + LIST_NAME + "' in schema 'lists' doesn't exist.");

        clickNavButton("Folder");
        AuditLogTest.verifyAuditEvent(this, LIST_AUDIT_EVENT, AuditLogTest.COMMENT_COLUMN, "The domain " +LIST_NAME + " was deleted", 5);
        AuditLogTest.verifyAuditEvent(this, LIST_AUDIT_EVENT, AuditLogTest.COMMENT_COLUMN, "An existing list record was deleted", 5);
        AuditLogTest.verifyAuditEvent(this, LIST_AUDIT_EVENT, AuditLogTest.COMMENT_COLUMN, "An existing list record was modified", 10);

        doRenameFieldsTest();
        doUploadTest();
        customFormattingTest();
        customizeURLTest();
    }

    private void filterTest()
    {
        log("Filter Test");
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Query");
        selectOptionByText("schemaName", "lists");
        selenium.click("document.frmCustomize.selectQuery[1]");
        submit();
        addWebPart("Query");
        selectOptionByText("schemaName", "lists");
        selenium.click("document.frmCustomize.selectQuery[1]");
        submit();

        log("Test that the right filters are present for each type");
        runMenuItemHandler("qwp3:" + _listCol4.getName() + ":filter");
        ExtHelper.waitForExtDialog(this, "Show Rows Where " + _listCol4.getLabel());
        click(Locator.xpath("//div["+Locator.NOT_HIDDEN+" and ./label/span[text()='Filter Type:']]/div/div//img[contains(@class, 'x-form-arrow-trigger')]"));

        assertElementNotPresent(Locator.xpath("//div["+Locator.NOT_HIDDEN+" and contains(@class, 'x-combo-list-item') and text()='Starts With']"));
        assertElementPresent(Locator.xpath("//div["+Locator.NOT_HIDDEN+" and contains(@class, 'x-combo-list-item') and text()='Is Blank']"));
        ExtHelper.clickExtButton(this, "Show Rows Where " + _listCol4.getLabel(), "CANCEL", 0);

        log("Test that filters don't affect multiple web parts");
        assertTextPresent(TEST_DATA[1][0], 2);
        setFilter("qwp3", _listCol4.getName(), "Is Less Than", "10");
        assertTextPresent(TEST_DATA[1][0], 1);

        clickLinkContainingText(LIST_NAME);
    }




    /*                Issue 11825: Create test for "Clear Sort"
        sort by a parameter, than clear sort.
        Verify that reverts to original sort and the dropdown menu disappears

        preconditions:  table already sorted by description
     */
    private void clearSortTest()
    {
        //make sure elements are ordered the way they should be
        assertTextPresentInThisOrder(TEST_DATA[5][0], TEST_DATA[5][1],TEST_DATA[5][2]);

        //sort  by element and verify it worked
        setSort("query", _listCol6.getName(), SortDirection.DESC);
        assertTextPresentInThisOrder(TEST_DATA[5][0], TEST_DATA[5][2], TEST_DATA[5][1]);

        //remove sort and verify we return to initial state
        clearSort("query", _listCol6.getName());
        assertTextPresentInThisOrder(TEST_DATA[5][0], TEST_DATA[5][1],TEST_DATA[5][2]);
    }

    private void doUploadTest()
    {
        if (!isFileUploadAvailable())
            return;

        log("Infer from excel file, then import data");
        File excelFile = new File(EXCEL_DATA_FILE);
        ListHelper.createListFromFile(this, PROJECT_NAME, "Fruits from Excel", excelFile);
        assertNoLabkeyErrors();
        assertTextPresent("pomegranate");

        File tsvFile = new File(TSV_DATA_FILE);
        //Cancel test disabled because teamcity is too slow to run it successfully
        /*log("Infer from tsv file, but cancel before completion");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText("manage lists");
        clickNavButton("Create New List");
        waitForElement(Locator.name("ff_name"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        setFormElement("ff_name",  TSV_LIST_NAME);
        checkCheckbox(Locator.xpath("//span[@id='fileImport']/input[@type='checkbox']"));
        clickNavButton("Create List", 0);
        waitForElement(Locator.xpath("//input[@name='uploadFormElement']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        setFormElement("uploadFormElement", tsvFile);
        waitForElement(Locator.xpath("//span[@id='button_Import']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        clickNavButton("Import", 0);
        waitForElement(Locator.xpath("//div[text()='Creating columns...']"), WAIT_FOR_JAVASCRIPT);
        clickNavButton("Cancel");
        assertTextNotPresent(TSV_LIST_NAME);*/

        log("Infer from a tsv file, then import data");
        ListHelper.createListFromFile(this, PROJECT_NAME, TSV_LIST_NAME, tsvFile);
        assertNoLabkeyErrors();
        assertTextPresent("pomegranate");
        log("Verify correct types are inferred from file");
        clickNavButton("View Design");
        waitForElement(Locator.xpath("//tr[./td/div[text()='BoolCol'] and ./td/div[text()='Boolean']]"), WAIT_FOR_JAVASCRIPT);
        assertElementPresent(Locator.xpath("//tr[./td/div[text()='IntCol'] and ./td/div[text()='Integer']]"));
        assertElementPresent(Locator.xpath("//tr[./td/div[text()='NumCol'] and ./td/div[text()='Number (Double)']]"));
        assertElementPresent(Locator.xpath("//tr[./td/div[text()='DateCol'] and ./td/div[text()='DateTime']]"));
    }

    private void customFormattingTest()
    {
        // Assumes we are at the list designer after doUploadTest()
        clickNavButton("Edit Design", 0);

        // Set conditional format on boolean column. Bold, italic, strikethrough, cyan text, red background
        click(Locator.name("ff_name3")); // BoolCol
        click(Locator.xpath("//span[text()='Format']"));
        clickNavButton("Add Conditional Format", 0);
        ExtHelper.waitForExtDialog(this, "Apply Conditional Format Where BoolCol", WAIT_FOR_JAVASCRIPT);
        setFormElement("value_1", "true");
        ExtHelper.clickExtButton(this, "Apply Conditional Format Where BoolCol", "OK", 0);
        checkCheckbox("Bold");
        checkCheckbox("Italic");
        checkCheckbox("Strikethrough");
        click(Locator.xpath("//div[@title='Color']"));
        waitForElement(Locator.xpath("//div[contains(@class, 'gwt-DialogBox')]//div[contains(@class, 'Caption') and text()='Conditional Format Colors']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//fieldset[./legend/span[text()='Background']]//input"), "FF0000"); // Red background
        click(Locator.id("button_OK"));
        waitForElementToDisappear(Locator.xpath("//div[contains(@class, 'gwt-DialogBox')]//div[contains(@class, 'Caption') and text()='Conditional Format Colors']"), WAIT_FOR_JAVASCRIPT);
        // Regression test for Issue 11435: reopen color dialog to set text color
        click(Locator.xpath("//div[@title='Color']"));
        waitForElement(Locator.xpath("//div[contains(@class, 'gwt-DialogBox')]//div[contains(@class, 'Caption') and text()='Conditional Format Colors']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//fieldset[./legend/span[text()='Foreground']]//input"), "00FFFF"); // Cyan text
        click(Locator.id("button_OK"));
        waitForElementToDisappear(Locator.xpath("//div[contains(@class, 'gwt-DialogBox')]//div[contains(@class, 'Caption') and text()='Conditional Format Colors']"), WAIT_FOR_JAVASCRIPT);

        // Set multiple conditional formats on int column.
        click(Locator.name("ff_name4")); // IntCol
        click(Locator.xpath("//span[text()='Format']"));
        // If greater than 7, strikethrough //TODO: Set after (>5) format. Blocked: 12865
        clickNavButton("Add Conditional Format", 0);
        ExtHelper.waitForExtDialog(this, "Apply Conditional Format Where IntCol", WAIT_FOR_JAVASCRIPT);
        ExtHelper.selectComboBoxItem(this, "Filter Type", "Is Greater Than");
        setFormElement("value_1", "7");
        ExtHelper.clickExtButton(this, "Apply Conditional Format Where IntCol", "OK", 0);
        checkCheckbox("Strikethrough");
        // If greater than 5, Bold  //TODO: Set before (>7) format. Blocked: 12865
        clickNavButton("Add Conditional Format", 0);
        ExtHelper.waitForExtDialog(this, "Apply Conditional Format Where IntCol", WAIT_FOR_JAVASCRIPT);
        ExtHelper.selectComboBoxItem(this, "Filter Type", "Is Greater Than");
        setFormElement("value_1", "5");
        ExtHelper.clickExtButton(this, "Apply Conditional Format Where IntCol", "OK", 0);
        checkCheckbox("Bold", 1);

        // TODO: Blocked: 12865: ListTest failing to reorder conditional formats
        // Switch the order of filters so that >7 takes precedence over >5
//        selenium.windowMaximize();
//        dragAndDrop(Locator.xpath("//div[text()='Is Greater Than 5']"), Locator.xpath("//div[text()='Is Greater Than 7']"));
        assertTextBefore("Is Greater Than 7", "Is Greater Than 5");
        
        clickNavButton("Save", 0);
        waitAndClickNavButton("Done");

        // Verify conditional format of boolean column
        // look for cells that do not match the
        assertTextPresent(TSV_LIST_NAME);
        assertElementNotPresent(Locator.xpath("//td[text() = 'true' and not(contains(@style, 'line-through'))]"));
        assertElementNotPresent(Locator.xpath("//td[text() = 'true' and not(contains(@style, 'bold'))]"));
        assertElementNotPresent(Locator.xpath("//td[text() = 'true' and not(contains(@style, 'italic'))]"));
        assertElementNotPresent(Locator.xpath("//td[text() = 'true' and not(contains(@style, 'color: rgb(0, 255, 255)') or contains(@style, 'color: #00FFFF'))]")); // Cyan text
        assertElementNotPresent(Locator.xpath("//td[text() = 'true' and not(contains(@style, 'background-color: rgb(255, 0, 0)') or contains(@style, 'color: #FF0000'))]")); // Red background
        assertElementNotPresent(Locator.xpath("//td[text() = 'false' and @style]")); // No style on false items
        assertElementPresent(Locator.xpath("//td[text()='5' and not(contains(@style, 'bold')) and not(contains(@style, 'line-through'))]"));
        assertElementPresent(Locator.xpath("//td[text()='6' and contains(@style, 'bold') and not(contains(@style, 'line-through'))]"));
        assertElementPresent(Locator.xpath("//td[text()='8' and contains(@style, 'line-through') and not(contains(@style, 'bold'))]"));

        // Check for appropriate tooltips
        assertTextNotPresent("Formatting applied because column > 5.");
        mouseOver(Locator.xpath("//td[text()='6' and contains(@style, 'bold')]"));
        // Tooltip doesn't show instantly, so wait for a bit
        sleep(2000);
        assertTextPresent("Formatting applied because column > 5.");
        mouseOut(Locator.xpath("//td[text()='6' and contains(@style, 'bold')]"));
        // Tooltip doesn't hide instantly, so wait for a bit
        sleep(2000);
        assertTextNotPresent("Formatting applied because column > 5.");

        assertTextNotPresent("Formatting applied because column = true.");
        mouseOver(Locator.xpath("//td[text()='true']"));
        // Tooltip doesn't show instantly, so wait for a bit
        sleep(2000);
        assertTextPresent("Formatting applied because column = true.");
        mouseOut(Locator.xpath("//td[text()='true']"));
        // Tooltip doesn't hide instantly, so wait for a bit
        sleep(2000);
        assertTextNotPresent("Formatting applied because column = true.");
    }

    private void doRenameFieldsTest()
    {
        log("8329: Test that renaming a field then creating a new field with the old name doesn't result in awful things");
        ListHelper.createList(this, PROJECT_NAME, "new", ListHelper.ListColumnType.AutoInteger, "key", new ListColumn("BarBar", "BarBar", ListHelper.ListColumnType.String, "Some new column"));
        assertTextPresent("BarBar");
        clickEditDesign();
        setColumnName(1,"FooFoo");
        setColumnLabel(1,"");
        clickSave();
        assertTextPresent("FooFoo");
        assertTextNotPresent("BarBar");
        clickEditDesign();
        clickNavButton("Add Field", 0);
        setColumnName(2,"BarBar");
        clickSave();
        assertTextPresent("FooFoo");
        assertTextPresent("BarBar");
        assertTextBefore("FooFoo", "BarBar");
    }



    //
    // CUSTOMIZE URL tests
    //

    ListHelper.ListColumn col(String name, ListHelper.ListColumnType type)
    {
        return new ListHelper.ListColumn(name, "", type, "");
    }

    ListHelper.ListColumn col(String name, ListHelper.ListColumnType type, String table)
    {
        return new ListHelper.ListColumn(name, "", type, "", new ListHelper.LookupInfo(null, "lists", table));
    }
    
    ListHelper.ListColumn colURL(String name, ListHelper.ListColumnType type, String url)
    {
        ListColumn c  = new ListHelper.ListColumn(name, "", type, "");
        c.setURL(url);
        return c;
    }

    List<ListColumn> Acolumns = Arrays.asList(
            col("A", Integer),
            colURL("title", String, "/junit/echoForm.view?key=${A}&title=${title}&table=A"),
            col("Bfk", Integer, "B")
    );
    String[][] Adata = new String[][]
    {
        {"1", "one A", "1"},
    };

    List<ListHelper.ListColumn> Bcolumns = Arrays.asList(
            col("B", Integer),
            colURL("title", String, "org.labkey.core.junit.JunitController$EchoFormAction.class?key=${B}&title=${title}&table=B"),
            col("Cfk", Integer, "C")
    );
    String[][] Bdata = new String[][]
    {
        {"1", "one B", "1"},
    };
    
    List<ListHelper.ListColumn> Ccolumns = Arrays.asList(
            col("C", Integer),
            colURL("title", String, "/junit/echoForm.view?key=${C}&title=${title}&table=C")
    );
    String[][] Cdata = new String[][]
    {
        {"1", "one C"},
    };


    String toTSV(List<ListHelper.ListColumn> cols, String[][] data)
    {
        StringBuilder sb = new StringBuilder();
        String tab = "";
        for (ListHelper.ListColumn c : cols)
        {
            sb.append(tab);
            sb.append(c.getName());
            tab = "\t";
        }
        tab = "\n";
        for (String[] row : data)
        {
            for (String cell : row)
            {
                sb.append(tab);
                sb.append(cell);
                tab = "\t";
            }
            tab = "\n";
        }
        sb.append(tab);
        return sb.toString();
    }


    void submitImportTsv(String error)
    {
        ListHelper.submitImportTsv_error(this, error);
    }

    void submitImportTsv()
    {
        ListHelper.submitImportTsv_success(this);
    }


    void createList(String name, List<ListHelper.ListColumn> cols, String[][] data)
    {
        log("Add List -- " + name);
        ListHelper.createList(this, PROJECT_NAME, name, cols.get(0).getType(), cols.get(0).getName(),
                cols.subList(1, cols.size()).toArray(new ListHelper.ListColumn[cols.size() - 1]));
        clickEditDesign();
        selectOptionByText("ff_titleColumn", cols.get(1).getName());    // Explicitly set to the PK (auto title will pick wealth column)
        clickSave();
        clickImportData();
        setFormElement("text", toTSV(cols,data));
        submitImportTsv();
    }


    Locator inputWithValue(String name, String value)
    {
        return Locator.xpath("//input[@name='" + name + "' and @value='" + value + "']");
    }
    

    protected void customizeURLTest()
    {
        this.pushLocation();
        {
            createList("C", Ccolumns, Cdata);
            createList("B", Bcolumns, Bdata);
            createList("A", Acolumns, Adata);

            beginAt("/query/" + EscapeUtil.encode(PROJECT_NAME) + "/executeQuery.view?schemaName=lists&query.queryName=A");

            pushLocation();
            {
                clickLinkWithText("one A");
                assertElementPresent(inputWithValue("table","A"));
                assertElementPresent(inputWithValue("title","one A"));
                assertElementPresent(inputWithValue("key","1"));
            }
            popLocation();

            pushLocation();
            {
                clickLinkWithText("one B");
                assertLinkPresentWithText("one B");
                assertLinkPresentWithText("one C");
            }
            popLocation();

            // show all columns
            CustomizeViewsHelper.openCustomizeViewPanel(this);
            CustomizeViewsHelper.addCustomizeViewColumn(this, "Bfk/B", "Bfk B");
            CustomizeViewsHelper.addCustomizeViewColumn(this, "Bfk/title", "Bfk Title");
            CustomizeViewsHelper.addCustomizeViewColumn(this, "Bfk/Cfk", "Bfk Cfk");
            CustomizeViewsHelper.addCustomizeViewColumn(this, "Bfk/Cfk/C", "Bfk Cfk C");
            CustomizeViewsHelper.addCustomizeViewColumn(this, "Bfk/Cfk/title", "Bfk Cfk Title");
            CustomizeViewsHelper.saveCustomView(this, "allColumns");

            clickLinkWithText("one C", 1);
            assertElementPresent(inputWithValue("key","1"));
            assertElementPresent(inputWithValue("table","C"));
            assertElementPresent(inputWithValue("title","one C"));
            assertTrue(getCurrentRelativeURL().contains("/junit/" + EscapeUtil.encode(PROJECT_NAME) + "/echoForm.view"));
        }        
        popLocation();
    }



    void dataregionToEditDesign()
    {
        clickNavButton("View Design");
        clickEditDesign();
    }

    void clickDone()
    {
        if (isElementPresent(Locator.navButton("Save")))
            clickSave();
        clickNavButton("Done");
    }

    void clickImportData()
    {
        ListHelper.clickImportData(this);
    }

    void clickEditDesign()
    {
        ListHelper.clickEditDesign(this);
    }

    void clickSave()
    {
        ListHelper.clickSave(this);
    }

    void clickDeleteList()
    {
        ListHelper.clickDeleteList(this);
    }

    void selectPropertyTab(String name)
    {
        click(Locator.xpath("//span[contains(@class,'x-tab-strip-text') and text()='" + name + "']"));
    }

    void setColumnName(int index, String name)
    {
        setFormElement(Locator.name("ff_name"+index), name);
        TAB(Locator.name("ff_name" + index));
    }
    void setColumnLabel(int index, String label)
    {
        setFormElement(Locator.name("ff_label"+index), label);
        TAB(Locator.name("ff_label"+index));
    }
    void setColumnType(int index, ListHelper.ListColumnType type)
    {
        setFormElement(Locator.name("ff_type" + index), type.toString());
        TAB(Locator.name("ff_type" + index));
    }
    void TAB(Locator l)
    {
        ListHelper.TAB(this, l);
    }
}
