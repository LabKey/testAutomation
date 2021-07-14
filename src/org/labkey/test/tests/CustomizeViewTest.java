/*
 * Copyright (c) 2011-2019 LabKey Corporation
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

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.list.GridPage;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldDefinition.ColumnType;
import org.labkey.test.params.FieldKey;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.params.list.ListDefinition;
import org.labkey.test.util.Crawler;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.SummaryStatisticsHelper;
import org.labkey.test.util.TestDataGenerator;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class})
@BaseWebDriverTest.ClassTimeout(minutes = 8)
public class CustomizeViewTest extends BaseWebDriverTest
{
    public static final String PROJECT_NAME = "CustomizeViewTest";
    public static final String LIST_NAME = "People" + INJECT_CHARS_1;
    private final static String LIST_KEY_COLUMN = "Key";
    private final static String LAST_NAME_COLUMN = "LastName" + INJECT_CHARS_2;
    private final static String FIRST_NAME_COLUMN = "FirstName";
    private final static String AGE_COLUMN = "Age";
    private final static String TEST_DATE_COLUMN = "TestDate";
    private final static List<FieldDefinition> LIST_COLUMNS = List.of(
            new FieldDefinition(FIRST_NAME_COLUMN, ColumnType.String).setLabel(FIRST_NAME_COLUMN + INJECT_CHARS_1).setDescription("The first name").setRequired(true),
            new FieldDefinition(LAST_NAME_COLUMN, ColumnType.String).setLabel("Last Name").setDescription("The last name").setRequired(true),
            new FieldDefinition(AGE_COLUMN, ColumnType.Integer).setLabel("Age").setDescription("The age" + INJECT_CHARS_1),
            new FieldDefinition(TEST_DATE_COLUMN, ColumnType.DateAndTime).setLabel("Test Date").setDescription("The test date")
    );

    private final static String[][] TEST_DATA =
            {
                    { "1", "Bill", "Billson", "34", "2016-05-01" },
                    { "2", "Jane", "Janeson", "42", "2016-05-02" },
                    { "3", "John", "Johnson", "17", "2016-05-03" },
                    { "4", "Mandy", "Mandyson", "32", "2016-05-04" },
                    { "5", "Norbert", "Norbertson", "28", "2016-05-05" },
                    { "6", "Penny", "Pennyson", "38", "" },
                    { "7", "Yak", "Yakson", "88", "" },
            };

    private SummaryStatisticsHelper _summaryStatisticsHelper;

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        CustomizeViewTest init = (CustomizeViewTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup() throws Exception
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        createList();
    }

    @Before
    public void preTest()
    {
        _summaryStatisticsHelper = new SummaryStatisticsHelper(this);

        GridPage.beginAt(this, PROJECT_NAME, 1);
    }

    @Test
    public void testSummaryStatistics()
    {
        final String statColumn = AGE_COLUMN;
        final String customTitle = "Oldness Factor" + INJECT_CHARS_2;

        setColumns(LAST_NAME_COLUMN, statColumn);
        DataRegionTable drt = new DataRegionTable("query", getDriver());

        log("** Set column title");
        assertTextNotPresent("Oldness Factor");
        setColumnTitle(statColumn, customTitle);
        assertTextPresent(customTitle);

        log("** Set summary statistics");
        drt.setSummaryStatistic(statColumn, SummaryStatisticsHelper.BASE_STAT_SUM, "279");
        drt.setSummaryStatistic(statColumn, SummaryStatisticsHelper.BASE_STAT_COUNT, "7");
        assertTrue("Summary statistic row didn't appear", drt.hasSummaryStatisticRow());
        String summaryStatStr = SummaryStatisticsHelper.BASE_STAT_COUNT + ": 7 " + SummaryStatisticsHelper.BASE_STAT_SUM + ": 279";
        assertEquals("Wrong summary statistics", summaryStatStr, _summaryStatisticsHelper.getSummaryStatisticFooterAsString(drt, statColumn));
        assertTextPresent(customTitle);

        log("** Clear column title");
        setColumnTitle(statColumn, null);
        assertTextNotPresent("Oldness Factor");

        log("** Clear summary statistics");
        drt.clearSummaryStatistic(statColumn, SummaryStatisticsHelper.BASE_STAT_SUM, "279");
        assertTrue("Summary statistic count should still be available", drt.hasSummaryStatisticRow());
        summaryStatStr = SummaryStatisticsHelper.BASE_STAT_COUNT + ": 7";
        assertEquals("Wrong summary statistics", summaryStatStr, _summaryStatisticsHelper.getSummaryStatisticFooterAsString(drt, statColumn));
        drt.clearSummaryStatistic(statColumn, SummaryStatisticsHelper.BASE_STAT_COUNT, "7");
        assertFalse("Summary statistic row still present", drt.hasSummaryStatisticRow());
        assertTextNotPresent("Oldness Factor");

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.saveCustomView("Saved-" + INJECT_CHARS_1);

        // TODO: pin, unpin, move columns/filters/sort, remove single filter clause

        log("** Test HTML/JavaScript escaping");
        Crawler.tryInject(this, () -> {
            _customizeViewsHelper.openCustomizeViewPanel();
            _customizeViewsHelper.saveCustomView("BAD" + Crawler.injectScriptBlock);
            assertTextBefore("Billson", "Johnson");
        });
    }

    @Test
    public void testSummaryStatisticsViaColumnHeader()
    {
        String statColumn1 = AGE_COLUMN;
        String statColumn2 = FIRST_NAME_COLUMN;

        setColumns(statColumn1, statColumn2);
        DataRegionTable drt = new DataRegionTable("query", getDriver());

        log("** Set summary statistics for " + statColumn1);
        drt.setSummaryStatistic(statColumn1, SummaryStatisticsHelper.BASE_STAT_SUM, "279");
        assertTrue("Summary statistic row didn't appear", drt.hasSummaryStatisticRow());
        String summaryStatStr = SummaryStatisticsHelper.BASE_STAT_SUM + ": 279";
        assertEquals("Wrong summary statistics", summaryStatStr, _summaryStatisticsHelper.getSummaryStatisticFooterAsString(drt, statColumn1));
        drt.setSummaryStatistic(statColumn1, SummaryStatisticsHelper.BASE_STAT_MEAN, "39.857");
        summaryStatStr = SummaryStatisticsHelper.BASE_STAT_SUM + ": 279 " + SummaryStatisticsHelper.BASE_STAT_MEAN + ": 39.857";
        assertEquals("Wrong summary statistics", summaryStatStr, _summaryStatisticsHelper.getSummaryStatisticFooterAsString(drt, statColumn1));

        log("** Set summary statistics for " + statColumn2);
        assertEquals("Wrong summary statistics", " ", _summaryStatisticsHelper.getSummaryStatisticFooterAsString(drt, statColumn2));
        drt.setSummaryStatistic(statColumn2, SummaryStatisticsHelper.BASE_STAT_COUNT, "7");
        summaryStatStr = SummaryStatisticsHelper.BASE_STAT_COUNT + ": 7";
        assertEquals("Wrong summary statistics", summaryStatStr, _summaryStatisticsHelper.getSummaryStatisticFooterAsString(drt, statColumn2));

        log("** Clear summary statistics for " + statColumn1);
        drt.clearSummaryStatistic(statColumn1, SummaryStatisticsHelper.BASE_STAT_SUM, "279");
        summaryStatStr = SummaryStatisticsHelper.BASE_STAT_MEAN + ": 39.857";
        assertEquals("Wrong summary statistics", summaryStatStr, _summaryStatisticsHelper.getSummaryStatisticFooterAsString(drt, statColumn1));
        drt.clearSummaryStatistic(statColumn1, SummaryStatisticsHelper.BASE_STAT_MEAN, "39.857");
        assertEquals("Wrong summary statistics", " ", _summaryStatisticsHelper.getSummaryStatisticFooterAsString(drt, statColumn1));

        log("** Clear summary statistics for " + statColumn2);
        drt.clearSummaryStatistic(statColumn2, SummaryStatisticsHelper.BASE_STAT_COUNT, "7");
        assertFalse("Summary statistic row shouldn't appear", drt.hasSummaryStatisticRow());
    }

    @Test
    public void verifySummaryStatisticsByColumnType()
    {
        // PK should not have mean and sum
        setColumns(LIST_KEY_COLUMN);
        _summaryStatisticsHelper.verifySummaryStatisticsDialog(LIST_KEY_COLUMN, "integer", false, true);

        // String column should only have count
        setColumns(FIRST_NAME_COLUMN);
        _summaryStatisticsHelper.verifySummaryStatisticsDialog(FIRST_NAME_COLUMN, "string");

        // Integer column should have all
        setColumns(AGE_COLUMN);
        _summaryStatisticsHelper.verifySummaryStatisticsDialog(AGE_COLUMN, "integer");

        // Date column should not have mean and sum
        setColumns(TEST_DATE_COLUMN);
        _summaryStatisticsHelper.verifySummaryStatisticsDialog(TEST_DATE_COLUMN, "date");

        // Folder column should only have count
        setColumns("container");
        _summaryStatisticsHelper.verifySummaryStatisticsDialog("container", "string");

        // Lookup column should only have count
        setColumns("CreatedBy");
        _summaryStatisticsHelper.verifySummaryStatisticsDialog("CreatedBy", "integer", true, false);
    }

    @Test
    public void testRemoveViaColumnHeader()
    {
        setColumns(FIRST_NAME_COLUMN, LAST_NAME_COLUMN);

        DataRegionTable drt = new DataRegionTable("query", getDriver());

        // remove the first column and verify that it is gone
        assertThat(drt.getColumnNames(), hasItem(FIRST_NAME_COLUMN));
        drt.removeColumn(FIRST_NAME_COLUMN);
        assertThat(drt.getColumnNames(), CoreMatchers.not(hasItem(FIRST_NAME_COLUMN)));

        // shouldn't be allowed to remove last column
        assertThat(drt.getColumnLabels(), hasItem("Last Name"));
        drt.removeColumn(FieldKey.fromParts(LAST_NAME_COLUMN).toString(), true);
        assertThat(drt.getColumnLabels(), hasItem("Last Name"));
    }

    @Test
    public void testFilteringAndSorting()
    {
        log("** Show only LastName and Age");
        setColumns(LAST_NAME_COLUMN, AGE_COLUMN);
        assertTextPresent("Norbertson");
        assertTextNotPresent("First Name");

        log("test js injection attack (Issue 14103) ");
        addFilter(FIRST_NAME_COLUMN, "Starts With", "K");
        removeFilter(FIRST_NAME_COLUMN);

        log("** Add filter: LastName starts with 'J'");
        addFilter(LAST_NAME_COLUMN, "Starts With", "J");
        assertTextNotPresent("Norbertson");
        assertTextPresent("Janeson", "Johnson");

        log("** Add another filter: LastName != 'Johnson'");
        addFilter(LAST_NAME_COLUMN, "Does Not Equal", "Johnson");
        assertTextPresent("Janeson");
        assertElementNotPresent(Locator.tagContainingText("td", "Johnson"));

        log("** Remove filter");
        removeFilter(LAST_NAME_COLUMN);
        assertTextPresent("Johnson", "Norbertson");

        log("** Add sort by Age");
        assertTextBefore("Billson", "Johnson");
        addSort(AGE_COLUMN, SortDirection.ASC);
        assertTextBefore("Johnson", "Billson");

        log("** Remove sort");
        removeSort(AGE_COLUMN);
        assertTextBefore("Billson", "Johnson");
    }

    @Test
    public void testSaveAfterApplyingView()
    {
        saveAfterApplyingView(null, "CreatedBy", "Created By");
        saveAfterApplyingView("New View", "ModifiedBy", "Modified By");
    }

    //Issue 13099: Unable to save custom view after applying view
    private void saveAfterApplyingView(String name, String newColumnLabel, String newColumnDisplayName)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn(newColumnLabel);
        _customizeViewsHelper.applyCustomView();
        assertTextPresent(newColumnDisplayName, "unsaved");

        _customizeViewsHelper.revertUnsavedViewGridClosed();
        assertTextNotPresent(newColumnDisplayName);

        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn(newColumnLabel);
        _customizeViewsHelper.applyCustomView();
        _customizeViewsHelper.saveUnsavedViewGridClosed(name);
        assertTextNotPresent("unsaved");
        assertTextPresent(newColumnDisplayName);
    }

    //Issue 12577: Save link in view/filter bar doesn't work
    //Issue 12103: Report names appear in random order on Views menu
    @Test
    public void saveFilterTest()
    {
        String fieldKey = FieldKey.fromParts(LAST_NAME_COLUMN).toString();
        String op = "Starts With";
        String value = "J";
        String[] viewNames = {TRICKY_CHARACTERS + "view", "AAC", "aaa", "aad", "zzz"};

        setColumns(fieldKey);
        for(String name : viewNames)
        {
            _customizeViewsHelper.openCustomizeViewPanel();
            _customizeViewsHelper.addFilter(fieldKey, fieldKey, op, value);
            _customizeViewsHelper.saveCustomView(name);
        }

        DataRegionTable drt = new DataRegionTable("query", getDriver());
        drt.goToView("Default");
        drt.getViewsMenu().expand();
        assertTextPresentInThisOrder("Default", viewNames[0], viewNames[2], viewNames[1], viewNames[3], viewNames[4]);
    }

    private void createList() throws Exception
    {
        ListDefinition listDefinition = new IntListDefinition(LIST_NAME, LIST_KEY_COLUMN).setFields(LIST_COLUMNS);
        TestDataGenerator testDataGenerator = listDefinition.create(createDefaultConnection(), PROJECT_NAME);

        for (String[] rowData : TEST_DATA)
        {
            testDataGenerator.addCustomRow(Map.of(
                    LIST_KEY_COLUMN, rowData[0],
                    LIST_COLUMNS.get(0).getName(), rowData[1],
                    LIST_COLUMNS.get(1).getName(), rowData[2],
                    LIST_COLUMNS.get(2).getName(), rowData[3],
                    LIST_COLUMNS.get(3).getName(), rowData[4]));
        }

        testDataGenerator.insertRows(createDefaultConnection());
    }

    private void setColumns(String... columnNames)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.clearColumns();
        for (String columnName : columnNames)
            _customizeViewsHelper.addColumn(FieldKey.fromParts(columnName).toString());
        _customizeViewsHelper.applyCustomView();
    }

    private void addFilter(String columnName, String op, String value)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addFilter(FieldKey.fromParts(columnName).toString(), columnName, op, value);
        _customizeViewsHelper.applyCustomView();
    }

    private void addSort(String columnName, SortDirection order)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addSort(FieldKey.fromParts(columnName).toString(), order);
        _customizeViewsHelper.applyCustomView();
    }

    private void removeFilter(String columnName)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeFilter(FieldKey.fromParts(columnName).toString());
        _customizeViewsHelper.applyCustomView();
    }

    private void removeSort(String columnName)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeSort(FieldKey.fromParts(columnName).toString());
        _customizeViewsHelper.applyCustomView();
    }

    private void setColumnTitle(String columnName, String columnTitle)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.setColumnTitle(FieldKey.fromParts(columnName).toString(), columnTitle);
        _customizeViewsHelper.applyCustomView();
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }
}
