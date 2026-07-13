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
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
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
import org.labkey.remoteapi.query.Sort;
import org.labkey.serverapi.reader.TabLoader;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.categories.Hosting;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.domain.AdvancedSettingsDialog;
import org.labkey.test.components.domain.BaseDomainDesigner;
import org.labkey.test.components.domain.ConditionalFormatDialog;
import org.labkey.test.components.domain.DomainFieldRow;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.list.AdvancedListSettingsDialog;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.labkey.test.pages.list.GridPage;
import org.labkey.test.pages.query.UpdateQueryRowPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.StringLookup;
import org.labkey.test.params.FieldInfo;
import org.labkey.test.params.FieldKey;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.VarListDefinition;
import org.labkey.test.tests.AuditLogTest;
import org.labkey.test.util.AbstractDataRegionExportOrSignHelper.ColumnHeaderType;
import org.labkey.test.util.AuditLogHelper;
import org.labkey.test.util.DataRegionExportHelper;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.DomainUtils;
import org.labkey.test.util.EscapeUtil;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.Maps;
import org.labkey.test.util.OptionalFeatureHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.TestDataGenerator;
import org.labkey.test.util.TextSearcher;
import org.labkey.test.util.data.TestDataUtils;
import org.labkey.test.util.search.SearchAdminAPIHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.params.FieldDefinition.ColumnType;
import static org.labkey.test.params.FieldDefinition.DOMAIN_TRICKY_CHARACTERS;
import static org.labkey.test.util.DataRegionTable.DataRegion;

@Category({Daily.class, Data.class, Hosting.class})
@BaseWebDriverTest.ClassTimeout(minutes = 14)
public class ListTest extends BaseWebDriverTest
{
    protected final static String PROJECT_VERIFY = "ListVerifyProject" ;//+ TRICKY_CHARACTERS_FOR_PROJECT_NAMES;
    private final static String PROJECT_OTHER = "OtherListVerifyProject";
    protected final static String LIST_NAME_COLORS = "A_Colors_" + DOMAIN_TRICKY_CHARACTERS;
    protected final static String LIST_NAME_HTML_KEY = "A_HtmlKey_" + DOMAIN_TRICKY_CHARACTERS;
    protected final static ColumnType LIST_KEY_TYPE = ColumnType.String;
    protected final static String LIST_KEY_NAME = "Key";
    boolean IS_POSTGRES = WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL;

    protected final static String LIST_KEY_NAME2 = "Color \"`~!@#$%^&*()_-+={}[]|\\:;<>,.?/";
    protected final static String LIST_KEY_NAME2_BULK = "\"Color \"\"`~!@#$%^&*()_-+={}[]|\\:;<>,.?/\"";

    protected final static String LIST_DESCRIPTION = "A list of colors and what they are like";
    protected final static String FAKE_COL_NAME = "FakeName";
    protected final static String ALIASED_KEY_NAME = "Material";
    protected final static String HIDDEN_TEXT = "CantSeeMe";

    protected final FieldDefinition _listColFake = new FieldDefinition(FAKE_COL_NAME, ColumnType.String).setDescription("What the color is like");
    protected final FieldDefinition _listColDesc = new FieldDefinition("Desc", ColumnType.String).setLabel("Description").setDescription("What the color is like");

    protected final FieldDefinition _listColMonth = new FieldDefinition("Month", ColumnType.TextChoice)
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
            LIST_KEY_NAME2_BULK + "\t" + FAKE_COL_NAME + "\t" + _listColTone.getName() + "\t" + _listColMonth.getName() + "\n" +
            LIST_ROW1 + "\n" +
            LIST_ROW2 + "\n" +
            LIST_ROW3;
    private final String LIST_DATA2 =
            LIST_KEY_NAME2_BULK + "\t" + FAKE_COL_NAME + "\t" + _listColTone.getName() + "\t" + _listColMonth.getName() + "\t" + _listColGood.getName() + "\t" + ALIASED_KEY_NAME + "\t" + _listColHidden.getName() + "\n" +
            LIST_ROW1 + "\t" + TEST_DATA[TD_GOOD][0] + "\t" + TEST_DATA[TD_ALIAS][0] + "\t" + HIDDEN_TEXT + "\n" +
            LIST_ROW2 + "\t" + TEST_DATA[TD_GOOD][1] + "\t" + TEST_DATA[TD_ALIAS][1] + "\t" + HIDDEN_TEXT + "\n" +
            LIST_ROW3 + "\t" + TEST_DATA[TD_GOOD][2] + "\t" + TEST_DATA[TD_ALIAS][2] + "\t" + HIDDEN_TEXT;
    private final static String TEST_FAIL = "testfail";
    private final static String TEST_FAIL2 = "testfail\n2\n";
    private final String TEST_FAIL3 = LIST_KEY_NAME2 + "\t" + FAKE_COL_NAME + "\t" + _listColMonth.getName() + "\n" +
            LIST_ROW1;
    private final static String TEST_VIEW = "list_view";
    private final static String LIST2_NAME_CARS = "Cars_" + DOMAIN_TRICKY_CHARACTERS;
    protected final static ColumnType LIST2_KEY_TYPE = ColumnType.String;
    protected final static String LIST2_KEY_NAME = "Car";

    protected final FieldDefinition _list2Col1 = new FieldDefinition(LIST_KEY_NAME2, new StringLookup(null, "lists", LIST_NAME_COLORS)).setDescription("The color of the car");
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
    private final static ColumnType LIST3_KEY_TYPE = ColumnType.String;
    private final static String LIST3_KEY_NAME = "Owner";
    private final FieldDefinition _list3Col2 = new FieldDefinition("Wealth", ColumnType.String);
    protected final FieldDefinition _list3Col1 = new FieldDefinition(LIST3_KEY_NAME, new StringLookup(PROJECT_OTHER, "lists", LIST3_NAME_OWNERS)).setDescription("Who owns the car");
    private final static String LIST3_COL2 = "Rich";
    private final String LIST2_DATA =
            LIST2_KEY_NAME + "\t" + LIST_KEY_NAME2_BULK  + "\t" + LIST3_KEY_NAME + "\n" +
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

    private final AuditLogHelper _auditLogHelper = new AuditLogHelper(this);

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

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(PROJECT_VERIFY, afterTest);
        _containerHelper.deleteProject(PROJECT_OTHER, afterTest);
    }

    @BeforeClass
    public static void setupProject()
    {
        ListTest init = getCurrentTest();
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

    /** Issue 53796: 25.3 -> 25.7: DataRegion.getChecked() incorrectly HTML encodes the results */
    @Test
    public void testKeyWithHtmlCharacters()
    {
        _listHelper.createList(getProjectName(), LIST_NAME_HTML_KEY, new FieldDefinition(LIST_KEY_NAME2, LIST_KEY_TYPE));
        ImportDataPage importDataPage = _listHelper.clickImportData();
        String value = "<>ThisIsTheKeyValueWithHtmlCharacters</>";
        importDataPage.setText(LIST_KEY_NAME2_BULK + "\n" + value);
        importDataPage.submit();
        assertTextPresent(value);
        final DataRegionTable dt = DataRegion(getDriver()).withName("query").find();
        dt.checkAllOnPage();
        @SuppressWarnings("unchecked") List<String> checked = (List<String>)executeScript("return LABKEY.DataRegions.query.getChecked()");
        assertEquals(Arrays.asList(value), checked);
        dt.deleteSelectedRows();
        assertTextNotPresent(value);
    }

    @LogMethod
    protected void setUpList()
    {
        // TODO: Break this up into explicit test cases and remove redundant test coverage.
        // But at least now it's only called from the one test case that relies on this list, testCustomViews().
        // Previously it was called from the @BeforeClass method, even though none of the other test cases use this list.

        log("Add list -- " + LIST_NAME_COLORS);
        _listHelper.createList(getProjectName(), LIST_NAME_COLORS, new FieldDefinition(LIST_KEY_NAME2, LIST_KEY_TYPE), _listColFake,
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
        table.clickEditRow(1);
        setFormElement(Locator.name(EscapeUtil.getFormFieldName(_listColMonth.getName())), VALID_MONTHS[1]);  // Has a funny format -- need to post converted date
        checkCheckbox(Locator.checkboxByName(EscapeUtil.getFormFieldName("JewelTone")));
        clickButton("Submit");
        // Third row (Red)
        assertEquals(2, table.getRowIndex(TEST_DATA[TD_COLOR][2]));
        table.clickEditRow(2);
        setFormElement(Locator.name(EscapeUtil.getFormFieldName(_listColMonth.getName())), VALID_MONTHS[2]);  // Has a funny format -- need to post converted date
        uncheckCheckbox(Locator.checkboxByName(EscapeUtil.getFormFieldName("JewelTone")));
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
            .setLabel(_listColDesc.getLabel())
            .setImportAliases(_listColFake.getName());
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
        setFormElement(Locator.name(EscapeUtil.getFormFieldName(_listColDesc.getName())), TEST_DATA[TD_DESC][3]);
        // Jewel Tone checkbox is left blank -- we'll make sure it's posted as false below
        setFormElement(Locator.name(EscapeUtil.getFormFieldName(_listColGood.getName())), TEST_DATA[TD_GOOD][3]);
        clickButton("Submit");
        assertTextPresent("This field is required");
        setFormElement(Locator.name(EscapeUtil.getFormFieldName(LIST_KEY_NAME2)), TEST_DATA[TD_COLOR][3]);
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
    public void testNameTrimming()
    {
        goToProjectHome();
        String trimmedName = "Trimmings";
        log("Add list with leading spaces");
        _listHelper.createList(getProjectName(), " Trimmings", new FieldDefinition(LIST_KEY_NAME2, LIST_KEY_TYPE), _listColFake);
        log("Assure we can go to the page with the trimmed name");
        GridPage grid = GridPage.beginAt(this, getProjectName(), trimmedName);
        grid.click(Locator.linkWithText("Design"));
        EditListDefinitionPage editList = new EditListDefinitionPage(this.getDriver());
        checker().withScreenshot().verifyEquals("Name not trimmed as expected", trimmedName, editList.getName());

        trimmedName = "Extra Trimmings";
        log("Add list with leading and trailing spaces");
        _listHelper.createList(getProjectName(), " Extra Trimmings   ", new FieldDefinition(LIST_KEY_NAME2, LIST_KEY_TYPE), _listColFake);
        log("Assure we can go to the page with the trimmed name");
        grid = GridPage.beginAt(this, getProjectName(), trimmedName);
        grid.click(Locator.linkWithText("Design"));
        editList = new EditListDefinitionPage(this.getDriver());
        checker().withScreenshot().verifyEquals("Name not trimmed as expected", trimmedName, editList.getName());
    }

    @Test // Issue 52339
    public void testLongName()
    {
        String listName = "A_+-:''.¡™£¢∞§¶•ªº–≠œ∑´®†¥¨ˆøπ“‘«æ…¬˚∆˙©√ƒ∂ßΩ≈ç√∫µ≤≥÷‹›ﬁﬂ‡°·‚—±⁄€‹›‡‰Æ«»¢∫√∑∏∂";
        var fieldWithDefault = FieldInfo.random("With Default", ColumnType.String);
        EditListDefinitionPage listEditPage = _listHelper.beginCreateList(getProjectName(), listName);
        listEditPage.manuallyDefineFieldsWithAutoIncrementingKey("Key");
        listEditPage.addField(fieldWithDefault.getFieldDefinition());
        listEditPage.clickSave();

        listEditPage = _listHelper.goToEditDesign(listName);
        var page = listEditPage.getFieldsPanel()
                .expand()
                .getField(fieldWithDefault.getName())
                .clickAdvancedSettings()
                .clickDefaultValuesLink();
        var input = Locator.tagContainingText("td", fieldWithDefault.getLabel()).followingSibling("td")
                .descendant("input").findElement(page.getDriver());
        setFormElement(input, "42");
        clickButton("Save Defaults");
        _listHelper.beginAtList(getProjectName(), listName);

        DataRegionTable list = new DataRegionTable("query", getDriver());
        UpdateQueryRowPage updatePage = list.clickInsertNewRow();
        checker().verifyEquals("Default value not as expected ", "42", updatePage.getTextInputValue(fieldWithDefault.getName()));
        updatePage.submit();
    }

    /* Issue 51572: Bug with creating a new list by uploading a csv file in "UTF-8 with BOM" format
     */
    @Test
    public void testCreateListWithBOMFile()
    {
        String listName = TestDataGenerator.randomDomainName("From BOM File", DomainUtils.DomainKind.IntList);
        File bomFile = TestFileUtils.getSampleData("lists/TestUTF8_BOM.csv");

        EditListDefinitionPage listEditPage = _listHelper.beginCreateList(getProjectName(), listName);
        listEditPage.getFieldsPanel()
                .setInferFieldFile(bomFile);

        String keyName = "KeyValue";
        listEditPage.selectKeyField(keyName);
        listEditPage.clickSave();

        List<FieldDefinition> fields = List.of(
                new FieldDefinition(keyName, ColumnType.Integer),
                new FieldDefinition("A", ColumnType.String),
                new FieldDefinition("B", ColumnType.String),
                new FieldDefinition("C", ColumnType.String)
        );

        String [][] data = { {"101","A2","B2","C2"},
                {"102","A3","B3","C3"},
                {"103","A4","B4","C4"},
                {"104","A5","B5","C5"},
                {"105","A6","B6","C6"},
                {"106","A7","B7","C7"},
                {"107","A8","B8","C8"},
                {"108","A9","B9","C9"},
                {"109","A10","B10","C10"},
                {"110","A11","B11","C11"} };

        _listHelper.verifyListData(fields, data, checker());

    }

    @Test
    public void testCustomViews()
    {
        goToProjectHome();
        setUpList();

        goToProjectHome();
        waitAndClickAndWait(Locator.linkWithText(LIST_NAME_COLORS));

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
        waitAndClickAndWait(Locator.linkWithText(LIST_NAME_COLORS));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeColumn(_listColGood.getName());
        _customizeViewsHelper.addFilter(_listColGood.getName(), "Is Less Than", "10");
        _customizeViewsHelper.addSort(_listColMonth.getName(), SortDirection.ASC);
        _customizeViewsHelper.saveCustomView(TEST_VIEW);

        log("Check Customize View worked");
        assertTextPresent(TEST_DATA[TD_COLOR][3]);

        // Sorting is different between MSSQL and postgres if one of the values is empty / blank.
        if (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.MicrosoftSQLServer)
        {
            assertTextPresentInThisOrder(TEST_DATA[TD_COLOR][3], TEST_DATA[TD_COLOR][1], TEST_DATA[TD_COLOR][2]);
        }
        else
        {
            assertTextPresentInThisOrder(TEST_DATA[TD_COLOR][1], TEST_DATA[TD_COLOR][2], TEST_DATA[TD_COLOR][3]);
        }

        assertTextNotPresent(TEST_DATA[TD_COLOR][0], _listColGood.getLabel());

        log("4725: Check Customize View can't remove all fields");
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeColumn(EscapeUtil.fieldKeyEncodePart(LIST_KEY_NAME2));
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

        if (WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.MicrosoftSQLServer)
        {
            assertTextPresentInThisOrder(tsvSearcher, TEST_DATA[TD_COLOR][3], TEST_DATA[TD_COLOR][1], TEST_DATA[TD_COLOR][2]);
        }
        else
        {
            assertTextPresentInThisOrder(tsvSearcher, TEST_DATA[TD_COLOR][1], TEST_DATA[TD_COLOR][2], TEST_DATA[TD_COLOR][3]);
        }

        assertTextNotPresent(tsvSearcher, TEST_DATA[TD_COLOR][0], _listColGood.getLabel());
        filterTest();

        clickProject(getProjectName());

        log("Test that sort only affects one web part");
        DataRegionTable firstList = DataRegion(getDriver()).find();
        DataRegionTable secondList = DataRegion(getDriver()).index(1).find();
        firstList.setSort(_listColGood.getName(), SortDirection.ASC);
        List<String> expectedColumn = new ArrayList<>(Arrays.asList(TEST_DATA[TD_GOOD]));
        List<String> firstListColumn = secondList.getColumnDataAsText(_listColGood.getName());
        assertEquals("Second query webpart shouldn't have been sorted", expectedColumn, firstListColumn);
        expectedColumn.sort(Comparator.comparingInt(Integer::parseInt)); // Parse to check sorting of 10 vs 7, 8, 9
        List<String> secondListColumn = firstList.getColumnDataAsText(_listColGood.getName());
        assertEquals("First query webpart should have been sorted", expectedColumn, secondListColumn);

        log("Test list history");
        clickAndWait(Locator.linkWithText("manage lists"));
        DataRegionTable drt = new DataRegionTable.DataRegionFinder(getDriver()).find();
        drt.setFilter("Name", "Equals", LIST_NAME_COLORS);
        waitFor(()->drt.getDataRowCount()==1,
                String.format("DataRegion table did not filter to list %s", LIST_NAME_COLORS), 2_500);
        waitAndClickAndWait(Locator.linkWithText("view history"));

        // Wait for the header to load on the page.
        waitForElementToBeVisible(Locator.tagContainingText("h3", ":History"));

        checker().verifyTrue("DataRegions didn't load.",
                waitFor(()->new DataRegionTable.DataRegionFinder(getDriver()).findAll().size() == 2, 3_000));

        checker().wrapAssertion(()->assertTextPresent("record was modified", 2));    // An existing list record was modified
        checker().wrapAssertion(()->assertTextPresent(" was created. The column(s) of domain ", 1));// Create domain and update columns combined into a single event
        checker().wrapAssertion(()->assertTextPresent(" were modified.", 7));          // The column(s) of LIST_NAME_COLORS domain were modified
        checker().wrapAssertion(()->assertTextPresent("The descriptor of domain", 1));          // The description LIST_NAME_COLORS domain were modified
        checker().wrapAssertion(()->assertTextPresent("Bulk inserted", 2));
        checker().wrapAssertion(()->assertTextPresent("A new list record was inserted", 1));
        checker().wrapAssertion(()->assertTextPresent("was created", 2));                // Once for the list, once for the domain
        // List insert/update events should each have a link to the list item that was modified, but the other events won't have a link
        checker().wrapAssertion(()->assertEquals("details Links", 6/*List Events*/ + 8/*Domain Audit*/, DataRegionTable.detailsLinkLocator().findElements(getDriver()).size()));
        checker().wrapAssertion(()->assertEquals("Project Links", 17, DataRegionTable.Locators.table().append(Locator.linkWithText(PROJECT_VERIFY)).findElements(getDriver()).size()));
        checker().wrapAssertion(()->assertEquals("List Links", 17, DataRegionTable.Locators.table().append(Locator.linkWithText(LIST_NAME_COLORS)).findElements(getDriver()).size()));
        checker().screenShotIfNewError("List_History_Error");

        DataRegionTable dataRegionTable = new DataRegionTable("query", getDriver());
        dataRegionTable.clickRowDetails(0);
        checker().wrapAssertion(()->assertTextPresent("List Item Details"));
        checker().wrapAssertion(()->assertTextNotPresent("No details available for this event.", "Unable to find the audit history detail for this event"));
        checker().screenShotIfNewError("History_Detail_Error");

        clickButton("Done");
        waitAndClickAndWait(Locator.linkWithText(PROJECT_VERIFY).index(3));

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
        _listHelper.createList(PROJECT_OTHER, LIST3_NAME_OWNERS, new FieldDefinition(LIST3_KEY_NAME, LIST3_KEY_TYPE), _list3Col2);

        log("Upload data to second list");
        _listHelper.goToList(LIST3_NAME_OWNERS);
        _listHelper.uploadData(LIST3_DATA);

        log("Add list -- " + LIST2_NAME_CARS);
        _listHelper.createList(PROJECT_VERIFY, LIST2_NAME_CARS, new FieldDefinition(LIST2_KEY_NAME, LIST2_KEY_TYPE), _list2Col1, _list3Col1);

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
        _customizeViewsHelper.addColumn(FieldKey.fromParts(_list2Col1.getName(), _listColDesc.getName()));
        _customizeViewsHelper.addColumn(FieldKey.fromParts(_list2Col1.getName(), _listColMonth.getName()));
        _customizeViewsHelper.addColumn(FieldKey.fromParts(_list2Col1.getName(), _listColGood.getName()));
        _customizeViewsHelper.addFilter(FieldKey.fromParts(_list2Col1.getName(), _listColGood.getName()), "Is Less Than", "10");
        _customizeViewsHelper.addSort(FieldKey.fromParts(_list2Col1.getName(), _listColGood.getName()), SortDirection.ASC);
        _customizeViewsHelper.addColumn(_list3Col1.getName() + "/" + _list3Col1.getName());
        _customizeViewsHelper.addColumn(_list3Col1.getName() + "/" + _list3Col2.getName());
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

        // Use TabLoader, it is easier to use than TextSearch when dealing with 'tricky characters'.
        TabLoader tabLoader = new TabLoader(expFile, true);
        tabLoader.parseAsCSV();

        // According to Issue 52318 field keys are encoded.
        List<String> expectedValues = List.of(EscapeUtil.fieldKeyEncodePart(LIST_KEY_NAME2) + '/' + _listColDesc.getName(),
                EscapeUtil.fieldKeyEncodePart(LIST_KEY_NAME2) + '/' + _listColMonth.getName(),
                EscapeUtil.fieldKeyEncodePart(LIST_KEY_NAME2) + '/' + _listColGood.getName());

        List<Map<String, Object>> exportedFileData = tabLoader.load();
        List<String> actualValues = exportedFileData.get(0).keySet().stream().toList();

        assertTrue("Exported file does not contain expected header values.",
                actualValues.containsAll(expectedValues));

        assertEquals("Key value in row 0 not as expected.",
                LIST2_KEY3, exportedFileData.get(0).get(LIST2_KEY_NAME));

        assertEquals("Key value in row 1 not as expected.",
                LIST2_KEY2, exportedFileData.get(1).get(LIST2_KEY_NAME));

        assertEquals("Value of foreign key in row 1 not as expected.",
                LIST2_FOREIGN_KEY_OUTSIDE, exportedFileData.get(1).get(LIST3_KEY_NAME));

        assertEquals("Value of 'Wealth' column in row 1 not as expected.",
                LIST3_COL2, exportedFileData.get(1).get(LIST3_KEY_NAME + "/" + _list3Col2.getName()));

        log("Test edit row");
        list.updateRow(LIST2_KEY3, Maps.of(
                LIST_KEY_NAME2, TEST_DATA[TD_DESC][1],
                LIST3_KEY_NAME, LIST2_FOREIGN_KEY_OUTSIDE));

        final DataRegionTable dt = DataRegion(getDriver()).withName("query").find();
        dt.goToView("Default");
        assertTextPresent(TEST_DATA[TD_DESC][1], 2);

        log("Test deleting rows");
        dataRegionTable.checkAllOnPage();
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
        String exportUrl = WebTestHelper.buildURL("query", PROJECT_VERIFY, "exportRowsTsv",
            Map.of("schemaName", "lists", "query.queryName", LIST_NAME_COLORS));
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

    /**
     * CWE-639 (IDOR): a list audit event loaded by user-controlled rowId in
     * ListItemDetailsAction must be tied back to the URL-requested listId before its
     * old/new record maps are rendered. Without this, listId=X&rowId=N-belonging-to-Y
     * would render List Y's audit payload inside List X's details page.
     *
     * Builds two lists in the same container, generates a modify-audit-event on the
     * second one, then verifies the action refuses to render it when the URL names the
     * first list. A positive control confirms the matched-listId path still works, so
     * the test fails if the predicate is ever inverted/over-rejects.
     */
    @Test
    public void testAuditDetailRejectsRowIdFromOtherList() throws Exception
    {
        final String LIST_X = "IDOR_VICTIM_LIST";       // attacker claims to be viewing details for this list
        final String LIST_Y = "IDOR_SOURCE_LIST";       // audit event actually belongs to this list
        final String NAME_FIELD = "Name";
        final String LIST_Y_ROW_VALUE = "y-original";
        final String LIST_Y_ROW_EDITED = "y-modified-secret";

        log("Set up two lists in the same container, each with a Name field");
        Connection connection = createDefaultConnection();
        new IntListDefinition(LIST_X, "Key")
            .addField(new FieldDefinition(NAME_FIELD, ColumnType.String))
            .create(connection, getProjectName());
        new IntListDefinition(LIST_Y, "Key")
            .addField(new FieldDefinition(NAME_FIELD, ColumnType.String))
            .create(connection, getProjectName());

        log("Insert and then modify a row in List Y so it generates an audit event with old/new record maps");
        _listHelper.goToList(LIST_Y);
        _listHelper.clickImportData()
            .setText(NAME_FIELD + "\n" + LIST_Y_ROW_VALUE)
            .submit();
        DataRegionTable yTable = new DataRegionTable("query", getDriver());
        yTable.clickEditRow(yTable.getRowIndex(LIST_Y_ROW_VALUE));
        setFormElement(Locator.name("quf_" + NAME_FIELD), LIST_Y_ROW_EDITED);
        clickButton("Submit");

        log("Discover List X's listId and the audit rowId for List Y's modification event");
        Connection cn = createDefaultConnection();
        int listXId = lookupListId(cn, LIST_X);
        int listYId = lookupListId(cn, LIST_Y);
        int listYAuditRowId = lookupListAuditRowId(cn, LIST_Y);

        log("Attack: URL says listId=" + LIST_X + " but rowId points at the audit event for " + LIST_Y);
        String attackUrl = WebTestHelper.buildURL("list", getProjectName(), "listItemDetails",
            Map.of("listId", String.valueOf(listXId), "rowId", String.valueOf(listYAuditRowId)));
        beginAt(attackUrl);
        assertEquals("Action should render normally (200), not throw", 200, getResponseCode());
        assertTextPresent("No details available for this event");
        assertTextNotPresent(LIST_Y_ROW_VALUE);   // proof of non-disclosure: pre-edit value
        assertTextNotPresent(LIST_Y_ROW_EDITED);  // proof of non-disclosure: post-edit value

        log("Positive control: same rowId but with the matching listId must still render the audit changes");
        String matchedUrl = WebTestHelper.buildURL("list", getProjectName(), "listItemDetails",
            Map.of("listId", String.valueOf(listYId), "rowId", String.valueOf(listYAuditRowId)));
        beginAt(matchedUrl);
        assertEquals(200, getResponseCode());
        assertTextPresent(LIST_Y_ROW_VALUE);
        assertTextPresent(LIST_Y_ROW_EDITED);
        assertTextNotPresent("No details available for this event");
    }

    private int lookupListId(Connection cn, String listName) throws Exception
    {
        SelectRowsCommand cmd = new SelectRowsCommand("exp", "Lists");
        cmd.setColumns(List.of("RowId", "Name"));
        cmd.addFilter(new Filter("Name", listName, Filter.Operator.EQUAL));
        SelectRowsResponse rs = cmd.execute(cn, getProjectName());
        if (rs.getRows().isEmpty())
            throw new AssertionError("No exp.Lists row for " + listName);
        return ((Number) rs.getRows().get(0).get("RowId")).intValue();
    }

    private int lookupListAuditRowId(Connection cn, String listName) throws Exception
    {
        // Most-recent ListAuditEvent for this list; the row-modify above will be it.
        SelectRowsCommand cmd = new SelectRowsCommand("auditLog", "ListAuditEvent");
        cmd.setColumns(List.of("RowId", "ListName", "Comment"));
        cmd.addFilter(new Filter("ListName", listName, Filter.Operator.EQUAL));
        cmd.setSorts(List.of(new Sort("RowId", Sort.Direction.DESCENDING)));
        cmd.setMaxRows(1);
        SelectRowsResponse rs = cmd.execute(cn, getProjectName());
        if (rs.getRows().isEmpty())
            throw new AssertionError("No ListAuditEvent for " + listName);
        return ((Number) rs.getRows().get(0).get("RowId")).intValue();
    }

    /* Issue 23487: add regression coverage for batch insert into list with multiple errors
    */
    @Test
    public void testBatchInsertErrors()
    {
        // create the list for this case
        String multiErrorListName = "multiErrorBatchList";
        String[] expectedErrors = new String[]{
            getConversionErrorMessage("green", "ShouldInsertCorrectly", Boolean.class),
            getConversionErrorMessage("five", "Id", Integer.class)
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

        _listHelper.createList(PROJECT_VERIFY, mergeListName, "Key", col("Name", ColumnType.String));

        ImportDataPage importDataPage = _listHelper.clickImportData();
        checker().verifyFalse("For list with an integer, auto-increment key, merge option should not be available", importDataPage.isPasteMergeOptionPresent());
    }

    @Test
    public void testAddListColumnOverRemoteAPI() throws Exception
    {
        List<FieldDefinition> cols = Arrays.asList(
                new FieldDefinition("name", ColumnType.String),
                new FieldDefinition("title", ColumnType.String),
                new FieldDefinition("dewey", ColumnType.Decimal)
        );
        String listName = "remoteApiListTestAddColumn";
        TestDataGenerator dgen = new TestDataGenerator("lists", listName, getProjectName())
                .withColumns(cols);
        DomainResponse createResponse = dgen.createList(createDefaultConnection(), "key");
        Domain listDomain = createResponse.getDomain();
        List<PropertyDescriptor> listFields = createResponse.getDomain().getFields();
        listFields.add(new FieldDefinition("volume", ColumnType.Decimal));
        listDomain.setFields(listFields);

        // now save with an extra field
        SaveDomainCommand saveCmd = new SaveDomainCommand(dgen.getSchema(), dgen.getQueryName());
        saveCmd.setDomainDesign(listDomain);
        DomainResponse saveResponse = saveCmd.execute(createDefaultConnection(), dgen.getContainerPath());

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
                new FieldDefinition("name", ColumnType.String),
                new FieldDefinition("title", ColumnType.String),
                new FieldDefinition("dewey", ColumnType.Decimal),
                new FieldDefinition("removeMe", ColumnType.Decimal)
        );
        String listName = "remoteApiListTestRemoveColumn";
        TestDataGenerator dgen = new TestDataGenerator("lists", listName, getProjectName())
                .withColumns(cols);
        DomainResponse createResponse = dgen.createList(createDefaultConnection(), "key");
        Domain listDomain = createResponse.getDomain();
        List<PropertyDescriptor> listFields = createResponse.getDomain().getFields();
        listFields.removeIf(a-> a.getName().equals("removeMe"));
        listDomain.setFields(listFields);

        SaveDomainCommand saveCmd = new SaveDomainCommand(dgen.getSchema(), dgen.getQueryName());
        saveCmd.setDomainDesign(listDomain);
        DomainResponse saveResponse = saveCmd.execute(createDefaultConnection(), dgen.getContainerPath());

        checker().verifyFalse("'removeMe' field was not deleted.",
                saveResponse.getDomain().getFields().stream()
                        .anyMatch(a -> a.getName().equals("removeMe")));
    }

    @Test
    public void testChangeListNameOverAPI() throws Exception
    {
        List<FieldDefinition> cols = Arrays.asList(
                new FieldDefinition("name", ColumnType.String),
                new FieldDefinition("title", ColumnType.String),
                new FieldDefinition("dewey", ColumnType.Decimal)
        );
        String listName = "remoteAPIBeforeRename";
        TestDataGenerator dgen = new TestDataGenerator("lists", listName, getProjectName())
                .withColumns(cols);
        DomainResponse createResponse = dgen.createList(createDefaultConnection(), "key");
        Domain listDomain = createResponse.getDomain();
        listDomain.setName("remoteAPIAfterRename");

        SaveDomainCommand saveCmd = new SaveDomainCommand(listDomain.getDomainId());
        saveCmd.setDomainDesign(listDomain);
        DomainResponse saveResponse = saveCmd.execute(createDefaultConnection(), dgen.getContainerPath());

        assertEquals("remoteAPIAfterRename", saveResponse.getDomain().getName());
    }

    // Issue 52694 Links broken to list, data classes from the (list)-begin.view page if their names end with a /
    @Test
    public void testChangeListName()
    {

        String listNameBefore = TestDataGenerator.randomDomainName("Before Rename", DomainUtils.DomainKind.IntList);

        _listHelper.createList(PROJECT_VERIFY, listNameBefore,
                new FieldDefinition("name", ColumnType.String),
                new FieldDefinition("title", ColumnType.String),
                new FieldDefinition("dewey", ColumnType.Decimal));

        String listNameAfter = "After Rename (Issue 52694) /";
        _listHelper.goToEditDesign(listNameBefore)
                .setName(listNameAfter)
                .clickSave();

        List<String> actualLists = goToManageLists().getGrid().getListNames();

        assertTrue(String.format("Updated list name '%s' is not there.", listNameAfter),
                actualLists.contains(listNameAfter));

        assertFalse(String.format("Previous list name '%s' is there, it should not be.", listNameBefore),
                actualLists.contains(listNameBefore));

        clickAndWait(Locator.linkWithText(listNameAfter));

        assertElementVisible(Locator.tagContainingText("h3", listNameAfter));

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
        final String listName = "listSelfJoin" + DOMAIN_TRICKY_CHARACTERS;
        final String dummyBase = "dummyCol";
        final String dummyCol = dummyBase + TRICKY_CHARACTERS;
        final String lookupField = "lookupField" + TRICKY_CHARACTERS;
        final String lookupSchema = "lists";
        final String keyCol = "Key &%<+\\"; // Issue 54094: Verify key field ending with "\"

        log("Issue 6883: test list self join");

        FieldDefinition[] columns = new FieldDefinition[] {
                new FieldDefinition(dummyCol, ColumnType.String)
        };
        FieldDefinition lookupCol = new FieldDefinition(lookupField,
                new FieldDefinition.IntLookup(lookupSchema, listName));
        // create the list
        _listHelper.createList(PROJECT_VERIFY, listName, keyCol, columns);
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
        regionTable.clickEditRow(0);
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
        FieldDefinition[] cols = new FieldDefinition[]{col(PROJECT_VERIFY, lookupColumn, "A" )};
        _listHelper.createList(PROJECT_OTHER, crossContainerLookupList, "Key", cols);
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

        checker().verifyEquals("Incorrect filter on list", Arrays.asList(_listColGood.getLabel() + " < 10"),
                getTexts(DataRegionTable.Locators.filterContextAction().findElements(getDriver())));
        region = new DataRegionTable("qwp3", getDriver());
        region.openFilterDialog(_listColGood.getName());

        // Issue 52547: LKS filter dialog treats many filter types as if they are Equals
        assertEquals("Faceted filter tab should not be selected.", "Choose Filters", getText(Locator.css(".x-tab-strip-active")));

        clickButton("Cancel", 0);
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
        assertEquals(ColumnType.Boolean, fieldsPanel.getField("BoolCol").getType());
        assertEquals(ColumnType.Integer, fieldsPanel.getField("IntCol").getType());
        assertEquals(ColumnType.Decimal, fieldsPanel.getField("NumCol").getType());
        assertEquals(ColumnType.DateAndTime, fieldsPanel.getField("DateCol").getType());
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

        AuditLogHelper.DetailedAuditEventRow expectedDomainEvent = new AuditLogHelper.DetailedAuditEventRow(null, TSV_LIST_NAME, null,
                "The column(s) of domain " + TSV_LIST_NAME + " were modified.",
                "", null, null, null);
        boolean pass = _auditLogHelper.validateLastDomainAuditEvents(TSV_LIST_NAME, getProjectName(), expectedDomainEvent,
                Map.of("IntCol", new AuditLogHelper.DetailedAuditEventRow(null, "IntCol", "Modified","The following property was updated: ConditionalFormat",null, null, null, "ConditionalFormat:  > format.column~gt=7: text-decoration: line-through;, format.column~gt=5: font-weight: bold;"),
                        "BoolCol", new AuditLogHelper.DetailedAuditEventRow(null, "BoolCol", "Modified","The following property was updated: ConditionalFormat",null, null, null, "ConditionalFormat:  > format.column~eq=true: text-decoration: line-through;font-weight: bold;font-style: italic;color: #68ccca;background-color: #d33115 !important;"))
        );
        checker().verifyTrue("Domain audit comment not as expected after changing conditional format", pass);

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
        // Issue 8329
        log("8329: Test that renaming a field then creating a new field with the old name doesn't result in awful things");
        String listName = "new";

        // Issue 52480
        String origFieldName = ": Some Field Name 1 /@\".";
        String newFieldName = "Some Field Name 1";

        String invalidListName = TestDataGenerator.randomInvalidDomainName(null, 0, 5);
        EditListDefinitionPage listDefinitionPage = _listHelper.beginCreateList(PROJECT_VERIFY, invalidListName);
        listDefinitionPage.manuallyDefineFieldsWithAutoIncrementingKey("key");
        List<String> errors = listDefinitionPage.clickSaveExpectingErrors();
        assertTrue("Error msg not as expected during list creation", errors.contains("Invalid IntList name '" + invalidListName + "'. IntList name must start with a letter or a number."));

        _listHelper.createList(PROJECT_VERIFY, listName, "key",
                new FieldDefinition(origFieldName, ColumnType.String).setLabel(origFieldName).setDescription("first column"));

        listDefinitionPage = _listHelper.goToEditDesign(listName);
        listDefinitionPage.setName(invalidListName);
        errors = listDefinitionPage.clickSaveExpectingErrors();
        assertTrue("Error msg not as expected during list renaming", errors.contains("Invalid IntList name '" + invalidListName + "'. IntList name must start with a letter or a number."));
        listDefinitionPage.setName(listName);
        listDefinitionPage.getFieldsPanel()
                .getField(origFieldName)
                .setName(newFieldName)
                .setLabel(newFieldName);
        listDefinitionPage.clickSave();

        AuditLogHelper.DetailedAuditEventRow expectedDomainEvent = new AuditLogHelper.DetailedAuditEventRow(null, listName, null,
                "The column(s) of domain " + listName + " were modified.",
                "", null, null, null);
        boolean pass = _auditLogHelper.validateLastDomainAuditEvents(listName, getProjectName(), expectedDomainEvent,
                Map.of(newFieldName, new AuditLogHelper.DetailedAuditEventRow(null, newFieldName, "Modified","The following properties were updated: Name, Label",null, null, null, "Name: "+ origFieldName + " > " + newFieldName + "\nLabel: " + origFieldName + " > " + newFieldName)));
        checker().verifyTrue("Domain audit comment not as expected after renaming a field", pass);

        assertTextPresent(newFieldName);
        assertTextNotPresent(origFieldName);

        listDefinitionPage = _listHelper.goToEditDesign(listName);
        FieldDefinition newCol = new FieldDefinition(origFieldName, ColumnType.String).setLabel(origFieldName).setDescription("second column");
        listDefinitionPage.addField(newCol);
        listDefinitionPage.clickSave();

        assertTextBefore(newFieldName, origFieldName);
    }


    @Test
    public void requiredFieldsTest()
    {
        log("Test changing required property of field");
        String listName = "requiredColList";
        String fieldA = "c$a";
        String fieldB = "c_b";

        _listHelper.createList(PROJECT_VERIFY, listName, "key",
                new FieldDefinition(fieldA, ColumnType.String).setDescription("first column").setRequired(false),
                new FieldDefinition(fieldB, ColumnType.String).setDescription("second column").setRequired(false)
        );

        // insert a row with a NULL value and NON-NULL value
        Map<String, String> row = new HashMap<>();
        row.put(fieldA, "not null");
        row.put(fieldB, "");
        _listHelper.insertNewRow(row, false);
        row.put(fieldA, "still not null");
        row.put(fieldB, "also not null");
        _listHelper.insertNewRow(row, false);

        // fieldA can be set to required==true
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(listName);
        listDefinitionPage.getFieldsPanel()
                .getField(fieldA)
                .setRequiredField(true);
        listDefinitionPage.clickSave();

        // fieldB can not be set to required==true
        listDefinitionPage = _listHelper.goToEditDesign(listName);
        listDefinitionPage.getFieldsPanel()
                .getField(fieldB)
                .setRequiredField(true);
        List<String> errors = listDefinitionPage.clickSaveExpectingErrors();
        assertEquals(2, errors.size());
        assertEquals("The property \"" + fieldB + "\" cannot be required when it contains rows with blank values.", errors.get(0));
        assertEquals("Please correct errors in " + listName + " before saving.", errors.get(1));

        goToProjectHome();
        clickAndWait(Locator.linkWithText(listName));
        _listHelper.deleteList();
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
        _listHelper.createList(PROJECT_VERIFY, listName, "key",
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

        AuditLogHelper.DetailedAuditEventRow expectedDomainEvent = new AuditLogHelper.DetailedAuditEventRow(null, listName, null,
                "The column(s) of domain " + listName + " were modified.",
                "", null, null, null);
        boolean pass = _auditLogHelper.validateLastDomainAuditEvents(listName, getProjectName(), expectedDomainEvent,
                Map.of("LimitedPhiColumn", new AuditLogHelper.DetailedAuditEventRow(null, "LimitedPhiColumn", "Modified","The following property was updated: PHI",null, null, null, "PHI: Not PHI > Limited PHI"),
                        "PhiColumn", new AuditLogHelper.DetailedAuditEventRow(null, "PhiColumn", "Modified","The following property was updated: PHI",null, null, null, "PHI: Not PHI > Full PHI"),
                        "RestrictedPhiColumn", new AuditLogHelper.DetailedAuditEventRow(null, "RestrictedPhiColumn", "Modified","The following property was updated: PHI",null, null, null, "PHI: Not PHI > Restricted PHI"))
        );
        checker().verifyTrue("Domain audit comment not as expected after changing PHI setting", pass);

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
        _listHelper.createList(getProjectName(), listName, "id",
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
        _listHelper.createList(getProjectName(), listName, "id",
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
        _listHelper.createList(getProjectName(), listName, "id",
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
        _listHelper.createList(PROJECT_VERIFY, listName, "key",
                new FieldDefinition(fieldName1, ColumnType.Integer),
                new FieldDefinition(fieldName2, ColumnType.DateAndTime),
                new FieldDefinition(fieldName3, ColumnType.Boolean));

        // verify initial set of indices
        viewRawTableMetadata(listName);
        verifyTableIndices("unique_constraint_list_", Collections.emptyList());

        // set fields to have constraints
        EditListDefinitionPage listDefinitionPage = _listHelper.goToEditDesign(listName);
        listDefinitionPage.getFieldsPanel()
                .getField(fieldName1).expand().clickAdvancedSettings().setSingleFieldIndex(AdvancedSettingsDialog.SingleFieldIndexType.UNIQUE_INDEX)
                .apply();
        listDefinitionPage.getFieldsPanel()
                .getField(fieldName2).expand().clickAdvancedSettings().setSingleFieldIndex(AdvancedSettingsDialog.SingleFieldIndexType.UNIQUE_INDEX)
                .apply();
        // set one field to have non-unique constraint
        listDefinitionPage.getFieldsPanel()
                .getField(fieldName3).expand().clickAdvancedSettings().setSingleFieldIndex(AdvancedSettingsDialog.SingleFieldIndexType.INDEX)
                .apply();
        listDefinitionPage.clickSave();

        AuditLogHelper.DetailedAuditEventRow expectedDomainEvent = new AuditLogHelper.DetailedAuditEventRow(null, listName, null,
                "The descriptor of domain " + listName + " was updated.",
                "", null, null, "Indices:  > [FieldName@3, unique: false, field Name1, unique: true, fieldName_2, unique: true]");
        boolean pass = _auditLogHelper.validateLastDomainAuditEvents(listName, getProjectName(), expectedDomainEvent, Collections.emptyMap());
        checker().verifyTrue("Domain audit comment not as expected after updating field unique constraint", pass);

        viewRawTableMetadata(listName);
        verifyTableIndices("unique_constraint_list_", List.of("field_Name1", "fieldName_2", "FieldName_3"));
        verifyTableIndexNonUnique("unique_constraint_list_", "field_Name1", true);
        verifyTableIndexNonUnique("unique_constraint_list_", "fieldName_2", true);
        verifyTableIndexNonUnique("unique_constraint_list_", "FieldName_3", false);

        // remove a field unique constraint, change a field from unique -> non-unique, and change one from non-unique -> unique
        listDefinitionPage = _listHelper.goToEditDesign(listName);
        listDefinitionPage.getFieldsPanel()
                .getField(fieldName1).expand().clickAdvancedSettings().setSingleFieldIndex(null)
                .apply();
        listDefinitionPage.getFieldsPanel()
                .getField(fieldName2).expand().clickAdvancedSettings().setSingleFieldIndex(AdvancedSettingsDialog.SingleFieldIndexType.INDEX)
                .apply();
        listDefinitionPage.getFieldsPanel()
                .getField(fieldName3).expand().clickAdvancedSettings().setSingleFieldIndex(AdvancedSettingsDialog.SingleFieldIndexType.UNIQUE_INDEX)
                .apply();
        listDefinitionPage.clickSave();

        String expectedDataChanges = "Indices: [field name1, unique: true, fieldname@3, unique: false, fieldname_2, unique: true] > [FieldName@3, unique: true, fieldName_2, unique: false]";
        if (!IS_POSTGRES) expectedDataChanges = "Indices: [FieldName@3, unique: false, field Name1, unique: true, fieldName_2, unique: true] > [FieldName@3, unique: true, fieldName_2, unique: false]";
        expectedDomainEvent = new AuditLogHelper.DetailedAuditEventRow(null, listName, null,
                "The descriptor of domain " + listName + " was updated.",
                "", null, null, expectedDataChanges);
        pass = _auditLogHelper.validateLastDomainAuditEvents(listName, getProjectName(), expectedDomainEvent, Collections.emptyMap());
        checker().verifyTrue("Domain audit comment not as expected after updating field unique constraint", pass);

        viewRawTableMetadata(listName);
        verifyTableIndices("unique_constraint_list_", List.of("fieldName_2", "FieldName_3"));
        assertTextNotPresent("unique_constraint_list_field_name1");
        verifyTableIndexNonUnique("unique_constraint_list_", "fieldName_2", false);
        verifyTableIndexNonUnique("unique_constraint_list_", "FieldName_3", true);
    }

    @Test // Issue 52247
    public void testAutoIncrementKeyEncoded()
    {
        // setup a list with an auto-increment key that we need to make sure is encoded in the form input
        String encodedListName = "autoIncrementEncodeList";
        String keyName = "'><script>alert(\":(\")</script>'";
        String encodedKeyFieldName = EscapeUtil.getFormFieldName(keyName);
        _listHelper.createList(PROJECT_VERIFY, encodedListName, keyName, col("Name", ColumnType.String));
        _listHelper.goToList(encodedListName);

        DataRegionTable table = new DataRegionTable("query", getDriver());
        CustomizeView customizeView = table.openCustomizeGrid();
        customizeView.showHiddenItems();
        customizeView.addColumn(EscapeUtil.fieldKeyEncodePart(keyName));
        customizeView.applyCustomView();

        // insert a new row and verify the key field is not present
        table.clickInsertNewRow();
        checker().withScreenshot().verifyEquals("List fields on insert form.", List.of("quf_Name"), getQueryFormFieldNames());
        String nameValue = "test";
        setFormElement(Locator.name(EscapeUtil.getFormFieldName("Name")), nameValue);
        clickButton("Submit");

        // verify the name value is persisted
        table = new DataRegionTable("query", getDriver());
        checker().verifyEquals("Key value not as expected", "1", table.getDataAsText(0, keyName));
        checker().verifyEquals("Name value not as expected", nameValue, table.getDataAsText(0, "Name"));

        // verify name value can be updated
        table.clickEditRow(0);
        checker().withScreenshot().verifyEquals("List fields on update form.", List.of("quf_Name", encodedKeyFieldName), getQueryFormFieldNames());
        nameValue = "test updated";
        setFormElement(Locator.name(EscapeUtil.getFormFieldName("Name")), nameValue);
        clickButton("Submit");

        // verify the name value is persisted
        table = new DataRegionTable("query", getDriver());
        checker().verifyEquals("Key value not as expected", "1", table.getDataAsText(0, keyName));
        checker().verifyEquals("Name value not as expected", nameValue, table.getDataAsText(0, "Name"));

        _listHelper.deleteList();
    }

    @Test
    public void testMultiChoiceValues()
    {
        OptionalFeatureHelper.enableOptionalFeature(getCurrentTest().createDefaultConnection(), "multiChoiceDataType");
        Assume.assumeTrue("Multi-choice text fields are only supported on PostgreSQL", WebTestHelper.getDatabaseType() == WebTestHelper.DatabaseType.PostgreSQL);
        // setup a list with an auto-increment key and multi text choice field
        String encodedListName = TestDataGenerator.randomDomainName("multiChoiceList", DomainUtils.DomainKind.IntList);
        String keyName = TestDataGenerator.randomFieldName("'><script>alert(\":(\")</script>'");
        String columnName = TestDataGenerator.randomFieldName("MultiChoiceField");
        List<String> tcValues = List.of("~`!@#$%^&*()_+=[]{}\\|';:\"<>?,./", "1", "2");
        _listHelper.createList(PROJECT_VERIFY, encodedListName, keyName, col(columnName, ColumnType.MultiValueTextChoice)
                .setMultiChoiceValues(tcValues));
        _listHelper.goToList(encodedListName);

        DataRegionTable table = new DataRegionTable("query", getDriver());
        UpdateQueryRowPage insertNewRow = table.clickInsertNewRow();
        List<String> valuesToChoose = tcValues.subList(1, 3);
        insertNewRow.setField(columnName, valuesToChoose);
        insertNewRow.submit();
        String expectedList = valuesToChoose.stream()
                .sorted()
                .collect(Collectors.joining(" "));
        checker().withScreenshot().verifyEquals("Multi choice value not as expected", expectedList, table.getDataAsText(0, columnName));

        UpdateQueryRowPage editRow = table.clickEditRow(0);
        valuesToChoose = tcValues.subList(1, 3);
        editRow.setField(columnName, valuesToChoose);
        editRow.submit();
        expectedList = valuesToChoose.stream()
                .sorted()
                .collect(Collectors.joining(" "));
        // verify the multi choice value is persisted
        checker().withScreenshot().verifyEquals("Multi choice value not as expected", expectedList, table.getDataAsText(0, columnName));

        _listHelper.deleteList();
    }

    private List<String> getQueryFormFieldNames()
    {
        return Locator.tag("input").attributeStartsWith("name", "quf_")
            .findElements(getDriver()).stream()
            .map(el -> el.getDomAttribute("name"))
            .toList();
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

    private void verifyTableIndexNonUnique(String prefix, String suffix, boolean isUnique)
    {
        String boolDisplay = isUnique ? "0" : "1";
        if (IS_POSTGRES) boolDisplay = isUnique ? "false" : "true";
        String fieldKey = prefix + suffix;
        if (IS_POSTGRES) fieldKey = fieldKey.toLowerCase();
        Locator locator = Locator.xpath("//td[contains(text(), '" + fieldKey + "')]/preceding-sibling::td[2][text()='" + boolDisplay + "']");
        checker().verifyTrue("Non_Unique value not as expected in metadata for locator: " + locator, locator.existsIn(getDriver()));
    }

    /**
     * Test "tricky characters" in field names, including key field. This will test CrUD operation for list items in
     * lists with an auto-key and user defined key. This  will also use file import for validation.
     *
     * @throws IOException Can be thrown by the file actions.
     */
    @Test
    public void testTrickyCharacterFields() throws IOException
    {
        // These validate Issue 52069 Issue 52070 Issue 52071
        testTricky("Tricky Field Character", false);
        testTricky("TrickyField Character Auto Key", true);

    }

    private void testTricky(String listName, boolean autoKey) throws IOException
    {

        String keyField = "Key Field \"`~!@#$%^&*()_-+={}[]|\\:;<>,.?/\u5668\u9aa8";
        String keyField_Bulk = "\"" + keyField.replace("\"", "\"\"") + "\"" ;
        String intField = "Int Field \"`~!@#$%^&*()_-+={}[]|\\:;<>,.?/\u00a5\u00e6";
        String intField_Bulk = "\"" + intField.replace("\"", "\"\"") + "\"";
        String trickyField = "\u5668\u9aa8\u00a5\u00e6\"`~!@#$%^&*()_-+={}[]|\\:;<>,.?/";
        String trickyField_Bulk = "\"" + trickyField.replace("\"", "\"\"") + "\"";

        log(String.format("Create list '%s' with key field '%s' and fields '%s', '%s'.",
                listName, keyField, intField, trickyField));

        if (!autoKey)
        {
            log("Key is not auto-increment.");
            _listHelper.createList(PROJECT_VERIFY, listName,
                    new FieldDefinition(keyField, ColumnType.Integer),
                    new FieldDefinition(intField, ColumnType.Integer),
                    new FieldDefinition(trickyField, ColumnType.Integer));
        }
        else
        {
            log("Key is auto-increment.");
            _listHelper.createList(PROJECT_VERIFY, listName, keyField,
                    new FieldDefinition(intField, ColumnType.Integer),
                    new FieldDefinition(trickyField, ColumnType.Integer));
        }

        assertNoLabKeyErrors();

        log("Insert a new row.");

        Map<String, String> row = new HashMap<>();

        List<Map<String, String>> expectedValues = new ArrayList<>();

        if (!autoKey)
        {
            row.put(keyField, "1");

            expectedValues.add(Map.of(EscapeUtil.fieldKeyEncodePart(keyField), "1",
                    EscapeUtil.fieldKeyEncodePart(intField), "100",
                    EscapeUtil.fieldKeyEncodePart(trickyField), "101"));
        }
        else
        {
            expectedValues.add(Map.of(EscapeUtil.fieldKeyEncodePart(intField), "100",
                    EscapeUtil.fieldKeyEncodePart(trickyField), "101"));
        }

        row.put(intField, "100");
        row.put(trickyField, "101");

        _listHelper.insertNewRow(row);

        assertNoLabKeyErrors();

        validateDataRegionTableForTricky(expectedValues);

        log("Use the bulk import form to add a new row.");

        StringBuilder sbBulkData = new StringBuilder();

        if (!autoKey)
        {
            sbBulkData.append(keyField_Bulk);
            sbBulkData.append("\t");
        }

        sbBulkData.append(intField_Bulk);
        sbBulkData.append("\t");
        sbBulkData.append(trickyField_Bulk);
        sbBulkData.append("\n");

        if (!autoKey)
        {
            sbBulkData.append("2\t");

            expectedValues.add(Map.of(EscapeUtil.fieldKeyEncodePart(keyField), "2",
                    EscapeUtil.fieldKeyEncodePart(intField), "200",
                    EscapeUtil.fieldKeyEncodePart(trickyField), "202"));
        }
        else
        {
            expectedValues.add(Map.of(EscapeUtil.fieldKeyEncodePart(intField), "200",
                    EscapeUtil.fieldKeyEncodePart(trickyField), "202"));
        }

        sbBulkData.append("200\t202");

        _listHelper.bulkImportData(sbBulkData.toString());

        assertNoLabKeyErrors();

        validateDataRegionTableForTricky(expectedValues);

        log("Use file import to add a new item.");
        sbBulkData = new StringBuilder();
        List<String> fileData = new ArrayList<>();

        if (!autoKey)
        {
            sbBulkData.append(keyField_Bulk);
            sbBulkData.append("\t");
        }

        sbBulkData.append(intField_Bulk);
        sbBulkData.append("\t");
        sbBulkData.append(trickyField_Bulk);
        fileData.add(sbBulkData.toString());

        sbBulkData = new StringBuilder();

        if (!autoKey)
        {
            sbBulkData.append("3\t");

            expectedValues.add(Map.of(EscapeUtil.fieldKeyEncodePart(keyField), "3",
                    EscapeUtil.fieldKeyEncodePart(intField), "300",
                    EscapeUtil.fieldKeyEncodePart(trickyField), "303"));

        }
        else
        {
            expectedValues.add(Map.of(EscapeUtil.fieldKeyEncodePart(intField), "300",
                    EscapeUtil.fieldKeyEncodePart(trickyField), "303"));
        }

        sbBulkData.append("300\t303");
        fileData.add(sbBulkData.toString());

        File importFile = TestFileUtils.writeTempFile("ListTest_Tricky.tsv", String.join(System.lineSeparator(), fileData));

        _listHelper.importDataFromFile(importFile);

        assertNoLabKeyErrors();

        validateDataRegionTableForTricky(expectedValues);

        log(String.format("For row 0 update the value in fields '%s' and '%s' in the UI.", intField, trickyField));

        DataRegionTable dataRegionTable = new DataRegionTable("query", getDriver());
        dataRegionTable.updateRow(0, Map.of(intField, "123",
                trickyField, "456"));

        assertNoLabKeyErrors();

        if (!autoKey)
        {
            expectedValues.set(0, Map.of(EscapeUtil.fieldKeyEncodePart(keyField), "1",
                    EscapeUtil.fieldKeyEncodePart(intField), "123",
                    EscapeUtil.fieldKeyEncodePart(trickyField), "456"));
        }
        else
        {
            expectedValues.set(0, Map.of(EscapeUtil.fieldKeyEncodePart(intField), "123",
                    EscapeUtil.fieldKeyEncodePart(trickyField), "456"));
        }

        validateDataRegionTableForTricky(expectedValues);

        // This will validate Issue 52069
        log("Check the column tooltip.");
        if (!autoKey)
        {
            assertEquals(String.format("Tooltip for column '%s' not as expected.", keyField),
                    keyField, dataRegionTable.getColumnTitle(EscapeUtil.fieldKeyEncodePart(keyField)));
        }

        assertEquals(String.format("Tooltip for column '%s' not as expected.", intField),
                intField, dataRegionTable.getColumnTitle(EscapeUtil.fieldKeyEncodePart(intField)));

        assertEquals(String.format("Tooltip for column '%s' not as expected.", trickyField),
                trickyField, dataRegionTable.getColumnTitle(EscapeUtil.fieldKeyEncodePart(trickyField)));

        log("Delete row 0.");

        dataRegionTable = new DataRegionTable("query", getDriver());
        dataRegionTable.checkCheckbox(0);
        dataRegionTable.deleteSelectedRows();

        assertNoLabKeyErrors();

        expectedValues.remove(0);

        validateDataRegionTableForTricky(expectedValues);

        // Validates Issue 52071
        dataRegionTable.clickRowDetails(0);

        List<String> actualFields = Locator.tagWithClass("td", "lk-form-label").findElements(getDriver()).stream().map(WebElement::getText).toList();

        List<String> expectedFields = new ArrayList<>();

        // Add the expected files in the expected display order.
        if (!autoKey)
        {
            expectedFields.add(keyField);
        }

        expectedFields.add(intField);
        expectedFields.add(trickyField);

        // Replace the _ with a space and add a : at the end.
        expectedFields.replaceAll(f -> f.replace("_", " ") + ":");

        for (int i = 0; i < expectedFields.size(); i++)
        {
            assertEquals(String.format("Row detail for column '%s' not as expected.", expectedFields.get(i)),
                    expectedFields.get(i), actualFields.get(i));
        }

    }

    private void validateDataRegionTableForTricky(List<Map<String, String>> expectedValue)
    {
        DataRegionTable dataRegionTable = new DataRegionTable("query", getDriver());
        List<Map<String, String>> actualValue = dataRegionTable.getTableData();

        assertEquals("List data not as expected after action.",
                expectedValue, actualValue);
    }

    //
    // CUSTOMIZE URL tests
    //

    FieldDefinition col(String name, ColumnType type)
    {
        return new FieldDefinition(name, type);
    }

    FieldDefinition col(String name, String table)
    {
        return col(null, name, table);
    }

    FieldDefinition col(String folder, String name, String table)
    {
        return new FieldDefinition(name, new FieldDefinition.IntLookup(folder, "lists", table));
    }

    FieldDefinition colURL(String name, ColumnType type, String url)
    {
        return new FieldDefinition(name, type).setURL(url);
    }

    List<FieldDefinition> Acolumns = Arrays.asList(
            col("A", ColumnType.Integer),
            colURL("title", ColumnType.String, "/junit/echoForm.view?key=${A}&title=${title}&table=A"),
            col("Bfk", "B")
    );
    String[][] Adata = new String[][]
    {
        {"1", "one A", "1"},
    };

    List<FieldDefinition> Bcolumns = Arrays.asList(
            col("B", ColumnType.Integer),
            colURL("title", ColumnType.String, "org.labkey.core.junit.JunitController$EchoFormAction.class?key=${B}&title=${title}&table=B"),
            col("Cfk", "C")
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
        createList(name, cols, toTSV(cols,data));
    }

    void createList(String name, List<FieldDefinition> cols, String tsvData)
    {
        log("Add List -- " + name);
        _listHelper.createList(PROJECT_VERIFY, name, cols.get(0), cols.subList(1, cols.size()).toArray(new FieldDefinition[cols.size() - 1]));
        _listHelper.goToList(name);
        _listHelper.clickImportData();
        setListImportAsTestDataField(tsvData);
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

            beginAt(WebTestHelper.buildURL("query", PROJECT_VERIFY, "executeQuery",
                Map.of("schemaName", "lists", "query.queryName", "A")));

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
            _customizeViewsHelper.addColumn("Bfk/B");
            _customizeViewsHelper.addColumn("Bfk/title");
            _customizeViewsHelper.addColumn("Bfk/Cfk");
            _customizeViewsHelper.addColumn("Bfk/Cfk/C");
            _customizeViewsHelper.addColumn("Bfk/Cfk/title");
            _customizeViewsHelper.saveCustomView("allColumns");

            clickAndWait(Locator.linkWithText("one C").index(1));
            assertElementPresent(inputWithValue("key","1"));
            assertElementPresent(inputWithValue("table","C"));
            assertElementPresent(inputWithValue("title","one C"));
            assertTrue(getCurrentRelativeURL().contains(WebTestHelper.buildRelativeUrl("junit", PROJECT_VERIFY, "echoForm")));
        }
        popLocation();
    }

    /**
     * Regression for issue 53361: 'list-details.view' doesn't work when list pk is named "name"
     * Test for both name and listId key field names.
     */
    @Test
    public void testPkNameParameterCollision() throws IOException, CommandException
    {
        // Create lists with PKs having the same name as detail URL list definition identifier
        // params to ensure we can resolve detail pages correctly
        validateDetailsView("list_name_key_check", "Name");
        validateDetailsView("list_id_key_check", "ListId");
    }

    private void validateDetailsView(String listName, String pkCol) throws CommandException, IOException
    {
        listName = TestDataGenerator.randomDomainName(listName, DomainUtils.DomainKind.IntList);
        var dgen = new VarListDefinition(listName)
                .setFields(List.of(new FieldDefinition(pkCol)))
                .create(createDefaultConnection(), getProjectName())
                .withGeneratedRows(10);
        List<String> pks = dgen.getRows().stream().map(row -> (String) row.get(pkCol)).toList();
        dgen.insertRows();

        goToProjectHome();
        goToManageLists().getGrid().viewListData(listName);

        clickAndWait(Locator.linkWithText(pks.get(0)));
        assertElementPresent(Locator.tagContainingText("td", pks.get(0)));
    }

    @Test // Issue 53979
    public void testDecimalFieldFiniteValues()
    {
        String listName = "DecimalFieldList";
        FieldInfo decField = FieldInfo.random("decimalField", ColumnType.Decimal);
        List<Map<String, Object>> rowMaps = new ArrayList<>();
        rowMaps.add(Map.of("key", 1, decField.getName(), "1.1"));
        rowMaps.add(Map.of("key", 2, decField.getName(), "1.7976931348623157e+308"));
        rowMaps.add(Map.of("key", 3, decField.getName(), "-1.7976931348623157e+308"));
        rowMaps.add(Map.of("key", 4, decField.getName(), "Infinity"));
        rowMaps.add(Map.of("key", 5, decField.getName(), "-Infinity"));
        rowMaps.add(Map.of("key", 6, decField.getName(), "Inf"));
        rowMaps.add(Map.of("key", 7, decField.getName(), "-Inf"));
        rowMaps.add(Map.of("key", 8, decField.getName(), "NaN"));

        createList(listName, List.of(
                new FieldDefinition("key", ColumnType.Integer),
                decField.getFieldDefinition()
        ), TestDataUtils.tsvStringFromRowMaps(rowMaps, List.of("key", decField.getName()), true));

        DataRegionTable table = new DataRegionTable("query", getDriver());
        checker().verifyEquals("Decimal field values not as expected",
                List.of("1.1", "1.7976931348623157E308", "-1.7976931348623157E308", "Infinity", "-Infinity", "Infinity", "-Infinity", "NaN"),
                table.getColumnDataAsText(decField.getName()));

        table.clickInsertNewRow();
        setFormElement(Locator.name(EscapeUtil.getFormFieldName("key")), "9");
        setFormElement(Locator.name(EscapeUtil.getFormFieldName(decField.getName())), "bogus");
        clickButton("Submit");
        assertTextPresent("Could not convert value: bogus");

        setFormElement(Locator.name(EscapeUtil.getFormFieldName(decField.getName())), "1.7976931348623157e+309");
        clickButton("Submit");
        assertTextPresent("Could not convert value: 1.7976931348623157e+309");

        setFormElement(Locator.name(EscapeUtil.getFormFieldName(decField.getName())), "-1.7976931348623157e+309");
        clickButton("Submit");
        assertTextPresent("Could not convert value: -1.7976931348623157e+309");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
