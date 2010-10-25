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
import org.apache.commons.lang.StringUtils;
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

    private static void addCustomizeViewItem(BaseSeleniumWebTest test, String column_id, String column_name, ViewItemType type)
    {
        // column_id is the value contained in ext:tree-node-id
        test.log("Adding " + column_name + " " + type.toString());

        changeTab(test, type);

        String[] nodes = column_id.split("/");
        String nodePath = "";

        // Expand all nodes necessary to reveal the desired node.
        for( int i = 0; i < nodes.length - 1; i ++ )
        {
            nodePath += nodes[i].replace("\\", "/"); // un-escape slashes
            try
            {
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

        test.checkCheckbox(Locator.xpath("//div[contains(@class, 'x-tree-node') and @*='" + column_id.replace("\\", "/") + "']/input[@type='checkbox']"));
    }

    public static void addCustomizeViewColumn(BaseSeleniumWebTest test, String column_id, String column_name)
    {
        // column_id is the value contained in ext:tree-node-id
        test.log("Adding " + column_name + " column");

        addCustomizeViewItem(test, column_id, column_name, ViewItemType.Columns);
    }

    public static void addCustomizeViewFilter(BaseSeleniumWebTest test, String column_id, String filter_type)
    {
        addCustomizeViewFilter(test, column_id, column_id, filter_type, "");
    }

    public static void addCustomizeViewFilter(BaseSeleniumWebTest test, String column_id, String filter_type, String filter)
    {
        addCustomizeViewFilter(test, column_id, column_id, filter_type, filter);
    }

    public static void addCustomizeViewFilter(BaseSeleniumWebTest test, String column_id, String column_name, String filter_type, String filter)
    {
        if (filter.equals(""))
            test.log("Adding " + column_name + " filter of " + filter_type);
        else
            test.log("Adding " + column_name + " filter of " + filter_type + " " + filter);

        changeTab(test, ViewItemType.Filter);
        String itemXPath = itemXPath(ViewItemType.Filter, column_id);

        if (!test.isElementPresent(Locator.xpath(itemXPath)))
        {
            // Add filter if it doesn't exist
            addCustomizeViewItem(test, column_id, column_name, ViewItemType.Filter);
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

    private static String itemXPath(ViewItemType type, String column_id)
    {
        // XXX: why doesn't 'fieldKey' work?
        //return tabContentXPath(type) + "//table[contains(@class, 'labkey-customview-item') and @fieldkey='" + column_id +"']";
        return "//table[contains(@class, 'labkey-customview-" + type.toString().toLowerCase() + "-item') and @fieldkey='" + column_id +"']";
    }

    private static String itemXPath(ViewItemType type, int item_index)
    {
        //return tabContentXPath(type) + "//table[contains(@class, 'labkey-customview-item')][" + item_index + "]";
        return "//table[contains(@class, 'labkey-customview-" + type.toString().toLowerCase() + "-item')][" + item_index + "]";
    }

    private static void removeCustomizeViewItem(BaseSeleniumWebTest test, String column_id, ViewItemType type)
    {
        changeTab(test, type);

        String itemXPath = itemXPath(type, column_id);

        Locator item = Locator.xpath(itemXPath);
        Locator close = Locator.xpath(itemXPath + "//*[contains(@class, 'labkey-tool-close')]");

        //test.assertElementPresent(item);

        // XXX: removes all filter clauses
        while (test.isElementPresent(item))
            test.click(close);
    }

    private static void removeCustomizeViewItem(BaseSeleniumWebTest test, int item_index, ViewItemType type)
    {
        changeTab(test, type);

        String itemXPath = itemXPath(type, item_index);

        Locator item = Locator.xpath(itemXPath);
        Locator close = Locator.xpath(itemXPath + "//*[contains(@class, 'labkey-tool-close')]");

        //test.assertElementPresent(item);

        // XXX: removes all filter clauses
        while (test.isElementPresent(item))
            test.click(close);
    }

    public static void addCustomizeViewSort(BaseSeleniumWebTest test, String column_name, String order)
    {
        addCustomizeViewSort(test, column_name, column_name, order);
    }

    public static void addCustomizeViewSort(BaseSeleniumWebTest test, String column_id, String column_name, String order)
    {
        test.log("Adding " + column_name + " sort");
        String itemXPath = itemXPath(ViewItemType.Sort, column_id);

        test.assertElementNotPresent(Locator.xpath(itemXPath));
        addCustomizeViewItem(test, column_id, column_name, ViewItemType.Sort);

        ExtHelper.selectComboBoxItem(test, Locator.xpath(itemXPath), order);
    }

    public static void removeCustomizeViewColumn(BaseSeleniumWebTest test, String column_id)
    {
        test.log("Removing " + column_id + " column");
        removeCustomizeViewItem(test, column_id, ViewItemType.Columns);
    }

    public static void removeCustomizeViewFilter(BaseSeleniumWebTest test, String column_id)
    {
        test.log("Removing " + column_id + " filter");
        removeCustomizeViewItem(test, column_id, ViewItemType.Filter);
    }

    public static void removeCustomizeViewFilter(BaseSeleniumWebTest test, int item_index)
    {
        test.log("Removing filter at position " + item_index);
        removeCustomizeViewItem(test, item_index, ViewItemType.Filter);
    }

    public static void removeCustomizeViewSort(BaseSeleniumWebTest test, String column_id)
    {
        test.log("Removing " + column_id + " sort");
        removeCustomizeViewItem(test, column_id, ViewItemType.Sort);
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

    /*
    public static void moveCustomizeViewColumn(BaseSeleniumWebTest test, String column_id, boolean moveUp)
    {
        test.log("Moving filter, " + column_id + " " + (moveUp ? "up." : "down."));
    }

    public static void moveCustomizeViewFilter(BaseSeleniumWebTest test, String column_id, boolean moveUp)
    {
        test.log("Moving filter, " + column_id + " " + (moveUp ? "up." : "down."));
        moveCustomizeViewItem(test, column_id, moveUp, ViewItemType.Filter);
    }

    public static void moveCustomizeViewSort(BaseSeleniumWebTest test, String column_id, boolean moveUp)
    {
        test.log("Moving sort, " + column_id + " " + (moveUp ? "up." : "down."));
        moveCustomizeViewItem(test, column_id, moveUp, ViewItemType.Sort);
    }

    private static void moveCustomizeViewItem(BaseSeleniumWebTest test, String column_id, boolean moveUp, ViewItemType type)
    {
        test.log("Moving " + column_id + " " + (moveUp ? "up." : "down."));

        String itemXPath = itemXPath(type, column_id);
        test.mouseDown(Locator.xpath(itemXPath));

        test.getWrapper().mouseMove();
        test.getWrapper().mouseOver();
        test.getWrapper().mouseUp();
    }
    */
}
