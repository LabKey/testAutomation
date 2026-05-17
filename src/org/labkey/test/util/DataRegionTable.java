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

import org.apache.commons.lang3.StringUtils;
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
import org.labkey.test.components.ui.grids.FieldReferenceManager;
import org.labkey.test.components.ui.grids.FieldReferenceManager.FieldReference;
import org.labkey.test.pages.ImportDataPage;
import org.labkey.test.pages.TimeChartWizard;
import org.labkey.test.pages.query.UpdateQueryRowPage;
import org.labkey.test.pages.reports.ManageViewsPage;
import org.labkey.test.params.FieldKey;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
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
    private final Map<String, Integer> _mapRows = new HashMap<>();
    private final Map<Integer, Map<Integer, String>> _dataCache = new LinkedHashMap<>();

    /**
     * @param regionName 'data-region-name' of the table
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

    public ManageViewsPage openManageViews()
    {
        getViewsMenu().clickSubMenu(false, "Manage Views");
        return new ManageViewsPage(getDriver());
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
        return elementCache().getColumnHeaders().size();
    }

    public boolean hasSummaryStatisticRow()
    {
        return !Locator.css("tr.labkey-col-total").findElements(elementCache()).isEmpty();
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
     * Assert that a Data Region has expected selection text (e.g. "Selected 100,000 of 1,234,567")
     * @param selectedRows number of selected rows
     * @param totalRows total number of rows in data behind the dataregion
     */
    public void assertSelectionText(int selectedRows, int totalRows)
    {
        String expected = "Selected " + String.format("%,d", selectedRows) + " of " + String.format("%,d", totalRows) + " rows.";
        String fullSelectionText = Locator.tagWithAttribute("div", "data-msgpart", "selection")
                .findElement(this).getText();

        assertTrue("Expected text to start with: " + expected + " Full text: " + fullSelectionText, fullSelectionText.startsWith(expected));
    }

    /**
     * @deprecated Renamed. Use {@link #getRowIndex(CharSequence, String)}
     */
    @Deprecated
    public int getRow(String columnLabel, String value)
    {
        return getRowIndex(columnLabel, value);
    }

    public int getRowIndex(CharSequence columnIdentifier, String value)
    {
        return getRowIndex(getColumnIndexStrict(columnIdentifier), value);
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

    public int getRowIndexStrict(CharSequence columnIdentifier, String value)
    {
        int rowIndex = getRowIndex(columnIdentifier, value);
        if (rowIndex < 0)
            throw new NoSuchElementException("No row where: " + columnIdentifier + "=" + value);
        return rowIndex;
    }

    public int getRowIndexStrict(int columnIndex, String value)
    {
        int rowIndex = getRowIndex(columnIndex, value);
        if (rowIndex < 0)
            throw new NoSuchElementException("No row where: " + elementCache().getColumnHeaderManager().getColumnHeader(columnIndex).getLabel() + " = " + value);
        return rowIndex;
    }

    public String getSummaryStatFooterText(CharSequence columnIdentifier)
    {
        return getSummaryStatFooterText(getColumnIndexStrict(columnIdentifier));
    }

    public String getSummaryStatFooterText(int columnIndex)
    {
        columnIndex += hasSelectors() ? 1 : 0;
        String footerText = elementCache().getSummaryStatisticCells().get(columnIndex).getText();
        return footerText.replaceAll("\\?", "");
    }

    /**
     * do nothing if column is already present, add it if it is not
     * @param fieldKey   fieldKey of column to add, if necessary
     */
    public void ensureColumnPresent(CharSequence fieldKey)
    {
        ensureColumnsPresent(fieldKey);
    }

    /**
     * check for presence of columns, add them if they are not already present
     * requires columns actually exist
     * @param fieldKeys fieldKeys of columns to add
     */
    public void ensureColumnsPresent(CharSequence... fieldKeys)
    {
        boolean opened = false;
        for (CharSequence fieldKey: fieldKeys)
        {
            fieldKey = FieldKey.fromFieldKey(fieldKey);
            if (getColumnIndex(fieldKey) == -1)
            {
                if (!opened)
                {
                    getCustomizeView().openCustomizeViewPanel();
                    opened = true;
                }
                getCustomizeView().addColumn(fieldKey.toString());
            }
        }
        if (opened)
            getCustomizeView().applyCustomView();
    }

    /**
     * returns index of the row of the first appearance of the specified data, in the specified column
     *
     * @deprecated Use {@link #getRowIndex(CharSequence, String)} or {@link #getRowIndexStrict(CharSequence, String)}
     */
    @Deprecated
    public int getIndexWhereDataAppears(String data, CharSequence columnIdentifier)
    {
        return getRowIndex(columnIdentifier, data);
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

    public WebElement link(int row, CharSequence columnIdentifier)
    {
        return link(row, getColumnIndexStrict(columnIdentifier));
    }

    public int getColumnIndexStrict(CharSequence columnIdentifier)
    {
        return elementCache().getColumnIndex(columnIdentifier).getDomIndex();
    }

    /**
     * Get column index for
     * @param columnIdentifier fieldKey, name, or label for desired column
     * @return column index or -1 if column isn't present
     */
    public int getColumnIndex(CharSequence columnIdentifier)
    {
        try
        {
            return getColumnIndexStrict(columnIdentifier);
        }
        catch (NoSuchElementException e1)
        {
            return -1;
        }
    }

    public List<String> getColumnLabels()
    {
        getComponentElement().isEnabled(); // validate cached element

        return elementCache().getColumnHeaderManager().getColumnHeaders().stream()
            .map(FieldReference::getLabel).toList();
    }

    public List<String> getColumnNames()
    {
        getComponentElement().isEnabled(); // validate cached element

        return elementCache().getColumnHeaderManager().getColumnHeaders().stream()
            .map(FieldReference::getName).toList();
    }

    public List<String> getFieldKeyStrings()
    {
        return getFieldKeys().stream()
            .map(FieldKey::toString).toList();
    }

    public List<FieldKey> getFieldKeys()
    {
        getComponentElement().isEnabled(); // validate cached element

        return elementCache().getColumnHeaderManager().getColumnHeaders().stream()
            .map(FieldReference::getFieldKey).toList();
    }

    public String getColumnTitle(CharSequence columnIdentifier)
    {
        WebElement columnHeader = elementCache().getColumnHeader(columnIdentifier).getElement();
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

    public List<String> getColumnDataAsText(CharSequence columnIdentifier)
    {
        return getColumnDataAsText(getColumnIndexStrict(columnIdentifier));
    }

    public Map<String, String> getRowDataAsMap(CharSequence columnIdentifier, String value)
    {
        return getRowDataAsMap(getRowIndexStrict(columnIdentifier, value));
    }

    public Map<String, String> getRowDataAsMap(int row)
    {
        Map<String, String> rowMap = new CaseInsensitiveHashMap<>();
        for (FieldKey fieldKey : getFieldKeys())
        {
            rowMap.put(fieldKey.toString(), getDataAsText(row, fieldKey));
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

    public List<String> getRowDataAsText(int row, CharSequence... columnIdentifiers)
    {
        List<String> rowData = new ArrayList<>();
        for (CharSequence column : columnIdentifiers)
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
    public List<List<String>> getFullColumnValues(CharSequence... columnIdentifiers)
    {
        boolean moreThanOnePage = getWrapper().isElementPresent(Locator.linkWithText("Next"));

        if (moreThanOnePage)
        {
            rowSelector().showAll();
        }

        List<List<String>> columns = new ArrayList<>();
        for (CharSequence columnIdentifier : columnIdentifiers)
        {
            columns.add(getColumnDataAsText(columnIdentifier));
        }

        if (moreThanOnePage)
        {
            setPageSize(100);
        }

        return columns;
    }

    public List<List<String>> getRows(CharSequence... columnIdentifiers)
    {
        List<List<String>> fullColumnValues = getFullColumnValues(columnIdentifiers);
        return collateColumnsIntoRows(fullColumnValues);
    }

    @SafeVarargs
    public static List<List<String>> collateColumnsIntoRows(List<String>... columns)
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

    /**
     * Strips the trailing Unicode word joiner character added by AJAXDetailsDisplayColumn for line wrapping purposes.
     */
    public static String stripWordJoiner(String value)
    {
        return value.replace("\u2060", "");
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

        int row = _mapRows.size();
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

    public WebElement findCell(int row, CharSequence columnIdentifier)
    {
        return findCell(row, getColumnIndexStrict(columnIdentifier));
    }

    public WebElement findCell(int row, int column)
    {
        column += hasSelectors() ? 1 : 0;
        return elementCache().getCell(row, column);
    }

    public String getDataAsText(int row, int column)
    {
        WebElement cell = findCell(row, column); // Will clear cache if needed

        return _dataCache
            .computeIfAbsent(row, k -> new LinkedHashMap<>())
            .computeIfAbsent(column, k -> cell.getText());
    }

    public String getDataAsText(int row, CharSequence columnIdentifier)
    {
        return getDataAsText(row, getColumnIndexStrict(columnIdentifier));
    }

    public String getDataAsText(String pk, CharSequence columnIdentifier)
    {
        return getDataAsText(getRowIndexStrict(pk), getColumnIndexStrict(columnIdentifier));
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
        return getLinkAttribute(row, column, "href");
    }

    public String getHref(int row, CharSequence columnIdentifier)
    {
        return getHref(row, getColumnIndexStrict(columnIdentifier));
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

    public boolean hasHref(int row, CharSequence columnIdentifier)
    {
        return hasHref(row, getColumnIndexStrict(columnIdentifier));
    }

    public String getLinkAttribute(int row, int column, String attr)
    {
        return link(row, column).getAttribute(attr);
    }

    public WebElement getFlag(int row, CharSequence columnIdentifier)
    {
        var cell = findCell(row, columnIdentifier);
        return tagWithAttribute("i", "data-flagid").findElement(cell);
    }

    public boolean isFlagEnabled(int row, CharSequence columnIdentifier)
    {
        var flag = getFlag(row, columnIdentifier);
        return isFlagEnabled(flag);
    }

    private boolean isFlagEnabled(WebElement flag)
    {
        return StringUtils.trimToEmpty(flag.getDomAttribute("class")).contains("lk-flag-enabled");
    }

    public boolean isFlagDisabled(int row, CharSequence columnIdentifier)
    {
        var flag = getFlag(row, columnIdentifier);
        return isFlagDisabled(flag);
    }

    private boolean isFlagDisabled(WebElement flag)
    {
        return StringUtils.trimToEmpty(flag.getDomAttribute("class")).contains("lk-flag-disabled");
    }

    /**
     * Get the flag value for the column or <code>null</code> if unset.
     */
    public String getFlagValue(int row, CharSequence columnIdentifier)
    {
        var flag = getFlag(row, columnIdentifier);
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
            assertEquals("Expect unset flag title to be 'Flag for review'", "Flag for review", title);
            return null;
        }
        throw new AssertionError("Expected flag class to be either 'lk-flag-enabled' or 'lk-flag-disabled'");
    }

    public void setFlagValueForSelectedRows(CharSequence columnIdentifier, String value)
    {
        int checkedCount = getCheckedCount();
        assertTrue(checkedCount > 0);

        var flag = getFlag(0, columnIdentifier);
        flag.click();

        var prompt = new MessagePrompt("Review", getDriver());
        assertThat(prompt.getBody(), containsString("Enter comment for " + checkedCount + " selected rows"));
        prompt.setValue(value).clickOK();
    }

    /**
     * Set the flag value for the column or <code>null</code> to clear the flag.
     */
    public void setFlagValue(int row, CharSequence columnIdentifier, String value)
    {
        var flag = getFlag(row, columnIdentifier);
        setFlagValue(flag, value);
    }

    private void setFlagValue(WebElement flag, String value)
    {
        flag.click();
        new MessagePrompt("Review", getDriver()).setValue(value).clickOK();
        assertEquals(value, getFlagValue(flag));
    }

    public void clearFlagValue(int row, CharSequence columnIdentifier)
    {
        setFlagValue(row, columnIdentifier, null);
    }

    /**
     * Clear all flag values on the grid, if any.
     */
    public void clearFlagValues()
    {
        List<WebElement> allFlags = Locator.tagWithAttribute("i", "data-flagid").findElements(elementCache());
        for (WebElement flag : allFlags)
        {
            if (isFlagEnabled(flag))
                setFlagValue(flag, null);
        }
    }

    public ColumnChartRegion createBarChart(CharSequence columnIdentifier)
    {
        return createColumnChart(columnIdentifier, "Bar Chart");
    }

    public ColumnChartRegion createPieChart(CharSequence columnIdentifier)
    {
        return createColumnChart(columnIdentifier, "Pie Chart");
    }

    public ColumnChartRegion createBoxAndWhiskerChart(CharSequence columnIdentifier)
    {
        return createColumnChart(columnIdentifier, "Box & Whisker");
    }

    @LogMethod
    public ColumnChartRegion createColumnChart(CharSequence columnIdentifier, String chartType)
    {
        Locator cssPlotLocator = Locator.css("div.labkey-dataregion-msg-plot-analytic svg");
        int initialNumOfPlots = cssPlotLocator.findElements(this).size();

        clickColumnMenu(columnIdentifier, false, chartType);
        cssPlotLocator.index(initialNumOfPlots).waitForElement(this, getUpdateTimeout());

        return new ColumnChartRegion(this);
    }

    public ColumnChartRegion getColumnPlotRegion()
    {
        return new ColumnChartRegion(this);
    }

    public void setSort(CharSequence columnIdentifier, SortDirection direction)
    {
        getWrapper().log("Setting sort in " + getDataRegionName() + " for " + columnIdentifier + " to " + direction.toString());
        doAndWaitForUpdate(() -> clickColumnMenu(columnIdentifier, !isAsync(), "Sort " + (direction.equals(SortDirection.ASC) ? "Ascending" : "Descending")));
    }

    public void setSummaryStatistic(CharSequence columnIdentifier, String stat, @Nullable String expectedValue)
    {
        clickSummaryStatistic(columnIdentifier, stat, false, expectedValue);
    }

    public void clearSummaryStatistic(CharSequence columnIdentifier, String stat, @Nullable String expectedValue)
    {
        clickSummaryStatistic(columnIdentifier, stat, true, expectedValue);
    }

    private void clickSummaryStatistic(CharSequence columnIdentifier, String stat, boolean isExpectedToBeChecked, @Nullable String expectedValue)
    {
        String clearOrSet = isExpectedToBeChecked ? "Clearing" : "Setting";
        TestLogger.log(clearOrSet + " the " + stat + " summary statistic in " + getDataRegionName() + " for " + columnIdentifier);
        clickColumnMenu(columnIdentifier, false, "Summary Statistics...");

        SummaryStatisticsDialog statsWindow = new SummaryStatisticsDialog(this);

        if (expectedValue != null)
            assertEquals("Stat value not as expected for: " + stat, expectedValue, statsWindow.getValue(stat));

        if (isExpectedToBeChecked)
            statsWindow.deselect(stat);
        else
            statsWindow.select(stat);

        statsWindow.apply();
    }

    public void verifySummaryStatisticValue(CharSequence columnIdentifier, String stat, String expectedValue, String filterDescription)
    {
        clickColumnMenu(columnIdentifier, false, "Summary Statistics...");
        SummaryStatisticsDialog statsWindow = new SummaryStatisticsDialog(this);
        assertEquals("Stat value not as expected for " + stat + " with filter " + filterDescription, expectedValue, statsWindow.getValue(stat));
        statsWindow.cancel();
    }

    public void removeColumn(CharSequence columnIdentifier)
    {
        removeColumn(columnIdentifier, false);
    }

    public void removeColumn(CharSequence columnIdentifier, boolean errorExpected)
    {
        TestLogger.log("Removing column " + columnIdentifier + " in " + getDataRegionName());
        clickColumnMenu(columnIdentifier, !errorExpected, "Remove Column");

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
                Window<?> removeError = new Window<>("Error", getDriver());
                assertTrue(removeError.getBody().contains("You must select at least one field to display in the grid."));
                removeError.clickButton("OK", true);
            }
        }
    }

    public void clearSort(CharSequence columnIdentifier)
    {
        doAndWaitForUpdate(() -> clickColumnMenu(columnIdentifier, !isAsync(), "Clear Sort"));
    }

    public WebElement openFilterDialog(CharSequence columnIdentifier)
    {
        String columnLabel = elementCache().getColumnHeader(columnIdentifier).getLabel();
        clickColumnMenu(columnIdentifier, false, "Filter...");

        final Locator.XPathLocator filterDialog = ExtHelper.Locators.window("Show Rows Where " + columnLabel + "...");
        WebElement filterDialogElement = getWrapper().waitForElement(filterDialog);

        WebDriverWrapper.waitFor(() -> getWrapper().isElementPresent(filterDialog.append(Locator.tagWithText("span","[All]")).notHidden()) ||
                        getWrapper().isElementPresent(filterDialog.append(Locator.tagWithId("input", "value_1").notHidden())) ||
                        getWrapper().isElementPresent(filterDialog.append(Locator.tagWithId("textarea", "value_1-1").notHidden())),
                "Filter Dialog", WAIT_FOR_JAVASCRIPT);
        getWrapper()._extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);

        return filterDialogElement;
    }

    @LogMethod (quiet = true)
    public void setFilter(@LoggedParam CharSequence columnIdentifier, @LoggedParam String filterType)
    {
        setFilter(columnIdentifier, filterType, null);
    }

    @LogMethod (quiet = true)
    public void setFilter(@LoggedParam CharSequence columnIdentifier, @LoggedParam String filterType, @Nullable @LoggedParam String filter)
    {
        setUpFilter(columnIdentifier, filterType, filter);
        doAndWaitForUpdate(() -> getWrapper().clickButton("OK", isAsync() ? 0 : getUpdateTimeout()));
    }

    /**
     * @deprecated Use {@link #setUpdateTimeout(int)} and {@link #setAsync(boolean)} to specify expected behavior
     */
    @Deprecated
    @LogMethod (quiet = true)
    public void setFilter(@LoggedParam CharSequence columnIdentifier, @LoggedParam String filterType, @Nullable @LoggedParam String filter, int waitMillis)
    {
        setUpFilter(columnIdentifier, filterType, filter);
        doAndWaitForUpdate(() -> getWrapper().clickButton("OK", waitMillis));
    }

    @LogMethod (quiet = true)
    public void setFilter(@LoggedParam CharSequence columnIdentifier, @LoggedParam String filterType, @Nullable @LoggedParam String filter, @Nullable @LoggedParam String filter2Type, @Nullable @LoggedParam String filter2)
    {
        setUpFilter(columnIdentifier, filterType, filter, filter2Type, filter2);
        doAndWaitForUpdate(() -> getWrapper().clickButton("OK", isAsync() ? 0 : getUpdateTimeout()));
    }

    @LogMethod (quiet = true)
    public void setFacetedFilter(@LoggedParam CharSequence columnIdentifier, @LoggedParam String... values)
    {
        setUpFacetedFilter(columnIdentifier, values);
        doAndWaitForUpdate(() -> getWrapper().clickButton("OK", isAsync() ? 0 : getUpdateTimeout()));
    }

    public void setUpFilter(CharSequence columnIdentifier, String filterType, String filter)
    {
        setUpFilter(columnIdentifier, filterType, filter, null, null);
    }

    public void setUpFilter(CharSequence columnIdentifier, String filter1Type, @Nullable String filter1, @Nullable String filter2Type, @Nullable  String filter2)
    {
        String log = "Setting filter in " + getDataRegionName() + " for " + columnIdentifier + " to " + filter1Type.toLowerCase() + (filter1 != null ? " " + filter1 : "");
        if (filter2Type != null)
        {
            log += " and " + filter2Type.toLowerCase() + (filter2 != null ? " " + filter2 : "");
        }
        TestLogger.log(log);

        openFilterDialog(columnIdentifier);

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
            String id = (filter1Type.matches("Contains One Of|Does Not Contain Any Of|Equals One Of|Does Not Equal Any Of")) ? "value_1-1" : "value_1";
            getWrapper().setFormElement(Locator.id(id), filter1);
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

    public void setUpFacetedFilter(CharSequence columnIdentifier, String... values)
    {
        if (values.length > 0)
        {
            TestLogger.log("Setting filter in " + getDataRegionName() + " for " + columnIdentifier + " to one of: [" + String.join(", ", values) + "]");
        }
        else
        {
            TestLogger.log("Clear filter in " + getDataRegionName() + " for " + columnIdentifier);
        }

        WebElement filterDialog = openFilterDialog(columnIdentifier);

        sleep(500);

        // Clear selections.
        assertEquals("Faceted filter tab should be selected.", "Choose Values", getWrapper().getText(Locator.css(".x-tab-strip-active")));
        if (!Locator.xpath("//div[contains(@class, 'x-grid3-hd-checker-on')]").existsIn(filterDialog))
        {
            getWrapper().click(Locator.tagWithText("span","[All]"));
        }
        getWrapper().click(Locator.tagWithText("span", "[All]"));

        if (values.length > 1)
        {
            for (String v : values)
            {
                Locator.xpath("//div[contains(@class,'x-grid3-row') and .//span[text()='" + v + "']]//div[@class='x-grid3-row-checker']")
                    .findElement(filterDialog).click();
            }
        }
        else if (values.length == 1)
        {
            Locator.xpath("//div[contains(@class,'x-grid3-row')]//span[text()='" + values[0] + "']")
                .findElement(filterDialog).click();
        }
    }

    public void clearFilter(CharSequence columnIdentifier)
    {
        clearFilter(columnIdentifier, isAsync() ? 0 : BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void clearFilter(CharSequence columnIdentifier, int waitMillis)
    {
        TestLogger.log("Clearing filter in " + getDataRegionName() + " for " + columnIdentifier);
        doAndWaitForUpdate(() -> clickColumnMenu(columnIdentifier, waitMillis > 0, "Clear Filter"));
    }

    public void clearAllFilters()
    {
        // TODO: This should be updated to use the UI once the UX Refresh configures a "Clear all" mechanism again
        TestLogger.log("Clearing all filters in " + getDataRegionName());
        api().expectingRefresh().executeScript("clearAllFilters()");
    }

    public void clearAllFilters(CharSequence columnIdentifier)
    {
        TestLogger.log("Clearing filter in " + getDataRegionName() + " for " + columnIdentifier);
        openFilterDialog(columnIdentifier);
        doAndWaitForUpdate(() -> getWrapper().clickButton("Clear All Filters", isAsync() ? 0 : getUpdateTimeout()));
    }

    public void clearVariables()
    {
        TestLogger.log("Clear variable in" + getDataRegionName());
        doAndWaitForUpdate(()-> getWrapper().click(Locator.tagWithClass("span", "labkey-button ctx-clear-var")));
    }

    public void clickColumnMenu(CharSequence columnIdentifier, boolean pageLoad, String... menuItems)
    {
        final WebElement menu = elementCache().getColumnHeader(columnIdentifier).getElement();
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
     * Show all rows using the row selector menu (actually maxRows=5000).
     * For more precise control, use {@link #rowSelector()}.{@link SelectorMenu#showAll()}
     * or {@link #getPagingWidget()}.{@link PagingWidget#clickShowAll()}
     */
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

    public ContainerFilterType getContainerFilter()
    {
        getViewsMenu().openMenuTo("Folder Filter", "Folder Filter");
        var visibleFilterMenuItemOptions = getViewsMenu().findVisibleMenuItems();
        for (WebElement el : visibleFilterMenuItemOptions)
        {
            if (Locator.tagWithClass("i", "fa-check-square-o").existsIn(el))
            {
                return ContainerFilterType.fromLabel(el.getText());
            }
        }
        return null;
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
    public TimeChartWizard createQuickChart(CharSequence columnIdentifier)
    {
        clickColumnMenu(columnIdentifier, true, "Quick Chart");
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

    public boolean columnHasChartOption(CharSequence columnIdentifier, String chartType)
    {
        BootstrapMenu menu = new BootstrapMenu(getDriver(), elementCache().getColumnHeader(columnIdentifier).getElement());
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
            return form().withAttributeMatchingOtherElementAttribute("data-region-form", Locator.xpath(".//table"), "data-region-name");
        }

        public static Locator.XPathLocator dataRegionTable(String regionName)
        {
            return form(regionName).withDescendant(table(regionName));
        }

        public static Locator.XPathLocator table()
        {
            return Locator.tag("table").withAttribute("data-region-name");
        }

        public static Locator.XPathLocator table(String regionName)
        {
            return Locator.tagWithAttribute("table", "data-region-name", regionName);
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
            return Locator.tagWithAttribute("th", "data-column-name", regionName + ":" + fieldName);
        }

        public static Locator.XPathLocator floatingHeader()
        {
            return Locator.tagWithClass("tr", "labkey-col-header-row").attributeEndsWith("id", "-float");
        }

        public static Locator.XPathLocator filterContextAction()
        {
            return Locator.tagWithClass("div", "lk-region-context-action").withChild(Locator.tagWithClass("i", "fa-filter"));
        }

        public static Locator.XPathLocator contextAction()
        {
            return Locator.tagWithClass("div", "lk-region-context-action");
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
        private List<WebElement> rows;
        private Map<Integer, List<WebElement>> cells;
        private final WebElement summaryStatRow = Locator.css("#" + getTableId() + " > tbody > tr.labkey-col-total").findWhenNeeded(getDriver());
        private List<WebElement> summaryStatCells;
        private final WebElement toggleHeaderCell = Locator.tag("th").withClasses("labkey-column-header", "labkey-selectors").findWhenNeeded(columnHeaderRow);
        private final WebElement toggleAllOnPage = Locator.input(".toggle").findWhenNeeded(toggleHeaderCell); // tri-state checkbox
        private final SelectorMenu selectionMenu = new SelectorMenu(toggleHeaderCell);

        private FieldReferenceManager _fieldReferenceManager;
        protected FieldReferenceManager getColumnHeaderManager()
        {
            if (_fieldReferenceManager == null)
            {
                List<DataRegionColumnHeader> columnHeaders = new ArrayList<>();
                List<WebElement> headerCellElements = Locator.css("th.labkey-column-header").waitForElements(columnHeaderRow, WAIT_FOR_JAVASCRIPT);

                headerCellElements = headerCellElements.subList(hasSelectors() ? 1 : 0, headerCellElements.size());

                int domIndex = 0; // Not actually the DOM index but many usages don't include the selector column
                for (; domIndex < headerCellElements.size(); domIndex++)
                {
                    columnHeaders.add(new DataRegionColumnHeader(headerCellElements.get(domIndex), domIndex));
                }

                _fieldReferenceManager = new FieldReferenceManager(columnHeaders);
            }
            return _fieldReferenceManager;
        }

        public @NotNull FieldReference getColumnIndex(@NotNull CharSequence columnIdentifier)
        {
            boolean foundWithLabel = false;
            FieldReference fieldReference = elementCache().getColumnHeaderManager().findFieldReferenceOrNull(columnIdentifier);

            if (fieldReference == null)
            {
                String noSpaces = columnIdentifier.toString().replace(" ", "");
                if (!noSpaces.equals(columnIdentifier.toString()))
                {
                    fieldReference = elementCache().getColumnHeaderManager().findFieldReferenceOrNull(noSpaces);
                    if (fieldReference != null)
                    {
                        TestLogger.warn("Unnecessary space in requested column name. " +
                            "Requested: \"%s\" Found: \"%s\"".formatted(columnIdentifier, fieldReference.getFieldKey()));
                    }
                    else
                    {
                        Map<String, FieldReference> columnLabelsWithoutSpaces = new HashMap<>();
                        getColumnHeaderManager().getColumnHeaders().forEach(fr -> columnLabelsWithoutSpaces.putIfAbsent(fr.getLabel().replace(" ", ""), fr));
                        fieldReference = columnLabelsWithoutSpaces.get(noSpaces);
                        if (fieldReference != null)
                        {
                            TestLogger.warn("Weird column label specified. Use actual label. " +
                                "Requested: \"%s\" Found: \"%s\"".formatted(columnIdentifier, fieldReference.getLabel()));
                            foundWithLabel = true;
                        }
                    }
                }
            }

            if (fieldReference == null)
            {
                throw new NoSuchElementException("No column: " + columnIdentifier);
            }

            if (foundWithLabel ||
                !columnIdentifier.equals(fieldReference.getFieldKey()) &&
                    !columnIdentifier.toString().equals(fieldReference.getName()))
            {
                TestLogger.warn("Please reference columns by name/fieldKey instead of label. " +
                    "Requested column with label: \"%s\" Found column with fieldKey: \"%s\""
                        .formatted(columnIdentifier, fieldReference.getFieldKey()));
            }

            return fieldReference;
        }

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
                            if (StringUtils.trimToEmpty(e.getMessage()).contains("Other element would receive the click: <div class=\"labkey-button-bar\">"))
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
                cells = new LinkedHashMap<>();
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

        protected @NotNull FieldReference getColumnHeader(CharSequence columnIdentifier)
        {
            return getColumnHeaderManager().findFieldReference(columnIdentifier);
        }

        protected List<FieldReference> getColumnHeaders()
        {
            return getColumnHeaderManager().getColumnHeaders();
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
        CURRENT_AND_SUBFOLDERS_PLUS_SHARED("Current folder, subfolders, and Shared project"),
        CURRENT_PLUS_PROJECT_AND_SHARED("Current folder, project, and Shared project"),
        ALL_FOLDERS("All folders");

        private final String _label;

        ContainerFilterType(String label)
        {
            _label = label;
        }

        public static ContainerFilterType fromLabel(String label)
        {
            return Arrays.stream(ContainerFilterType.values())
                    .filter(a-> a.getLabel().equals(label)).findFirst().orElse(null);
        }

        public String getLabel()
        {
            return _label;
        }
    }

    protected class DataRegionColumnHeader extends FieldReference
    {
        public DataRegionColumnHeader(WebElement element, int domIndex)
        {
            super(element, domIndex);
        }

        @Override
        protected String labelSupplier()
        {
            return getElement().getText();
        }

        @Override
        protected FieldKey fieldKeySupplier()
        {
            String columnNameAttribute = StringUtils.trimToEmpty(getElement().getDomAttribute("data-column-name"));
            if (columnNameAttribute.startsWith(getDataRegionName() + ":"))
                columnNameAttribute = columnNameAttribute.substring(getDataRegionName().length() + 1);
            return FieldKey.fromFieldKey(columnNameAttribute);
        }
    }
}
