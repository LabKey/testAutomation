/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.glassLibrary.grids;

import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;

public class ResponsiveGrid extends WebDriverComponent<ResponsiveGrid.ElementCache>
{
    final WebElement _gridElement;
    private WebDriver _driver;

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

    /**
     * Sorts from the grid header menu (rather than from the omnibox)
     * @param columnName
     * @return
     */
    public ResponsiveGrid sortColumnAscending(String columnName)
    {
        doAndWaitForUpdate(()->
            sortColumn(columnName, SortDirection.ASC));
        return this;
    }

    /**
     * Sorts from the grid header menu (rather than from the omnibox)
     * @param columnName
     * @return
     */
    public ResponsiveGrid sortColumnDescending(String columnName)
    {
        doAndWaitForUpdate(()->
            sortColumn(columnName, SortDirection.DESC));
        return this;
    }

    private void sortColumn(String columnName, SortDirection direction)
    {
        String sortCls = "fa-sort-amount-" + (direction.equals(SortDirection.DESC) ? "desc" : "asc");
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

    public ResponsiveGrid selectRow(int index, boolean checked)
    {
        getRow(index).select(checked);
        return this;
    }

    public ResponsiveGrid selectRow(String columnName, String columnValue, boolean checked)
    {
        getRow(columnValue, columnName).get().select(checked);
        return this;
    }

    // TODO consider refactoring to use selectRow above in tests
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

    /**
     * where possible, find ways to identify rows other than via index
     * use
     * @param index
     * @return
     */
    @Deprecated
    public boolean isRowSelected(int index)
    {
       return new GridRow.GridRowFinder(this).index(index).find(this).isSelected();
    }

    /**
     * returns whether or not the selector checkbox in the first row containing the specified text
     * @param text
     * @return
     */
    public boolean isRowSelected(String text)
    {
        return new GridRow.GridRowFinder(this).withCellWithText(text)
                .find(this).isSelected();
    }

    /**
     * returns whether or not the checkbox in the specified row is checked
     * @param text
     * @param column
     * @return
     */
    public boolean isRowSelected(String text, String column)
    {
        return new GridRow.GridRowFinder(this).withTextAtColumn(text, getColumnIndex(column))
                .find(this).isSelected();
    }

    protected Checkbox selectAllBox()
    {
        Checkbox box = elementCache().selectAllCheckbox;
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(box.getComponentElement()));
        return box;
    }

    /**
     * Use this method to know if the 'selectAllBox' is checked.
     * Note: it has three possible states:
     *      checked (meaning all rows on the page are selected)
     *      indeterminate (meaning some rows are selected but others are not)
     *      unchecked (meaning no rows on the page are selected)
     * @return
     */
    public boolean areAllRowsOnPageSelected()
    {
        Checkbox box = selectAllBox();

        String isIndeterminate = box.getComponentElement().getAttribute("indeterminate");
        return box.isChecked() || (isIndeterminate != null && isIndeterminate.equals("true"));
    }

    public ResponsiveGrid selectAllOnPage(boolean checked)
    {
        Checkbox box = selectAllBox();
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

    /**
     * Returns a list of visible GridRows that are selected
     * @return
     */
    public List<GridRow> getSelectedRows()
    {
        return new GridRow.GridRowFinder(this).withCheckedBox().findAll(this);
    }

    private GridRow getRow(int index)
    {
        return new GridRow.GridRowFinder(this).index(index).find(this);
    }

    /**
     * Returns the first row containing a cell with matching full text
     * @param containsText
     * @return
     */
    public Optional<GridRow> getRow(String containsText)
    {
        return new GridRow.GridRowFinder(this).withCellWithText(containsText).findOptional(this);
    }

    /**
     * Returns the first row with matching text in the specified column
     * @param containsText The full text of the cell to match
     * @param columnHeader The exact text of the column header
     * @return
     */
    public Optional<GridRow> getRow(String containsText, String columnHeader)
    {
        // try to normalize column index to start at 0, excluding row selector column
        Integer columnIndex = getColumnIndex(columnHeader);
        return new GridRow.GridRowFinder(this).withTextAtColumn(containsText, columnIndex)
                .findOptional(this);
    }

    /**
     * Returns the first row containing a descendant matching the supplied locator
     * @param containing
     * @return
     */
    public Optional<GridRow> getRow(Locator.XPathLocator containing)
    {
        return new GridRow.GridRowFinder(this).withDescendant(containing).findOptional();
    }

    public List<GridRow> getRows()
    {
        return new GridRow.GridRowFinder(this).findAll(getComponentElement());
    }

    public List<String> getColumnDataAsText(String columnHeader)
    {
        List<String> columnData = new ArrayList<>();
        for (GridRow row : getRows())
        {
            columnData.add(row.getText(columnHeader));
        }
        return columnData;
    }

    // TODO I don't think Responsive grids have a checkbox need to verify with dev. If not this should be moved to QueryGrid (or what ever it will be called).
    // This feels like a bug waiting to happen. The entry in the column name collections are removed if it is the
    // select column. However a similar removal does not happen when getting the text from a row. This causes the
    // getRowMap(WebElement row) function to be off by 1 if the table has a select column.
    public boolean hasSelectColumn()
    {
        return elementCache().selectColumn.isPresent();
    }

    /**
     * used to find the raw index of a given column as rendered in the dom.
     * To get the normalized index (which excludes selector rows if present) use
     * elementCache().indexes.get(column).get("normalizedIndex")
     */
    protected Integer getColumnIndex(String columnHeader)
    {
        if (elementCache().indexes == null)
        {
            getColumnNames();
        }

        if (elementCache().indexes.containsKey(columnHeader))
            return elementCache().indexes.get(columnHeader).get("rawIndex");
        else
            return -1;
    }

    public List<String> getColumnNames()
    {
        elementCache().initColumnsAndIndices();
        return elementCache().columnNames;
    }

    /**
     * there are ways to get rowMaps without exposing indexes to outside use
     * @param rowIndex
     * @return
     */
    @Deprecated
    public List<String> getRowTexts(int rowIndex)
    {
        // preserves the ordering of the values as they appear in the row.
        if (!hasData())
            throw new IllegalStateException("Attempting to get a row by index, but no rows exist");

        return getRow(rowIndex).getTexts();
    }

    /**
     * we should avoid having test code rely on row indexes; find the row by text/column where possible
     * @param rowIndex
     * @return
     */
    @Deprecated
    public Map<String, String> getRowMap(int rowIndex)
    {
        return getRow(rowIndex).getRowMap();
    }

    /**
     * Where possible, avoid using row index in test code; use text/column or other means to get a GridRow
     * @param rowIndex
     * @param columnHeader
     * @return
     */
    @Deprecated
    public String getCellText(int rowIndex, String columnHeader)
    {
        return getRow(rowIndex).getText(columnHeader);
    }

    public List<Map<String, String>> getRowMaps()
    {
        if(null == elementCache().mapList)
        {
            elementCache().mapList = elementCache()._initGridData();
        }
        return elementCache().mapList;
    }

    public void clickLink(String text)
    {
        getRow(text).orElseThrow(()-> new NotFoundException("Did not find a row with a link with text ["+text+"]"))
                .clickLink(text);
    }

    public void clickLink(String text, String column)
    {
        getRow(text, column)
                .orElseThrow(()-> new NotFoundException("Did not find a row with a link with text ["+text+"] at column ["+column+"]"))
                .clickLink(text);
    }

    public boolean gridMessagePresent()
    {
        return Locator.tagWithClass("div", "grid-messages")
                .withChild(Locator.tagWithClass("div", "grid-message")).existsIn(this);
    }

    public List<String> getGridMessages()
    {
        return getWrapper().getTexts(Locator.tagWithClass("div", "grid-messages")
                .child(Locator.tagWithClass("div", "grid-message")).findElements(this));
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
        elementCache().mapList = elementCache()._initGridData();
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
        private Map<String, Map<String, Integer>> indexes;
        protected void initColumnsAndIndices()
        {
            if (columnNames == null || indexes == null)
            {
                List<WebElement> headerCellElements = Locators.headerCells.findElements(this);
                int offset = 0;
                if (hasSelectColumn())
                {
                    headerCellElements.remove(0);
                    offset = 1;
                }
                columnNames = getWrapper().getTexts(headerCellElements);
                indexes = new HashMap<>();
                for (int i = 0; i < headerCellElements.size(); i++)
                {
                    headerCells.put(columnNames.get(i), headerCellElements.get(i)); // Fill out the headerCells Map since we have them all
                    indexes.put(columnNames.get(i), Map.of("normalizedIndex", i, "rawIndex", i + offset));
                }
            }
        }

        protected List<Map<String, String>> mapList;
        protected List<GridRow> gridRows;
        private List<Map<String, String>> _initGridData()
        {
            List<Map<String, String>> rowMaps = new ArrayList<>();
            elementCache().gridRows = getRows();
            for(GridRow row : elementCache().gridRows)
            {
                rowMaps.add(row.getRowMap());
            }
            return rowMaps;
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

        public ResponsiveGridFinder inParentWithId(String id)
        {
            _locator = Locator.id(id).withChild(Locators.responsiveGrid());
            return this;
        }

        public ResponsiveGridFinder withGridId(String id)
        {
            _locator = Locators.responsiveGrid(id);
            return this;
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
