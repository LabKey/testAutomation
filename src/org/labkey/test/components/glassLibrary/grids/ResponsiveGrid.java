/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.glassLibrary.grids;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.util.TestLogger.log;

public class ResponsiveGrid extends WebDriverComponent<ResponsiveGrid.ElementCache>
{
    final WebElement _gridElement;
    private WebDriver _driver;
    private List<Map<String, String>> _gridData;

    protected ResponsiveGrid(WebElement queryGrid, WebDriver driver)
    {
        _gridElement = queryGrid;
        _driver = driver;
        waitForLoaded();
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _gridElement;
    }

    public Boolean isLoaded()
    {
        return !Locators.loadingGrid.existsIn(this) &&
                !Locators.spinner.existsIn(this) &&
                Locator.tag("td").existsIn(this);
    }

    private void waitForLoaded()
    {
        WebDriverWrapper.waitFor(this::isLoaded, "Grid still loading", 30000);
    }

    private void waitForUpdate()
    {
        waitForLoaded();
        clearElementCache();
    }

    @Override
    protected void clearElementCache()
    {
        super.clearElementCache();
        _gridData = null;
    }

    public void doAndWaitForUpdate(Runnable func)
    {
        // Look at WebDriverWrapper.doAndWaitForElementToRefresh for an example.
        func.run();

        waitForUpdate();
    }

    public Boolean hasData()
    {
        return elementCache().emptyGrid.isEmpty();
    }

    public List<WebElement> getRows()
    {
        return elementCache().getRows();
    }

    // TODO Responsive grids cannot be sorted, this should be moved to QueryGrid (or what ever it will be called).
    public ResponsiveGrid sortColumnAscending(String columnName)
    {
        sortColumn(columnName, false);
        return this;
    }

    // TODO Responsive grids cannot be sorted, this should be moved to QueryGrid (or what ever it will be called).
    public ResponsiveGrid sortColumnDescending(String columnName)
    {
        sortColumn(columnName, true);
        return this;
    }

    // TODO Responsive grids cannot be sorted, this should be moved to QueryGrid (or what ever it will be called).
    public ResponsiveGrid sortOn(String column, boolean descending)
    {
        sortColumn(column, descending);
        return this;
    }

    // TODO Responsive grids cannot be sorted, this should be moved to QueryGrid (or what ever it will be called).
    private void sortColumn(String columnName, boolean descending)
    {
        String sortCls = "fa-sort-amount-" + (descending ? "desc" : "asc");
        WebElement headerCell = elementCache().getColumnHeaderCell(columnName);
        Locator.tagWithClass("span", "fa-chevron-circle-down")
                .findElement(headerCell)
                .click();
        WebElement menuItem = Locator.css("li > a > span." + sortCls)
                .findElement(headerCell);
        getWrapper().waitFor(()-> menuItem.isDisplayed(), 1000);
        doAndWaitForUpdate(menuItem::click);
        getWrapper().waitFor(()-> !menuItem.isDisplayed(), 1000);
    }

    // TODO I don't think Responsive grids have a checkbox need to verify with dev. If not this should be moved to QueryGrid (or what ever it will be called).
    public ResponsiveGrid selectRow(int index, boolean checked)
    {
        WebElement checkBox = Locator.css("td > input[type=checkbox]").findElement(getRow(index));
        getWrapper().setCheckbox(checkBox, checked);
        return this;
    }

    // TODO I don't think Responsive grids have a checkbox need to verify with dev. If not this should be moved to QueryGrid (or what ever it will be called).
    public ResponsiveGrid selectRow(String columnName, String columnValue, boolean checked)
    {
        return selectRow(columnName, List.of(columnValue), checked);
    }

    // TODO I don't think Responsive grids have a checkbox need to verify with dev. If not this should be moved to QueryGrid (or what ever it will be called).
    public ResponsiveGrid selectRow(String columnName, List<String> columnValues, boolean checked)
    {
        List<Map<String, String>> gridData = getRowMaps();
        int index = 0;

        for(Map<String, String> rowData : gridData)
        {
            if(columnValues.contains(rowData.get(columnName)))
                selectRow(index, checked);

            index++;
        }

        return this;
    }

    // TODO I don't think Responsive grids have a checkbox need to verify with dev. If not this should be moved to QueryGrid (or what ever it will be called).
    public boolean isRowSelected(int index)
    {
        WebElement checkBox = elementCache().getRowCheckbox(index);
        return checkBox.isSelected();
    }

    // TODO I don't think Responsive grids have a checkbox need to verify with dev. If not this should be moved to QueryGrid (or what ever it will be called).
    public ResponsiveGrid selectAllOnPage(boolean checked)
    {
        Checkbox box = elementCache().selectAllCheckbox;
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(box.getComponentElement()));
        String isIndeterminate = box.getComponentElement().getAttribute("indeterminate");
        if (isIndeterminate != null && isIndeterminate.equals("true"))
        {
            box.check(); // toggle out of indeterminate state
        }
        box.set(checked);
        Locator selectedText = Locator.xpath("//span[contains(text(),'Selected all ')]");

        // If checked is false, so un-selecting a value, don't wait for a confirmation message.
        if(checked)
            getWrapper().waitFor(()-> selectedText.findOptionalElement(getComponentElement()).isPresent(), WAIT_FOR_JAVASCRIPT);

        return this;
    }

    // TODO I don't think Responsive grids have a checkbox need to verify with dev. If not this should be moved to QueryGrid (or what ever it will be called).
    public boolean areElementsSelected()
    {
        Checkbox box = elementCache().selectAllCheckbox;
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(box.getComponentElement()));
        String isIndeterminate = box.getComponentElement().getAttribute("indeterminate");
        return box.isChecked() || (isIndeterminate != null && isIndeterminate.equals("true"));
    }

    private WebElement getRow(int index)
    {
        return getRows().get(index);
    }

    protected WebElement getCell(int rowIndex, int colIndex)
    {
        return Locator.tag("td").index(colIndex).findElement(getRow(rowIndex));
    }

    public Integer getRowIndex(String containsText)
    {
        List<WebElement> rows = getRows();
        for(int i=0; i<rows.size(); i++ )
        {
            if (getRow(i).getText().contains(containsText))
            {
                return i;
            }
        }
        return -1;
    }

    public Integer getRowIndex(String containsText, String searchColumnHeader)
    {
        List<WebElement> rows = getRows();
        int colIndex = getColumnIndex(searchColumnHeader);
        for(int i=0; i<rows.size(); i++ )
        {
            // find the nth td in the row, see if its text matches
            if (getCell(i, colIndex).getText().equalsIgnoreCase(containsText))
            {
                return i;
            }
        }
        return -1;
    }

    public List<String> getColumnDataAsText(String columnHeader)
    {
        List<String> columnData = new ArrayList<>();
        Integer columnIndex = getColumnIndex(columnHeader);

        for (int i = 0; i < getRows().size(); i++)
        {
            columnData.add(getCell(i, columnIndex).getText());
        }

        return columnData;
    }

    public WebElement getRow(String exactColumnText, String columnHeader)
    {
        int colIndex = getColumnIndex(columnHeader);
        return Locator.xpath("//tbody/tr[./td[" + (colIndex + 1) + "]/span[text()=normalize-space('" + exactColumnText + "')]]")
                .findOptionalElement(this).orElseThrow(() -> new NoSuchElementException("Could not find a row where column '" + columnHeader + "' had a value of '" + exactColumnText + "'."));
    }

    public Integer getRowIndex(Locator containing)
    {
        List<WebElement> rows = getRows();
        for(int i=0; i<rows.size(); i++ )
        {
            // find the intended element in the row, if it exists
            WebElement targetRow = getRow(i);
            if (containing.findOptionalElement(targetRow).isPresent())
            {
                return i;
            }
        }
        return -1;
    }

    public Integer getRowIndex(Locator containing, String searchColumnHeader)
    {
        List<WebElement> rows = getRows();
        for(int i=0; i<rows.size(); i++ )
        {
            // find the nth td in the row, see if its text matches
            if (containing.findOptionalElement(getCell(i, getColumnIndex(searchColumnHeader))).isPresent())
            {
                return i;
            }
        }
        return -1;
    }

    // TODO I don't think Responsive grids have a checkbox need to verify with dev. If not this should be moved to QueryGrid (or what ever it will be called).
    // This feels like a bug waiting to happen. The entry in the column name collections are removed if it is the
    // select column. However a similar removal does not happen when getting the text from a row. This causes the
    // getRowMap(WebElement row) function to be off by 1 if the table has a select column.
    public boolean hasSelectColumn()
    {
        return elementCache().selectColumn.isPresent();
    }

    protected Integer getColumnIndex(String columnHeader) // gets the column index t
    {
        List<String> columnTexts = getColumnNames();
        int offset = hasSelectColumn() ? 1 : 0;
        for (int i=0; i< columnTexts.size(); i++ )
        {
            if (columnTexts.get(i).equalsIgnoreCase(columnHeader))
                return i + offset;  // the presence of a select column in the grid will shift everything right by 1
        }
        return -1;
    }

    public List<String> getColumnNames()
    {
        return elementCache().getColumnNames();
    }

    public List<String> getRowValues(int rowIndex)
    {
        // preserves the ordering of the values as they appear in the row.
        if (!hasData())
            throw new IllegalStateException("Attempting to get a row by index, but no rows exist");

        List<String> rowValues = getWrapper().getTexts(Locator.css("td").findElements(getRow(rowIndex)));
        if (hasSelectColumn())
        {
            rowValues.remove(0);
        }
        return rowValues;
    }

    public Map<String, String> getRowMap(int rowIndex)
    {
        HashMap<String, String> map = new HashMap<>();
        List<String> columns = getColumnNames();
        List<String> rowCellTexts = getRowValues(rowIndex);
        for (int i=0; i < columns.size(); i++)
        {
            map.put(columns.get(i), rowCellTexts.get(i));
        }
        return map;
    }

    public Map<String, String> getRowMap(WebElement row)
    {
        List<String> tableColumns = getColumnNames();
        List<String> rowValues = getWrapper().getTexts(Locator.css("td").findElements(row));

        // If it has a select column remove that entry from the rowValues.
        if(hasSelectColumn())
        {
            rowValues.remove(0);
        }

        Map<String, String> rowMap = new HashMap<>();
        for (int i=0; i < tableColumns.size(); i++)
        {
            rowMap.put(tableColumns.get(i), rowValues.get(i));
        }
        return  rowMap;
    }

    public String getCellValue(int rowIndex, String columnHeader)
    {
        return getRowMap(rowIndex).get(columnHeader);
    }

    public String getCellValue(WebElement row, String columnHeader)
    {
        return getRowMap(row).get(columnHeader);
    }

    public List<Map<String, String>> getRowMaps()
    {
        if(null == _gridData)
        {
            _gridData = _initGridData();
        }
        return _gridData;
    }

    /**
     * Call this function to force a re-initialization of the internal data representation of the grid data.
     * When trying to be more efficient the grid data is stored in an internal variable (so this is a stateful object).
     * On creates the internal grid data is initialize by calling waitForLoaded. The waitForLoaded function  is also
     * called when the page/grid is navigated, but it is not when a search, or ordering is done. As a temporary work
     * around this function is made public so the calling function can update the data.
     *
     * The real fix would be to add an event listener to the grid and reinitialize the internal data when it detects a change.
     *
     */
    public void initGridData()
    {
        waitForLoaded();
        _gridData = _initGridData();
    }

    private List<Map<String, String>> _initGridData()
    {
        List<Map<String, String>> rowMaps = new ArrayList<>();
        List<WebElement> rows = getRows();
        for(int i=0; i < rows.size(); i++)
        {
            rowMaps.add(getRowMap(i));
        }
        return rowMaps;
    }

    private WebElement getLink(String text)
    {
        log("seeking link with text [" + text + "]");
        WebElement link = getComponentElement().findElement(By.partialLinkText(text));
        getWrapper().scrollIntoView(link);
        log("found element with text [" + link.getText() + "]");
        return link;

    }

    public void clickLink(String text)
    {
        getLink(text).click();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public ElementCache()
        {
            waitForLoaded();
        }

        protected Optional<WebElement> emptyGrid = Locators.emptyGrid.findOptionalElement(this);

        public WebElement button = Locator.xpath("//button[contains(@class,'dropdown-toggle')]").findWhenNeeded(this);

        // TODO I don't think Responsive grids have a checkbox need to verify with dev. If not this should be moved to QueryGrid (or what ever it will be called).
        public Optional<WebElement> selectColumn = Locator.xpath("//th/input[@type='checkbox']").findOptionalElement(getComponentElement());
        Checkbox selectAllCheckbox = Checkbox.Checkbox(Locator.xpath("//th/input[@type='checkbox']")).findWhenNeeded(this);

        private final Map<String, WebElement> headerCells = new HashMap<>();
        protected final WebElement getColumnHeaderCell(String headerText)
        {
            if (!headerCells.containsKey(headerText))
            {
                WebElement headerCell = Locator.xpath("//th[./span[contains(text(), '" + headerText + "')]]").findElement(this);
                headerCells.put(headerText, headerCell);
            }
            return headerCells.get(headerText);
        }

        private List<String> columnNames;
        protected List<String> getColumnNames()
        {
            if (columnNames == null)
            {
                List<WebElement> headerCellElements = Locators.headerCells.findElements(this);
                if (hasSelectColumn())
                {
                    headerCellElements.remove(0);
                }
                columnNames = getWrapper().getTexts(headerCellElements);
                for (int i = 0; i < headerCellElements.size(); i++)
                {
                    headerCells.put(columnNames.get(i), headerCellElements.get(i)); // Fill out the headerCells Map since we have them all
                }
            }
            return columnNames;
        }

        private List<WebElement> rows;
        protected List<WebElement> getRows()
        {
            if (rows == null)
            {
                rows = Locators.rows.findElements(getComponentElement());
            }
            return rows;
        }

        // TODO I don't think Responsive grids have a checkbox need to verify with dev. If not this should be moved to QueryGrid (or what ever it will be called).
        private final Map<Integer, WebElement> rowCheckboxes = new HashMap<>();
        protected final WebElement getRowCheckbox(Integer rowIndex)
        {
            if (!rowCheckboxes.containsKey(rowIndex))
            {
                WebElement rowCheckbox = Locator.xpath("//td/input[@type='checkbox']").findElement(getRows().get(rowIndex));
                rowCheckboxes.put(rowIndex, rowCheckbox);
            }
            return rowCheckboxes.get(rowIndex);
        }
    }

    protected static abstract class Locators
    {
        static public Locator.XPathLocator responsiveGrid()
        {
            return Locator.byClass("table-responsive");
        }

        static public Locator.XPathLocator responsiveGrid(String gridId)
        {
            return responsiveGrid().withAttribute("data-gridid", gridId);
        }

        static public Locator.XPathLocator responsiveGridByBaseId(String baseGridId)
        {
            return responsiveGrid().attributeStartsWith("data-gridid", baseGridId);
        }

        static final Locator loadingGrid = Locator.css("tbody tr.grid-loading");
        static final Locator emptyGrid = Locator.css("tbody tr.grid-empty");
        static final Locator spinner = Locator.css("span i.fa-spinner");
        static final Locator.XPathLocator rows = Locator.tag("tbody").childTag("tr").withoutClass("grid-empty").withoutClass("grid-loading");
        static final Locator headerCells = Locator.xpath("//thead/tr/th");

    }

    public static class ResponsiveGridFinder extends WebDriverComponentFinder<ResponsiveGrid, ResponsiveGridFinder>
    {
        private Locator _locator;

        public ResponsiveGridFinder(WebDriver driver)
        {
            super(driver);
            _locator= Locators.responsiveGrid();
        }

        @Override
        protected ResponsiveGridFinder getThis()
        {
            return this;
        }

        @Override
        protected ResponsiveGrid construct(WebElement el, WebDriver driver)
        {
            return new ResponsiveGrid(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
