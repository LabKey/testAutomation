/*
 * Copyright (c) 2015-2016 LabKey Corporation
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
package org.labkey.test.components;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.ext4.RadioButton;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.selenium.RefindingWebElement;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.labkey.test.components.ext4.Checkbox.Ext4Checkbox;
import static org.labkey.test.components.ext4.RadioButton.RadioButton;
import static org.labkey.test.components.ext4.Window.Window;

public class CustomizeView extends Component
{
    protected static final Locator.CssLocator CUSTOMIZE_VIEW_LOCATOR = Locator.css(".customize-grid-panel");

    protected final WebDriverWrapper _driver;
    protected final DataRegionTable _dataRegion;
    private WebElement panelEl;
    private Elements _elements;

    public CustomizeView(WebDriverWrapper driver)
    {
        _driver = driver;
        _dataRegion = null;
    }

    public CustomizeView(DataRegionTable dataRegion)
    {
        _driver = dataRegion.getWrapper();
        _dataRegion = dataRegion;
        panelEl = new RefindingWebElement(CUSTOMIZE_VIEW_LOCATOR, _dataRegion.getComponentElement());
    }

    public DataRegionTable getDataRegion()
    {
        if (_dataRegion != null)
            return _dataRegion;
        return DataRegionTable.findDataRegion(_driver); // Not tied to a specific DataRegion
    }

    private Elements elements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    public void openCustomizeViewPanel()
    {
        getDataRegion().openCustomizeGrid();
    }

    public boolean isPanelExpanded()
    {
        try
        {
            return getComponentElement().isDisplayed();
        }
        catch (NoSuchElementException notCreated)
        {
            return false;
        }
    }

    @Deprecated
    public void closeCustomizeViewPanel()
    {
        closePanel();
    }

    public void closePanel()
    {
        if (isPanelExpanded())
        {
            _driver.doAndWaitForPageSignal(() ->
                    Locator.css(".x4-panel-header .x4-tool-after-title").findElement(this).click(),
                    DataRegionTable.PANEL_HIDE_SIGNAL);
        }
    }

    public void applyCustomView()
    {
        applyCustomView(_driver.getDefaultWaitForPage());
    }

    public void applyCustomView(int waitMillis)
    {
        if (waitMillis > 0)
            _driver.clickAndWait(elements().viewGridButton, waitMillis);
        else
            _driver.doAndWaitForPageSignal(() -> clickViewGrid(), DataRegionTable.PANEL_HIDE_SIGNAL);
    }

    public void clickViewGrid()
    {
        elements().viewGridButton.click();
    }

    public SaveWindow clickSave()
    {
        elements().saveButton.click();
        return new SaveWindow(_driver.getDriver());
    }

    public void saveDefaultView()
    {
        saveCustomView("");
    }

    public void saveCustomView()
    {
        saveCustomView(null);
    }

    /**
     * Save a custom view
     * @param name if null, saves the current custom view, otherwise the saves the view with the name (empty string for default.)
     */
    public void saveCustomView(@Nullable String name)
    {
        saveCustomView(name, false, false);
    }

    /**
     * Save a custom view
     * @param name if null, saves the current custom view, otherwise the saves the view with the name (empty string for default.)
     * @param shared if false the report will not be shared, otherwise will mark the view as shared.
     */
    public void saveCustomView(String name, boolean shared)
    {
        saveCustomView(name, shared, false);
    }

    /**
     * Save a custom view
     * @param name if null, saves the current custom view, otherwise the saves the view with the name (empty string for default.)
     * @param shared if false the report will not be shared, otherwise will mark the view as shared.
     */
    public void saveCustomView(String name, boolean shared, boolean inherit)
    {
        SaveWindow saveWindow = clickSave();

        if (shared)
        {
            saveWindow.shareCheckbox.check();
        }

        if (inherit)
        {
            saveWindow.inheritCheckbox.check();
            // TODO: select folder to save custom view into
        }

        if (name != null)
        {
            if ("".equals(name))
            {
                _driver.log("Saving default custom view");
                saveWindow.defaultViewRadio.check();
            }
            else
            {
                _driver.log("Saving custom view '" + name + "'");
                saveWindow.namedViewRadio.check();
                saveWindow.setName(name);
            }
        }
        else
        {
            _driver.log("Saving current custom view");
        }
        saveWindow.save();
    }

    public static class SaveWindow extends Window
    {
        public final Checkbox shareCheckbox = Ext4Checkbox().withLabel("Make this grid view available to all users").findWhenNeeded(this);
        public final Checkbox inheritCheckbox = Ext4Checkbox().withLabel("Make this grid view available in child folders").findWhenNeeded(this);
        public final RadioButton defaultViewRadio = RadioButton().withLabelContaining("Default").findWhenNeeded(this);
        public final RadioButton namedViewRadio = RadioButton().withLabelContaining("Named").findWhenNeeded(this);
        private final WebElement viewNameInput = new LazyWebElement(Locator.xpath("//input[@name='saveCustomView_name']"), this);
        private final WebElement targetContainerInput = new LazyWebElement(Locator.xpath("//input[@name='saveCustomView_targetContainer']"), this);

        protected SaveWindow(WebDriver driver)
        {
            super(Window().withTitleContaining("Save Custom Grid View"), driver);
        }

        public void setName(String name)
        {
            getWrapper().setFormElement(viewNameInput, name);
        }

        public void setTargetContainer(String container)
        {
            getWrapper().setFormElement(targetContainerInput, container);
        }

        public Window saveError()
        {
            clickButton("Save", 0);
            return new Window("Error saving grid view", getWrapper().getDriver());
        }

        public void save()
        {
            clickButton("Save");
        }

        public void cancel()
        {
            clickButton("Cancel", 0);
            waitForClose();
        }
    }

    public void deleteView()
    {
        elements().deleteButton.click();
        Window confirm = new Window(Ext4Helper.Locators.windowWithTitleContaining("Delete").findElement(_driver.getDriver()), _driver.getDriver());
        confirm.clickButton("Yes");
    }

    /**
     * This helper is meant to be used when you have customized a view and chosen "view grid" rather than save,
     * but would now like to save.
     * @param name Name you would like to save the view under.  Null = current (if using default or already saved view)
     */
    public void saveUnsavedViewGridClosed(String name)
    {
//        Actions builder = new Actions(_test._driver);
//        WebElement msg = _test._driver.findElement(By.cssSelector(".labkey-dataregion-msg"));
//        WebElement btn = _test._driver.findElement(By.cssSelector(".unsavedview-save"));
//        builder.moveToElement(msg).moveToElement(btn).click().build().perform();
//
//        _test.waitForText("Save Custom View");
//
//        if(name!=null)
//        {
//              _test.click(Locator.tagContainingText("label", "Named"));
//        }
//
//        _test.clickButton("Save");
        // WORKAROUND: Not working with closed panel

        openCustomizeViewPanel();
        saveCustomView(name);
    }

    public void revertUnsavedViewGridClosed()
    {
//        Locator revertButton = Locator.tagContainingText("span", "Revert");
//
//        _test.mouseOver(Locator.css(".labkey-dataregion-msg"));
//        _test.click(revertButton);
        // WORKAROUND: Not working with closed panel

        openCustomizeViewPanel();
        revertUnsavedView();
    }

    public void revertUnsavedView()
    {
        _driver.clickAndWait(elements().revertButton);
    }

    @Deprecated
    public void addCustomizeViewColumn(String column_name)
    {
        addColumn(column_name);
    }

    @Deprecated
    public void addCustomizeViewColumn(String[] fieldKeyParts)
    {
        addColumn(fieldKeyParts);
    }

    /**
     * add a column to an already open customize view grid
     *
     * @param column_name Name of the column.  If your column is nested, should be of the form
     *          "nodename/nodename/lastnodename", where nodename is not the displayed text of a node
     *          but the name included in the span containing the checkbox.  It will often be the same name,
     *          but with less whitespace
     */
    public void addColumn(String column_name)
    {
        addColumn(column_name, column_name);
    }

    public void addColumn(String[] fieldKeyParts)
    {
        addColumn(fieldKeyParts, StringUtils.join(fieldKeyParts, "/"));
    }

    public void changeTab(ViewItemType tab)
    {
        Locator.tag("ul").child(Locator.tag("li").withClass("labkey-customview-tab").containing(tab.toString())).findElement(this).click();
    }

    @Override
    public WebElement getComponentElement()
    {
        if (panelEl == null) // Not tied to a specific DataRegionTable
            panelEl = new RefindingWebElement(CUSTOMIZE_VIEW_LOCATOR, _driver.getDriver());
        return panelEl;
    }

    public enum ViewItemType
    {
        Columns,
        Filter,
        Sort
    }

    /**
     * expand customize view menu to all but the last of fieldKeyParts
     * @param fieldKeyParts
     * @return The &lt;tr&gt; element for the specified field in the "Available Fields" column tree.
     */
    private WebElement expandPivots(String[] fieldKeyParts)
    {
        String nodePath = "";
        String fieldKey = StringUtils.join(fieldKeyParts, "/").toUpperCase();

        for (int i = 0; i < fieldKeyParts.length - 1; i ++ )
        {
            nodePath += fieldKeyParts[i].toUpperCase();
            WebElement fieldRow = Locator.tag("tr").withClass("x4-grid-data-row").withAttribute("data-recordid", nodePath).waitForElement(getComponentElement(), 10000);

            _driver.scrollIntoView(fieldRow, false);
            if (!fieldRow.getAttribute("class").contains("expanded"))
            {
                Locator.css(".x4-tree-expander").findElement(fieldRow).click();
            }
            Locator.tag("tr").withClass("x4-grid-tree-node-expanded").withAttribute("data-recordid", nodePath).waitForElement(getComponentElement(), 10000);
            WebDriverWrapper.waitFor(() -> Locator.css("tr[data-recordid] + tr:not(.x4-grid-row)").findElements(getComponentElement()).size() == 0, 2000); // Spacer row appears during expansion animation
            nodePath += "/";
        }

        WebElement tr = Locator.tag("tr").withClass("x4-grid-data-row").withAttribute("data-recordid", fieldKey).findElement(getComponentElement());
        return _driver.scrollIntoView(tr, false);
    }

    private void addItem(String[] fieldKeyParts, String column_name, ViewItemType type)
    {
        // fieldKey is the value contained in @fieldkey
        _driver.log("Adding " + column_name + " " + type.toString());

        changeTab(type);

        // Expand all nodes necessary to reveal the desired node.
        WebElement fieldRow = expandPivots(fieldKeyParts);
        WebElement checkbox = Locator.css("input[type=button]").findElement(fieldRow);
        new Checkbox(checkbox).check();
    }

    @Deprecated
    public void addCustomizeViewColumn(String[] fieldKeyParts, String label)
    {
        addColumn(fieldKeyParts, label);
    }

    @Deprecated
    public void addCustomizeViewColumn(String fieldKey, String column_name)
    {
        addColumn(fieldKey, column_name);
    }

    @Deprecated
    public void addCustomizeViewFilter(String fieldKey, String filter_type)
    {
        addFilter(fieldKey, filter_type);
    }

    @Deprecated
    public void addCustomizeViewFilter(String fieldKey, String filter_type, String filter)
    {
        addFilter(fieldKey, filter_type, filter);
    }

    @Deprecated
    public void addCustomizeViewFilter(String fieldKey, String column_name, String filter_type, String filter)
    {
        addFilter(fieldKey, column_name, filter_type, filter);
    }

    @Deprecated
    public void addCustomizeViewFilter(String[] fieldKeyParts, String column_name, String filter_type, String filter)
    {
        addFilter(fieldKeyParts, column_name, filter_type, filter);
    }

    public void addColumn(String[] fieldKeyParts, String label)
    {
        addItem(fieldKeyParts, label, ViewItemType.Columns);
    }

    public void addColumn(String fieldKey, String column_name)
    {
        // fieldKey is the value contained in @fieldkey
        _driver.log("Adding " + column_name + " column");

        addItem(fieldKey.split("/"), column_name, ViewItemType.Columns);
    }

    public void addFilter(String fieldKey, String filter_type)
    {
        addFilter(fieldKey, fieldKey, filter_type, "");
    }

    public void addFilter(String fieldKey, String filter_type, String filter)
    {
        addFilter(fieldKey, fieldKey, filter_type, filter);
    }

    public void addFilter(String fieldKey, String column_name, String filter_type, String filter)
    {
        addFilter(fieldKey.split("/"), column_name, filter_type, filter);
    }

    public void addFilter(String[] fieldKeyParts, String column_name, String filter_type, String filter)
    {
        if (filter.equals(""))
            _driver.log("Adding " + column_name + " filter of " + filter_type);
        else
            _driver.log("Adding " + column_name + " filter of " + filter_type + " " + filter);

        changeTab(ViewItemType.Filter);
        Locator.XPathLocator itemXPath = itemXPath(ViewItemType.Filter, fieldKeyParts);

        if (!_driver.isElementPresent(itemXPath))
        {
            // Add filter if it doesn't exist
            addItem(fieldKeyParts, column_name, ViewItemType.Filter);
            _driver.assertElementPresent(itemXPath);
        }
        else
        {
            // Add new clause
            _driver.click(itemXPath.append("//a[text() = 'Add']"));
        }

        // XXX: why doesn't 'clauseIndex' work?
        Locator.XPathLocator clauseXPath = itemXPath.append("//tr[@clauseindex]");
        int clauseIndex = _driver.getElementCount(clauseXPath) -1;

        Locator.XPathLocator newClauseXPath = itemXPath.append("//tr[@clauseindex='" + clauseIndex + "']");
        _driver.assertElementPresent(newClauseXPath);

        _driver._ext4Helper.selectComboBoxItem(newClauseXPath, filter_type);
        _driver.fireEvent(newClauseXPath, BaseWebDriverTest.SeleniumEvent.blur);

        if ( !(filter.compareTo("") == 0) )
        {
            _driver.setFormElement(newClauseXPath.append("//input[contains(@id, 'filterValue')]"), filter);
            _driver.fireEvent(newClauseXPath.append("//input[contains(@id, 'filterValue')]"), BaseWebDriverTest.SeleniumEvent.blur);
//            itemXPath.append("//tr").findElement(this).click(); // Filter doesn't stick without this
        }
        _driver.click(Locator.xpath("//div[contains(@class, 'x4-panel-header')]"));
    }

    private Locator.XPathLocator tabContentXPath(ViewItemType type)
    {
        return Locator.tagWithClass("div", "test-" + type.toString().toLowerCase() + "-tab");
    }

    private Locator.XPathLocator itemXPath(ViewItemType type, String[] fieldKeyParts)
    {
        return itemXPath(type, StringUtils.join(fieldKeyParts, "/"));
    }

    private Locator.XPathLocator itemXPath(ViewItemType type, String fieldKey)
    {
        FieldKey parsedFieldKey = new FieldKey(fieldKey);
        return itemXPath(type).withPredicate("@fieldkey=" + Locator.xq(fieldKey) + " or @fieldkey=" + Locator.xq(parsedFieldKey.toString()));
    }

    private Locator.XPathLocator itemXPath(ViewItemType type, int item_index)
    {
        return itemXPath(type).index(item_index);
    }

    private Locator.XPathLocator itemXPath(ViewItemType type)
    {
        return Locator.tagWithClass("table", "labkey-customview-" + type.toString().toLowerCase() + "-item");
    }

    private void removeItem(String fieldKey, ViewItemType type)
    {
        changeTab(type);

        Locator closeButtonLoc = itemXPath(type, fieldKey).append(Locator.tagWithClass("*", "labkey-tool-close"));

        Actions builder = new Actions(_driver.getDriver());

        List<WebElement> elements = closeButtonLoc.findElements(this);

        for (WebElement el : elements)
        {
            builder.moveToElement(el).click().build().perform();
            try {el.click();} catch (StaleElementReferenceException ignore) {}
            _driver.shortWait().until(ExpectedConditions.stalenessOf(el));
        }
    }

    public static class FieldKey
    {
        public static final String SEPARATOR = "/";
        private final String fieldName;
        private final String fieldKey;
        private final List<String> lookupParts;

        public FieldKey(String fieldKey)
        {
            List<String> allParts = Arrays.asList(fieldKey.split(SEPARATOR));
            lookupParts = allParts.subList(0, allParts.size() - 1);
            for (int i = 0; i < lookupParts.size(); i++)
            {
                lookupParts.set(i, lookupParts.get(i));//.toUpperCase());
            }
            fieldName = allParts.get(allParts.size() - 1);
            allParts = new ArrayList<>(lookupParts);
            allParts.add(fieldName);
            this.fieldKey = String.join(SEPARATOR, allParts);
        }

        public String getFieldName()
        {
            return fieldName;
        }

        public List<String> getLookupParts()
        {
            return lookupParts;
        }

        public String toString()
        {
            return fieldKey;
        }
    }

    //enable customize view grid to show hidden fields
    public void showHiddenItems()
    {
        _driver.click(Locator.tagWithText("Label", "Show Hidden Fields"));
        BaseWebDriverTest.sleep(250); // wait for columns to display
    }

    private void removeItem(int item_index, ViewItemType type)
    {
        changeTab(type);

        String fieldKey = itemXPath(type, item_index).findElement(this).getAttribute("fieldkey");

        removeItem(fieldKey, type); // Need to remove by key to avoid unintentional removals
    }

    @Deprecated
    public void addCustomizeViewSort(String column_name, String order)
    {
        addSort(column_name, order);
    }

    @Deprecated
    public void addCustomizeViewSort(String fieldKey, String column_name, String order)
    {
        addSort(fieldKey, column_name, order);
    }

    @Deprecated
    public void addCustomizeViewSort(String[] fieldKeyParts, String column_name, String order)
    {
        addSort(fieldKeyParts, column_name, order);
    }

    public void addSort(String column_name, String order)
    {
        addSort(column_name, column_name, order);
    }

    public void addSort(String fieldKey, String column_name, String order)
    {
        addSort(fieldKey.split("/"), column_name, order);
    }

    public void addSort(String[] fieldKeyParts, String column_name, String order)
    {
        _driver.log("Adding " + column_name + " sort");
        Locator.XPathLocator itemXPath = itemXPath(ViewItemType.Sort, fieldKeyParts);

        _driver.assertElementNotPresent(itemXPath);
        addItem(fieldKeyParts, column_name, ViewItemType.Sort);

        _driver._ext4Helper.selectComboBoxItem(itemXPath, order);
        itemXPath.append("//tr").findElement(this).click(); // Sort direction doesn't stick without this
    }

    @Deprecated
    public void removeCustomizeViewColumn(String fieldKey)
    {
        removeColumn(fieldKey);
    }

    @Deprecated
    public void removeCustomizeViewFilter(String fieldKey)
    {
        removeFilter(fieldKey);
    }

    @Deprecated
    public void removeCustomizeViewSort(String fieldKey)
    {
        removeSort(fieldKey);
    }

    public void removeColumn(String fieldKey)
    {
        _driver.log("Removing " + fieldKey + " column");
        removeItem(fieldKey, ViewItemType.Columns);
    }

    public void removeFilter(String fieldKey)
    {
        _driver.log("Removing " + fieldKey + " filter");
        removeItem(fieldKey, ViewItemType.Filter);
    }

    public void removeSort(String fieldKey)
    {
        _driver.log("Removing " + fieldKey + " sort");
        removeItem(fieldKey, ViewItemType.Sort);
    }

    @Deprecated
    public void clearCustomizeViewColumns()
    {
        clearColumns();
    }

    @Deprecated
    public void clearCustomizeViewFilters()
    {
        clearFilters();
    }

    @Deprecated
    public void clearCustomizeViewSorts()
    {
        clearSorts();
    }

    public void clearColumns()
    {
        _driver.log("Clear all Customize View columns.");
        clearItems(ViewItemType.Columns);
    }

    public void clearFilters()
    {
        _driver.log("Clear all Customize View filters.");
        clearItems(ViewItemType.Filter);
    }

    public void clearSorts()
    {
        _driver.log("Clear all Customize View sorts.");
        clearItems(ViewItemType.Sort);
    }

    private void clearItems(ViewItemType type)
    {
        changeTab(type);

        List<WebElement> closeButtons = tabContentXPath(type).append(Locator.tagWithClass("*", "labkey-tool-close")).findElements(this);
        for (WebElement closeButton : closeButtons)
        {
            closeButton.click();
            _driver.shortWait().until(ExpectedConditions.stalenessOf(closeButton));
        }
    }

    @LogMethod(quiet = true)
    public void setFolderFilter(@LoggedParam String folderFilter)
    {
        changeTab(ViewItemType.Filter);

        WebElement folderFilterCombo = Locator.css("div.labkey-folder-filter-combo").findElement(getComponentElement());
        _driver._ext4Helper.selectComboBoxItem(Locator.id(folderFilterCombo.getAttribute("id")), folderFilter);
    }

    private void setFolderFilterClip(boolean clip)
    {
        changeTab(ViewItemType.Filter);
        WebElement clipEl = Locator.css("a.labkey-folder-filter-paperclip").findElement(getComponentElement());
        boolean clipped = clipEl.getAttribute("class").contains("x4-btn-pressed");
        if (clip != clipped)
            clipEl.click();
    }

    @LogMethod(quiet = true)
    public void clipFolderFilter()
    {
        setFolderFilterClip(true);
    }

    @LogMethod(quiet = true)
    public void unclipFolderFilter()
    {
        setFolderFilterClip(false);
    }

    public void clipFilter(String fieldkey)
    {
        changeTab(ViewItemType.Filter);
        SelectedFilterRow filterRow = new SelectedFilterRow(fieldkey);
        filterRow.clip();
    }

    public void unclipFilter(String fieldkey)
    {
        changeTab(ViewItemType.Filter);
        SelectedFilterRow filterRow = new SelectedFilterRow(fieldkey);
        filterRow.unclip();
    }

    public void clipSort(String fieldkey)
    {
        changeTab(ViewItemType.Sort);
        SelectedSortRow sortRow = new SelectedSortRow(fieldkey);
        sortRow.clip();
    }

    public void unclipSort(String fieldkey)
    {
        changeTab(ViewItemType.Sort);
        SelectedSortRow sortRow = new SelectedSortRow(fieldkey);
        sortRow.unclip();
    }

    public void moveColumn(String fieldKey, boolean moveUp)
    {
        _driver.log("Moving filter, " + fieldKey + " " + (moveUp ? "up." : "down."));
        moveItem(fieldKey, moveUp, ViewItemType.Columns);
    }

    public void moveFilter(String fieldKey, boolean moveUp)
    {
        _driver.log("Moving filter, " + fieldKey + " " + (moveUp ? "up." : "down."));
        moveItem(fieldKey, moveUp, ViewItemType.Filter);
    }

    public void moveSort(String fieldKey, boolean moveUp)
    {
        _driver.log("Moving sort, " + fieldKey + " " + (moveUp ? "up." : "down."));
        moveItem(fieldKey, moveUp, ViewItemType.Sort);
    }

    private void moveItem(String fieldKey, boolean moveUp, ViewItemType type)
    {
        changeTab(type);
        final int itemIndex = _driver.getElementIndex(itemXPath(type, fieldKey).findElement(this));

        moveItem(itemIndex, moveUp, type);

        _driver.waitFor(() -> itemIndex != _driver.getElementIndex(itemXPath(type, fieldKey).findElement(this)),
                "Item was not reordered.", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    private void moveItem(int field_index, boolean moveUp, ViewItemType type)
    {
        changeTab(type);
        if (!moveUp) field_index++; // dragAndDrop only moves items up
        WebElement fromItem = itemXPath(type, field_index).findElement(getComponentElement());
        WebElement toItem= itemXPath(type, field_index - 1).findElement(getComponentElement());

        Actions builder = new Actions(_driver.getDriver());
        builder.dragAndDrop(fromItem, toItem).build().perform();
    }

    @Deprecated
    public void moveCustomizeViewColumn(String fieldKey, boolean moveUp)
    {
        moveColumn(fieldKey, moveUp);
    }

    @Deprecated
    public void moveCustomizeViewFilter(String fieldKey, boolean moveUp)
    {
        moveFilter(fieldKey, moveUp);
    }

    @Deprecated
    public void moveCustomizeViewSort(String fieldKey, boolean moveUp)
    {
        moveSort(fieldKey, moveUp);
    }

    public void removeColumnProperties(String fieldKey)
    {
        setColumnProperties(fieldKey, null, new ArrayList<Map<String, String>>());
    }

    public void setColumnProperties(String fieldKey, String caption, Map<String, String> aggregate)
    {
        List<Map<String, String>> aggregates = new ArrayList<>();
        aggregates.add(aggregate);
        setColumnProperties(fieldKey, caption, aggregates);
    }

    /**
     * Sets the column title and aggregate.
     * @param fieldKey The field key of the column to change.  Note that the column should already be in the selected column list.
     * @param caption The caption value or null to unset the column caption.
     * @param aggregates An array of the aggregates to apply to the column or null to unset.
     */
    public void setColumnProperties(String fieldKey, String caption, List<Map<String, String>> aggregates)
    {
        String msg = "Setting column " + fieldKey;
        if (caption != null)
            msg = msg + " caption to '" + caption + "'";
        if (aggregates != null && aggregates.size() > 0)
            msg = msg + " aggregates to '" + StringUtils.join(aggregates, ", ") + "'";
        TestLogger.log(msg);

        changeTab(ViewItemType.Columns);

        Window window = new SelectedColumnRow(fieldKey).clickEdit();

        if (caption == null)
            caption = "";
        _driver.setFormElement(Locator.xpath("//input[contains(@name, 'title')]").findElement(window), caption);

        //reset existing aggregates
        List<WebElement> deleteButtons = Locator.xpath("//*[contains(@src, 'delete')]").findElements(window);
        for (WebElement deleteButton : deleteButtons)
        {
            deleteButton.click();
            _driver.shortWait().until(ExpectedConditions.stalenessOf(deleteButton));
        }

        //then re-add them
        WebElement grid = Locator.css("div.x4-panel").findElement(window);

        if(aggregates != null)
        {
            for(Map<String, String> aggregate : aggregates)
            {
                if (aggregate == null || aggregate.get("type") == null)
                    continue;

                window.clickButton("Add Aggregate", 0);
                Locator.XPathLocator row = Locator.tagWithClass("tr", "x4-grid-data-row").last();

                WebElement comboCell = row.append(Locator.xpath("/td[1]")).findElement(window);
                comboCell.click();
                _driver._ext4Helper.selectComboBoxItem(Locator.id(grid.getAttribute("id")), aggregate.get("type"));

                if(aggregate.get("label") != null){
                    WebElement labelCell = row.append(Locator.xpath("/td[2]/div")).findElement(window);
                    labelCell.click();

                    WebElement fieldPath = Locator.xpath("//input[@name='label']").findElement(window);
                    _driver.setFormElement(fieldPath, aggregate.get("label"));
                }
                _driver.fireEvent(_driver.getDriver().switchTo().activeElement(), WebDriverWrapper.SeleniumEvent.blur);
            }
        }
        Locator.xpath("//label").findElement(window).click();
        window.clickButton("OK", 0);
        window.waitForClose();
    }

    /** Check that a column is present. */
    public boolean isColumnPresent(String fieldKey)
    {
        try
        {
            expandPivots(fieldKey.split("/"));
            return true;
        }
        catch (NoSuchElementException no)
        {
            return false;
        }
    }

    /** Check that a column is present and is not selectable. */
    public boolean isColumnUnselectable(String fieldKey)
    {
        WebElement fieldRow = expandPivots(fieldKey.split("/"));
        return "on".equals(fieldRow.getAttribute("unselectable"));
    }

    /** Check that a column is present and not hidden. Assumes that the 'show hidden columns' is unchecked. */
    public boolean isColumnVisible(String fieldKey)
    {
        try
        {
            WebElement fieldRow = expandPivots(fieldKey.split("/"));
            return fieldRow.isDisplayed();
        }
        catch (NoSuchElementException no)
        {
            return false;
        }
    }

    /** Check that a column is present and is a lookup column. */
    public boolean isLookupColumn(String fieldKey)
    {
        WebElement fieldRow = expandPivots(fieldKey.split("/"));
        return Locator.css("img.x4-tree-expander").findElements(fieldRow).size() > 0;
    }

    private class Elements extends ComponentElements
    {
        @Override
        protected SearchContext getContext()
        {
            return getComponentElement();
        }

        protected final WebElement deleteButton = new RefindingWebElement(Ext4Helper.Locators.ext4Button("Delete"), this);
        protected final WebElement revertButton = new RefindingWebElement(Ext4Helper.Locators.ext4Button("Revert"), this);
        protected final WebElement viewGridButton = new RefindingWebElement(Ext4Helper.Locators.ext4Button("View Grid"), this);
        protected final WebElement saveButton = new RefindingWebElement(Ext4Helper.Locators.ext4Button("Save"), this);
    }

    private class SelectedItemRow extends Component
    {
        private WebElement _element;
        private WebElement _clip = new LazyWebElement(Locator.css("div.item-paperclip > a"), this);
        private String _fieldKey;

        protected SelectedItemRow(ViewItemType itemType, String fieldkey)
        {
            _fieldKey = fieldkey;
            _element = itemXPath(itemType, fieldkey).findElement(CustomizeView.this);
        }

        @Override
        public WebElement getComponentElement()
        {
            return _element;
        }

        protected String getFieldKey()
        {
            return _fieldKey;
        }

        public void clickDelete()
        {
            Locator.css("div.labkey-tool-close").findElement(getComponentElement()).click();
            WebDriverWrapper.waitFor(() -> ExpectedConditions.stalenessOf(getComponentElement()).apply(null), 1000);
        }

        public boolean isClipped()
        {
            return _clip.getAttribute("class").contains("pressed");
        }

        public void clip()
        {
            setClip(true);
        }

        public void unclip()
        {
            setClip(false);
        }

        public void setClip(boolean isClipped)
        {
            if (isClipped() != isClipped)
                _clip.click();
        }
    }

    private class SelectedColumnRow extends SelectedItemRow
    {
        public SelectedColumnRow(String fieldkey)
        {
            super(ViewItemType.Columns, fieldkey);
        }

        public Window clickEdit()
        {
            Locator.css("div.labkey-tool-gear").findElement(getComponentElement()).click();
            return Window().withTitleContaining("Edit column properties for").find(_driver.getDriver());
        }
    }

    private class SelectedFilterRow extends SelectedItemRow
    {
        public SelectedFilterRow(String fieldkey)
        {
            super(ViewItemType.Filter, fieldkey);
        }

        public void setFilter(Filter filter)
        {
            throw new NotImplementedException("Coming soon");
        }
    }

    private class SelectedSortRow extends SelectedItemRow
    {
        public SelectedSortRow(String fieldkey)
        {
            super(ViewItemType.Sort, fieldkey);
        }

        public void setSort(Sort.Direction direction)
        {
            throw new NotImplementedException("Coming soon");
        }
    }
}
