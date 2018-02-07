/*
 * Copyright (c) 2010-2017 LabKey Corporation
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.categories.DailyB;
import org.labkey.test.util.Crawler;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.SummaryStatisticsHelper;

import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({DailyB.class})
public class CustomizeViewTest extends BaseWebDriverTest
{
    public static final String PROJECT_NAME = "CustomizeViewTest";
    public static final String LIST_NAME = "People" + INJECT_CHARS_1;
    private final static ListHelper.ListColumnType LIST_KEY_TYPE = ListHelper.ListColumnType.AutoInteger;
    private final static String LIST_KEY_COLUMN = "Key";
    private final static String LAST_NAME_COLUMN = "LastName" + INJECT_CHARS_2;
    private final static String FIRST_NAME_COLUMN = "FirstName";
    private final static String AGE_COLUMN = "Age";
    private final static String TEST_DATE_COLUMN = "TestDate";
    private final static ListHelper.ListColumn[] LIST_COLUMNS = new ListHelper.ListColumn[]
            {
                    new ListHelper.ListColumn(FIRST_NAME_COLUMN, FIRST_NAME_COLUMN + INJECT_CHARS_1, ListHelper.ListColumnType.String, "The first name"),
                    new ListHelper.ListColumn(LAST_NAME_COLUMN, "Last Name", ListHelper.ListColumnType.String, "The last name"),
                    new ListHelper.ListColumn(AGE_COLUMN, "Age", ListHelper.ListColumnType.Integer, "The age" + INJECT_CHARS_1),
                    new ListHelper.ListColumn(TEST_DATE_COLUMN, "Test Date", ListHelper.ListColumnType.DateTime, "The test date")
            };

    static
    {
        LIST_COLUMNS[0].setRequired(true);
        LIST_COLUMNS[1].setRequired(true);
    }

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
    public static void setupProject()
    {
        CustomizeViewTest init = (CustomizeViewTest) getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _containerHelper.createProject(PROJECT_NAME, null);
        createList();
    }

    @Before
    public void preTest()
    {
        _summaryStatisticsHelper = new SummaryStatisticsHelper(this);

        goToProjectHome();
        clickAndWait(Locator.linkContainingText(LIST_NAME));
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
        Crawler.tryInject(this, new Function<Void, Void>()
        {
            @Override
            public Void apply(Void v)
            {
                _customizeViewsHelper.openCustomizeViewPanel();
                _customizeViewsHelper.saveCustomView("EVIL: " + Crawler.injectString);
                assertTextBefore("Billson", "Johnson");
                return null;
            }
        }, null);
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
        assertTrue(drt.getColumnNames().contains(FIRST_NAME_COLUMN));
        drt.removeColumn(FIRST_NAME_COLUMN);
        assertTrue(!drt.getColumnNames().contains(FIRST_NAME_COLUMN));

        // shouldn't be allowed to remove last column
        assertTrue(drt.getColumnNames().contains(LAST_NAME_COLUMN));
        drt.removeColumn(LAST_NAME_COLUMN, true);
        assertTrue(drt.getColumnNames().contains(LAST_NAME_COLUMN));
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
        String fieldKey = LAST_NAME_COLUMN;
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
        drt.goToView("default");
        drt.getViewsMenu().expand();
        assertTextPresentInThisOrder("default", viewNames[0], viewNames[2], viewNames[1], viewNames[3], viewNames[4]);
    }

    private void createList()
    {
        _listHelper.createList(PROJECT_NAME, LIST_NAME, LIST_KEY_TYPE, LIST_KEY_COLUMN, LIST_COLUMNS);

        StringBuilder data = new StringBuilder();
        data.append(LIST_KEY_COLUMN).append("\t");
        for (int i = 0; i < LIST_COLUMNS.length; i++)
        {
            data.append(LIST_COLUMNS[i].getName());
            data.append(i < LIST_COLUMNS.length - 1 ? "\t" : "\n");
        }
        for (String[] rowData : TEST_DATA)
        {
            for (int col = 0; col < rowData.length; col++)
            {
                data.append(rowData[col]);
                data.append(col < rowData.length - 1 ? "\t" : "\n");
            }
        }

        _listHelper.clickImportData();
        _listHelper.submitTsvData(data.toString());
        for (String[] rowData : TEST_DATA)
        {
            // check that all the data is in the grid (skipping the key column at index 0)
            for (int col = 1; col < rowData.length; col++)
            {
                waitForText(rowData[col]);
            }
        }
    }

    private void setColumns(String... fieldKeys)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.showHiddenItems();
        _customizeViewsHelper.clearColumns();
        for (String fieldKey : fieldKeys)
            _customizeViewsHelper.addColumn(fieldKey);
        _customizeViewsHelper.applyCustomView();
    }

    private void addFilter(String fieldKey, String op, String value)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addFilter(fieldKey, fieldKey, op, value);
        _customizeViewsHelper.applyCustomView();
    }

    private void addSort(String fieldKey, SortDirection order)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addSort(fieldKey, order);
        _customizeViewsHelper.applyCustomView();
    }

    private void removeFilter(String fieldKey)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeFilter(fieldKey);
        _customizeViewsHelper.applyCustomView();
    }

    private void removeSort(String fieldKey)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.removeSort(fieldKey);
        _customizeViewsHelper.applyCustomView();
    }

    private void setColumnTitle(String fieldKey, String columnTitle)
    {
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.setColumnTitle(fieldKey, columnTitle);
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
