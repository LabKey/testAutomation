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

    public static void addCustomizeViewColumn(BaseSeleniumWebTest test, String column_id, String column_name)
    {
        // column_id is the value contained in ext:tree-node-id
        test.log("Adding " + column_name + " column");

        ExtHelper.clickExtTab(test, "Columns");

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

    private static enum ViewItemType
    { filter, sort }

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
        if (filter.compareTo("") == 0)
            test.log("Adding " + column_name + " filter of " + filter_type);
        else
            test.log("Adding " + column_name + " filter of " + filter_type + " " + filter);

        addCustomizeViewItem(test, column_id, column_name, filter_type, filter, ViewItemType.filter);
    }

    private static void addCustomizeViewItem(BaseSeleniumWebTest test, String column_id, String column_name, String filter_type, String filter, ViewItemType type)
    {
        // column_id refers to the '/' delimited String describing the path to the desired column

        String customFilterTabXpath = "//div[contains(@class, 'test-" + type.toString() + "-tab')]";
        String customFilterItemXpath = customFilterTabXpath + "//dt[contains(@class, 'labkey-customview-item')]";

        ExtHelper.clickExtTabContainingText(test, type == ViewItemType.filter ? "Filter" : "Sort");
        test.click(Locator.xpath(customFilterTabXpath + "//button[@title='Add']"));

        String indexStr = "[" + test.getXpathCount(new Locator.XPathLocator(customFilterItemXpath)) + "]";
        selectField(test, column_id, column_name, customFilterItemXpath + indexStr);

        ExtHelper.selectComboBoxItem(test, Locator.xpath(customFilterItemXpath + indexStr), filter_type);

        if ( !(filter.compareTo("") == 0) )
        {
            test.setFormElement(Locator.xpath(customFilterItemXpath + indexStr + "//input[contains(@class, 'item-value')]"), filter);
            test.fireEvent(Locator.xpath(customFilterItemXpath + indexStr + "//input[contains(@class, 'item-value')]"), BaseSeleniumWebTest.SeleniumEvent.blur);
        }
    }

    private static void removeCustomizeViewItem(BaseSeleniumWebTest test, String column_name, ViewItemType type)
    {
        String customFilterTabXpath = "//div[contains(@class, 'test-" + type.toString() + "-tab')]";
        String customFilterItemXpath = customFilterTabXpath + "//dt[contains(@class, 'labkey-customview-item') and .//button[text() = '" + column_name + "']]";
        ExtHelper.clickExtTabContainingText(test, type == ViewItemType.filter ? "Filter" : "Sort");

        test.click(Locator.xpath(customFilterItemXpath + "//div[contains(@class, 'item-close')]"));
    }

    private static void removeCustomizeViewItem(BaseSeleniumWebTest test, int item_index, ViewItemType type)
    {
        String customFilterTabXpath = "//div[contains(@class, 'test-" + type.toString() + "-tab')]";
        String customFilterItemXpath = customFilterTabXpath + "//dt[contains(@class, 'labkey-customview-item')][" + item_index + "]";
        ExtHelper.clickExtTabContainingText(test, type == ViewItemType.filter ? "Filter" : "Sort");

        test.click(Locator.xpath(customFilterItemXpath + "//div[contains(@class, 'item-close')]"));
    }

    private static void selectField(BaseSeleniumWebTest test, String column_id, String column_name, String itemXpath)
    {
        // This could be unstable if there are multiple menu options with the same text.
        String[] nodes = column_id.split("/");

        test.click(Locator.xpath(itemXpath + "//button"));

        String menuXpath = "//div[contains(@style, 'visibility: visible')]";

        if ( nodes.length <= 1 )
        {
            test.waitForElement(Locator.xpath(menuXpath + "//span[contains(@class, 'x-menu-item-text') and text()='" + column_name.replace("\\", "/") + "']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
            test.click(Locator.xpath(menuXpath + "//span[contains(@class, 'x-menu-item-text') and text()='" + column_name.replace("\\", "/") + "']"));
            return;
        }

        test.waitForElement(Locator.xpath(menuXpath + "//span[contains(@class, 'x-menu-item-text') and text()='" + nodes[0].replace("\\", "/") + "']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        // Traverse sub-menus.
        int i = 0;
        for( i = 0; i < nodes.length - 1; i ++ )
        {
            test.mouseOver(Locator.xpath(menuXpath + "//span[contains(@class, 'x-menu-item-text') and not(../../../li[contains(@class, 'x-menu-item-active')]) and text()='" + nodes[i].replace("\\", "/") + "']"));
            test.waitForElement(Locator.xpath(menuXpath + "//span[contains(@class, 'x-menu-item-text') and not(../../../li[contains(@class, 'x-menu-item-active')]) and text()='" + nodes[i + 1].replace("\\", "/") + "']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        }
        test.click(Locator.xpath(menuXpath + "//span[contains(@class, 'x-menu-item-text') and not(../../../li[contains(@class, 'x-menu-item-active')]) and text()='" + column_name.replace("\\", "/") + "']"));
    }

    public static void addCustomizeViewSort(BaseSeleniumWebTest test, String column_name, String order)
    {
        addCustomizeViewSort(test, column_name, column_name, order);
    }

    public static void addCustomizeViewSort(BaseSeleniumWebTest test, String column_id, String column_name, String order)
    {
        test.log("Adding " + column_name + " sort");
        addCustomizeViewItem(test, column_id, column_name, order, "", ViewItemType.sort);
    }

    public static void removeCustomizeViewColumn(BaseSeleniumWebTest test, String column_name)
    {
        test.log("Removing " + column_name + " column");
        test.click(Locator.xpath("//em[text()='" + column_name + "']"));
        test.click(Locator.xpath("//button[@title = 'Delete']"));
    }

    public static void removeCustomizeViewFilter(BaseSeleniumWebTest test, String column_name)
    {
        test.log("Removing " + column_name + " filter");
        removeCustomizeViewItem(test, column_name, ViewItemType.filter);
    }

    public static void removeCustomizeViewFilter(BaseSeleniumWebTest test, int filter_place)
    {
        test.log("Removing filter at position " + filter_place);
        removeCustomizeViewItem(test, filter_place, ViewItemType.filter);
    }

    public static void removeCustomizeViewSort(BaseSeleniumWebTest test, String column_name)
    {
        test.log("Removing " + column_name + " sort");
        removeCustomizeViewItem(test, column_name, ViewItemType.sort);
    }

    public static void clearCustomizeViewFilters(BaseSeleniumWebTest test)
    {
        test.log("Clear all Customize View filters.");
        clearAllCustomizeViewItems(test, ViewItemType.filter);
    }

    public static void clearCustomizeViewSorts(BaseSeleniumWebTest test)
    {
        test.log("Clear all Customize View sorts.");
        clearAllCustomizeViewItems(test, ViewItemType.sort);
    }

    private static void clearAllCustomizeViewItems(BaseSeleniumWebTest test, ViewItemType type)
    {
        String customFilterTabXpath = "//div[contains(@class, 'test-" + type.toString() + "-tab')]";
        String customFilterItemXpath = customFilterTabXpath + "//dt[contains(@class, 'labkey-customview-item')]";
        String deleteButtonXpath = customFilterItemXpath + "//div[contains(@class, 'item-close')]";

        ExtHelper.clickExtTabContainingText(test, type == ViewItemType.filter ? "Filter" : "Sort");

        while(test.isElementPresent(Locator.xpath(deleteButtonXpath)))
            test.click(Locator.xpath(deleteButtonXpath));
    }

    public static void clearCustomizeViewColumns(BaseSeleniumWebTest test)
    {
        test.click(Locator.xpath("//span[contains(@class, 'x-tab-strip-text') and text()='Columns']"));
        while ( !test.isElementPresent(Locator.xpath("//div[text()='No fields selected']")) )
        {
            test.click(Locator.xpath("//div[contains(@class, 'x-list-body-inner')]//em[1]"));
            test.click(Locator.xpath("//button[@title='Delete']"));
        }
    }

    public static void moveCustomizeViewColumn(BaseSeleniumWebTest test, String column_name, boolean moveUp)
    {
        test.log("Moving " + column_name + " " + (moveUp ? "up." : "down."));
        test.click(Locator.xpath("//em[text()='" + column_name + "']"));

        String direction = moveUp ? "Move Up" : "Move Down";
        test.click(Locator.xpath("//button[@title = '" + direction + "']"));
    }

    public static void moveCustomizeViewFilter(BaseSeleniumWebTest test, String column_name, boolean moveUp)
    {
        test.log("Moving filter, " + column_name + " " + (moveUp ? "up." : "down."));
        moveCustomizeViewItem(test, column_name, moveUp, ViewItemType.filter);
    }

    public static void moveCustomizeViewSort(BaseSeleniumWebTest test, String column_name, boolean moveUp)
    {
        test.log("Moving sort, " + column_name + " " + (moveUp ? "up." : "down."));
        moveCustomizeViewItem(test, column_name, moveUp, ViewItemType.sort);
    }

    private static void moveCustomizeViewItem(BaseSeleniumWebTest test, String column_name, boolean moveUp, ViewItemType type)
    {
        String customFilterTabXpath = "//div[contains(@class, 'test-" + type.toString() + "-tab')]";
        String customFilterItemXpath = customFilterTabXpath + "//dt[contains(@class, 'labkey-customview-item') and .//button[text() = '" + column_name + "']]";

        ExtHelper.clickExtTabContainingText(test, type == ViewItemType.filter ? "Filter" : "Sort");
        String direction = moveUp ? "Move Up" : "Move Down";

        test.click(Locator.xpath(customFilterItemXpath));
        test.click(Locator.xpath(customFilterTabXpath + "//button[@title='" + direction + "']"));
    }
}
