package org.labkey.test.components.ui.entities;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.ui.files.FileUploadPanel;
import org.labkey.test.components.ui.grids.EditableGrid;
import org.labkey.test.components.ui.grids.ResponsiveGrid;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

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

    public ReactSelect entityTypeSelect()
    {
        return ReactSelect.finder(getDriver()).withIdStartingWith("targetEntityType").waitFor();
    }

    public ReactSelect getEntityTypeSelect(String labelText)
    {
        return ReactSelect.finder(getDriver()).followingLabelWithSpan(labelText).findWhenNeeded(getDriver());
    }

    public EntityInsertPanel addParent(String label, String parentType)
    {
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().addParent));
        elementCache().addParent.click();
        getWrapper().waitForElement(Locator.tag("label").withChild(Locator.tagWithText("span", label)));
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

        List<Integer> rowIndices = grid().getEditableRowIndices();

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
        int insertRowIndex = grid().getEditableRowIndices().get(0);
        return setRecordValues(columnValues, insertRowIndex);
    }

    public EntityInsertPanel setRecordValues(Map<String, Object> columnValues, int row)
    {
        for(String columnName : columnValues.keySet())
        {
            grid().setCellValue(row, columnName, columnValues.get(columnName));
        }

        return this;
    }

    public EditableGrid getEditableGrid()
    {
        showGrid();
        return grid();
    }

    public FileUploadPanel getFileUploadPanel()
    {
        showFileUpload();
        return fileUploadPanel();
    }

    private EditableGrid grid()
    {
        return new EditableGrid.EditableGridFinder(_driver).timeout(WAIT_FOR_JAVASCRIPT).waitFor(this);
    }

    public EntityInsertPanel setUpdateDataForFileUpload(boolean checked)
    {
        showFileUpload();
        elementCache().updateDataCheckbox.set(checked);
        return this;
    }

    private FileUploadPanel fileUploadPanel()
    {
        return new FileUploadPanel.FileUploadPanelFinder(_driver).timeout(WAIT_FOR_JAVASCRIPT).waitFor(this);
    }

    public List<String> getColumnHeaders()
    {
        showGrid();
        return grid().getColumnNames();
    }

    public List<Map<String, String>> getGridData()
    {
        showGrid();
        return grid().getGridData();
    }

    public boolean isGridVisible()
    {
        return grid().isDisplayed();
    }

    public EntityInsertPanel setAddRows(int numOfRows)
    {
        showGrid();
        getWrapper().setFormElement(elementCache().addRowsTxtBox, Integer.toString(numOfRows));
        return this;
    }

    public EntityInsertPanel clickAddRows()
    {
        showGrid();
        elementCache().addRowsButton.click();
        return this;
    }

    public EntityInsertPanel clickRemove()
    {
        showGrid();
        getEditableGrid().doAndWaitForUpdate(()->
                elementCache().deleteRowsBtn().click());
        return this;
    }

    public EntityBulkInsertDialog clickBulkInsert()
    {
        showGrid();
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().bulkInsertBtn()));
        elementCache().bulkInsertBtn().click();
        return new EntityBulkInsertDialog(getDriver());
    }

    public boolean isBulkInsertVisible()
    {
        return modeSelectListItem("from Grid").withClass("active").findOptionalElement(this).isPresent() &&
                isElementVisible(elementCache().bulkInsertBtn());
    }

    public boolean isDeleteRowsVisible()
    {
        return modeSelectListItem("from Grid").withClass("active").findOptionalElement(this).isPresent() &&
                isElementVisible(elementCache().deleteRowsBtn());
    }

    public boolean isFileUploadVisible()
    {
        return modeSelectListItem("from File").withClass("active").findOptionalElement(this).isPresent() &&
                isElementVisible(fileUploadPanel().getComponentElement());
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

    public EntityInsertPanel showGrid()
    {
        if (!isGridVisible())
            elementCache().modeSelectListItem("from Grid")
                    .waitForElement(this, 2000).click();
        clearElementCache();
        WebDriverWrapper.waitFor(()-> isGridVisible(),
                "the grid did bot become visible", 2000);
        return this;
    }

    public ResponsiveGrid uploadFileExpectingPreview(File file)
    {
        showFileUpload();
        fileUploadPanel().uploadFile(file);
        return new ResponsiveGrid.ResponsiveGridFinder(getDriver()).waitFor(this);
    }

    public EntityInsertPanel showFileUpload()
    {
        if (!isFileUploadVisible())
        {
            var toggle = elementCache().modeSelectListItem("from File")
                    .waitForElement(this, 2000);
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(toggle));
            toggle.click();
        }
        clearElementCache();
        WebDriverWrapper.waitFor(()-> isFileUploadVisible(),
                "the file upload panel did bot become visible", 2000);
        return this;
    }

    private void waitForLoaded()
    {
        WebDriverWrapper.waitFor(()-> {
            try
            {
                return  isGridVisible() ||          // when uploading assay data there is no target select
                        isFileUploadVisible() ||
                        entityTypeSelect().isInteractive();
            }catch (NoSuchElementException nse)
            {
                return false;
            }
        }, "The insert panel did not become loaded", WAIT_FOR_JAVASCRIPT);
    }

    /**
     * finds the mode select tabs, to switch between grid input and file upload
     * @param containsText
     * @return
     */
    private Locator.XPathLocator modeSelectListItem(String containsText)
    {
        return Locator.tagWithClass("li", "list-group-item").containing(containsText);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component.ElementCache
    {
        public ElementCache()
        {
            waitForLoaded();
        }

        Locator modeSelectListItem(String containsText)
        {
            return Locator.tagWithClass("li", "list-group-item").containing(containsText);
        }

        Locator deleteRowsBtnLoc = Locator.XPathLocator.union(
                Locator.button("Delete rows"),
                Locator.buttonContainingText("Remove"));

        WebElement bulkInsertBtn()
        {
            return Locator.button("Bulk Insert").findElement(this);
        }
        WebElement bulkUpdateBtn()
        {
            return Locator.button("Bulk update").findElement(this);
        }
        WebElement deleteRowsBtn()
        {
            return deleteRowsBtnLoc.findElement(this);
        }

        WebElement addRowsTxtBox = Locator.tagWithName("input", "addCount").findWhenNeeded(this);
        WebElement addRowsButton = Locator.buttonContainingText("Add").findWhenNeeded(this);

        WebElement addParent = Locator.tagWithClass("span", "container--action-button")
                .containing("Parent").findWhenNeeded(getDriver());

        WebElement updateCheckboxElem = Locator.tag("div")
                .withChild(Locator.tagWithClass("span", "entity-mergeoption-checkbox"))
                .child("input").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        Checkbox updateDataCheckbox = new Checkbox(updateCheckboxElem);
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
