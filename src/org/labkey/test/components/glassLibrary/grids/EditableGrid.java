package org.labkey.test.components.glassLibrary.grids;

import org.apache.commons.lang3.SystemUtils;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        if (hasSelectColumn())
        {
            columns.set(0, SELECT_COLUMN_HEADER);
            columns.set(1, ROW_NUMBER_COLUMN_HEADER);
        }
        else
        {
            columns.set(0, ROW_NUMBER_COLUMN_HEADER);
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
        if(hasSelectColumn())
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

        for(int rowIndex = 0; rowIndex < numOfRows; rowIndex++)
        {
            WebElement row = getRow(rowIndex);
            List<WebElement> cells = row.findElements(By.tagName("td"));
            Map<String, String> mapData = new HashMap<>();

            int index = 0;
            String columnName;
            for(WebElement cell : cells)
            {
                columnName = columnNames.get(index);

                if(columnName.equals(SELECT_COLUMN_HEADER))
                {
                    // Special case the check box.
                    if(null == cell.findElement(By.tagName("input")).getAttribute("checked"))
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

    private WebElement getRow(int index)
    {
        return getRows().get(index);
    }

    private WebElement getCell(int row, String column)
    {
        int columnIndex = getColumnIndex(column);
        WebElement gridCell = getRow(row).findElement(By.cssSelector("td:nth-of-type(" + columnIndex + ")"));
        getWrapper().scrollIntoView(gridCell);
        return gridCell;
    }

    public int getRowCount()
    {
        return getRows().size();
    }

    public List<Integer> listOfnotPopulatedRows()
    {
        return getRowTypes().get(0);
    }

    public List<Integer> listOfPopulatedRows()
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

        for(WebElement row : getRows())
        {
            if(!row.findElement(By.cssSelector("td:nth-child(" + checkColumn + ") > div"))
                    .getAttribute("class").contains("cell-selection"))
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

    public void setCellValue(String columnNameToSearch, String valueToSearch, String columnNameToSet, Object valueToSet)
    {
        List<Map<String, String>> gridData = getGridData();
        int index = 0;

        for(Map<String, String> rowData : gridData)
        {
            if(rowData.get(columnNameToSearch).equals(valueToSearch))
            {
                setCellValue(index, columnNameToSet, valueToSet);
                break;
            }
            index++;
        }
    }

    /**
     *
     * @param row   index of the row
     * @param columnName
     * @param value If the cell is a lookup, value should be List.of(value(s))
     */
    public void setCellValue(int row, String columnName, Object value)
    {
        // Get a reference to the cell.
        WebElement gridCell = getCell(row, columnName);

        // Double click to edit the cell.
        Actions actions = new Actions(getDriver());
        actions.doubleClick(gridCell).perform();

        // Check to see if the double click caused a select list to appear if it did select from it.
        if(value instanceof List)
        {
            // If this is a list assume that it will need a lookup.
            List<String> values = (List)value;

            WebElement lookupInputCell = elementCache().lookupInputCell();

            for(String _value : values)
            {
                getWrapper().setFormElement(lookupInputCell, _value); // Add the RETURN to close the inputCell.
                WebElement listItem = elementCache().listGroupItem(_value);
                listItem.click();

                // If after selecting a value the grid text is equal to the item list then it was a single select
                // list box and we are done, otherwise we need to wait for the appropriate element.

                if(!gridCell.getText().trim().equalsIgnoreCase(_value))
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
            getWrapper().waitFor(() -> gridCell.getText().contains(value.toString()),
                    "Value entered into inputCell '" + value + "' did not appear in grid cell.", WAIT_FOR_JAVASCRIPT);

        }

    }

    /**
     * pastes delimited text to the grid, from a single target.  The component is clever enough to target
     * text into cells based on text delimiters; thus we can paste a square of data into the grid.
     * @param row           index of the target cell
     * @param columnName    column of the target cell
     * @param pasteText     tab-delimited or csv or excel data
     * @return
     */
    public EditableGrid pasteFromCell(int row, String columnName, String pasteText)
    {
        WebElement gridCell = getCell(row, columnName);
        selectCell(gridCell);

        getWrapper().actionPaste(null, pasteText);
        return this;
    }

    /**
     * pastes a single value into as many cells as are selected, or supports pasting a square shaped blob of data
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
            new WebDriverWait(getDriver(), 3);
            return copyCurrentSelection();
        }
        return selection;
    }

    private String copyCurrentSelection() throws IOException, UnsupportedFlavorException
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
            selectCell(getCell(0, getColumnNames().get(1)));    // forces the index cell into selected state
                                                                // this resets the grid state to a known base condition
            // use 'ctrl-a' to select the entire grid
            Keys cmdKey = SystemUtils.IS_OS_MAC ? Keys.COMMAND : Keys.CONTROL;
            new Actions(getDriver()).keyDown(cmdKey).sendKeys("a").keyUp(cmdKey).build().perform();
            getWrapper().waitFor(() -> areAllInSelection(),
                    "the expected cells did not become selected", 3000);
        }
    }

    /**
     * puts the specified cell into a selected state, (appears as a dark-blue outline) with an active input present in it.
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
     *  tests the specified cell element to see if it is highlit as a single-or-multi-cell selection area.  this appears as
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
        WebElement indexCell = getCell(0, columns.get(1));
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
        catch(NoSuchElementException nse)
        {
            return false;
        }
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
            getWrapper().waitForElementToBeVisible(Locators.inputCell);
            return Locators.inputCell.findElement(getComponentElement());
        }

        public WebElement lookupInputCell()
        {
            getWrapper().waitForElementToBeVisible(Locators.lookupInputCell);
            return Locators.lookupInputCell.findElement(getComponentElement());
        }

        public WebElement lookupMenu()
        {
            getWrapper().waitForElementToBeVisible(Locators.lookupMenu);
            return Locators.lookupMenu.findElement(getComponentElement());
        }

        public WebElement listGroupItem(String text)
        {
            getWrapper().waitForElementToBeVisible(Locators.lookupMenu);
            getWrapper().waitForElementToBeVisible(Locators.listGroupItem(text));
            return Locators.listGroupItem(text).findElement(getComponentElement());
        }

        public WebElement itemElement(String text)
        {
            getWrapper().waitForElementToBeVisible(Locators.itemElement(text));
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
//        static final Locator headerCells = Locator.xpath("//thead/tr/th[contains(@class, 'grid-header-cell')]");
        static final Locator headerCells = Locator.xpath("//thead/tr/th");
        static final Locator inputCell = Locator.tagWithClass("input", "cellular-input");
        static final Locator lookupInputCell = Locator.tagWithClass("input", "cell-lookup-input");
        static final Locator lookupMenu = Locator.tagWithClass("div", "cell-lookup-menu");

        static final Locator listGroupItem(String text)
        {
            return Locator.tagWithClass("a", "list-group-item").withText(text);
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
