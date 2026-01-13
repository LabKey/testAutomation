package org.labkey.test.components.ui.grids;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.ReactDateTimePicker;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.ui.entities.EntityBulkInsertDialog;
import org.labkey.test.components.ui.entities.EntityBulkUpdateDialog;
import org.labkey.test.components.ui.grids.FieldReferenceManager.FieldReference;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldKey;
import org.labkey.test.util.CachingSupplier;
import org.labkey.test.util.selenium.ScrollUtils;
import org.labkey.test.util.selenium.WebElementUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.waitFor;
import static org.labkey.test.util.TestLogger.log;
import static org.labkey.test.util.selenium.ScrollUtils.Alignment.center;
import static org.labkey.test.util.selenium.WebDriverUtils.MODIFIER_KEY;

public class EditableGrid extends WebDriverComponent<EditableGrid.ElementCache>
{
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final String SELECT_COLUMN_LABEL_PLACEHOLDER = "<select>";
    public static final FieldKey SELECT_COLUMN_ID = FieldKey.fromParts("__select__");
    public static final String ROW_NUMBER_COLUMN_HEADER = "<row number>";

    private final WebElement _gridElement;
    private final WebDriver _driver;

    protected EditableGrid(WebElement editableGrid, WebDriver driver)
    {
        _gridElement = editableGrid;
        _driver = driver;
    }

    protected EditableGrid(EditableGrid wrappedGrid)
    {
        this(wrappedGrid.getComponentElement(), wrappedGrid.getDriver());
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

    @Override
    public void waitForReady()
    {
        Locators.loadingGrid.waitForElementToDisappear(this, 30000);
        Locators.spinner.waitForElementToDisappear(this, 30000);
    }

    /**
     * Quote values to be pasted into lookup columns. Prevents a value containing a comma from being interpreted as
     * multiple values.
     * @param values the raw values
     * @return The values, quoted if necessary for pasting into a single lookup cell
     */
    public static String quoteForPaste(String... values)
    {
        return Arrays.stream(values).map(CSVFormat.DEFAULT::format).collect(Collectors.joining(","));
    }

    public void clickDelete()
    {
        doAndWaitForRowCountUpdate(() -> elementCache().deleteRowsBtn.click());
    }

    public EntityBulkInsertDialog clickBulkAdd()
    {
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().bulkAddBtn));
        elementCache().bulkAddBtn.click();

        return new EntityBulkInsertDialog(getDriver());
    }

    public EntityBulkUpdateDialog clickBulkUpdate()
    {
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().bulkUpdateBtn));
        elementCache().bulkUpdateBtn.click();

        return new EntityBulkUpdateDialog(getDriver());
    }

    public ExportMenu getExportMenu()
    {
        return elementCache().exportMenu;
    }

    public List<String> getColumnLabels()
    {
        return elementCache().getColumnLabels();
    }

    public Integer getColumnIndex(CharSequence columnIdentifier)
    {
        return elementCache().getColumnIndex(columnIdentifier);
    }

    /**
     * Remove the specified column from the grid
     * @param columnIdentifier fieldKey, name, or label
     * @return this component
     */
    public EditableGrid removeColumn(CharSequence columnIdentifier)
    {
        doAndWaitForColumnUpdate(() ->
        {
            WebElement headerCell = elementCache().getColumnHeaderCell(columnIdentifier);
            Locator.byClass("fa-chevron-circle-down").findElement(headerCell).click();
            Locator.tagWithText("a", "Remove Column").findElement(headerCell).click();
        });
        return this;
    }

    public boolean canRemoveColumn(CharSequence columnIdentifier)
    {
        WebElement headerCell = elementCache().getColumnHeaderCell(columnIdentifier);
        WebElement downBtn = Locator.byClass("fa-chevron-circle-down").findElementOrNull(headerCell);
        if (downBtn == null)
            return false;
        downBtn.click();
        WebElement removeBtn = Locator.tagWithText("a", "Remove Column").findElementOrNull(headerCell);
        boolean canRemove = removeBtn != null && removeBtn.isDisplayed() && removeBtn.isEnabled();
        downBtn.click(); // close dropdown
        return canRemove;
    }

    private boolean hasSelectColumn()
    {
        return elementCache().hasSelectColumn.get();
    }

    public EditableGrid selectRow(int index, boolean checked)
    {
        elementCache().getCheckbox(index).set(checked);
        return this;
    }

    public boolean isRowSelected(int index)
    {
        return elementCache().getCheckbox(index).isSelected();
    }

    public EditableGrid selectAll(boolean checked)
    {
        elementCache().selectAllCheckbox.set(checked);
        return this;
    }

    public boolean areAllRowsSelected()
    {
        return elementCache().selectAllCheckbox.isSelected();
    }

    /**
     * Selects a range of rows in the current view.
     * If the range is within a range of already-selected rows, will deselect the specified range
     * @param start the starting index (0-based), of non-header rows with checkboxes
     * @param end the ending index
     * @return  the current instance
     */
    public EditableGrid shiftSelectRange(int start, int end)
    {
        if (!hasSelectColumn())
            throw new NoSuchElementException("there is no selection column for grid");

        var checkBoxes = Locator.tag("tr").child("td")
                .child(Locator.byClass("table-cell-content"))
                .child(Locator.tagWithAttribute("input", "type", "checkbox"))
                .findElements(elementCache().table);
        getWrapper().scrollIntoView(checkBoxes.get(start)); // Make sure the header isn't in the way
        checkBoxes.get(start).click();
        getWrapper().scrollIntoView(checkBoxes.get(end)); // Actions.click() doesn't scroll
        new Actions(getDriver())
                .keyDown(Keys.SHIFT)
                .click(checkBoxes.get(end))
                .keyUp(Keys.SHIFT)
                .perform();
        return this;
    }

    /**
     * @param columnIdentifiers fieldKeys, names, or labels of columns
     * @return grid data for the specified columns, keyed by column label
     */
    public List<Map<String, String>> getGridDataByLabel(CharSequence... columnIdentifiers)
    {
        return getGridData(FieldReferenceManager.FieldReference::getLabel, columnIdentifiers);
    }

    /**
     * @param columnIdentifiers fieldKeys, names, or labels of columns
     * @return grid data for the specified columns, keyed by column fieldKey
     */
    public List<Map<FieldKey, String>> getGridDataByFieldKey(CharSequence... columnIdentifiers)
    {
        return getGridData(FieldReferenceManager.FieldReference::getFieldKey, columnIdentifiers);
    }

    /**
     * @param columnIdentifiers fieldKeys, names, or labels of columns
     * @return grid data for the specified columns, keyed by column name
     */
    public List<Map<String, String>> getGridDataByName(CharSequence... columnIdentifiers)
    {
        return getGridData(FieldReference::getName, columnIdentifiers);
    }

    /**
     * @param columnIdentifiers fieldKeys, names, or labels of columns
     * @return grid data for the specified columns, ordered as the provided columnIdentifiers
     */
    public List<List<String>> getGridData(CharSequence... columnIdentifiers)
    {
        List<Map<Integer, String>> rowMaps = getGridData(FieldReference::getDomIndex, columnIdentifiers);
        List<List<String>> gridData = new ArrayList<>();
        for (Map<Integer, String> gridMap : rowMaps)
        {
            gridData.add(new ArrayList<>(gridMap.values())); // row maps remember insertion order
        }
        return gridData;
    }

    private <T> List<Map<T, String>> getGridData(Function<FieldReferenceManager.FieldReference, T> keyGenerator, CharSequence... columnIdentifiers)
    {
        List<Map<T, String>> gridData = new ArrayList<>();

        Set<FieldReference> includedColHeaders = new LinkedHashSet<>();
        if (columnIdentifiers.length == 0)
        {
            includedColHeaders.addAll(elementCache().findHeaders());
        }
        else
        {
            for (CharSequence columnIdentifier : columnIdentifiers)
            {
                includedColHeaders.add(elementCache().findColumnHeader(columnIdentifier));
            }
        }

        for (WebElement row : elementCache().getRows())
        {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            Map<T, String> rowMap = new LinkedHashMap<>(includedColHeaders.size());

            for (FieldReference fieldReference : includedColHeaders)
            {
                WebElement cell = cells.get(fieldReference.getDomIndex());

                T key = keyGenerator.apply(fieldReference);
                String value;

                if (fieldReference.getDomIndex() == 0 && hasSelectColumn())
                {
                    value = String.valueOf(Locator.tag("input").findElement(cell).isSelected());
                }
                else
                {
                    value = cell.getText();
                }

                rowMap.put(key, value);
            }

            gridData.add(rowMap);
        }

        return gridData;
    }

    @Deprecated
    public List<String> getColumnDataByLabel(CharSequence columnIdentifier)
    {
        return getColumnData(columnIdentifier);
    }

    /**
     * @param columnIdentifier fieldKey, name, or label of column
     */
    public List<String> getColumnData(CharSequence columnIdentifier)
    {
        return getGridData(ch -> 1, columnIdentifier).stream().map(a-> a.get(1)).collect(Collectors.toList());
    }

    private WebElement getRow(int index)
    {
        return elementCache().getRows().get(index);
    }

    /**
     * Find the first row index containing the text value in the given column.
     * If not found -1 is returned.
     *
     * @param columnIdentifier fieldKey, name, or label of column
     * @param text Text to look for (must match exactly).
     * @return The first row index where found, -1 if not found.
     */
    public Integer getRowIndex(CharSequence columnIdentifier, String text)
    {
        int index = -1;

        List<String> columnData = getColumnData(columnIdentifier);
        for (int i = 0; i < columnData.size(); i++)
        {
            if (columnData.get(i).equals(text))
            {
                index = i;
                break;
            }
        }

        return index;
    }

    /**
     * Get the td element for a cell.
     *
     * @param row The 0 based row index.
     * @param columnIdentifier fieldKey, name, or label of column
     * @return A {@link WebElement} that is the td for the cell.
     */
    public WebElement getCell(int row, CharSequence columnIdentifier)
    {
        int columNumber = getColumnIndex(columnIdentifier) + 1;
        return Locator.css("td:nth-of-type(" + columNumber + ")").findElement(getRow(row));
    }

    public boolean isCellReadOnly(int row, CharSequence columnIdentifier)
    {
        WebElement div = Locator.tag("div").findElement(getCell(row, columnIdentifier));
        String cellClass = div.getDomAttribute("class");
        return cellClass != null && cellClass.contains("cell-read-only");
    }

    public int getRowCount()
    {
        return elementCache().getRows().size();
    }

    /**
     * <p>
     *     For a given column, 'columnToSet', set the lookup cell in the first row where the value in column 'columnToSearch'
     *     equals 'valueToSearch'. The value chosen will be at the specified index in the lookup options. Supply a 'value' in order to
     *     filter the set of options shown.
     * </p>
     *
     * @param columnToSearch fieldKey, name, or label of column to check if a row should be updated or not.
     * @param valueToSearch The value to check for in 'columnToSearch' to see if the row should be updated.
     * @param columnToSet The column to update in a row.
     * @param value Optional value to supply for filtering lookup options before selection
     * @param index The 0-based index of the option to choose from the possibly filtered list of options.
     */
    public void setCellValueForLookup(CharSequence columnToSearch, String valueToSearch, CharSequence columnToSet, @Nullable String value, int index)
    {
        setCellValueForLookup(getRowIndex(columnToSearch, valueToSearch), columnToSet, value, index);
    }

    /**
     * <p>
     *     For a given column, 'columnToSet', set the cell in the row if value in column 'columnToSearch'
     *     equals 'valueToSearch'.
     * </p>
     * <p>
     *     Rather than set one cell in a specific row, this function will loop through all the rows in the grid and
     *     will update the value in column 'columnToSet' only if the value in the column 'columnToSearch' equal
     *     'valueToSearch' in that row.
     * </p>
     * <p>
     *     The check for equality for 'valueToSearch' is case sensitive.
     * </p>
     *
     * @param columnToSearch fieldKey, name, or label of column to check if a row should be updated or not.
     * @param valueToSearch The value to check for in 'columnToSearch' to see if the row should be updated.
     * @param columnToSet The column to update in a row.
     * @param valueToSet The new value to put into column 'columnToSet'.
     */
    public void setCellValue(CharSequence columnToSearch, String valueToSearch, CharSequence columnToSet, Object valueToSet)
    {
        setCellValue(getRowIndex(columnToSearch, valueToSearch), columnToSet, valueToSet);
    }

    /**
     * <p>
     * For the identified row set the value in the identified column.
     * </p>
     * <p>
     * If the column to be updated is a look-up, the value passed in must be a list, even if it is just one value.
     * This is needed so the function knows how to set the value.
     * </p>
     *
     * @param row        Index of the row (0 based).
     * @param columnIdentifier fieldKey, name, or label of column
     * @param value      If the cell is a lookup, value should be List.of(value(s)). To use the date picker pass a 'Date', 'LocalDate', or 'LocalDateTime'
     * @return cell WebElement
     */
    public WebElement setCellValue(int row, CharSequence columnIdentifier, Object value)
    {
        return setCellValue(row, columnIdentifier, value, true, false);
    }

    /**
     * <p>
     * For the identified row set the value in the identified lookup column by selecting the given index in the lookup list.
     * </p>
     *
     * @param row        Index of the row (0 based).
     * @param columnIdentifier fieldKey, name, or label of column
     * @param value      Optional value to type in to filter the options shown
     * @param index      The index of the option to select for the lookup
     * @return cell WebElement
     */
    public WebElement setCellValueForLookup(int row, CharSequence columnIdentifier, @Nullable String value, int index)
    {
        WebElement gridCell = selectCell(row, columnIdentifier);

        ReactSelect lookupSelect = elementCache().lookupSelect(gridCell);

        lookupSelect.open();
        if (value != null)
            lookupSelect.enterValueInTextbox(value);

        List<WebElement> elements = lookupSelect.getOptionElements();
        if (elements.size() < index)
            throw new NotFoundException("Could not select option at index " + index + " in lookup for " + columnIdentifier + ". Only " + elements.size() + " options found.");
        elements.get(index).click();
        return gridCell;
    }

    /**
     * <p>
     * For the identified row set the value in the identified column.
     * </p>
     * <p>
     * If the column to be updated is a look-up, the value passed in must be a list, even if it is just one value.
     * This is needed so the function knows how to set the value.
     * </p>
     *
     * @param row        Index of the row (0 based).
     * @param columnIdentifier fieldKey, name, or label of column
     * @param value      If the cell is a lookup, value should be List.of(value(s)). To use the date picker pass a 'Date', 'LocalDate', or 'LocalDateTime'
     * @param checkContains Check to see if the value passed in is contained in the value shown in the grid after the edit.
     *                   Will be true most of the time but can be false if the field has formatting that may alter the value passed in like date values.
     * @return cell WebElement
     */
    public WebElement setCellValue(int row, CharSequence columnIdentifier, Object value, boolean checkContains, boolean centerSelectedCell)
    {
        // Normalize date values
        if (value instanceof Date date)
        {
            value = LocalDateTime.ofInstant(date.toInstant(), TimeZone.getDefault().toZoneId());
        }

        if (centerSelectedCell)
            ScrollUtils.scrollIntoView(getCell(row, columnIdentifier), center, center);

        WebElement gridCell = selectCell(row, columnIdentifier);

        if (value instanceof List)
        {
            // If this is a list assume that it will need a lookup.
            List<String> values = (List) value;

            ReactSelect lookupSelect = elementCache().lookupSelect(gridCell);

            lookupSelect.open();

            for (String _value : values)
            {
                lookupSelect.typeOptionThenSelect(_value);
            }

        }
        else if (value instanceof LocalDateTime localDateTime)
        {
            // Activate the cell.
            activateCell(gridCell);

            ReactDateTimePicker dateTimePicker = elementCache().datePicker();
            dateTimePicker.select(localDateTime);
        }
        else if (value instanceof LocalDate localDate)
        {
            activateCell(gridCell);
            ReactDateTimePicker datePicker = elementCache().datePicker();
            datePicker.selectDate(localDate);
        }
        else if (value instanceof LocalTime localTime)
        {
            activateCell(gridCell);
            ReactDateTimePicker datePicker = elementCache().datePicker();
            datePicker.selectTime(localTime);
        }
        else
        {
            String beforeText = gridCell.getText();

            activateCell(gridCell);

            String str = value.toString();
            WebElement inputCell = elementCache().inputCell();

            // Remove the text that is there.
            inputCell.clear();

            // If the cell had text calling '.clear()' requires a reactivation of the cell.
            if(!inputCell.isDisplayed())
            {
                gridCell.click();
                activateCell(gridCell);
                inputCell = elementCache().inputCell();
            }

            if(!str.isEmpty())
            {
                inputCell.sendKeys(str);
            }

            inputCell.sendKeys(Keys.RETURN); // Close the inputCell.

            getWrapper().shortWait().until(ExpectedConditions.stalenessOf(inputCell));

            if (checkContains)
            {
                // Wait until the grid cell has the updated text. Check for contains, not equal, because when updating a cell
                // the cell's new value will be the old value plus the new value and the cursor may not be placed at the end
                // of the existing value so the new value should exist somewhere in the cell text value not necessarily
                // at the end of it.
                WebDriverWrapper.waitFor(() -> gridCell.getText().contains(str),
                        "Value entered into inputCell '" + value + "' did not appear in grid cell.", WAIT_FOR_JAVASCRIPT);
            }
            else
            {
                // Wait until the grid cell is not the same as before.
                WebDriverWrapper.waitFor(() -> !gridCell.getText().equals(beforeText),
                        "Value entered into inputCell '" + value + "' did not appear in grid cell.", WAIT_FOR_JAVASCRIPT);
            }
        }
        return gridCell;
    }

    public void setEntityData(List<Map<String, Object> >data, List<FieldDefinition> fields)
    {
        for (int i = 0; i < data.size(); i++)
        {
            Map<String, Object> rowData = data.get(i);
            for (FieldDefinition field : fields) {
                Object value = rowData.get(field.getEffectiveLabel());
                if (value != null)
                    setCellValue(i, field.getName(), value);
            }
        }
    }

    public EditableGrid setRecordValues(List<Map<String, Object>> rowValues)
    {
        for (int i = 0; i < rowValues.size(); i++)
        {
            Map<String, Object> columnValues = rowValues.get(i);
            for(String fieldIdentifier : columnValues.keySet())
                setCellValue(i, fieldIdentifier, columnValues.get(fieldIdentifier), true, true);
        }
        return this;
    }

    /**
     * Set the value of a multi-line field for the given row & column. This uses javascript to set the value, not sendKeys.
     * Use '\n' for a new line.
     *
     * @param row Row to update.
     * @param columnIdentifier fieldKey, name, or label of column
     * @param value The value to set.
     */
    public void setMultiLineCellValue(int row, CharSequence columnIdentifier, String value)
    {

        WebElement gridCell = getCell(row, columnIdentifier);
        String beforeText = gridCell.getText();

        WebElement textArea = activateCellUsingDoubleClick(row, columnIdentifier);

        textArea.sendKeys(value, Keys.RETURN); // Add the RETURN to close the inputCell.

        waitFor(()->getWrapper().shortWait().until(ExpectedConditions.stalenessOf(textArea)),
                "TextArea did not go away.", 500);

        // Wait until the cell shows some kind of update before leaving.
        WebDriverWrapper.waitFor(() -> !gridCell.getText().equals(beforeText),
                "Doesn't look like the multi-line field was updated.", WAIT_FOR_JAVASCRIPT);

    }

    /**
     * Double-clicking a cell that is "text" value field will activate it and present a textArea for editing the value.
     * This will return the textArea WebElement that can be used to set the field.
     * @param row Row to be edited.
     * @param columnIdentifier fieldKey, name, or label of column
     * @return The TextArea component that can be used to edit the field.
     */
    public WebElement activateCellUsingDoubleClick(int row, CharSequence columnIdentifier)
    {
        WebElement gridCell = getCell(row, columnIdentifier);
        WebElement textArea = Locator.tag("textarea").refindWhenNeeded(gridCell);

        // Account for the cell already being active.
        if(!textArea.isDisplayed())
        {
            getWrapper().scrollIntoView(gridCell);
            getWrapper().doubleClick(gridCell);
            waitFor(textArea::isDisplayed,
                    String.format("Table cell for row %d and column '%s' was not activated.", row, columnIdentifier), 1_000);
        }
        return textArea;
    }

    /**
     * Creates a value in a select that allows the user to insert/create a value, vs. selecting from an existing/populated set
     * @param row   the row
     * @param columnIdentifier fieldKey, name, or label of column
     * @param value     value to insert
     */
    public void setNewSelectValue(int row, CharSequence columnIdentifier, String value)
    {
        WebElement gridCell = selectCell(row, columnIdentifier);

        ReactSelect createSelect = elementCache().lookupSelect(gridCell);

        createSelect.createValue(value);
    }

    /**
     * Search for a row and then clear the given cell (columnToClear) on the row.
     *
     * @param columnToSearch Column to search.
     * @param valueToSearch Value in the column to search for.
     * @param columnToClear Column to clear.
     */
    public void clearCellValue(CharSequence columnToSearch, String valueToSearch, CharSequence columnToClear)
    {
        clearCellValue(getRowIndex(columnToSearch, valueToSearch), columnToClear);
    }

    /**
     * Clear the cell (columnIdentifier) in the row.
     *
     * @param row Row of the cell to clear.
     * @param columnIdentifier fieldKey, name, or label of column
     */
    public void clearCellValue(int row, CharSequence columnIdentifier)
    {
        selectCell(row, columnIdentifier);
        new Actions(getDriver()).sendKeys(Keys.DELETE).perform();
    }

    /**
     * For a given row get the value in the given column.
     *
     * @param row The row index (0 based).
     * @param columnIdentifier fieldKey, name, or label of column
     * @return The string value of the {@link WebElement} that is the cell.
     */
    public String getCellValue(int row, CharSequence columnIdentifier)
    {
        return getCellValue(getCell(row, columnIdentifier));
    }

    private String getCellValue(WebElement cell)
    {
        return cell.getText().trim();
    }

    /**
     * Dismiss the dropdown list that is currently displayed on the grid.
     *
     * @return A reference to this EditableGrid.
     */
    public EditableGrid dismissDropdownList()
    {
        ReactSelect.finder(getDriver()).find(getComponentElement()).close();

        return this;
    }

    /**
     * For the given row get the values displayed in the dropdown list for the given column.
     *
     * @param row The 0 based row index.
     * @param columnIdentifier fieldKey, name, or label of column
     * @return A list of strings from the dropdown list. If the cell does not have a dropdown then an empty list is returned.
     */
    public List<String> getDropdownListForCell(int row, CharSequence columnIdentifier)
    {
        return getFilteredDropdownListForCell(row, columnIdentifier, null);
    }

    /**
     * For the given row and column type some text into the cell to get the 'filtered' values displayed in the dropdown list.
     * If this cell is not a lookup cell, does not have a dropdown, the text will not be entered and an empty list will be returned.
     *
     * @param row A 0 based index containing the cell.
     * @param columnIdentifier fieldKey, name, or label of column
     * @param filterText The text to type into the cell. If the value is null it will not filter the list.
     * @return A list values shown in the dropdown list after the text has been entered.
     */
    public List<String> getFilteredDropdownListForCell(int row, CharSequence columnIdentifier, @Nullable String filterText)
    {

        WebElement gridCell = selectCell(row, columnIdentifier);

        ReactSelect lookupSelect = elementCache().lookupSelect(gridCell);

        // If the click did not expand the select this will.
        // This will have no effect if the list is expended.
        lookupSelect.open();

        if (StringUtils.isNotBlank(filterText))
        {
            lookupSelect.enterValueInTextbox(filterText);
        }

        return lookupSelect.getOptions();
    }

    /**
     * Values will be quoted appropriately for pasting into editable grid lookups.
     */
    public static String getPastableColumn(List<?> values)
    {
        List<String> valueList = new ArrayList<>();
        for (Object value : values)
        {
            String strVal = CSVFormat.DEFAULT.format(value); // Just quote commas
            valueList.add(strVal);
        }
        return String.join("\n", valueList);
    }

    /**
     * Pastes text to a single column of the grid.
     * @param columnIdentifier fieldKey, name, or label of column
     * @param pasteValues      list of values to paste
     * @return A Reference to this editableGrid object.
     */
    public EditableGrid pasteColumn(CharSequence columnIdentifier, List<?> pasteValues)
    {
        if (pasteValues.isEmpty())
            throw new IllegalArgumentException("No paste values provided");
        return pasteFromCell(0, columnIdentifier, getPastableColumn(pasteValues), false);
    }

    /**
     * Pastes delimited text to the grid, via a single target.  The component is clever enough to target
     * text into cells based on text delimiters; thus we can paste a square of data into the grid.
     * @param row           index of the target cell
     * @param columnIdentifier fieldKey, name, or label of column
     * @param pasteText     tab-delimited or csv or excel data
     * @return A Reference to this editableGrid object.
     */
    public EditableGrid pasteFromCell(int row, CharSequence columnIdentifier, String pasteText)
    {
        return pasteFromCell(row, columnIdentifier, pasteText, false);
    }

    /**
     * Pastes delimited text to the grid, via a single target.  The component is clever enough to target
     * text into cells based on text delimiters; thus we can paste a square of data into the grid.
     * @param row           index of the target cell
     * @param columnIdentifier fieldKey, name, or label of column
     * @param pasteText     tab-delimited or csv or excel data
     * @param validate      whether to await/confirm the presence of pasted text before resuming
     * @return A Reference to this editableGrid object.
     */
    public EditableGrid pasteFromCell(int row, CharSequence columnIdentifier, String pasteText, boolean validate)
    {
        int initialRowCount = getRowCount();
        WebElement gridCell = getCell(row, columnIdentifier);
        String indexValue = gridCell.getText();
        selectCell(gridCell);

        getWrapper().actionPaste(null, pasteText);

        // wait for the cell value to change or the rowcount to change, and the target cell to go into highlight,
        // ... or for a second and a half
        WebDriverWrapper.waitFor(()-> (getRowCount() > initialRowCount || !indexValue.equals(gridCell.getText())) &&
                        isInSelection(gridCell), 1500);
        if (validate)
            waitForAnyPasteContent(pasteText);

        return this;
    }

    /**
     * Awaits any elements (except for empty, or space-only) of the pasted content to be present in the grid
     * @param pasteContent  tab-separated text
     */
    protected void waitForAnyPasteContent(String pasteContent)
    {
        // split pasteContent into its parts
        var contentParts = pasteContent.replace("\n", "\t").split("\t");
        // filter out empty and space-only values
        var filteredParts = Arrays.stream(contentParts).filter(a-> !a.isEmpty() && !a.equals(" ")).collect(Collectors.toList());
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(()-> Assertions.assertThat(getSelectionCellTexts())
                        .containsAnyElementsOf(filteredParts));
    }

    /**
     * Awaits all elements (except empty or space-only) of the pasted content to be present in the grid.
     * Use this to validate all expected content appears after pasting to the grid
     * @param pasteContent  tab-separated text of the sort usually pasted into the edit grid
     */
    public void waitForPasteContent(String pasteContent)
    {
        // split pasteContent into its parts
        var contentParts = pasteContent.split("\\s*[\n\t]\\s*");
        // filter out empty and space-only values
        var filteredParts = Arrays.stream(contentParts)
                .filter(a-> !a.isBlank())
                .map(str -> {
                    if (str.startsWith("\"") && str.endsWith("\""))
                    {
                        // reverse TsvQuoter.quote
                        str = str.replaceAll("\"\"", "\"");
                        str = str.substring(1, str.length() - 1); // remove surrounding quotes
                    }
                    return str;
                })
                .collect(Collectors.toList());
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(()-> Assertions.assertThat(getSelectionCellTexts())
                        .containsAll(filteredParts));
    }

    // captures the texts of any cells currently in selection
    public List<String> getSelectionCellTexts()
    {
        var cells = Locator.tagWithClass("div", "cellular-display")
                .withAttributeContaining("class","cell-selection").findElements(this);
        return getWrapper().getTexts(cells);
    }

    public List<WebElement> getSelectedCells()
    {
        return Locator.tagWithClass("div", "cell-selection").parent("td").findElements(this);
    }

    /**
     * Pastes a single value into as many cells as are selected, or supports pasting a square shaped blob of data
     * of the same shape as the prescribed selection.  If a single value is supplied, that value will be put into
     * every cell in the selection.  If the data doesn't match the selection dimensions (e.g., has fewer or more columns)
     * the grid should produce an error/alert.
     * @param pasteText     The text to paste
     * @param startRowIndex index of the starting row
     * @param startColumn   fieldKey, name, or label of the starting cell
     * @param endRowIndex   index of the ending row
     * @param endColumn     fieldKey, name, or label of the ending cell
     * @return  the current grid instance
     */
    public EditableGrid pasteMultipleCells(String pasteText, int startRowIndex, CharSequence startColumn, int endRowIndex, CharSequence endColumn)
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
     * @param startColumn   fieldKey, name, or label of the top-left cell
     * @param endRowIndex   Index of the bottom-right cell's row
     * @param endColumn     fieldKey, name, or label of the bottom-right cell
     * @return  the text contained in the prescribed selection
     */
    public String copyCellRange(int startRowIndex, CharSequence startColumn, int endRowIndex, CharSequence endColumn) throws IOException, UnsupportedFlavorException
    {
        WebElement startCell = getCell(startRowIndex, startColumn);
        WebElement endCell = getCell(endRowIndex, endColumn);
        selectCellRange(startCell, endCell);
        return copyCurrentSelection();
    }

    /**
     * Selects all cells in the table, then copies their contents into delimited text
     * @return  delimited text content of the cells in the grid
     */
    public String copyAllCells() throws IOException, UnsupportedFlavorException
    {
        selectAllCells();
        WebDriverWrapper.waitFor(this::areAllInSelection,
                "expect all cells to be selected before copying grid values", 1500);

        String selection = copyCurrentSelection();
        if (selection.isEmpty())
        {
            log("initial attempt to copy current selection came up empty.  re-trying after 3000 ms");
            new WebDriverWait(getDriver(), Duration.ofSeconds(3));
            return copyCurrentSelection();
        }
        return selection;
    }

    /**
     * Selects all cells in the table, then deletes their content
     */
    public void clearAllCells()
    {
        selectAllCells();
        new Actions(getDriver()).sendKeys(Keys.DELETE).perform();
    }

    public String copyCurrentSelection() throws IOException, UnsupportedFlavorException
    {
        // now copy the contents of the current selection to the clipboard
        Keys cmdKey = MODIFIER_KEY;
        Actions actions = new Actions(getDriver());
        actions.keyDown(cmdKey)
                .sendKeys( "c")
                .keyUp(cmdKey)
                .build()
                .perform();

        return getWrapper().getClipboardContent();
    }

    public void dragFill(WebElement startCell, WebElement endCell)
    {
        Locator.XPathLocator selectionHandleLoc = Locator.byClass("cell-selection-handle");
        WebElement selectionHandle = selectionHandleLoc.findElement(startCell);
        dragToCell(selectionHandle, endCell);
        selectionHandleLoc.waitForElement(endCell, 5_000);
    }

    public void selectCellRange(WebElement startCell, WebElement endCell)
    {
        dragToCell(startCell, endCell);

        WebDriverWrapper.waitFor(()-> isInSelection(startCell) && isInSelection(endCell),
                "Cell range did not become selected", 2000);
    }

    private void dragToCell(WebElement elementToDrag, WebElement destinationCell)
    {
        var size = destinationCell.getSize();
        Locator.XPathLocator selectionHandleLoc = Locator.byClass("no-margin-top detail__header--name");
        WebElement title = selectionHandleLoc.findElement(getDriver());

        new Actions(getDriver())
                // Action to avoid tooltip
                .moveToElement(title)
                // WebDriver doesn't calculate correct location to click the cell selection handle
                .moveToElement(elementToDrag, 0, 7)
                .clickAndHold()
                .moveToElement(destinationCell)
                // Extra wiggle to get it to stick
                .moveByOffset(0, -size.getHeight())
                .moveByOffset(0, size.getHeight())
                .release()
                .perform();
    }

    private void selectAllCells()
    {
        if (areAllInSelection())
            return;

        int indexOffset = hasSelectColumn() ? 1 : 0;
        selectCell(getCell(0, getColumnLabels().get(1 + indexOffset)));    // forces the index cell into selected state
                                                            // this resets the grid state to a known base condition
        // use 'ctrl-a' to select the entire grid
        Keys cmdKey = MODIFIER_KEY;
        new Actions(getDriver()).keyDown(cmdKey).sendKeys("a").keyUp(cmdKey).build().perform();
        WebDriverWrapper.waitFor(this::areAllInSelection,
                "the expected cells did not become selected", 3000);
    }

    public WebElement selectCell(int row, CharSequence columnIdentifier)
    {
        // Get a reference to the cell.
        WebElement gridCell = getCell(row, columnIdentifier);

        // Select the cell.
        selectCell(gridCell);
        return gridCell;
    }

    /**
     * puts the specified cell into a selected state (appears as a dark-blue outline) with an active input present in it.
     */
    private void selectCell(WebElement cell)
    {
        getWrapper().scrollIntoView(cell);

        if (isCellSelected(cell))
            return;

        cell.click();

        // The initial click may work but the selection style may go away in some scenarios, like click happening
        // before some required cells are populated. This is a retry to protect against those scenarios.
        if (Boolean.FALSE.equals(WebDriverWrapper.waitFor(()->  isCellSelected(cell), 1_000)))
        {
            cell.click();
            WebDriverWrapper.waitFor(()->  isCellSelected(cell), "The target cell did not become selected.", 1_000);
        }
    }

    private void activateCell(WebElement cell)
    {
        // If it is a selector, and it already has focus (is active), it will not have a div.cellular-display
        if (Locator.tagWithClass("div", "select-input__control--is-focused").findElements(cell).isEmpty())
            sendKeysToCell(cell, Keys.ENTER);
    }

    public void sendKeysToCell(WebElement cell, CharSequence... keysToSend)
    {
        var cellContent = Locator.tagWithClass("div", "cellular-display").findElement(cell);
        cellContent.sendKeys(keysToSend);
    }

    /**
     * Tests the specified webElement to see if it is in 'cell-selected' state, which means it has an active/focused input in it
     * @param cell A WebElement that is the grid cell (a  td).
     * @return True if the edit is present
     */
    public boolean isCellSelected(WebElement cell)
    {
        try
        {
            // If the cell is a reactSelect, and it is open/active, this will throw a NoSuchElementException because the
            // div will not have the cell-selected in the class attribute.
            return Locator.tagWithClass("div", "cellular-display")
                    .findElement(cell)
                    .getDomAttribute("class").contains("cell-selected");
        }
        catch (NoSuchElementException nse)
        {
            // If the cell is an open/active reactSelect the class attribute is different.
            return Locator.tagWithClass("div", "select-input__control")
                    .findElement(cell)
                    .getDomAttribute("class").contains("select-input__control--is-focused");
        }
    }

    /**
     *  tests the specified cell element to see if it is highlighted as a single-or-multi-cell selection area.  this appears as
     *  light-blue background, and is distinct from 'selected'
     * @param cell WebElement (grid td) to check.
     */
    private boolean isInSelection(WebElement cell)  // 'in selection' shows as blue color, means it is part of one or many selected cells for copy/paste, etc
    {
        // Should not need to add code for a reactSelect here. A selection involves clicking/dragging, which closes the reactSelect.
        return Locator.tagWithClass("div", "cellular-display")
                .findElement(cell)
                .getDomAttribute("class").contains("cell-selection");
    }

    /**
     * attempts to determine whether the entire grid is selected
     * assumes that the first row is never selectable (it's either a selector row, or a row-number cell)
     * @return  True if the top-left and bottom-right cells are 'in-selection', otherwise false
     */
    private boolean areAllInSelection()
    {
        List<String> columns = getColumnLabels();
        int selectIndexOffset = hasSelectColumn() ? 1 : 0;
        WebElement indexCell = getCell(0, columns.get(1 + selectIndexOffset));
        WebElement endCell = getCell(elementCache().getRows().size()-1, columns.get(columns.size()-1));
        return (isInSelection(indexCell) && isInSelection(endCell));
    }

    public boolean hasCellError(int row, CharSequence columnIdentifier)
    {
        WebElement gridCell = getCell(row, columnIdentifier);
        return cellHasError(gridCell);
    }

    private boolean cellHasError(WebElement cell)
    {
        return Locator.tagWithClass("div", "cell-error").existsIn(cell);
    }

    /**
     * @param row row index
     * @param columnIdentifier fieldKey, name, or label of column
     * @return error text in the specified cell or 'null' if there is no error
     */
    public String getCellError(int row, CharSequence columnIdentifier)
    {
        WebElement gridCell = getCell(row, columnIdentifier);

        if (cellHasError(gridCell))
            return Locator.tagWithClass("div", "cell-error").findElement(gridCell).getText();
        return null;
    }

    /**
     * @param row row index
     * @param columnIdentifier fieldKey, name, or label of column
     * @return error popover text in the specified cell or 'null' if there is no error
     */
    public String getErrorPopoverText(int row, CharSequence columnIdentifier)
    {
        WebElement gridCell = getCell(row, columnIdentifier);

        if (cellHasError(gridCell))
            return getCellPopoverText(row, columnIdentifier);
        return null;
    }

    /**
     * @param row row index
     * @param columnIdentifier fieldKey, name, or label of column
     * @return popover text when mousing over the specified cell or 'null' if there is none
     */
    public String getCellPopoverText(int row, CharSequence columnIdentifier)
    {
        dismissPopover(); // Other popovers can block the target cell
        getWrapper().mouseOver(Locator.tag("td").findElement(getRow(row))); // Avoid passing over any header cells on the way to the target cell
        WebElement cellDiv = Locator.tagWithClass("div", "cellular-display").findElement(getCell(row, columnIdentifier));
        getWrapper().mouseOver(cellDiv);   // cause the tooltip to be present
        return Optional.ofNullable(WebDriverWrapper.waitFor(()-> Locators.popover.findElementOrNull(getDriver()), 1000))
            .map(WebElement::getText)
            .orElse(null);
    }

    public void dismissPopover()
    {
        Locators.popover.findOptionalElement(getDriver()).ifPresent(popover -> {
            getWrapper().mouseOver(popover);
            getWrapper().mouseOut();
            getWrapper().mouseOver(elementCache().getGridHeaderManager().getColumnHeader(0).getElement());
            getWrapper().shortWait().until(ExpectedConditions.invisibilityOf(popover));
        });
    }

    public List<WebElement> getCellErrors()
    {
        return Locator.tagWithClass("div", "cell-error").findElements(this);
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

    public void setAddRows(int count)
    {
        elementCache().addCountInput.set(String.valueOf(count));
    }

    public void addRows(int count)
    {
        setAddRows(count);
        doAndWaitForRowCountUpdate(() -> {
            elementCache().addRowsButton.click();
        });
    }

    private void doAndWaitForRowCountUpdate(Runnable func)
    {
        int initialCount = getRowCount();

        func.run();

        waitFor(() -> getRowCount() != initialCount, "Failed to add/remove rows", 5_000);
    }

    /**
     * Wait for column count to change after the provided action
     */
    public void doAndWaitForColumnUpdate(Runnable func)
    {
        int initialCount = elementCache().findHeaders().size();

        func.run();

        waitFor(() -> {
            clearElementCache();
            return elementCache().findHeaders().size() != initialCount;
        }, "Failed to add/remove column", 5_000);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement topControls = Locator.byClass("editable-grid-buttons__action-buttons").findWhenNeeded(this);
        final WebElement bulkAddBtn = Locator.byClass("bulk-add-button").findWhenNeeded(topControls);
        final WebElement bulkUpdateBtn = Locator.byClass("bulk-update-button").findWhenNeeded(topControls);
        final WebElement deleteRowsBtn =  Locator.byClass("bulk-remove-button").findWhenNeeded(topControls);
        final ExportMenu exportMenu = ExportMenu.finder(getDriver()).findWhenNeeded(topControls);
        final WebElement table = Locator.byClass("table-cellular").findWhenNeeded(this);
        private final Checkbox selectAllCheckbox = new Checkbox(Locator.xpath("//th/input[@type='checkbox']").findWhenNeeded(table));
        private final CachingSupplier<Boolean> hasSelectColumn = new CachingSupplier<>(selectAllCheckbox::isDisplayed);

        Checkbox getCheckbox(int rowIndex)
        {
            return new Checkbox(Locator.css("td > .table-cell-content > input[type=checkbox]").findElement(getRow(rowIndex)));
        }

        protected WebElement getColumnHeaderCell(CharSequence columnIdentifier)
        {
            return findColumnHeader(columnIdentifier).getElement();
        }

        private FieldReferenceManager _fieldReferenceManager;
        protected FieldReferenceManager getGridHeaderManager()
        {
            if (_fieldReferenceManager == null)
            {
                List<EditableGridColumnHeader> columnHeaders = new ArrayList<>();
                List<WebElement> headerCellElements = Locators.headerCells.waitForElements(table, WAIT_FOR_JAVASCRIPT);
                int domIndex = 0;

                if (hasSelectColumn())
                {
                    columnHeaders.add(new EditableGridColumnHeader(headerCellElements.get(0), domIndex, SELECT_COLUMN_LABEL_PLACEHOLDER));
                    domIndex++;
                }

                for (; domIndex < headerCellElements.size(); domIndex++)
                {
                    columnHeaders.add(new EditableGridColumnHeader(headerCellElements.get(domIndex), domIndex));
                }

                _fieldReferenceManager = new FieldReferenceManager(columnHeaders);
            }
            return _fieldReferenceManager;
        }

        protected List<FieldReference> findHeaders()
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
            return findHeaders().stream().map(FieldReference::getLabel).collect(Collectors.toList());
        }

        public WebElement inputCell()
        {
            return Locators.inputCell.refindWhenNeeded(table);
        }

        public ReactSelect lookupSelect(WebElement cell)
        {
            Locator.byClass("cell-menu-selector").findOptionalElement(cell).ifPresent(WebElement::click);
            ReactSelect lookupSelect = ReactSelect.finder(getDriver()).timeout(2_000).find(table);
            waitFor(()->lookupSelect.isInteractive() && !lookupSelect.isLoading(), "Select control is not ready.", 1_000);
            return lookupSelect;
        }

        public ReactDateTimePicker datePicker()
        {
            return new ReactDateTimePicker.ReactDateTimeInputFinder(getDriver()).withClassName("date-input-cell").find(table);
        }

        final WebElement addRowsPanel = Locator.byClass("editable-grid__controls").findWhenNeeded(this);
        final Input addCountInput = Input.Input(Locator.name("addCount"), getDriver()).findWhenNeeded(addRowsPanel);
        final WebElement addRowsButton = Locator.byClass("btn-primary").findWhenNeeded(addRowsPanel);

        List<WebElement> getRows()
        {
            return Locators.rows.findElements(table);
        }
    }

    protected abstract static class Locators
    {
        private Locators()
        {
            // Do nothing constructor to prevent instantiation.
        }

        static final Locator loadingGrid = Locator.css("tbody tr.grid-loading");
        static final Locator emptyGrid = Locator.css("tbody tr.grid-empty");
        static final Locator spinner = Locator.css(".fa-spinner");
        static final Locator.XPathLocator rows = Locator.tag("tbody").childTag("tr").withoutClass("grid-empty").withoutClass("grid-loading");
        static final Locator headerCells = Locator.css("thead tr th");
        static final Locator inputCell = Locator.css(".eg-input-cell");
        static final Locator popover = Locator.byClass("popover");
    }

    public static class EditableGridFinder extends WebDriverComponent.WebDriverComponentFinder<EditableGrid, EditableGridFinder>
    {
        private final Locator _locator = Locator.byClass("editable-grid__container").parent();

        public EditableGridFinder(WebDriver driver)
        {
            super(driver);
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

    protected static class EditableGridColumnHeader extends FieldReferenceManager.FieldReference
    {
        private final Mutable<String> _fieldLabel = new MutableObject<>();

        public EditableGridColumnHeader(WebElement element, int domIndex)
        {
            super(element, domIndex);
        }

        public EditableGridColumnHeader(WebElement element, int domIndex, String label)
        {
            this(element, domIndex);
            _fieldLabel.setValue(label);
        }

        @Override
        public String getLabel()
        {
            if (_fieldLabel.getValue() == null)
            {
                _fieldLabel.setValue(getLabelFromHeaderCell(getElement()).trim());
            }
            return _fieldLabel.getValue();
        }


        /**
         * Extract label from header cell. Editable grid header cells have several different layouts. What they have in
         * common is that the label is the first text node in the cell, possibly within a &lt;span&gt;
         */
        private String getLabelFromHeaderCell(WebElement el)
        {
            // Use text nodes to ignore browser whitespace formatting
            List<String> textNodes = WebElementUtils.getTextNodesWithin(el);
            if (textNodes.isEmpty())
            {
                List<WebElement> children = Locator.xpath("./*").findElements(el);
                if (children.isEmpty())
                {
                    return ""; // probably the selection checkbox column
                }
                else
                {
                    // Depth-first search until we find some text
                    return getLabelFromHeaderCell(children.get(0));
                }
            }
            else
            {
                boolean required = Locator.byClass("required-symbol").existsIn(el);
                String label = textNodes.get(0).trim(); // trim trailing NBSP
                return label + (required ? " *" : ""); // re-add required asterisk for tests that expect it
            }
        }
    }
}
