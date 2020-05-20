/*
 * Copyright (c) 2018-2019 LabKey Corporation
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

package org.labkey.test.tests.list;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.DomainResponse;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.remoteapi.domain.SaveDomainCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.Hosting;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.components.domain.ConditionalFormatDialog;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.list.AdvancedListSettingsDialog;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.tests.AuditLogTest;
import org.labkey.test.util.AbstractDataRegionExportOrSignHelper.ColumnHeaderType;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.ListHelper.ListColumn;
import org.labkey.test.util.ListHelper.LookupInfo;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.TextSearcher;
import org.labkey.test.util.search.SearchAdminAPIHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.DataRegionTable.DataRegion;
import static org.labkey.test.util.ListHelper.ListColumnType;

@Category({DailyA.class, Data.class, Hosting.class})
@BaseWebDriverTest.ClassTimeout(minutes = 14)
public class ListTest extends BaseWebDriverTest
{
    protected final static String PROJECT_VERIFY = "ListVerifyProject" ;//+ TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    private final static String PROJECT_OTHER = "OtherListVerifyProject";
    protected final static String LIST_NAME_COLORS = TRICKY_CHARACTERS_NO_QUOTES + "Colors";
    protected final static ListColumnType LIST_KEY_TYPE = ListColumnType.String;
    protected final static String LIST_KEY_NAME = "Key";
    protected final static String LIST_KEY_NAME2 = "Color";
    protected final static String LIST_DESCRIPTION = "A list of colors and what they are like";
    protected final static String FAKE_COL1_NAME = "FakeName";
    protected final static String ALIASED_KEY_NAME = "Material";
    protected final static String HIDDEN_TEXT = "CantSeeMe";

    protected final ListColumn _listCol1Fake = new ListColumn(FAKE_COL1_NAME, FAKE_COL1_NAME, ListColumnType.String, "What the color is like");
    protected final ListColumn _listCol1 = new ListColumn("Desc", "Description", ListColumnType.String, "What the color is like");
    protected final ListColumn _listCol2 = new ListColumn("Month", "Month to Wear", ListColumnType.DateAndTime, "When to wear the color", "M");
    protected final ListColumn _listCol3 = new ListColumn("JewelTone", "Jewel Tone", ListColumnType.Boolean, "Am I a jewel tone?");
    protected final ListColumn _listCol4 = new ListColumn("Good", "Quality", ListColumnType.Integer, "How nice the color is");
    protected final ListColumn _listCol5 = new ListColumn("HiddenColumn", HIDDEN_TEXT, ListColumnType.String, "I should be hidden!");
    protected final ListColumn _listCol6 = new ListColumn("Aliased,Column", "Element", ListColumnType.String, "I show aliased data.");
    protected final static String[][] TEST_DATA = {
            { "Blue", "Green", "Red", "Yellow" },
            { "Light", "Mellow", "Robust", "ZanzibarMasinginiTanzaniaAfrica" },
            { "true", "false", "true", "false"},
            { "1", "4", "3", "2" },
            { "10", "9", "8", "7"},
            { "Water", "Earth", "Fire", "Air"}};
    protected final static String[] CONVERTED_MONTHS = { "2000-01-01", "2000-04-04", "2000-03-03", "2000-02-02" };
    private final static String LIST_ROW1 = TEST_DATA[0][0] + "\t" + TEST_DATA[1][0] + "\t" + TEST_DATA[2][0] + "\t" + CONVERTED_MONTHS[0];
    private final static String LIST_ROW2 = TEST_DATA[0][1] + "\t" + TEST_DATA[1][1] + "\t" + TEST_DATA[2][1] + "\t" + CONVERTED_MONTHS[1];
    private final static String LIST_ROW3 = TEST_DATA[0][2] + "\t" + TEST_DATA[1][2] + "\t" + TEST_DATA[2][2] + "\t" + CONVERTED_MONTHS[2];
    private final String LIST_DATA =
            LIST_KEY_NAME2 + "\t" + FAKE_COL1_NAME + "\t" + _listCol3.getName() + "\t" + _listCol2.getName() + "\n" +
            LIST_ROW1 + "\n" +
            LIST_ROW2 + "\n" +
            LIST_ROW3;
    private final String LIST_DATA2 =
            LIST_KEY_NAME2 + "\t" + FAKE_COL1_NAME + "\t" + _listCol3.getName() + "\t" + _listCol2.getName() + "\t" + _listCol4.getName() + "\t" + ALIASED_KEY_NAME + "\t" + _listCol5.getName() + "\n" +
            LIST_ROW1 + "\t" + TEST_DATA[4][0] + "\t" + TEST_DATA[5][0] + "\t" + HIDDEN_TEXT + "\n" +
            LIST_ROW2 + "\t" + TEST_DATA[4][1] + "\t" + TEST_DATA[5][1] + "\t" + HIDDEN_TEXT + "\n" +
            LIST_ROW3 + "\t" + TEST_DATA[4][2] + "\t" + TEST_DATA[5][2] + "\t" + HIDDEN_TEXT;
    private final static String TEST_FAIL = "testfail";
    private final static String TEST_FAIL2 = "testfail\n2\n";
    private final String TEST_FAIL3 = LIST_KEY_NAME2 + "\t" + FAKE_COL1_NAME + "\t" + _listCol2.getName() + "\n" +
            LIST_ROW1 + "\t" + "String";
    private final static String TEST_VIEW = "list_view";
    private final static String LIST2_NAME_CARS = TRICKY_CHARACTERS_NO_QUOTES + "Cars";
    protected final static ListColumnType LIST2_KEY_TYPE = ListColumnType.String;
    protected final static String LIST2_KEY_NAME = "Car";

    protected final ListColumn _list2Col1 = new ListColumn(LIST_KEY_NAME2, LIST_KEY_NAME2, LIST2_KEY_TYPE, "The color of the car", new LookupInfo(null, "lists", LIST_NAME_COLORS).setTableType(FieldDefinition.ColumnType.String));
    private final static String LIST2_KEY = "Car1";
    private final static String LIST2_FOREIGN_KEY = "Blue";
    private final static String LIST2_KEY2 = "Car2";
    private final static String LIST2_FOREIGN_KEY2 = "Green";
    private final static String LIST2_FOREIGN_KEY_OUTSIDE = "Guy";
    private final static String LIST2_KEY3 = "Car3";
    private final static String LIST2_FOREIGN_KEY3 = "Red";
    private final static String LIST2_KEY4 = "Car4";
    private final static String LIST2_FOREIGN_KEY4 = "Brown";
    private final static String LIST3_NAME_OWNERS = "Owners";
    private final static ListColumnType LIST3_KEY_TYPE = ListColumnType.String;
    private final static String LIST3_KEY_NAME = "Owner";
    private final ListColumn _list3Col2 = new ListColumn("Wealth", "Wealth", ListColumnType.String, "");
    protected final ListColumn _list3Col1 = new ListColumn(LIST3_KEY_NAME, LIST3_KEY_NAME, LIST3_KEY_TYPE, "Who owns the car", new LookupInfo("/" + PROJECT_OTHER, "lists", LIST3_NAME_OWNERS).setTableType(FieldDefinition.ColumnType.String));
    private final static String LIST3_COL2 = "Rich";
    private final String LIST2_DATA =
            LIST2_KEY_NAME + "\t" + _list2Col1.getName()  + "\t" + LIST3_KEY_NAME + "\n" +
            LIST2_KEY + "\t" + LIST2_FOREIGN_KEY + "\n" +
            LIST2_KEY2  + "\t" + LIST2_FOREIGN_KEY2 + "\t" + LIST2_FOREIGN_KEY_OUTSIDE + "\n" +
            LIST2_KEY3  + "\t" + LIST2_FOREIGN_KEY3 + "\n" +
            LIST2_KEY4  + "\t" + LIST2_FOREIGN_KEY4;
    private final String LIST3_DATA =
            LIST3_KEY_NAME + "\t" + _list3Col2.getName() + "\n" +
            LIST2_FOREIGN_KEY_OUTSIDE + "\t" + LIST3_COL2;
    public static final String LIST_AUDIT_EVENT = "List events";
    public static final String DOMAIN_AUDIT_EVENT = "Domain events";

    private final File EXCEL_DATA_FILE = TestFileUtils.getSampleData("dataLoading/excel/fruits.xls");
    private final File TSV_DATA_FILE = TestFileUtils.getSampleData("dataLoading/excel/fruits.tsv");
    private final File EXCEL_APILIST_FILE = TestFileUtils.getSampleData("dataLoading/excel/ClientAPITestList.xls");
    private final File TSV_SAMPLE_FILE = TestFileUtils.getSampleData("fileTypes/tsv_sample.tsv");
    private final String TSV_LIST_NAME = "Fruits from TSV";

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("list");
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_VERIFY;
    }

    @BeforeClass
    public static void setupProject()
    {
        ListTest init = (ListTest)getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        log("Setup project and list module");
        _containerHelper.createProject(PROJECT_VERIFY, null);

        log("Create second project");
        _containerHelper.createProject(PROJECT_OTHER, null);
        goToProjectHome();
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _containerHelper.deleteProject(PROJECT_OTHER, afterTest);
    }

    @Before
    public void preTest()
    {
        goToProjectHome();
        if (isElementPresent(PortalHelper.Locators.webPartTitle("Search")))
            new PortalHelper(this).removeWebPart("Search");
    }

    @Override
    protected Set<String> getOrphanedViews()
    {
        Set<String> views = new HashSet<>();
        views.add(TEST_VIEW);
        return views;
    }

    @LogMethod
    protected void setUpListFinish()
    {
        // delete existing rows
        log("Test deleting rows");
        DataRegionTable table = new DataRegionTable("query", getDriver());
        table.checkAllOnPage();
        table.deleteSelectedRows();
        // load test data
        _listHelper.clickImportData();
        setFormElement(Locator.name("text"), LIST_DATA2);
        submitImportTsv();
    }

    @LogMethod
    protected void setUpList(String projectName)
    {
        // TODO: Break this up into explicit test cases and remove redundant test coverage.
        // But at least now it's only called from the one test case that relies on this list, testCustomViews().
        // Previously it was called from the @BeforeClass method, even though none of the other test cases use this list.

        log("Add list -- " + LIST_NAME_COLORS);
        _listHelper.createList(projectName, LIST_NAME_COLORS, LIST_KEY_TYPE, LIST_KEY_NAME2, _listCol1Fake, _listCol2, _listCol3);

        log("Add description and test edit");
        _listHelper.goToEditDesign(LIST_NAME_COLORS)
            .setDescription(LIST_DESCRIPTION)
            .clickSave();

        log("Test upload data");
        _listHelper.clickImportData();
        submitImportTsv("Form contains no data");

        setFormElement(Locator.id("tsv3"), TEST_FAIL);
        submitImportTsv("No rows were inserted.");

        setFormElement(Locator.id("tsv3"), TEST_FAIL2);
        submitImportTsv("Data does not contain required field: Color");

        setFormElement(Locator.id("tsv3"), TEST_FAIL3);
        submitImportTsv("Could not convert");
        setFormElement(Locator.id("tsv3"), LIST_DATA);
        submitImportTsv();

        log("Check upload worked correctly");
        assertTextPresent(
                _listCol2.getLabel(),
                TEST_DATA[0][0],
                TEST_DATA[1][1],
                TEST_DATA[3][2]);

        DataRegionTable table = new DataRegionTable("query", getDriver());
        assertEquals(TEST_DATA[2][0], table.getDataAsText(table.getRowIndex(TEST_DATA[0][0]), _listCol3.getLabel()));
        assertEquals(TEST_DATA[2][1], table.getDataAsText(table.getRowIndex(TEST_DATA[0][1]), _listCol3.getLabel()));
        assertEquals(TEST_DATA[2][2], table.getDataAsText(table.getRowIndex(TEST_DATA[0][2]), _listCol3.getLabel()));

        log("Test check/uncheck of checkboxes");
        // Second row (Green)
        assertEquals(1, table.getRowIndex(TEST_DATA[0][1]));
        clickAndWait(table.updateLink(1));
        setFormElement(Locator.name("quf_" + _listCol2.getName()), CONVERTED_MONTHS[1]);  // Has a funny format -- need to post converted date
        checkCheckbox(Locator.checkboxByName("quf_JewelTone"));
        clickButton("Submit");
        // Third row (Red)
        assertEquals(2, table.getRowIndex(TEST_DATA[0][2]));
        clickAndWait(table.updateLink(2));
        setFormElement(Locator.name("quf_" + _listCol2.getName()), CONVERTED_MONTHS[2]);  // Has a funny format -- need to post converted date
        uncheckCheckbox(Locator.checkboxByName("quf_JewelTone"));
        clickButton("Submit");

        table = new DataRegionTable("query", getDriver());
        assertEquals(TEST_DATA[2][0], table.getDataAsText(table.getRowIndex(TEST_DATA[0][0]), _listCol3.getLabel()));
        assertEquals("true", table.getDataAsText(table.getRowIndex(TEST_DATA[0][1]), _listCol3.getLabel()));
        assertEquals("false", table.getDataAsText(table.getRowIndex(TEST_DATA[0][2]), _listCol3.getLabel()));

        log("Test edit and adding new field with imported data present");
        clickTab("List");
        _listHelper.goToList(LIST_NAME_COLORS);
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(LIST_NAME_COLORS);
        DomainFormPanel fieldsPanel = listDefinitionPage.getFieldsPanel();
        fieldsPanel.getField(1)
            .setName(_listCol1.getName())
            .setLabel(_listCol1.getLabel());
        fieldsPanel.addField(_listCol4);

        // Create "Hidden Field" and remove from all views.
        fieldsPanel.addField(_listCol5);
        fieldsPanel.getField(_listCol5.getName())
            .showFieldOnDefaultView(false)
            .showFieldOnInsertView(false)
            .showFieldOnUpdateView(false)
            .showFieldOnDetailsView(false);

        fieldsPanel.addField(_listCol6);
        fieldsPanel.getField(_listCol6.getName())
            .setImportAliases(ALIASED_KEY_NAME);

        listDefinitionPage.clickSave();

        log("Check new field was added correctly");
        assertTextPresent(_listCol4.getName());

        log("Set title field of 'Colors' to 'Desc'");
        listDefinitionPage = _listHelper.goToEditDesign(LIST_NAME_COLORS);
        listDefinitionPage.openAdvancedListSettings().setFieldUsedForDisplayTitle("Desc").clickApply();
        listDefinitionPage.clickSave();

        assertTextPresent(
                TEST_DATA[0][0],
                TEST_DATA[1][1],
                TEST_DATA[3][2]);

        assertTextNotPresent(HIDDEN_TEXT); // Hidden from Grid view.

        setUpListFinish();

        log("Check that data was added correctly");
        assertTextPresent(
                TEST_DATA[0][0],
                TEST_DATA[1][1],
                TEST_DATA[3][2],
                TEST_DATA[4][0],
                TEST_DATA[4][1],
                TEST_DATA[4][2],
                TEST_DATA[5][0],
                TEST_DATA[5][1],
                TEST_DATA[5][2]);

        log("Check that hidden column is hidden.");
        DataRegionTable regionTable = new DataRegionTable("query", getDriver());
        clickAndWait(regionTable.detailsLink(0));
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from details view.
        clickButton("Edit");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from update view.
        clickButton("Cancel");

        log("Test inserting new row");
        regionTable = new DataRegionTable("query", getDriver());
        regionTable.clickInsertNewRow();
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from insert view.
        String html = getHtmlSource();
        assertTrue("Description \"" + _listCol1.getDescription() + "\" not present.", html.contains(_listCol1.getDescription()));
        assertTrue("Description \"" + _listCol3.getDescription() + "\" not present.", html.contains(_listCol3.getDescription()));
        setFormElement(Locator.name("quf_" + _listCol1.getName()), TEST_DATA[1][3]);
        setFormElement(Locator.name("quf_" + _listCol2.getName()), "wrong type");
        // Jewel Tone checkbox is left blank -- we'll make sure it's posted as false below
        setFormElement(Locator.name("quf_" + _listCol4.getName()), TEST_DATA[4][3]);
        clickButton("Submit");
        assertTextPresent("This field is required");
        setFormElement(Locator.name("quf_" + LIST_KEY_NAME2), TEST_DATA[0][3]);
        clickButton("Submit");
        assertTextPresent("Could not convert");
        setFormElement(Locator.name("quf_" + _listCol2.getName()), CONVERTED_MONTHS[3]);
        clickButton("Submit");

        log("Check new row was added");
        assertTextPresent(
                TEST_DATA[0][3],
                TEST_DATA[1][3],
                TEST_DATA[2][3],
                TEST_DATA[3][3]);
        table = new DataRegionTable("query", getDriver());
        assertEquals(TEST_DATA[2][2], table.getDataAsText(2, _listCol3.getLabel()));
        assertEquals(3, table.getRowIndex(TEST_DATA[0][3]));
        assertEquals(TEST_DATA[2][3], table.getDataAsText(3, _listCol3.getLabel()));

        log("Check hidden field is hidden only where specified.");
        listDefinitionPage = _listHelper.goToEditDesign(LIST_NAME_COLORS);
        fieldsPanel = listDefinitionPage.getFieldsPanel();
        fieldsPanel.getField(5) // Select Hidden field.
            .showFieldOnDefaultView(true);
        listDefinitionPage.clickSave();

        log("Check that hidden column is hidden.");
        assertTextPresent(HIDDEN_TEXT); // Not hidden from grid view.
        table = new DataRegionTable("query", getDriver());
        clickAndWait(table.detailsLink(0));
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from details view.
        assertTextBefore(_listCol2.getLabel(), _listCol3.getLabel());
        clickButton("Edit");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from update view.
        assertTextBefore(_listCol2.getLabel(), _listCol3.getLabel());
        clickButton("Cancel");
        table.clickInsertNewRow();
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from insert view.
        assertTextBefore(_listCol2.getLabel(), _listCol3.getLabel());
        clickButton("Cancel");

        listDefinitionPage = _listHelper.goToEditDesign(LIST_NAME_COLORS);
        fieldsPanel = listDefinitionPage.getFieldsPanel();
        fieldsPanel.getField(5) // Select Hidden field.
            .showFieldOnDefaultView(false)
            .showFieldOnInsertView(true);
        listDefinitionPage.clickSave();

        assertTextNotPresent(HIDDEN_TEXT); // Hidden from grid view.
        table = new DataRegionTable("query", getDriver());
        clickAndWait(table.detailsLink(0));
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from details view.
        clickButton("Edit");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from update view.
        clickButton("Cancel");
        table.clickInsertNewRow();
        assertTextPresent(HIDDEN_TEXT); // Not hidden from insert view.
        clickButton("Cancel");

        listDefinitionPage = _listHelper.goToEditDesign(LIST_NAME_COLORS);
        fieldsPanel = listDefinitionPage.getFieldsPanel();
        fieldsPanel.getField(5) // Select Hidden field.
            .showFieldOnInsertView(false)
            .showFieldOnUpdateView(true);
        listDefinitionPage.clickSave();

        assertTextNotPresent(HIDDEN_TEXT); // Hidden from grid view.
        table = new DataRegionTable("query", getDriver());
        clickAndWait(table.detailsLink(0));
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from details view.
        clickButton("Edit");
        assertTextPresent(HIDDEN_TEXT); // Not hidden from update view.
        clickButton("Cancel");
        table.clickInsertNewRow();
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from insert view.
        clickButton("Cancel");

        listDefinitionPage = _listHelper.goToEditDesign(LIST_NAME_COLORS);
        fieldsPanel = listDefinitionPage.getFieldsPanel();
        fieldsPanel.getField(5) // Select Hidden field.
            .showFieldOnUpdateView(false)
            .showFieldOnDetailsView(true);
        listDefinitionPage.clickSave();

        assertTextNotPresent(HIDDEN_TEXT); // Hidden from grid view.
        table = new DataRegionTable("query", getDriver());
        clickAndWait(table.detailsLink(0));
        assertTextPresent(HIDDEN_TEXT); // Not hidden from details view.
        clickButton("Edit");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from update view.
        clickButton("Cancel");
        table.clickInsertNewRow();
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from insert view.
        clickButton("Cancel");
    }

    @Test
    public void testCustomViews()
    {
        goToProjectHome();
        setUpList(getProjectName());

        goToProjectHome();
        clickAndWait(Locator.linkWithText(LIST_NAME_COLORS));

        log("Test Sort and Filter in Data View");
        DataRegionTable region = new DataRegionTable("query", getDriver());
        region.setSort(_listCol1.getName(), SortDirection.ASC);
        assertTextBefore(TEST_DATA[0][0], TEST_DATA[0][1]);

        clearSortTest();

        region.setFilter(_listCol4.getName(), "Is Greater Than", "7");
        assertTextNotPresent(TEST_DATA[0][3]);

        log("Test Customize View");
        // Re-navigate to the list to clear filters and sorts
        clickTab("List");
        clickAndWait(Locator.linkWithText(LIST_NAME_COLORS));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeColumn(_listCol4.getName());
        _customizeViewsHelper.addFilter(_listCol4.getName(), _listCol4.getLabel(), "Is Less Than", "10");
        _customizeViewsHelper.addSort(_listCol2.getName(), _listCol2.getLabel(), SortDirection.ASC);
        _customizeViewsHelper.saveCustomView(TEST_VIEW);

        log("Check Customize View worked");
        assertTextPresent(TEST_DATA[0][3]);
        assertTextPresentInThisOrder(TEST_DATA[0][3], TEST_DATA[0][2], TEST_DATA[0][1]);
        assertTextNotPresent(TEST_DATA[0][0], _listCol4.getLabel());

        log("4725: Check Customize View can't remove all fields");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeColumn(LIST_KEY_NAME2);
        _customizeViewsHelper.removeColumn(_listCol1.getName());
        _customizeViewsHelper.removeColumn(_listCol2.getName());
        _customizeViewsHelper.removeColumn(_listCol3.getName());
        _customizeViewsHelper.removeColumn(EscapeUtil.fieldKeyEncodePart(_listCol6.getName()));
        _customizeViewsHelper.clickViewGrid();
        assertExt4MsgBox("You must select at least one field to display in the grid.", "OK");
        _customizeViewsHelper.closePanel();

        log("Test Export");

        File tableFile = new DataRegionExportHelper(new DataRegionTable("query", getDriver())).exportText();
        TextSearcher tsvSearcher = new TextSearcher(tableFile);

        assertTextPresentInThisOrder(tsvSearcher, TEST_DATA[0][3], TEST_DATA[0][2], TEST_DATA[0][1]);
        assertTextNotPresent(tsvSearcher, TEST_DATA[0][0], _listCol4.getLabel());
        filterTest();

        clickProject(getProjectName());

        log("Test that sort only affects one web part");
        DataRegionTable firstList = DataRegionTable.DataRegion(getDriver()).find();
        DataRegionTable secondList = DataRegionTable.DataRegion(getDriver()).index(1).find();
        firstList.setSort(_listCol4.getName(), SortDirection.ASC);
        List<String> expectedColumn = new ArrayList<>(Arrays.asList(TEST_DATA[4]));
        List<String> firstListColumn = secondList.getColumnDataAsText(_listCol4.getName());
        assertEquals("Second query webpart shouldn't have been sorted", expectedColumn, firstListColumn);
        expectedColumn.sort(Comparator.comparingInt(Integer::parseInt)); // Parse to check sorting of 10 vs 7, 8, 9
        List<String> secondListColumn = firstList.getColumnDataAsText(_listCol4.getName());
        assertEquals("First query webpart should have been sorted", expectedColumn, secondListColumn);

        log("Test list history");
        clickAndWait(Locator.linkWithText("manage lists"));
        clickAndWait(Locator.linkWithText("view history"));
        assertTextPresent(":History");
        assertTextPresent("record was modified", 2);    // An existing list record was modified
        assertTextPresent("were modified", 8);          // The column(s) of domain ></% 1äöüColors were modified
        assertTextPresent("Bulk inserted", 2);
        assertTextPresent("A new list record was inserted", 1);
        assertTextPresent("was created", 2);                // Once for the list, once for the domain
        // List insert/update events should each have a link to the list item that was modified, but the other events won't have a link
        assertEquals("details Links", 6, DataRegionTable.detailsLinkLocator().findElements(getDriver()).size());
        assertEquals("Project Links", 18, DataRegionTable.Locators.table().append(Locator.linkWithText(PROJECT_VERIFY)).findElements(getDriver()).size());
        assertEquals("List Links", 18, DataRegionTable.Locators.table().append(Locator.linkWithText(LIST_NAME_COLORS)).findElements(getDriver()).size());
        DataRegionTable dataRegionTable = new DataRegionTable("query", getDriver());
        dataRegionTable.clickRowDetails(0);
        assertTextPresent("List Item Details");
        assertTextNotPresent("No details available for this event.", "Unable to find the audit history detail for this event");

        clickButton("Done");
        clickAndWait(Locator.linkWithText(PROJECT_VERIFY).index(3));

        log("Test single list web part");
        new PortalHelper(this).addWebPart("List - Single");
        setFormElement(Locator.name("title"), "This is my single list web part title");
        _ext4Helper.selectComboBoxItem("List:", LIST_NAME_COLORS);
        clickButton("Submit");
        waitForText(DataRegionTable.getImportBulkDataText());
        assertTextPresent("View Design");
        new DataRegionTable.DataRegionFinder(getDriver()).index(2).waitFor();
        Locator loc = Locator.linkWithSpan("This is my single list web part title");
        scrollIntoView(loc);
        clickAndWait(loc, WAIT_FOR_PAGE);
        assertTextPresent("Colors", "Views");

        log("Add List -- " + LIST3_NAME_OWNERS);
        _listHelper.createList(PROJECT_OTHER, LIST3_NAME_OWNERS, LIST3_KEY_TYPE, LIST3_KEY_NAME, _list3Col2);

        log("Upload data to second list");
        _listHelper.goToList(LIST3_NAME_OWNERS);
        _listHelper.uploadData(LIST3_DATA);

        log("Add list -- " + LIST2_NAME_CARS);
        _listHelper.createList(PROJECT_VERIFY, LIST2_NAME_CARS, LIST2_KEY_TYPE, LIST2_KEY_NAME, _list2Col1, _list3Col1);

        log("Upload data to second list");
        _listHelper.goToList(LIST2_NAME_CARS);
        _listHelper.uploadData(LIST2_DATA);

        log("Check that upload worked");
        assertTextPresent(
                LIST2_KEY,
                LIST2_KEY2,
                LIST2_KEY3,
                LIST2_KEY4);

        log("Check that reference worked");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn(_list2Col1.getName() + "/" + _listCol1.getName(), _list2Col1.getLabel() + " " + _listCol1.getLabel());
        _customizeViewsHelper.addColumn(_list2Col1.getName() + "/" + _listCol2.getName(), _list2Col1.getLabel() + " " + _listCol2.getLabel());
        _customizeViewsHelper.addColumn(_list2Col1.getName() + "/" + _listCol4.getName(), _list2Col1.getLabel() + " " + _listCol4.getLabel());
        _customizeViewsHelper.addFilter(_list2Col1.getName() + "/" + _listCol4.getName(), _listCol4.getLabel(), "Is Less Than", "10");
        _customizeViewsHelper.addSort(_list2Col1.getName() + "/" + _listCol4.getName(), _listCol4.getLabel(), SortDirection.ASC);
        _customizeViewsHelper.addColumn(_list3Col1.getName() + "/" + _list3Col1.getName(), _list3Col1.getLabel() + " " + _list3Col1.getLabel());
        _customizeViewsHelper.addColumn(_list3Col1.getName() + "/" + _list3Col2.getName(), _list3Col1.getLabel() + " " + _list3Col2.getLabel());
        _customizeViewsHelper.saveCustomView(TEST_VIEW);

        log("Check adding referenced fields worked");
        waitForText(WAIT_FOR_JAVASCRIPT, _listCol1.getLabel());
        assertTextPresent(
                _listCol1.getLabel(),
                _listCol2.getLabel(),
                _listCol4.getLabel(),
                LIST2_FOREIGN_KEY_OUTSIDE,
                LIST3_COL2);
        assertTextNotPresent(LIST2_KEY);
        assertTextBefore(LIST2_KEY3, LIST2_KEY2);
        assertTextNotPresent(LIST2_KEY4);

        log("Test export");
        DataRegionTable list = new DataRegionTable("query", getDriver());
        waitForElement(Locator.tagWithAttribute("a", "data-original-title", "Delete"));

        DataRegionExportHelper helper = new DataRegionExportHelper(list);
        File expFile = helper.exportText(ColumnHeaderType.FieldKey, DataRegionExportHelper.TextSeparator.COMMA);
        TextSearcher srch = new TextSearcher(expFile);
        assertTextPresent(srch, LIST_KEY_NAME2 + '/' + _listCol1.getName(),
                LIST_KEY_NAME2 + '/' + _listCol2.getName(),
                LIST_KEY_NAME2 + '/' + _listCol4.getName(),
                LIST2_FOREIGN_KEY_OUTSIDE,
                LIST3_COL2);
        assertTextNotPresent(srch, LIST2_KEY, LIST2_KEY4);
        assertTextPresentInThisOrder(srch, LIST2_KEY3, LIST2_KEY2);

        log("Test edit row");
        list.updateRow(LIST2_KEY3, Maps.of(
                "Color", TEST_DATA[1][1],
                "Owner", LIST2_FOREIGN_KEY_OUTSIDE));

        final DataRegionTable dt = DataRegion(getDriver()).withName("query").find();
        dt.goToView("default");
        assertTextPresent(TEST_DATA[1][1], 2);

        log("Test deleting rows");
        dataRegionTable.checkAll();
        doAndWaitForPageToLoad(() ->
        {
            dt.clickHeaderButton("Delete");
            assertAlert("Are you sure you want to delete the selected rows?");
        });
        assertEquals("Failed to delete all rows", 0, dataRegionTable.getDataRowCount());
        assertTextNotPresent(LIST2_KEY, LIST2_KEY2, LIST2_KEY3, LIST2_KEY4);

        log("Test deleting data (should any list custom views)");
        clickTab("List");
        clickAndWait(Locator.linkWithText(LIST_NAME_COLORS));
        _listHelper.deleteList("Custom view '" + TEST_VIEW + "'");

        log("Test that deletion happened");
        assertTextNotPresent(LIST_NAME_COLORS);
        clickAndWait(Locator.linkWithText(LIST2_NAME_CARS));
        _customizeViewsHelper.openCustomizeViewPanel();
        waitForElement(Locator.tagWithAttribute("tr", "data-recordid", LIST3_KEY_NAME.toUpperCase()));
        assertElementNotPresent(Locator.tagWithAttribute("tr", "data-recordid", LIST_KEY_NAME.toUpperCase()));
        goToProjectHome();
        assertTextPresent("query not found");

        log("Test exporting a nonexistent list returns a 404");
        String exportUrl = "/" + EscapeUtil.encode(PROJECT_VERIFY) + "/query-exportRowsTsv.view?schemaName=lists&query.queryName=" + EscapeUtil.encode(LIST_NAME_COLORS);
        beginAt(exportUrl);
        assertEquals("Incorrect response code", 404, getResponseCode());
        assertTextPresent("Query '" + LIST_NAME_COLORS + "' in schema 'lists' doesn't exist.");

        clickButton("Folder");
        // after the 13.2 audit log migration, we are no longer going to co-mingle domain and list events in the same table
        AuditLogTest.verifyAuditEvent(this, DOMAIN_AUDIT_EVENT, AuditLogTest.COMMENT_COLUMN, "The domain " + LIST_NAME_COLORS + " was deleted", 5);
        AuditLogTest.verifyAuditEvent(this, LIST_AUDIT_EVENT, AuditLogTest.COMMENT_COLUMN, "An existing list record was deleted", 5);
        AuditLogTest.verifyAuditEvent(this, LIST_AUDIT_EVENT, AuditLogTest.COMMENT_COLUMN, "An existing list record was modified", 10);

        customizeURLTest();
        crossContainerLookupTest();
    }

    /* Issue 23487: add regression coverage for batch insert into list with multiple errors
    */
    @Test
    public void testBatchInsertErrors() throws IOException, CommandException
    {
        // create the list for this case
        String multiErrorListName = "multiErrorBatchList";
        String[] expectedErrors = new String[]{
                "Could not convert 'green' for field ShouldInsertCorrectly, should be of type Boolean",
                "Could not convert 'five' for field Id, should be of type Integer; Missing value for required property: Id"
        };

        createList(multiErrorListName, BatchListColumns, BatchListData);
        beginAt("/query/" + EscapeUtil.encode(PROJECT_VERIFY) + "/executeQuery.view?schemaName=lists&query.queryName=" + multiErrorListName);
        _listHelper.clickImportData();

        // insert the new list data and verify the expected errors appear
        setListImportAsTestDataField(toTSV(BatchListColumns, BatchListExtraData), expectedErrors);

        // no need to query the list; nothing will be inserted if the batch insert fails/errors
    }

    @Test
    public void testAddListColumnOverRemoteAPI() throws Exception
    {
        List<FieldDefinition> cols = Arrays.asList(
               new FieldDefinition("name", FieldDefinition.ColumnType.String),
                new FieldDefinition("title", FieldDefinition.ColumnType.String),
                new FieldDefinition("dewey", FieldDefinition.ColumnType.Decimal)
        );
        String listName = "remoteApiListTestAddColumn";
        FieldDefinition.LookupInfo info = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        TestDataGenerator dgen = new TestDataGenerator(info)
                .withColumns(cols);
        DomainResponse createResponse = dgen.createList(createDefaultConnection(true), "key");
        Domain listDomain = createResponse.getDomain();
        List<PropertyDescriptor> listFields = createResponse.getDomain().getFields();
        listFields.add(new FieldDefinition("volume", FieldDefinition.ColumnType.Decimal));
        listDomain.setFields(listFields);

        // now save with an extra field
        SaveDomainCommand saveCmd = new SaveDomainCommand(info.getSchema(), info.getTable());
        saveCmd.setDomainDesign(listDomain);
        DomainResponse saveResponse = saveCmd.execute(createDefaultConnection(true), info.getFolder());

        // now verify
        assertEquals(listFields.size(), saveResponse.getDomain().getFields().size());
        for (PropertyDescriptor expectedField : listFields)
        {
            checker().verifyTrue( "expect field [" + expectedField.getName() + "] with type [" +expectedField.getRangeURI()+ "]",
                    saveResponse.getDomain().getFields().stream()
                    .anyMatch(a -> a.getName().equals(expectedField.getName()) &&
                            a.getRangeURI().endsWith(expectedField.getRangeURI())));
        }
    }

    @Test
    public void testRemoveColumnOverAPI() throws Exception
    {
        List<FieldDefinition> cols = Arrays.asList(
                new FieldDefinition("name", FieldDefinition.ColumnType.String),
                new FieldDefinition("title", FieldDefinition.ColumnType.String),
                new FieldDefinition("dewey", FieldDefinition.ColumnType.Decimal),
                new FieldDefinition("removeMe", FieldDefinition.ColumnType.Decimal)
        );
        String listName = "remoteApiListTestRemoveColumn";
        FieldDefinition.LookupInfo info = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        TestDataGenerator dgen = new TestDataGenerator(info)
                .withColumns(cols);
        DomainResponse createResponse = dgen.createList(createDefaultConnection(true), "key");
        Domain listDomain = createResponse.getDomain();
        List<PropertyDescriptor> listFields = createResponse.getDomain().getFields();
        listFields.removeIf(a-> a.getName().equals("removeMe"));
        listDomain.setFields(listFields);

        SaveDomainCommand saveCmd = new SaveDomainCommand(info.getSchema(), info.getTable());
        saveCmd.setDomainDesign(listDomain);
        DomainResponse saveResponse = saveCmd.execute(createDefaultConnection(true), info.getFolder());

        checker().verifyFalse("'removeMe' field was not deleted.",
                saveResponse.getDomain().getFields().stream()
                        .anyMatch(a -> a.getName().equals("removeMe")));
    }

    @Test
    @Ignore // ignore until remoteAPI supports rename
    public void testChangeListName() throws Exception
    {
        List<FieldDefinition> cols = Arrays.asList(
                new FieldDefinition("name", FieldDefinition.ColumnType.String),
                new FieldDefinition("title", FieldDefinition.ColumnType.String),
                new FieldDefinition("dewey", FieldDefinition.ColumnType.Decimal)
        );
        String listName = "remoteAPIBeforeRename";
        FieldDefinition.LookupInfo info = new FieldDefinition.LookupInfo(getProjectName(), "lists", listName);
        TestDataGenerator dgen = new TestDataGenerator(info)
                .withColumns(cols);
        DomainResponse createResponse = dgen.createList(createDefaultConnection(true), "key");
        Domain listDomain = createResponse.getDomain();
        listDomain.setName("remoteAPIAfterRename");

        SaveDomainCommand saveCmd = new SaveDomainCommand(listDomain.getDomainId());
        saveCmd.setDomainDesign(listDomain);
        DomainResponse saveResponse = saveCmd.execute(createDefaultConnection(true), info.getFolder());

        assertEquals("remoteAPIAfterRename", saveResponse.getDomain().getName());
    }

    /*  Issue 6883: Create test for list self join
        Issue 10394: Test spaces & special characters in table/column names

        - Create a new list (use special characters)
        - Add a field (use special characters)
        - Make it a lookup linked back to the list itself

        preconditions:  ListVerifyProject
    */
    @Test
    public void listSelfJoinTest()
    {
        final String listName = "listSelfJoin" + TRICKY_CHARACTERS;
        final String dummyBase = "dummyCol";
        final String dummyCol = dummyBase + TRICKY_CHARACTERS;
        final String lookupField = "lookupField" + TRICKY_CHARACTERS;
        final String lookupSchema = "lists";
        final String lookupTable = listName;
        final String keyCol = "Key &%<+";

        log("Issue 6883: test list self join");

        ListHelper.ListColumn[] columns = new ListHelper.ListColumn[] {
                new ListHelper.ListColumn(dummyCol, dummyCol, ListColumnType.String, "")
        };
        ListHelper.ListColumn lookupCol = new ListHelper.ListColumn(lookupField, lookupField, ListColumnType.Integer, "",
                new ListHelper.LookupInfo(null, lookupSchema, lookupTable).setTableType(FieldDefinition.ColumnType.Integer));
        // create the list
        _listHelper.createList(PROJECT_VERIFY, listName, ListColumnType.AutoInteger, keyCol, columns);
        // now add the lookup column (which references the new table)
        _listHelper.goToEditDesign(listName)
                .addField(lookupCol)
                .clickSave();

        _listHelper.goToList(listName);
        assertTextPresent(dummyBase);
        assertTextNotPresent("An unexpected error");
        Map<String, String> row = new HashMap<>();
        row.put(dummyCol, "dummy one");
        _listHelper.insertNewRow(row);

        DataRegionTable regionTable = new DataRegionTable("query", getDriver());
        clickAndWait(regionTable.detailsLink(0));
        assertTextPresent("dummy one");
        clickButton("Edit");
        assertTextPresent("dummy one");
        clickButton("Cancel");
        clickAndWait(regionTable.updateLink(0));
        assertTextPresent("dummy one");
        clickButton("Cancel");
    }

    String crossContainerLookupList = "CCLL";
    @LogMethod
    private void crossContainerLookupTest()
    {
        goToProjectHome(PROJECT_OTHER);
        //create list with look up A
        String lookupColumn = "lookup";
        _listHelper.createList(PROJECT_OTHER, crossContainerLookupList, ListColumnType.AutoInteger, "Key",  col(PROJECT_VERIFY, lookupColumn, ListColumnType.Integer, "A" ));
        _listHelper.goToList(crossContainerLookupList);
        _listHelper.clickImportData();
        setListImportAsTestDataField(lookupColumn + "\n1");

        log("verify look column set properly");
        assertTextPresent("one A");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("lookup/Bfk/Cfk/title");
        _customizeViewsHelper.saveCustomView();

        clickAndWait(Locator.linkContainingText("one C"));
        assertElementPresent(Locator.xpath("//input[@type='submit']"));
        goBack();


        //add columns to look all the way to C
    }

    @LogMethod
    private void filterTest()
    {
        log("Filter Test");
        clickProject(PROJECT_VERIFY);

        PortalHelper portalHelper = new PortalHelper(this);
        portalHelper.addQueryWebPart(null, "lists", LIST_NAME_COLORS, null);
        portalHelper.addQueryWebPart(null, "lists", LIST_NAME_COLORS, null);

        log("Test that the right filters are present for each type");
        DataRegionTable region = new DataRegionTable("qwp3", getDriver());
        region.openFilterDialog(_listCol4.getName());
        _extHelper.clickExtTab("Choose Filters");
        click(Locator.xpath("//div[" + Locator.NOT_HIDDEN + " and ./label/span[text()='Filter Type:']]/div/div//img[contains(@class, 'x-form-arrow-trigger')]"));

        assertElementNotPresent(Locator.xpath("//div[" + Locator.NOT_HIDDEN + " and contains(@class, 'x-combo-list-item') and text()='Starts With']"));
        assertElementPresent(Locator.xpath("//div[" + Locator.NOT_HIDDEN + " and contains(@class, 'x-combo-list-item') and text()='Is Blank']"));
        click(Locator.xpath("//div[" + Locator.NOT_HIDDEN + " and ./label/span[text()='Filter Type:']]/div/div//img[contains(@class, 'x-form-arrow-trigger')]"));
        _extHelper.clickExtButton("Show Rows Where " + _listCol4.getLabel(), "Cancel", 0);

        log("Test that filters don't affect multiple web parts");
        assertTextPresent(TEST_DATA[1][0], 2);
        region.setFilter(_listCol4.getName(), "Is Less Than", "10");
        assertTextPresent(TEST_DATA[1][0], 1);

        clickAndWait(Locator.linkContainingText(LIST_NAME_COLORS));
    }

    /*  Issue 11825: Create test for "Clear Sort"
        Issue 15567: Can't sort DataRegion by column name that has comma

        sort by a parameter, than clear sort.
        Verify that reverts to original sort and the dropdown menu disappears

        preconditions:  table already sorted by description
     */
    @LogMethod
    private void clearSortTest()
    {
        //make sure elements are ordered the way they should be
        assertTextPresentInThisOrder(TEST_DATA[5][0], TEST_DATA[5][1],TEST_DATA[5][2]);

        String encodedName = EscapeUtil.fieldKeyEncodePart(_listCol6.getName());

        DataRegionTable query = new DataRegionTable("query", getDriver());

        //sort  by element and verify it worked
        query.setSort(encodedName, SortDirection.DESC);
        assertTextPresentInThisOrder(TEST_DATA[5][0], TEST_DATA[5][2], TEST_DATA[5][1]);

        //remove sort and verify we return to initial state
        query.clearSort(encodedName);
        assertTextPresentInThisOrder(TEST_DATA[5][0], TEST_DATA[5][1],TEST_DATA[5][2]);
    }

    @Test
    public void uploadAndCustomFormat()  // customFormattingTest assumes it picks up where doUploadTest leaves off
    {
        doUploadTest();
        customFormattingTest();   // todo: evaluate whether or not the custom-formatting test here is redundant to the format testing in domainDesignerTest
    }

    @LogMethod
    private void doUploadTest()
    {
        log("Infer from excel file, then import data");
        _listHelper.createListFromFile(PROJECT_VERIFY, "Fruits from Excel", EXCEL_DATA_FILE);
        _listHelper.goToList("Fruits from Excel");
        waitForElement(Locator.linkWithText("pomegranate"));
        assertNoLabKeyErrors();

        //Cancel test disabled because teamcity is too slow to run it successfully
        /*log("Infer from tsv file, but cancel before completion");
        clickProject(PROJECT_NAME);
        clickAndWait(Locator.linkWithText("manage lists"));
        clickButton("Create New List");
        waitForElement(Locator.id("ff_name"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.id("ff_name"),  TSV_LIST_NAME);
        checkCheckbox(Locator.xpath("//span[@id='fileImport']/input[@type='checkbox']"));
        clickButton("Create List", 0);
        waitForElement(Locator.xpath("//input[@name='uploadFormElement']"), WAIT_FOR_JAVASCRIPT);
        setFormElement("uploadFormElement", tsvFile);
        waitForElement(Locator.xpath("//span[@id='button_Import']"), WAIT_FOR_JAVASCRIPT);
        clickButton("Import", 0);
        waitForElement(Locator.xpath("//div[text()='Creating columns...']"), WAIT_FOR_JAVASCRIPT);
        clickButton("Cancel");
        assertTextNotPresent(TSV_LIST_NAME);*/

        log("Infer from a tsv file, then import data");
        _listHelper.createListFromFile(PROJECT_VERIFY, TSV_LIST_NAME, TSV_DATA_FILE);
        _listHelper.goToList(TSV_LIST_NAME);
        waitForElement(Locator.linkWithText("pomegranate"));
        assertNoLabKeyErrors();
        log("Verify correct types are inferred from file");
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(TSV_LIST_NAME);
        DomainFormPanel fieldsPanel = listDefinitionPage.getFieldsPanel();
        assertEquals(FieldDefinition.ColumnType.Boolean, fieldsPanel.getField("BoolCol").getType());
        assertEquals(FieldDefinition.ColumnType.Integer, fieldsPanel.getField("IntCol").getType());
        assertEquals(FieldDefinition.ColumnType.Decimal, fieldsPanel.getField("NumCol").getType());
        assertEquals(FieldDefinition.ColumnType.DateAndTime, fieldsPanel.getField("DateCol").getType());
        listDefinitionPage.clickSave();
    }

    @LogMethod
    private void customFormattingTest()
    {
        String red = "#D33115";
        String cyan = "#68CCCA";

        // Assumes we are at the list designer after doUploadTest()
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(TSV_LIST_NAME);
        DomainFormPanel fieldsPanel = listDefinitionPage.getFieldsPanel();

        // Set conditional format on boolean column. Bold, italic, strikethrough, cyan text, red background
        DomainFieldRow boolField = fieldsPanel.getField("BoolCol");
        ConditionalFormatDialog formatDlg = boolField.clickConditionalFormatButton();
        formatDlg.getOpenFormatPanel()
            .setFirstValue("true")
            .setBoldCheckbox(true)
            .setItalicsCheckbox(true)
            .setStrikethroughCheckbox(true)
            .setFillColor(red) //red background
            .setTextColor(cyan); //cyan text (Issue 11435: set text color)
        formatDlg.clickApply();

        // Set multiple conditional formats on int column.
        DomainFieldRow intField = fieldsPanel.getField("IntCol");
        formatDlg = intField.clickConditionalFormatButton();
        // If greater than 7, strikethrough
        formatDlg.getOpenFormatPanel()
            .setFirstCondition(Filter.Operator.GT)
            .setFirstValue("7")
            .setStrikethroughCheckbox(true);
        // If greater than 5, Bold
        formatDlg.addFormatPanel()
            .setFirstCondition(Filter.Operator.GT)
            .setFirstValue("5")
            .setBoldCheckbox(true);
        formatDlg.clickApply();

        listDefinitionPage.clickSave();

        // Verify conditional format of boolean column
        // look for cells that do not match the
        assertTextPresent(TSV_LIST_NAME);
        assertElementNotPresent(Locator.xpath("//td[text() = 'true' and not(contains(@style, 'line-through'))]"));
        assertElementNotPresent(Locator.xpath("//td[text() = 'true' and not(contains(@style, 'bold'))]"));
        assertElementNotPresent(Locator.xpath("//td[text() = 'true' and not(contains(@style, 'italic'))]"));
        assertElementNotPresent(Locator.xpath("//td[text() = 'true' and not(contains(@style, 'color: rgb(0, 255, 255)') or contains(@style, 'color: " + cyan.toLowerCase() + "'))]")); // Cyan text
        assertElementNotPresent(Locator.xpath("//td[text() = 'true' and not(contains(@style, 'background-color: rgb(255, 0, 0)') or contains(@style, 'color: " + red.toLowerCase() + "'))]")); // Red background
        assertElementNotPresent(Locator.xpath("//td[text() = 'false' and @style]")); // No style on false items
        assertElementPresent(Locator.xpath("//td[text()='5' and not(contains(@style, 'bold')) and not(contains(@style, 'line-through'))]"));
        assertElementPresent(Locator.xpath("//td[text()='6' and contains(@style, 'bold') and not(contains(@style, 'line-through'))]"));
        assertElementPresent(Locator.xpath("//td[text()='8' and contains(@style, 'line-through') and not(contains(@style, 'bold'))]"));

        // Check for appropriate tooltips
        assertElementNotPresent(Locator.id("helpDivBody")
                        .withText("Formatting applied because column > 5."));
        Actions builder = new Actions(getDriver());
        builder.moveToElement(Locator.xpath("//td[text()='6' and contains(@style, 'bold')]").findElement(getDriver())).build().perform();
        // Tooltip doesn't show instantly, so wait for a bit
        shortWait().until(ExpectedConditions.visibilityOf(Locator.id("helpDivBody")
                .withText("Formatting applied because column > 5.").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT)));
        click(Locator.css("img[alt=close]"));
        // Tooltip doesn't hide instantly, so wait for a bit
        shortWait().until(ExpectedConditions.invisibilityOfElementLocated(By.id("helpDiv")));

        assertElementNotPresent(Locator.id("helpDivBody")
                        .withText("Formatting applied because column = true."));
        builder.moveToElement(Locator.xpath("//td[text()='true']").findElement(getDriver())).build().perform();
        // Tooltip doesn't show instantly, so wait for a bit
        shortWait().until(ExpectedConditions.visibilityOf(Locator.id("helpDivBody")
                .withText("Formatting applied because column = true.").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT)));
        click(Locator.css("img[alt=close]"));
        // Tooltip doesn't hide instantly, so wait for a bit
        shortWait().until(ExpectedConditions.invisibilityOfElementLocated(By.id("helpDiv")));
    }

    @Test
    public void doRenameFieldsTest()
    {
        log("8329: Test that renaming a field then creating a new field with the old name doesn't result in awful things");
        String listName = "new";
        String origFieldName = "BarBar";
        String newFieldName = "FooFoo";
        _listHelper.createList(PROJECT_VERIFY, listName, ListColumnType.AutoInteger, "key", new ListColumn(origFieldName, origFieldName, ListColumnType.String, "first column"));

        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(listName);
        listDefinitionPage.getFieldsPanel()
                .getField(origFieldName)
                .setName(newFieldName)
                .setLabel(newFieldName);
        listDefinitionPage.clickSave();

        assertTextPresent(newFieldName);
        assertTextNotPresent(origFieldName);

        listDefinitionPage = _listHelper.goToEditDesign(listName);
        ListColumn newCol = new ListColumn(origFieldName, origFieldName, ListColumnType.String, "second column");
        listDefinitionPage.addField(newCol);
        listDefinitionPage.clickSave();

        assertTextBefore(newFieldName, origFieldName);
    }

    @Test
    public void exportPhiFileColumn() throws Exception
    {
        goToProjectHome(PROJECT_VERIFY);
        String listName = "phiFileColumnList";
        String notPhiColumn = "NotPhiColumn";
        String limitedPhiColumn = "LimitedPhiColumn";
        String phiColumn = "PhiColumn";
        String restrictedPhiColumn = "RestrictedPhiColumn";
        _listHelper.createList(PROJECT_VERIFY, listName, ListColumnType.AutoInteger, "key",
                new ListColumn("FileName", "FileName", ListColumnType.String, "name of the file"),
                new ListColumn("FileExtension", "ext", ListColumnType.String, "the file extension"),
                new ListColumn(notPhiColumn, "NotPhiFile", ListColumnType.Attachment, "the file itself"),
                new ListColumn(limitedPhiColumn, "LimitedPhiFile", ListColumnType.Attachment, "the file itself"),
                new ListColumn(phiColumn, "PhiFile", ListColumnType.Attachment, "the file itself"),
                new ListColumn(restrictedPhiColumn, "RestrictedFile", ListColumnType.Attachment, "the file itself"));


        // set phi levels
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(listName);
        listDefinitionPage.setColumnPhiLevel("NotPhiColumn", PropertiesEditor.PhiSelectType.NotPHI);
        listDefinitionPage.setColumnPhiLevel("LimitedPhiColumn", PropertiesEditor.PhiSelectType.Limited);
        listDefinitionPage.setColumnPhiLevel("PhiColumn", PropertiesEditor.PhiSelectType.PHI);
        listDefinitionPage.setColumnPhiLevel("RestrictedPhiColumn", PropertiesEditor.PhiSelectType.Restricted);
        listDefinitionPage.clickSave();

        goToProjectHome();
        clickAndWait(Locator.linkWithText(listName));

        // add rows to list
        Map<String, String> xlsRow = new HashMap<>();
        xlsRow.put(notPhiColumn, EXCEL_APILIST_FILE.getAbsolutePath());
        xlsRow.put("FileName", EXCEL_DATA_FILE.getName());
        xlsRow.put("FileExtension", ".xls");
        xlsRow.put(limitedPhiColumn, EXCEL_DATA_FILE.getAbsolutePath());
        _listHelper.insertNewRow(xlsRow, false);

        Map<String, String> tsvRow = new HashMap<>();
        tsvRow.put(phiColumn, TSV_SAMPLE_FILE.getAbsolutePath());
        tsvRow.put("FileName", TSV_DATA_FILE.getName());
        tsvRow.put("FileExtension", ".tsv");
        tsvRow.put(restrictedPhiColumn, TSV_DATA_FILE.getAbsolutePath());
        _listHelper.insertNewRow(tsvRow, false);

        // go to admin/folder/management, click 'export'
        clickAdminMenuItem("Folder", "Management");
        click(Locator.linkContainingText("Export"));
        // select 'remove all columns tagged as protected'
        new Checkbox(Locator.tagContainingText("label", "Include PHI Columns:")
                .precedingSibling("input").waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT)).uncheck();

        // click 'export', capture the zip archive download
        File projectZipArchive = clickAndWaitForDownload(findButton("Export"));

        assertFalse("Restricted PHI column attachment should not be included in export",
                TestFileUtils.isFileInZipArchive(projectZipArchive, TSV_DATA_FILE.getName()));
        assertFalse("Limited PHI column attachment should not be included in export",
                TestFileUtils.isFileInZipArchive(projectZipArchive, EXCEL_DATA_FILE.getName()));
        assertTrue("Not PHI column attachment should be included in export",
                TestFileUtils.isFileInZipArchive(projectZipArchive, EXCEL_APILIST_FILE.getName()));
        assertFalse("PHI column attachment should not be included in export",
                TestFileUtils.isFileInZipArchive(projectZipArchive, TSV_SAMPLE_FILE.getName()));

        goToProjectHome();
        clickAndWait(Locator.linkWithText(listName));
        _listHelper.deleteList();
    }

    @Test
    public void testAttachmentSearch()
    {
        final String listName = "Attachment Search List";
        final String path = TestFileUtils.getSampleData("lists/searchData.tsv").getAbsolutePath();
        final String attachmentCol = "Attachment";
        final String descriptionCol = "Description";

        Map<String, String> row = new HashMap<>();
        row.put(descriptionCol, "randomText");
        row.put(attachmentCol, path);

        goToProjectHome();

        // create list with an attachment column
        _listHelper.createList(getProjectName(), listName, ListColumnType.AutoInteger, "id",
                col(descriptionCol, ListColumnType.String),
                col(attachmentCol, ListColumnType.Attachment));
        // index for entire list as single document and index on attachment column
        _listHelper.goToEditDesign(listName)
                .openAdvancedListSettings()
                .indexEntireListAsASingleDocument(true, "",
                        AdvancedListSettingsDialog.SearchIncludeOptions.MetadataAndData,
                        AdvancedListSettingsDialog.SearchIndexOptions.NonPhiText)
                .setIndexFileAttachments(true)
                .clickApply()
                .clickSave();

        // Insert data, upload attachment
        goToProjectHome();
        clickAndWait(Locator.linkWithText(listName));
        _listHelper.insertNewRow(row);

        startSystemMaintenance("SearchService");
        SearchAdminAPIHelper.waitForIndexer();

        goToProjectHome();
        new PortalHelper(this).addWebPart("Search");
        searchFor(getProjectName(), "hypertrophimadeupword", 1, null);
    }

    @Test
    public void testAttachmentColumnDeletion()
    {
        final String listName = "Attachment Column Delete List";
        final String path = TestFileUtils.getSampleData("lists/searchData.tsv").getAbsolutePath();
        final String attachmentCol = "Attachment";
        final String descriptionCol = "Description";

        Map<String, String> row = new HashMap<>();
        row.put(descriptionCol, "randomText");
        row.put(attachmentCol, path);

        goToProjectHome();

        // create list with an attachment column
        _listHelper.createList(getProjectName(), listName, ListColumnType.AutoInteger, "id",
                               col(descriptionCol, ListColumnType.String),
                               col(attachmentCol, ListColumnType.Attachment));
        // index on attachment column
        EditListDefinitionPage editListDefinitionPage = _listHelper.goToEditDesign(listName);
        editListDefinitionPage.openAdvancedListSettings()
                .setIndexFileAttachments(true)
                .clickApply() // Advanced settings
                .clickSave();

        // Insert data, upload attachment
        goToProjectHome();
        clickAndWait(Locator.linkWithText(listName));
        _listHelper.insertNewRow(row);

        // Now remove attachment column and check audit log
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(listName);
        listDefinitionPage.getFieldsPanel()
            .getField("Attachment")
            .clickRemoveField(true);
        listDefinitionPage.clickSave();
        AuditLogTest.verifyAuditEvent(this, "Attachment events", AuditLogTest.COMMENT_COLUMN, "The attachment searchData.tsv was deleted", 1);
    }

    //
    // CUSTOMIZE URL tests
    //

    ListHelper.ListColumn col(String name, ListColumnType type)
    {
        return new ListHelper.ListColumn(name, "", type, "");
    }

    ListHelper.ListColumn col(String name, ListColumnType type, String table)
    {
        return col(null, name, type, table);
    }

    ListHelper.ListColumn col(String folder, String name, ListColumnType type, String table)
    {
        return new ListHelper.ListColumn(name, "", type, "", new ListHelper.LookupInfo(folder, "lists", table).setTableType(type.toNew()));
    }

    ListHelper.ListColumn colURL(String name, ListColumnType type, String url)
    {
        ListColumn c  = new ListHelper.ListColumn(name, "", type, "");
        c.setURL(url);
        return c;
    }

    List<ListColumn> Acolumns = Arrays.asList(
            col("A", ListColumnType.Integer),
            colURL("title", ListColumnType.String, "/junit/echoForm.view?key=${A}&title=${title}&table=A"),
            col("Bfk", ListColumnType.Integer, "B")
    );
    String[][] Adata = new String[][]
    {
        {"1", "one A", "1"},
    };

    List<ListHelper.ListColumn> Bcolumns = Arrays.asList(
            col("B", ListColumnType.Integer),
            colURL("title", ListColumnType.String, "org.labkey.core.junit.JunitController$EchoFormAction.class?key=${B}&title=${title}&table=B"),
            col("Cfk", ListColumnType.Integer, "C")
    );
    String[][] Bdata = new String[][]
    {
        {"1", "one B", "1"},
    };

    List<ListHelper.ListColumn> Ccolumns = Arrays.asList(
            col("C", ListColumnType.Integer),
            colURL("title", ListColumnType.String, "/junit/echoForm.view?key=${C}&title=${title}&table=C")
    );
    String[][] Cdata = new String[][]
    {
        {"1", "one C"},
    };

    List<ListHelper.ListColumn> BatchListColumns = Arrays.asList(
            col("Id", ListColumnType.Integer),
            col("FirstName", ListColumnType.String),
            col("LastName", ListColumnType.String),
            col("IceCreamFlavor", ListColumnType.String),
            col("ShouldInsertCorrectly", ListColumnType.Boolean)
    );
    String[][] BatchListData = new String[][]
            {
                    {"1", "Joe", "Test", "Vanilla", "true"},
                    {"2", "Jane", "Test", "Rum Raisin", "true"},
                    {"3", "Jeff", "BugCatcher", "Rocky Road", "true"},
            };
    String[][] BatchListExtraData = new String[][]
            {
                    {"4", "Crash", "Test", "Vanilla", "green"},
                    {"five", "Crunch", "Test", "Rum Raisin", "false"},
                    {"6", "Will", "ShouldPass", "Rocky Road", "true"},
                    {"7", "Liam", "ShouldPass", "Chocolate", "true"},
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
        _listHelper.submitImportTsv_error(error);
    }

    void submitImportTsv()
    {
        _listHelper.submitImportTsv_success();
    }


    void createList(String name, List<ListHelper.ListColumn> cols, String[][] data)
    {
        log("Add List -- " + name);
        _listHelper.createList(PROJECT_VERIFY, name, ListColumnType.fromNew(cols.get(0).getType()), cols.get(0).getName(),
                cols.subList(1, cols.size()).toArray(new ListHelper.ListColumn[cols.size() - 1]));
        _listHelper.goToList(name);
        _listHelper.clickImportData();
        setListImportAsTestDataField(toTSV(cols,data));
    }

    private void setListImportAsTestDataField(String data, String... expectedErrors)
    {
        setFormElement(Locator.name("text"), data);
        if (expectedErrors.length == 0)
        {
            submitImportTsv();
        }
        else
        {
            _listHelper.submitImportTsv_errors(Arrays.asList(expectedErrors));
        }

    }


    Locator inputWithValue(String name, String value)
    {
        return Locator.xpath("//input[@name='" + name + "' and @value='" + value + "']");
    }

    @LogMethod
    public void customizeURLTest()
    {
        this.pushLocation();
        {
            createList("C", Ccolumns, Cdata);
            createList("B", Bcolumns, Bdata);
            createList("A", Acolumns, Adata);

            beginAt("/query/" + EscapeUtil.encode(PROJECT_VERIFY) + "/executeQuery.view?schemaName=lists&query.queryName=A");

            pushLocation();
            {
                clickAndWait(Locator.linkWithText("one A"));
                assertElementPresent(inputWithValue("table","A"));
                assertElementPresent(inputWithValue("title","one A"));
                assertElementPresent(inputWithValue("key","1"));
            }
            popLocation();

            pushLocation();
            {
                clickAndWait(Locator.linkWithText("one B"));
                assertElementPresent(Locator.linkWithText("one B"));
                assertElementPresent(Locator.linkWithText("one C"));
            }
            popLocation();

            // show all columns
            _customizeViewsHelper.openCustomizeViewPanel();
            _customizeViewsHelper.addColumn("Bfk/B", "Bfk B");
            _customizeViewsHelper.addColumn("Bfk/title", "Bfk Title");
            _customizeViewsHelper.addColumn("Bfk/Cfk", "Bfk Cfk");
            _customizeViewsHelper.addColumn("Bfk/Cfk/C", "Bfk Cfk C");
            _customizeViewsHelper.addColumn("Bfk/Cfk/title", "Bfk Cfk Title");
            _customizeViewsHelper.saveCustomView("allColumns");

            clickAndWait(Locator.linkWithText("one C").index(1));
            assertElementPresent(inputWithValue("key","1"));
            assertElementPresent(inputWithValue("table","C"));
            assertElementPresent(inputWithValue("title","one C"));
            assertTrue(getCurrentRelativeURL().contains(WebTestHelper.buildRelativeUrl("junit", PROJECT_VERIFY, "echoForm")));
        }
        popLocation();
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
