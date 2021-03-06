package org.labkey.test.components.ui.entities;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.ui.grids.EditableGrid;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;

/**
 * This class automates the UI component defined in <a href="https://github.com/LabKey/labkey-ui-components/blob/master/packages/components/src/components/entities/EntityInsertPanel.tsx">components/entities/EntityInsertPanel.tsx</a>
 * This is the same component (collection of atomic elements) used in insertAssay and createSamples.
 */
public class EntityInsertPanel extends WebDriverComponent<EntityInsertPanel.ElementCache>
{

    private final WebDriver _driver;
    private final WebElement _editingDiv;

    public EntityInsertPanel(WebElement element, WebDriver driver)
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
    public WebDriver getDriver()
    {
        return _driver;
    }

    public ReactSelect getEntityTypeSelect(String labelText)
    {
        return ReactSelect.finder(getDriver()).followingLabelWithSpan(labelText).waitFor();
    }

    public EntityInsertPanel addParent(String label, String parentType)
    {
        elementCache().addParent.click();
        getEntityTypeSelect(label).select(parentType);
        return this;
    }

    public ReactSelect getParentSelect(String label)
    {
        if (!ReactSelect.finder(getDriver()).followingLabelWithSpan(label).findOptional(this).isPresent())
            elementCache().addParent.click();
        return getEntityTypeSelect(label);
    }

    public EntityInsertPanel clearParents()
    {
        Locator loc = Locator.tagWithClass("span", "container--action-button")
                .withChild(Locator.tagWithClass("i", "container--removal-icon")).withText("Remove Parent 1");

        getWrapper().waitFor(()-> loc.findElementOrNull(getDriver()) != null, 1500);  // it's okay if it isn't there

        while (loc.findElementOrNull(getDriver()) != null)      // click the top one until they are all gone
        {
            WebElement parentBtn = loc.findElement(getDriver());
            getWrapper().log("removing parent " + parentBtn.getText());
            parentBtn.click();
            getWrapper().sleep(500);
        }

        return this;
    }

    public EntityInsertPanel addRecords(List<Map<String, Object>> records)
    {
        getWrapper().setFormElement(elementCache().addRowsTxtBox, Integer.toString(records.size()));
        elementCache().addRowsButton.click();

        List<Integer> rowIndices = elementCache().grid.getEditableRowIndices();

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

    public EntityInsertPanel setRecordValues(Map<String, Object> columnValues)
    {
        int insertRowIndex = elementCache().grid.getEditableRowIndices().get(0);
        return setRecordValues(columnValues, insertRowIndex);
    }

    public EntityInsertPanel setRecordValues(Map<String, Object> columnValues, int row)
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

    public EntityInsertPanel setAddRows(int numOfRows)
    {
        getWrapper().setFormElement(elementCache().addRowsTxtBox, Integer.toString(numOfRows));
        return this;
    }

    public EntityInsertPanel clickAddRows()
    {
        elementCache().addRowsButton.click();
        return this;
    }

    public EntityInsertPanel clickRemove()
    {
        getEditableGrid().doAndWaitForUpdate(()->
                elementCache().deleteRows.click());
        return this;
    }

    public EntityBulkInsertDialog clickBulkInsert()
    {
        elementCache().bulkInsert.click();
        return new EntityBulkInsertDialog(getDriver());
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
        Locator deleteRowsBtnLoc = Locator.XPathLocator.union(
                Locator.button("Delete rows"),
                Locator.buttonContainingText("Remove"));

        WebElement bulkInsert = Locator.button("Bulk Insert").findWhenNeeded(this);
        WebElement bulkUpdate = Locator.button("Bulk update").findWhenNeeded(this);
        WebElement deleteRows = deleteRowsBtnLoc.findWhenNeeded(this);
        EditableGrid grid = new EditableGrid.EditableGridFinder(_driver).findWhenNeeded();
        WebElement addRowsTxtBox = Locator.tagWithName("input", "addCount").findWhenNeeded(this);
        WebElement addRowsButton = Locator.buttonContainingText("Add").findWhenNeeded(this);

        WebElement addParent = Locator.tagWithClass("span", "container--action-button")
                .containing("Parent").findWhenNeeded(getDriver());
    }

    public static class EntityInsertPanelFinder extends WebDriverComponent.WebDriverComponentFinder<EntityInsertPanel, EntityInsertPanelFinder>
    {
        private Locator _locator;

        public EntityInsertPanelFinder(WebDriver driver)
        {
            super(driver);
            _locator = Locator.tagWithClass("div", "panel").child(Locator.tagWithClass("div", "panel-body"));
        }

        @Override
        protected EntityInsertPanel construct(WebElement element, WebDriver driver)
        {
            return new EntityInsertPanel(element, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }

    }

}
