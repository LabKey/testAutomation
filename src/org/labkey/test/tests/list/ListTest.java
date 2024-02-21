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

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.domain.Domain;
import org.labkey.remoteapi.domain.DomainResponse;
import org.labkey.remoteapi.domain.PropertyDescriptor;
import org.labkey.remoteapi.domain.SaveDomainCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.Hosting;
import org.labkey.test.components.domain.BaseDomainDesigner;
import org.labkey.test.components.domain.ConditionalFormatDialog;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.list.AdvancedListSettingsDialog;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.core.admin.LookAndFeelSettingsPage;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.LookupInfo;
import org.labkey.test.tests.AuditLogTest;
import org.labkey.test.util.AbstractDataRegionExportOrSignHelper.ColumnHeaderType;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.ListHelper.ListColumn;
import org.labkey.test.util.ListHelper.ListColumnType;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.TestDateUtils;
import org.labkey.test.util.TextSearcher;
import org.labkey.test.util.search.SearchAdminAPIHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.params.FieldDefinition.ColumnType;
import static org.labkey.test.util.DataRegionTable.DataRegion;

@Category({Daily.class, Data.class, Hosting.class})
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
    protected final static String FAKE_COL_NAME = "FakeName";
    protected final static String ALIASED_KEY_NAME = "Material";
    protected final static String HIDDEN_TEXT = "CantSeeMe";

    protected final FieldDefinition _listColFake = new FieldDefinition(FAKE_COL_NAME, ColumnType.String).setDescription("What the color is like");
    protected final FieldDefinition _listColDesc = new FieldDefinition("Desc", ColumnType.String).setLabel("Description").setDescription("What the color is like");

    protected final FieldDefinition _listColMonth = new FieldDefinition("Month", FieldDefinition.ColumnType.TextChoice)
        .setTextChoiceValues(List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"))
        .setLabel("Month to Wear").setDescription("When to wear the color");


    protected final FieldDefinition _listColTone = new FieldDefinition("JewelTone", ColumnType.Boolean).setLabel("Jewel Tone").setDescription("Am I a jewel tone?");
    protected final FieldDefinition _listColGood = new FieldDefinition("Good", ColumnType.Integer).setLabel("Quality").setDescription("How nice the color is");
    protected final FieldDefinition _listColHidden = new FieldDefinition("HiddenColumn", ColumnType.String).setLabel(HIDDEN_TEXT).setDescription("I should be hidden!");
    protected final FieldDefinition _listColAliased = new FieldDefinition("Aliased,Column", ColumnType.String).setLabel("Element").setDescription("I show aliased data.");

    private static final int TD_COLOR = 0;
    private static final int TD_DESC = 1;
    private static final int TD_TONE = 2;
    private static final int TD_MONTH = 3;
    private static final int TD_GOOD = 4;
    private static final int TD_ALIAS = 5;

    protected final static String[] VALID_MONTHS = { "Jan", "Apr", "Mar", "Feb" };
    protected final static String[][] TEST_DATA = {
            { "Blue", "Green", "Red", "Yellow" },
            { "Light", "Mellow", "Robust", "ZanzibarMasinginiTanzaniaAfrica" },
            { "true", "false", "true", "false"},
            VALID_MONTHS,
            { "10", "9", "8", "7"},
            { "Water", "Earth", "Fire", "Air"}};
    private final static String LIST_ROW1 = TEST_DATA[TD_COLOR][0] + "\t" + TEST_DATA[TD_DESC][0] + "\t" + TEST_DATA[TD_TONE][0] + "\t" + VALID_MONTHS[0];
    private final static String LIST_ROW2 = TEST_DATA[TD_COLOR][1] + "\t" + TEST_DATA[TD_DESC][1] + "\t" + TEST_DATA[TD_TONE][1] + "\t" + VALID_MONTHS[1];
    private final static String LIST_ROW3 = TEST_DATA[TD_COLOR][2] + "\t" + TEST_DATA[TD_DESC][2] + "\t" + TEST_DATA[TD_TONE][2] + "\t" + VALID_MONTHS[2];
    private final String LIST_DATA =
            LIST_KEY_NAME2 + "\t" + FAKE_COL_NAME + "\t" + _listColTone.getName() + "\t" + _listColMonth.getName() + "\n" +
            LIST_ROW1 + "\n" +
            LIST_ROW2 + "\n" +
            LIST_ROW3;
    private final String LIST_DATA2 =
            LIST_KEY_NAME2 + "\t" + FAKE_COL_NAME + "\t" + _listColTone.getName() + "\t" + _listColMonth.getName() + "\t" + _listColGood.getName() + "\t" + ALIASED_KEY_NAME + "\t" + _listColHidden.getName() + "\n" +
            LIST_ROW1 + "\t" + TEST_DATA[TD_GOOD][0] + "\t" + TEST_DATA[TD_ALIAS][0] + "\t" + HIDDEN_TEXT + "\n" +
            LIST_ROW2 + "\t" + TEST_DATA[TD_GOOD][1] + "\t" + TEST_DATA[TD_ALIAS][1] + "\t" + HIDDEN_TEXT + "\n" +
            LIST_ROW3 + "\t" + TEST_DATA[TD_GOOD][2] + "\t" + TEST_DATA[TD_ALIAS][2] + "\t" + HIDDEN_TEXT;
    private final static String TEST_FAIL = "testfail";
    private final static String TEST_FAIL2 = "testfail\n2\n";
    private final String TEST_FAIL3 = LIST_KEY_NAME2 + "\t" + FAKE_COL_NAME + "\t" + _listColMonth.getName() + "\n" +
            LIST_ROW1;
    private final static String TEST_VIEW = "list_view";
    private final static String LIST2_NAME_CARS = TRICKY_CHARACTERS_NO_QUOTES + "Cars";
    protected final static ListColumnType LIST2_KEY_TYPE = ListColumnType.String;
    protected final static String LIST2_KEY_NAME = "Car";

    protected final FieldDefinition _list2Col1 = new FieldDefinition(LIST_KEY_NAME2, new LookupInfo(null, "lists", LIST_NAME_COLORS).setTableType(FieldDefinition.ColumnType.String)).setDescription("The color of the car");
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
    private final FieldDefinition _list3Col2 = new FieldDefinition("Wealth", ColumnType.String);
    protected final FieldDefinition _list3Col1 = new FieldDefinition(LIST3_KEY_NAME, new LookupInfo("/" + PROJECT_OTHER, "lists", LIST3_NAME_OWNERS).setTableType(FieldDefinition.ColumnType.String)).setDescription("Who owns the car");
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

    @Override
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

    @Override
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
        _listHelper.clickImportData()
                .setText(LIST_DATA2)
                .submit();
    }

    @LogMethod
    protected void setUpList(String projectName)
    {
        // TODO: Break this up into explicit test cases and remove redundant test coverage.
        // But at least now it's only called from the one test case that relies on this list, testCustomViews().
        // Previously it was called from the @BeforeClass method, even though none of the other test cases use this list.

        log("Add list -- " + LIST_NAME_COLORS);
        _listHelper.createList(projectName, LIST_NAME_COLORS, LIST_KEY_TYPE, LIST_KEY_NAME2, _listColFake,
        _listColMonth, _listColTone);

        log("Add description and test edit");
        _listHelper.goToEditDesign(LIST_NAME_COLORS)
            .setDescription(LIST_DESCRIPTION)
            .clickSave();

        log("Test upload data");
        ImportDataPage importDataPage = _listHelper.clickImportData();
        importDataPage.submitExpectingErrorContaining("Form contains no data");

        importDataPage.setText(TEST_FAIL);
        importDataPage.submitExpectingErrorContaining("No rows were inserted.");

        importDataPage.setText(TEST_FAIL2);
        importDataPage.submitExpectingErrorContaining("Data does not contain required field: Color");

        importDataPage.setText(TEST_FAIL3);
        importDataPage.submitExpectingErrorContaining(String.format("Value 'true' for field '%s' is invalid.", _listColMonth.getLabel()));

        importDataPage.setText(LIST_DATA);
        importDataPage.submit();

        log("Check upload worked correctly");
        assertTextPresent(
                _listColMonth.getLabel(),
                TEST_DATA[TD_COLOR][0],
                TEST_DATA[TD_DESC][1],
                TEST_DATA[TD_MONTH][2]);

        DataRegionTable table = new DataRegionTable("query", getDriver());
        assertEquals(TEST_DATA[TD_TONE][0], table.getDataAsText(table.getRowIndex(TEST_DATA[TD_COLOR][0]), _listColTone.getLabel()));
        assertEquals(TEST_DATA[TD_TONE][1], table.getDataAsText(table.getRowIndex(TEST_DATA[TD_COLOR][1]), _listColTone.getLabel()));
        assertEquals(TEST_DATA[TD_TONE][2], table.getDataAsText(table.getRowIndex(TEST_DATA[TD_COLOR][2]), _listColTone.getLabel()));

        log("Test check/uncheck of checkboxes");
        // Second row (Green)
        assertEquals(1, table.getRowIndex(TEST_DATA[TD_COLOR][1]));
        clickAndWait(table.updateLink(1));
        setFormElement(Locator.name("quf_" + _listColMonth.getName()), VALID_MONTHS[1]);  // Has a funny format -- need to post converted date
        checkCheckbox(Locator.checkboxByName("quf_JewelTone"));
        clickButton("Submit");
        // Third row (Red)
        assertEquals(2, table.getRowIndex(TEST_DATA[TD_COLOR][2]));
        clickAndWait(table.updateLink(2));
        setFormElement(Locator.name("quf_" + _listColMonth.getName()), VALID_MONTHS[2]);  // Has a funny format -- need to post converted date
        uncheckCheckbox(Locator.checkboxByName("quf_JewelTone"));
        clickButton("Submit");

        table = new DataRegionTable("query", getDriver());
        assertEquals(TEST_DATA[TD_TONE][0], table.getDataAsText(table.getRowIndex(TEST_DATA[TD_COLOR][0]), _listColTone.getLabel()));
        assertEquals("true", table.getDataAsText(table.getRowIndex(TEST_DATA[TD_COLOR][1]), _listColTone.getLabel()));
        assertEquals("false", table.getDataAsText(table.getRowIndex(TEST_DATA[TD_COLOR][2]), _listColTone.getLabel()));

        log("Test edit and adding new field with imported data present");
        clickTab("List");
        _listHelper.goToList(LIST_NAME_COLORS);
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(LIST_NAME_COLORS);
        DomainFormPanel fieldsPanel = listDefinitionPage.getFieldsPanel();
        fieldsPanel.getField(_listColFake.getName())
            .setName(_listColDesc.getName())
            .setLabel(_listColDesc.getLabel());
        fieldsPanel.addField(_listColGood);

        // Create "Hidden Field" and remove from all views.
        fieldsPanel.addField(_listColHidden);
        fieldsPanel.getField(_listColHidden.getName())
            .showFieldOnDefaultView(false)
            .showFieldOnInsertView(false)
            .showFieldOnUpdateView(false)
            .showFieldOnDetailsView(false);

        fieldsPanel.addField(_listColAliased);
        fieldsPanel.getField(_listColAliased.getName())
            .setImportAliases(ALIASED_KEY_NAME);

        listDefinitionPage.clickSave();

        log("Check new field was added correctly");
        assertTextPresent(_listColGood.getName());

        log("Set title field of 'Colors' to 'Desc'");
        listDefinitionPage = _listHelper.goToEditDesign(LIST_NAME_COLORS);
        listDefinitionPage.openAdvancedListSettings().setFieldUsedForDisplayTitle("Desc").clickApply();
        listDefinitionPage.clickSave();

        assertTextPresent(
                TEST_DATA[TD_COLOR][0],
                TEST_DATA[TD_DESC][1],
                TEST_DATA[TD_MONTH][2]);

        assertTextNotPresent(HIDDEN_TEXT); // Hidden from Grid view.

        setUpListFinish();

        log("Check that data was added correctly");
        assertTextPresent(
                TEST_DATA[TD_COLOR][0],
                TEST_DATA[TD_DESC][1],
                TEST_DATA[TD_MONTH][2],
                TEST_DATA[TD_GOOD][0],
                TEST_DATA[TD_GOOD][1],
                TEST_DATA[TD_GOOD][2],
                TEST_DATA[TD_ALIAS][0],
                TEST_DATA[TD_ALIAS][1],
                TEST_DATA[TD_ALIAS][2]);

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
        assertTrue("Description \"" + _listColDesc.getDescription() + "\" not present.", html.contains(_listColDesc.getDescription()));
        assertTrue("Description \"" + _listColTone.getDescription() + "\" not present.", html.contains(_listColTone.getDescription()));
        setFormElement(Locator.name("quf_" + _listColDesc.getName()), TEST_DATA[TD_DESC][3]);
        // Jewel Tone checkbox is left blank -- we'll make sure it's posted as false below
        setFormElement(Locator.name("quf_" + _listColGood.getName()), TEST_DATA[TD_GOOD][3]);
        clickButton("Submit");
        assertTextPresent("This field is required");
        setFormElement(Locator.name("quf_" + LIST_KEY_NAME2), TEST_DATA[TD_COLOR][3]);
        clickButton("Submit");

        log("Check new row was added");
        assertTextPresent(
                TEST_DATA[TD_COLOR][3],
                TEST_DATA[TD_DESC][3],
                TEST_DATA[TD_TONE][3]);
        table = new DataRegionTable("query", getDriver());
        assertEquals(TEST_DATA[TD_TONE][2], table.getDataAsText(2, _listColTone.getLabel()));
        assertEquals(3, table.getRowIndex(TEST_DATA[TD_COLOR][3]));
        assertEquals(TEST_DATA[TD_TONE][3], table.getDataAsText(3, _listColTone.getLabel()));

        log("Check hidden field is hidden only where specified.");
        listDefinitionPage = _listHelper.goToEditDesign(LIST_NAME_COLORS);
        fieldsPanel = listDefinitionPage.getFieldsPanel();
        fieldsPanel.getField(_listColHidden.getName()) // Select Hidden field.
            .showFieldOnDefaultView(true);
        listDefinitionPage.clickSave();

        log("Check that hidden column is hidden.");
        assertTextPresent(HIDDEN_TEXT); // Not hidden from grid view.
        table = new DataRegionTable("query", getDriver());
        clickAndWait(table.detailsLink(0));
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from details view.
        assertTextBefore(_listColMonth.getLabel(), _listColTone.getLabel());
        clickButton("Edit");
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from update view.
        assertTextBefore(_listColMonth.getLabel(), _listColTone.getLabel());
        clickButton("Cancel");
        table.clickInsertNewRow();
        assertTextNotPresent(HIDDEN_TEXT); // Hidden from insert view.
        assertTextBefore(_listColMonth.getLabel(), _listColTone.getLabel());
        clickButton("Cancel");

        listDefinitionPage = _listHelper.goToEditDesign(LIST_NAME_COLORS);
        fieldsPanel = listDefinitionPage.getFieldsPanel();
        fieldsPanel.getField(_listColHidden.getName()) // Select Hidden field.
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
        fieldsPanel.getField(_listColHidden.getName()) // Select Hidden field.
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
        fieldsPanel.getField(_listColHidden.getName()) // Select Hidden field.
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
        region.setSort(_listColDesc.getName(), SortDirection.ASC);
        assertTextBefore(TEST_DATA[TD_COLOR][0], TEST_DATA[TD_COLOR][1]);

        clearSortTest();

        region.setFilter(_listColGood.getName(), "Is Greater Than", "7");
        assertTextNotPresent(TEST_DATA[TD_COLOR][3]);

        log("Test Customize View");
        // Re-navigate to the list to clear filters and sorts
        clickTab("List");
        clickAndWait(Locator.linkWithText(LIST_NAME_COLORS));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeColumn(_listColGood.getName());
        _customizeViewsHelper.addFilter(_listColGood.getName(), _listColGood.getLabel(), "Is Less Than", "10");
        _customizeViewsHelper.addSort(_listColMonth.getName(), _listColMonth.getLabel(), SortDirection.ASC);
        _customizeViewsHelper.saveCustomView(TEST_VIEW);

        log("Check Customize View worked");
        assertTextPresent(TEST_DATA[TD_COLOR][3]);
        assertTextPresentInThisOrder(TEST_DATA[TD_COLOR][1], TEST_DATA[TD_COLOR][2], TEST_DATA[TD_COLOR][3]);
        assertTextNotPresent(TEST_DATA[TD_COLOR][0], _listColGood.getLabel());

        log("4725: Check Customize View can't remove all fields");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeColumn(LIST_KEY_NAME2);
        _customizeViewsHelper.removeColumn(_listColDesc.getName());
        _customizeViewsHelper.removeColumn(_listColMonth.getName());
        _customizeViewsHelper.removeColumn(_listColTone.getName());
        _customizeViewsHelper.removeColumn(EscapeUtil.fieldKeyEncodePart(_listColAliased.getName()));
        _customizeViewsHelper.clickViewGrid();
        assertExt4MsgBox("You must select at least one field to display in the grid.", "OK");
        _customizeViewsHelper.closePanel();

        log("Test Export");

        File tableFile = new DataRegionExportHelper(new DataRegionTable("query", getDriver())).exportText();
        TextSearcher tsvSearcher = new TextSearcher(tableFile);

        assertTextPresentInThisOrder(tsvSearcher, TEST_DATA[TD_COLOR][1], TEST_DATA[TD_COLOR][2], TEST_DATA[TD_COLOR][3]);
        assertTextNotPresent(tsvSearcher, TEST_DATA[TD_COLOR][0], _listColGood.getLabel());
        filterTest();

        clickProject(getProjectName());

        log("Test that sort only affects one web part");
        DataRegionTable firstList = DataRegionTable.DataRegion(getDriver()).find();
        DataRegionTable secondList = DataRegionTable.DataRegion(getDriver()).index(1).find();
        firstList.setSort(_listColGood.getName(), SortDirection.ASC);
        List<String> expectedColumn = new ArrayList<>(Arrays.asList(TEST_DATA[TD_GOOD]));
        List<String> firstListColumn = secondList.getColumnDataAsText(_listColGood.getName());
        assertEquals("Second query webpart shouldn't have been sorted", expectedColumn, firstListColumn);
        expectedColumn.sort(Comparator.comparingInt(Integer::parseInt)); // Parse to check sorting of 10 vs 7, 8, 9
        List<String> secondListColumn = firstList.getColumnDataAsText(_listColGood.getName());
        assertEquals("First query webpart should have been sorted", expectedColumn, secondListColumn);

        log("Test list history");
        clickAndWait(Locator.linkWithText("manage lists"));
        clickAndWait(Locator.linkWithText("view history"));
        checker().wrapAssertion(()->assertTextPresent(":History"));
        checker().wrapAssertion(()->assertTextPresent("record was modified", 2));    // An existing list record was modified
        checker().wrapAssertion(()->assertTextPresent("were modified", 8));          // The column(s) of domain ></% 1äöüColors were modified
        checker().wrapAssertion(()->assertTextPresent("Bulk inserted", 2));
        checker().wrapAssertion(()->assertTextPresent("A new list record was inserted", 1));
        checker().wrapAssertion(()->assertTextPresent("was created", 2));                // Once for the list, once for the domain
        // List insert/update events should each have a link to the list item that was modified, but the other events won't have a link
        checker().wrapAssertion(()->assertEquals("details Links", 6, DataRegionTable.detailsLinkLocator().findElements(getDriver()).size()));
        checker().wrapAssertion(()->assertEquals("Project Links", 18, DataRegionTable.Locators.table().append(Locator.linkWithText(PROJECT_VERIFY)).findElements(getDriver()).size()));
        checker().wrapAssertion(()->assertEquals("List Links", 18, DataRegionTable.Locators.table().append(Locator.linkWithText(LIST_NAME_COLORS)).findElements(getDriver()).size()));
        DataRegionTable dataRegionTable = new DataRegionTable("query", getDriver());
        dataRegionTable.clickRowDetails(0);
        checker().wrapAssertion(()->assertTextPresent("List Item Details"));
        checker().wrapAssertion(()->assertTextNotPresent("No details available for this event.", "Unable to find the audit history detail for this event"));

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
        _customizeViewsHelper.addColumn(_list2Col1.getName() + "/" + _listColDesc.getName(), _list2Col1.getLabel() + " " + _listColDesc.getLabel());
        _customizeViewsHelper.addColumn(_list2Col1.getName() + "/" + _listColMonth.getName(), _list2Col1.getLabel() + " " + _listColMonth.getLabel());
        _customizeViewsHelper.addColumn(_list2Col1.getName() + "/" + _listColGood.getName(), _list2Col1.getLabel() + " " + _listColGood.getLabel());
        _customizeViewsHelper.addFilter(_list2Col1.getName() + "/" + _listColGood.getName(), _listColGood.getLabel(), "Is Less Than", "10");
        _customizeViewsHelper.addSort(_list2Col1.getName() + "/" + _listColGood.getName(), _listColGood.getLabel(), SortDirection.ASC);
        _customizeViewsHelper.addColumn(_list3Col1.getName() + "/" + _list3Col1.getName(), _list3Col1.getLabel() + " " + _list3Col1.getLabel());
        _customizeViewsHelper.addColumn(_list3Col1.getName() + "/" + _list3Col2.getName(), _list3Col1.getLabel() + " " + _list3Col2.getLabel());
        _customizeViewsHelper.saveCustomView(TEST_VIEW);

        log("Check adding referenced fields worked");
        waitForText(WAIT_FOR_JAVASCRIPT, _listColDesc.getLabel());
        assertTextPresent(
                _listColDesc.getLabel(),
                _listColMonth.getLabel(),
                _listColGood.getLabel(),
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
        assertTextPresent(srch, LIST_KEY_NAME2 + '/' + _listColDesc.getName(),
                LIST_KEY_NAME2 + '/' + _listColMonth.getName(),
                LIST_KEY_NAME2 + '/' + _listColGood.getName(),
                LIST2_FOREIGN_KEY_OUTSIDE,
                LIST3_COL2);
        assertTextNotPresent(srch, LIST2_KEY, LIST2_KEY4);
        assertTextPresentInThisOrder(srch, LIST2_KEY3, LIST2_KEY2);

        log("Test edit row");
        list.updateRow(LIST2_KEY3, Maps.of(
                "Color", TEST_DATA[TD_DESC][1],
                "Owner", LIST2_FOREIGN_KEY_OUTSIDE));

        final DataRegionTable dt = DataRegion(getDriver()).withName("query").find();
        dt.goToView("Default");
        assertTextPresent(TEST_DATA[TD_DESC][1], 2);

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
        _listHelper.deleteList();

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
        assertTextPresent("The specified query '%s' does not exist in schema '%s'".formatted(LIST_NAME_COLORS, "lists"));

        clickButton("Back");
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
            getConversionErrorMessage("green", "ShouldInsertCorrectly", Boolean.class),
            getConversionErrorMessage("five", "Id", Integer.class) + "; Missing value for required property: Id"
        };

        createList(multiErrorListName, BatchListColumns, BatchListData);
        _listHelper.beginAtList(PROJECT_VERIFY, multiErrorListName);
        _listHelper.clickImportData();

        // insert the new list data and verify the expected errors appear
        setListImportAsTestDataField(toTSV(BatchListColumns, BatchListExtraData), expectedErrors);

        // no need to query the list; nothing will be inserted if the batch insert fails/errors
    }

    @Test
    public void testListMerge()
    {
        String mergeListName = "listForMerging";
        createList(mergeListName, BatchListColumns, BatchListData);
        _listHelper.beginAtList(PROJECT_VERIFY, mergeListName);

        ImportDataPage importDataPage = _listHelper.clickImportData();
        checker().verifyTrue("For list with an integer, non-auto-increment key, merge option should be available for copy/paste", importDataPage.isPasteMergeOptionPresent());
        _listHelper.chooseFileUpload();
        checker().verifyTrue("For list with an integer, non-auto-increment key, merge option should be available for file upload", importDataPage.isFileMergeOptionPresent());
        _listHelper.chooseCopyPasteText();

        log("Try to upload the same data without choosing to merge.  Errors are expected.");
        String[] expectedErrors = new String[]{
                "duplicate key value"
        };
        setListImportAsTestDataField(toTSV(BatchListColumns, BatchListMergeData), expectedErrors);
        _listHelper.beginAtList(PROJECT_VERIFY, mergeListName);
        _listHelper.verifyListData(BatchListColumns, BatchListData, checker());

        log("Upload the same data using the merge operation. No errors should result.");
        importDataPage = _listHelper.clickImportData();
        importDataPage.setCopyPasteMerge(true);
        setListImportAsTestDataField(toTSV(BatchListColumns, BatchListData));
        _listHelper.verifyListData(BatchListColumns, BatchListData, checker());

        log("Now upload some new data and modify existing data");
        importDataPage = _listHelper.clickImportData();
        importDataPage.setCopyPasteMerge(true);
        setListImportAsTestDataField(toTSV(BatchListMergeColumns, BatchListMergeData));
        _listHelper.verifyListData(BatchListColumns, BatchListAfterMergeData, checker());
    }

    @Test
    public void testAutoIncrementKeyListNoMerge()
    {
        String mergeListName = "autoIncrementIdList";

        _listHelper.createList(PROJECT_VERIFY, mergeListName, ListColumnType.AutoInteger, "Key", col("Name", ColumnType.String));

        ImportDataPage importDataPage = _listHelper.clickImportData();
        checker().verifyFalse("For list with an integer, auto-increment key, merge option should not be available", importDataPage.isPasteMergeOptionPresent());
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
        DomainResponse createResponse = dgen.createList(createDefaultConnection(), "key");
        Domain listDomain = createResponse.getDomain();
        List<PropertyDescriptor> listFields = createResponse.getDomain().getFields();
        listFields.add(new FieldDefinition("volume", FieldDefinition.ColumnType.Decimal));
        listDomain.setFields(listFields);

        // now save with an extra field
        SaveDomainCommand saveCmd = new SaveDomainCommand(info.getSchema(), info.getTable());
        saveCmd.setDomainDesign(listDomain);
        DomainResponse saveResponse = saveCmd.execute(createDefaultConnection(), info.getFolder());

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
        DomainResponse createResponse = dgen.createList(createDefaultConnection(), "key");
        Domain listDomain = createResponse.getDomain();
        List<PropertyDescriptor> listFields = createResponse.getDomain().getFields();
        listFields.removeIf(a-> a.getName().equals("removeMe"));
        listDomain.setFields(listFields);

        SaveDomainCommand saveCmd = new SaveDomainCommand(info.getSchema(), info.getTable());
        saveCmd.setDomainDesign(listDomain);
        DomainResponse saveResponse = saveCmd.execute(createDefaultConnection(), info.getFolder());

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
        DomainResponse createResponse = dgen.createList(createDefaultConnection(), "key");
        Domain listDomain = createResponse.getDomain();
        listDomain.setName("remoteAPIAfterRename");

        SaveDomainCommand saveCmd = new SaveDomainCommand(listDomain.getDomainId());
        saveCmd.setDomainDesign(listDomain);
        DomainResponse saveResponse = saveCmd.execute(createDefaultConnection(), info.getFolder());

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

        FieldDefinition[] columns = new FieldDefinition[] {
                new FieldDefinition(dummyCol, ColumnType.String)
        };
        FieldDefinition lookupCol = new FieldDefinition(lookupField,
                new FieldDefinition.LookupInfo(null, lookupSchema, lookupTable).setTableType(FieldDefinition.ColumnType.Integer));
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
        _listHelper.createList(PROJECT_OTHER, crossContainerLookupList, ListColumnType.AutoInteger, "Key",  col(PROJECT_VERIFY, lookupColumn, ColumnType.Integer, "A" ));
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
        region.openFilterDialog(_listColGood.getName());
        _extHelper.clickExtTab("Choose Filters");
        click(Locator.xpath("//div[" + Locator.NOT_HIDDEN + " and ./label/span[text()='Filter Type:']]/div/div//img[contains(@class, 'x-form-arrow-trigger')]"));

        assertElementNotPresent(Locator.xpath("//div[" + Locator.NOT_HIDDEN + " and contains(@class, 'x-combo-list-item') and text()='Starts With']"));
        assertElementPresent(Locator.xpath("//div[" + Locator.NOT_HIDDEN + " and contains(@class, 'x-combo-list-item') and text()='Is Blank']"));
        click(Locator.xpath("//div[" + Locator.NOT_HIDDEN + " and ./label/span[text()='Filter Type:']]/div/div//img[contains(@class, 'x-form-arrow-trigger')]"));
        _extHelper.clickExtButton("Show Rows Where " + _listColGood.getLabel(), "Cancel", 0);

        log("Test that filters don't affect multiple web parts");
        assertTextPresent(TEST_DATA[TD_DESC][0], 2);
        region.setFilter(_listColGood.getName(), "Is Less Than", "10");
        assertTextPresent(TEST_DATA[TD_DESC][0], 1);

        clickAndWait(Locator.linkContainingText(LIST_NAME_COLORS));
    }

    /*  Issue 11825: Create test for "Clear Sort"
        Issue 15567: Can't sort DataRegion by column name that has comma

        sort by a parameter, then clear sort.
        Verify that reverts to original sort and the dropdown menu disappears

        preconditions:  table already sorted by description
     */
    @LogMethod
    private void clearSortTest()
    {
        //make sure elements are ordered the way they should be
        assertTextPresentInThisOrder(TEST_DATA[TD_ALIAS][0], TEST_DATA[TD_ALIAS][1],TEST_DATA[TD_ALIAS][2]);

        String encodedName = EscapeUtil.fieldKeyEncodePart(_listColAliased.getName());

        DataRegionTable query = new DataRegionTable("query", getDriver());

        //sort  by element and verify it worked
        query.setSort(encodedName, SortDirection.DESC);
        assertTextPresentInThisOrder(TEST_DATA[TD_ALIAS][0], TEST_DATA[TD_ALIAS][2], TEST_DATA[TD_ALIAS][1]);

        //remove sort and verify we return to initial state
        query.clearSort(encodedName);
        assertTextPresentInThisOrder(TEST_DATA[TD_ALIAS][0], TEST_DATA[TD_ALIAS][1],TEST_DATA[TD_ALIAS][2]);
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

    @Test
    public void testIgnoreReservedFieldNames() throws Exception
    {
        final String expectedInfoMsg = BaseDomainDesigner.RESERVED_FIELDS_WARNING_PREFIX +
                "These fields are already used by LabKey: " +
                "Created, createdBy, Modified, modifiedBy, container, created, createdby, modified, modifiedBy, Container.";

        List<String> lines = new ArrayList<>();
        lines.add("Name,TextField1,DecField1,DateField1,Created,createdBy,Modified,modifiedBy,container,created,createdby,modified,modifiedBy,Container,SampleID");

        File inferenceFile = TestFileUtils.writeTempFile("InferFieldsForList.csv", String.join(System.lineSeparator(), lines));

        goToProjectHome();

        String name = "Ignore Reserved Fields List";

        log("Infer fields from a file that contains some reserved fields.");
        EditListDefinitionPage listEditPage = _listHelper.beginCreateList(PROJECT_VERIFY, name);
        DomainFormPanel domainForm = listEditPage.getFieldsPanel()
                .setInferFieldFile(inferenceFile);

        checker().verifyEquals("Reserved field warning not as expected",  expectedInfoMsg, domainForm.getPanelAlertText(1));
        listEditPage.selectAutoIntegerKeyField();
        listEditPage.clickSave();
        goToProjectHome();
        checker().verifyTrue("Link to new list not present", Locator.linkWithText(name).existsIn(getDriver()));

        log("End of test.");
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
                new FieldDefinition("FileName", ColumnType.String).setLabel("FileName").setDescription("name of the file"),
                new FieldDefinition("FileExtension", ColumnType.String).setLabel("ext").setDescription("the file extension"),
                new FieldDefinition(notPhiColumn, ColumnType.Attachment).setLabel("NotPhiFile").setDescription("the file itself"),
                new FieldDefinition(limitedPhiColumn, ColumnType.Attachment).setLabel("LimitedPhiFile").setDescription("the file itself"),
                new FieldDefinition(phiColumn, ColumnType.Attachment).setLabel("PhiFile").setDescription("the file itself"),
                new FieldDefinition(restrictedPhiColumn, ColumnType.Attachment).setLabel("RestrictedFile").setDescription("the file itself"));


        // set phi levels
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(listName);
        listDefinitionPage.setColumnPhiLevel("NotPhiColumn", FieldDefinition.PhiSelectType.NotPHI);
        listDefinitionPage.setColumnPhiLevel("LimitedPhiColumn", FieldDefinition.PhiSelectType.Limited);
        listDefinitionPage.setColumnPhiLevel("PhiColumn", FieldDefinition.PhiSelectType.PHI);
        listDefinitionPage.setColumnPhiLevel("RestrictedPhiColumn", FieldDefinition.PhiSelectType.Restricted);
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
                col(descriptionCol, ColumnType.String),
                col(attachmentCol, ColumnType.Attachment));
        // index for entire list as single document and index on attachment column
        _listHelper.goToEditDesign(listName)
                .openAdvancedListSettings()
                .indexEntireListAsASingleDocument(true, "",
                        AdvancedListSettingsDialog.SearchIncludeOptions.MetadataAndData,
                        AdvancedListSettingsDialog.SearchIndexOptions.NonPhiText, null)
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
    public void testAttachmentFieldWithSpace()
    {
        final String listName = "Attachment Field with Space List";
        final String attachmentFileName = "searchData.tsv";
        final String path = TestFileUtils.getSampleData("lists/" + attachmentFileName).getAbsolutePath();
        final String attachmentCol = "Attachment Field With Space";

        Map<String, String> row = new HashMap<>();
        row.put(attachmentCol, path);

        goToProjectHome();

        log("create list with an attachment column '" + attachmentCol + "'");
        _listHelper.createList(getProjectName(), listName, ListColumnType.AutoInteger, "id",
                col(attachmentCol, ColumnType.Attachment));

        log("Insert data, upload attachment for col '" + attachmentCol + "'");
        goToProjectHome();
        clickAndWait(Locator.linkWithText(listName));
        _listHelper.insertNewRow(row, false);
        assertTextPresent(attachmentFileName);
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
                               col(descriptionCol, ColumnType.String),
                               col(attachmentCol, ColumnType.Attachment));
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

    @Test
    public void testFieldUniqueConstraint()
    {
        String listName = "Unique Constraint List";
        String fieldName1 = "field Name1";
        String fieldName2 = "fieldName_2";
        String fieldName3 = "FieldName@3";
        _listHelper.createList(PROJECT_VERIFY, listName, ListColumnType.AutoInteger, "key",
                new FieldDefinition(fieldName1, ColumnType.Integer),
                new FieldDefinition(fieldName2, ColumnType.DateAndTime),
                new FieldDefinition(fieldName3, ColumnType.Boolean)
        );

        // verify initial set of indices
        viewRawTableMetadata(listName);
        verifyTableIndices("unique_constraint_list_", Collections.emptyList());

        // set two fields to have unique constraints
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(listName);
        listDefinitionPage.getFieldsPanel()
                .getField(fieldName1).expand().clickAdvancedSettings().setUniqueConstraint(true)
                .apply();
        listDefinitionPage.getFieldsPanel()
                .getField(fieldName2).expand().clickAdvancedSettings().setUniqueConstraint(true)
                .apply();
        listDefinitionPage.clickSave();
        viewRawTableMetadata(listName);
        verifyTableIndices("unique_constraint_list_", List.of("field_name1", "fieldname_2"));
        assertTextNotPresent("unique_constraint_list_fieldname_3");

        // remove a field unique constraint and add a new one
        listDefinitionPage = _listHelper.goToEditDesign(listName);
        listDefinitionPage.getFieldsPanel()
                .getField(fieldName2).expand().clickAdvancedSettings().setUniqueConstraint(false)
                .apply();
        listDefinitionPage.getFieldsPanel()
                .getField(fieldName3).expand().clickAdvancedSettings().setUniqueConstraint(true)
                .apply();
        listDefinitionPage.clickSave();
        viewRawTableMetadata(listName);
        verifyTableIndices("unique_constraint_list_", List.of("field_name1", "fieldname_3"));
        assertTextNotPresent("unique_constraint_list_fieldname_2");
    }

    private void viewRawTableMetadata(String listName)
    {
        goToSchemaBrowser();
        selectQuery("lists", listName);
        waitAndClickAndWait(Locator.linkWithText("view raw table metadata"));
    }

    private void verifyTableIndices(String prefix, List<String> indexSuffixes)
    {
        List<String> suffixes  = new ArrayList<>();
        suffixes.add("pk");
        suffixes.addAll(indexSuffixes);

        for (String suffix : suffixes)
            assertTextPresentCaseInsensitive(prefix + suffix);
    }

    //
    // CUSTOMIZE URL tests
    //

    FieldDefinition col(String name, ColumnType type)
    {
        return new FieldDefinition(name, type);
    }

    FieldDefinition col(String name, ColumnType type, String table)
    {
        return col(null, name, type, table);
    }

    FieldDefinition col(String folder, String name, ColumnType type, String table)
    {
        return new FieldDefinition(name, new FieldDefinition.LookupInfo(folder, "lists", table).setTableType(type));
    }

    FieldDefinition colURL(String name, ColumnType type, String url)
    {
        return new FieldDefinition(name, type).setURL(url);
    }

    List<FieldDefinition> Acolumns = Arrays.asList(
            col("A", ColumnType.Integer),
            colURL("title", ColumnType.String, "/junit/echoForm.view?key=${A}&title=${title}&table=A"),
            col("Bfk", ColumnType.Integer, "B")
    );
    String[][] Adata = new String[][]
    {
        {"1", "one A", "1"},
    };

    List<FieldDefinition> Bcolumns = Arrays.asList(
            col("B", ColumnType.Integer),
            colURL("title", ColumnType.String, "org.labkey.core.junit.JunitController$EchoFormAction.class?key=${B}&title=${title}&table=B"),
            col("Cfk", ColumnType.Integer, "C")
    );
    String[][] Bdata = new String[][]
    {
        {"1", "one B", "1"},
    };

    List<FieldDefinition> Ccolumns = Arrays.asList(
            col("C", ColumnType.Integer),
            colURL("title", ColumnType.String, "/junit/echoForm.view?key=${C}&title=${title}&table=C")
    );
    String[][] Cdata = new String[][]
    {
            {"1", "one C"},
    };

    List<FieldDefinition> BatchListColumns = Arrays.asList(
            col("Id", ColumnType.Integer),
            col("FirstName", ColumnType.String),
            col("LastName", ColumnType.String),
            col("IceCreamFlavor", ColumnType.String),
            col("ShouldInsertCorrectly", ColumnType.Boolean)
    );

    List<FieldDefinition> BatchListMergeColumns = Arrays.asList(
            BatchListColumns.get(0),
            BatchListColumns.get(1),
            BatchListColumns.get(3)
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
    String[][] BatchListMergeData = new String[][]
            {
                    {"2", "Jane", ""},
                    {"3", "Jeffrey", "Strawberry"},
                    {"8", "Jamie", "Salted Caramel"},
            };
    String[][] BatchListAfterMergeData = new String[][]
            {
                    {"1", "Joe", "Test", "Vanilla", "true"},
                    {"2", "Jane", "Test", " ", "true"},
                    {"3", "Jeffrey", "BugCatcher", "Strawberry", "true"},
                    {"8", "Jamie", " ", "Salted Caramel", " "},
            };

    String toTSV(List<FieldDefinition> cols, String[][] data)
    {
        StringBuilder sb = new StringBuilder();
        String tab = "";
        for (FieldDefinition c : cols)
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

    void createList(String name, List<FieldDefinition> cols, String[][] data)
    {
        log("Add List -- " + name);
        _listHelper.createList(PROJECT_VERIFY, name, ListHelper.ListColumnType.fromNew(cols.get(0).getType()), cols.get(0).getName(),
                cols.subList(1, cols.size()).toArray(new FieldDefinition[cols.size() - 1]));
        _listHelper.goToList(name);
        _listHelper.clickImportData();
        setListImportAsTestDataField(toTSV(cols,data));
    }

    private void setListImportAsTestDataField(String data, String... expectedErrors)
    {
        ImportDataPage importDataPage = new ImportDataPage(getDriver());
        importDataPage.setText(data);
        if (expectedErrors.length == 0)
        {
            importDataPage.submit();
        }
        else
        {
            String errors = importDataPage.submitExpectingError();
            for (String expectedError : expectedErrors)
            {
                MatcherAssert.assertThat("Import errors", errors, CoreMatchers.containsString(expectedError));
            }
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
            assertTrue(getCurrentRelativeURL(false).contains(WebTestHelper.buildRelativeUrl("junit", PROJECT_VERIFY, "echoForm")));
        }
        popLocation();
    }

    private static SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static SimpleDateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static SimpleDateFormat DEFAULT_DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private void prepForDatAndTimeOnlyTest(String listName) throws IOException, CommandException
    {
        log("Reset site setting to have default formats for date and time values.");
        LookAndFeelSettingsPage settingsPage = LookAndFeelSettingsPage.beginAt(this);
        settingsPage.reset();

        // Use java's Date object and SimpleDateFormatter (not strings) to enter the data.
        DEFAULT_DATE_FORMAT = new SimpleDateFormat(settingsPage.getDefaultDateDisplay());
        DEFAULT_TIME_FORMAT = new SimpleDateFormat(settingsPage.getDefaultTimeDisplay());
        DEFAULT_DATE_TIME_FORMAT = new SimpleDateFormat(settingsPage.getDefaultDateTimeDisplay());

        deleteListIfPresent(getProjectName(), listName);

    }

    private void deleteListIfPresent(String projectName, String listName) throws IOException, CommandException
    {

        Connection cn = createDefaultConnection();
        SelectRowsCommand cmd = new SelectRowsCommand("ListManager", "ListManager");
        cmd.setColumns(Arrays.asList("Name", "Container"));
        cmd.addFilter("Container/DisplayName", projectName, Filter.Operator.getOperator("EQUAL"));
        cmd.addFilter("Name", listName, Filter.Operator.getOperator("EQUAL"));
        SelectRowsResponse response = cmd.execute(cn, projectName);

        if(response.getRowCount().intValue() != 0)
        {
            _listHelper.beginAtList(projectName, listName);
            _listHelper.deleteList();
        }

    }

    private void validateListDataInUI(DataRegionTable table, List<Map<String, String>> expectedData, String desc)
    {
        checker().verifyEquals("Number of rows in the UI list not equal to number of expected rows.",
                expectedData.size(), table.getDataRowCount());

        int rowIndex = 0;
        for(Map<String, String> expectedRow : expectedData)
        {
            checker().verifyEquals(String.format("For %s list row %d not as expected.", desc, rowIndex),
                    expectedRow, table.getRowDataAsMap(rowIndex));
            rowIndex++;
        }
    }

    /**
     * <p>
     *     Primarily testing the import of values into a date-only and time-only field in a list.
     * </p>
     * <p>
     *     Test covers:
     *     <ul>
     *         <li>Import date-only, time-only and dateTime values by bulk.</li>
     *         <li>Import date-only, time-only and dateTime values by xlsx. The xlsx file has columns formatted for date and time.</li>
     *         <li>Add a row through the UI.</li>
     *         <li>Import a time value into a date-only field and a date value into a time-only field.</li>
     *         <li>The test also uses different formats and validates the list displays in default site formats.</li>
     *     </ul>
     * </p>
     */
    @Test
    public void testDateAndTimeColumnsInsert() throws IOException, CommandException
    {

        SimpleDateFormat variantDateFormat = new SimpleDateFormat("MMMM dd, yyyy");
        SimpleDateFormat variantTimeFormat = new SimpleDateFormat("hh:mm:ss aa");
        SimpleDateFormat variantDateTimeFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm aa");

        String listName = "Date and Time Insert List";
        String dateCol = "Date";
        String timeCol = "Time";
        String dateTimeCol = "DateTime";

        prepForDatAndTimeOnlyTest(listName);

        log(String.format("Create a list named '%s' with date-only, time-only and dateTime fields.", listName));

        _listHelper.createList(PROJECT_VERIFY, listName, ListColumnType.AutoInteger, "key",
                new FieldDefinition(dateCol, ColumnType.Date),
                new FieldDefinition(timeCol, ColumnType.Time),
                new FieldDefinition(dateTimeCol, ColumnType.DateAndTime).setLabel(dateTimeCol)
        );

        log("Validate adding entries in bulk. Use a different format for the date and time values in the second row.");

        List<Map<String, String>> expectedData = new ArrayList<>();

        Date testDate01 = new Calendar.Builder()
                .setDate(2023, 1, 17)
                .setTimeOfDay(11, 12, 03)
                .build().getTime();

        expectedData.add(Map.of(dateCol, DEFAULT_DATE_FORMAT.format(testDate01),
                timeCol, DEFAULT_TIME_FORMAT.format(testDate01),
                dateTimeCol, DEFAULT_DATE_TIME_FORMAT.format(testDate01)));

        // Use a separate variable for this date. It will be formatted differently from the default when entered.
        Date testDate02 = new Calendar.Builder()
                .setDate(2022, 6, 10)
                .setTimeOfDay(20, 45, 15)
                .build().getTime();

        expectedData.add(Map.of(dateCol, DEFAULT_DATE_FORMAT.format(testDate02),
                timeCol, DEFAULT_TIME_FORMAT.format(testDate02),
                dateTimeCol, DEFAULT_DATE_TIME_FORMAT.format(testDate02)));

        testDate01 = new Calendar.Builder()
                .setDate(1994, 11, 21)
                .setTimeOfDay(3, 23, 00)
                .build().getTime();

        expectedData.add(Map.of(dateCol, DEFAULT_DATE_FORMAT.format(testDate01),
                timeCol, DEFAULT_TIME_FORMAT.format(testDate01),
                dateTimeCol, DEFAULT_DATE_TIME_FORMAT.format(testDate01)));

        String bulkImportText = String.format("%s\t%s\t%s\n", dateCol, timeCol, dateTimeCol) +
                String.format("%s\t%s\t%s\n",
                        expectedData.get(0).get(dateCol), expectedData.get(0).get(timeCol), expectedData.get(0).get(dateTimeCol)) +
                String.format("%s\t%s\t%s\n",
                        variantDateFormat.format(testDate02), variantTimeFormat.format(testDate02), DEFAULT_DATE_TIME_FORMAT.format(testDate02)) +
                String.format("%s\t%s\t%s\n",
                        expectedData.get(2).get(dateCol), expectedData.get(2).get(timeCol), expectedData.get(2).get(dateTimeCol));

        _listHelper.bulkImportData(bulkImportText);

        log("Validate adding entries by xlsx import. Note the xlsx file has columns formatted as time and date specific.");
        File excelDateTimeFile = TestFileUtils.getSampleData("lists/Date_And_Time_Format.xlsx");

        _listHelper.importDataFromFile(excelDateTimeFile);

        // Note, the month is zero based. The value of the month here is one less than what is seen in the file.
        // Also of note the xlsx used has a column formatted as time and another as date. Excel does not have a DateTime specific format.
        testDate01 = new Calendar.Builder()
                .setDate(2024, 1, 29)
                .setTimeOfDay(11, 28, 54)
                .build().getTime();

        expectedData.add(Map.of(dateCol, DEFAULT_DATE_FORMAT.format(testDate01),
                timeCol, DEFAULT_TIME_FORMAT.format(testDate01),
                dateTimeCol, DEFAULT_DATE_TIME_FORMAT.format(testDate01)));

        testDate01 = new Calendar.Builder()
                .setDate(1998, 2, 10)
                .setTimeOfDay(18, 12, 00)
                .build().getTime();

        expectedData.add(Map.of(dateCol, DEFAULT_DATE_FORMAT.format(testDate01),
                timeCol, DEFAULT_TIME_FORMAT.format(testDate01),
                dateTimeCol, DEFAULT_DATE_TIME_FORMAT.format(testDate01)));

        testDate01 = new Calendar.Builder()
                .setDate(2002, 9, 31)
                .setTimeOfDay(22, 20, 00)
                .build().getTime();

        expectedData.add(Map.of(dateCol, DEFAULT_DATE_FORMAT.format(testDate01),
                timeCol, DEFAULT_TIME_FORMAT.format(testDate01),
                dateTimeCol, DEFAULT_DATE_TIME_FORMAT.format(testDate01)));

        log("Validate adding an entry through the UI. Use a different format for the date and dateTime field.");

        testDate01 = new Calendar.Builder()
                .setDate(2024, 2, 14)
                .setTimeOfDay(12, 00, 45)
                .build().getTime();

        expectedData.add(Map.of(dateCol, DEFAULT_DATE_FORMAT.format(testDate01),
                timeCol, DEFAULT_TIME_FORMAT.format(testDate01),
                dateTimeCol, DEFAULT_DATE_TIME_FORMAT.format(testDate01)));

        Map<String, String> uiDateFormat = Map.of(
                dateCol, variantDateFormat.format(testDate01),
                timeCol, DEFAULT_TIME_FORMAT.format(testDate01),
                dateTimeCol, variantDateTimeFormat.format(testDate01));

        _listHelper.insertNewRow(uiDateFormat, false);

        log("Bulk import a time for the date field and a date for the time field.");

        testDate01 = new Calendar.Builder()
                .setDate(1992, 8, 4)
                .setTimeOfDay(19, 30, 32)
                .build().getTime();

        // If a time is given for a date-only field the date will default to 1-1-1970
        // If a date is given for a time-only field the time will default to 9:00:00
        Date dateMissing = new Calendar.Builder()
                .setDate(1970, 0, 1)
                .setTimeOfDay(9, 00, 00)
                .build().getTime();

        expectedData.add(Map.of(dateCol, DEFAULT_DATE_FORMAT.format(dateMissing),
                timeCol, DEFAULT_TIME_FORMAT.format(dateMissing),
                dateTimeCol, DEFAULT_DATE_TIME_FORMAT.format(testDate01)));

        // Making sure the time format and date formats are in the wrong column.
        bulkImportText = String.format("%s\t%s\t%s\n", dateCol, timeCol, dateTimeCol) +
                String.format("%s\t%s\t%s\n",
                        DEFAULT_TIME_FORMAT.format(testDate01), DEFAULT_DATE_FORMAT.format(testDate01), DEFAULT_DATE_TIME_FORMAT.format(testDate01));

        _listHelper.bulkImportData(bulkImportText);

        DataRegionTable table = new DataRegionTable("query", getDriver());

        validateListDataInUI(table, expectedData, "InsertTest");

    }

    private List<Map<String, String>> buildExpectedSorted(List<Date> dates, Date useDateOnly, Date useTimeOnly,
                                                          String dateCol, String timeCol)
    {
        List<Map<String, String>> expectedData = new ArrayList<>();

        for(Date date : dates)
        {
            Map<String, String> expectedRow = new HashMap<>();

            if(date.equals(useDateOnly))
            {
                expectedRow.put(dateCol, DEFAULT_DATE_FORMAT.format(date));
                expectedRow.put(timeCol, " ");
            }
            else if(date.equals(useTimeOnly))
            {
                expectedRow.put(dateCol, " ");
                expectedRow.put(timeCol, DEFAULT_TIME_FORMAT.format(date));
            }
            else
            {
                expectedRow.put(dateCol, DEFAULT_DATE_FORMAT.format(date));
                expectedRow.put(timeCol, DEFAULT_TIME_FORMAT.format(date));
            }

            expectedData.add(expectedRow);
        }

        return expectedData;
    }

    @Test
    public void testDateAndTimeColumnsFilterAndSort() throws IOException, CommandException
    {

        String listName = "Date and Time Sort and Filter List";
        String dateCol = "Date";
        String timeCol = "Time";

        prepForDatAndTimeOnlyTest(listName);

        log(String.format("Create a list named '%s' with date-only and time-only fields.", listName));

        _listHelper.createList(PROJECT_VERIFY, listName, ListColumnType.AutoInteger, "key",
                new FieldDefinition(dateCol, ColumnType.Date),
                new FieldDefinition(timeCol, ColumnType.Time)
        );

        List<Date> dates = new ArrayList<>();

        Date date01 = new Calendar.Builder()
                .setDate(1950, 9, 12)
                .setTimeOfDay(8, 00, 01)
                .build().getTime();

        dates.add(date01);

        // Add a date in the future.
        Calendar calToday = Calendar.getInstance();
        calToday.setTime(TestDateUtils.getTodaysDate());
        calToday.add(Calendar.MONTH, 2);
        calToday.set(Calendar.HOUR, 14);
        calToday.set(Calendar.MINUTE, 23);
        calToday.set(Calendar.SECOND, 54);
        Date dateFuture = calToday.getTime();

        dates.add(dateFuture);

        Date date03 = new Calendar.Builder()
                .setDate(2024, 0, 1)
                .setTimeOfDay(0, 00, 00)
                .build().getTime();

        dates.add(date03);

        Date dateDuplicate = new Calendar.Builder()
                .setDate(1992, 2, 3)
                .setTimeOfDay(10, 10, 10)
                .build().getTime();

        dates.add(dateDuplicate);

        // Have two entries with the same values.
        dates.add(dateDuplicate);

        Date date05 = new Calendar.Builder()
                .setDate(1992, 2, 3)
                .setTimeOfDay(10, 11, 34)
                .build().getTime();

        dates.add(date05);

        Date date06 = new Calendar.Builder()
                .setDate(1995, 2, 3)
                .setTimeOfDay(9, 10, 10)
                .build().getTime();

        dates.add(date06);

        // Add leap day to the mix
        Date date07 = new Calendar.Builder()
                .setDate(2024, 1, 29)
                .setTimeOfDay(18, 32, 00)
                .build().getTime();

        dates.add(date07);

        Date date08 = new Calendar.Builder()
                .setDate(2002, 8, 15)
                .setTimeOfDay(17, 45, 20)
                .build().getTime();

        dates.add(date08);

        StringBuilder bulkInsertText = new StringBuilder();
        bulkInsertText.append(String.format("%s\t%s\n", dateCol, timeCol));

        for(Date date : dates)
        {
            bulkInsertText.append(String.format("%s\t%s\n",
                    DEFAULT_DATE_FORMAT.format(date), DEFAULT_TIME_FORMAT.format(date)));
        }

        Date dateUseDateOnly = new Calendar.Builder()
                .setDate(1989, 7, 12)
                .setTimeOfDay(0, 0, 0)
                .build().getTime();

        // Add a line with only a date.
        bulkInsertText.append(String.format("%s\t\n", DEFAULT_DATE_FORMAT.format(dateUseDateOnly)));

        Date dateUseTimeOnly = new Calendar.Builder()
                .setDate(1970, 0, 1)
                .setTimeOfDay(14, 59, 25)
                .build().getTime();

        // Add a line with only a time.
        bulkInsertText.append(String.format("\t%s\n", DEFAULT_TIME_FORMAT.format(dateUseTimeOnly)));

        log("Populate the list with values to sort.");
        _listHelper.bulkImportData(bulkInsertText.toString());

        // List sorted by date in ascending order.
        // Ascending order should be:
        // 1950-10-12
        // 1989-08-12
        // 1992-03-03 (10:10:10)
        // 1992-03-03 (10:10:10)
        // 1992-03-03 (10:11:34)
        // 1995-03-03
        // 2002-09-15
        // 2024-01-01
        // 2024-02-29
        // 2024-04-21
        // (empty)
        List<Date> dateSorted = new ArrayList<>();
        dateSorted.add(date01);
        dateSorted.add(dateUseDateOnly);
        dateSorted.add(dateDuplicate);
        dateSorted.add(dateDuplicate);
        dateSorted.add(date05);
        dateSorted.add(date06);
        dateSorted.add(date08);
        dateSorted.add(date03);
        dateSorted.add(date07);
        dateSorted.add(dateFuture);
        dateSorted.add(dateUseTimeOnly);

        // List sorted by time in ascending order.
        // Ascending order for time should be:
        // 00:00:00
        // 02:23:54
        // 08:00:01
        // 09:10:10
        // 10:10:10
        // 10:10:10
        // 10:11:34
        // 14:59:25
        // 17:45:20
        // 18:32:00
        // (empty)
        List<Date> timeSorted = new ArrayList<>();
        timeSorted.add(date03);
        timeSorted.add(dateFuture);
        timeSorted.add(date01);
        timeSorted.add(date06);
        timeSorted.add(dateDuplicate);
        timeSorted.add(dateDuplicate);
        timeSorted.add(date05);
        timeSorted.add(dateUseTimeOnly);
        timeSorted.add(date08);
        timeSorted.add(date07);
        timeSorted.add(dateUseDateOnly);

        DataRegionTable table = new DataRegionTable("query", getDriver());

        log("Sort the date-only field in ascending order.");
        List<Map<String, String>> expectedData = buildExpectedSorted(dateSorted, dateUseDateOnly, dateUseTimeOnly, dateCol, timeCol);
        table.setSort(dateCol, SortDirection.ASC);
        validateListDataInUI(table, expectedData, "date ASC");
        checker().screenShotIfNewError("Error_Sort_Date_ASC");

        log("Sort the date-only field in descending order.");
        Collections.reverse(dateSorted);
        expectedData = buildExpectedSorted(dateSorted, dateUseDateOnly, dateUseTimeOnly, dateCol, timeCol);
        table.setSort(dateCol, SortDirection.DESC);
        validateListDataInUI(table, expectedData, "date DESC");
        checker().screenShotIfNewError("Error_Sort_Date_DESC");

        log("Clear the sort from the date-only column.");
        table.clearSort(dateCol);

        log("Sort the time-only field in ascending order.");
        expectedData = buildExpectedSorted(timeSorted, dateUseDateOnly, dateUseTimeOnly, dateCol, timeCol);
        table.setSort(timeCol, SortDirection.ASC);
        validateListDataInUI(table, expectedData, "time ASC");
        checker().screenShotIfNewError("Error_Sort_Time_ASC");

        log("Sort the time-only field in descending order.");
        Collections.reverse(timeSorted);
        expectedData = buildExpectedSorted(timeSorted, dateUseDateOnly, dateUseTimeOnly, dateCol, timeCol);
        table.setSort(timeCol, SortDirection.DESC);
        validateListDataInUI(table, expectedData, "time DESC");
        checker().screenShotIfNewError("Error_Sort_Time_DESC");

        log("Clear the sort from the time-only column.");
        table.clearSort(timeCol);

    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
