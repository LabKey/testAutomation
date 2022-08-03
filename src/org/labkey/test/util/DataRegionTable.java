/*
 * Copyright (c) 2008-2021 LabKey Corporation
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
import org.labkey.test.components.MessagePrompt;
import org.labkey.test.components.PagingWidget;
import org.labkey.test.components.SummaryStatisticsDialog;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.labkey.LabKeyAlert;
import org.labkey.test.components.study.DatasetFacetPanel;
import org.labkey.test.components.study.ViewPreferencesPage;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.TimeChartWizard;
import org.labkey.test.pages.query.UpdateQueryRowPage;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.selenium.RefindingWebElement;
import org.labkey.test.selenium.WebElementDecorator;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.Locator.tagWithAttribute;
import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.sleep;

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
    private final Map<String, Integer> _columnIndexMap = new CaseInsensitiveHashMap<>();
    private final Map<String, Integer> _mapRows = new HashMap<>();
    private final Map<Integer, Map<Integer, String>> _dataCache = new TreeMap<>();

    /**
     * @param regionName 'lk-region-name' of the table
     */
    public DataRegionTable(String regionName, WebDriverWrapper driverWrapper)
    {
        super(Locators.dataRegion(regionName).refindWhenNeeded(driverWrapper.getDriver()).withTimeout(DEFAULT_WAIT_MS), driverWrapper);
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

    @Override
    protected void clearCache()
    {
        super.clearCache();
        _pagingWidget = null;
        _customizeView = null;
        _exportHelper = null;
        _columnLabels.clear();
        _columnNames.clear();
        _columnIndexMap.clear();
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

    public ViewPreferencesPage clicksetDefault()
    {
        if (!getCustomizeView().isPanelExpanded())
        {
            getViewsMenu().clickSubMenu(true, "Set Default");
        }
        return new ViewPreferencesPage(getDriver());
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

    protected DataRegionExportHelper getExportPanel()
    {
        if (_exportHelper == null)
            _exportHelper = new DataRegionExportHelper(this);
        return _exportHelper;
    }

    public DataRegionExportHelper expandExportPanel()
    {
        getExportPanel().expandExportPanel();
        return getExportPanel();
    }

    public static DataRegionFinder DataRegion(WebDriver driver)
    {
        return new DataRegionFinder(driver);
    }

    public static class DataRegionFinder extends WebDriverComponentFinder<DataRegionTable, DataRegionFinder>
    {
        private Locator _loc = Locators.dataRegion();

        public DataRegionFinder(WebDriver driver)
        {
            super(driver);
            timeout(DEFAULT_WAIT_MS);
        }

        public DataRegionFinder withName(String name)
        {
            _loc = Locators.dataRegion(name);
            return this;
        }

        @Override
        protected Locator locator()
        {
            return _loc;
        }

        @Override
        protected DataRegionTable construct(WebElement el, WebDriver driver)
        {
            boolean lazy = el instanceof LazyWebElement;
            if (buildLocator() != null && getContext() != null // Prevent NPE after using `DataRegionFinder.locatedBy(..)`
                    && ! (el instanceof RefindingWebElement)) // 'RefindingWebElement' prohibits nesting
            {
                // Numerous tests expect to be able to reuse 'DataRegionTable' instances between page loads
                el = new RefindingWebElement(el, buildLocator(), getContext()).withTimeout(getTimeout());
            }
            DataRegionTable constructed = new DataRegionTable(el, driver);
            constructed.setUpdateTimeout(getTimeout());
            if (!lazy)
            {
                constructed.elementCache();
            }
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

    public int getColumnCount()
    {
        return elementCache().getColumnHeaders().size() - (hasSelectors() ? 1 : 0);
    }

    public boolean hasSummaryStatisticRow()
    {
        return Locator.css("tr.labkey-col-total").findElements(elementCache()).size() > 0;
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
        String expected = String.format("%,d", firstRow) + " - " + String.format("%,d", lastRow) + " of " + String.format("%,d", totalRows);
        String fullPaginationText = Locator.css(".paging-widget > a")
                .findElement(elementCache().getButtonBar()).getText();

        assertEquals("Wrong pagination text", expected, fullPaginationText);
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

    public int getRowIndexStrict(String columnLabel, String value)
    {
        int rowIndex = getRowIndex(columnLabel, value);
        if (rowIndex < 0)
            throw new NoSuchElementException("No row where: " + columnLabel + "=" + value);
        return rowIndex;
    }

    public int getRowIndexStrict(int columnIndex, String value)
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
     *
     * @deprecated Use {@link #getRowIndex(String, String)} or {@link #getRowIndexStrict(String, String)}
     */
    @Deprecated
    public int getIndexWhereDataAppears(String data, String column)
    {
        return getRowIndex(column, data);
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
        return new WebElementDecorator(link)
        {
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
        if (_columnIndexMap.isEmpty())
        {
            final var columnNames = getColumnNames();
            for (int j = 0; j < columnNames.size(); j++)
            {
                _columnIndexMap.put(columnNames.get(j), j);
            }
        }
        int i = _columnIndexMap.getOrDefault(name, -1);

        // Try removing spaces from column name
        if (i < 0)
        {
            i = _columnIndexMap.getOrDefault(name.replace(" ", ""), -1);
            if (i >= 0)
            {
                TestLogger.warn(String.format("Unnecessary space in requested column name. " +
                        "Requested: \"%s\" Found: \"%s\"", name, getColumnNames().get(i)));
            }
        }

        // Try matching column label
        if (i < 0)
        {
            List<String> columnLabelsWithoutSpaces = new ArrayList<>(getColumnLabels().size());
            getColumnLabels().stream().forEach(s -> columnLabelsWithoutSpaces.add(s.replace(" ", "")));
            i = columnLabelsWithoutSpaces.indexOf(name.replace(" ", ""));
            if (i >= 0)
            {
                TestLogger.warn(String.format("Please reference columns by name instead of label. " +
                        "Requested column with label: \"%s\" Found column with name: \"%s\"", name, getColumnNames().get(i)));
            }
        }

        return i;
    }

    public List<String> getColumnLabels()
    {
        getComponentElement().isEnabled(); // validate cached element

        if (_columnLabels.isEmpty())
        {
            _columnLabels.addAll(getWrapper().getTexts(elementCache().getColumnHeaders()));
            if (hasSelectors())
                _columnLabels.remove(0);
        }

        return new ArrayList<>(_columnLabels);
    }

    public List<String> getColumnNames()
    {
        getComponentElement().isEnabled(); // validate cached element

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

        return new ArrayList<>(_columnNames);
    }

    public String getColumnTitle(String columnName)
    {
        WebElement columnHeader = elementCache().getColumnHeader(columnName);
        return columnHeader.getAttribute("title");
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
        Map<String, String> rowMap = new CaseInsensitiveHashMap<>();
        for (String colName : getColumnNames())
        {
            rowMap.put(colName, getDataAsText(row, colName));
        }
        return rowMap;
    }

    public List<Map<String, String>> getTableData()
    {
        List<Map<String, String>> dataRows = new ArrayList<>();
        for (int i=0; i<getDataRowCount(); i++)
        {
            dataRows.add(getRowDataAsMap(i));
        }
        return dataRows;
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

        getComponentElement().isEnabled(); // refresh cache

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
    public void updateRow(String key, Map<String, ?> data)
    {
        updateRow(key, data, true);
    }

    /* Key is the PK on the table, it is usually the contents of the 'value' attribute of the row selector checkbox  */
    public void updateRow(String key, Map<String, ?> data, boolean validateText)
    {
        UpdateQueryRowPage updatePage = clickEditRow(key);

        setRowData(updatePage, data, validateText);
    }

    public void updateRow(int rowIndex, Map<String, ?> data)
    {
        updateRow(rowIndex, data, true);
    }

    public void updateRow(int rowIndex, Map<String, ?> data, boolean validateText)
    {
        UpdateQueryRowPage updatePage = clickEditRow(rowIndex);

        setRowData(updatePage, data, validateText);
    }

    public UpdateQueryRowPage clickEditRow(int rowIndex)
    {
        WebElement updateLink = updateLink(rowIndex);
        getWrapper().fireEvent(updateLink, WebDriverWrapper.SeleniumEvent.mouseover);
        getWrapper().clickAndWait(updateLink);
        return new UpdateQueryRowPage(getDriver());
    }

    public UpdateQueryRowPage clickEditRow(String key)
    {
        return clickEditRow(getRowIndexStrict(key));
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

    protected void setRowData(UpdateQueryRowPage updatePage, Map<String, ?> data, boolean validateText)
    {
        updatePage.update(data);
        if (validateText)
        {
            getWrapper().assertTextPresent(String.valueOf(data.values().iterator().next()));  //make sure some text from the map is present
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

    public WebElement getFlag(int row, String columnName)
    {
        var cell = findCell(row, columnName);
        return tagWithAttribute("i", "flagid").findElement(cell);
    }

    public boolean isFlagEnabled(int row, String columnName)
    {
        var flag = getFlag(row, columnName);
        return isFlagEnabled(flag);
    }

    private boolean isFlagEnabled(WebElement flag)
    {
        return flag.getAttribute("class").contains("lk-flag-enabled");
    }

    public boolean isFlagDisabled(int row, String columnName)
    {
        var flag = getFlag(row, columnName);
        return isFlagDisabled(flag);
    }

    private boolean isFlagDisabled(WebElement flag)
    {
        return flag.getAttribute("class").contains("lk-flag-disabled");
    }

    /**
     * Get the flag value for the column or <code>null</code> if unset.
     */
    public String getFlagValue(int row, String columnName)
    {
        var flag = getFlag(row, columnName);
        return getFlagValue(flag);
    }

    private String getFlagValue(WebElement flag)
    {
        String title = flag.getAttribute("title");
        if (isFlagEnabled(flag))
        {
            return title;
        }
        else if (isFlagDisabled(flag))
        {
            assertEquals("Expect unset flag title to be 'Flag for review'", title, "Flag for review");
            return null;
        }
        throw new AssertionError("Expected flag class to be either 'lk-flag-enabled' or 'lk-flag-disabled'");
    }

    public void setFlagValueForSelectedRows(String columnName, String value)
    {
        int checkedCount = getCheckedCount();
        assertTrue(checkedCount > 0);

        var flag = getFlag(0, columnName);
        flag.click();

        var prompt = new MessagePrompt("Review", getDriver());
        assertThat(prompt.getBody(), containsString("Enter comment for " + checkedCount + " selected rows"));
        prompt.setValue(value).clickOK();
    }

    /**
     * Set the flag value for the column or <code>null</code> to clear the flag.
     */
    public void setFlagValue(int row, String columnName, String value)
    {
        var flag = getFlag(row, columnName);
        setFlagValue(flag, value);
    }

    private void setFlagValue(WebElement flag, String value)
    {
        flag.click();
        new MessagePrompt("Review", getDriver()).setValue(value).clickOK();
        assertEquals(value, getFlagValue(flag));
    }

    public void clearFlagValue(int row, String columnName)
    {
        setFlagValue(row, columnName, null);
    }

    /**
     * Clear all flag values on the grid, if any.
     */
    public void clearFlagValues()
    {
        List<WebElement> allFlags = Locator.tagWithAttribute("i", "flagid").findElements(elementCache());
        for (WebElement flag : allFlags)
        {
            if (isFlagEnabled(flag))
                setFlagValue(flag, null);
        }
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
        cssPlotLocator.index(initialNumOfPlots).waitForElement(this, getUpdateTimeout());

        return new ColumnChartRegion(this);
    }

    public ColumnChartRegion getColumnPlotRegion()
    {
        return new ColumnChartRegion(this);
    }

    public void setSort(String columnName, SortDirection direction)
    {
        getWrapper().log("Setting sort in " + getDataRegionName() + " for " + columnName + " to " + direction.toString());
        doAndWaitForUpdate(() -> clickColumnMenu(columnName, !isAsync(), "Sort " + (direction.equals(SortDirection.ASC) ? "Ascending" : "Descending")));
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

    public void verifySummaryStatisticValue(String columnName, String stat, String expectedValue, String filterDescription)
    {
        clickColumnMenu(columnName, false, "Summary Statistics...");
        SummaryStatisticsDialog statsWindow = new SummaryStatisticsDialog(getDriver());
        assertEquals("Stat value not as expected for " + stat + " with filter " + filterDescription, expectedValue, statsWindow.getValue(stat));
        statsWindow.cancel();
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
            // If Ext4 is not on the page when Remove Column is clicked, this error will show as a bootstrap modal (i.e. LabKeyAlert)
            LabKeyAlert alert = LabKeyAlert.getFinder(getDriver()).find();
            if (alert != null)
            {
                assertTrue(alert.getText().contains("You must select at least one field to display in the grid."));
                alert.accept();
            }
            else
            {
                Window removeError = new Window("Error", getDriver());
                assertTrue(removeError.getBody().contains("You must select at least one field to display in the grid."));
                removeError.clickButton("OK", true);
            }
        }
    }

    public void clearSort(String columnName)
    {
        doAndWaitForUpdate(() -> clickColumnMenu(columnName, !isAsync(), "Clear Sort"));
    }

    public WebElement openFilterDialog(String columnName)
    {
        String columnLabel = elementCache().getColumnHeader(columnName).getText();
        clickColumnMenu(columnName, false, "Filter...");

        final Locator.XPathLocator filterDialog = ExtHelper.Locators.window("Show Rows Where " + columnLabel + "...");
        WebElement filterDialogElement = getWrapper().waitForElement(filterDialog);

        WebDriverWrapper.waitFor(() -> getWrapper().isElementPresent(filterDialog.append(Locator.linkWithText("[All]")).notHidden()) ||
                        getWrapper().isElementPresent(filterDialog.append(Locator.tagWithId("input", "value_1").notHidden())),
                "Filter Dialog", WAIT_FOR_JAVASCRIPT);
        getWrapper()._extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);

        return filterDialogElement;
    }

    @LogMethod (quiet = true)
    public void setFilter(@LoggedParam String columnName, @LoggedParam String filterType)
    {
        setFilter(columnName, filterType, null);
    }

    @LogMethod (quiet = true)
    public void setFilter(@LoggedParam String columnName, @LoggedParam String filterType, @Nullable @LoggedParam String filter)
    {
        setUpFilter(columnName, filterType, filter);
        doAndWaitForUpdate(() -> getWrapper().clickButton("OK", isAsync() ? 0 : getUpdateTimeout()));
    }

    /**
     * @deprecated Use {@link #setUpdateTimeout(int)} and {@link #setAsync(boolean)} to specify expected behavior
     */
    @Deprecated
    @LogMethod (quiet = true)
    public void setFilter(@LoggedParam String columnName, @LoggedParam String filterType, @Nullable @LoggedParam String filter, int waitMillis)
    {
        setUpFilter(columnName, filterType, filter);
        doAndWaitForUpdate(() -> getWrapper().clickButton("OK", waitMillis));
    }

    @LogMethod (quiet = true)
    public void setFilter(@LoggedParam String columnName, @LoggedParam String filterType, @Nullable @LoggedParam String filter, @Nullable @LoggedParam String filter2Type, @Nullable @LoggedParam String filter2)
    {
        setUpFilter(columnName, filterType, filter, filter2Type, filter2);
        doAndWaitForUpdate(() -> getWrapper().clickButton("OK", isAsync() ? 0 : getUpdateTimeout()));
    }

    @LogMethod (quiet = true)
    public void setFacetedFilter(@LoggedParam String columnName, @LoggedParam String... values)
    {
        setUpFacetedFilter(columnName, values);
        doAndWaitForUpdate(() -> getWrapper().clickButton("OK", isAsync() ? 0 : getUpdateTimeout()));
    }

    public void setUpFilter(String columnName, String filterType, String filter)
    {
        setUpFilter(columnName, filterType, filter, null, null);
    }

    public void setUpFilter(String columnName, String filter1Type, @Nullable String filter1, @Nullable String filter2Type, @Nullable  String filter2)
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
            getWrapper()._extHelper.clickExtTab("Choose Filters");
            getWrapper().waitForElement(Locator.xpath("//span[" + Locator.NOT_HIDDEN + " and text()='Filter Type:']"), WAIT_FOR_JAVASCRIPT);
        }

        //Select combo box item
        sleep(1000);
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

    public void setUpFacetedFilter(String columnName, String... values)
    {
        if (values.length > 0)
        {
            TestLogger.log("Setting filter in " + getDataRegionName() + " for " + columnName + " to one of: [" + String.join(", ", values) + "]");
        }
        else
        {
            TestLogger.log("Clear filter in " + getDataRegionName() + " for " + columnName);
        }

        openFilterDialog(columnName);
        String columnLabel = elementCache().getColumnHeader(columnName).getText();

        sleep(500);

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
        clearFilter(columnName, isAsync() ? 0 : BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void clearFilter(String columnName, int waitMillis)
    {
        TestLogger.log("Clearing filter in " + getDataRegionName() + " for " + columnName);
        doAndWaitForUpdate(() -> clickColumnMenu(columnName, waitMillis > 0, "Clear Filter"));
    }

    public void clearAllFilters()
    {
        // TODO: This should be updated to use the UI once the UX Refresh configures a "Clear all" mechanism again
        TestLogger.log("Clearing all filters in " + getDataRegionName());
        api().expectingRefresh().executeScript("clearAllFilters()");
    }

    public void clearAllFilters(String columnName)
    {
        TestLogger.log("Clearing filter in " + getDataRegionName() + " for " + columnName);
        openFilterDialog(columnName);
        doAndWaitForUpdate(() -> getWrapper().clickButton("Clear All Filters", isAsync() ? 0 : getUpdateTimeout()));
    }

    public void clickColumnMenu(String columnName, boolean pageLoad, String... menuItems)
    {
        final WebElement menu = elementCache().getColumnHeader(columnName);
        getWrapper().scrollIntoView(menu);   // some columns will be scrolled out of view;
        new BootstrapMenu(getDriver(), menu)
                .clickSubMenu(pageLoad ? getUpdateTimeout() : 0, menuItems);
    }

    public SelectorMenu rowSelector()
    {
        return elementCache().selectionMenu;
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

        //if the DR already has all rows checked, this initial click will de-selected all and return '0'
        if (!"0".equals(doAndWaitForUpdate(toggle::click)))
            doAndWaitForUpdate(toggle::click);
    }

    /**
     * @deprecated Use {@link #checkAllOnPage()} or {@link #rowSelector()}.{@link SelectorMenu#selectAll()}
     */
    @Deprecated
    public void checkAll()
    {
        checkAllOnPage();
    }

    /**
     * @deprecated Use {@link #uncheckAllOnPage()} or {@link #rowSelector()}.{@link SelectorMenu#selectNone()}
     */
    @Deprecated
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
        getPagingWidget().clickNextPage();
    }

    public void pagePrev()
    {
        TestLogger.log("Clicking page previous on data region '" + getDataRegionName() + "'");
        getPagingWidget().clickPreviousPage();
    }

    /**
     * @deprecated Ambiguous. Use {@link #rowSelector()}.{@link SelectorMenu#showAll()}
     * or {@link #getPagingWidget()}.{@link PagingWidget#clickShowAll()}
     */
    @Deprecated
    public void showAll()
    {
        rowSelector().showAll();
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
    public TimeChartWizard createQuickChart(String columnName)
    {
        clickColumnMenu(columnName, true, "Quick Chart");
        return new TimeChartWizard(getWrapper()).waitForReportRender();
    }

    // ======== Side facet panel =========

    public DatasetFacetPanel openSideFilterPanel()
    {
        if (!getWrapper().isElementPresent(DatasetFacetPanel.Locators.expandedFacetPanel(getDataRegionName())))
        {
            clickHeaderButton("Filter");
            getWrapper().waitForElement(Locator.css(".lk-filter-panel-label"));
        }
        // See handleAfterInitGroupConfig in ReportFilterPanel.js
        org.labkey.test.Locators.pageSignal("initSelectionComplete").waitForElement(getDriver(), 5_000);
        WebElement panelEl = getWrapper().waitForElement(DatasetFacetPanel.Locators.expandedFacetPanel(getDataRegionName()));
        getWrapper().shortWait().until(LabKeyExpectedConditions.animationIsDone(panelEl));
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

    /* sometimes 'insert new row' is a top-level button, other times it's a dropdown
    *  under an 'insert data' top-level button. This handles either case. */
    public UpdateQueryRowPage clickInsertNewRow()
    {
        if (hasHeaderMenu("Insert data"))
            clickHeaderMenu("Insert data", getInsertNewButtonText());
        else
            clickHeaderButtonAndWait(getInsertNewButtonText());
        return new UpdateQueryRowPage(getDriver());
    }

    public ImportDataPage clickImportBulkData()
    {
        if (hasHeaderMenu("Insert data"))
            clickHeaderMenu("Insert data", getImportBulkDataText());
        else
            clickHeaderButtonAndWait(getImportBulkDataText());
        return new ImportDataPage(getDriver());
    }

    public void clickDeleteAllButton()
    {
        elementCache().getHeaderButton("Delete All Rows").click();
        getWrapper().waitAndClick(Locator.linkWithText("Yes"));  //Confirmation popup
        getWrapper().waitAndClick(Locator.linkWithText("Ok"));  //results popup
    }

    public void deleteSelectedRows()
    {
        doAndWaitForUpdate(() ->
        {
            clickHeaderButton("Delete");
            getWrapper().acceptAlert();
        });
    }

    /** Get the number of items checked within the current page of the grid. */
    public int getCheckedCount()
    {
        return api().executeScript("getChecked().length;", Long.class).intValue();
    }

    /**
     * Get the selected item count persisted in the session state.  Includes rows not visible
     * on the current page of the grid.  Useful for validating the selection is cleared after
     * deleting a row from the grid.
     */
    public int getSelectedCount()
    {
        return api().executeScript("selectedCount;", Long.class).intValue();
    }

    @Deprecated
    public int getCheckedCount(BaseWebDriverTest test)
    {
        return getCheckedCount();
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

        public static Locator.XPathLocator filterContextAction()
        {
            return Locator.tagWithClass("div", "lk-region-context-action").withChild(Locator.tagWithClass("i", "fa-filter"));
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
        private final WebElement toggleHeaderCell = Locator.tag("th").withClasses("labkey-column-header", "labkey-selectors").findWhenNeeded(columnHeaderRow);
        private final WebElement toggleAllOnPage = Locator.input(".toggle").findWhenNeeded(toggleHeaderCell); // tri-state checkbox
        private SelectorMenu selectionMenu = new SelectorMenu(toggleHeaderCell);

        protected List<WebElement> getDataRows()
        {
            if (rows == null)
                rows = Collections.unmodifiableList(Locator.css(""+
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
                cells.put(row, Collections.unmodifiableList(Locator.xpath("td").findElements(getDataRow(row))));
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
                summaryStatCells = Collections.unmodifiableList(Locator.xpath("td").findElements(summaryStatRow));

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
                columnHeaders = Collections.unmodifiableList(Locator.css("th.labkey-column-header").findElements(columnHeaderRow));
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

    public class SelectorMenu extends BootstrapMenu
    {
        private SelectorMenu(WebElement menu)
        {
            super(DataRegionTable.this.getDriver(), menu);
        }

        private void clickSubMenu(String... subMenuLabels)
        {
            DataRegionTable.this.doAndWaitForUpdate(() ->
                    super.clickSubMenu(DataRegionTable.this.isAsync() ? 0 : DataRegionTable.this.getUpdateTimeout(), subMenuLabels));
        }

        public void selectAll()
        {
            clickSubMenu("Select All");
        }

        public void selectNone()
        {
            clickSubMenu("Select None");
        }

        public void showSelected()
        {
            clickSubMenu("Show Selected");
        }

        public void showUnselected()
        {
            clickSubMenu("Show Unselected");
        }

        public void showAll()
        {
            clickSubMenu("Show All");
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
