package org.labkey.test.components.glassLibrary.grids;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;

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

    public EditableGrid pasteFromCell(int row, String columnName, String pasteText)
    {
        WebElement gridCell = getCell(row, columnName);

        if (!isCellSelected(gridCell))
        {
            gridCell.click();
            getWrapper().waitFor(()->  isCellSelected(gridCell),
                    "the target cell did not become selected", 4000);
        }

        getWrapper().actionPaste(null, pasteText);
        return this;
    }

    private boolean isCellSelected(WebElement cell)
    {
        return Locator.tagWithClass("div", "cellular-display")
                .findElement(cell)
                .getAttribute("class").contains("cell-selected");
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
