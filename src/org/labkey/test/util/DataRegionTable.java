/*
 * Copyright (c) 2007-2016 LabKey Corporation
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
import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.ComponentElements;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.selenium.RefindingWebElement;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Component wrapper class for interacting with a LabKey Data Region (see clientapi/dom/DataRegion.js)
 */
public class DataRegionTable extends Component
{
    public static final boolean isNewDataRegion = false; // TODO: Remove flag once conversion is complete

    public static final String SELECTION_SIGNAL = "dataRegionSelectionChange";
    public static final String PANEL_SHOW_SIGNAL = "dataRegionPanelShow";
    public static final String PANEL_HIDE_SIGNAL = "dataRegionPanelHide";
    private static final int DEFAULT_WAIT = 30000;

    protected final String _regionName;
    @Deprecated public BaseWebDriverTest _test;
    protected WebDriverWrapper _driver;
    private WebElement _tableElement;
    protected final boolean _selectors;
    protected final boolean _floatingHeaders;

    // Cached items
    private CustomizeView _customizeView;
    protected final List<String> _columnLabels = new ArrayList<>();
    protected final List<String> _columnNames = new ArrayList<>();
    protected final Map<String, Integer> _mapRows = new HashMap<>();
    private Elements _elements;

    private DataRegionTable(WebElement table, String name, WebDriverWrapper driverWrapper)
    {
        if (driverWrapper instanceof BaseWebDriverTest)
            _test = (BaseWebDriverTest)driverWrapper;
        _driver = driverWrapper;
        _driver.waitForElement(Locators.pageSignal(SELECTION_SIGNAL), DEFAULT_WAIT);

        if ((table == null) == (name == null))
            throw new IllegalArgumentException("Specify either a table element or data region name");

        if (table == null)
        {
            _tableElement = new RefindingWebElement(Locators.dataRegion(name), driverWrapper.getDriver()).
                    withRefindListener(element ->
                    {
                        clearCache();
                        _driver.waitForElement(Locators.pageSignal(SELECTION_SIGNAL), DEFAULT_WAIT);
                    });
            _regionName = name;
        }
        else
        {
            _tableElement = table;
            _regionName = table.getAttribute("lk-region-name");
        }

        _selectors = !Locator.css(".labkey-selectors").findElements(_tableElement).isEmpty();
        _floatingHeaders = !Locator.xpath("tbody/tr").withClass("dataregion_column_header_row_spacer").findElements(_tableElement).isEmpty();

        _driver.addPageLoadListener(this::clearCache);
    }

    /**
     * @param test Necessary while DRT methods live in BWDT
     * @param table table element that contains data region
     */
    public DataRegionTable(WebDriverWrapper test, WebElement table)
    {
        this(table, null, test);
    }

    /**
     * @param regionName 'lk-region-name' of the table
     * @param test Necessary while DRT methods live in BWDT
     */
    public DataRegionTable(String regionName, WebDriverWrapper test)
    {
        this(null, regionName, test);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _tableElement;
    }

    protected Elements elements()
    {
        getComponentElement().isDisplayed(); // Trigger cache reset
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    private void clearCache()
    {
        _elements = null;
        _customizeView = null;
        _columnLabels.clear();
        _columnNames.clear();
        _mapRows.clear();
    }

    public CustomizeView getCustomizeView()
    {
        if (_customizeView == null)
        {
            if (isNewDataRegion)
                _customizeView = new CustomizeView(this);
            else
                _customizeView = new CustomizeViewsHelper(this);
        }
        return _customizeView;
    }

    protected int getHeaderRowCount()
    {
        return 2 + (_floatingHeaders ? 2 : 0);
    }

    public static DataRegionTable waitForDataRegion(WebDriverWrapper test, String regionName)
    {
        return waitForDataRegion(test, regionName, DEFAULT_WAIT);
    }

    public static DataRegionTable waitForDataRegion(WebDriverWrapper test, String regionName, int msTimeout)
    {
        test.waitForElement(Locators.dataRegion(regionName), msTimeout);
        return new DataRegionTable(regionName, test);
    }

    public static DataRegionTable findDataRegion(WebDriverWrapper test)
    {
        return findDataRegion(test, 0);
    }

    public static DataRegionTable findDataRegion(WebDriverWrapper test, int index)
    {
        return findDataRegionWithin(test, test.getDriver(), index);
    }

    public static DataRegionTable findDataRegionWithin(WebDriverWrapper test, SearchContext context)
    {
        return findDataRegionWithin(test, context, 0);
    }

    public static DataRegionTable findDataRegionWithin(WebDriverWrapper test, SearchContext context, int index)
    {
        Locator dataRegionLoc = Locator.css("table[lk-region-name]").index(index);
        return new DataRegionTable(test, new RefindingWebElement(dataRegionLoc, context));
    }

    public static DataRegionTable findDataRegionWithinWebpart(WebDriverWrapper test, String webPartTitle)
    {
        return findDataRegionWithin(test, new RefindingWebElement(PortalHelper.Locators.webPart(webPartTitle), test.getDriver()));
    }

    public String getTableName()
    {
        return _regionName;
    }

    /**
     * @deprecated We should find sub-elements by SearchContext, rather than building up big Locator strings
     */
    @Deprecated
    private String getTableId()
    {
        return getComponentElement().getAttribute("id");
    }

    public Locator.IdLocator locator()
    {
        return Locator.id(getTableId());
    }

    public int getColumnCount()
    {
        return elements().getColumnHeaders().size() - (_selectors ? 1 : 0);
    }

    private boolean bottomBarPresent()
    {
        return _driver.isElementPresent(Locator.tagWithId("table", getTableId() + "-footer"));
    }

    public boolean hasAggregateRow()
    {
        return _driver.isElementPresent(Locator.xpath("//table[@id=" + Locator.xq(getTableId()) + "]//tr[contains(@class, 'labkey-col-total')]"));
    }

    public List<WebElement> getHeaderButtons()
    {
        return elements().getHeaderButtons();
    }

    public int getDataRowCount()
    {
        return elements().getRows().size();
    }

    public int getRow(String columnLabel, String value)
    {
        return getRow(getColumnIndex(columnLabel), value);
    }

    public int getRow(int columnIndex, String value)
    {
        int rowCount = getDataRowCount();
        for (int i=0; i < rowCount; i++)
        {
            if (value.equals(getDataAsText(i, columnIndex)))
                return i;
        }
        return -1;
    }

    public String getTotal(String columnLabel)
    {
        return getTotal(getColumnIndex(columnLabel));
    }

    public String getTotal(int columnIndex)
    {
        return Locator.css("#" + getTableId() + " tr.labkey-col-total > td:nth-of-type(" + (columnIndex + (_selectors ? 2 : 1)) + ")")
                .findElement(getComponentElement()).getText();
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
                getCustomizeView().addCustomizeViewColumn(name);
            }
        }
        if (opened)
            getCustomizeView().applyCustomView();
    }

    /**
     * returns index of the row of the first appearance of the specified data, in the specified column
     * @param data
     * @param column
     * @return
     */
    public int getIndexWhereDataAppears(String data, String column)
    {
        List<String> allData = getColumnDataAsText(column);
        return allData.indexOf(data);
    }

    public Locator.XPathLocator detailsXpath(int row)
    {
        return Locator.xpath("//table[@id=" + Locator.xq(getTableId()) + "]/tbody/tr[" + (row + getHeaderRowCount() + 1) + "]/td[contains(@class, 'labkey-details')]");
    }

    public Locator.XPathLocator detailsLink(int row)
    {
        Locator.XPathLocator cell = detailsXpath(row);
        return cell.child("a[1]");
    }

    public Locator.XPathLocator updateXpath(int row)
    {
        return Locator.xpath("//table[@id=" + Locator.xq(getTableId()) + "]/tbody/tr[" + (row + getHeaderRowCount() + 1) + "]/td[contains(@class, 'labkey-update')]");
    }

    public Locator.XPathLocator updateLink(int row)
    {
        Locator.XPathLocator cell = updateXpath(row);
        return cell.child("a[1]");
    }

    public Locator.XPathLocator xpath(int row, int col)
    {
        return Locator.xpath("//table[@id=" + Locator.xq(getTableId()) + "]/tbody/tr[" + (row + getHeaderRowCount() + 1) + "]/td[" + (col + 1 + (_selectors ? 1 : 0)) + "]");
    }

    public Locator.XPathLocator link(int row, int col)
    {
        Locator.XPathLocator cell = xpath(row, col);
        return cell.child("a[1]");
    }

    public WebElement link(int row, String columnName)
    {
        int col = getColumnIndex(columnName);
        if (col == -1)
            fail("Couldn't find column '" + columnName + "'");
        return findElement(link(row, col));
    }

    public int getColumnIndex(String name)
    {
        name = name.replaceAll(" ", "");
        int i;

        i = getColumnNames().indexOf(_regionName + ":" + name);

        if (i < 0)
        {
            List<String> columnsWithoutWhitespace = new ArrayList<>(getColumnHeaders().size());
            getColumnHeaders().stream().forEachOrdered(s -> columnsWithoutWhitespace.add(s.replaceAll(" ", "")));

            i = columnsWithoutWhitespace.indexOf(name);
        }

        if (i < 0)
            TestLogger.log("Column '" + name + "' not found");
        return i;
    }

    /**
     * @deprecated Renamed: {@link #getColumnIndex(String)}
     */
    @Deprecated
    public int getColumn(String name)
    {
        return getColumnIndex(name);
    }

    public List<String> getColumnHeaders()
    {
        getComponentElement().isDisplayed(); // validate cached element

        if (_columnLabels.isEmpty())
        {
            _columnLabels.addAll(_driver.getTexts(elements().getColumnHeaders()));
            if (_selectors)
                _columnLabels.remove(0);
        }

        return ImmutableList.copyOf(_columnLabels);
    }

    public List<String> getColumnNames()
    {
        getComponentElement().isDisplayed(); // validate cached element

        if (_columnNames.isEmpty())
        {
            List<WebElement> columnHeaders = elements().getColumnHeaders();
            for (int i = _selectors ? 1 : 0; i< columnHeaders.size(); i++)
            {
                _columnNames.add(columnHeaders.get(i).getAttribute("column-name"));
            }
        }

        return ImmutableList.copyOf(_columnNames);
    }

    public List<String> getColumnDataAsText(int col)
    {
        int rowCount = getDataRowCount();
        List<String> columnText = new ArrayList<>();

        if (col >= 0)
        {
            for (int row = 0; row < rowCount; row++)
            {
                columnText.add(getDataAsText(row, col));
            }
        }

        return columnText;
    }

    public List<String> getColumnDataAsText(String name)
    {
        int col = getColumnIndex(name);
        return getColumnDataAsText(col);
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

    /**
     * Get values for all specified columns for all pages of the table
     * preconditions:  must be on start page of table
     * postconditions:  at start of table
     */
    public List<List<String>> getFullColumnValues(String... columnNames)
    {
        boolean moreThanOnePage = _driver.isElementPresent(Locator.linkWithText("Next"));

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

    public int getRow(String pk)
    {
        return getRowIndex(pk);
    }

    /** Find the row number for the given primary key. */
    public int getRowIndex(String pk)
    {
        assertTrue("Need the selector checkboxes value to find row by pk", _selectors);

        getComponentElement().isDisplayed(); // refresh cache

        Integer cached = _mapRows.get(pk);
        if (cached != null)
            return cached.intValue();

        int row = 0;
        try
        {
            while (true)
            {
                String value = _driver.getAttribute(Locator.xpath("//table[@id=" + Locator.xq(getTableId()) +"]//tr[" + (row+5) + "]//input[@name='.select']"), "value");
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

    private boolean hasNoDataToShow()
    {
        return Locator.tagWithText("tr", "No data to show.").findElements(getComponentElement()).size() == 1;
    }

    public WebElement findCell(int row, String column)
    {
        if (getColumnIndex(column) < 0)
            throw new NoSuchElementException("No such column '" + column + "'");
        return findCell(row, getColumnIndex(column));
    }

    public WebElement findCell(int row, int column)
    {
        column += _selectors ? 1 : 0;
        return elements().getCell(row, column);
    }

    public String getDataAsText(int row, int column)
    {
        return findCell(row, column).getText();
    }

    public String getDataAsText(int row, String columnName)
    {
        int col = getColumnIndex(columnName);
        if (col == -1)
            return null;
        return getDataAsText(row, col);
    }

    public String getDataAsText(String pk, String columnName)
    {
        int row = getRow(pk);
        if (row == -1)
            return null;
        int col = getColumnIndex(columnName);
        if (col == -1)
            return null;
        return getDataAsText(row, col);
    }

    public String getDetailsHref(int row)
    {
        Locator l = detailsLink(row);
        return _driver.getAttribute(l, "href");
    }

    public String getUpdateHref(int row)
    {
        Locator l = updateLink(row);
        return _driver.getAttribute(l, "href");
    }

    public String getHref(int row, int column)
    {
        // headerRows and selector offsets are applied in link() locator
        return _getHref(row, column);
    }

    private String _getHref(int row, int column)
    {
        Locator l = link(row, column);
        return _driver.getAttribute(l, "href");
    }

    public String getHref(int row, String columnName)
    {
        int col = getColumnIndex(columnName);
        if (col == -1)
            return null;
        return getHref(row, col);
    }

    public String getHref(String pk, String columnName)
    {
        int row = getRow(pk);
        if (row == -1)
            return null;
        int col = getColumnIndex(columnName);
        if (col == -1)
            return null;
        return getHref(row, col);
    }

    public boolean hasHref(int row, int column)
    {
        // headerRows and selector offsets are applied in link() locator
        return _hasHref(row, column);
    }

    private boolean _hasHref(int row, int column)
    {
        // Check the td cell is present, but has no link.
        Locator.XPathLocator cell = xpath(row, column);
        _driver.assertElementPresent(cell);

        Locator link = link(row, column);
        return _driver.isElementPresent(link);
    }

    public boolean hasHref(int row, String columnName)
    {
        int col = getColumnIndex(columnName);
        if (col == -1)
            fail("Column '" + columnName + "' not found.");
        return hasHref(row, col);
    }

    public void setSort(String columnName, SortDirection direction)
    {
        _driver.setSort(_regionName, columnName, direction);
    }

    public void clearSort(String columnName)
    {
        _driver.clearSort(_regionName, columnName);
    }

    public void openFilterDialog(String columnName)
    {
        Locator.XPathLocator menuLoc = DataRegionTable.Locators.columnHeader(getTableName(), columnName);
        String columnLabel = _driver.getText(menuLoc);
        _driver._ext4Helper.clickExt4MenuButton(false, menuLoc, false, "Filter...");

        final Locator.XPathLocator filterDialog = ExtHelper.Locators.window("Show Rows Where " + columnLabel + "...");
        _driver.waitForElement(filterDialog);

        _driver.waitFor(() -> _driver.isElementPresent(filterDialog.append(Locator.linkWithText("[All]")).notHidden()) ||
                        _driver.isElementPresent(filterDialog.append(Locator.tagWithId("input", "value_1").notHidden())),
                "Filter Dialog", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _driver._extHelper.waitForLoadingMaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
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
        setUpFilter(columnName, filterType, filter);
        _driver.clickButton("OK", waitMillis);
    }

    public void setFilter(String columnName, String filterType, @Nullable String filter, @Nullable String filter2Type, @Nullable String filter2)
    {
        setUpFilter(columnName, filterType, filter, filter2Type, filter2);
        _driver.clickButton("OK", BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void setFacetedFilter(String columnName, String... values)
    {
        setUpFacetedFilter(columnName, values);
        _driver.clickButton("OK");
    }

    public void setUpFilter(String columnName, String filterType, String filter)
    {
        setUpFilter(columnName, filterType, filter, null, null);
    }

    public void setUpFilter(String columnName, String filter1Type, @Nullable String filter1, @Nullable String filter2Type, @Nullable  String filter2)
    {
        String log = "Setting filter in " + _regionName + " for " + columnName + " to " + filter1Type.toLowerCase() + (filter1 != null ? " " + filter1 : "");
        if (filter2Type != null)
        {
            log += " and " + filter2Type.toLowerCase() + (filter2 != null ? " " + filter2 : "");
        }
        TestLogger.log(log);

        openFilterDialog(columnName);

        if (_driver.isElementPresent(Locator.css("span.x-tab-strip-text").withText("Choose Values")))
        {
            TestLogger.log("Switching to advanced filter UI");
            _driver._extHelper.clickExtTab("Choose Filters");
            _driver.waitForElement(Locator.xpath("//span[" + Locator.NOT_HIDDEN + " and text()='Filter Type:']"), WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        }

        //Select combo box item
        _driver._extHelper.selectComboBoxItem("Filter Type:", filter1Type);

        if (filter1 != null && !filter1Type.contains("Blank"))
        {
            _driver.setFormElement(Locator.id("value_1"), filter1);
        }

        if (filter2Type != null && !filter2Type.contains("Blank"))
        {
            //Select combo box item
            _driver._extHelper.selectComboBoxItem("and:", filter2Type);
            if (filter2 != null)
            {
                _driver.setFormElement(Locator.id("value_2"), filter2);
            }
        }
    }

    public void setUpFacetedFilter(String columnName, String... values)
    {
        String log;
        if (values.length > 0)
        {
            log = "Setting filter in " + _regionName + " for " + columnName + " to one of: [";
            for (String v : values)
            {
                log += v + ", ";
            }
            log = log.substring(0, log.length() - 2) + "]";
        }
        else
        {
            log = "Clear filter in " + _regionName + " for " + columnName;
        }

        TestLogger.log(log);

        openFilterDialog(columnName);
        String columnLabel = _driver.getText(DataRegionTable.Locators.columnHeader(_regionName, columnName));

        WebDriverWrapper.sleep(500);

        // Clear selections.
        assertEquals("Faceted filter tab should be selected.", "Choose Values", _driver.getText(Locator.css(".x-tab-strip-active")));
        if (!_driver.isElementPresent(Locator.xpath("//div[contains(@class, 'x-grid3-hd-checker-on')]")))
        {
            _driver.click(Locator.linkWithText("[All]"));
        }
        _driver.click(Locator.linkWithText("[All]"));

        if (values.length > 1)
        {
            for (String v : values)
            {
                _driver.click(Locator.xpath(_driver._extHelper.getExtDialogXPath("Show Rows Where " + columnLabel + "...") +
                        "//div[contains(@class,'x-grid3-row') and .//span[text()='" + v + "']]//div[@class='x-grid3-row-checker']"));
            }
        }
        else if (values.length == 1)
        {
            _driver.click(Locator.xpath(_driver._extHelper.getExtDialogXPath("Show Rows Where " + columnLabel + "...")+
                    "//div[contains(@class,'x-grid3-row')]//span[text()='" + values[0] + "']"));
        }
    }

    public void clearFilter(String columnName)
    {
        clearFilter(columnName, BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void clearFilter(String columnName, int waitMillis)
    {
        TestLogger.log("Clearing filter in " + _regionName + " for " + columnName);

        Runnable clickClearFilter = () ->
                _driver._ext4Helper.clickExt4MenuButton(false, Locators.columnHeader(_regionName, columnName), false, "Clear Filter");

        if (waitMillis == 0)
            _driver.doAndWaitForPageSignal(clickClearFilter, SELECTION_SIGNAL);
        else
            _driver.doAndWaitForPageToLoad(clickClearFilter, waitMillis);

    }

    public void clearAllFilters(String columnName)
    {
        TestLogger.log("Clearing filter in " + _regionName + " for " + columnName);
        openFilterDialog(columnName);
        _driver.clickButton("CLEAR ALL FILTERS");
    }

    public void checkAllOnPage()
    {
        checkAll();
    }

    public void uncheckAllOnPage()
    {
        uncheckAll();
    }

    private WebElement getToggle()
    {
        return Locator.tagWithAttribute("input", "name", ".toggle").findElement(getComponentElement());
    }

    public void checkAll()
    {
        WebElement toggle = getToggle();
        if (!toggle.isSelected())
            _driver.doAndWaitForPageSignal(toggle::click, SELECTION_SIGNAL);
    }

    public void uncheckAll()
    {
        WebElement toggle = getToggle();
        if (null != _driver.doAndWaitForPageSignal(toggle::click, SELECTION_SIGNAL))
            _driver.doAndWaitForPageSignal(toggle::click, SELECTION_SIGNAL);
    }

    // NOTE: this method would be better named checkCheckboxByPrimaryKey --> while it does take a string, this string will often be a string value
    public void checkCheckbox(String value)
    {
        WebElement checkbox = Locator.css(".labkey-selectors > input[type=checkbox][value=" + Locator.xq(value) + "]")
                .findElement(getComponentElement());
        if (!checkbox.isSelected())
            _driver.doAndWaitForPageSignal(checkbox::click, SELECTION_SIGNAL);
    }

    public void checkCheckbox(int index)
    {
        WebElement checkbox = Locator.css(".labkey-selectors > input[type=checkbox][value]").index(index)
                .findElement(getComponentElement());
        if (!checkbox.isSelected())
            _driver.doAndWaitForPageSignal(checkbox::click, SELECTION_SIGNAL);
    }

    public void uncheckCheckbox(int index)
    {
        WebElement checkbox = Locator.css(".labkey-selectors > input[type=checkbox][value]").index(index)
                .findElement(getComponentElement());
        if (checkbox.isSelected())
            _driver.doAndWaitForPageSignal(checkbox::click, SELECTION_SIGNAL);
    }

    public void pageFirst()
    {
        TestLogger.log("Clicking page first on data region '" + _regionName + "'");
        clickDataRegionPageLink("First Page");
    }

    public void pageLast()
    {
        TestLogger.log("Clicking page last on data region '" + _regionName + "'");
        clickDataRegionPageLink("Last Page");
    }

    public void pageNext()
    {
        TestLogger.log("Clicking page next on data region '" + _regionName + "'");
        clickDataRegionPageLink("Next Page");
    }

    public void pagePrev()
    {
        TestLogger.log("Clicking page previous on data region '" + _regionName + "'");
        clickDataRegionPageLink("Previous Page");
    }

    public void clickDataRegionPageLink(String title)
    {
        String headerId = Locator.xq(getTableId() + "-header");
        _driver.clickAndWait(Locator.xpath("//table[@id=" + headerId + "]//div/a[@title='" + title + "']"));
    }

    public void showAll()
    {
        clickHeaderButton("Paging", "Show All");
    }

    public void setPageSize(int size)
    {
        clickHeaderButton("Paging", size + " per page");
    }

    /**
     * Set the current offset by manipulating the url rather than using the pagination buttons.
     * @param offset
     */
    public void setOffset(int offset)
    {
        String url = replaceParameter(_regionName + ".offset", String.valueOf(offset));
        _driver.beginAt(url);
    }

    /**
     * Set the page size by manipulating the url rather than using the "XXX per page" menu items.
     * @param size new page size
     */
    public void setMaxRows(int size)
    {
        String url = replaceParameter(_regionName + ".maxRows", String.valueOf(size));
        _driver.beginAt(url);
    }

    @LogMethod
    public void createQuickChart(String columnName)
    {
        _driver._ext4Helper.clickExt4MenuButton(true, DataRegionTable.Locators.columnHeader(_regionName, columnName), false, "Quick Chart");
        _driver.waitForElement(Locator.css("svg"));
    }

    private String replaceParameter(String param, String newValue)
    {
        URL url = _driver.getURL();
        String file = url.getFile();
        String encodedParam = EscapeUtil.encode(param);
        file = file.replaceAll("&" + Pattern.quote(encodedParam) + "=\\p{Alnum}+?", "");
        if (newValue != null)
            file += "&" + encodedParam + "=" + EscapeUtil.encode(newValue);

        try
        {
            url = new URL(url.getProtocol(), url.getHost(), url.getPort(), file);
        }
        catch (MalformedURLException mue)
        {
            throw new RuntimeException(mue);
        }
        return url.getFile();
    }

    // ======== Side facet panel =========

    public void openSideFilterPanel()
    {
        _driver.click(Locators.headerButton(_regionName, "Filter"));
        _driver.waitForElement(Locators.expandedFacetPanel(_regionName));
        _driver.waitForElement(Locator.css(".lk-filter-panel-label"));
    }

    public void toggleAllFacetsCheckbox()
    {
        _driver.click(Locator.xpath("//div").withClass("lk-filter-panel-label").withText("All"));
    }

    public void clickHeaderButtonByText(String buttonText)
    {
        _driver.waitAndClick(Locator.lkButton(buttonText));
    }

    public void clickHeaderButton(String buttonText, String ... subMenuLabels)
    {
        clickHeaderButton(buttonText, true, subMenuLabels);
    }

    public void openHeaderMenu(String buttonText, String ... subMenuLabels)
    {
        _driver._ext4Helper.clickExt4MenuButton(false, DataRegionTable.Locators.headerMenuButton(_regionName, buttonText), true, subMenuLabels);
    }

    public void clickHeaderButton(String buttonText, boolean wait, String ... subMenuLabels)
    {
        _driver._ext4Helper.clickExt4MenuButton(wait, DataRegionTable.Locators.headerMenuButton(_regionName, buttonText), false, subMenuLabels);
    }

    public List<String> getHeaderButtonSubmenuText(String buttonText)
    {
        List<String> subMenuItems = new ArrayList<>();
        List<WebElement> buttonElements = _driver._ext4Helper.getExt4MenuButtonSubMenu(DataRegionTable.Locators.headerMenuButton(_regionName, buttonText));
        for (WebElement buttonElement : buttonElements)
        {
            subMenuItems.add(buttonElement.getText());
        }
        return subMenuItems;
    }

    public static class Locators extends org.labkey.test.Locators
    {
        public static Locator.XPathLocator dataRegion()
        {
            return Locator.tagWithClass("table", "labkey-data-region").attributeStartsWith("id", "lk-region-");
        }

        public static Locator.XPathLocator dataRegion(String regionName)
        {
            return Locator.tagWithAttribute("table", "lk-region-name", regionName);
        }

        public static Locator.XPathLocator headerButton(String regionName, String text)
        {
            return dataRegion(regionName).append(Locator.tagWithClass("a", "labkey-button").withText(text));
        }

        public static Locator.XPathLocator headerMenuButton(String regionName, String text)
        {
            return dataRegion(regionName).append(Locator.tagWithClass("a", "labkey-menu-button").withText(text));
        }

        private static Locator.XPathLocator facetPanel(String regionName)
        {
            return Locator.tagWithAttribute("div", "lk-region-facet-name", regionName);
        }

        public static Locator.XPathLocator expandedFacetPanel(String regionName)
        {
            return facetPanel(regionName).withDescendant(Locator.xpath("div").withPredicate("not(contains(@class, 'x4-panel-collapsed'))").withClass("labkey-data-region-facet"));
        }

        public static Locator.XPathLocator collapsedFacetPanel(String regionName)
        {
            return facetPanel(regionName).withPredicate(Locator.xpath("div/div").withClass("x4-panel-collapsed").withClass("labkey-data-region-facet"));
        }

        public static Locator.XPathLocator facetRow(String category)
        {
            return Locator.xpath("//div").withClass("x4-grid-body").withPredicate(Locator.xpath("//div").withClass("lk-filter-panel-label").withText(category));
        }

        public static Locator.XPathLocator facetRow(String category, String group)
        {
            return facetRow(category).withPredicate(Locator.xpath("//div").withClass("lk-filter-panel-label").withText(group));
        }

        public static Locator.XPathLocator facetRowCheckbox(String category)
        {
            return facetRow(category).append(Locator.tag("div").withClass("x4-grid-row-checker"));
        }

        public static Locator.XPathLocator facetRowCheckbox(String category, String group)
        {
            return facetRow(category, group).append(Locator.tag("div").withClass("x4-grid-row-checker"));
        }

        public static Locator.XPathLocator columnHeader(String regionName, String fieldName)
        {
            return Locator.tagWithAttribute("td", "column-name", regionName + ":" + fieldName);
        }

        public static Locator.XPathLocator columnHeaderWithLabel(String regionName, String fieldLabel)
        {
            return Locator.id(regionName).append(Locator.tagWithClass("td", "labkey-column-header")).withText(fieldLabel);
        }
    }

    protected class Elements extends ComponentElements
    {
        @Override
        protected SearchContext getContext()
        {
            return getComponentElement();
        }

        private List<WebElement> rows;
        private Map<Integer, List<WebElement>> cells;
        private List<WebElement> columnHeaders;
        private List<WebElement> headerButtons;

        private WebElement header = new LazyWebElement(Locator.id(getTableId() + "-header"), this);
        private WebElement columnHeaderRow = new LazyWebElement(Locator.id(getTableId() + "-column-header-row"), this);

        protected List<WebElement> getRows()
        {
            if (rows == null)
                rows = ImmutableList.copyOf(Locator.css(".labkey-alternate-row, .labkey-row").findElements(this));
            return rows;
        }

        protected WebElement getRow(int row)
        {
            return getRows().get(row);
        }

        protected List<WebElement> getCells(int row)
        {
            if (cells == null)
                cells = new TreeMap<>();
            if (cells.get(row) == null)
                cells.put(row, ImmutableList.copyOf(Locator.css("td").findElements(getRow(row))));
            return cells.get(row);
        }

        protected WebElement getCell(int row, int col)
        {
            return getCells(row).get(col);
        }

        protected List<WebElement> getColumn(int col)
        {
            List<WebElement> columnCells = new ArrayList<>();
            for (int row = 0; row < getRows().size(); row++)
                columnCells.add(getCell(row, col));
            return columnCells;
        }

        protected List<WebElement> getColumnHeaders()
        {
            if (columnHeaders == null)
                columnHeaders = ImmutableList.copyOf(Locator.css("td.labkey-column-header").findElements(columnHeaderRow));
            return columnHeaders;
        }

        protected List<WebElement> getHeaderButtons()
        {
            if (headerButtons == null)
                headerButtons = ImmutableList.copyOf(Locator.css("a.labkey-button, a.labkey-menu-button").findElements(header));
            return headerButtons;
        }
    }
}
