/*
 * Copyright (c) 2007-2015 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
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
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DataRegionTable
{
    protected final String _tableName;
    protected BaseWebDriverTest _test;
    protected final boolean _selectors;
    protected final Map<String, Integer> _mapColumns = new HashMap<>();
    protected final Map<String, Integer> _mapRows = new HashMap<>();
    protected final int _columnCount;
    protected final boolean _floatingHeaders;

    public DataRegionTable(String tableName, BaseWebDriverTest test)
    {
        this(tableName, test, true, true);
    }

    public DataRegionTable(String tableName, BaseWebDriverTest test, boolean selectors)
    {
        this(tableName, test, selectors, true);
    }

    public DataRegionTable(String tableName, BaseWebDriverTest test, boolean selectors, boolean floatingHeaders)
    {
        _tableName = tableName;
        _selectors = selectors;
        _test = test;
        _test.waitForElement(Locator.xpath("//table[@id=" + Locator.xq(getHtmlName()) + "]"));
        _columnCount = _test.getTableColumnCount(getHtmlName());
        _floatingHeaders = floatingHeaders;
    }

    private int getHeaderRowCount()
    {
        return 2 + (_floatingHeaders ? 2 : 0);
    }

    public static String getTableNameByTitle(String title, BaseWebDriverTest test)
    {
        return Locator.xpath("//th[@title='" + title +"']/../..//table").findElement(test.getDriver()).getAttribute("id").replace("dataregion_", "");
    }

    public static DataRegionTable findDataRegion(BaseWebDriverTest test)
    {
        return findDataRegion(test, 0);
    }

    public static DataRegionTable findDataRegion(BaseWebDriverTest test, int index)
    {
        return findDataRegionWithin(test, test.getDriver(), index);
    }

    public static DataRegionTable findDataRegionWithin(BaseWebDriverTest test, SearchContext context)
    {
        return findDataRegionWithin(test, context, 0);
    }

    public static DataRegionTable findDataRegionWithin(BaseWebDriverTest test, SearchContext context, int index)
    {
        Locator.CssLocator dataRegionLoc = Locator.css("table.labkey-data-region[id^='dataregion_']");
        List<WebElement> dataRegions = dataRegionLoc.findElements(context);
        if (dataRegions.size() > index)
            return new DataRegionTable(dataRegions.get(index).getAttribute("id").replace("dataregion_", ""), test);
        else
            throw new NoSuchElementException(String.format("Not enough data regions. Index: %d, Count: %d", index, dataRegions.size()));
    }

    public static String getQueryWebPartName(BaseWebDriverTest test)
    {
        return getQueryWebPartName(0, test);
    }

    public static String getQueryWebPartName(int qwpIndex, BaseWebDriverTest test)
    {
        Locator qwpLocator = Locator.tag("table").attributeStartsWith("id", "dataregion_aqwp");
        test.waitForElement(qwpLocator);

        List<WebElement> qwps = qwpLocator.findElements(test.getDriver());
        if (qwps.size() > qwpIndex)
            return qwps.get(qwpIndex).getAttribute("id").replace("dataregion_", "");
        else
            throw new NoSuchElementException(String.format("Not enough qwps on page. Looking for: %d, Found: %d", qwpIndex, qwps.size()));
    }

    public String getTableName()
    {
        return _tableName;
    }

    public String getHtmlName()
    {
        return "dataregion_" + _tableName;
    }

    public Locator.IdLocator locator()
    {
        return Locator.id(getHtmlName());
    }

    public int getColumnCount()
    {
        return _columnCount;
    }

    private boolean bottomBarPresent()
    {
        return _test.isElementPresent(Locator.xpath("//table[starts-with(@id, 'dataregion_footer_')]"));
    }

    public boolean hasAggregateRow()
    {
        return _test.isElementPresent(Locator.xpath("//table[@id=" + Locator.xq(getHtmlName()) + "]//tr[contains(@class, 'labkey-col-total')]"));
    }

    public List<WebElement> getHeaderButtons()
    {
        Locator.CssLocator allButtonsLoc = Locator.css("#dataregion_header_row_" + _tableName + " a.labkey-button, #dataregion_header_row_" + _tableName + " a.labkey-menu-button");
        List<WebElement> headerButtons = allButtonsLoc.findElements(_test.getDriver());

        return headerButtons;
    }

    public int getDataRowCount()
    {
        int rows = 0;
        rows = _test.getTableRowCount(getHtmlName()) - (getHeaderRowCount() + (bottomBarPresent()?1:0));
        if (hasAggregateRow())
            rows -= 1;

        if (rows == 1 && hasNoDataToShow())
            rows = 0;

        return rows;
    }

    public int getRow(String columnLabel, String value)
    {
        return getRow(getColumn(columnLabel), value);
    }

    public int getRow(int columnIndex, String value)
    {
        int rowCount = getDataRowCount();
        for(int i=0; i<rowCount; i++)
        {
            if(value.equals(getDataAsText(i, columnIndex)))
                return i;
        }
          return -1;
    }

    public String getTotal(String columnLabel)
    {
        return getTotal(getColumn(columnLabel));
    }

    public String getTotal(int columnIndex)
    {
        return _test.getText(Locator.css("#" + getHtmlName() + " tr.labkey-col-total > td:nth-of-type(" + (columnIndex + (_selectors ? 2 : 1)) + ")"));
    }

    /**
     * do nothing if column is already present, add it if it is not
     * @param columnName   name of column to add, if necessary
     */
    public void ensureColumnPresent(String columnName)
    {
        if(getColumn(columnName) < 0)
        {
            _test._customizeViewsHelper.openCustomizeViewPanel();
            _test._customizeViewsHelper.addCustomizeViewColumn(columnName);
            _test._customizeViewsHelper.applyCustomView();
        }
    }

    /**
     * check for presence of columns, add them if they are not already present
     * requires columns actually exist
     * @param names names of columns to add
     */
    public void ensureColumnsPresent(String... names)
    {
        boolean opened = false;
        for(String name: names)
        {
            if(getColumn(name) == -1)
            {
                if(!opened)
                {
                    _test._customizeViewsHelper.openCustomizeViewPanel();
                    opened = true;
                }
                _test._customizeViewsHelper.addCustomizeViewColumn(name);
            }
        }
        if(opened)
            _test._customizeViewsHelper.applyCustomView();
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

    public int getDataRowCount(int div)
    {
        int rows = 0;
        while (getDataAsText(rows, 0) != null)
            rows += div;

        if (rows == 1 && "No data to show.".equals(getDataAsText(0, 0)))
            rows = 0;

        return rows;
    }

    public Locator.XPathLocator detailsXpath(int row)
    {
        return Locator.xpath("//table[@id=" + Locator.xq(getHtmlName()) + "]/tbody/tr[" + (row + getHeaderRowCount() + 1) + "]/td[contains(@class, 'labkey-details')]");
    }

    public Locator.XPathLocator detailsLink(int row)
    {
        Locator.XPathLocator cell = detailsXpath(row);
        return cell.child("a[1]");
    }

    public Locator.XPathLocator updateXpath(int row)
    {
        return Locator.xpath("//table[@id=" + Locator.xq(getHtmlName()) + "]/tbody/tr[" + (row + getHeaderRowCount() + 1) + "]/td[contains(@class, 'labkey-update')]");
    }

    public Locator.XPathLocator updateLink(int row)
    {
        Locator.XPathLocator cell = updateXpath(row);
        return cell.child("a[1]");
    }

    public Locator.XPathLocator xpath(int row, int col)
    {
        return Locator.xpath("//table[@id=" + Locator.xq(getHtmlName()) + "]/tbody/tr[" + (row + getHeaderRowCount() + 1) + "]/td[" + (col + 1 + (_selectors ? 1 : 0)) + "]");
    }

    public Locator.XPathLocator link(int row, int col)
    {
        Locator.XPathLocator cell = xpath(row, col);
        return cell.child("a[1]");
    }

    public Locator.XPathLocator link(int row, String columnName)
    {
        int col = getColumn(columnName);
        if (col == -1)
            fail("Couldn't find column '" + columnName + "'");
        return link(row, col);
    }

    public int getColumn(String name)
    {
        name = name.replaceAll(" ", "");

        if (_mapColumns.containsKey(name))
            return _mapColumns.get(name);

        getColumnHeaders();

        if (_mapColumns.containsKey(name))
            return _mapColumns.get(name);

        _test.log("Column '" + name + "' not found");
        return -1;
    }

    public List<String> getColumnHeaders()
    {
        List<String> columnHeaders = new ArrayList<>();
        _mapColumns.clear(); // Start fresh

        for (int col = 0; col < _columnCount; col++)
        {
            String header = getDataAsText(-(getHeaderRowCount()/2), col);
            columnHeaders.add(header);
            if( header != null )
            {
                String headerName = header.split("\n")[0];
                headerName = headerName.replaceAll(" ", "");
                if (!StringUtils.isEmpty(headerName)
                        && !_mapColumns.containsKey(headerName)) // Remember only the first occurrence of each column label
                    _mapColumns.put(headerName, col);
            }
        }

        return columnHeaders;
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
        int col = getColumn(name);
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
        boolean moreThanOnePage = _test.isElementPresent(Locator.linkWithText("Next"));

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

    /** Find the row number for the given primary key. */
    public int getRow(String pk)
    {
        assertTrue("Need the selector checkbox's value to find the row with the given pk", _selectors);

        Integer cached = _mapRows.get(pk);
        if (cached != null)
            return cached.intValue();

        int row = 0;
        try
        {
            while (true)
            {
                String value = _test.getAttribute(Locator.xpath("//table[@id=" + Locator.xq(getHtmlName()) +"]//tr[" + (row+5) + "]//input[@name='.select']"), "value");
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
        return "No data to show.".equals(_getDataAsText(getHeaderRowCount(), 0));
    }

    public String getDataAsText(int row, int column)
    {
        return _getDataAsText(row + getHeaderRowCount(), column + (_selectors ? 1 : 0));
    }

    // Doesn't adjust for header rows or selector columns.
    private String _getDataAsText(int row, int column)
    {
        String ret = null;

        try
        {
            ret = _test.getTableCellText(getHtmlName(), row, column);
        }
        catch (NoSuchElementException ignore) {}

        return ret;
    }

    public String getDataAsText(int row, String columnName)
    {
        int col = getColumn(columnName);
        if (col == -1)
            return null;
        return getDataAsText(row, col);
    }

    public String getDataAsText(String pk, String columnName)
    {
        int row = getRow(pk);
        if (row == -1)
            return null;
        int col = getColumn(columnName);
        if (col == -1)
            return null;
        return getDataAsText(row, col);
    }

    public String getDetailsHref(int row)
    {
        Locator l = detailsLink(row);
        return _test.getAttribute(l, "href");
    }

    public String getUpdateHref(int row)
    {
        Locator l = updateLink(row);
        return _test.getAttribute(l, "href");
    }

    public String getHref(int row, int column)
    {
        // headerRows and selector offsets are applied in link() locator
        return _getHref(row, column);
    }

    private String _getHref(int row, int column)
    {
        Locator l = link(row, column);
        return _test.getAttribute(l, "href");
    }

    public String getHref(int row, String columnName)
    {
        int col = getColumn(columnName);
        if (col == -1)
            return null;
        return getHref(row, col);
    }

    public String getHref(String pk, String columnName)
    {
        int row = getRow(pk);
        if (row == -1)
            return null;
        int col = getColumn(columnName);
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
        _test.assertElementPresent(cell);

        Locator link = link(row, column);
        return _test.isElementPresent(link);
    }

    public boolean hasHref(int row, String columnName)
    {
        int col = getColumn(columnName);
        if (col == -1)
            fail("Column '" + columnName + "' not found.");
        return hasHref(row, col);
    }

    public void setSort(String columnName, SortDirection direction)
    {
        _test.setSort(_tableName, columnName, direction);
    }

    public void clearSort(String columnName)
    {
        _test.clearSort(_tableName, columnName);
    }

    public void openFilterDialog(String columnName)
    {
        Locator.XPathLocator menuLoc = DataRegionTable.Locators.columnHeader(getTableName(), columnName);
        String columnLabel = _test.getText(menuLoc);
        _test._ext4Helper.clickExt4MenuButton(false, menuLoc, false, "Filter...");

        final Locator.XPathLocator filterDialog = ExtHelper.Locators.window("Show Rows Where " + columnLabel + "...");
        _test.waitForElement(filterDialog);

        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return _test.isElementPresent(filterDialog.append(Locator.linkWithText("[All]")).notHidden())||
                       _test.isElementPresent(filterDialog.append(Locator.tagWithId("input", "value_1").notHidden()));
            }
        }, "Filter Dialog", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test._extHelper.waitForLoadingMaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void setFilter(String columnName, String filterType, String filter)
    {
        setFilter(columnName, filterType, filter, BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void setFilter(String columnName, String filterType, String filter, int waitMillis)
    {
        _test.setFilter(_tableName, columnName, filterType, filter, waitMillis);
    }

    public void clearFilter(String columnName)
    {
        _test.clearFilter(_tableName, columnName);
    }

    public void clearFilter(String columnName, int waitMillis)
    {
        _test.clearFilter(_tableName, columnName, waitMillis);
    }

    public void clearAllFilters(String columnName)
    {
        _test.clearAllFilters(_tableName, columnName);
    }

    public void checkAllOnPage()
    {
        checkAll();
    }

    public void uncheckAllOnPage()
    {
        uncheckAll();
    }

    public void checkAll()
    {
        WebElement toggleAll = Locators.dataRegion(getTableName()).append(Locator.tagWithName("input", ".toggle")).findElement(_test.getDriver());
        _test.uncheckCheckbox(toggleAll);
        _test.shortWait().until(LabKeyExpectedConditions.animationIsDone(toggleAll));
        _test.checkCheckbox(toggleAll);
        _test.shortWait().until(LabKeyExpectedConditions.animationIsDone(toggleAll));
    }

    public void uncheckAll()
    {
        WebElement toggleAll = Locators.dataRegion(getTableName()).append(Locator.tagWithName("input", ".toggle")).findElement(_test.getDriver());
        _test.checkCheckbox(toggleAll);
        _test.shortWait().until(LabKeyExpectedConditions.animationIsDone(toggleAll));
        _test.uncheckCheckbox(toggleAll);
        _test.shortWait().until(LabKeyExpectedConditions.animationIsDone(toggleAll));
    }

    private void waitForAllChecked()
    {
        _test.waitForElementToDisappear(Locator.css("#" + getHtmlName() + " input:not(:checked)[name='.select']"));
    }

    private void waitForAllUnchecked()
    {
        _test.waitForElementToDisappear(Locator.css("#" + getHtmlName() + " input:checked[name='.select']"));
    }

    // NOTE: this method would be better named checkCheckboxByPrimaryKey --> while it does take a string, this string will often be a string value
    public void checkCheckbox(String value)
    {
        _test.checkDataRegionCheckbox(_tableName, value);
    }

    public void checkCheckbox(int index)
    {
        _test.checkDataRegionCheckbox(_tableName, index);
    }

    public void uncheckCheckbox(int index)
    {
        _test.uncheckDataRegionCheckbox(_tableName, index);
    }

    public void pageFirst()
    {
        _test.dataRegionPageFirst(_tableName);
    }

    public void pageLast()
    {
        _test.dataRegionPageLast(_tableName);
    }

    public void pageNext()
    {
        _test.dataRegionPageNext(_tableName);
    }

    public void pagePrev()
    {
        _test.dataRegionPagePrev(_tableName);
    }

    public void showAll()
    {
        clickHeaderButton("Page Size", "Show All");
    }

    public void setPageSize(int size)
    {
        clickHeaderButton("Page Size", size + " per page");
    }

    /**
     * Set the current offset by manipulating the url rather than using the pagination buttons.
     * @param offset
     */
    public void setOffset(int offset)
    {
        String url = replaceParameter(_tableName + ".offset", String.valueOf(offset));
        _test.beginAt(url);
    }

    /**
     * Set the page size by manipulating the url rather than using the "XXX per page" menu items.
     * @param size new page size
     */
    public void setMaxRows(int size)
    {
        String url = replaceParameter(_tableName + ".maxRows", String.valueOf(size));
        _test.beginAt(url);
    }

    @LogMethod
    public void createQuickChart(String columnName)
    {
        _test._ext4Helper.clickExt4MenuButton(true, DataRegionTable.Locators.columnHeader(_tableName, columnName), false, "Quick Chart");
        _test.waitForElement(Locator.css("svg"));
    }

    private String replaceParameter(String param, String newValue)
    {
        URL url = _test.getURL();
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
        _test.click(Locators.headerButton(_tableName, "Filter"));
        _test.waitForElement(Locators.expandedFacetPanel(_tableName));
        _test.waitForElement(Locator.css(".lk-filter-panel-label"));
    }

    public void toggleAllFacetsCheckbox()
    {
        _test.click(Locator.xpath("//div").withClass("lk-filter-panel-label").withText("All"));
    }

    public void clickHeaderButtonByText(String buttonText)
    {
        _test.waitAndClick(Locators.headerButton(_tableName, buttonText));
    }

    public void clickHeaderButton(String buttonText, String ... subMenuLabels)
    {
        clickHeaderButton(buttonText, true, subMenuLabels);
    }

    public void clickHeaderButton(String buttonText, boolean wait, String ... subMenuLabels)
    {
        _test._ext4Helper.clickExt4MenuButton(wait, DataRegionTable.Locators.headerMenuButton(_tableName, buttonText), false, subMenuLabels);
    }

    public List<String> getHeaderButtonSubmenuText(String buttonText)
    {
        List<String> subMenuItems = new ArrayList<>();
        List<WebElement> buttonElements = _test._ext4Helper.getExt4MenuButtonSubMenu(DataRegionTable.Locators.headerMenuButton(_tableName, buttonText));
        for(WebElement buttonElement : buttonElements)
        {
            subMenuItems.add(buttonElement.getText());
        }
        return subMenuItems;
    }

    public static class Locators
    {
        public static Locator.XPathLocator dataRegion(String tableName)
        {
            return Locator.tagWithId("table", "dataregion_" + tableName);
        }

        public static Locator.XPathLocator headerButton(String tableName, String text)
        {
            return Locator.xpath("id('dataregion_header_row_" + tableName + "')//a").withClass("labkey-button").withText(text);
        }

        public static Locator.XPathLocator headerMenuButton(String tableName, String text)
        {
            return Locator.xpath("id('dataregion_header_row_" + tableName + "')//a").withClass("labkey-menu-button").withText(text);
        }

        public static Locator.IdLocator facetPanel(String tableName)
        {
            return Locator.id("dataregion_facet_" + tableName);
        }

        public static Locator.XPathLocator expandedFacetPanel(String tableName)
        {
            return facetPanel(tableName).withDescendant(Locator.xpath("div").withPredicate("not(contains(@class, 'x4-panel-collapsed'))").withClass("labkey-data-region-facet"));
        }

        public static Locator.XPathLocator collapsedFacetPanel(String tableName)
        {
            return facetPanel(tableName).withPredicate(Locator.xpath("div/div").withClass("x4-panel-collapsed").withClass("labkey-data-region-facet"));
        }

        public static Locator.XPathLocator facetRow(String category)
        {
            return Locator.xpath("//div").withClass("x4-grid-body").withPredicate(Locator.xpath("//div").withClass("lk-filter-panel-label").withText(category));
        }

        public static Locator.XPathLocator facetRow(String category, String group)
        {
            return facetRow(category).withPredicate(Locator.xpath("//div").withClass("lk-filter-panel-label").withText(group));
        }

        public static Locator.XPathLocator faceRowCheckbox(String category)
        {
            return facetRow(category).append(Locator.tag("div").withClass("x4-grid-row-checker"));
        }

        public static Locator.XPathLocator faceRowCheckbox(String category, String group)
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
}
