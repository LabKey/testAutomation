/*
 * Copyright (c) 2010 LabKey Corporation
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

import junit.framework.AssertionFailedError;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

/**
 * User: Trey Chadick
 * Date: Oct 6, 2010
 * Time: 2:32:05 PM
 */

public class CustomizeViewsHelper
{
    public static void openCustomizeViewPanel(BaseSeleniumWebTest test)
    {
        test.clickMenuButtonAndContinue("Views", "Customize New View");
        test.waitForElement(Locator.xpath("//button[text()='Apply']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public static void saveCustomView(BaseSeleniumWebTest test, String name)
    {
        test.clickNavButton("Save", 0);
        if (name != null)
            test.setFormElement(Locator.xpath("//div[contains(@style, 'display: block')]//input[@type='text']"), name);
        test.clickButtonByIndex("Save", 1);
    }

    public static void resetCustomView(BaseSeleniumWebTest test)
    {
        test.clickNavButton("Reset", 0);
        test.clickNavButton("Yes");
    }

    public static void addCustomizeViewColumn(BaseSeleniumWebTest test, String column_name)
    {
        addCustomizeViewColumn(test, column_name, column_name);
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

    private static void addCustomizeViewItem(BaseSeleniumWebTest test, String fieldKey, String column_name, ViewItemType type)
    {
        // fieldKey is the value contained in ext:tree-node-id
        test.log("Adding " + column_name + " " + type.toString());

        changeTab(test, type);

        String[] nodes = fieldKey.split("/");
        String nodePath = "";

        // Expand all nodes necessary to reveal the desired node.
        for( int i = 0; i < nodes.length - 1; i ++ )
        {
            nodePath += nodes[i].replace("\\", "/"); // un-escape slashes
            try
            {
                // Selenium XPath doesn't support attribute namespaces. Looking for fieldkey in @* should be good enough.
                test.click(Locator.xpath("//div[contains(@class, 'x-tree-node') and @*='" + nodePath + "']/img[1][contains(@class, 'plus')]"));
            }
            catch(AssertionFailedError se)
            {
                test.log("Unable to expand node (probably already expanded): " + nodePath);
                // continue
            }
            test.waitForElement(Locator.xpath("//div[contains(@class, 'x-tree-node') and @*='" + nodePath + "']/img[1][contains(@class, 'minus')]"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
            nodePath += "/";
        }

        test.checkCheckbox(Locator.xpath("//div[contains(@class, 'x-tree-node') and @*='" + fieldKey.replace("\\", "/") + "']/input[@type='checkbox']"));
    }

    public static void addCustomizeViewColumn(BaseSeleniumWebTest test, String fieldKey, String column_name)
    {
        // fieldKey is the value contained in ext:tree-node-id
        test.log("Adding " + column_name + " column");

        addCustomizeViewItem(test, fieldKey, column_name, ViewItemType.Columns);
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
        if (filter.equals(""))
            test.log("Adding " + column_name + " filter of " + filter_type);
        else
            test.log("Adding " + column_name + " filter of " + filter_type + " " + filter);

        changeTab(test, ViewItemType.Filter);
        String itemXPath = itemXPath(ViewItemType.Filter, fieldKey);

        if (!test.isElementPresent(Locator.xpath(itemXPath)))
        {
            // Add filter if it doesn't exist
            addCustomizeViewItem(test, fieldKey, column_name, ViewItemType.Filter);
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

    private static String itemXPath(ViewItemType type, String fieldKey)
    {
        return "//table[contains(@class, 'labkey-customview-" + type.toString().toLowerCase() + "-item') and @fieldkey='" + fieldKey +"']";
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
        test.log("Adding " + column_name + " sort");
        String itemXPath = itemXPath(ViewItemType.Sort, fieldKey);

        test.assertElementNotPresent(Locator.xpath(itemXPath));
        addCustomizeViewItem(test, fieldKey, column_name, ViewItemType.Sort);

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

    private static String folderFilterPinXPath()
    {
        return tabContentXPath(ViewItemType.Filter) + "//div[contains(@class, 'labkey-folder-filter-pin')]";
    }

    public static void pinAllFiltersAndSorts(BaseSeleniumWebTest test)
    {
        test.log("Pin all filters and sorts.");

        String pinXpath = "//div[contains(@class, 'labkey-tool-unpin')]";

        changeTab(test, ViewItemType.Filter);
        while (test.isElementPresent(Locator.xpath(pinXpath)))
        {
            int count = test.getXpathCount(Locator.xpath(pinXpath));
            test.click(Locator.xpath(pinXpath));
            test.assertElementPresent(Locator.xpath(pinXpath), count - 1);
        }

        throw new RuntimeException("Method not tested, please verify results");
    }

    public static void setFolderFilter(BaseSeleniumWebTest test, String folderFilter)
    {
        test.log("Setting folder filter to: " + folderFilter);
        changeTab(test, ViewItemType.Filter);

        String folderFilterComboXPath = folderFilterComboXPath();
        ExtHelper.selectComboBoxItem(test, Locator.xpath(folderFilterComboXPath), folderFilter);
    }

    public static void togglePinFolderFilter(BaseSeleniumWebTest test)
    {
        Locator folderFilterPinXPath = Locator.xpath(folderFilterPinXPath());
        String attr = test.getAttribute(folderFilterPinXPath, "class");
        if (attr.contains("labkey-tool-pin"))
            unpinFolderFilter(test);
        else if (attr.contains("labkey-tool-unpin"))
            pinFolderFilter(test);
        else
            test.fail("Expected to find folder filter pin state in attribute value: " + attr);
    }

    public static void pinFolderFilter(BaseSeleniumWebTest test)
    {
        test.log("Pinning folder filter");
        changeTab(test, ViewItemType.Filter);

        Locator folderFilterPinXPath = Locator.xpath(folderFilterPinXPath());
        test.assertAttributeContains(folderFilterPinXPath, "class", "labkey-tool-unpin");
        test.click(folderFilterPinXPath);
        test.assertAttributeContains(folderFilterPinXPath, "class", "labkey-tool-pin");
    }

    public static void unpinFolderFilter(BaseSeleniumWebTest test)
    {
        test.log("Unpinning folder filter");
        changeTab(test, ViewItemType.Filter);

        Locator folderFilterPinXPath = Locator.xpath(folderFilterPinXPath());
        test.assertAttributeContains(folderFilterPinXPath, "class", "labkey-tool-unpin");
        test.click(folderFilterPinXPath);
        test.assertAttributeContains(folderFilterPinXPath, "class", "labkey-tool-pin");
    }

    public static void pinFilter(BaseSeleniumWebTest test, String column_id)
    {
        throw new RuntimeException("not yet implemented");
    }

    public static void pinSort(BaseSeleniumWebTest test, String column_id)
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

    private static void moveCustomizeViewItem(BaseSeleniumWebTest test, String fieldKey, boolean moveUp, ViewItemType type)
    {
        String itemXPath = itemXPath(type, fieldKey);
        changeTab(test, type);
        int itemIndex = test.getElementIndex(Locator.xpath(itemXPath));

        moveCustomizeViewItem(test, itemIndex, moveUp, type);
    }

    private static void moveCustomizeViewItem(BaseSeleniumWebTest test, int field_index, boolean moveUp, ViewItemType type)
    {
        String fromItemXPath = itemXPath(type, field_index);
        String toItemXPath = itemXPath(type, moveUp ? field_index - 1 : field_index + 1 );
                                 
        changeTab(test, type);
        test.dragAndDrop(Locator.xpath(fromItemXPath), Locator.xpath(toItemXPath));               
    }
}
