/*
 * Copyright (c) 2012-2015 LabKey Corporation
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
package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CustomizeViewsHelper
{
    private final BaseWebDriverTest _test;
    private final DataRegionTable _dataRegion;
    private final Locator.IdLocator _dataRegionLoc;
    private final RReportHelper _reportHelper;

    public CustomizeViewsHelper(BaseWebDriverTest test)
    {
        _test = test;
        _dataRegion = null;
        _dataRegionLoc = Locator.id("");
        _reportHelper = new RReportHelper(test);
    }

    public CustomizeViewsHelper(DataRegionTable dataRegion)
    {
        _test = dataRegion._test;
        _dataRegion = dataRegion;
        _dataRegionLoc = dataRegion.locator();
        _reportHelper = new RReportHelper(_test);
    }

    public DataRegionTable getDataRegion()
    {
        if (_dataRegion != null)
            return _dataRegion;
        else
            return DataRegionTable.findDataRegion(_test);
    }

    public void openCustomizeViewPanel()
    {
        if (Locator.button("View Grid").findElements(_test.getDriver()).size() < 1)
        {
            _test._ext4Helper.clickExt4MenuButton(false, Locator.lkButton("Views"), false, "Customize View");
            _test.shortWait().until(LabKeyExpectedConditions.dataRegionPanelIsExpanded(getDataRegion()));
        }
        _test.waitForElement(Locator.css(".customizeViewPanel"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.shortWait().until(LabKeyExpectedConditions.animationIsDone(_dataRegionLoc.toCssLocator().append(Locator.css(".customizeViewPanel"))));
    }

    public void closeCustomizeViewPanel()
    {
        _test.click(_dataRegionLoc.toCssLocator().append(Locator.css(".x-panel-header > .x-tool-close")));
        _test.shortWait().until(ExpectedConditions.invisibilityOfElementLocated(_dataRegionLoc.toCssLocator().append(Locator.css(".labkey-data-region-header-container .labkey-ribbon")).toBy()));
    }

    public void applyCustomView()
    {
        applyCustomView(_test.getDefaultWaitForPage());
    }

    public void applyCustomView(int waitMillis)
    {
        _test.clickButton("View Grid", waitMillis);
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
        _test.scrollIntoView(_test.getButtonLocator("Save"));
        _test.clickButton("Save", 0);

        _test._extHelper.waitForExtDialog("Save Custom View");
        
        if (shared)
            _test.checkCheckbox(Locator.checkboxByName("saveCustomView_shared"));

        if (inherit)
        {
            _test.checkCheckbox(Locator.checkboxByName("saveCustomView_inherit"));
            // TODO: select folder to save custom view into
        }

        if (name != null)
        {
            if ("".equals(name))
            {
                _test.log("Saving default custom view");
                _test.click(Locator.radioButtonByNameAndValue("saveCustomView_namedView", "default"));
            }
            else
            {
                _test.log("Saving custom view '" + name + "'");
                _test.click(Locator.radioButtonByNameAndValue("saveCustomView_namedView", "named"));
                _test.setFormElement(Locator.xpath("//input[@name='saveCustomView_name']"), name);
            }
        }
        else
        {
            _test.log("Saving current custom view");
        }
        _test.clickButtonByIndex("Save", 1);
    }

    public void deleteView()
    {
        _test.clickButton("Delete", 0);
        _test.clickButton("Yes");
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
     * add a column to an already open customize view gried
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
            _test.click(_dataRegionLoc.append(Locator.tag("li").withClass("x-grouptabs-main").containing(tab.toString())));
        else
            // Tab has rendered
            _test.click(_dataRegionLoc.append("//ul[contains(@class, 'x-grouptabs-strip')]/li[a[contains(@class, 'x-grouptabs-text') and contains(text(), '" + tab.toString() + "')]]"));
    }

    public static enum ViewItemType
    {
        Columns,
        Filter,
        Sort
    }

    /**
     * expand customize view menu to all but the last of fieldKeyParts
     * @param fieldKeyParts
     * @return A Locator for the &lt;div&gt; item in the "Available Fields" column tree.
     */
    private Locator.XPathLocator expandPivots(String[] fieldKeyParts)
    {
        String nodePath = "";
        String fieldKey = StringUtils.join(fieldKeyParts, "/");

        for (int i = 0; i < fieldKeyParts.length - 1; i ++ )
        {
            nodePath += fieldKeyParts[i];
            _test.waitForElement(_dataRegionLoc.append("//div[contains(@class, 'x-tree-node') and @fieldkey=" + Locator.xq(nodePath) + "]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            if (_test.isElementPresent(_dataRegionLoc.append("//div[contains(@class, 'x-tree-node') and @fieldkey=" + Locator.xq(nodePath) + "]/img[1][contains(@class, 'plus')]")))
            {
                _test.click(_dataRegionLoc.append("//div[contains(@class, 'x-tree-node') and @fieldkey=" + Locator.xq(nodePath) + "]/img[1][contains(@class, 'plus')]"));
            }
            _test.waitForElement(_dataRegionLoc.append("//div[contains(@class, 'x-tree-node') and @fieldkey=" + Locator.xq(nodePath) + "][img[1][contains(@class, 'minus')]]/following-sibling::ul").notHidden(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT * 2);
            nodePath += "/";
        }

        return _dataRegionLoc.append("//div[contains(@class, 'x-tree-node') and @fieldkey=" + Locator.xq(fieldKey) + "]");
    }

    private void addCustomizeViewItem(String[] fieldKeyParts, String column_name, ViewItemType type)
    {
        // fieldKey is the value contained in @fieldkey
        _test.log("Adding " + column_name + " " + type.toString());

        changeTab(type);

        // Expand all nodes necessary to reveal the desired node.
        Locator.XPathLocator columnItem = expandPivots(fieldKeyParts);
        Locator checkbox = columnItem.append("/input[@type='checkbox']");
        _test.waitForElement(checkbox);
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
        int clauseCount = _test.getElementCount(new Locator.XPathLocator(clauseXPath));

        String newClauseXPath = clauseXPath + "[" + clauseCount + "]";
        _test.assertElementPresent(Locator.xpath(newClauseXPath));

        _test._extHelper.selectComboBoxItem(Locator.xpath(newClauseXPath), filter_type);

        if ( !(filter.compareTo("") == 0) )
        {
            _test.setFormElement(Locator.xpath(newClauseXPath + "//input[contains(@class, 'item-value')]"), filter);
            _test.fireEvent(Locator.xpath(newClauseXPath + "//input[contains(@class, 'item-value')]"), BaseWebDriverTest.SeleniumEvent.blur);
        }
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

        _test._extHelper.selectComboBoxItem(Locator.xpath(itemXPath), order);
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

    private String folderFilterComboXPath()
    {
        return tabContentXPath(ViewItemType.Filter) + "//div[contains(@class, 'labkey-folder-filter-combo')]";
    }

    private String folderFilterPaperclipXPath()
    {
        return tabContentXPath(ViewItemType.Filter) + "//table[contains(@class, 'labkey-folder-filter-paperclip')]";
    }

    public void setFolderFilter(String folderFilter)
    {
        _test.log("Setting folder filter to: " + folderFilter);
        changeTab(ViewItemType.Filter);

        String folderFilterComboXPath = folderFilterComboXPath();
        _test._extHelper.selectComboBoxItem(Locator.xpath(folderFilterComboXPath), folderFilter);
    }

    public void togglePaperclipFolderFilter()
    {
        Locator loc = Locator.xpath(folderFilterPaperclipXPath());
        String attr = _test.getAttribute(loc, "class");
        if (attr.contains("x-btn-pressed"))
            unclipFolderFilter();
        else
            clipFolderFilter();
    }

    public void clipFolderFilter()
    {
        _test.log("Clip folder filter");
        changeTab(ViewItemType.Filter);

        Locator loc = Locator.xpath(folderFilterPaperclipXPath());
        _test.assertAttributeNotContains(loc, "class", "x-btn-pressed");
        _test.click(loc);
        _test.assertAttributeContains(loc, "class", "x-btn-pressed");
    }

    public void unclipFolderFilter()
    {
        _test.log("Unclip folder filter");
        changeTab(ViewItemType.Filter);

        Locator loc = Locator.xpath(folderFilterPaperclipXPath());
        _test.assertAttributeContains(loc, "class", "x-btn-pressed");
        _test.click(loc);
        _test.assertAttributeNotContains(loc, "class", "x-btn-pressed");
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

        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            public boolean check()
            {
                return itemIndex != _test.getElementIndex(Locator.xpath(itemXPath));
            }
        }, "Item was not reordered.", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
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

        String itemXPath = itemXPath(ViewItemType.Columns, fieldKey);
        _test.click(Locator.xpath(itemXPath + "//div[contains(@class, 'labkey-tool-gear')]"));
        _test._extHelper.waitForExtDialog("Edit column properties for", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        String parent = "//div[contains(@class, 'x-window')]";

        if (caption == null)
            caption = "";
        _test.setFormElement(Locator.xpath(parent + "//input[contains(@class, 'x-form-text')]"), caption);

        //reset all aggregates
        String deleteButtonXPath = "//div[contains(@class, 'x-window')]" + "//*[contains(@class, 'labkey-tool-close')]";
        while (_test.isElementPresent(Locator.xpath(deleteButtonXPath)))
            _test.click(Locator.xpath(deleteButtonXPath));

        //then re-add them
        int idx = 1;
        Locator grid = Locator.xpath(parent + "//div[contains(@class, 'x-grid-panel')]");

        if(aggregates != null)
        {
            for(Map<String, String> aggregate : aggregates)
            {
                if (aggregate == null || aggregate.get("type") == null)
                    continue;

                _test.clickButton("Add Aggregate", 0);
                Locator row = ExtHelper.locateExt3GridRow(idx, parent);

                Locator comboCell = ExtHelper.locateExt3GridCell(row, 1);
                _test.doubleClick(comboCell);
                _test._extHelper.selectComboBoxItem((Locator.XPathLocator)grid, aggregate.get("type"));

                if(aggregate.get("label") != null){
                    Locator labelCell = ExtHelper.locateExt3GridCell(row, 2);
                    _test.doubleClick(labelCell);

                    Locator fieldPath = ((Locator.XPathLocator) grid).child("/input[contains(@class, 'x-form-text') and not(../img)]");
                    _test.setFormElement(fieldPath, aggregate.get("label"));
                }

                idx++;
            }
        }
        _test.clickButton("OK", 0);
    }

    /**
     * pre-conditions:  at page with grid for which you would like an R view (grid should be only
     *      or at least first element on page)
     * post-conditions:  grid has R view of name name
     * @param name name to give new R view
     */
    public void createRView(String name)
    {
        createRView(name, false);
    }

    /**
     * pre-conditions:  at page with grid for which you would like an R view (grid should be only
     *      or at least first element on page)
     * post-conditions:  grid has R view of name name
     * @param name name to give new R view
     * @param shareView should this view be available to all users
     */
    public void createRView(String name, boolean shareView)
    {
        _test.waitForText(("Views"));
        _test._extHelper.clickMenuButton("Views", "Create", "R View");

        if (shareView)
            _reportHelper.selectOption(RReportHelper.ReportOption.shareReport);

        _reportHelper.saveReport(name);
    }

    /** Check that a column is present. */
    public boolean isColumnPresent(String fieldKey)
    {
        Locator columnItem = expandPivots(fieldKey.split("/"));
        return _test.isElementPresent(columnItem);
    }

    /** Check that a column is present and is not selectable. */
    public boolean isColumnUnselectable(String fieldKey)
    {
        Locator columnItem = expandPivots(fieldKey.split("/"));
        return _test.isElementPresent(columnItem) && "on".equals(_test.getAttribute(columnItem, "unselectable"));
    }

    /** Check that a column is present and not hidden. Assumes that the 'show hidden columns' is unchecked. */
    public boolean isColumnVisible(String fieldKey)
    {
        Locator.XPathLocator columnItem = expandPivots(fieldKey.split("/"));
        if (!_test.isElementPresent(columnItem))
            return false;

        // back up the DOM one element to find the <li> node
        WebElement li = columnItem.append("/..").findElement(_test.getDriver());
        return li.isDisplayed();
    }

    /** Check that a column is present and is a lookup column. */
    public boolean isLookupColumn(String fieldKey)
    {
        Locator.XPathLocator columnItem = expandPivots(fieldKey.split("/"));
        Locator plus = columnItem.child("img[1][contains(@class, 'plus')]");
        Locator minus = columnItem.child("img[1][contains(@class, 'minus')]");
        return _test.isElementPresent(plus) || _test.isElementPresent(minus);
    }

    public static class Locators
    {
        public static Locator.CssLocator viewItemClip(ViewItemType itemType, String fieldkey)
        {
            return Locator.css("table.labkey-customview-" + itemType.toString().toLowerCase() + "-item[fieldkey='" + fieldkey + "'] button.labkey-paperclip");
        }
    }
}
