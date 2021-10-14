package org.labkey.test.components.ui.grids;

import org.apache.commons.lang3.SystemUtils;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.util.TestLogger.log;

public class EditableGrid extends WebDriverComponent<EditableGrid.ElementCache>
{
    public static final String SELECT_COLUMN_HEADER = "<select>";
    public static final String ROW_NUMBER_COLUMN_HEADER = "<row number>";

    private final WebElement _gridElement;
    private final WebDriver _driver;

    protected EditableGrid(WebElement editableGrid, WebDriver driver)
    {
        _gridElement = editableGrid;
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

    private void waitForLoaded()
    {
        Locators.loadingGrid.waitForElementToDisappear(this, 30000);
        Locators.spinner.waitForElementToDisappear(this, 30000);
    }

    public List<String> getColumnNames()
    {
        List<String> columns = new ArrayList<>();
        List<WebElement> headerCells = Locators.headerCells.waitForElements(getComponentElement(), WAIT_FOR_JAVASCRIPT);
        for (WebElement el : headerCells)
        {
            columns.add(el.getText().trim());
        }

        int rowNumberColumn;

        if (hasSelectColumn())
        {
            columns.set(0, SELECT_COLUMN_HEADER);
            rowNumberColumn = 1;
        }
        else
        {
            rowNumberColumn = 0;
        }

        if (columns.get(rowNumberColumn).trim().isEmpty())
        {
            columns.set(rowNumberColumn, ROW_NUMBER_COLUMN_HEADER);
        }

        return columns;
    }

    protected Integer getColumnIndex(String columnHeader)
    {
        List<String> columnTexts = getColumnNames();
        for (int i=0; i< columnTexts.size(); i++ )
        {
            if (columnTexts.get(i).equalsIgnoreCase(columnHeader))
                return i + 1; // Zero (0) based index in the list, one (1) based index with the controls collection.
        }
        return -1;
    }

    private boolean hasSelectColumn()
    {
        return elementCache().selectColumn.isPresent();
    }

    public EditableGrid selectRow(int index, boolean checked)
    {
        if (hasSelectColumn())
        {
            WebElement checkBox = Locator.css("td > input[type=checkbox]").findElement(getRow(index));
            getWrapper().setCheckbox(checkBox, checked);
        }
        else
        {
            throw new NoSuchElementException("There is no select checkbox for row " + index);
        }
        return this;
    }

    public EditableGrid selectAll(boolean checked)
    {
        if (hasSelectColumn())
        {
            getWrapper().setCheckbox(elementCache().selectColumn.get(), checked);
        }
        else
        {
            throw new NoSuchElementException("There is no select checkbox for all rows.");
        }
        return this;
    }

    private List<WebElement> getRows()
    {
        waitForLoaded();
        return Locators.rows.findElements(getComponentElement());
    }

    public List<Map<String, String>> getGridData()
    {
        List<Map<String, String>> gridData = new ArrayList<>();

        List<String> columnNames = getColumnNames();
        int numOfRows = getRowCount();

        for (int rowIndex = 0; rowIndex < numOfRows; rowIndex++)
        {
            WebElement row = getRow(rowIndex);
            List<WebElement> cells = row.findElements(By.tagName("td"));
            Map<String, String> mapData = new HashMap<>();

            int index = 0;
            String columnName;
            for (WebElement cell : cells)
            {
                columnName = columnNames.get(index);

                if (columnName.equals(SELECT_COLUMN_HEADER))
                {
                    // Special case the check box.
                    if (null == cell.findElement(By.tagName("input")).getAttribute("checked"))
                        mapData.put(columnName, "false");
                    else
                        mapData.put(columnName, cell.findElement(By.tagName("input")).getAttribute("checked").trim().toLowerCase());
                }
                else
                {
                    mapData.put(columnName, cell.getText());
                }

                index++;
            }

            gridData.add(mapData);
        }

        return gridData;
    }

    public List<String> getColumnData(String columnLabel)
    {
        return getGridData().stream().map(a-> a.get(columnLabel)).collect(Collectors.toList());
    }

    private WebElement getRow(int index)
    {
        return getRows().get(index);
    }

    /**
     * Get the td element for a cell.
     *
     * @param row The 0 based row index.
     * @param column The name of the column to get the cell.
     * @return A {@link WebElement} that is the td for the cell.
     */
    private WebElement getCell(int row, String column)
    {
        int columnIndex = getColumnIndex(column);
        WebElement gridCell = getRow(row).findElement(By.cssSelector("td:nth-of-type(" + columnIndex + ")"));
        getWrapper().scrollIntoView(gridCell);
        return gridCell;
    }

    public boolean isCellReadOnly(int row, String column)
    {
        WebElement div = Locator.tag("div").findElement(getCell(row, column));
        String cellClass = div.getAttribute("class");
        return cellClass != null && cellClass.contains("cell-read-only");
    }

    public int getRowCount()
    {
        return getRows().size();
    }

    /**
     * As best as possible get a list of row indices from the grid for editable rows. That is rows where the values can
     * be entered or changed.
     *
     * @return A list of indices (0 based) for rows that can be edited.
     */
    public List<Integer> getEditableRowIndices()
    {
        return getRowTypes().get(0);
    }

    /**
     * Some EditableGrids have read only rows. These are rows in the grid that display data but cannot be updated. As
     * best as possible return a list of those rows.
     *
     * @return A list of indices (0 based) of rows that cannot be edited.
     */
    public List<Integer> getReadonlyRowIndices()
    {
        return getRowTypes().get(1);
    }

    private List<List<Integer>> getRowTypes()
    {
        List<Integer> unPopulatedRows = new ArrayList<>();
        List<Integer> populatedRows = new ArrayList<>();

        // Need to look at an attribute of a cell to see if it has pre-populated data.
        // But this info will not be in the select or row-number cells, so need to find a cell other than that.
        int checkColumn = getColumnNames().size() - 1;
        int rowCount = 0;

        for (WebElement row : getRows())
        {
            String classAttribute = row.findElement(By.cssSelector("td:nth-child(" + checkColumn + ") > div"))
                    .getAttribute("class");

            if ((!classAttribute.contains("cell-selection")) && (!classAttribute.contains("cell-read-only")))
            {
                unPopulatedRows.add(rowCount);
            }
            else
            {
                populatedRows.add(rowCount);
            }

            rowCount++;
        }

        return new ArrayList<List<Integer>>(Arrays.asList(unPopulatedRows, populatedRows));
    }

    /**
     * <p>
     *     For a given column, 'columnNameToSet', set the cell in the row if value in column 'columnNameToSearch'
     *     equals 'valueToSearch'.
     * </p>
     * <p>
     *     Rather than set one cell in a specific row, this function will loop through all the rows in the grid and
     *     will update the value in column 'columnNameToSet' only if the value in the column 'columnNameToSearch' equal
     *     'valueToSearch' in that row.
     * </p>
     * <p>
     *     The check for equality for 'valueToSearch' is case sensitive.
     * </p>
     *
     * @param columnNameToSearch The name of the column to check if a row should be updated or not.
     * @param valueToSearch The value to check for in 'columnNameToSearch' to see if a should be updated.
     * @param columnNameToSet The column to update in a row.
     * @param valueToSet The new value to put into column 'columnNameToSet'.
     */
    public void setCellValue(String columnNameToSearch, String valueToSearch, String columnNameToSet, Object valueToSet)
    {
        List<Map<String, String>> gridData = getGridData();
        int index = 0;

        for (Map<String, String> rowData : gridData)
        {
            if (rowData.get(columnNameToSearch).equals(valueToSearch))
            {
                setCellValue(index, columnNameToSet, valueToSet);
                break;
            }
            index++;
        }
    }

    /**
     * <p>
     *     For the identified row set the value in the identified column.
     * </p>
     * <p>
     *     If the column to be updated is a look-up, the value passed in must be a list, even if it is just one value.
     *     This is needed so the function knows how to set the value.
     * </p>
     *
     * @param row Index of the row (0 based).
     * @param columnName Name of the column to update.
     * @param value If the cell is a lookup, value should be List.of(value(s))
     */
    public void setCellValue(int row, String columnName, Object value)
    {
        // Get a reference to the cell.
        WebElement gridCell = getCell(row, columnName);

        // Select the cell.
        selectCell(gridCell);

        // Activate the cell.
        var cellContent = Locator.tagWithClass("div", "cellular-display").findElement(gridCell);
        cellContent.sendKeys(Keys.ENTER);

        if (value instanceof List)
        {
            // If this is a list assume that it will need a lookup.
            List<String> values = (List)value;

            WebElement lookupInputCell = elementCache().lookupInputCell();

            for (String _value : values)
            {
                getWrapper().setFormElement(lookupInputCell, _value);

                // was previously using elementCache().listGroupItem(_value).click() but the click would attempt to
                // scroll the list item into view which would result in the menu being reattached to the input element,
                // see changes in labkey-ui-components for issue 43051
                lookupInputCell.sendKeys(Keys.DOWN, Keys.ENTER);

                // If after selecting a value the grid text is equal to the item list then it was a single select
                // list box and we are done, otherwise we need to wait for the appropriate element.

                if (!getCellValue(gridCell).equalsIgnoreCase(_value))
                {
                    getWrapper().waitForElementToBeVisible(Locators.itemElement(_value));
                }
            }
        }
        else
        {
            // Treat the object being sent in as a string.
            // Get the inputCell enter the text and then make the inputCell go away (hit RETURN).

            WebElement inputCell = elementCache().inputCell();
            inputCell.clear();

            inputCell.sendKeys(Keys.END + value.toString() + Keys.RETURN); // Add the RETURN to close the inputCell.
            getWrapper().waitForElementToDisappear(Locators.inputCell, WAIT_FOR_JAVASCRIPT);

            // Wait until the grid cell has the updated text. Check for contains, not equal, because when updating a cell
            // the cell's new value will be the old value plus the new value and the cursor may not be placed at the end
            // of the existing value so the new value should exist some where in the cell text value not necessarily
            // at the end of it.
            WebDriverWrapper.waitFor(() -> gridCell.getText().contains(value.toString()),
                    "Value entered into inputCell '" + value + "' did not appear in grid cell.", WAIT_FOR_JAVASCRIPT);
        }
    }

    /**
     * For a given row get the value in the given column.
     *
     * @param row The row index (0 based).
     * @param columnName The name of the column to get the value for.
     * @return The string value of the {@link WebElement} that is the cell.
     */
    public String getCellValue(int row, String columnName)
    {
        return getCellValue(getCell(row, columnName));
    }

    private String getCellValue(WebElement cell)
    {
        return cell.getText().trim();
    }

    /**
     * Get the displayed dropdown list for the given grid cell (td).
     *
     * @param gridCell The td element that contains the list.
     * @return A list of the items in the list.
     */
    private List<String> getDropdownList(WebElement gridCell)
    {
        WebElement divLookup = Locators.lookupMenu.findWhenNeeded(gridCell);

        // Wait for the dropdown list to show.
        WebDriverWrapper.waitFor(divLookup::isDisplayed,
                "The dropdown list for the cell did not appear in time.", 5_000);

        List<WebElement> items = Locators.lookupItem.findElements(divLookup);
        return getWrapper().getTexts(items);
    }

    /**
     * Dismiss the dropdown list that is currently displayed on the grid.
     *
     * @return A reference to this EditableGrid.
     */
    public EditableGrid dismissDropdownList()
    {
        var menu = Locators.lookupMenu.findOptionalElement(getComponentElement());

        if (menu.isPresent())
        {
            Actions builder = new Actions(getDriver());
            builder.sendKeys(elementCache().lookupInputCell(), Keys.ESCAPE).build().perform();
            getWrapper().shortWait().until(ExpectedConditions.stalenessOf(menu.get()));
        }

        return this;
    }

    /**
     * For the given row get the values displayed in the dropdown list for the given column.
     *
     * @param row The 0 based row index.
     * @param columnName The name of the column.
     * @return A list of strings from the dropdown list. If the cell does not have a dropdown then an empty list is returned.
     */
    public List<String> getDropdownListForCell(int row, String columnName)
    {
        WebElement td = getCell(row, columnName);

        // If the td does not contain a div with a class containing 'size-limited' then it is not a look-up.
        WebElement div = Locator.findAnyElementOrNull(td, Locator.tagWithClassContaining("div", "size-limited"));

        List<String> listText = new ArrayList<>();

        if (div != null)
        {
            // Make the dropdown appear.
            getWrapper().doubleClick(td);
            listText = getDropdownList(td);
        }

        return listText;
    }

    /**
     * For the given row and column type some text into the cell to get the 'filtered' values displayed in the dropdown list.
     * If this cell is not a lookup cell, does not have a dropdown, the text will not be entered and it will return an empty list.
     *
     * @param row A 0 based index containing the cell.
     * @param columnName The column of the cell.
     * @param filterText The text to type into the cell.
     * @return A list values shown in the dropdown list after the text has been entered.
     */
    public List<String> getFilteredDropdownListForCell(int row, String columnName, String filterText)
    {

        WebElement td = getCell(row, columnName);

        // If the td does not contain a div with a class containing 'size-limited' then it is not a look-up.
        WebElement div = Locator.findAnyElementOrNull(td, Locator.tagWithClassContaining("div", "size-limited"));

        List<String> listText = new ArrayList<>();

        if (div != null)
        {
            // Get the input, will also make the dropdown show up.
            getWrapper().doubleClick(td);

            // Type the filter into the cell.
            WebElement lookupInputCell = elementCache().lookupInputCell();
            getWrapper().setFormElement(lookupInputCell, filterText);

            listText = getDropdownList(td);
        }

        return listText;
    }

    /**
     * Pastes delimited text to the grid, from a single target.  The component is clever enough to target
     * text into cells based on text delimiters; thus we can paste a square of data into the grid.
     * @param row           index of the target cell
     * @param columnName    column of the target cell
     * @param pasteText     tab-delimited or csv or excel data
     * @return A Reference to this editableGrid object.
     */
    public EditableGrid pasteFromCell(int row, String columnName, String pasteText)
    {
        int initialRowCount = getRowCount();
        WebElement gridCell = getCell(row, columnName);
        String indexValue = gridCell.getText();
        selectCell(gridCell);

        getWrapper().actionPaste(null, pasteText);

        // wait for the cell value to change or the rowcount to change, and the target cell to go into highlight,
        // ... or for a second and a half
        getWrapper().waitFor(()-> (getRowCount() > initialRowCount || !indexValue.equals(gridCell.getText())) &&
                        isInSelection(gridCell), 1500);
        return this;
    }

    /**
     * Pastes a single value into as many cells as are selected, or supports pasting a square shaped blob of data
     * of the same shape as the prescribed selection.  If a single value is supplied, that value will be put into
     * every cell in the selection.  If the data doesn't match the selection dimensions (e.g., has fewer or more columns)
     * the grid should produce an error/alert.
     * @param pasteText     The text to paste
     * @param startRowIndex index of the starting row
     * @param startColumn   text of the starting cell
     * @param endRowIndex   index of the ending row
     * @param endColumn     text of the ending cell
     * @return  the current grid instance
     */
    public EditableGrid pasteMultipleCells(String pasteText, int startRowIndex, String startColumn, int endRowIndex, String endColumn)
    {
        WebElement startCell = getCell(startRowIndex, startColumn);
        WebElement endCell = getCell(endRowIndex, endColumn);
        selectCellRange(startCell, endCell);
        getWrapper().actionPaste(null, pasteText);
        return this;
    }

    /**
     * Copies text from the grid, b
     * @param startRowIndex Index of the top-left cell's row
     * @param startColumn   Column header of the top-left cell
     * @param endRowIndex   Index of the bottom-right cell's row
     * @param endColumn     Column header of the bottom-right cell
     * @return  the text contained in the prescribed selection
     * @throws IOException
     * @throws UnsupportedFlavorException
     */
    public String copyCellRange(int startRowIndex, String startColumn, int endRowIndex, String endColumn) throws IOException, UnsupportedFlavorException
    {
        WebElement startCell = getCell(startRowIndex, startColumn);
        WebElement endCell = getCell(endRowIndex, endColumn);
        selectCellRange(startCell, endCell);
        return copyCurrentSelection();
    }

    /**
     * Selects all cells in the table, then copies their contents into delimited text
     * @return  delimited text content of the cells in the grid
     * @throws IOException
     * @throws UnsupportedFlavorException
     */
    public String copyAllCells() throws IOException, UnsupportedFlavorException
    {
        selectAllCells();
        getWrapper().waitFor(()-> areAllInSelection(),
                "expect all cells to be selected before copying grid values", 1500);

        String selection = copyCurrentSelection();
        if (selection.isEmpty())
        {
            log("initial attempt to copy current selection came up empty.  re-trying after 3000 msec");
            new WebDriverWait(getDriver(), Duration.ofSeconds(3));
            return copyCurrentSelection();
        }
        return selection;
    }

    public String copyCurrentSelection() throws IOException, UnsupportedFlavorException
    {
        // now copy the contents of the current selection to the clipboard
        Keys cmdKey = SystemUtils.IS_OS_MAC ? Keys.COMMAND : Keys.CONTROL;
        Actions actions = new Actions(getDriver());
        actions.keyDown(cmdKey)
                .sendKeys( "c")
                .keyUp(cmdKey)
                .build()
                .perform();

        return  (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                .getData(DataFlavor.stringFlavor);
    }

    private EditableGrid selectCellRange(WebElement startCell, WebElement endCell)
    {
        selectCell(startCell);
        getWrapper().scrollIntoView(endCell);
        // now drag mouse from start to end cell
        Actions selectRange = new Actions(getDriver());
        selectRange.dragAndDrop(startCell, endCell)
                .build()
                .perform();

        getWrapper().waitFor(()-> isInSelection(startCell) && isInSelection(endCell),
                "Cell range did not become selected", 2000);
        return this;
    }

    private void selectAllCells()
    {
        if (!areAllInSelection())
        {
            int indexOffset = hasSelectColumn() ? 1 : 0;
            selectCell(getCell(0, getColumnNames().get(1 + indexOffset)));    // forces the index cell into selected state
                                                                // this resets the grid state to a known base condition
            // use 'ctrl-a' to select the entire grid
            Keys cmdKey = SystemUtils.IS_OS_MAC ? Keys.COMMAND : Keys.CONTROL;
            new Actions(getDriver()).keyDown(cmdKey).sendKeys("a").keyUp(cmdKey).build().perform();
            getWrapper().waitFor(() -> areAllInSelection(),
                    "the expected cells did not become selected", 3000);
        }
    }

    /**
     * puts the specified cell into a selected state (appears as a dark-blue outline) with an active input present in it.
     * @param cell
     */
    private void selectCell(WebElement cell)
    {
        if (!isCellSelected(cell))
        {
            cell.click();
            getWrapper().waitFor(()->  isCellSelected(cell),
                    "the target cell did not become selected", 4000);
        }
    }

    /**
     * tests the specified webElement to see if it is in 'cell-selected' state, which means it has an active/focused input in it
     * @param cell
     * @return True if the edit is present
     */
    private boolean isCellSelected(WebElement cell)
    {
        return Locator.tagWithClass("div", "cellular-display")
                .findElement(cell)
                .getAttribute("class").contains("cell-selected");
    }

    /**
     *  tests the specified cell element to see if it is highlighted as a single-or-multi-cell selection area.  this appears as
     *  light-blue background, and is distinct from 'selected'
     * @param cell
     * @return
     */
    private boolean isInSelection(WebElement cell)  // 'in selection' shows as blue color, means it is part of one or many selected cells for copy/paste, etc
    {
        return Locator.tagWithClass("div", "cellular-display")
                .findElement(cell)
                .getAttribute("class").contains("cell-selection");
    }

    /**
     * attempts to determine whether the entire grid is selected
     * assumes that the first row is never selectable (it's either a selector row, or a row-number cell)
     * @return  True if the top-left and bottom-right cells are 'in-selection', otherwise false
     */
    private boolean areAllInSelection()
    {
        List<String> columns = getColumnNames();
        int selectIndexOffset = hasSelectColumn() ? 1 : 0;
        WebElement indexCell = getCell(0, columns.get(1 + selectIndexOffset));
        WebElement endCell = getCell(getRows().size()-1, columns.get(columns.size()-1));
        return (isInSelection(indexCell) && isInSelection(endCell));
    }

    private boolean cellHasWarning(WebElement cell)
    {
        return Locator.tagWithClass("div", "cell-warning").existsIn(cell);
    }

    public String getCellError(int row, String column)
    {
        WebElement gridCell = getCell(row, column);

        if (! cellHasWarning(gridCell))
            return null;
        else
            return Locator.tagWithClass("div", "cell-warning").findElement(gridCell).getText();
    }

    public String getCellPopoverText(int row, String column)
    {
        WebElement warnDiv = Locator.tagWithClass("div", "cellular-display").findElement(getCell(row, column));
        getWrapper().mouseOver(warnDiv);   // cause the tooltip to be present
        if (getWrapper().waitFor(()-> null != warnDiv.getAttribute("aria-describedby"), 1000))
        {
            String popoverId = warnDiv.getAttribute("aria-describedby");
            return Locator.id(popoverId).findElement(getDriver()).getText();
        }
        else
            return null;
    }

    public List<WebElement> getCellErrors()
    {
        return Locator.tagWithClass("div", "cell-warning").findElements(this);
    }

    public boolean isDisplayed()
    {
        try
        {
            return getComponentElement().isDisplayed();
        }
        catch (NoSuchElementException nse)
        {
            return false;
        }
    }

    public void doAndWaitForUpdate(Runnable func)
    {
        func.run();
        waitForUpdate();
    }

    private void waitForUpdate()
    {
        waitForLoaded();
        clearElementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public Optional<WebElement> selectColumn = Locator.xpath("//th/input[@type='checkbox']").findOptionalElement(getComponentElement());
        public WebElement inputCell()
        {
            return Locators.inputCell.findElement(getComponentElement());
        }

        public WebElement lookupInputCell()
        {
            return Locators.lookupInputCell.findElement(getComponentElement());
        }

        public WebElement lookupMenu()
        {
            return Locators.lookupMenu.findElement(getComponentElement());
        }

        public WebElement listGroupItem(String text)
        {
            return Locators.listGroupItem(text).findElement(getComponentElement());
        }

        public WebElement itemElement(String text)
        {
            return Locators.itemElement(text).findElement(getComponentElement());
        }

    }

    protected static abstract class Locators
    {
        static public Locator.XPathLocator editableGrid()
        {
            return Locator.byClass("table-cellular");
        }

        static final Locator loadingGrid = Locator.css("tbody tr.grid-loading");
        static final Locator emptyGrid = Locator.css("tbody tr.grid-empty");
        static final Locator spinner = Locator.css("span i.fa-spinner");
        static final Locator.XPathLocator rows = Locator.tag("tbody").childTag("tr").withoutClass("grid-empty").withoutClass("grid-loading");
        static final Locator headerCells = Locator.xpath("//thead/tr/th");
        static final Locator inputCell = Locator.tagWithClass("input", "cellular-input");
        static final Locator lookupInputCell = Locator.tagWithClass("input", "cell-lookup-input");
        static final Locator lookupMenu = Locator.tagWithClass("div", "cell-lookup-menu");
        static final Locator lookupItem = Locator.tagWithClass("a", "list-group-item");

        static final Locator listGroupItem(String text)
        {
            return Locators.lookupItem.withText(text);
        }

        static final Locator itemElement(String text)
        {
            return Locator.tagContainingText("span", text).withClass("btn-primary");
        }

    }

    public static class EditableGridFinder extends WebDriverComponent.WebDriverComponentFinder<EditableGrid, EditableGridFinder>
    {
        private Locator _locator;

        public EditableGridFinder(WebDriver driver)
        {
            super(driver);
            _locator= Locators.editableGrid();
        }

        @Override
        protected EditableGrid construct(WebElement el, WebDriver driver)
        {
            return new EditableGrid(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
