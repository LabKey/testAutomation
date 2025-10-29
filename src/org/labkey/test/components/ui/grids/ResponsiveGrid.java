/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.ui.grids;

import org.awaitility.Awaitility;
import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.query.Filter;
import org.labkey.test.Locator;
import org.labkey.test.SortDirection;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.react.ReactCheckBox;
import org.labkey.test.components.ui.grids.FieldReferenceManager.FieldReference;
import org.labkey.test.components.ui.search.FilterExpressionPanel;
import org.labkey.test.params.FieldKey;
import org.labkey.test.util.selenium.WebElementUtils;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.labkey.test.WebDriverWrapper.waitFor;

public class ResponsiveGrid<T extends ResponsiveGrid<?>> extends WebDriverComponent<ResponsiveGrid<T>.ElementCache> implements UpdatingComponent
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
            (Locator.tag("td").existsIn(this) ||
                getGridEmptyMessage().isPresent());
    }

    protected void waitForLoaded()
    {
        WebDriverWrapper.waitFor(this::isLoaded, "Grid still loading", 30000);
    }

    @Override
    public void doAndWaitForUpdate(Runnable func)
    {
        // Look at WebDriverWrapper.doAndWaitForElementToRefresh for an example.
        func.run();

        waitForLoaded();
        clearElementCache();
    }

    public Boolean hasData()
    {
        return !Locators.emptyGrid.existsIn(this);
    }

    /**
     * Is the left column on this grid locked. I think this will always be true for this grid type.
     *
     * @return True if left column is locked, false otherwise.
     */
    public boolean hasLockedColumn()
    {
        return Locator.tagWithClass("div", "grid-panel__grid")
                .findElement(getComponentElement())
                .getDomAttribute("class").contains("grid-panel__lock-left");
    }

    /**
     * Scroll the grid to the top row and left most column.
     */
    public void scrollToOrigin()
    {
        getWrapper().executeScript("arguments[0].scrollBy(-arguments[0].scrollLeft, -arguments[0].scrollHeight)",
                Locators.responsiveGrid().findElement(getComponentElement()));
    }

    /**
     * Sorts from the grid header menu
     * @param columnIdentifier fieldKey, name, or label of column
     * @return this grid
     */
    public T sortColumnAscending(CharSequence columnIdentifier)
    {
        sortColumn(columnIdentifier, SortDirection.ASC);
        return getThis();
    }

    /**
     * Sorts from the grid header menu
     * @param columnIdentifier fieldKey, name, or label of column
     * @return this grid
     */
    public T sortColumnDescending(CharSequence columnIdentifier)
    {
        sortColumn(columnIdentifier, SortDirection.DESC);
        return getThis();
    }

    /**
     * Sorts from the grid header menu
     * @param columnIdentifier fieldKey, name, or label of column
     */
    public void sortColumn(CharSequence columnIdentifier, SortDirection direction)
    {
        clickColumnMenuItem(columnIdentifier, direction.equals(SortDirection.DESC) ? "Sort descending" : "Sort ascending", true);
    }

    /**
     * @param columnIdentifier fieldKey, name, or label of column
     */
    public void clearSort(CharSequence columnIdentifier)
    {
        clickColumnMenuItem(columnIdentifier, "Clear sort", true);
    }

    /**
     * @param columnIdentifier fieldKey, name, or label of column
     */
    public boolean hasColumnSortIcon(CharSequence columnIdentifier)
    {
        WebElement headerCell = elementCache().getColumnHeaderCell(columnIdentifier);
        Optional<WebElement> colHeaderIcon = Locator.XPathLocator.union(
                Locator.tagWithClass("span", "grid-panel__col-header-icon").withClass("fa-sort-amount-asc"),
                Locator.tagWithClass("span", "grid-panel__col-header-icon").withClass("fa-sort-amount-desc")
        ).findOptionalElement(headerCell);
        return colHeaderIcon.isPresent();

    }

    /**
     * @param columnIdentifier fieldKey, name, or label of column
     */
    public T filterColumn(CharSequence columnIdentifier, Filter.Operator operator)
    {
        return filterColumn(columnIdentifier, operator, null);
    }

    /**
     * @param columnIdentifier fieldKey, name, or label of column
     */
    public T filterColumn(CharSequence columnIdentifier, Filter.Operator operator, Object value)
    {
        T _this = getThis();
        doAndWaitForUpdate(()->initFilterColumn(columnIdentifier, operator, value).confirm());
        return _this;
    }

    /**
     * @param columnIdentifier fieldKey, name, or label of column
     */
    public T filterColumn(CharSequence columnIdentifier, Filter.Operator operator1, Object value1, Filter.Operator operator2, Object value2)
    {
        T _this = getThis();
        doAndWaitForUpdate(()-> {
            GridFilterModal filterModal = initFilterColumn(columnIdentifier, null, null);
            filterModal.selectExpressionTab().setFilters(
                    new FilterExpressionPanel.Expression(operator1, value1),
                    new FilterExpressionPanel.Expression(operator2, value2)
            );
            filterModal.confirm();
        });

        return _this;
    }

    /**
     * @param columnIdentifier fieldKey, name, or label of column
     */
    public T filterBooleanColumn(CharSequence columnIdentifier, boolean value)
    {
        T _this = getThis();
        doAndWaitForUpdate(() -> {
            clickColumnMenuItem(columnIdentifier, "Filter...", false);
            GridFilterModal filterModal = new GridFilterModal(getDriver(), this);
            new RadioButton.RadioButtonFinder().withNameAndValue("field-value-bool-0", value?"true":"false").find(filterModal).set(true);
            filterModal.confirm();
        });
        return _this;
    }

    /**
     * @param columnIdentifier fieldKey, name, or label of column
     */
    public String filterColumnExpectingError(CharSequence columnIdentifier, Filter.Operator operator, Object value)
    {
        GridFilterModal filterModal = initFilterColumn(columnIdentifier, operator, value);
        String errorMsg = filterModal.confirmExpectingError();
        filterModal.cancel();
        return errorMsg;
    }

    private GridFilterModal initFilterColumn(CharSequence columnIdentifier, Filter.Operator operator, Object value)
    {
        clickColumnMenuItem(columnIdentifier, "Filter...", false);
        GridFilterModal filterModal = new GridFilterModal(getDriver(), this);
        if (operator != null)
        {
            if (operator.equals(Filter.Operator.IN) && value instanceof List<?>)
            {
                List<String> values = (List<String>) value;
                filterModal.selectFacetTab().selectValue(values.get(0));
                filterModal.selectFacetTab().checkValues(values.toArray(String[]::new));
            }
            else
                filterModal.selectExpressionTab().setFilter(new FilterExpressionPanel.Expression(operator, value));
        }
        return filterModal;
    }

    /**
     * @param columnIdentifier fieldKey, name, or label of column
     */
    public T removeColumnFilter(CharSequence columnIdentifier)
    {
        clickColumnMenuItem(columnIdentifier, "Remove filter", true);
        return getThis();
    }

    /**
     * @param columnIdentifier fieldKey, name, or label of column
     */
    public boolean hasColumnFilterIcon(CharSequence columnIdentifier)
    {
        WebElement headerCell = elementCache().getColumnHeaderCell(columnIdentifier);
        Optional<WebElement> colHeaderIcon = Locator.tagWithClass("span", "grid-panel__col-header-icon")
                .withClass("fa-filter")
                .findOptionalElement(headerCell);
        return colHeaderIcon.isPresent();

    }

    /**
     * use the column menu to hide the given column.
     *
     * @param columnIdentifier fieldKey, name, or label of column
     * @return This grid.
     */
    public T hideColumn(CharSequence columnIdentifier)
    {
        // Because this will remove the column wait for the grid to update.
        clickColumnMenuItem(columnIdentifier, "Hide Column", true);
        return getThis();
    }

    /**
     * Use the column menu to show a Field Selection dialog {@link FieldSelectionDialog}. This will click the first column
     * in the grid.
     * @return A {@link FieldSelectionDialog}
     */
    public FieldSelectionDialog insertColumn()
    {
        return insertColumn(getHeaders().get(0).getFieldKey().toString());
    }

    /**
     * Use the column menu to show a Field Selection dialog {@link FieldSelectionDialog}. This will use the given column to
     * get the menu. This should insert the column after (to the right) of this column.
     *
     * @param columnIdentifier fieldKey, name, or label of column
     * @return A {@link FieldSelectionDialog}
     */
    public FieldSelectionDialog insertColumn(CharSequence columnIdentifier)
    {
        // Because this is going to show the customize grid dialog don't wait for a grid update. the dialog will wait for the update.
        clickColumnMenuItem(columnIdentifier, "Insert Column", false);
        return new FieldSelectionDialog(getDriver(), this);
    }

    public void clickColumnMenuItem(CharSequence columnIdentifier, String menuText, boolean waitForUpdate)
    {

        if(hasLockedColumn())
        {
            scrollToOrigin();
        }

        WebElement headerCell = elementCache().getColumnHeaderCell(columnIdentifier);
        // Scroll to middle in order to make room for the dropdown menu
        getWrapper().scrollToMiddle(headerCell);

        WebElement toggle = Locator.tagWithClass("span", "fa-chevron-circle-down")
                .findElement(headerCell);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(toggle));
        toggle.click();

        // Use getDriver() because the grid menus are rendered in a "react portal" at the end of the HTML body, so they
        // are totally detached from the rest of the grid.
        WebElement menu = Locator.css("ul.grid-header-cell__dropdown-menu.open").findWhenNeeded(getDriver());
        WebElement menuItem = Locator.css("li > a").containing(menuText).findWhenNeeded(menu);
        waitFor(menuItem::isDisplayed, 1000);
        if (waitForUpdate)
            doAndWaitForUpdate(menuItem::click);
        else
            menuItem.click();
        waitFor(()-> !menuItem.isDisplayed(), 1000);
    }

    /**
     * @param columnIdentifier fieldKey, name, or label of column
     * @param newColumnLabel new label for the column
     */
    public void editColumnLabel(CharSequence columnIdentifier, String newColumnLabel)
    {
        // Get the column header.
        WebElement headerCell = elementCache().getColumnHeaderCell(columnIdentifier);

        // Select the edit menu.
        clickColumnMenuItem(columnIdentifier, "Edit Label", false);

        // Get the textbox.
        WebElement textEdit = Locator.tag("input").findWhenNeeded(headerCell);

        // Clear the text box.
        new Actions(getDriver())
                .keyDown(Keys.SHIFT)
                .sendKeys(Keys.HOME)
                .sendKeys(Keys.DELETE)
                .keyUp(Keys.SHIFT)
                .perform();

        doAndWaitForUpdate(()-> {
            textEdit.sendKeys(newColumnLabel, Keys.RETURN);

            getWrapper().shortWait()
                .withMessage("Column label edit text box did not go away.")
                .until(ExpectedConditions.stalenessOf(textEdit));

            Awaitility.await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> assertEquals("Column label",
                newColumnLabel, WebElementUtils.getTextContent(headerCell)));
        });
    }

    /**
     * Check/uncheck row at index
     * @param index Row index (zero-based)
     * @param checked the desired checkbox state
     * @return this grid
     */
    public T selectRow(int index, boolean checked)
    {
        getRow(index).select(checked);
        return getThis();
    }

    /**
     * Finds the first row with the specified texts in the specified columns, and sets its checkbox
     * @param partialMap    key-column (fieldKey, name, or label), value-text in that column
     * @param checked       the desired checkbox state
     * @return this grid
     */
    public T selectRow(Map<String, String> partialMap, boolean checked)
    {
        GridRow row = getRow(partialMap);
        selectRowAndVerifyCheckedCounts(row, checked);
        getWrapper().log("Row described by map ["+partialMap+"] selection state set to + ["+row.isSelected()+"]");

        return getThis();
    }

    /**
     * Finds the first row with the specified text in the specified column and sets its checkbox
     * @param columnIdentifier fieldKey, name, or label of column
     * @param text          Text to be found in the specified column
     * @param checked       true for checked, false for unchecked
     * @return this grid
     */
    public ResponsiveGrid<?> selectRow(CharSequence columnIdentifier, String text, boolean checked)
    {
        GridRow row = getRow(columnIdentifier, text);
        selectRowAndVerifyCheckedCounts(row, checked);
        getWrapper().log("Row at column ["+columnIdentifier+"] with text ["+text+"] selection state set to + ["+row.isSelected()+"]");

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
     * @param columnIdentifier fieldKey, name, or label of column
     * @param texts         Text to search for in the specified column
     * @param checked       True for checked, false for unchecked
     * @return this grid
     */
    public T selectRows(CharSequence columnIdentifier, Collection<String> texts, boolean checked)
    {
        for (String text : texts)
        {
            selectRow(columnIdentifier, text, checked);
        }
        return getThis();
    }

    /**
     * Checks the specified rows' selector checkboxes
     * @param columnIdentifier fieldKey, name, or label of column
     * @param texts         Text to search for in the specified column
     * @return this grid
     */
    public T selectRows(CharSequence columnIdentifier, String... texts)
    {
        return selectRows(columnIdentifier, Arrays.asList(texts), true);
    }

    /**
     * Is the row at the selected index selected
     * @param index Row index (zero-based)
     * @return <code>true</code> if row is checked
     */
    public boolean isRowSelected(int index)
    {
       return new GridRow.GridRowFinder(this).index(index).find(this).isSelected();
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
     * @return this grid
     */
    public T selectAllOnPage(boolean checked, @Nullable ReactCheckBox.CheckboxState expectedState)
    {
        selectAllBox().set(checked, expectedState);

        if (expectedState == null)  // we didn't verify expected state of the select-all box,
        {                           // ensure intended state of rows here
            if (!checked)
                assertThat("more than 0 rows in the current page were selected", getSelectedRows().size(), is(0));
            else
                assertEquals("not all rows in the current page were selected", getRows().size(), getSelectedRows().size());
        }

        return getThis();
    }

    /**
     * Returns a list of visible GridRows that are selected
     * @return  a list of all rows that are selected
     */
    public List<GridRow> getSelectedRows()
    {
        elementCache().getColumnLabels();     // force-initialize the element cache, wait for loaded
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
     * @param columnIdentifier fieldKey, name, or label of column to search
     * @param text The full text of the cell to match
     * @return  the first row that matches
     */
    public GridRow getRow(CharSequence columnIdentifier, String text)
    {
        return elementCache().getRow(columnIdentifier, text);
    }

    /**
     * Returns the first row with matching text in the specified column
     * @param columnIdentifier fieldKey, name, or label of column of the column to search
     * @param text  exact text to match in that column
     * @return  the first row matching the search criteria
     */
    public Optional<GridRow> getOptionalRow(CharSequence columnIdentifier, String text)
    {
        return elementCache().getOptionalRow(columnIdentifier, text);
    }

    /**
     * Returns the first row with matching text in the specified columns
     * @param partialMap Map of key (fieldKey, name, or label of column), value (text)
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
     * @return All grid rows
     */
    public List<GridRow> getRows()
    {
        return elementCache().getRows();
    }

    /**
     * @param columnIdentifier fieldKey, name, or label of column
     */
    public List<String> getColumnDataAsText(CharSequence columnIdentifier)
    {
        List<String> columnData = new ArrayList<>();
        for (GridRow row : getRows())
        {
            columnData.add(row.getText(columnIdentifier));
        }
        return columnData;
    }

    /**
     *  Not all grids have selector rows; this method determines if the current one does.
     * @return  Returns whether or not the grid has a 'select all' checkbox.
     */
    public boolean hasSelectColumn()
    {
        return elementCache().hasSelectColumn();
    }

    /**
     * @param columnIdentifier fieldKey, name, or label of column
     * @return the DOM index of the specified column (e.g. '0' for the row selection column)
     */
    protected Integer getColumnIndex(CharSequence columnIdentifier)
    {
        return elementCache().getColumnIndex(columnIdentifier);
    }

    /**
     *
     * @return a List&#60;String&#62; containing the text of each column header
     */
    public List<String> getColumnLabels()
    {
        return elementCache().getColumnLabels();
    }

    /**
     *
     * @return a List&#60;String&#62; containing the names of the fields for each column header
     */
    public List<String> getColumnNames()
    {
        return elementCache().getColumnNames();
    }

    /**
     *
     * @return a List&#60;FieldKey&#62; containing the fieldKeys for each column header
     */
    public List<FieldKey> getColumnFieldKeys()
    {
        return elementCache().getColumnFieldKeys();
    }

    /**
     * Get data from a row
     * @param rowIndex  the index of the desired row
     * @return  a list of the text values in the row
     */
    public List<String> getRowTexts(int rowIndex)
    {
        // preserves the ordering of the values as they appear in the row.
        if (!hasData())
            throw new IllegalStateException("Attempting to get a row by index, but no rows exist");

        return getRow(rowIndex).getTexts();
    }

    /**
     * Get data from a row in map form
     * @param rowIndex  the index of the desired row
     * @return  A Map containing column/value pairs for the specified row
     */
    public Map<String, String> getRowMapByLabel(int rowIndex)
    {
        return getRow(rowIndex).getRowMapByLabel();
    }

    /**
     * Get text from the specified column in the specified row
     * @param columnIdentifier fieldKey, name, or label of column
     */
    public String getCellText(int rowIndex, CharSequence columnIdentifier)
    {
        return getRow(rowIndex).getText(columnIdentifier);
    }

    /**
     * @return a list of Map&#60;String, String&#62; containing keys and values for each row
     */
    public List<Map<String, String>> getRowMapsByLabel()
    {
        return elementCache().getRows().stream().map(GridRow::getRowMapByLabel).toList();
    }

    /**
     * @return a list of Map&#60;String, String&#62; containing keys and values for each row
     */
    public List<Map<String, String>> getRowMapsByName()
    {
        return elementCache().getRows().stream().map(GridRow::getRowMapByName).toList();
    }

    /**
     * @return a list of Map&#60;String, String&#62; containing keys and values for each row
     */
    public List<Map<FieldKey, String>> getRowMapsByFieldKey()
    {
        return elementCache().getRows().stream().map(GridRow::getRowMapByFieldKey).toList();
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
     * @param columnIdentifier fieldKey, name, or label of column
     * @param text  text for link to match
     */
    public void clickLink(CharSequence columnIdentifier, String text)
    {
        getRow(columnIdentifier, text).clickLink(text);
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

    public boolean hasGridError()
    {
        return getGridError() != null;
    }

    public String getGridError()
    {
        Locator.XPathLocator errorLocator = Locator.tagWithClass("div", "alert-danger");
        return errorLocator.findOptionalElement(this).map(WebElement::getText).orElse(null);
    }

    /** The responsiveGrid now supports redacting fields
     *
     * @param columnIdentifier fieldKey, name, or label of column
     * @return  true if the specified grid header cell has the 'phi-protected' class on it
     */
    public boolean getColumnPHIProtected(CharSequence columnIdentifier)
    {
        WebElement columnHeader = elementCache().getColumnHeaderCell(columnIdentifier);
        return columnHeader.getDomAttribute("class").contains("phi-protected");
    }

    public Optional<String> getGridEmptyMessage()
    {
        Optional<String> msg = Optional.empty();

        try
        {
            WebElement tr = Locator.tagWithClass("tr", "grid-empty").findWhenNeeded(this);
            if (tr.isDisplayed())
            {
                msg = Optional.of(Locator.tag("td").findElement(tr).getText());
            }
        }
        catch (StaleElementReferenceException stale)
        {
            getWrapper().log("Grid empty message was present but has now gone stale (went away).");
            msg = Optional.empty();
        }

        return msg;
    }

    public List<FieldReference> getHeaders()
    {
        return Collections.unmodifiableList(elementCache().findHeaders());
    }

    /**
     * supports chaining between base and derived instances
     * @return  magic
     */
    protected T getThis()
    {
        return (T) this;
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

        private Boolean hasSelectColumn = null;
        protected boolean hasSelectColumn()
        {
            if (hasSelectColumn == null)
            {
                hasSelectColumn = selectAllCheckbox.isDisplayed();
            }
            return hasSelectColumn;
        }

        ReactCheckBox selectAllCheckbox = new ReactCheckBox(Locator.xpath("//th/input[@type='checkbox']").findWhenNeeded(this))
        {
            @Override
            public void toggle()
            {
                doAndWaitForUpdate(super::toggle);
            }
        };

        protected final WebElement getColumnHeaderCell(CharSequence columnIdentifier)
        {
            return findColumnHeader(columnIdentifier).getElement();
        }

        private FieldReferenceManager _fieldReferenceManager;
        protected FieldReferenceManager getGridHeaderManager()
        {
            if (_fieldReferenceManager == null)
            {
                List<FieldReference> fieldReferences = new ArrayList<>();
                List<WebElement> headerCellElements = Locators.headerCells.findElements(this);
                for (int domIndex = hasSelectColumn() ? 1 : 0; domIndex < headerCellElements.size(); domIndex++)
                {
                    fieldReferences.add(new FieldReference(headerCellElements.get(domIndex), domIndex));
                }

                _fieldReferenceManager = new FieldReferenceManager(fieldReferences);
            }
            return _fieldReferenceManager;
        }

        protected List<FieldReferenceManager.FieldReference> findHeaders()
        {
            return getGridHeaderManager().getColumnHeaders();
        }

        protected FieldReference findColumnHeader(CharSequence columnIdentifier)
        {
            return getGridHeaderManager().findFieldReference(columnIdentifier);
        }

        protected int getColumnIndex(CharSequence columnIdentifier)
        {
            return findColumnHeader(columnIdentifier).getDomIndex();
        }

        protected List<String> getColumnLabels()
        {
            return findHeaders().stream().map(FieldReferenceManager.FieldReference::getLabel).collect(Collectors.toList());
        }

        protected List<String> getColumnNames()
        {
            return findHeaders().stream().map(FieldReferenceManager.FieldReference::getName).collect(Collectors.toList());
        }

        protected List<FieldKey> getColumnFieldKeys()
        {
            return findHeaders().stream().map(FieldReferenceManager.FieldReference::getFieldKey).collect(Collectors.toList());
        }

        protected GridRow getRow(int index)
        {
            return getRows().get(index);
        }

        protected GridRow getRow(String text)
        {
            return new GridRow.GridRowFinder(ResponsiveGrid.this).withCellWithText(text).find(this);
        }

        protected Optional<GridRow> getOptionalRow(String text)
        {
            return new GridRow.GridRowFinder(ResponsiveGrid.this).withCellWithText(text).findOptional(this);
        }

        protected GridRow getRow(CharSequence columnIdentifier, String text)
        {
            int columnIndex = getColumnIndex(columnIdentifier);
            return new GridRow.GridRowFinder(ResponsiveGrid.this).withTextAtColumn(text, columnIndex)
                    .find(this);
        }

        protected Optional<GridRow> getOptionalRow(CharSequence columnIdentifier, String text)
        {
            int columnIndex = getColumnIndex(columnIdentifier);
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

        private List<GridRow> gridRows;
        protected List<GridRow> getRows()
        {
            if (gridRows == null)
            {
                gridRows = new GridRow.GridRowFinder(ResponsiveGrid.this).findAll(this);
            }
            return gridRows;
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
        static final Locator spinner = Locator.byClass("fa-spinner");
        static final Locator headerCells = Locator.tagWithClass("th", "grid-header-cell");
        static public Locator.XPathLocator headerCellBody(String label)
        {
            return Locator.tagWithClass("div", "grid-header-cell__body")
                    .withChild(Locator.tag("span").startsWith(label));
        }
    }

    public static class ResponsiveGridFinder extends WebDriverComponentFinder<ResponsiveGrid<?>, ResponsiveGridFinder>
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

        public ResponsiveGridFinder inParentWithClass(String className)
        {
            _locator = Locator.byClass(className).child(Locators.responsiveGrid());
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
        protected ResponsiveGrid<?> construct(WebElement el, WebDriver driver)
        {
            return new ResponsiveGrid<>(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
