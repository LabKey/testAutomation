/*
 * Copyright (c) 2010-2012 LabKey Corporation
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
import org.junit.Assert;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.openqa.selenium.internal.seleniumemulation.IsElementPresent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: Trey Chadick
 * Date: Oct 6, 2010
 * Time: 2:32:05 PM
 */

public class CustomizeViewsHelper
{
    public static void openCustomizeViewPanel(BaseSeleniumWebTest test)
    {
        ExtHelper.clickExtMenuButton(test, false, Locator.navButton("Views"), "Customize View");
        test.waitForElement(Locator.xpath("//button[text()='View Grid']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public static void closeCustomizeViewPanel(BaseSeleniumWebTest test)
    {
        // UNDONE: click 'X' close button on columns/filter/sort tab panel instead.
        test.clickMenuButtonAndContinue("Views", "Customize View");
    }

    public static void applyCustomView(BaseSeleniumWebTest test)
    {
        applyCustomView(test, test.getDefaultWaitForPage());
    }

    public static void applyCustomView(BaseSeleniumWebTest test, int waitMillis)
    {
        test.clickNavButtonAt("View Grid", waitMillis, "1,1");
    }

    public static void saveDefaultView(BaseSeleniumWebTest test)
    {
        saveCustomView(test, "");
    }

    public static void saveCustomView(BaseSeleniumWebTest test)
    {
        saveCustomView(test, null);
    }

    /**
     * Save a custom view
     * @param test test
     * @param name if null, saves the current custom view, otherwise the saves the view with the name (empty string for default.)
     */
    public static void saveCustomView(BaseSeleniumWebTest test, String name)
    {
        test.clickNavButton("Save", 0);
        if (name != null)
        {
            if ("".equals(name))
            {
                test.log("Saving default custom view");
                test.click(Locator.radioButtonByNameAndValue("saveCustomView_namedView", "default"));
            }
            else
            {
                test.log("Saving custom view '" + name + "'");
                test.click(Locator.radioButtonByNameAndValue("saveCustomView_namedView", "named"));
                test.setFormElement(Locator.xpath("//input[@name='saveCustomView_name']"), name);
            }
        }
        else
        {
            test.log("Saving current custom view");
        }
        test.clickButtonByIndex("Save", 1);
    }

    public static void deleteView(BaseSeleniumWebTest test)
    {
        test.clickNavButton("Delete", 0);
        test.clickNavButton("Yes");
    }

    public static void revertUnsavedView(BaseSeleniumWebTest test)
    {
        test.clickNavButton("Revert");
    }

    /**
     * add a column to an already open customize view gried
     *
     * @param test The BaseSeleniumWebTest that will be used to make Selenium calls.  Almost always "this"
     * @param column_name Name of the column.  If your column is nested, should be of the form
     *          "nodename/nodename/lastnodename", where nodename is not the displayed text of a node
     *          but the name included in the span containing the checkbox.  It will often be the same name,
     *          but with less whitespace
     */
    public static void addCustomizeViewColumn(BaseSeleniumWebTest test, String column_name)
    {
        addCustomizeViewColumn(test, column_name, column_name);
    }

    public static void addCustomizeViewColumn(BaseSeleniumWebTest test, String[] fieldKeyParts)
    {
        addCustomizeViewItem(test, fieldKeyParts, StringUtils.join(fieldKeyParts, "/"), ViewItemType.Columns);
    }

    public static void changeTab(BaseSeleniumWebTest test, ViewItemType tab)
    {
        if (test.isElementPresent(Locator.xpath("//a[contains(@class, 'x-grouptabs-text') and span[contains(text(), '" + tab.toString() + "')]]")))
            // Tab hasn't rendered yet
            test.mouseDown(Locator.xpath("//a[contains(@class, 'x-grouptabs-text') and span[contains(text(), '" + tab.toString() + "')]]"));
        else
            // Tab has rendered
            test.mouseDown(Locator.xpath("//ul[contains(@class, 'x-grouptabs-strip')]/li[a[contains(@class, 'x-grouptabs-text') and contains(text(), '" + tab.toString() + "')]]"));
    }

    private static enum ViewItemType
    {
        Columns,
        Filter,
        Sort
    }

    /**
     * expand customize view menu to all but the last of fieldKeyParts
     * @param test
     * @param fieldKeyParts
     * @return
     */
    private static Locator expandPivots(BaseSeleniumWebTest test, String[] fieldKeyParts)
    {
        String nodePath = "";
        String fieldKey = StringUtils.join(fieldKeyParts, "/");

        for( int i = 0; i < fieldKeyParts.length - 1; i ++ )
        {
            nodePath += fieldKeyParts[i];
            test.waitForElement(Locator.xpath("//div[contains(@class, 'x-tree-node') and @fieldKey=" + Locator.xq(nodePath) + "]"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
            if( test.isElementPresent(Locator.xpath("//div[contains(@class, 'x-tree-node') and @fieldKey=" + Locator.xq(nodePath) + "]/img[1][contains(@class, 'plus')]")))
                test.click(Locator.xpath("//div[contains(@class, 'x-tree-node') and @fieldKey=" + Locator.xq(nodePath) + "]/img[1][contains(@class, 'plus')]"));
            test.waitForElement(Locator.xpath("//div[contains(@class, 'x-tree-node') and @fieldKey=" + Locator.xq(nodePath) + "]/img[1][contains(@class, 'minus')]"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
            nodePath += "/";
        }

        return Locator.xpath("//div[contains(@class, 'x-tree-node') and @fieldKey=" + Locator.xq(fieldKey) + "]/input[@type='checkbox']");
    }

    private static void addCustomizeViewItem(BaseSeleniumWebTest test, String[] fieldKeyParts, String column_name, ViewItemType type)
    {
        // fieldKey is the value contained in @fieldKey
        test.log("Adding " + column_name + " " + type.toString());

        changeTab(test, type);

//        String fieldKey = StringUtils.join(fieldKeyParts, "/");
//        String nodePath = "";

        // Expand all nodes necessary to reveal the desired node.
        Locator checkbox = expandPivots(test, fieldKeyParts);

        test.checkCheckbox(checkbox);
    }

    public static void addCustomizeViewColumn(BaseSeleniumWebTest test, String fieldKey, String column_name)
    {
        // fieldKey is the value contained in @fieldKey
        test.log("Adding " + column_name + " column");

        addCustomizeViewItem(test, fieldKey.split("/"), column_name, ViewItemType.Columns);
    }

    public static void addCustomizeViewFilter(BaseSeleniumWebTest test, String fieldKey, String filter_type)
    {
        addCustomizeViewFilter(test, fieldKey, fieldKey, filter_type, "");
    }

    public static void addCustomizeViewFilter(BaseSeleniumWebTest test, String fieldKey, String filter_type, String filter)
    {
        addCustomizeViewFilter(test, fieldKey, fieldKey, filter_type, filter);
    }

    public static void addCustomizeViewFilter(BaseSeleniumWebTest test, String fieldKey, String column_name, String filter_type, String filter)
    {
        addCustomizeViewFilter(test, fieldKey.split("/"), column_name, filter_type, filter);
    }

    public static void addCustomizeViewFilter(BaseSeleniumWebTest test, String[] fieldKeyParts, String column_name, String filter_type, String filter)
    {
        if (filter.equals(""))
            test.log("Adding " + column_name + " filter of " + filter_type);
        else
            test.log("Adding " + column_name + " filter of " + filter_type + " " + filter);

        changeTab(test, ViewItemType.Filter);
        String itemXPath = itemXPath(ViewItemType.Filter, fieldKeyParts);

        if (!test.isElementPresent(Locator.xpath(itemXPath)))
        {
            // Add filter if it doesn't exist
            addCustomizeViewItem(test, fieldKeyParts, column_name, ViewItemType.Filter);
            test.assertElementPresent(Locator.xpath(itemXPath));
        }
        else
        {
            // Add new clause
            test.click(Locator.xpath(itemXPath + "//a[text() = 'Add']"));
        }

        // XXX: why doesn't 'clauseIndex' work?
        String clauseXPath = itemXPath + "//tr[@clauseindex]";
        int clauseCount = test.getXpathCount(new Locator.XPathLocator(clauseXPath));

        String newClauseXPath = clauseXPath + "[" + clauseCount + "]";
        test.assertElementPresent(Locator.xpath(newClauseXPath));

        ExtHelper.selectComboBoxItem(test, Locator.xpath(newClauseXPath), filter_type);

        if ( !(filter.compareTo("") == 0) )
        {
            test.setFormElement(Locator.xpath(newClauseXPath + "//input[contains(@class, 'item-value')]"), filter);
            test.fireEvent(Locator.xpath(newClauseXPath + "//input[contains(@class, 'item-value')]"), BaseSeleniumWebTest.SeleniumEvent.blur);
        }
    }

    private static String tabContentXPath(ViewItemType type)
    {
        return "//div[contains(@class, 'test-" + type.toString().toLowerCase() + "-tab')]";
    }

    private static String itemXPath(ViewItemType type, String[] fieldKeyParts)
    {
        return itemXPath(type, StringUtils.join(fieldKeyParts, "/"));
    }

    private static String itemXPath(ViewItemType type, String fieldKey)
    {
        return "//table[contains(@class, 'labkey-customview-" + type.toString().toLowerCase() + "-item') and @fieldkey=" + Locator.xq(fieldKey) +"]";
    }

    private static String itemXPath(ViewItemType type, int item_index)
    {
        return "//table[contains(@class, 'labkey-customview-" + type.toString().toLowerCase() + "-item')][" + (item_index + 1) + "]";
    }

    private static void removeCustomizeViewItem(BaseSeleniumWebTest test, String fieldKey, ViewItemType type)
    {
        changeTab(test, type);

        String itemXPath = itemXPath(type, fieldKey);

        Locator.XPathLocator item = Locator.xpath(itemXPath);
        Locator.XPathLocator close = Locator.xpath(itemXPath + "//*[contains(@class, 'labkey-tool-close')]");

        int clauseCount = test.getXpathCount(close);

        do{
            test.click(close);
            test.assertElementPresent(close, --clauseCount); // Make sure clause is deleted. Prevent infinite loop on failure.
        }while (test.isElementPresent(item)); // Removes all clauses for a single fieldkey
    }

    //enable customize view grid to show hidden fields
    public static void showHiddenItems(BaseSeleniumWebTest test)
    {
        test.click(Locator.tagWithText("Label","Show Hidden Fields"));
    }

    private static void removeCustomizeViewItem(BaseSeleniumWebTest test, int item_index, ViewItemType type)
    {
        changeTab(test, type);

        String itemXPath = itemXPath(type, item_index);
        String fieldKey = test.getAttribute(Locator.xpath(itemXPath), "fieldkey");

        removeCustomizeViewItem(test, fieldKey, type); // Need to remove by key to avoid unintentional removals
    }

    public static void addCustomizeViewSort(BaseSeleniumWebTest test, String column_name, String order)
    {
        addCustomizeViewSort(test, column_name, column_name, order);
    }

    public static void addCustomizeViewSort(BaseSeleniumWebTest test, String fieldKey, String column_name, String order)
    {
        addCustomizeViewSort(test, fieldKey.split("/"), column_name, order);
    }

    public static void addCustomizeViewSort(BaseSeleniumWebTest test, String[] fieldKeyParts, String column_name, String order)
    {
        test.log("Adding " + column_name + " sort");
        String itemXPath = itemXPath(ViewItemType.Sort, fieldKeyParts);

        test.assertElementNotPresent(Locator.xpath(itemXPath));
        addCustomizeViewItem(test, fieldKeyParts, column_name, ViewItemType.Sort);

        ExtHelper.selectComboBoxItem(test, Locator.xpath(itemXPath), order);
    }

    public static void removeCustomizeViewColumn(BaseSeleniumWebTest test, String fieldKey)
    {
        test.log("Removing " + fieldKey + " column");
        removeCustomizeViewItem(test, fieldKey, ViewItemType.Columns);
    }

    public static void removeCustomizeViewFilter(BaseSeleniumWebTest test, String fieldKey)
    {
        test.log("Removing " + fieldKey + " filter");
        removeCustomizeViewItem(test, fieldKey, ViewItemType.Filter);
    }

    public static void removeCustomizeViewFilter(BaseSeleniumWebTest test, int item_index)
    {
        test.log("Removing filter at position " + item_index);
        removeCustomizeViewItem(test, item_index, ViewItemType.Filter);
    }

    public static void removeCustomizeViewSort(BaseSeleniumWebTest test, String fieldKey)
    {
        test.log("Removing " + fieldKey + " sort");
        removeCustomizeViewItem(test, fieldKey, ViewItemType.Sort);
    }

    public static void clearCustomizeViewColumns(BaseSeleniumWebTest test)
    {
        test.log("Clear all Customize View columns.");
        clearAllCustomizeViewItems(test, ViewItemType.Columns);
    }

    public static void clearCustomizeViewFilters(BaseSeleniumWebTest test)
    {
        test.log("Clear all Customize View filters.");
        clearAllCustomizeViewItems(test, ViewItemType.Filter);
    }

    public static void clearCustomizeViewSorts(BaseSeleniumWebTest test)
    {
        test.log("Clear all Customize View sorts.");
        clearAllCustomizeViewItems(test, ViewItemType.Sort);
    }

    private static void clearAllCustomizeViewItems(BaseSeleniumWebTest test, ViewItemType type)
    {
        changeTab(test, type);
        String tabXPath = tabContentXPath(type);

        String deleteButtonXPath = tabXPath + "//*[contains(@class, 'labkey-tool-close')]";
        while (test.isElementPresent(Locator.xpath(deleteButtonXPath)))
            test.click(Locator.xpath(deleteButtonXPath));
    }

    private static String folderFilterComboXPath()
    {
        return tabContentXPath(ViewItemType.Filter) + "//div[contains(@class, 'labkey-folder-filter-combo')]";
    }

    private static String folderFilterPaperclipXPath()
    {
        return tabContentXPath(ViewItemType.Filter) + "//table[contains(@class, 'labkey-folder-filter-paperclip')]";
    }

    public static void setFolderFilter(BaseSeleniumWebTest test, String folderFilter)
    {
        test.log("Setting folder filter to: " + folderFilter);
        changeTab(test, ViewItemType.Filter);

        String folderFilterComboXPath = folderFilterComboXPath();
        ExtHelper.selectComboBoxItem(test, Locator.xpath(folderFilterComboXPath), folderFilter);
    }

    public static void togglePaperclipFolderFilter(BaseSeleniumWebTest test)
    {
        Locator loc = Locator.xpath(folderFilterPaperclipXPath());
        String attr = test.getAttribute(loc, "class");
        if (attr.contains("x-btn-pressed"))
            unclipFolderFilter(test);
        else
            clipFolderFilter(test);
    }

    public static void clipFolderFilter(BaseSeleniumWebTest test)
    {
        test.log("Clip folder filter");
        changeTab(test, ViewItemType.Filter);

        Locator loc = Locator.xpath(folderFilterPaperclipXPath());
        test.assertAttributeNotContains(loc, "class", "x-btn-pressed");
        test.click(loc);
        test.assertAttributeContains(loc, "class", "x-btn-pressed");
    }

    public static void unclipFolderFilter(BaseSeleniumWebTest test)
    {
        test.log("Unclip folder filter");
        changeTab(test, ViewItemType.Filter);

        Locator loc = Locator.xpath(folderFilterPaperclipXPath());
        test.assertAttributeContains(loc, "class", "x-btn-pressed");
        test.click(loc);
        test.assertAttributeNotContains(loc, "class", "x-btn-pressed");
    }

    public static void clipFilter(BaseSeleniumWebTest test, String column_id)
    {
        throw new RuntimeException("not yet implemented");
    }

    public static void clipSort(BaseSeleniumWebTest test, String column_id)
    {
        throw new RuntimeException("not yet implemented");
    }

    public static void moveCustomizeViewColumn(BaseSeleniumWebTest test, String fieldKey, boolean moveUp)
    {
        test.log("Moving filter, " + fieldKey + " " + (moveUp ? "up." : "down."));
        moveCustomizeViewItem(test, fieldKey, moveUp, ViewItemType.Columns);
    }

    public static void moveCustomizeViewFilter(BaseSeleniumWebTest test, String fieldKey, boolean moveUp)
    {
        test.log("Moving filter, " + fieldKey + " " + (moveUp ? "up." : "down."));
        moveCustomizeViewItem(test, fieldKey, moveUp, ViewItemType.Filter);
    }

    public static void moveCustomizeViewSort(BaseSeleniumWebTest test, String fieldKey, boolean moveUp)
    {
        test.log("Moving sort, " + fieldKey + " " + (moveUp ? "up." : "down."));
        moveCustomizeViewItem(test, fieldKey, moveUp, ViewItemType.Sort);
    }

    private static void moveCustomizeViewItem(final BaseSeleniumWebTest test, String fieldKey, boolean moveUp, ViewItemType type)
    {
        final String itemXPath = itemXPath(type, fieldKey);
        changeTab(test, type);
        final int itemIndex = test.getElementIndex(Locator.xpath(itemXPath));

        moveCustomizeViewItem(test, itemIndex, moveUp, type);

        test.waitFor(new BaseSeleniumWebTest.Checker()
        {
            public boolean check()
            {
                return itemIndex != test.getElementIndex(Locator.xpath(itemXPath));
            }
        }, "Item was not reordered.", BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    private static void moveCustomizeViewItem(BaseSeleniumWebTest test, int field_index, boolean moveUp, ViewItemType type)
    {
        String fromItemXPath = itemXPath(type, field_index);
        String toItemXPath = itemXPath(type, moveUp ? field_index - 1 : field_index + 1 );

        changeTab(test, type);
        test.dragAndDrop(Locator.xpath(fromItemXPath), Locator.xpath(toItemXPath));               
    }

    public static void removeColumnProperties(BaseSeleniumWebTest test, String fieldKey)
    {
        setColumnProperties(test, fieldKey, null, new ArrayList<Map<String,String>>());
    }

    public static void setColumnProperties(BaseSeleniumWebTest test, String fieldKey, String caption, Map<String, String> aggregate)
    {
        List<Map<String, String>> aggregates = new ArrayList<Map<String,String>>();
        aggregates.add(aggregate);
        setColumnProperties(test, fieldKey, caption, aggregates);
    }

    /**
     * Sets the column title and aggregate.
     * @param test The test.
     * @param fieldKey The field key of the column to change.  Note that the column should already be in the selected column list.
     * @param caption The caption value or null to unset the column caption.
     * @param aggregates An array of the aggregates to apply to the column or null to unset.
     */
    public static void setColumnProperties(BaseSeleniumWebTest test, String fieldKey, String caption, List<Map<String, String>> aggregates)
    {
        String msg = "Setting column " + fieldKey;
        if (caption != null)
            msg = msg + " caption to '" + caption + "'";
        if (aggregates != null && aggregates.size() > 0)
            msg = msg + " aggregates to '" + StringUtils.join(aggregates, ", ") + "'";
        test.log(msg);

        changeTab(test, ViewItemType.Columns);

        String itemXPath = itemXPath(ViewItemType.Columns, fieldKey);
        test.click(Locator.xpath(itemXPath + "//div[contains(@class, 'labkey-tool-gear')]"));
        ExtHelper.waitForExtDialog(test, "Edit column properties for", BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        String parent = "//div[contains(@class, 'x-window')]";

        if (caption == null)
            caption = "";
        test.setFormElement(Locator.xpath(parent + "//input[contains(@class, 'x-form-text')]"), caption);

        //reset all aggregates
        String deleteButtonXPath = "//div[contains(@class, 'x-window')]" + "//*[contains(@class, 'labkey-tool-close')]";
        while (test.isElementPresent(Locator.xpath(deleteButtonXPath)))
            test.click(Locator.xpath(deleteButtonXPath));

        //then re-add them
        int idx = 1;
        Locator grid = Locator.xpath(parent + "//div[contains(@class, 'x-grid-panel')]");

        if(aggregates != null)
        {
            for(Map<String, String> aggregate : aggregates)
            {
                if (aggregate == null || aggregate.get("type") == null)
                    continue;

                test.clickButton("Add Aggregate", 0);
                Locator row = ExtHelper.locateExt3GridRow(idx, parent);

                Locator comboCell = ExtHelper.locateExt3GridCell(row, 1);
                test.dblclickAtAndWait(comboCell);
                ExtHelper.selectComboBoxItem(test, (Locator.XPathLocator)grid, aggregate.get("type"));

                if(aggregate.get("label") != null){
                    Locator labelCell = ExtHelper.locateExt3GridCell(row, 2);
                    test.dblclickAtAndWait(labelCell);

                    Locator fieldPath = ((Locator.XPathLocator) grid).child("/input[contains(@class, 'x-form-text') and not(../img)]");
                    test.setFormElement(fieldPath, aggregate.get("label"));
                }

                idx++;
            }
        }
        test.clickButton("OK", 0);
    }

    /**
     * pre-conditions:  at page with grid for which you would like an R view (grid should be only
     *      or at least first element on page)
     * post-conditions:  grid has R view of name name
     * @param test
     * @param view   string to enter in view box (null for default)
     * @param name name to give new R view
     */
    public static void createRView(BaseSeleniumWebTest test, String view, String name)
    {
        test.waitForText(("Views"));
        test.clickMenuButtonAndContinue("Views", "Create", "R View");
        test.waitForPageToLoad();

        if(view!=null)
            Assert.fail("Unimplemented");

        test.clickButton("Save", 0);


        test.setFormElement(Locator.xpath("//input[@class='ext-mb-input']"), name);
        ExtHelper.clickExtButton(test, "Save");
    }

    public static boolean isColumnPresent(BaseSeleniumWebTest test, String fieldKey)
    {
        Locator checkbox = expandPivots(test,fieldKey.split("/"));
        return test.isElementPresent(checkbox);
    }
}
