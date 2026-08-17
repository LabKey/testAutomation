/*
 * Copyright (c) 2020-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.components.ui.entities;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.react.MultiMenu;
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
import java.util.Optional;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

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

    public String getTargetEntityType()
    {
        return elementCache().targetTab.getText();
    }

    public boolean isAddParentMenuPresent()
    {
        return getWrapper().isElementPresent(Locator.buttonContainingText("Add Parent"));
    }

    public boolean isAddParentMenuEnabled()
    {
        return Locator.buttonContainingText("Add Parent").findElement(this).isEnabled();
    }

    /**
     * Get 'Add Parent' menu for passive inspection. Use {@link EntityInsertPanel#addParent(String)} to actually add
     * parents to the entity
     */
    public MultiMenu getAddParentMenu()
    {
        return new ReadOnlyMenu(elementCache().addParent, "Parent");
    }

    public EntityInsertPanel addParent(String parentType)
    {
        Assert.assertTrue("Add Parent menu not present", isAddParentMenuPresent());
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().addParent.getComponentElement()));
        getEditableGrid().doAndWaitForColumnUpdate(() -> elementCache().addParent.doMenuAction(parentType));
        return this;
    }

    public boolean isAddSourceMenuPresent()
    {
        return getWrapper().isElementPresent(Locator.buttonContainingText("Add Source"));
    }

    public boolean isAddSourceMenuEnabled()
    {
        return Locator.buttonContainingText("Add Source").findElement(this).isEnabled();
    }

    /**
     * Get 'Add Source' menu for passive inspection. Use {@link EntityInsertPanel#addSource(String)} to actually add
     * sources to the entity
     */
    public MultiMenu getAddSourceMenu()
    {
        return new ReadOnlyMenu(elementCache().addSource, "Source");
    }

    public EntityInsertPanel addSource(String sourceType)
    {
        Assert.assertTrue("Add Source menu not present", isAddSourceMenuPresent());
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().addSource.getComponentElement()));
        getEditableGrid().doAndWaitForColumnUpdate(() -> elementCache().addSource.doMenuAction(sourceType));
        return this;
    }

    /**
     * @param columnIdentifier fieldKey, name, or label
     * @return this component
     */
    public EntityInsertPanel removeColumn(CharSequence columnIdentifier)
    {
        showGrid();
        elementCache().grid.removeColumn(columnIdentifier);
        return this;
    }

    public EntityInsertPanel addRecords(List<Map<String, Object>> records)
    {
        showGrid();
        elementCache().grid.addRows(records.size());

        Assert.assertFalse(String.format("Trying to add more records than there are rows. Number of records to create: %d number of available rows: %d",
                        records.size(),  elementCache().grid.getRowCount()),
                elementCache().grid.getRowCount() < records.size());

        int index = 0;
        for(Map<String, Object> record : records)
        {
            setRecordValues(record, index);
            index++;
        }

        return this;
    }

    public EntityInsertPanel setRecordValues(Map<String, Object> columnValues)
    {
        return setRecordValues(columnValues, 0);
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

    public EntityInsertPanel setRecordValues(List<Map<String, Object>> rowValues)
    {
        for (int i = 0; i < rowValues.size(); i++)
        {
            Map<String, Object> columnValues = rowValues.get(i);
            for(String columnName : columnValues.keySet())
            {
                elementCache().grid.setCellValue(i, columnName, columnValues.get(columnName));
            }
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
        return getEditableGrid().getColumnLabels();
    }

    public List<Map<String, String>> getGridData()
    {
        return getEditableGrid().getGridDataByLabel();
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
        elementCache().grid.clickDelete();
        return this;
    }

    public EntityBulkInsertDialog clickBulkAdd()
    {
        showGrid();
        return elementCache().grid.clickBulkAdd();
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
        elementCache().grid.waitForReady();
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

    public EntityInsertPanel removeFile(String fileName)
    {
        var panel = showFileUpload();
        panel.fileUploadPanel().removeFile(fileName);
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
                        isFileUploadVisible();
            }catch (NoSuchElementException nse)
            {
                return false;
            }
        }, "The insert panel did not become loaded", _readyTimeout);
    }

    /**
     * finds the mode select tabs, to switch between grid input and file upload
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
        Locator navTab = Locator.tagWithClass("ul", "nav-tabs")
                .child(Locator.tag("li").withChild(Locator.tag("a")));
        WebElement targetTab = navTab.findWhenNeeded(this);

        MultiMenu addParent = new MultiMenu.MultiMenuFinder(getDriver()).containsText("Add Parent")
                .timeout(WAIT_FOR_JAVASCRIPT).refindWhenNeeded(this);
        MultiMenu addSource = new MultiMenu.MultiMenuFinder(getDriver()).containsText("Add Source")
                .timeout(WAIT_FOR_JAVASCRIPT).refindWhenNeeded(this);

        RadioButton allowMergeRadio = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("insertOption", "true")).findWhenNeeded(this);
        RadioButton notAllowMergeRadio = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("insertOption", "false")).findWhenNeeded(this);

        EditableGrid grid = new EditableGrid.EditableGridFinder(_driver).timeout(WAIT_FOR_JAVASCRIPT).findWhenNeeded(this);

        private Optional<EditableGrid> optionalGrid()
        {
            return new EditableGrid.EditableGridFinder(_driver).findOptional(this);
        }

        private Optional<FileUploadPanel> optionalFileUploadPanel()
        {
            return new FileUploadPanel.FileUploadPanelFinder(getDriver()).findOptional(this);
        }

        protected FileUploadPanel fileUploadPanel()
        {
            return new FileUploadPanel.FileUploadPanelFinder(_driver).timeout(WAIT_FOR_JAVASCRIPT).waitFor(this);
        }

        WebElement formatString = Locator.tagWithClass("div","file-form-formats")
                .refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

        public boolean hasTabs()
        {
            return Locator.tagWithClassContaining("ul", "list-group").existsIn(this);
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

/**
 * Provides read-only access to parent and source menus
 * Prevents tests from using them to add parents or sources without proper grid synchronization
 */
class ReadOnlyMenu extends MultiMenu
{
    private final String _entityType;

    ReadOnlyMenu(MultiMenu menu, String entityType)
    {
        super(menu.getComponentElement(), menu.getWrapper().getDriver());
        _entityType = entityType;
    }

    @Override
    public void doMenuAction(String toggleText, String menuAction)
    {
        throw new UnsupportedOperationException("Use add%s()".formatted(_entityType));
    }

    @Override
    public void doMenuAction(String menuAction)
    {
        throw new UnsupportedOperationException("Use add%s()".formatted(_entityType));
    }
}