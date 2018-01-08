/*
 * Copyright (c) 2007-2017 LabKey Corporation
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
package org.labkey.test.util;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.components.ColumnChartRegion;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.PagingWidget;
import org.labkey.test.components.SummaryStatisticsDialog;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.study.DatasetFacetPanel;
import org.labkey.test.selenium.RefindingWebElement;
import org.labkey.test.selenium.WebElementWrapper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.springframework.util.LinkedCaseInsensitiveMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/**
 * Component wrapper class for interacting with a LabKey Data Region (see clientapi/dom/DataRegion.js)
 */
public class DataRegionTable extends DataRegion
{
    public static final String UPDATE_SIGNAL = DataRegion.UPDATE_SIGNAL;
    public static final String PANEL_SHOW_SIGNAL = DataRegion.PANEL_SHOW_SIGNAL;
    public static final String PANEL_HIDE_SIGNAL = DataRegion.PANEL_HIDE_SIGNAL;

    // Cached items
    private CustomizeView _customizeView;
    private DataRegionExportHelper _exportHelper;
    private PagingWidget _pagingWidget;
    private final List<String> _columnLabels = new ArrayList<>();
    private final List<String> _columnNames = new ArrayList<>();
    private final Map<String, Integer> _mapRows = new HashMap<>();
    private final Map<Integer, Map<Integer, String>> _dataCache = new TreeMap<>();

    /**
     * @param regionName 'lk-region-name' of the table
     */
    public DataRegionTable(String regionName, WebDriverWrapper driverWrapper)
    {
        super(Locators.dataRegion(regionName).refindWhenNeeded(driverWrapper.getDriver()).withTimeout(10000), driverWrapper);
        setRegionName(regionName);
    }

    protected DataRegionTable(WebElement table, WebDriver driver)
    {
        super(table, new WebDriverWrapperImpl(driver));
    }

    public DataRegionTable(String regionName, WebDriver driver)
    {
        this(regionName, new WebDriverWrapperImpl(driver));
    }

    @Override
    public Elements elementCache()
    {
        return (Elements) super.elementCache();
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    protected void clearCache()
    {
        super.clearCache();
        _pagingWidget = null;
        _customizeView = null;
        _exportHelper = null;
        _columnLabels.clear();
        _columnNames.clear();
        _mapRows.clear();
        _dataCache.clear();
    }

    protected boolean hasSelectors()
    {
        return elementCache().hasSelectors();
    }

    public CustomizeView getCustomizeView()
    {
        if (_customizeView == null)
        {
            _customizeView = new CustomizeView(this);
        }
        return _customizeView;
    }

    public PagingWidget getPagingWidget()
    {
        if (_pagingWidget == null)
        {
            _pagingWidget = new PagingWidget(this);
        }
        return _pagingWidget;
    }

    public CustomizeView openCustomizeGrid()
    {
        if (!getCustomizeView().isPanelExpanded())
        {
            getWrapper().doAndWaitForPageSignal(() ->
                            getViewsMenu().clickSubMenu(false, "Customize Grid"),
                    DataRegion.PANEL_SHOW_SIGNAL);
        }
        return getCustomizeView();
    }

    public DataRegionTable closeCustomizeGrid()
    {
        getCustomizeView().closePanel();
        return this;
    }

    protected DataRegionExportHelper getExportPanel()
    {
        if (_exportHelper == null)
            _exportHelper = new DataRegionExportHelper(this);
        return _exportHelper;
    }

    public AbstractDataRegionExportOrSignHelper expandExportPanel()
    {
        return _exportHelper.expandExportPanel();
    }

    public static DataRegionFinder DataRegion(WebDriver driver)
    {
        return new DataRegionFinder(driver);
    }

    public static class DataRegionFinder extends WebDriverComponentFinder<DataRegionTable, DataRegionFinder>
    {
        private Locator _loc = Locators.dataRegion();
        private boolean _lazy = false;

        public DataRegionFinder(WebDriver driver)
        {
            super(driver);
            timeout(DEFAULT_WAIT);
        }

        public DataRegionFinder withName(String name)
        {
            _loc = Locators.dataRegion(name);
            return this;
        }

        @Override
        public DataRegionTable findWhenNeeded(SearchContext context)
        {
            try
            {
                _lazy = true;
                return super.findWhenNeeded(context);
            }
            finally
            {
                _lazy = false;
            }
        }

        @Override
        protected Locator locator()
        {
            return _loc;
        }

        @Override
        protected DataRegionTable construct(WebElement el, WebDriver driver)
        {
            DataRegionTable constructed = new DataRegionTable(new RefindingWebElement(el, buildLocator(), getContext()), driver);
            if (!_lazy)
                constructed.elementCache();
            return constructed;
        }
    }

    @Deprecated
    public static DataRegionTable findDataRegion(WebDriverWrapper test)
    {
        return DataRegion(test.getDriver()).find();
    }

    public static DataRegionTable findDataRegionWithinWebpart(WebDriverWrapper test, String webPartTitle)
    {
        return DataRegion(test.getDriver()).find(new RefindingWebElement(PortalHelper.Locators.webPart(webPartTitle), test.getDriver()));
    }

    /**
     * @deprecated Use {@link #getDataRegionName()}
     * Rename to better match terminology in DataRegion.js
     */
    @Deprecated
    public String getTableName()
    {
        return getDataRegionName();
    }

    public int getColumnCount()
    {
        return elementCache().getColumnHeaders().size() - (hasSelectors() ? 1 : 0);
    }

    public boolean hasSummaryStatisticRow()
    {
        return Locator.css("tr.labkey-col-total").findElements(getComponentElement()).size() > 0;
    }

    /**
     *
     * @return The count of rows being displayed
     */
    public int getDataRowCount()
    {
        return elementCache().getDataRows().size();
    }

    /**
     * Assert that a Data Region has expected pagination text (e.g. "1 - 3 of 16")
     * @param firstRow position in data of first displayed row
     * @param lastRow position in data of last displayed row
     * @param totalRows total number of rows in data behind the dataregion
     */
    public void assertPaginationText(int firstRow, int lastRow, int totalRows)
    {
        String expected = firstRow + " - " + lastRow + " of " + totalRows;
        String fullPaginationText = Locator.css(".labkey-pagination")
                .findElement(elementCache().getButtonBar()).getText();

        Pattern pattern = Pattern.compile("\\d+ - \\d+ of \\d+");
        Matcher matcher = pattern.matcher(fullPaginationText);
        assertTrue("Unable to parse pagination text: " + fullPaginationText, matcher.find());
        String actual = matcher.group(0);
        assertEquals("Wrong pagination text", expected, actual);
    }

    /**
     * @deprecated Renamed. Use {@link #getRowIndex(String, String)}
     */
    @Deprecated
    public int getRow(String columnLabel, String value)
    {
        return getRowIndex(columnLabel, value);
    }

    public int getRowIndex(String columnLabel, String value)
    {
        return getRowIndex(getColumnIndexStrict(columnLabel), value);
    }

    public int getRowIndex(int columnIndex, String value)
    {
        int rowCount = getDataRowCount();
        for (int i=0; i < rowCount; i++)
        {
            if (value.equals(getDataAsText(i, columnIndex)))
                return i;
        }
        return -1;
    }

    protected int getRowIndexStrict(String columnLabel, String value)
    {
        int rowIndex = getRowIndex(columnLabel, value);
        if (rowIndex < 0)
            throw new NoSuchElementException("No row where: " + columnLabel + "=" + value);
        return rowIndex;
    }

    protected int getRowIndexStrict(int columnIndex, String value)
    {
        int rowIndex = getRowIndex(columnIndex, value);
        if (rowIndex < 0)
            throw new NoSuchElementException("No row where: " + elementCache().getColumnHeaders().get(columnIndex).getText() + " = " + value);
        return rowIndex;
    }

    public String getSummaryStatFooterText(String columnLabel)
    {
        return getSummaryStatFooterText(getColumnIndexStrict(columnLabel));
    }

    public String getSummaryStatFooterText(int columnIndex)
    {
        columnIndex += hasSelectors() ? 1 : 0;
        String footerText = elementCache().getSummaryStatisticCells().get(columnIndex).getText();
        return footerText != null ? footerText.replaceAll("\\?", "") : null;
    }

    /**
     * do nothing if column is already present, add it if it is not
     * @param columnName   name of column to add, if necessary
     */
    public void ensureColumnPresent(String columnName)
    {
        ensureColumnsPresent(columnName);
    }

    /**
     * check for presence of columns, add them if they are not already present
     * requires columns actually exist
     * @param names names of columns to add
     */
    public void ensureColumnsPresent(String... names)
    {
        boolean opened = false;
        for (String name: names)
        {
            if (getColumnIndex(name) == -1)
            {
                if (!opened)
                {
                    getCustomizeView().openCustomizeViewPanel();
                    opened = true;
                }
                getCustomizeView().addColumn(name);
            }
        }
        if (opened)
            getCustomizeView().applyCustomView();
    }

    /**
     * returns index of the row of the first appearance of the specified data, in the specified column
     */
    public int getIndexWhereDataAppears(String data, String column)
    {
        List<String> allData = getColumnDataAsText(column);
        return allData.indexOf(data);
    }

    public static Locator.XPathLocator detailsLinkLocator()
    {
        return Locator.tagWithClass("td", "labkey-selectors")
                .child("a").withAttribute("data-original-title", "details");
    }

    public WebElement detailsLink(int row)
    {
        return detailsLinkLocator().findElement(elementCache().getDataRow(row));
    }

    public static Locator.XPathLocator updateLinkLocator()
    {
        return Locator.tagWithClass("td","labkey-selectors")
                .child("a").withAttribute("data-original-title", "edit");
    }

    /**
     * @deprecated Use {@link #clickEditRow(String)} or {@link #clickEditRow(int)}
     */
    @Deprecated
    public WebElement updateLink(int row)
    {
        return updateLinkLocator().findElement(elementCache().getDataRow(row));
    }

    public WebElement link(int row, int col)
    {
        col += hasSelectors() ? 1 : 0;
        final WebElement cell = elementCache().getCell(row, col);
        final WebElement link = Locator.xpath("a").findElement(cell);
        return new WebElementWrapper()
        {
            @Override
            public WebElement getWrappedElement()
            {
                return link;
            }

            @Override
            public void click()
            {
                getWrapper().scrollIntoView(cell); // clicks sometimes no-op when at the very bottom of the window
                super.click();
            }
        };
    }

    public WebElement link(int row, String columnName)
    {
        return link(row, getColumnIndexStrict(columnName));
    }

    protected int getColumnIndexStrict(String name)
    {
        int columnIndex = getColumnIndex(name);
        if (columnIndex < 0)
            throw new NoSuchElementException("No column: " + name);
        return columnIndex;
    }

    /**
     * Get column index for
     * @param name or label for desired column
     * @return column index or -1 if column isn't present
     */
    public int getColumnIndex(String name)
    {
        int i = getColumnNames().indexOf(name);

        if (i < 0)
            i = getColumnNames().indexOf(name.replaceAll(" ", ""));

        if (i < 0)
        {
            List<String> columnLabelsWithoutSpaces = new ArrayList<>(getColumnLabels().size());
            getColumnLabels().stream().forEachOrdered(s -> columnLabelsWithoutSpaces.add(s.replaceAll(" ", "")));
            i = columnLabelsWithoutSpaces.indexOf(name.replaceAll(" ", ""));
        }

        if (i < 0)
            TestLogger.log("Column '" + name + "' not found");
        return i;
    }

    public List<String> getColumnLabels()
    {
        getComponentElement().isDisplayed(); // validate cached element

        if (_columnLabels.isEmpty())
        {
            _columnLabels.addAll(getWrapper().getTexts(elementCache().getColumnHeaders()));
            if (hasSelectors())
                _columnLabels.remove(0);
        }

        return ImmutableList.copyOf(_columnLabels);
    }

    public List<String> getColumnNames()
    {
        getComponentElement().isDisplayed(); // validate cached element

        if (_columnNames.isEmpty())
        {
            List<WebElement> columnHeaders = elementCache().getColumnHeaders();
            for (int i = hasSelectors() ? 1 : 0; i < columnHeaders.size(); i++)
            {
                String columnName = columnHeaders.get(i).getAttribute("column-name");
                if (columnName.startsWith(getDataRegionName() + ":"))
                    columnName = columnName.substring(getDataRegionName().length() + 1);
                _columnNames.add(columnName);
            }
        }

        return ImmutableList.copyOf(_columnNames);
    }

    @NotNull
    public List<String> getColumnDataAsText(int col)
    {
        int rowCount = getDataRowCount();
        List<String> columnText = new ArrayList<>();

        if (col >= 0)
        {
            for (int row = 0; row < rowCount; row++)
                columnText.add(getDataAsText(row, col));
        }

        return columnText;
    }

    public List<String> getColumnDataAsText(String name)
    {
        return getColumnDataAsText(getColumnIndexStrict(name));
    }

    public Map<String, String> getRowDataAsMap(String colName, String value)
    {
        return getRowDataAsMap(getRowIndexStrict(colName, value));
    }

    public Map<String, String> getRowDataAsMap(int row)
    {
        Map<String, String> rowMap = new LinkedCaseInsensitiveMap<>();
        for (String colName : getColumnNames())
        {
            rowMap.put(colName, getDataAsText(row, colName));
        }
        return rowMap;
    }

    public List<String> getRowDataAsText(int row)
    {
        final int colCount = getColumnCount();
        List<String> rowText = new ArrayList<>();

        for (int col = 0; col < colCount; col++)
        {
            rowText.add(getDataAsText(row, col));
        }

        return rowText;
    }

    public List<String> getRowDataAsText(int row, String... columns)
    {
        List<String> rowData = new ArrayList<>();
        for (String column : columns)
        {
            rowData.add(getDataAsText(row, column));
        }
        return rowData;
    }

    /**
     * Get values for all specified columns for all pages of the table
     * preconditions:  must be on start page of table
     * postconditions:  at start of table
     */
    public List<List<String>> getFullColumnValues(String... columnNames)
    {
        boolean moreThanOnePage = getWrapper().isElementPresent(Locator.linkWithText("Next"));

        if (moreThanOnePage)
        {
            showAll();
        }

        List<List<String>> columns = new ArrayList<>();
        for (String columnName : columnNames)
        {
            columns.add(getColumnDataAsText(columnName));
        }

        if (moreThanOnePage)
        {
            setPageSize(100);
        }

        return columns;
    }

    public List<List<String>> getRows(String...columnNames)
    {
        List<List<String>> fullColumnValues = getFullColumnValues(columnNames);
        return collateColumnsIntoRows(fullColumnValues);
    }

    @SafeVarargs
    public static List<List<String>> collateColumnsIntoRows(List<String>...columns)
    {
        return collateColumnsIntoRows(Arrays.asList(columns));
    }

    public static List<List<String>> collateColumnsIntoRows(List<List<String>> columns)
    {
        int rowCount = 0;
        for (int i = 0; i < columns.size() - 1; i++)
        {
            rowCount = columns.get(i).size();
            if (columns.get(i).size() != columns.get(i+1).size())
                throw new IllegalArgumentException("Columns not of equal sizes");
        }

        List<List<String>> rows = new ArrayList<>();

        for (int rowNum = 0; rowNum < rowCount; rowNum++)
        {
            List<String> row = new ArrayList<>(columns.size());
            for (List<String> column : columns)
            {
                row.add(column.get(rowNum));
            }
            rows.add(row);
        }

        return rows;
    }

    public WebElement findRow(int index)
    {
        return elementCache().getDataRow(index);
    }

    public int getRowIndex(String pk)
    {
        assertTrue("Need the selector checkboxes value to find row by pk", hasSelectors());

        getComponentElement().isDisplayed(); // refresh cache

        Integer cached = _mapRows.get(pk);
        if (cached != null)
            return cached.intValue();

        int row = 0;
        try
        {
            while (true)
            {
                String value = getWrapper().getAttribute(Locator.xpath("//table[@id=" + Locator.xq(getTableId()) +"]//tr[" + (row+1) + "]//input[@name='.select']"), "value");
                _mapRows.put(value, row);
                if (value.equals(pk))
                    return row;
                row += 1;
            }
        }
        catch (NoSuchElementException ignore)
        {
            // Throws an exception, if row is out of bounds.
        }

        return -1;
    }

    protected int getRowIndexStrict(String pk)
    {
        int rowIndex = getRowIndex(pk);
        if (rowIndex < 0)
            throw new NoSuchElementException("No row with primary key: " + pk);
        return rowIndex;
    }

    public WebElement findCell(int row, String column)
    {
        return findCell(row, getColumnIndexStrict(column));
    }

    public WebElement findCell(int row, int column)
    {
        column += hasSelectors() ? 1 : 0;
        return elementCache().getCell(row, column);
    }

    public String getDataAsText(int row, int column)
    {
        WebElement cell = findCell(row, column); // Will clear cache if needed

        if (_dataCache.get(row) == null)
            _dataCache.put(row, new TreeMap<>());
        if (_dataCache.get(row).get(column) == null)
            _dataCache.get(row).put(column, cell.getText());

        return _dataCache.get(row).get(column);
    }

    public String getDataAsText(int row, String columnName)
    {
        return getDataAsText(row, getColumnIndexStrict(columnName));
    }

    public String getDataAsText(String pk, String columnName)
    {
        return getDataAsText(getRowIndexStrict(pk), getColumnIndexStrict(columnName));
    }

    /* Key is the PK on the table, it is usually the contents of the 'value' attribute of the row selector checkbox  */
    public void updateRow(String key, Map<String, String> data)
    {
        updateRow(key, data, true);
    }

    /* Key is the PK on the table, it is usually the contents of the 'value' attribute of the row selector checkbox  */
    public void updateRow(String key, Map<String, String> data, boolean validateText)
    {
        clickEditRow(key);

        setRowData(data, validateText);
    }

    public void updateRow(int rowIndex, Map<String, String> data)
    {
        updateRow(rowIndex, data, true);
    }

    public void updateRow(int rowIndex, Map<String, String> data, boolean validateText)
    {
        clickEditRow(rowIndex);

        setRowData(data, validateText);
    }

    //todo: return edit page
    public void clickEditRow(int rowIndex)
    {
        WebElement updateLink = updateLink(rowIndex);
        getWrapper().fireEvent(updateLink, WebDriverWrapper.SeleniumEvent.mouseover);
        getWrapper().clickAndWait(updateLink);
    }

    //todo: return edit page
    public void clickEditRow(String key)
    {
        clickEditRow(getRowIndexStrict(key));
    }

    public void clickRowDetails(int rowIndex)
    {
        WebElement updateLink = detailsLink(rowIndex);
        getWrapper().fireEvent(updateLink, WebDriverWrapper.SeleniumEvent.mouseover);
        getWrapper().clickAndWait(detailsLink(rowIndex));
    }

    public void clickRowDetails(String key)
    {
        clickRowDetails(getRowIndexStrict(key));
    }


    protected void setRowData(Map<String, String> data, boolean validateText)
    {
        for (String key : data.keySet())
        {
            getWrapper().waitForElement(Locator.name("quf_" + key));
            WebElement field = Locator.name("quf_" + key).findElement(getDriver());
            String inputType = field.getAttribute("type");
            switch (inputType)
            {
                case "checkbox":
                    getWrapper().setCheckbox(field, data.get(key).toLowerCase().equals("true"));
                    break;
                case "file":
                    getWrapper().setFormElement(field, new File(data.get(key)));
                    break;
                default:
                    getWrapper().setFormElement(field, data.get(key));
            }
        }
        getWrapper().clickButton("Submit");

        if (validateText)
        {
            getWrapper().assertTextPresent(data.values().iterator().next());  //make sure some text from the map is present
        }
    }

    public String getDetailsHref(int row)
    {
        return detailsLink(row).getAttribute("href");
    }

    public String getUpdateHref(int row)
    {
        return updateLink(row).getAttribute("href");
    }

    public String getHref(int row, int column)
    {
        // headerRows and selector offsets are applied in link() locator
        return _getHref(row, column);
    }

    private String _getHref(int row, int column)
    {
        return link(row, column).getAttribute("href");
    }

    public String getHref(int row, String columnName)
    {
        return getHref(row, getColumnIndexStrict(columnName));
    }

    public boolean hasHref(int row, int column)
    {
        // headerRows and selector offsets are applied in link() locator
        return _hasHref(row, column);
    }

    private boolean _hasHref(int row, int column)
    {
        // Check the td cell is present, but has no link.
        WebElement cell = findCell(row, column);

        return !Locator.xpath("a").findElements(cell).isEmpty();
    }

    public boolean hasHref(int row, String columnName)
    {
        return hasHref(row, getColumnIndexStrict(columnName));
    }

    public ColumnChartRegion createBarChart(String columnName)
    {
        return createColumnChart(columnName, "Bar Chart");
    }

    public ColumnChartRegion createPieChart(String columnName)
    {
        return createColumnChart(columnName, "Pie Chart");
    }

    public ColumnChartRegion createBoxAndWhiskerChart(String columnName)
    {
        return createColumnChart(columnName, "Box & Whisker");
    }

    @LogMethod
    public ColumnChartRegion createColumnChart(String columnName, String chartType)
    {
        Locator cssPlotLocator = Locator.css("div.labkey-dataregion-msg-plot-analytic svg");
        int initialNumOfPlots = cssPlotLocator.findElements(this).size();

        clickColumnMenu(columnName, false, chartType);
        cssPlotLocator.index(initialNumOfPlots).waitForElement(this, 60000);

        return new ColumnChartRegion(this);
    }

    public ColumnChartRegion getColumnPlotRegion()
    {
        return new ColumnChartRegion(this);
    }

    public void setSort(String columnName, SortDirection direction)
    {
        getWrapper().log("Setting sort in " + getDataRegionName() + " for " + columnName + " to " + direction.toString());
        if (isAsync())
            doAndWaitForUpdate(() -> clickColumnMenu(columnName, false, "Sort " + (direction.equals(SortDirection.ASC) ? "Ascending" : "Descending")));
        else
            clickColumnMenu(columnName, true, "Sort " + (direction.equals(SortDirection.ASC) ? "Ascending" : "Descending"));
    }

    public void setSummaryStatistic(String columnName, String stat, @Nullable String expectedValue)
    {
        clickSummaryStatistic(columnName, stat, false, expectedValue);
    }

    public void clearSummaryStatistic(String columnName, String stat, @Nullable String expectedValue)
    {
        clickSummaryStatistic(columnName, stat, true, expectedValue);
    }

    private void clickSummaryStatistic(String columnName, String stat, boolean isExpectedToBeChecked, @Nullable String expectedValue)
    {
        String clearOrSet = isExpectedToBeChecked ? "Clearing" : "Setting";
        TestLogger.log(clearOrSet + " the " + stat + " summary statistic in " + getDataRegionName() + " for " + columnName);
        clickColumnMenu(columnName, false, "Summary Statistics...");

        SummaryStatisticsDialog statsWindow = new SummaryStatisticsDialog(getDriver());

        if (expectedValue != null)
            assertEquals("Stat value not as expected for: " + stat, expectedValue, statsWindow.getValue(stat));

        if (isExpectedToBeChecked)
            statsWindow.deselect(stat);
        else
            statsWindow.select(stat);

        statsWindow.apply();
    }

    public void removeColumn(String columnName)
    {
        removeColumn(columnName, false);
    }

    public void removeColumn(String columnName, boolean errorExpected)
    {
        TestLogger.log("Removing column " + columnName + " in " + getDataRegionName());
        clickColumnMenu(columnName, !errorExpected, "Remove Column");

        if (errorExpected)
        {
            Window removeError = new Window("Error", getDriver());
            assertTrue(removeError.getBody().contains("You must select at least one field to display in the grid."));
            removeError.clickButton("OK", true);
        }
    }

    public void clearSort(String columnName)
    {
        if (isAsync())
            doAndWaitForUpdate(() -> clickColumnMenu(columnName, false, "Clear Sort"));
        else
            clickColumnMenu(columnName, true, "Clear Sort");
    }

    public void openFilterDialog(String columnName)
    {
        String columnLabel = elementCache().getColumnHeader(columnName).getText();
        clickColumnMenu(columnName, false, "Filter...");

        final Locator.XPathLocator filterDialog = ExtHelper.Locators.window("Show Rows Where " + columnLabel + "...");
        getWrapper().waitForElement(filterDialog);

        WebDriverWrapper.waitFor(() -> getWrapper().isElementPresent(filterDialog.append(Locator.linkWithText("[All]")).notHidden()) ||
                        getWrapper().isElementPresent(filterDialog.append(Locator.tagWithId("input", "value_1").notHidden())),
                "Filter Dialog", WAIT_FOR_JAVASCRIPT);
        getWrapper()._extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
    }

    public void setFilter(String columnName, String filterType)
    {
        setFilter(columnName, filterType, null, BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void setFilter(String columnName, String filterType, @Nullable String filter)
    {
        setFilter(columnName, filterType, filter, BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void setFilter(String columnName, String filterType, @Nullable String filter, int waitMillis)
    {
        setFilter(columnName, filterType, filter, waitMillis, false);
    }

    public void setFilter(String columnName, String filterType, @Nullable String filter, int waitMillis, boolean isPHILoggingColumn)
    {
        setUpFilter(columnName, filterType, filter, isPHILoggingColumn);
        doAndWaitForUpdate(() -> getWrapper().clickButton("OK", waitMillis));
    }

    public void setFilter(String columnName, String filterType, @Nullable String filter, @Nullable String filter2Type, @Nullable String filter2)
    {
        setUpFilter(columnName, filterType, filter, filter2Type, filter2);
        doAndWaitForUpdate(() -> getWrapper().clickButton("OK", BaseWebDriverTest.WAIT_FOR_PAGE));
    }

    public void setFacetedFilter(String columnName, String... values)
    {
        setUpFacetedFilter(columnName, values);
        doAndWaitForUpdate(() -> getWrapper().clickButton("OK"));
    }

    public void setUpFilter(String columnName, String filterType, String filter)
    {
        setUpFilter(columnName, filterType, filter, false);
    }

    public void setUpFilter(String columnName, String filterType, String filter, boolean isPHILoggingColumn)
    {
        setUpFilter(columnName, filterType, filter, null, null, isPHILoggingColumn);
    }

    public void setUpFilter(String columnName, String filter1Type, @Nullable String filter1, @Nullable String filter2Type, @Nullable  String filter2)
    {
        setUpFilter(columnName, filter1Type, filter1, filter2Type, filter2, false);
    }

    public void setUpFilter(String columnName, String filter1Type, @Nullable String filter1, @Nullable String filter2Type, @Nullable  String filter2, boolean isPHILoggingColumn)
    {
        String log = "Setting filter in " + getDataRegionName() + " for " + columnName + " to " + filter1Type.toLowerCase() + (filter1 != null ? " " + filter1 : "");
        if (filter2Type != null)
        {
            log += " and " + filter2Type.toLowerCase() + (filter2 != null ? " " + filter2 : "");
        }
        TestLogger.log(log);

        openFilterDialog(columnName);

        if (getWrapper().isElementPresent(Locator.css("span.x-tab-strip-text").withText("Choose Values")))
        {
            TestLogger.log("Switching to advanced filter UI");

            if (isPHILoggingColumn)
                closePhiLoggingColumnMsg();

            getWrapper()._extHelper.clickExtTab("Choose Filters");
            getWrapper().waitForElement(Locator.xpath("//span[" + Locator.NOT_HIDDEN + " and text()='Filter Type:']"), WAIT_FOR_JAVASCRIPT);
        }

        //Select combo box item
        getWrapper()._extHelper.selectComboBoxItem("Filter Type:", filter1Type);

        if (filter1 != null && !filter1Type.contains("Blank"))
        {
            getWrapper().setFormElement(Locator.id("value_1"), filter1);
        }

        if (filter2Type != null && !filter2Type.contains("Blank"))
        {
            //Select combo box item
            getWrapper()._extHelper.selectComboBoxItem("and:", filter2Type);
            if (filter2 != null)
            {
                getWrapper().setFormElement(Locator.id("value_2"), filter2);
            }
        }
    }

    private void closePhiLoggingColumnMsg()
    {
        Window confirmWindow = Window.Window(getDriver()).withTitle("Error").waitFor();
        getWrapper().waitForText("Cannot choose values from a column that requires logging.");
        confirmWindow.clickButton("OK", 0);
        getWrapper()._ext4Helper.waitForMaskToDisappear();
    }

    public void setUpFacetedFilter(String columnName, String... values)
    {
        String log;
        if (values.length > 0)
        {
            log = "Setting filter in " + getDataRegionName() + " for " + columnName + " to one of: [";
            for (String v : values)
            {
                log += v + ", ";
            }
            log = log.substring(0, log.length() - 2) + "]";
        }
        else
        {
            log = "Clear filter in " + getDataRegionName() + " for " + columnName;
        }

        TestLogger.log(log);

        openFilterDialog(columnName);
        String columnLabel = elementCache().getColumnHeader(columnName).getText();

        WebDriverWrapper.sleep(500);

        // Clear selections.
        assertEquals("Faceted filter tab should be selected.", "Choose Values", getWrapper().getText(Locator.css(".x-tab-strip-active")));
        if (!getWrapper().isElementPresent(Locator.xpath("//div[contains(@class, 'x-grid3-hd-checker-on')]")))
        {
            getWrapper().click(Locator.linkWithText("[All]"));
        }
        getWrapper().click(Locator.linkWithText("[All]"));

        if (values.length > 1)
        {
            for (String v : values)
            {
                getWrapper().click(Locator.xpath(getWrapper()._extHelper.getExtDialogXPath("Show Rows Where " + columnLabel + "...") +
                        "//div[contains(@class,'x-grid3-row') and .//span[text()='" + v + "']]//div[@class='x-grid3-row-checker']"));
            }
        }
        else if (values.length == 1)
        {
            getWrapper().click(Locator.xpath(getWrapper()._extHelper.getExtDialogXPath("Show Rows Where " + columnLabel + "...")+
                    "//div[contains(@class,'x-grid3-row')]//span[text()='" + values[0] + "']"));
        }
    }

    public void clearFilter(String columnName)
    {
        clearFilter(columnName, BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void clearFilter(String columnName, int waitMillis)
    {
        TestLogger.log("Clearing filter in " + getDataRegionName() + " for " + columnName);

        Runnable clickClearFilter = () -> clickColumnMenu(columnName, false, "Clear Filter");

        if (waitMillis == 0)
            doAndWaitForUpdate(clickClearFilter);
        else
            getWrapper().doAndWaitForPageToLoad(clickClearFilter, waitMillis);

    }

    public void clearAllFilters()
    {
        // TODO: This should be updated to use the UI once the UX Refresh configures a "Clear all" mechanism again
        TestLogger.log("Clearing all filters in " + getDataRegionName());
        api().expectingRefresh().executeScript("clearAllFilters()");
    }

    public void clearAllFilters(String columnName)
    {
        clearAllFilters(columnName, false);
    }

    public void clearAllFilters(String columnName, boolean isPHILoggingColumn)
    {
        TestLogger.log("Clearing filter in " + getDataRegionName() + " for " + columnName);
        openFilterDialog(columnName);
        if (isPHILoggingColumn)
        {
            if (getWrapper().isElementPresent(Locator.css("span.x-tab-strip-text").withText("Choose Values")))
            {
                closePhiLoggingColumnMsg();
            }
        }
        doAndWaitForUpdate(() -> getWrapper().clickButton("Clear All Filters"));
    }

    public void clickColumnMenu(String columnName, boolean pageLoad, String... menuItems)
    {
        final WebElement menu = elementCache().getColumnHeader(columnName);
        getWrapper().scrollIntoView(menu);   // some columns will be scrolled out of view;
        new BootstrapMenu(getDriver(), menu)
                .clickSubMenu(pageLoad, menuItems);
    }

    public void openSelectionMenu()
    {
        WebElement firstColumnHeader = elementCache().getColumnHeaders().get(0);
        Locator.XPathLocator loc = Locator.tagWithClass("span", "dropdown-toggle");
        loc.findElement(firstColumnHeader).click();
    }

    public void showSelected()
    {
        if (!getDataRegionName().contains("'") && !getDataRegionName().contains(">") &&!getDataRegionName().contains("<"))
        {   // use API unless the table name contains illegal chars for script
            new DataRegionApiExpectingRefresh().executeScript("showSelected()");
        }
        else
        {
            // find the flyout menu toggle in the first column header
            openSelectionMenu();
            Locator.linkContainingText("Show Selected").waitForElement(getDriver(), 2000).click();
        }
    }

    public void checkAllOnPage()
    {
        WebElement toggle = elementCache().toggleAllOnPage;
        if (!toggle.isSelected())
            doAndWaitForUpdate(toggle::click);
    }

    public void uncheckAllOnPage()
    {
        WebElement toggle = elementCache().toggleAllOnPage;
        if (null != doAndWaitForUpdate(toggle::click))
            doAndWaitForUpdate(toggle::click);
    }

    // TODO: Inline usages so that check/uncheckAll can select all rows on paged data region
    public void checkAll()
    {
        checkAllOnPage();
    }

    public void uncheckAll()
    {
        uncheckAllOnPage();
    }

    public void checkCheckboxByPrimaryKey(Object value)
    {
        elementCache().getRowCheckbox(value).check();
    }

    public void uncheckCheckboxByPrimaryKey(Object value)
    {
        elementCache().getRowCheckbox(value).uncheck();
    }

    public void checkCheckbox(int index)
    {
        elementCache().getRowCheckbox(index).check();
    }

    public void uncheckCheckbox(int index)
    {
        elementCache().getRowCheckbox(index).uncheck();
    }

    public void pageFirst()
    {
        TestLogger.log("Clicking page first on data region '" + getDataRegionName() + "'");
        getPagingWidget().clickGoToFirst();
    }

    public void pageLast()
    {
        TestLogger.log("Clicking page last on data region '" + getDataRegionName() + "'");
        getPagingWidget().clickGoToLast();
    }

    public void pageNext()
    {
        TestLogger.log("Clicking page next on data region '" + getDataRegionName() + "'");
        doAndWaitForUpdate(getPagingWidget()::clickNextPage);
    }

    public void pagePrev()
    {
        TestLogger.log("Clicking page previous on data region '" + getDataRegionName() + "'");
        doAndWaitForUpdate(getPagingWidget()::clickPreviousPage);
    }

    public void clickDataRegionPageLink(String title)
    {
        String headerId = Locator.xq(getTableId() + "-header");
        getWrapper().clickAndWait(Locator.xpath("//table[@id=" + headerId + "]//div/a[@title='" + title + "']"));
    }

    public void showAll()
    {
        openSelectionMenu();
        Locator.linkContainingText("Show All").waitForElement(getDriver(), 2000).click();
    }

    public void setPageSize(int size)
    {
        setPageSize(size, false);
    }

    public void setPageSize(int size, boolean wait)
    {
        getPagingWidget().setPageSize(size, wait);
    }

    public void setContainerFilter(ContainerFilterType filterType)
    {
        getViewsMenu().clickSubMenu(true, "Folder Filter", filterType.getLabel());
    }

    /**
     * Set the current offset by manipulating the url rather than using the pagination buttons.
     */
    public void setOffset(int offset)
    {
        String url = replaceParameter(getDataRegionName() + ".offset", String.valueOf(offset));
        getWrapper().beginAt(url);
    }

    /**
     * Set the page size by manipulating the url rather than using the "XXX per page" menu items.
     * @param size new page size
     */
    public void setMaxRows(int size)
    {
        String url = replaceParameter(getDataRegionName() + ".maxRows", String.valueOf(size));
        getWrapper().beginAt(url);
    }

    @LogMethod
    public void createQuickChart(String columnName)
    {
        clickColumnMenu(columnName, true, "Quick Chart");
        getWrapper().waitForElement(Locator.css("svg"));
    }

    // ======== Side facet panel =========

    public DatasetFacetPanel openSideFilterPanel()
    {
        if (!getWrapper().isElementPresent(DatasetFacetPanel.Locators.expandedFacetPanel(getDataRegionName())))
        {
            clickHeaderButton("Filter");
            getWrapper().waitForElement(Locator.css(".lk-filter-panel-label"));
        }
        WebElement panelEl = getWrapper().waitForElement(DatasetFacetPanel.Locators.expandedFacetPanel(getDataRegionName()));
        return new DatasetFacetPanel(panelEl, this);
    }

    public void clickExportButton()
    {
        elementCache().getExportButton().click();
    }

    public boolean columnHasChartOption(String columnName, String chartType)
    {
        BootstrapMenu menu = new BootstrapMenu(getDriver(), elementCache().getColumnHeader(columnName));
        menu.expand();
        return menu.findVisibleMenuItems()
                .stream()
                .anyMatch((m)-> m.getText().equalsIgnoreCase(chartType));
    }

    @Deprecated // use clickInsertNewRow()
    public void clickInsertNewRowDropdown()
    {
        clickInsertNewRow();
    }

    /* sometimes 'insert new row' is a top-level button, other times it's a dropdown
    *  under an 'insert data' top-level button. This handles either case. */
    public void clickInsertNewRow()
    {
        if (hasHeaderMenu("Insert data"))
            clickHeaderMenu("Insert data", getInsertNewButtonText());
        else
            clickHeaderButton(getInsertNewButtonText());
    }

    public void clickImportBulkData()
    {
        if (hasHeaderMenu("Insert data"))
            clickHeaderMenu("Insert data", getImportBulkDataText());
        else
            clickHeaderButton(getInsertNewButtonText());
    }

    public void clickDeleteAllButton()
    {
        elementCache().getHeaderButton("Delete All Rows").click();
        getWrapper().waitAndClick(Locator.linkWithText("Yes"));  //Confirmation popup
        getWrapper().waitAndClick(Locator.linkWithText("Ok"));  //results popup
    }

    public static String getInsertNewButtonText()
    {
        return "Insert new row";
    }

    public static String getImportBulkDataText()
    {
        return "Import bulk data";
    }

    public void clickApplyGridFilter()
    {
        goToView("Apply Grid Filter");
    }

    public static class Locators extends DataRegion.Locators
    {
        public static Locator.XPathLocator dataRegion()
        {
            return form();
        }

        public static Locator.XPathLocator dataRegion(String regionName)
        {
            return form(regionName);
        }

        public static Locator.XPathLocator dataRegionTable()
        {
            return form().withAttributeMatchingOtherElementAttribute("lk-region-form", Locator.xpath(".//table"), "lk-region-name");
        }

        public static Locator.XPathLocator dataRegionTable(String regionName)
        {
            return form(regionName).withDescendant(table(regionName));
        }

        public static Locator.XPathLocator table()
        {
            return Locator.tag("table").withAttribute("lk-region-name");
        }

        public static Locator.XPathLocator table(String regionName)
        {
            return Locator.tagWithAttribute("table", "lk-region-name", regionName);
        }

        public static Locator.XPathLocator facetRow(String category)
        {
            return Locator.xpath("//div").withClass("x4-grid-body")
                    .withPredicate(Locator.xpath("//div").withClass("lk-filter-panel-label")
                            .withText(category));
        }

        public static Locator.XPathLocator facetRowCheckbox(String category)
        {
            return facetRow(category).append(Locator.tag("div").withClass("x4-grid-row-checker"));
        }

        public static Locator.XPathLocator columnHeader(String regionName, String fieldName)
        {
            return Locator.tagWithAttribute("th", "column-name", regionName + ":" + fieldName);
        }

        public static Locator.XPathLocator floatingHeader()
        {
            return Locator.tagWithClass("tr", "labkey-col-header-row").attributeEndsWith("id", "-float");
        }
    }

    public class Elements extends ElementCache
    {
        private Boolean _selectors;
        protected boolean hasSelectors()
        {
            if (_selectors == null)
            {
                _selectors = Locator.css(".labkey-selectors").findElementOrNull(this) != null;
            }
            return _selectors;
        }

        private final WebElement columnHeaderRow = Locator.id(getTableId() + "-column-header-row").findWhenNeeded(this);
        private List<WebElement> columnHeaders;
        private final Map<String, WebElement> columnHeadersByName = new CaseInsensitiveHashMap<>();
        private List<WebElement> rows;
        private Map<Integer, List<WebElement>> cells;
        private final WebElement summaryStatRow = Locator.css("#" + getTableId() + " > tbody > tr.labkey-col-total").findWhenNeeded(getDriver());
        private List<WebElement> summaryStatCells;
        private final WebElement toggleAllOnPage = Locator.tagWithAttribute("input", "name", ".toggle").findWhenNeeded(this); // tri-state checkbox

        protected List<WebElement> getDataRows()
        {
            if (rows == null)
                rows = ImmutableList.copyOf(Locator.css(""+
                        "#" + getTableId() + " > tbody > tr.labkey-alternate-row:not(.labkey-col-total)," +
                        "#" + getTableId() + " > tbody > tr.labkey-row:not(.labkey-col-total)," +
                        "#" + getTableId() + " > tbody > tr.labkey-error-row:not(.labkey-col-total)").findElements(getDriver()));
            return rows;
        }

        protected WebElement getDataRow(int row)
        {
            return getDataRows().get(row);
        }

        protected Checkbox getRowCheckbox(int index)
        {
            return getRowCheckbox(Locator.css(".labkey-selectors > input[type=checkbox][value]").index(index).findElement(this));
        }

        protected Checkbox getRowCheckbox(Object pk)
        {
            return getRowCheckbox(Locator.css(".labkey-selectors > input[type=checkbox][value=" + Locator.cq(String.valueOf(pk)) + "]").findElement(this));
        }

        private Checkbox getRowCheckbox(WebElement checkboxEl)
        {
            return new Checkbox(checkboxEl)
            {
                @Override
                public void toggle()
                {
                    doAndWaitForUpdate(() -> {
                        try
                        {
                            super.toggle();
                        }
                        catch (WebDriverException e)
                        {
                            if (e.getMessage().contains("Other element would receive the click: <div class=\"labkey-button-bar\">"))
                            {
                                // The checkbox obscured by the floating header
                                getWrapper().scrollIntoView(getComponentElement());
                                super.toggle();
                            }
                        }
                    });
                }
            };
        }

        protected List<WebElement> getCells(int row)
        {
            if (cells == null)
                cells = new TreeMap<>();
            if (cells.get(row) == null)
                cells.put(row, ImmutableList.copyOf(Locator.xpath("td").findElements(getDataRow(row))));
            return cells.get(row);
        }

        protected WebElement getCell(int row, int col)
        {
            return getCells(row).get(col);
        }

        protected List<WebElement> getColumn(int col)
        {
            List<WebElement> columnCells = new ArrayList<>();
            for (int row = 0; row < getDataRows().size(); row++)
                columnCells.add(getCell(row, col));
            return columnCells;
        }

        protected List<WebElement> getSummaryStatisticCells()
        {
            if (summaryStatCells == null)
                summaryStatCells = ImmutableList.copyOf(Locator.xpath("td").findElements(summaryStatRow));

            return summaryStatCells;
        }

        protected WebElement getColumnHeader(String colName)
        {
            if (!columnHeadersByName.containsKey(colName))
            {
                columnHeadersByName.put(colName, Locator.findAnyElement("Column header named " + colName, this,
                        Locators.columnHeader(getDataRegionName(), colName),
                        Locators.columnHeader(getDataRegionName(), colName.toLowerCase())));
            }
            return columnHeadersByName.get(colName);
        }

        protected List<WebElement> getColumnHeaders()
        {
            if (columnHeaders == null)
                columnHeaders = ImmutableList.copyOf(Locator.css("th.labkey-column-header").findElements(columnHeaderRow));
            return columnHeaders;
        }

        protected WebElement getExportButton()
        {
            WebElement button = getHeaderButtonOrNull("Export");
            if (null != button)
                return button;
            return getHeaderButton("Export / Sign Data");
        }
    }

    public enum ContainerFilterType
    {
        CURRENT_FOLDER("Current folder"),
        CURRENT_AND_SUBFOLDERS("Current folder and subfolders"),
        ALL_FOLDERS("All folders");

        private final String _label;

        ContainerFilterType(String label)
        {
            _label = label;
        }

        public String getLabel()
        {
            return _label;
        }
    }
}
