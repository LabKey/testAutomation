/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.glassLibrary.grids;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.ReactCheckBox;
import org.labkey.test.components.react.UpdatingComponent;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.waitFor;

public class ResponsiveGrid<T extends ResponsiveGrid> extends WebDriverComponent<ResponsiveGrid.ElementCache> implements UpdatingComponent
{
    final WebElement _gridElement;
    final WebDriver _driver;

    protected ResponsiveGrid(WebElement queryGrid, WebDriver driver)
    {
        _gridElement = queryGrid;
        _driver = driver;
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
        return getComponentElement().isDisplayed() &&
                !Locators.loadingGrid.existsIn(this) &&
                !Locators.spinner.existsIn(this) &&
                Locator.tag("td").existsIn(this);
    }

    protected void waitForLoaded()
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
     * @param columnLabel column header for
     * @return
     */
    public ResponsiveGrid sortColumnAscending(String columnLabel)
    {
        doAndWaitForUpdate(()->
            sortColumn(columnLabel, SortDirection.ASC));
        return getThis();
    }

    /**
     * Sorts from the grid header menu (rather than from the omnibox)
     * @param columnLabel
     * @return
     */
    public ResponsiveGrid sortColumnDescending(String columnLabel)
    {
        doAndWaitForUpdate(()->
            sortColumn(columnLabel, SortDirection.DESC));
        return getThis();
    }

    private void sortColumn(String columnLabel, SortDirection direction)
    {
        String sortCls = "fa-sort-amount-" + (direction.equals(SortDirection.DESC) ? "desc" : "asc");
        WebElement headerCell = elementCache().getColumnHeaderCell(columnLabel);
        Locator.tagWithClass("span", "fa-chevron-circle-down")
                .findElement(headerCell)
                .click();
        WebElement menuItem = Locator.css("li > a > span." + sortCls)
                .findElement(headerCell);
        getWrapper().waitFor(()-> menuItem.isDisplayed(), 1000);
        doAndWaitForUpdate(menuItem::click);
        getWrapper().waitFor(()-> !menuItem.isDisplayed(), 1000);
    }

    /**
     * where possible, use text, column or other strategies to get the row
     * @param index
     * @param checked
     * @return
     */
    @Deprecated
    public ResponsiveGrid selectRow(int index, boolean checked)
    {
        getRow(index).select(checked);
        return getThis();
    }

    /**
     * Finds the first row with the specified texts in the specified columns, and sets its checkbox
     * @param partialMap    key-column, value-text in that column
     * @param checked       the desired checkbox state
     * @return
     */
    public ResponsiveGrid selectRow(Map<String, String> partialMap, boolean checked)
    {
        GridRow row = getRow(partialMap);
        selectRowAndVerifyCheckedCounts(row, checked);
        getWrapper().log("Row described by map ["+partialMap+"] selection state set to + ["+row.isSelected()+"]");

        return getThis();
    }

    /**
     * Finds the first row with the specified text in the specified column and sets its checkbox
     * @param columnLabel    header text of the specified column
     * @param text          Text to be found in the specified column
     * @param checked       true for checked, false for unchecked
     * @return
     */
    public ResponsiveGrid selectRow(String columnLabel, String text, boolean checked)
    {
        GridRow row = getRow(columnLabel, text);
        selectRowAndVerifyCheckedCounts(row, checked);
        getWrapper().log("Row at column ["+columnLabel+"] with text ["+text+"] selection state set to + ["+row.isSelected()+"]");

        return getThis();
    }

    private void selectRowAndVerifyCheckedCounts(GridRow row, boolean checked)
    {
        Locator selectedCheckboxes = Locator.css("tr td input:checked[type='checkbox']");
        int initialCount = selectedCheckboxes.findElements(this).size();
        int increment = 0;

        if (checked && !row.isSelected())
            increment++;
        else if (!checked && row.isSelected())
            increment--;

        row.select(checked);

        int finalIncrement = increment;
        int subsequentCount = selectedCheckboxes.findElements(this).size();
        waitFor(()-> subsequentCount == initialCount + finalIncrement, 1000);
    }

    /**
     * Sets the specified rows' selector checkboxes to the requested select state
     * @param columnLabel    Header text of the column to search
     * @param texts         Text to search for in the specified column
     * @param checked       True for checked, false for unchecked
     * @return
     */
    public ResponsiveGrid selectRows(String columnLabel, List<String> texts, boolean checked)
    {
        for (String text : texts)
        {
            selectRow(columnLabel, text, checked);
        }
        return getThis();
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
     * finds the first row containing the specified text and returns the checked state
     * @param text  A value in the row, used to identify the row.  (preferably a key)
     * @return  whether or not the selector checkbox is checked
     */
    public boolean isRowSelected(String text)
    {
        return new GridRow.GridRowFinder(this).withCellWithText(text)
                .find(this).isSelected();
    }

    /**
     * finds the first row containing the specified text in the specified column and returns the checked state
     * @param columnLabel    the text in the column to search
     * @param text  the value in the row to find
     * @return  true if the checkbox is checked, otherwise false
     */
    public boolean isRowSelected(String columnLabel, String text)
    {
        return new GridRow.GridRowFinder(this).withTextAtColumn(text, getColumnIndex(columnLabel))
                .find(this).isSelected();
    }

    protected ReactCheckBox selectAllBox()
    {
        ReactCheckBox box = elementCache().selectAllCheckbox;
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(box.getComponentElement()));
        return box;
    }

    /**
     * Use this method to know if the 'selectAllBox' is checked.
     * Note: it has three possible states:
     *      checked (meaning all rows on the page are selected)
     *      indeterminate (meaning some rows are selected but others are not)
     *      unchecked (meaning no rows on the page are selected)
     * @return  If the select-all box is checked, true. If indeterminate or unchecked, false
     */
    public boolean areAllRowsOnPageSelected()
    {
        return selectAllBox().isChecked();
    }

    /**
     * sets the 'select all' checkbox to the desired state
     * @param checked   true to check the box, false to uncheck it
     * @return  the current instance
     */
    public T selectAllOnPage(boolean checked)
    {
        if (checked)
            return selectAllOnPage(true, ReactCheckBox.CheckboxState.Checked);
        else
            return selectAllOnPage(false, ReactCheckBox.CheckboxState.Unchecked);
    }

    /**
     * Sometimes in a test scenario, the resulting state in the select-all box will depend upon whether or not
     * rows on other pages (or in filters) exist and aren't in the select-state intended with the checked parameter.
     * @param checked   true to select all on the current page, false to deselect all
     * @param expectedState signals what state to verify in the select-all box.
     *                      if null, no verification is done on the select-all box, but .
     * @return
     */
    public T selectAllOnPage(boolean checked, @Nullable ReactCheckBox.CheckboxState expectedState)
    {
        selectAllBox().set(checked, expectedState);
        Locator selectedText = Locator.XPathLocator.union(
                Locator.xpath("//span[@class='QueryGrid-right-spacing' and normalize-space(contains(text(), 'selected'))]"),
                Locator.tagWithClass("span", "selection-status__count").containing("selected"));

        // If checked is false, so un-selecting a value, don't wait for a confirmation message.
        if(checked)
            WebDriverWrapper.waitFor(()->
                    selectedText.findOptionalElement(getComponentElement()).isPresent(), WAIT_FOR_JAVASCRIPT);

        if (expectedState == null)  // we didn't verify expected state of the select-all box,
        {                           // ensure intended state of rows here
            if (!checked)
                assertThat("more than 0 rows in the current page were selected", getSelectedRows().size(), is(0));
            else
                assertTrue("not all rows in the current page were selected", getSelectedRows().stream().allMatch(a-> a.isSelected()));
        }

        return getThis();
    }

    /**
     * Returns a list of visible GridRows that are selected
     * @return  a list of all rows that are selected
     */
    public List<GridRow> getSelectedRows()
    {
        elementCache().initColumnsAndIndices();     // force-initialize the element cache, wait for loaded
        return new GridRow.GridRowFinder(this).findAll(this)
                .stream().filter(GridRow::isSelected).collect(Collectors.toList());
    }

    private GridRow getRow(int index)
    {
        return elementCache().getRow(index);
    }

    /**
     * Returns the first row containing a cell with matching full text
     * @param text exact matching to the text in at least one cell in the row
     * @return  the first row with a matching value in one of its cells
     */
    public GridRow getRow(String text)
    {
        return elementCache().getRow(text);
    }

    /**
     * Returns an optional row with at least one cell equal to the supplied text
     * @param text  exact match to one value in the row
     * @return      the first row with matching text in one of its cells
     */
    public Optional<GridRow> getOptionalRow(String text)
    {
        return elementCache().getOptionalRow(text);
    }

    /**
     * Returns the first row with matching text in the specified column
     * @param columnLabel The exact text of the column header
     * @param text The full text of the cell to match
     * @return  the first row that matches
     */
    public GridRow getRow(String columnLabel, String text)
    {
        return elementCache().getRow(columnLabel, text);
    }

    /**
     * Returns the first row with matching text in the specified column
     * @param columnLabel  the column to search
     * @param text  exact text to match in that column
     * @return  the first row matching the search criteria
     */
    public Optional<GridRow> getOptionalRow(String columnLabel, String text)
    {
        return elementCache().getOptionalRow(columnLabel, text);
    }

    /**
     * Returns the first row with matching text in the specified columns
     * @param partialMap Map of key (column), value (text)
     * @return  the first row with matching column/text for all of the supplied key/value pairs, or NotFoundException
     */
    public GridRow getRow(Map<String, String> partialMap)
    {
        return elementCache().getRow(partialMap);
    }

    /**
     * Returns the first row containing a descendant matching the supplied locator
     * @param containing    A locator matching an element the row must contain
     * @return  the first GridRow with a descendant matching the supplied locator
     */
    public GridRow getRow(Locator.XPathLocator containing)
    {
        return elementCache().getRow(containing);
    }

    /**
     * gets a list of all rows currently in the grid, after waiting for it to be loaded
     * @return
     */
    public List<GridRow> getRows()
    {
        return elementCache().getRows();
    }

    public List<String> getColumnDataAsText(String columnLabel)
    {
        List<String> columnData = new ArrayList<>();
        for (GridRow row : getRows())
        {
            columnData.add(row.getText(columnLabel));
        }
        return columnData;
    }

    /**
     *  Not all grids have selector rows; this method determines if the current one does.
     * @return  Returns whether or not the grid has a 'select all' checkbox.
     */
    public boolean hasSelectColumn()
    {
        return elementCache().selectColumn.isPresent();
    }

    /**
     * used to find the raw index of a given column as rendered in the dom.
     * To get the normalized index (which excludes selector rows if present) use
     * elementCache().indexes.get(column).getNormalizedIndex()
     */
    protected Integer getColumnIndex(String columnLabel)
    {
        if (elementCache().indexes == null)
        {
            getColumnNames();
        }

        if (elementCache().indexes.containsKey(columnLabel))
            return ((ColumnIndex)elementCache().indexes.get(columnLabel)).getRawIndex();
        else
            return -1;
    }

    /**
     *
     * @return  a List<String> containing the text of each column header
     */
    public List getColumnNames()
    {
        elementCache().initColumnsAndIndices();
        return elementCache().columnNames;
    }

    /**
     * there are ways to get rowMaps without exposing indexes to outside use
     * @param rowIndex  the index of the desired row
     * @return  a list of the text values in the row
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
     * @param rowIndex  the index of the desired row
     * @return  A Map containing column/value pairs for the specified row
     */
    @Deprecated
    public Map<String, String> getRowMap(int rowIndex)
    {
        return getRow(rowIndex).getRowMap();
    }

    /**
     * Where possible, avoid using row index in test code; use text/column or other means to get a GridRow
     * @param rowIndex  the
     * @param columnLabel
     * @return
     */
    public String getCellText(int rowIndex, String columnLabel)
    {
        return getRow(rowIndex).getText(columnLabel);
    }

    /**
     *
     * @return a list of Map<String, String> containing keys and values for each row
     */
    public List<Map<String, String>> getRowMaps()
    {
        if(null == elementCache().mapList)
        {
            elementCache().mapList = elementCache()._initGridData();
        }
        return elementCache().mapList;
    }

    /**
     * locates the first link in the grid with matching text
     * @param text  the text to match
     */
    public void clickLink(String text)
    {
        getRow(text).clickLink(text);
    }

    /**
     * locates the first link in the specified column, clicks it, and waits for the URL to update
     * @param columnLabel    column in which to search
     * @param text  text for link to match
     */
    public void clickLink(String columnLabel, String text)
    {
        getRow(columnLabel, text).clickLink(text);
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
     * supports chaining between base and derived instances
     * @return  magic
     */
    protected T getThis()
    {
        return (T) this;
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

        public Optional<WebElement> selectColumn = Locator.xpath("//th/input[@type='checkbox']").findOptionalElement(getComponentElement());
        ReactCheckBox selectAllCheckbox = new ReactCheckBox(Locator.xpath("//th/input[@type='checkbox']").findWhenNeeded(this));

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

        protected List<String> columnNames;
        protected Map<String, ColumnIndex> indexes;
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
                    indexes.put(columnNames.get(i), new ColumnIndex(columnNames.get(i), i+offset, i));
                }
            }
        }

        protected List<Map<String, String>> mapList;
        protected List<GridRow> gridRows;
        private List<Map<String, String>> _initGridData()
        {
            List<Map<String, String>> rowMaps = new ArrayList<>();
            gridRows = getRows();
            for(GridRow row : gridRows)
            {
                rowMaps.add(row.getRowMap());
            }
            return rowMaps;
        }

        protected GridRow getRow(int index)
        {
            return new GridRow.GridRowFinder(ResponsiveGrid.this).index(index).find(this);
        }

        protected GridRow getRow(String text)
        {
            return new GridRow.GridRowFinder(ResponsiveGrid.this).withCellWithText(text).find(this);
        }

        protected Optional<GridRow> getOptionalRow(String text)
        {
            return new GridRow.GridRowFinder(ResponsiveGrid.this).withCellWithText(text).findOptional(this);
        }

        protected GridRow getRow(String columnHeader, String text)
        {
            // try to normalize column index to start at 0, excluding row selector column
            Integer columnIndex = getColumnIndex(columnHeader);
            return new GridRow.GridRowFinder(ResponsiveGrid.this).withTextAtColumn(text, columnIndex)
                    .find(this);
        }

        protected Optional<GridRow> getOptionalRow(String columnHeader, String text)
        {
            // try to normalize column index to start at 0, excluding row selector column
            Integer columnIndex = getColumnIndex(columnHeader);
            return new GridRow.GridRowFinder(ResponsiveGrid.this).withTextAtColumn(text, columnIndex)
                    .findOptional(this);
        }

        protected GridRow getRow(Map<String, String> partialMap)
        {
            return getRows().stream().filter(a -> a.hasMatchingValues(partialMap))
                    .findFirst()
                    .orElseThrow(()-> new NotFoundException("No row with matching parameters was present: ["+partialMap+"]"));
        }

        protected GridRow getRow(Locator.XPathLocator containing)
        {
            return new GridRow.GridRowFinder(ResponsiveGrid.this).withDescendant(containing).find();
        }

        protected List<GridRow> getRows()
        {
            return new GridRow.GridRowFinder(ResponsiveGrid.this).findAll(getComponentElement());
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

    class ColumnIndex
    {
        private Integer _rawIndex;
        private Integer _normalizedIndex;
        private String _columnText;

        /**
         * Helper to
         * @param columnText    text of the column header
         * @param rawIndex      dom-oriented index of the column
         * @param normalizedIndex   index of the list of columns
         */
        public ColumnIndex(String columnText, int rawIndex, int normalizedIndex)
        {
            _columnText = columnText;
            _rawIndex = rawIndex;
            _normalizedIndex = normalizedIndex;
        }

        public String getColumnText()
        {
            return _columnText;
        }
        public Integer getRawIndex()
        {
            return _rawIndex;
        }
        public Integer getNormalizedIndex()
        {
            return _normalizedIndex;
        }
    }