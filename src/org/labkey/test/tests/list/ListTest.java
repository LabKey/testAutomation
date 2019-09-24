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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.domain.DomainResponse;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.Hosting;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.Format;
import org.labkey.test.tests.AuditLogTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.ListHelper.ListColumn;
import org.labkey.test.util.ListHelper.LookupInfo;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.SearchHelper;
import org.labkey.test.util.TestDataGenerator;
import org.openqa.selenium.By;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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

    protected final FieldDefinition _listCol1Fake = new FieldDefinition(FAKE_COL1_NAME, FieldDefinition.ColumnType.String)
            .setLabel(FAKE_COL1_NAME).setDescription("What the color is like");
    protected final FieldDefinition _listCol1 = new FieldDefinition("Desc", FieldDefinition.ColumnType.String)
            .setLabel("Description").setDescription("What the color is like");
    protected final FieldDefinition _listCol2 = new FieldDefinition("Month", FieldDefinition.ColumnType.DateTime)
            .setLabel("Month to Wear").setDescription("When to wear the color").setFormat("M");
    protected final FieldDefinition _listCol3 = new FieldDefinition("JewelTone", FieldDefinition.ColumnType.Boolean)
            .setLabel("Jewel Tone").setDescription("Am I a jewel tone?");
    protected final FieldDefinition _listCol4 = new FieldDefinition("Good", FieldDefinition.ColumnType.Integer)
            .setLabel("Quality").setDescription("How nice the color is");
    protected final FieldDefinition _listCol5 = new FieldDefinition("HiddenColumn", FieldDefinition.ColumnType.String)
        .setLabel(HIDDEN_TEXT).setDescription("I should be hidden!").isHidden(true);
    protected final FieldDefinition _listCol6 = new FieldDefinition("Aliased,Column", FieldDefinition.ColumnType.String)
            .setLabel("Element").setDescription("I show aliased data.");
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

    protected final ListColumn _list2Col1 = new ListColumn(LIST_KEY_NAME2, LIST_KEY_NAME2, LIST2_KEY_TYPE, "The color of the car", new LookupInfo(null, "lists", LIST_NAME_COLORS));
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
    protected final ListColumn _list3Col1 = new ListColumn(LIST3_KEY_NAME, LIST3_KEY_NAME, LIST3_KEY_TYPE, "Who owns the car", new LookupInfo("/" + PROJECT_OTHER, "lists", LIST3_NAME_OWNERS));
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

    @Test
    public void testShowHideColumnInDefaultView() throws Exception
    {
        String tableName = "listWithHiddenColumn";
        String keyFieldName = "StringKeyField";
        PropertyDescriptor descriptionCol = new PropertyDescriptor("Description", FieldDefinition.ColumnType.String.getJsonType())
                .setLabel("Description").setDescription("Describes the field, yo");
        PropertyDescriptor intCol = new PropertyDescriptor("intCol", FieldDefinition.ColumnType.Integer.getJsonType())
                .setLabel("TestInteger").setDescription("test int field to be used for filter");
        PropertyDescriptor hiddenCol = new PropertyDescriptor("hidden", FieldDefinition.ColumnType.String.getJsonType())    // create the column as 'hidden' initially
                .setLabel("Hidden").setDescription("should not see me").setHidden(true);

        FieldDefinition.LookupInfo colorsLookup = new LookupInfo(getProjectName(), "lists", tableName);
        TestDataGenerator dgen = new TestDataGenerator(colorsLookup)
                .withColumns(List.of(TestDataGenerator.simpleFieldDef(keyFieldName, FieldDefinition.ColumnType.String),  // note: for list, key is hidden by default
                        descriptionCol, intCol, hiddenCol));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "IntList", Map.of("keyName", keyFieldName));

        // give the list some data to work with
        dgen.addCustomRow(Map.of(keyFieldName, "first","Description", "kindly", "intCol", 1, "hidden", "eek!"));
        dgen.addCustomRow(Map.of(keyFieldName, "second","Description", "nicely", "intCol", 2, "hidden", "eek!"));
        dgen.addCustomRow(Map.of(keyFieldName, "third","Description", "rudely", "intCol", 3, "hidden", "eek!"));
        dgen.insertRows(createDefaultConnection(true), dgen.getRows());

        goToManageLists();
        clickAndWait(Locator.linkWithText(tableName));
        DataRegionTable dt = DataRegion(getDriver()).withName("query").find();
        assertEquals("expect contents of hidden col not to be shown",
                Arrays.asList("1", "kindly"), dt.getRowDataAsText(dt.getRowIndex("intCol", "1")));
        assertEquals(Arrays.asList("intCol", "Description"), dt.getColumnNames());

        // change the default view to show 'hidden' column
        _customizeViewsHelper.openCustomizeViewPanel();
        CustomizeView cv = dt.getCustomizeView();
        cv.showHiddenItems();
        cv.addColumn("hidden");
        cv.saveDefaultView();

        // verify the 'hidden' col is shown now
        assertEquals(Arrays.asList("intCol", "Description", "hidden"), dt.getColumnNames());
        assertEquals("expect formerly-hidden data to be shown", Arrays.asList("eek!", "eek!", "eek!"), dt.getColumnDataAsText("hidden"));
    }

    @Test
    public void testSortInDefaultView() throws Exception
    {
        String tableName = "listForTestSort";
        String keyFieldName = "StringKeyField";
        PropertyDescriptor descriptionCol = new PropertyDescriptor("Description", FieldDefinition.ColumnType.String.getJsonType())
                .setLabel("Description").setDescription("Describes the field, yo");
        PropertyDescriptor intCol = new PropertyDescriptor("intCol", FieldDefinition.ColumnType.Integer.getJsonType())
                .setLabel("TestInteger").setDescription("test int field to be used for Sort");
        PropertyDescriptor hiddenCol = new PropertyDescriptor("hidden", FieldDefinition.ColumnType.String.getJsonType())    // create the column as 'hidden' initially
                .setLabel("Hidden").setDescription("should not see me").setHidden(true);

        FieldDefinition.LookupInfo colorsLookup = new LookupInfo(getProjectName(), "lists", tableName);
        TestDataGenerator dgen = new TestDataGenerator(colorsLookup)
                .withColumns(List.of(TestDataGenerator.simpleFieldDef(keyFieldName, FieldDefinition.ColumnType.String),  // note: for list, key is hidden by default
                        descriptionCol, intCol, hiddenCol));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "IntList", Map.of("keyName", keyFieldName));

        // give the list some data to work with
        dgen.addCustomRow(Map.of(keyFieldName, "first","Description", "kindly", "intCol", 1, "hidden", "eek!"));
        dgen.addCustomRow(Map.of(keyFieldName, "second","Description", "nicely", "intCol", 2, "hidden", "eek!"));
        dgen.addCustomRow(Map.of(keyFieldName, "third","Description", "rudely", "intCol", 3, "hidden", "eek!"));
        dgen.insertRows(createDefaultConnection(true), dgen.getRows());

        goToManageLists();
        clickAndWait(Locator.linkWithText(tableName));
        DataRegionTable dt = DataRegion(getDriver()).withName("query").find();

        // change the default view to show 'hidden' column
        _customizeViewsHelper.openCustomizeViewPanel();
        CustomizeView cv = dt.getCustomizeView();
        cv.addSort("intCol", SortDirection.DESC);
        cv.saveDefaultView();

        // verify the sort order is changed appropriately/as expected
        assertEquals(Arrays.asList("intCol", "Description"), dt.getColumnNames());
        assertEquals("expect row integrity",
                Arrays.asList("3", "rudely"), dt.getRowDataAsText(dt.getRowIndex("intCol", "3")));
        assertEquals("expect descending sort order", Arrays.asList("3", "2", "1"),
                dt.getColumnDataAsText("intCol"));
    }

    @Test
    public void testFilterInDefaultView() throws Exception
    {
        String tableName = "listForTestFilter";
        String keyFieldName = "StringKeyField";
        PropertyDescriptor descriptionCol = new PropertyDescriptor("Description", FieldDefinition.ColumnType.String.getJsonType())
                .setLabel("Description").setDescription("Describes the field, yo");
        PropertyDescriptor intCol = new PropertyDescriptor("intCol", FieldDefinition.ColumnType.Integer.getJsonType())
                .setLabel("TestInteger").setDescription("test int field to be used for Filtering");
        PropertyDescriptor hiddenCol = new PropertyDescriptor("hidden", FieldDefinition.ColumnType.String.getJsonType())    // create the column as 'hidden' initially
                .setLabel("Hidden").setDescription("should not see me").setHidden(true);

        FieldDefinition.LookupInfo colorsLookup = new LookupInfo(getProjectName(), "lists", tableName);
        TestDataGenerator dgen = new TestDataGenerator(colorsLookup)
                .withColumns(List.of(TestDataGenerator.simpleFieldDef(keyFieldName, FieldDefinition.ColumnType.String),  // note: for list, key is hidden by default
                        descriptionCol, intCol, hiddenCol));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "IntList", Map.of("keyName", keyFieldName));

        // give the list some data to work with
        dgen.addCustomRow(Map.of(keyFieldName, "first","Description", "kindly", "intCol", 1, "hidden", "eek!"));
        dgen.addCustomRow(Map.of(keyFieldName, "second","Description", "nicely", "intCol", 2, "hidden", "eek!"));
        dgen.addCustomRow(Map.of(keyFieldName, "third","Description", "rudely", "intCol", 3, "hidden", "eek!"));
        dgen.addCustomRow(Map.of(keyFieldName, "fourth","Description", "excessively", "intCol", 4, "hidden", "eek!"));
        dgen.addCustomRow(Map.of(keyFieldName, "fifth","Description", "madly", "intCol", 5, "hidden", "eek!"));
        dgen.insertRows(createDefaultConnection(true), dgen.getRows());

        goToManageLists();
        clickAndWait(Locator.linkWithText(tableName));
        DataRegionTable dt = DataRegion(getDriver()).withName("query").find();

        // change the default view to filter out values 4 or above, and to sort desc by intCol
        _customizeViewsHelper.openCustomizeViewPanel();
        CustomizeView cv = dt.getCustomizeView();
        cv.addFilter("intCol", "intCol", "Is Less Than", "4");
        cv.addSort("intCol", SortDirection.DESC);
        cv.saveDefaultView();

        // verify the sort order is changed appropriately/as expected, and only values 3 or less are shown
        assertEquals(Arrays.asList("intCol", "Description"), dt.getColumnNames());
        assertEquals("expect row integrity",
                Arrays.asList("3", "rudely"), dt.getRowDataAsText(dt.getRowIndex("intCol", "3")));
        assertEquals("expect descending sort order", Arrays.asList("3", "2", "1"),
                dt.getColumnDataAsText("intCol"));
    }

    /**
     * coverage for 4725: Check Customize View can't remove all fields
     * @throws Exception
     */
    @Test
    public void verifyNotAllColumnsCanBeHiddenInView() throws Exception
    {
        String tableName = "viewCannotHideAllColumnsTest";
        String keyFieldName = "StringKeyField";

        FieldDefinition.LookupInfo colorsLookup = new LookupInfo(getProjectName(), "lists", tableName);
        TestDataGenerator dgen = new TestDataGenerator(colorsLookup)
                .withColumns(List.of(TestDataGenerator.simpleFieldDef(keyFieldName, FieldDefinition.ColumnType.String),  // note: for list, key is hidden by default
                        TestDataGenerator.simpleFieldDef("field1", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("field2", FieldDefinition.ColumnType.String),
                        TestDataGenerator.simpleFieldDef("field3", FieldDefinition.ColumnType.String)));
        DomainResponse createResponse = dgen.createDomain(createDefaultConnection(true), "IntList", Map.of("keyName", keyFieldName));

        // give the list some data to work with
        dgen.addCustomRow(Map.of(keyFieldName, "first","field1", "kindly", "field2", "argy", "field3", "eek!"));
        dgen.addCustomRow(Map.of(keyFieldName, "second","field1", "nicely", "field2", "bargy", "field3", "eek!"));
        dgen.addCustomRow(Map.of(keyFieldName, "third","field1", "rudely", "field2", "whatnot", "field3", "eek!"));
        dgen.insertRows(createDefaultConnection(true), dgen.getRows());

        goToManageLists();
        clickAndWait(Locator.linkWithText(tableName));
        DataRegionTable dt = DataRegion(getDriver()).withName("query").find();


        // change the default view to hide all visible columns
        _customizeViewsHelper.openCustomizeViewPanel();
        CustomizeView cv = dt.getCustomizeView();
        cv.showHiddenItems();
        cv.removeColumn("field1");
        cv.removeColumn("field2");
        cv.removeColumn("field3");
        _customizeViewsHelper.clickViewGrid();
        assertExt4MsgBox("You must select at least one field to display in the grid.", "OK");
        _customizeViewsHelper.closePanel();
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
                new ListHelper.ListColumn(dummyCol, dummyCol, ListColumnType.String, ""),
                new ListHelper.ListColumn(lookupField, lookupField, ListColumnType.String, "", new ListHelper.LookupInfo(null, lookupSchema, lookupTable))
        };
        _listHelper.createList(PROJECT_VERIFY, listName, ListColumnType.AutoInteger, keyCol, columns);
        clickButton("Done");
        clickAndWait(Locator.linkWithText(listName));
        waitForElement(Locator.xpath("//table[@lk-region-name='query']"));
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
    //@LogMethod

    @Test
    public void crossContainerLookupTest()
    {
        goToProjectHome(PROJECT_OTHER);
        //create list with look up A
        String lookupColumn = "lookup";
        _listHelper.createList(PROJECT_OTHER, crossContainerLookupList, ListColumnType.AutoInteger, "Key",  col(PROJECT_VERIFY, lookupColumn, ListColumnType.Integer, "A" ));
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
    public void filterTest()
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
        customFormattingTest();
    }

    @LogMethod
    private void doUploadTest()
    {
        log("Infer from excel file, then import data");
        _listHelper.createListFromFile(PROJECT_VERIFY, "Fruits from Excel", EXCEL_DATA_FILE);
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
        waitForElement(Locator.linkWithText("pomegranate"));
        assertNoLabKeyErrors();
        log("Verify correct types are inferred from file");
        clickButton("Design");
        waitForElement(Locator.xpath("//tr[./td/div[text()='BoolCol'] and ./td/div[text()='Boolean']]"), WAIT_FOR_JAVASCRIPT);
        assertElementPresent(Locator.xpath("//tr[./td/div[text()='IntCol'] and ./td/div[text()='Integer']]"));
        assertElementPresent(Locator.xpath("//tr[./td/div[text()='NumCol'] and ./td/div[text()='Number (Double)']]"));
        assertElementPresent(Locator.xpath("//tr[./td/div[text()='DateCol'] and ./td/div[text()='DateTime']]"));
    }

    @LogMethod
    private void customFormattingTest()
    {
        // Assumes we are at the list designer after doUploadTest()

        clickButton("Edit Design", 0);

        // Set conditional format on boolean column. Bold, italic, strikethrough, cyan text, red background
        PropertiesEditor editor = _listHelper.getListFieldEditor();

        PropertiesEditor.FieldRow row = editor.selectField("BoolCol");
        PropertiesEditor.FieldPropertyDock.FormatTabPane tabPane = row.properties().selectFormatTab();
        tabPane.addConditionalFormat("true", new Format.Builder().setBold(true).setItalics(true).setStrikethrough(true).build());
        click(Locator.xpath("//div[@title='Color']"));
        waitForElement(Locator.xpath("//div[contains(@class, 'gwt-DialogBox')]//div[contains(@class, 'Caption') and text()='Conditional Format Colors']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//fieldset[./legend/span[text()='Background']]//input"), "FF0000"); // Red background
        // don't know why this is necessary TODO: investigate
        click(Locator.id("button_OK"));
        click(Locator.id("button_OK"));
        waitForElementToDisappear(Locator.xpath("//div[contains(@class, 'gwt-DialogBox')]//div[contains(@class, 'Caption') and text()='Conditional Format Colors']"), WAIT_FOR_JAVASCRIPT);
        // Regression test for Issue 11435: reopen color dialog to set text color
        click(Locator.xpath("//div[@title='Color']"));
        waitForElement(Locator.xpath("//div[contains(@class, 'gwt-DialogBox')]//div[contains(@class, 'Caption') and text()='Conditional Format Colors']"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.xpath("//fieldset[./legend/span[text()='Foreground']]//input"), "00FFFF"); // Cyan text
        // don't know why this is necessary TODO: investigate
        click(Locator.id("button_OK"));
        click(Locator.id("button_OK"));
        waitForElementToDisappear(Locator.xpath("//div[contains(@class, 'gwt-DialogBox')]//div[contains(@class, 'Caption') and text()='Conditional Format Colors']"), WAIT_FOR_JAVASCRIPT);

        // Set multiple conditional formats on int column.
        row = editor.selectField("IntCol");
        tabPane = row.properties().selectFormatTab();
        // If greater than 5, Bold
        tabPane.addConditionalFormat("Is Greater Than", "5", Format.BOLD);
        // If greater than 7, strikethrough
        tabPane.addConditionalFormat("Is Greater Than", "7", Format.STRIKETHROUGH);

        // Switch the order of filters so that >7 takes precedence over >5
        dragAndDrop(Locator.xpath("//div[text()='Is Greater Than 5']"), Locator.xpath("//div[text()='Is Greater Than 7']"));
        assertTextBefore("Is Greater Than 7", "Is Greater Than 5");

        _listHelper.clickSave();
        clickButton("Done", defaultWaitForPage);

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
        _listHelper.createList(PROJECT_VERIFY, "new", ListColumnType.AutoInteger, "key", new ListColumn("BarBar", "BarBar", ListColumnType.String, "Some new column"));
        assertTextPresent("BarBar");
        _listHelper.clickEditDesign();
        setColumnName(1, "FooFoo");
        setColumnLabel(1, "");
        _listHelper.clickSave();
        assertTextPresent("FooFoo");
        assertTextNotPresent("BarBar");
        _listHelper.clickEditDesign();
        ListHelper listHelper = new ListHelper(this);
        ListColumn newCol = new ListColumn("BarBar", "BarBar", ListColumnType.String, "None");
        listHelper.addField(newCol);
        _listHelper.clickSave();
        assertTextBefore("FooFoo", "BarBar");
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
        _listHelper.clickEditDesign();

        // set phi levels
        PropertiesEditor listFieldEditor = _listHelper.getListFieldEditor();
        listFieldEditor.selectField("NotPhiColumn");
        listFieldEditor.fieldProperties().selectAdvancedTab().setPhiLevel(PropertiesEditor.PhiSelectType.NotPHI);
        listFieldEditor.selectField("LimitedPhiColumn");
        listFieldEditor.fieldProperties().selectAdvancedTab().setPhiLevel(PropertiesEditor.PhiSelectType.Limited);
        listFieldEditor.selectField("PhiColumn");
        listFieldEditor.fieldProperties().selectAdvancedTab().setPhiLevel(PropertiesEditor.PhiSelectType.PHI);
        listFieldEditor.selectField("RestrictedPhiColumn");
        listFieldEditor.fieldProperties().selectAdvancedTab().setPhiLevel(PropertiesEditor.PhiSelectType.Restricted);

        _listHelper.clickSave();
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
        // index on attachment column
        _listHelper.clickEditDesign();
        _listHelper.checkIndexFileAttachements(true);
        _listHelper.clickSave();

        // Insert data, upload attachment
        goToProjectHome();
        clickAndWait(Locator.linkWithText(listName));
        _listHelper.insertNewRow(row);

        startSystemMaintenance("SearchService");
        SearchHelper.waitForIndexer();

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
        _listHelper.clickEditDesign();
        _listHelper.checkIndexFileAttachements(true);
        _listHelper.clickSave();

        // Insert data, upload attachment
        goToProjectHome();
        clickAndWait(Locator.linkWithText(listName));
        _listHelper.insertNewRow(row);

        // Now remove attachment column and check audit log
        dataregionToEditDesign();
        _listHelper.getListFieldEditor().selectField(2).markForDeletion();
        _listHelper.clickSave();
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
        return new ListHelper.ListColumn(name, "", type, "", new ListHelper.LookupInfo(folder, "lists", table));
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
        _listHelper.clickEditDesign();
        selectOptionByText(Locator.id("ff_titleColumn"), cols.get(1).getName());    // Explicitly set to the PK (auto title will pick wealth column)
        _listHelper.clickSave();
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

   // @LogMethod
    @Test
    public void customizeURLTest()
    {
        goToProjectHome();
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

            goToProjectHome();
    }

    void dataregionToEditDesign()
    {
        clickButton("Design");
        _listHelper.clickEditDesign();
    }

    void clickDone()
    {
        if (isElementPresent(Locator.lkButton("Save")))
            _listHelper.clickSave();
        clickButton("Done");
    }

    void selectPropertyTab(String name)
    {
        click(Locator.xpath("//span[contains(@class,'x-tab-strip-text') and text()='" + name + "']"));
    }

    void setColumnName(int index, String name)
    {
        Locator nameLoc = Locator.name("ff_name"+index);
        click(nameLoc);
        setFormElement(nameLoc, name);
        pressTab(nameLoc);
    }
    void setColumnLabel(int index, String label)
    {
        Locator labelLoc = Locator.name("ff_label"+index);
        click(labelLoc);
        setFormElement(labelLoc, label);
        pressTab(labelLoc);
    }
    void setColumnType(int index, ListColumnType type)
    {
        Locator typeLoc = Locator.name("ff_type"+index);
        click(typeLoc);
        setFormElement(typeLoc, type.toString());
        pressTab(typeLoc);
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
