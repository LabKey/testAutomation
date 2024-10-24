package org.labkey.test.components.ui.entities;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.ui.files.FileUploadPanel;
import org.labkey.test.components.ui.grids.EditableGrid;
import org.labkey.test.components.ui.grids.ResponsiveGrid;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.sleep;

/**
 * This class automates the UI component defined in <a href="https://github.com/LabKey/labkey-ui-components/blob/master/packages/components/src/components/entities/EntityInsertPanel.tsx">components/entities/EntityInsertPanel.tsx</a>
 * This is the same component (collection of atomic elements) used in insertAssay and createSamples.
 */
public class EntityInsertPanel extends WebDriverComponent<EntityInsertPanel.ElementCache>
{
    private final WebDriver _driver;
    private final WebElement _editingDiv;

    private int _readyTimeout = WAIT_FOR_JAVASCRIPT;

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

    public boolean hasTargetEntityTypeSelect()
    {
        return Locator.tagWithName("input", "targetEntityType").existsIn(this);
    }

    public ReactSelect targetEntityTypeSelect()
    {
        // Which tabs are available and selected can vary so try finding the visible react select
        ReactSelect firstTabSelect = ReactSelect.finder(getDriver()).withName("targetEntityType").index(0).waitFor();
        if (firstTabSelect.getComponentElement().isDisplayed())
        {
            return firstTabSelect;
        }
        else
        {
            return ReactSelect.finder(getDriver()).withName("targetEntityType").index(1).waitFor();
        }
    }

    private ReactSelect parentEntityTypeSelect(String label)
    {
        return ReactSelect.finder(getDriver()).followingLabelWithSpan(label).findWhenNeeded(getDriver());
    }

    public EntityInsertPanel addParent(String label, String parentType)
    {
        return addParent(label, parentType, true);
    }

    /*
        Occasionally the 'add parent' functionality of the EntityInsertPanel will show the parent select
        briefly after clicking the 'show parent' button, but then it will disappear.  This occasionally causes
        test failures; until we can address the product-side issue, adding retry to prevent unwanted false-failure
     */
    private EntityInsertPanel addParent(String label, String parentType, boolean retry)
    {
        try
        {
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().addParent));
            elementCache().addParent.click();
            getWrapper().waitForElement(Locator.tag("label").withChild(Locator.tagWithText("span", label)));
            parentEntityTypeSelect(label).select(parentType);
            return this;
        }
        catch (WebDriverException ex)
        {
            if (retry)
            {
                sleep(3_000);    // penalty sleep, make *sure* it's ready to be clicked now
                return addParent(label, parentType, false); // false here prevents looping
            }
            else
            {
                throw ex;
            }
        }
    }

    public ReactSelect getParentSelect(String label)
    {
        if (ReactSelect.finder(getDriver()).followingLabelWithSpan(label).findOptional(this).isEmpty())
            elementCache().addParent.click();
        return parentEntityTypeSelect(label);
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
        showGrid();
        elementCache().grid.addRows(records.size());

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
        int insertRowIndex = getEditableGrid().getEditableRowIndices().get(0);
        return setRecordValues(columnValues, insertRowIndex);
    }

    public EntityInsertPanel setRecordValues(Map<String, Object> columnValues, int row)
    {
        showGrid();
        for(String columnName : columnValues.keySet())
        {
            elementCache().grid.setCellValue(row, columnName, columnValues.get(columnName));
        }

        return this;
    }

    public EditableGrid getEditableGrid()
    {
        showGrid();
        return elementCache().grid;
    }

    public FileUploadPanel getFileUploadPanel()
    {
        var panel = showFileUpload();
        return panel.fileUploadPanel();
    }

    public EntityInsertPanel setMergeData(boolean allowMerge)
    {
        var panel = showFileUpload();
        if (panel.elementCache().allowMergeRadio.isDisplayed())
        {
            if (allowMerge)
                panel.elementCache().allowMergeRadio.set(true);
            else
                panel.elementCache().notAllowMergeRadio.set(true);
        }

        return this;
    }

    public boolean hasMergeOption()
    {
        return elementCache().allowMergeRadio.isDisplayed();
    }

    protected FileUploadPanel fileUploadPanel()
    {
        return elementCache().fileUploadPanel();
    }

    private Optional<FileUploadPanel> optionalFileUploadPanel()
    {
        return new FileUploadPanel.FileUploadPanelFinder(getDriver()).findOptional();
    }

    public List<String> getColumnHeaders()
    {
        return getEditableGrid().getColumnNames();
    }

    public List<Map<String, String>> getGridData()
    {
        return getEditableGrid().getGridData();
    }

    public boolean isGridVisible()
    {
        var optionalGrid = elementCache().optionalGrid();
        return optionalGrid.isPresent() && optionalGrid.get().isDisplayed();
    }

    public EntityInsertPanel setAddRows(int numOfRows)
    {
        showGrid();
        elementCache().grid.setAddRows(numOfRows);
        return this;
    }

    public EntityInsertPanel addRows(int count)
    {
        showGrid();
        elementCache().grid.addRows(count);
        return this;
    }

    public EntityInsertPanel clickRemove()
    {
        showGrid();
        elementCache().grid.clickRemove();
        return this;
    }

    public EntityBulkInsertDialog clickBulkInsert()
    {
        showGrid();
        return elementCache().grid.clickBulkInsert();
    }

    public EntityBulkUpdateDialog clickBulkUpdate()
    {
        showGrid();
        return elementCache().grid.clickBulkUpdate();
    }

    public boolean hasTabs()
    {
        return elementCache().hasTabs();
    }

    public boolean isFileUploadVisible()
    {
        if (!hasTabs())
            return optionalFileUploadPanel().isPresent();

        return modeSelectListItem("from File").withClass("active").findOptionalElement(this).isPresent() &&
                optionalFileUploadPanel().isPresent() &&
                isElementVisible(fileUploadPanel().getComponentElement());
    }

    public boolean hasFileUpload()
    {
        return modeSelectListItem("from File").findOptionalElement(this).isPresent();
    }

    public boolean hasGridCreate()
    {
        return modeSelectListItem("from Grid").findOptionalElement(this).isPresent();
    }

    public String getFormats()
    {
        String[] parts = elementCache().formatString.getText().split(": ");
        return parts.length > 1 ? parts[1] : null;
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
        /* either this is a grid-only insert panel, or there will be a mode-select list-item to
            allow the user to select the grid. Await one or the other to be present   */
        WebDriverWrapper.waitFor(()-> isGridVisible() || hasTabs(),
                "Neither the grid nor its selector appeared within the ready timeout", _readyTimeout);

        if (!isGridVisible())
        {
            modeSelectListItem("from Grid")
                    .waitForElement(this, 2000).click();
            clearElementCache();
            WebDriverWrapper.waitFor(this::isGridVisible,
                    "the grid did bot become visible", 2000);
        }
        elementCache().grid.waitForLoaded();
        return this;
    }
    public ResponsiveGrid uploadFileExpectingPreview(File file)
    {
        var panel = uploadFile(file);
        return new ResponsiveGrid.ResponsiveGridFinder(getDriver()).waitFor(panel);
    }

    public EntityInsertPanel uploadFile(File file)
    {
        var panel = showFileUpload();
        panel.fileUploadPanel().uploadFile(file);
        return panel;
    }

    private WebElement getFileUploadTab()
    {
        return modeSelectListItem("from File")
                .waitForElement(this, 2000);
    }

    public EntityInsertPanel showFileUpload()
    {
        if (!hasTabs())
            return this;

        if (!isFileUploadVisible())
        {
            var toggle = getFileUploadTab();
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(toggle));
            toggle.click();

            // This component may remount so find it again
            var newPanel = new EntityInsertPanel.EntityInsertPanelFinder(getDriver()).findWhenNeeded(getDriver());
            
            newPanel.clearElementCache();
            newPanel.waitForReady();
            WebDriverWrapper.waitFor(newPanel::isFileUploadVisible,
                    "the file upload panel did bot become visible", 2000);

            return newPanel;
        }
        return this;
    }

    public EntityInsertPanel setReadyTimeout(int readyTimeout)
    {
        _readyTimeout = readyTimeout;
        return this;
    }

    @Override
    protected void waitForReady()
    {
        WebDriverWrapper.waitFor(()-> {
            try
            {
                return  isGridVisible() ||          // when uploading assay data there is no target select
                        isFileUploadVisible() ||
                        targetEntityTypeSelect().isInteractive();
            }catch (NoSuchElementException nse)
            {
                return false;
            }
        }, "The insert panel did not become loaded", _readyTimeout);
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

    protected class ElementCache extends Component<?>.ElementCache
    {
        WebElement addParent = Locator.tagWithClass("span", "container--action-button")
                .containing("Parent").findWhenNeeded(getDriver());

        RadioButton allowMergeRadio = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("insertOption", "true")).findWhenNeeded(this);
        RadioButton notAllowMergeRadio = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("insertOption", "false")).findWhenNeeded(this);

        EditableGrid grid = new EditableGrid.EditableGridFinder(_driver).timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded();

        private Optional<EditableGrid> optionalGrid()
        {
            return new EditableGrid.EditableGridFinder(_driver).findOptional(this);
        }

        private Optional<FileUploadPanel> optionalFileUploadPanel()
        {
            return new FileUploadPanel.FileUploadPanelFinder(getDriver()).findOptional();
        }

        protected FileUploadPanel fileUploadPanel()
        {
            return new FileUploadPanel.FileUploadPanelFinder(_driver).timeout(WAIT_FOR_JAVASCRIPT).waitFor(this);
        }

        WebElement formatString = Locator.tagWithClass("div","file-form-formats")
                .refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

        public boolean hasTabs()
        {
            return Locator.tagWithClassContaining("ul", "list-group").existsIn(elementCache());
        }
    }

    public static class EntityInsertPanelFinder extends WebDriverComponent.WebDriverComponentFinder<EntityInsertPanel, EntityInsertPanelFinder>
    {
        private final Locator _locator;

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
