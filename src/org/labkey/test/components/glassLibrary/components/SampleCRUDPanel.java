package org.labkey.test.components.glassLibrary.components;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.glassLibrary.grids.EditableGrid;
import org.labkey.test.components.react.dialogs.BulkCreateSamplesDialog;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;

// This is the same component (collection of atomic elements) used in insertAssay and createSamples.

public class SampleCRUDPanel extends WebDriverComponent<SampleCRUDPanel.ElementCache>
{

    private final WebDriver _driver;
    private final WebElement _editingDiv;

    public SampleCRUDPanel(WebElement element, WebDriver driver)
    {
        _driver = driver;
        _editingDiv = element;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _editingDiv;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    public SampleCRUDPanel addRecords(List<Map<String, Object>> records)
    {
        getWrapper().setFormElement(elementCache().addRowsTxtBox, Integer.toString(records.size()));
        elementCache().addRowsButton.click();

        List<Integer> rowIndices = elementCache().grid.listOfnotPopulatedRows();

        if(rowIndices.size() < records.size())
        {
            throw new IllegalStateException("Trying to add more records than there are rows. Number of records to create: " + records.size() + " number of available rows: " + rowIndices.size());
        }

        int index = 0;

        for(Map<String, Object> record : records)
        {
            setRecordValues(record, rowIndices.get(index));
            index++;
        }

        return this;
    }

    public SampleCRUDPanel setRecordValues(Map<String, Object> columnValues)
    {
        int insertRowIndex = elementCache().grid.listOfnotPopulatedRows().get(0);
        return setRecordValues(columnValues, insertRowIndex);
    }

    public SampleCRUDPanel setRecordValues(Map<String, Object> columnValues, int row)
    {
        for(String columnName : columnValues.keySet())
        {
            elementCache().grid.setCellValue(row, columnName, columnValues.get(columnName));
        }

        return this;
    }

    public EditableGrid getEditableGrid()
    {
        return elementCache().grid;
    }

    public List<String> getColumnHeaders()
    {
        return elementCache().grid.getColumnNames();
    }

    public List<Map<String, String>> getGridData()
    {
        return elementCache().grid.getGridData();
    }

    public boolean isGridVisible()
    {
        return elementCache().grid.isDisplayed();
    }

    public SampleCRUDPanel setAddRows(int numOfRows)
    {
        getWrapper().setFormElement(elementCache().addRowsTxtBox, Integer.toString(numOfRows));
        return this;
    }

    public SampleCRUDPanel clickAddRows()
    {
        elementCache().addRowsButton.click();
        return this;
    }

    public BulkCreateSamplesDialog clickBulkInsert()
    {
        elementCache().bulkInsert.click();
        return new BulkCreateSamplesDialog(getDriver());
    }

    public boolean isBulkInsertVisible()
    {
        return isElementVisible(elementCache().bulkInsert);
    }

    public boolean isDeleteRowsVisible()
    {
        return isElementVisible(elementCache().deleteRows);
    }

    protected boolean isElementVisible(WebElement element)
    {
        try
        {
            return element.isDisplayed();
        }
        catch(NoSuchElementException nse)
        {
            return false;
        }

    }

    protected boolean isVisible(Locator locator)
    {
        try
        {
            return getWrapper().isElementVisible(locator);
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

    protected class ElementCache extends Component.ElementCache
    {
        WebElement bulkInsert = Locator.button("Bulk Insert").findWhenNeeded(_driver);
        WebElement deleteRows = Locator.button("Delete rows").findWhenNeeded(_driver);
        EditableGrid grid = new EditableGrid.EditableGridFinder(_driver).findWhenNeeded();
        WebElement addRowsTxtBox = Locator.tagWithName("input", "addCount").findWhenNeeded(_driver);
        WebElement addRowsButton = Locator.buttonContainingText("Add").findWhenNeeded(_driver);
    }

    public static class InsertRecordFinder extends WebDriverComponent.WebDriverComponentFinder<SampleCRUDPanel, InsertRecordFinder>
    {

        private Locator _locator;

        public InsertRecordFinder(WebDriver driver)
        {
            super(driver);
            _locator = Locator.tagWithClass("div", "panel").child(Locator.tagWithClass("div", "panel-body"));
        }

        @Override
        protected SampleCRUDPanel construct(WebElement element, WebDriver driver)
        {
            return new SampleCRUDPanel(element, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }

    }

}
