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
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.selenium.RefindingWebElement;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.RReportHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomizeView extends Component
{
    protected static final Locator.CssLocator CUSTOMIZE_VIEW_LOCATOR = Locator.css(".customize-grid-panel");

    protected final BaseWebDriverTest _test;
    protected final DataRegionTable _dataRegion;
    protected final Locator.IdLocator _dataRegionLoc;
    protected final RReportHelper _reportHelper;
    private WebElement panelEl;
    private Elements _elements;

    public CustomizeView(BaseWebDriverTest test)
    {
        _test = test;
        _dataRegion = null;
        _dataRegionLoc = Locator.id("");
        _reportHelper = new RReportHelper(test);
    }

    public CustomizeView(DataRegionTable dataRegion)
    {
        _test = dataRegion._test;
        _dataRegion = dataRegion;
        _dataRegionLoc = dataRegion.locator();
        _reportHelper = new RReportHelper(_test);
        panelEl = new RefindingWebElement(CUSTOMIZE_VIEW_LOCATOR, _dataRegion.getComponentElement());
    }

    public DataRegionTable getDataRegion()
    {
        if (_dataRegion != null)
            return _dataRegion;
        return DataRegionTable.findDataRegion(_test); // Not tied to a specific DataRegion
    }

    private Elements elements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    public void openCustomizeViewPanel()
    {
        if (!isPanelExpanded())
        {
            _test.doAndWaitForPageSignal(() ->
                    getDataRegion().clickHeaderButton("Grid Views", false, "Customize Grid"),
                    DataRegionTable.PANEL_SHOW_SIGNAL);
        }
    }

    private boolean isPanelExpanded()
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

    public void closeCustomizeViewPanel()
    {
        if (isPanelExpanded())
        {
            _test.doAndWaitForPageSignal(() ->
                    _test.click(_dataRegionLoc.toCssLocator().append(Locator.css(".x4-panel-header .x4-tool-after-title"))),
                    DataRegionTable.PANEL_HIDE_SIGNAL);
        }
    }

    public void applyCustomView()
    {
        applyCustomView(_test.getDefaultWaitForPage());
    }

    public void applyCustomView(int waitMillis)
    {
        if (waitMillis > 0)
            _test.clickAndWait(elements().viewGridButton, waitMillis);
        else
            _test.doAndWaitForPageSignal(() -> clickViewGrid(), DataRegionTable.PANEL_HIDE_SIGNAL);
    }

    public void clickViewGrid()
    {
        elements().viewGridButton.click();
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
        _test.scrollIntoView(_test.findButton("Save")).
                click();

        final WebElement saveWindow = _test.waitForElement(Ext4Helper.Locators.windowWithTitleContaining("Save Custom Grid View"));

        if (shared)
            _test.checkCheckbox(Locator.checkboxByName("saveCustomView_shared").findElement(saveWindow));

        if (inherit)
        {
            _test.checkCheckbox(Locator.checkboxByName("saveCustomView_inherit").findElement(saveWindow));
            // TODO: select folder to save custom view into
        }

        if (name != null)
        {
            if ("".equals(name))
            {
                _test.log("Saving default custom view");
                Locator.xpath("//label[contains(@id, 'radiofield') and contains(@class, 'x4-form-cb-label') and contains(text(), 'Default')]")
                        .findElement(saveWindow).click();
            }
            else
            {
                _test.log("Saving custom view '" + name + "'");
                Locator.xpath("//label[text()='Named']/../input").findElement(saveWindow).click();
                _test.setFormElement(Locator.xpath("//input[@name='saveCustomView_name']").findElement(saveWindow), name);
            }
        }
        else
        {
            _test.log("Saving current custom view");
        }
        _test.clickAndWait(Ext4Helper.Locators.ext4Button("Save").findElement(saveWindow));
    }

    public void deleteView()
    {
        Ext4Helper.Locators.ext4Button("Delete").findElement(getComponentElement()).click();
        _test.clickAndWait(Ext4Helper.Locators.windowButton("Delete your view", "Yes"));
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
        _test.clickButton("Revert");
    }

    /**
     * add a column to an already open customize view grid
     *
     * @param column_name Name of the column.  If your column is nested, should be of the form
     *          "nodename/nodename/lastnodename", where nodename is not the displayed text of a node
     *          but the name included in the span containing the checkbox.  It will often be the same name,
     *          but with less whitespace
     */
    public void addCustomizeViewColumn(String column_name)
    {
        addCustomizeViewColumn(column_name, column_name);
    }

    public void addCustomizeViewColumn(String[] fieldKeyParts)
    {
        addCustomizeViewColumn(fieldKeyParts, StringUtils.join(fieldKeyParts, "/"));
    }

    public void changeTab(ViewItemType tab)
    {
        if (_test.isElementPresent(_dataRegionLoc.append("//a[contains(@class, 'x-grouptabs-text') and span[contains(text(), '" + tab.toString() + "')]]")))
            // Tab hasn't rendered yet
            _test.click(_dataRegionLoc.append(Locator.tag("li").withClass("labkey-customview-tab").containing(tab.toString())));
        else
            // Tab has rendered
            _test.click(_dataRegionLoc.append("//ul/li[contains(@class, 'labkey-customview-tab') and contains(text(), '" + tab.toString() + "')]"));
    }

    @Override
    public WebElement getComponentElement()
    {
        if (panelEl == null) // Not tied to a specific DataRegionTable
            panelEl = new RefindingWebElement(CUSTOMIZE_VIEW_LOCATOR, _test.getDriver());
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

            if (!fieldRow.getAttribute("class").contains("expanded"))
            {
                Locator.css("img.x4-tree-elbow-plus").findElement(fieldRow).click();
            }
            Locator.tag("tr").withClass("x4-grid-tree-node-expanded").withAttribute("data-recordid", nodePath).waitForElement(getComponentElement(), 10000);
            nodePath += "/";
        }

        return Locator.tag("tr").withClass("x4-grid-data-row").withAttribute("data-recordid", fieldKey).waitForElement(getComponentElement(), 10000);
    }

    private void addCustomizeViewItem(String[] fieldKeyParts, String column_name, ViewItemType type)
    {
        // fieldKey is the value contained in @fieldkey
        _test.log("Adding " + column_name + " " + type.toString());

        changeTab(type);

        // Expand all nodes necessary to reveal the desired node.
        WebElement fieldRow = expandPivots(fieldKeyParts);
        WebElement checkbox = Locator.css("input[type=button]").findElement(fieldRow);
        //note: if the column is not in view, it does not seem to get checked
        _test.scrollIntoView(checkbox);
        _test.checkCheckbox(checkbox);
    }

    public void addCustomizeViewColumn(String[] fieldKeyParts, String label)
    {
        addCustomizeViewItem(fieldKeyParts, label, ViewItemType.Columns);
    }

    public void addCustomizeViewColumn(String fieldKey, String column_name)
    {
        // fieldKey is the value contained in @fieldkey
        _test.log("Adding " + column_name + " column");

        addCustomizeViewItem(fieldKey.split("/"), column_name, ViewItemType.Columns);
    }

    public void addCustomizeViewFilter(String fieldKey, String filter_type)
    {
        addCustomizeViewFilter(fieldKey, fieldKey, filter_type, "");
    }

    public void addCustomizeViewFilter(String fieldKey, String filter_type, String filter)
    {
        addCustomizeViewFilter(fieldKey, fieldKey, filter_type, filter);
    }

    public void addCustomizeViewFilter(String fieldKey, String column_name, String filter_type, String filter)
    {
        addCustomizeViewFilter(fieldKey.split("/"), column_name, filter_type, filter);
    }

    public void addCustomizeViewFilter(String[] fieldKeyParts, String column_name, String filter_type, String filter)
    {
        if (filter.equals(""))
            _test.log("Adding " + column_name + " filter of " + filter_type);
        else
            _test.log("Adding " + column_name + " filter of " + filter_type + " " + filter);

        changeTab(ViewItemType.Filter);
        String itemXPath = itemXPath(ViewItemType.Filter, fieldKeyParts);

        if (!_test.isElementPresent(Locator.xpath(itemXPath)))
        {
            // Add filter if it doesn't exist
            addCustomizeViewItem(fieldKeyParts, column_name, ViewItemType.Filter);
            _test.assertElementPresent(Locator.xpath(itemXPath));
        }
        else
        {
            // Add new clause
            _test.click(Locator.xpath(itemXPath + "//a[text() = 'Add']"));
        }

        // XXX: why doesn't 'clauseIndex' work?
        String clauseXPath = itemXPath + "//tr[@clauseindex]";
        int clauseIndex = _test.getElementCount(new Locator.XPathLocator(clauseXPath)) -1;

        String newClauseXPath = itemXPath + "//tr[@clauseindex='" + clauseIndex + "']";
        _test.assertElementPresent(Locator.xpath(newClauseXPath));

        _test._ext4Helper.selectComboBoxItem(Locator.xpath(newClauseXPath), filter_type);
        _test.fireEvent(Locator.xpath(newClauseXPath), BaseWebDriverTest.SeleniumEvent.blur);

        if ( !(filter.compareTo("") == 0) )
        {
            _test.setFormElement(Locator.xpath(newClauseXPath + "//input[contains(@id, 'filterValue')]"), filter);
            _test.fireEvent(Locator.xpath(newClauseXPath + "//input[contains(@id, 'filterOpCombo')]"), BaseWebDriverTest.SeleniumEvent.blur);
        }
        _test.click(Locator.xpath("//div[contains(@class, 'x4-panel-header')]"));
    }

    private String tabContentXPath(ViewItemType type)
    {
        return _dataRegionLoc.append("//div[contains(@class, 'test-" + type.toString().toLowerCase() + "-tab')]").toXpath();
    }

    private String itemXPath(ViewItemType type, String[] fieldKeyParts)
    {
        return itemXPath(type, StringUtils.join(fieldKeyParts, "/"));
    }

    private String itemXPath(ViewItemType type, String fieldKey)
    {
        return _dataRegionLoc.append("//table[contains(@class, 'labkey-customview-" + type.toString().toLowerCase() + "-item') and @fieldkey=" + Locator.xq(fieldKey) + "]").toXpath();
    }

    private String itemXPath(ViewItemType type, int item_index)
    {
        return _dataRegionLoc.append("//table[contains(@class, 'labkey-customview-" + type.toString().toLowerCase() + "-item')][" + (item_index + 1) + "]").toXpath();
    }

    private void removeCustomizeViewItem(String fieldKey, ViewItemType type)
    {
        changeTab(type);

        String itemXPath = itemXPath(type, fieldKey);
        String closeXPath = "//*[contains(@class, 'labkey-tool-close')]";

        Actions builder = new Actions(_test.getDriver());

        List<WebElement> elements = _test.getDriver().findElements(By.xpath(itemXPath + closeXPath));

        for (WebElement el : elements)
        {
            builder.moveToElement(el).click().build().perform();
            try {el.click();} catch (StaleElementReferenceException ignore) {}
            _test.shortWait().until(ExpectedConditions.stalenessOf(el));
        }
    }

    //enable customize view grid to show hidden fields
    public void showHiddenItems()
    {
        _test.click(Locator.tagWithText("Label", "Show Hidden Fields"));
        _test.sleep(250); // wait for columns to display
    }

    private void removeCustomizeViewItem(int item_index, ViewItemType type)
    {
        changeTab(type);

        String itemXPath = itemXPath(type, item_index);
        String fieldKey = _test.getAttribute(Locator.xpath(itemXPath), "fieldkey");

        removeCustomizeViewItem(fieldKey, type); // Need to remove by key to avoid unintentional removals
    }

    public void addCustomizeViewSort(String column_name, String order)
    {
        addCustomizeViewSort(column_name, column_name, order);
    }

    public void addCustomizeViewSort(String fieldKey, String column_name, String order)
    {
        addCustomizeViewSort(fieldKey.split("/"), column_name, order);
    }

    public void addCustomizeViewSort(String[] fieldKeyParts, String column_name, String order)
    {
        _test.log("Adding " + column_name + " sort");
        String itemXPath = itemXPath(ViewItemType.Sort, fieldKeyParts);

        _test.assertElementNotPresent(Locator.xpath(itemXPath));
        addCustomizeViewItem(fieldKeyParts, column_name, ViewItemType.Sort);

        _test._ext4Helper.selectComboBoxItem(Locator.xpath(itemXPath), order);
    }

    public void removeCustomizeViewColumn(String fieldKey)
    {
        _test.log("Removing " + fieldKey + " column");
        removeCustomizeViewItem(fieldKey, ViewItemType.Columns);
    }

    public void removeCustomizeViewFilter(String fieldKey)
    {
        _test.log("Removing " + fieldKey + " filter");
        removeCustomizeViewItem(fieldKey, ViewItemType.Filter);
    }

    public void removeCustomizeViewFilter(int item_index)
    {
        _test.log("Removing filter at position " + item_index);
        removeCustomizeViewItem(item_index, ViewItemType.Filter);
    }

    public void removeCustomizeViewSort(String fieldKey)
    {
        _test.log("Removing " + fieldKey + " sort");
        removeCustomizeViewItem(fieldKey, ViewItemType.Sort);
    }

    public void clearCustomizeViewColumns()
    {
        _test.log("Clear all Customize View columns.");
        clearAllCustomizeViewItems(ViewItemType.Columns);
    }

    public void clearCustomizeViewFilters()
    {
        _test.log("Clear all Customize View filters.");
        clearAllCustomizeViewItems(ViewItemType.Filter);
    }

    public void clearCustomizeViewSorts()
    {
        _test.log("Clear all Customize View sorts.");
        clearAllCustomizeViewItems(ViewItemType.Sort);
    }

    private void clearAllCustomizeViewItems(ViewItemType type)
    {
        changeTab(type);
        String tabXPath = tabContentXPath(type);

        String deleteButtonXPath = tabXPath + "//*[contains(@class, 'labkey-tool-close')]";
        while (_test.isElementPresent(Locator.xpath(deleteButtonXPath)))
            _test.click(Locator.xpath(deleteButtonXPath));
    }

    @LogMethod(quiet = true)
    public void setFolderFilter(@LoggedParam String folderFilter)
    {
        changeTab(ViewItemType.Filter);

        WebElement folderFilterCombo = Locator.css("div.labkey-folder-filter-combo").findElement(getComponentElement());
        _test._ext4Helper.selectComboBoxItem(Locator.id(folderFilterCombo.getAttribute("id")), folderFilter);
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
        Locator.CssLocator itemClip = Locators.viewItemClip(ViewItemType.Filter, fieldkey);
        if (!_dataRegionLoc.toCssLocator().append(itemClip).findElement(_test.getDriver()).getAttribute("class").contains("pressed"))
        {
            _test.click(itemClip);
        }
    }

    public void unclipFilter(String fieldkey)
    {
        changeTab(ViewItemType.Filter);
        Locator.CssLocator itemClip = Locators.viewItemClip(ViewItemType.Filter, fieldkey);
        if (_dataRegionLoc.toCssLocator().append(itemClip).findElement(_test.getDriver()).getAttribute("class").contains("pressed"))
        {
            _test.click(itemClip);
        }
    }

    public void clipSort(String fieldkey)
    {
        changeTab(ViewItemType.Sort);
        Locator.CssLocator itemClip = Locators.viewItemClip(ViewItemType.Sort, fieldkey);
        if (!_dataRegionLoc.toCssLocator().append(itemClip).findElement(_test.getDriver()).getAttribute("class").contains("pressed"))
        {
            _test.click(itemClip);
        }
    }

    public void unclipSort(String fieldkey)
    {
        changeTab(ViewItemType.Sort);
        Locator.CssLocator itemClip = Locators.viewItemClip(ViewItemType.Sort, fieldkey);
        if (_dataRegionLoc.toCssLocator().append(itemClip).findElement(_test.getDriver()).getAttribute("class").contains("pressed"))
        {
            _test.click(itemClip);
        }
    }

    public void moveCustomizeViewColumn(String fieldKey, boolean moveUp)
    {
        _test.log("Moving filter, " + fieldKey + " " + (moveUp ? "up." : "down."));
        moveCustomizeViewItem(fieldKey, moveUp, ViewItemType.Columns);
    }

    public void moveCustomizeViewFilter(String fieldKey, boolean moveUp)
    {
        _test.log("Moving filter, " + fieldKey + " " + (moveUp ? "up." : "down."));
        moveCustomizeViewItem(fieldKey, moveUp, ViewItemType.Filter);
    }

    public void moveCustomizeViewSort(String fieldKey, boolean moveUp)
    {
        _test.log("Moving sort, " + fieldKey + " " + (moveUp ? "up." : "down."));
        moveCustomizeViewItem(fieldKey, moveUp, ViewItemType.Sort);
    }

    private void moveCustomizeViewItem(String fieldKey, boolean moveUp, ViewItemType type)
    {
        final String itemXPath = itemXPath(type, fieldKey);
        changeTab(type);
        final int itemIndex = _test.getElementIndex(Locator.xpath(itemXPath));

        moveCustomizeViewItem(itemIndex, moveUp, type);

        _test.waitFor(() -> itemIndex != _test.getElementIndex(Locator.xpath(itemXPath)),
                "Item was not reordered.", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    private void moveCustomizeViewItem(int field_index, boolean moveUp, ViewItemType type)
    {
        String fromItemXPath = itemXPath(type, field_index);
        String toItemXPath = itemXPath(type, moveUp ? field_index - 1 : field_index + 1 ) + "/tbody/" + (moveUp ? "tr[1]" : "tr[2]");

        changeTab(type);
        _test.dragAndDrop(Locator.xpath(fromItemXPath), Locator.xpath(toItemXPath));
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
        _test.log(msg);

        changeTab(ViewItemType.Columns);

        WebElement window = new SelectedColumnRow(fieldKey).clickEdit();

        if (caption == null)
            caption = "";
        _test.setFormElement(Locator.xpath("//input[contains(@name, 'title')]").findElement(window), caption);

        //reset existing aggregates
        List<WebElement> deleteButtons = Locator.xpath("//div[contains(@class, 'x4-window')]//*[contains(@src, 'delete')]").findElements(window);
        for (WebElement deleteButton : deleteButtons)
        {
            deleteButton.click();
            WebDriverWrapper.waitFor(() -> ExpectedConditions.stalenessOf(deleteButton).apply(null), 1000);
        }

        //then re-add them
        int idx = 1;
        WebElement grid = Locator.xpath("//div[contains(@class, 'x4-panel')]").findElement(window);

        if(aggregates != null)
        {
            for(Map<String, String> aggregate : aggregates)
            {
                if (aggregate == null || aggregate.get("type") == null)
                    continue;

                _test.clickButton("Add Aggregate", 0);
                Locator.XPathLocator row = Locator.xpath("//div[contains(@class, 'x4-window')]//tr[contains(@class,' x4-grid-data-row')][" + idx + "]");

                Locator comboCell = row.append(Locator.xpath("/td[1]"));
                _test.doubleClick(comboCell);
                _test._ext4Helper.selectComboBoxItem(Locator.id(grid.getAttribute("id")), aggregate.get("type"));

                if(aggregate.get("label") != null){
                    Locator labelCell = row.append(Locator.xpath("/td[2]/div"));
                    _test.doubleClick(labelCell);

                    Locator fieldPath = Locator.xpath("//input[@name='label']");//(grid).child("/input[contains(@class, 'x4-form-text') and not(../img)]");
                    _test.setFormElement(fieldPath, aggregate.get("label"));
                }
                idx++;
            }
        }
        Locator.xpath("//label").findElement(window).click();
        _test.clickButton("OK", 0);
        WebDriverWrapper.waitFor(() -> !window.isDisplayed(), 10000);
    }

    /**
     * pre-conditions:  at page with grid for which you would like an R view (grid should be only
     *      or at least first element on page)
     * post-conditions:  grid has R view of name name
     * @param name name to give new R view
     */
    public void createRReport(String name)
    {
        createRReport(name, false);
    }

    /**
     * pre-conditions:  at page with grid for which you would like an R view (grid should be only
     *      or at least first element on page)
     * post-conditions:  grid has R view of name name
     * @param name name to give new R view
     * @param shareView should this view be available to all users
     */
    public void createRReport(String name, boolean shareView)
    {
        _test.waitForText(("Reports"));
        _test._extHelper.clickMenuButton("Reports", "Create R Report");

        if (shareView)
            _reportHelper.selectOption(RReportHelper.ReportOption.shareReport);

        _reportHelper.saveReport(name);
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

        protected final WebElement viewGridButton = new RefindingWebElement(Locator.button("View Grid"), this);
    }

    private class SelectedItemRow extends Component
    {
        private WebElement _element;
        private WebElement _clip;
        private String _fieldKey;

        protected SelectedItemRow(ViewItemType itemType, String fieldkey)
        {
            _fieldKey = fieldkey;
            _element = Locator.css("table.labkey-customview-" + itemType.toString().toLowerCase() + "-item[fieldkey=" + Locator.xq(fieldkey) + "]")
                    .findElement(CustomizeView.this.getComponentElement());
            _clip = new LazyWebElement(Locator.css("div.item-paperclip > a"), this);
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
            if (!isClipped())
                _clip.click();
        }

        public void unclip()
        {
            if (isClipped())
                _clip.click();
        }
    }

    private class SelectedColumnRow extends SelectedItemRow
    {
        public SelectedColumnRow(String fieldkey)
        {
            super(ViewItemType.Columns, fieldkey);
        }

        public WebElement clickEdit()
        {
            Locator.css("div.labkey-tool-gear").findElement(getComponentElement()).click();
            return Ext4Helper.Locators.windowWithTitleContaining("Edit column properties for").notHidden().waitForElement(_test.getDriver(), 10000);
        }
    }

    /**
     * @deprecated Not actually deprecated. Just under construction
     */
    @Deprecated
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

    private static class Locators
    {
        private static Locator.CssLocator selectedViewItem(ViewItemType itemType, String fieldkey)
        {
            return Locator.css("table.labkey-customview-" + itemType.toString().toLowerCase() + "-item[fieldkey=" + Locator.xq(fieldkey) + "]");
        }

        private static Locator.CssLocator viewItemClip(ViewItemType itemType, String fieldkey)
        {
            return selectedViewItem(itemType, fieldkey).append(" div.item-paperclip > a");
        }
    }
}
