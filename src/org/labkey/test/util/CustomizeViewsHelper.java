/*
 * Copyright (c) 2012-2016 LabKey Corporation
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
import org.labkey.test.components.CustomizeView;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.Map;

public class CustomizeViewsHelper extends CustomizeView
{
    protected final Locator.IdLocator _dataRegionLoc;

    public CustomizeViewsHelper(BaseWebDriverTest test)
    {
        super(test);
        _dataRegionLoc = Locator.id("");
    }

    public CustomizeViewsHelper(DataRegionTable dataRegion)
    {
        super(dataRegion);
        _dataRegionLoc = Locator.id(dataRegion.getComponentElement().getAttribute("id"));
    }

    public void openCustomizeViewPanel()
    {
        if (Locator.button("View Grid").findElements(_driver.getDriver()).size() < 1)
        {
            _driver.doAndWaitForPageSignal(() ->
                    _driver._ext4Helper.clickExt4MenuButton(false, _dataRegionLoc.append(Locator.lkButton("Grid Views")), false, "Customize Grid"),
                    DataRegionTable.PANEL_SHOW_SIGNAL);
        }
        _driver.waitForElement(CUSTOMIZE_VIEW_LOCATOR, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _driver.shortWait().until(LabKeyExpectedConditions.animationIsDone(_dataRegionLoc.toCssLocator().append(CUSTOMIZE_VIEW_LOCATOR)));
    }

    public void closeCustomizeViewPanel()
    {
        _driver.click(_dataRegionLoc.toCssLocator().append(Locator.css(".x-panel-header > .x-tool-close")));
        _driver.shortWait().until(ExpectedConditions.invisibilityOfElementLocated(_dataRegionLoc.toCssLocator().append(Locator.css(".labkey-data-region-header-container .labkey-ribbon")).toBy()));
    }

    public void applyCustomView()
    {
        applyCustomView(_driver.getDefaultWaitForPage());
    }

    public void applyCustomView(int waitMillis)
    {
        _driver.clickButton("View Grid", waitMillis);
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
        _driver.scrollIntoView(_driver.findButton("Save")).
                click();

        _driver._extHelper.waitForExtDialog("Save Custom Grid View");
        
        if (shared)
            _driver.checkCheckbox(Locator.checkboxByName("saveCustomView_shared"));

        if (inherit)
        {
            _driver.checkCheckbox(Locator.checkboxByName("saveCustomView_inherit"));
            // TODO: select folder to save custom view into
        }

        if (name != null)
        {
            if ("".equals(name))
            {
                _driver.log("Saving default custom view");
                _driver.click(Locator.radioButtonByNameAndValue("saveCustomView_namedView", "default"));
            }
            else
            {
                _driver.log("Saving custom view '" + name + "'");
                _driver.click(Locator.radioButtonByNameAndValue("saveCustomView_namedView", "named"));
                _driver.setFormElement(Locator.xpath("//input[@name='saveCustomView_name']"), name);
            }
        }
        else
        {
            _driver.log("Saving current custom view");
        }
        _driver.clickButtonByIndex("Save", 1);
    }

    public void deleteView()
    {
        _driver.clickButton("Delete", 0);
        _driver.clickButton("Yes");
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
        _driver.clickButton("Revert");
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
        if (_driver.isElementPresent(_dataRegionLoc.append("//a[contains(@class, 'x-grouptabs-text') and span[contains(text(), '" + tab.toString() + "')]]")))
            // Tab hasn't rendered yet
            _driver.click(_dataRegionLoc.append(Locator.tag("li").withClass("x-grouptabs-main").containing(tab.toString())));
        else
            // Tab has rendered
            _driver.click(_dataRegionLoc.append("//ul[contains(@class, 'x-grouptabs-strip')]/li[a[contains(@class, 'x-grouptabs-text') and contains(text(), '" + tab.toString() + "')]]"));
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
            _driver.waitForElement(_dataRegionLoc.append("//div[contains(@class, 'x-tree-node') and @fieldkey=" + Locator.xq(nodePath) + "]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            if (_driver.isElementPresent(_dataRegionLoc.append("//div[contains(@class, 'x-tree-node') and @fieldkey=" + Locator.xq(nodePath) + "]/img[1][contains(@class, 'plus')]")))
            {
                _driver.click(_dataRegionLoc.append("//div[contains(@class, 'x-tree-node') and @fieldkey=" + Locator.xq(nodePath) + "]/img[1][contains(@class, 'plus')]"));
            }
            _driver.waitForElement(_dataRegionLoc.append("//div[contains(@class, 'x-tree-node') and @fieldkey=" + Locator.xq(nodePath) + "][img[1][contains(@class, 'minus')]]/following-sibling::ul").notHidden(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT * 2);
            nodePath += "/";
        }

        return _dataRegionLoc.append("//div[contains(@class, 'x-tree-node') and @fieldkey=" + Locator.xq(fieldKey) + "]");
    }

    private void addCustomizeViewItem(String[] fieldKeyParts, String column_name, ViewItemType type)
    {
        // fieldKey is the value contained in @fieldkey
        _driver.log("Adding " + column_name + " " + type.toString());

        changeTab(type);

        // Expand all nodes necessary to reveal the desired node.
        Locator.XPathLocator columnItem = expandPivots(fieldKeyParts);
        Locator checkbox = columnItem.append("/input[@type='checkbox']");
        _driver.waitForElement(checkbox);
        //note: if the column is not in view, it does not seem to get checked
        _driver.scrollIntoView(checkbox);
        _driver.checkCheckbox(checkbox);
    }

    public void addCustomizeViewColumn(String[] fieldKeyParts, String label)
    {
        addCustomizeViewItem(fieldKeyParts, label, ViewItemType.Columns);
    }

    public void addCustomizeViewColumn(String fieldKey, String column_name)
    {
        // fieldKey is the value contained in @fieldkey
        _driver.log("Adding " + column_name + " column");

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
            _driver.log("Adding " + column_name + " filter of " + filter_type);
        else
            _driver.log("Adding " + column_name + " filter of " + filter_type + " " + filter);

        changeTab(ViewItemType.Filter);
        String itemXPath = itemXPath(ViewItemType.Filter, fieldKeyParts);

        if (!_driver.isElementPresent(Locator.xpath(itemXPath)))
        {
            // Add filter if it doesn't exist
            addCustomizeViewItem(fieldKeyParts, column_name, ViewItemType.Filter);
            _driver.assertElementPresent(Locator.xpath(itemXPath));
        }
        else
        {
            // Add new clause
            _driver.click(Locator.xpath(itemXPath + "//a[text() = 'Add']"));
        }

        // XXX: why doesn't 'clauseIndex' work?
        String clauseXPath = itemXPath + "//tr[@clauseindex]";
        int clauseCount = _driver.getElementCount(new Locator.XPathLocator(clauseXPath));

        String newClauseXPath = clauseXPath + "[" + clauseCount + "]";
        _driver.assertElementPresent(Locator.xpath(newClauseXPath));

        _driver._extHelper.selectComboBoxItem(Locator.xpath(newClauseXPath), filter_type);

        if ( !(filter.compareTo("") == 0) )
        {
            _driver.setFormElement(Locator.xpath(newClauseXPath + "//input[contains(@class, 'item-value')]"), filter);
            _driver.fireEvent(Locator.xpath(newClauseXPath + "//input[contains(@class, 'item-value')]"), BaseWebDriverTest.SeleniumEvent.blur);
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

        Actions builder = new Actions(_driver.getDriver());

        List<WebElement> elements = _driver.getDriver().findElements(By.xpath(itemXPath + closeXPath));

        for (WebElement el : elements)
        {
            builder.moveToElement(el).click().build().perform();
            try {el.click();} catch (StaleElementReferenceException ignore) {}
            _driver.shortWait().until(ExpectedConditions.stalenessOf(el));
        }
    }

    //enable customize view grid to show hidden fields
    public void showHiddenItems()
    {
        _driver.click(Locator.tagWithText("Label", "Show Hidden Fields"));
    }

    private void removeCustomizeViewItem(int item_index, ViewItemType type)
    {
        changeTab(type);

        String itemXPath = itemXPath(type, item_index);
        String fieldKey = _driver.getAttribute(Locator.xpath(itemXPath), "fieldkey");

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
        _driver.log("Adding " + column_name + " sort");
        String itemXPath = itemXPath(ViewItemType.Sort, fieldKeyParts);

        _driver.assertElementNotPresent(Locator.xpath(itemXPath));
        addCustomizeViewItem(fieldKeyParts, column_name, ViewItemType.Sort);

        _driver._extHelper.selectComboBoxItem(Locator.xpath(itemXPath), order);
    }

    public void removeCustomizeViewColumn(String fieldKey)
    {
        _driver.log("Removing " + fieldKey + " column");
        removeCustomizeViewItem(fieldKey, ViewItemType.Columns);
    }

    public void removeCustomizeViewFilter(String fieldKey)
    {
        _driver.log("Removing " + fieldKey + " filter");
        removeCustomizeViewItem(fieldKey, ViewItemType.Filter);
    }

    public void removeCustomizeViewFilter(int item_index)
    {
        _driver.log("Removing filter at position " + item_index);
        removeCustomizeViewItem(item_index, ViewItemType.Filter);
    }

    public void removeCustomizeViewSort(String fieldKey)
    {
        _driver.log("Removing " + fieldKey + " sort");
        removeCustomizeViewItem(fieldKey, ViewItemType.Sort);
    }

    public void clearCustomizeViewColumns()
    {
        _driver.log("Clear all Customize View columns.");
        clearAllCustomizeViewItems(ViewItemType.Columns);
    }

    public void clearCustomizeViewFilters()
    {
        _driver.log("Clear all Customize View filters.");
        clearAllCustomizeViewItems(ViewItemType.Filter);
    }

    public void clearCustomizeViewSorts()
    {
        _driver.log("Clear all Customize View sorts.");
        clearAllCustomizeViewItems(ViewItemType.Sort);
    }

    private void clearAllCustomizeViewItems(ViewItemType type)
    {
        changeTab(type);
        String tabXPath = tabContentXPath(type);

        String deleteButtonXPath = tabXPath + "//*[contains(@class, 'labkey-tool-close')]";
        while (_driver.isElementPresent(Locator.xpath(deleteButtonXPath)))
            _driver.click(Locator.xpath(deleteButtonXPath));
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
        _driver.log("Setting folder filter to: " + folderFilter);
        changeTab(ViewItemType.Filter);

        String folderFilterComboXPath = folderFilterComboXPath();
        _driver._extHelper.selectComboBoxItem(Locator.xpath(folderFilterComboXPath), folderFilter);
    }

    public void clipFolderFilter()
    {
        _driver.log("Clip folder filter");
        changeTab(ViewItemType.Filter);

        Locator loc = Locator.xpath(folderFilterPaperclipXPath());
        _driver.assertAttributeNotContains(loc, "class", "x-btn-pressed");
        _driver.click(loc);
        _driver.assertAttributeContains(loc, "class", "x-btn-pressed");
    }

    public void unclipFolderFilter()
    {
        _driver.log("Unclip folder filter");
        changeTab(ViewItemType.Filter);

        Locator loc = Locator.xpath(folderFilterPaperclipXPath());
        _driver.assertAttributeContains(loc, "class", "x-btn-pressed");
        _driver.click(loc);
        _driver.assertAttributeNotContains(loc, "class", "x-btn-pressed");
    }

    public void clipFilter(String fieldkey)
    {
        changeTab(ViewItemType.Filter);
        Locator.CssLocator itemClip = Locators.viewItemClip(ViewItemType.Filter, fieldkey);
        if (!_dataRegionLoc.toCssLocator().append(itemClip).findElement(_driver.getDriver()).getAttribute("class").contains("pressed"))
        {
            _driver.click(itemClip);
        }
    }

    public void unclipFilter(String fieldkey)
    {
        changeTab(ViewItemType.Filter);
        Locator.CssLocator itemClip = Locators.viewItemClip(ViewItemType.Filter, fieldkey);
        if (_dataRegionLoc.toCssLocator().append(itemClip).findElement(_driver.getDriver()).getAttribute("class").contains("pressed"))
        {
            _driver.click(itemClip);
        }
    }

    public void clipSort(String fieldkey)
    {
        changeTab(ViewItemType.Sort);
        Locator.CssLocator itemClip = Locators.viewItemClip(ViewItemType.Sort, fieldkey);
        if (!_dataRegionLoc.toCssLocator().append(itemClip).findElement(_driver.getDriver()).getAttribute("class").contains("pressed"))
        {
            _driver.click(itemClip);
        }
    }

    public void unclipSort(String fieldkey)
    {
        changeTab(ViewItemType.Sort);
        Locator.CssLocator itemClip = Locators.viewItemClip(ViewItemType.Sort, fieldkey);
        if (_dataRegionLoc.toCssLocator().append(itemClip).findElement(_driver.getDriver()).getAttribute("class").contains("pressed"))
        {
            _driver.click(itemClip);
        }
    }

    public void moveCustomizeViewColumn(String fieldKey, boolean moveUp)
    {
        _driver.log("Moving filter, " + fieldKey + " " + (moveUp ? "up." : "down."));
        moveCustomizeViewItem(fieldKey, moveUp, ViewItemType.Columns);
    }

    public void moveCustomizeViewFilter(String fieldKey, boolean moveUp)
    {
        _driver.log("Moving filter, " + fieldKey + " " + (moveUp ? "up." : "down."));
        moveCustomizeViewItem(fieldKey, moveUp, ViewItemType.Filter);
    }

    public void moveCustomizeViewSort(String fieldKey, boolean moveUp)
    {
        _driver.log("Moving sort, " + fieldKey + " " + (moveUp ? "up." : "down."));
        moveCustomizeViewItem(fieldKey, moveUp, ViewItemType.Sort);
    }

    private void moveCustomizeViewItem(String fieldKey, boolean moveUp, ViewItemType type)
    {
        final String itemXPath = itemXPath(type, fieldKey);
        changeTab(type);
        final int itemIndex = _driver.getElementIndex(Locator.xpath(itemXPath));

        moveCustomizeViewItem(itemIndex, moveUp, type);

        _driver.waitFor(() -> itemIndex != _driver.getElementIndex(Locator.xpath(itemXPath)),
                "Item was not reordered.", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    private void moveCustomizeViewItem(int field_index, boolean moveUp, ViewItemType type)
    {
        String fromItemXPath = itemXPath(type, field_index);
        String toItemXPath = itemXPath(type, moveUp ? field_index - 1 : field_index + 1 ) + "/tbody/" + (moveUp ? "tr[1]" : "tr[2]");

        changeTab(type);
        _driver.dragAndDrop(Locator.xpath(fromItemXPath), Locator.xpath(toItemXPath));
    }

    public void removeColumnProperties(String fieldKey)
    {
        setColumnTitle(fieldKey, null);
    }

    /**
     * Sets the column title.
     * @param fieldKey The field key of the column to change.  Note that the column should already be in the selected column list.
     * @param caption The caption value or null to unset the column caption.
     */
    public void setColumnTitle(String fieldKey, String caption)
    {
        String msg = "Setting column " + fieldKey;
        if (caption != null)
            msg = msg + " caption to '" + caption + "'";
        _driver.log(msg);

        changeTab(ViewItemType.Columns);

        String itemXPath = itemXPath(ViewItemType.Columns, fieldKey);
        _driver.click(Locator.xpath(itemXPath + "//div[contains(@class, 'labkey-tool-gear')]"));
        _driver._extHelper.waitForExtDialog("Edit column properties for", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        String parent = "//div[contains(@class, 'x-window')]";

        if (caption == null)
            caption = "";
        _driver.setFormElement(Locator.xpath(parent + "//input[contains(@class, 'x-form-text')]"), caption);

        _driver.clickButton("OK", 0);
    }

    /** Check that a column is present. */
    public boolean isColumnPresent(String fieldKey)
    {
        Locator columnItem = expandPivots(fieldKey.split("/"));
        return _driver.isElementPresent(columnItem);
    }

    /** Check that a column is present and is not selectable. */
    public boolean isColumnUnselectable(String fieldKey)
    {
        Locator columnItem = expandPivots(fieldKey.split("/"));
        return _driver.isElementPresent(columnItem) && "on".equals(_driver.getAttribute(columnItem, "unselectable"));
    }

    /** Check that a column is present and not hidden. Assumes that the 'show hidden columns' is unchecked. */
    public boolean isColumnVisible(String fieldKey)
    {
        Locator.XPathLocator columnItem = expandPivots(fieldKey.split("/"));
        if (!_driver.isElementPresent(columnItem))
            return false;

        // back up the DOM one element to find the <li> node
        WebElement li = columnItem.append("/..").findElement(_driver.getDriver());
        return li.isDisplayed();
    }

    /** Check that a column is present and is a lookup column. */
    public boolean isLookupColumn(String fieldKey)
    {
        Locator.XPathLocator columnItem = expandPivots(fieldKey.split("/"));
        Locator plus = columnItem.child("img[1][contains(@class, 'plus')]");
        Locator minus = columnItem.child("img[1][contains(@class, 'minus')]");
        return _driver.isElementPresent(plus) || _driver.isElementPresent(minus);
    }

    public static class Locators
    {
        public static Locator.CssLocator viewItemClip(ViewItemType itemType, String fieldkey)
        {
            return Locator.css("table.labkey-customview-" + itemType.toString().toLowerCase() + "-item[fieldkey='" + fieldkey + "'] button.labkey-paperclip");
        }
    }
}
