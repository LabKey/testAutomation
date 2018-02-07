/*
 * Copyright (c) 2012-2017 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_PAGE;
import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.Locator.NOT_HIDDEN;

import static org.junit.Assert.*;

public class ExtHelper
{
    WebDriverWrapper _test;
    public String insertNewRowText = DataRegionTable.getInsertNewButtonText();
    public String importBulkDataText = DataRegionTable.getImportBulkDataText();

    public ExtHelper(WebDriverWrapper test)
    {
        _test = test;
    }

    public void clickInsertNewRow()
    {
        clickMenuButton(true, "Insert", insertNewRowText);
    }

    public void clickInsertNewRow(boolean wait)
    {
        clickMenuButton(wait, "Insert", insertNewRowText);
    }

    public void clickImportBulkData()
    {
        clickMenuButton(true, "Insert", importBulkDataText);
    }

    public void clickImportBulkData(boolean wait)
    {
        clickMenuButton(wait, "Insert", importBulkDataText);
    }

    public void clickMenuButton(String menusLabel, String... subMenuLabels)
    {
        clickMenuButton(true, menusLabel, subMenuLabels);
    }

    /**
     * Clicks the Ext or labkey menu item from the submenu specified by the menu object's text
     */
    public void clickMenuButton(boolean wait, String menusLabel, String... subMenuLabels)
    {
        Locator menu = Locator.extButton(menusLabel);
        if (!_test.isElementPresent(menu))
            menu = Locator.lkButton(menusLabel);
        if (!_test.isElementPresent(menu))
            fail("No Ext or LabKey menu for label '" + menusLabel + "' found");
        clickExtMenuButton(wait, menu, subMenuLabels);
    }

    public void clickExtMenuButton(Locator menu, String... subMenuLabels)
    {
        clickExtMenuButton(true, menu, subMenuLabels);
    }

    /**
     * Clicks the ext menu item from the submenu specified by the ext object's text
     */
    public void clickExtMenuButton(boolean wait, Locator menu, String... subMenuLabels)
    {
        _test.mouseOver(menu);
        _test.click(menu);
        for (int i = 0; i < subMenuLabels.length - 1; i++)
        {
            Locator parentLocator = Locator.menuItem(subMenuLabels[i]);
            _test.waitForElement(parentLocator, WAIT_FOR_JAVASCRIPT);
            _test.mouseOver(parentLocator);
        }
        if(subMenuLabels.length > 0)
        {
            Locator itemLocator = Locator.menuItem(subMenuLabels[subMenuLabels.length - 1]);
            _test.waitForElement(itemLocator, WAIT_FOR_JAVASCRIPT);
            if (wait)
                _test.clickAndWait(itemLocator);
            else
                _test.click(itemLocator);
        }
    }

    // Tests for the presence of the last specified submenu. Main menu item plus intervening submenus must exist.
    public boolean isExtMenuPresent(String menuLabel, String... subMenuLabels)
    {
        Locator menu = Locator.extButton(menuLabel);
        if (!_test.isElementPresent(menu))
            menu = Locator.lkButton(menuLabel);
        if (!_test.isElementPresent(menu))
            fail("No Ext or LabKey menu for label '" + menuLabel + "' found");
        _test.click(menu);

        for (int i = 0; i < subMenuLabels.length - 1; i++)
        {
            Locator parentLocator = Locator.menuItem(subMenuLabels[i]);
            _test.waitForElement(parentLocator, WAIT_FOR_JAVASCRIPT);
            _test.mouseOver(parentLocator);
        }

        Locator itemLocator = Locator.menuItem(subMenuLabels[subMenuLabels.length - 1]);
        return _test.isElementPresent(itemLocator);
    }

    public boolean clickExtComponent(String id)
    {
        String script =
                "clickExtComponent = function (id) {\n" +
                "    var cmp = Ext.getCmp(id);\n" +
                "    if (cmp.handler)\n" +
                "        return cmp.fireEvent(\"click\");\n" +
                "    else if (cmp.href)\n" +
                "    {\n" +
                "        cmp.show();\n" +
                "        return true;\n" +
                "    }\n" +
                "    return false;\n" +
                "};" +
                "return clickExtComponent(arguments[0]);";
        return (Boolean)_test.executeScript(script, id);
    }

    public int getExtElementHeight(String className, int index)
    {
        List<WebElement> elements = _test.getDriver().findElements(By.className(className));
        return elements.get(index).getSize().height;        
    }

    public void setCodeMirrorValue(String id, String value)
    {
        String script =
                "setCodeMirrorValue = function(id, value) {\n" +
                "    try {\n" +
                "        if (LABKEY.CodeMirror && LABKEY.CodeMirror[id]) {\n" +
                "            var eal = LABKEY.CodeMirror[id];\n" +
                "            eal.setValue(value);\n" +
                "        }\n" +
                "        else {\n" +
                "            throw 'Unable to find code mirror instance.';\n" +
                "        }\n" +
                "    } catch (e) {\n" +
                "        throw 'setCodeMirrorValue() threw an exception: ' + e.message;\n" +
                "    }\n" +
                "};\n" +
                "setCodeMirrorValue(arguments[0], arguments[1]);";
        _test.executeScript(script, id, value);
    }

    public String getCodeMirrorValue(String id)
    {
        String script =
                "var getCodeMirrorValue = function(id) {\n" +
                "    try {\n" +
                "        if (LABKEY.CodeMirror && LABKEY.CodeMirror[id]) {\n" +
                "            var eal = LABKEY.CodeMirror[id];\n" +
                "            return eal.getValue();\n" +
                "        }\n" +
                "        else {\n" +
                "            throw 'Unable to find code mirror instance.';\n" +
                "        }\n" +
                "    } catch (e) {\n" +
                "        throw 'getCodeMirrorValue() threw an exception: ' + e.message;\n" +
                "    }\n" +
                "};\n" +
                "return getCodeMirrorValue(arguments[0]);";
        return (String)_test.executeScript(script, id);
    }

    /**
     * Returns a DOM Element id from an ext object id. Assumes that the ext component
     * has already been rendered.
     */
    public String getExtElementId(String extId)
    {
        for (int attempt = 0; attempt < 5; attempt++)
        {
            String script =
                    "getExtElementId = function (id) {\n" +
                    "    var cmp = Ext.getCmp(id);\n" +
                    "    if (cmp)\n" +
                    "    {\n" +
                    "        var el = cmp.getEl();\n" +
                    "        if (el)\n" +
                    "            return el.id;\n" +
                    "    }\n" +
                    "    return null;\n" +
                    "};" +
                    "return getExtElementId(arguments[0]);";
            String id = (String) _test.executeScript(script, extId);

            _test.log("Element id for ext component id: " + extId + " is: " + id);
            if (id != null)
                return id;
            _test.sleep(500);
        }

        fail("Failed to get element id for Ext component '" + extId + "'");
        return null;
    }

    public void waitForExtDialog(final String title)
    {
        waitForExtDialog(title, WAIT_FOR_JAVASCRIPT);
    }

    public void waitForExtDialog(final String title, int timeout)
    {
        _test.waitFor(() -> _test.isElementPresent(Locators.extDialog(title)),
                "Ext Dialog with title '" + title + "' did not appear after " + timeout + "ms", timeout);
    }

    public void waitForExtDialogToDisappear(String title)
    {
        waitForExtDialogToDisappear(title, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForExtDialogToDisappear(final String title, int timeout)
    {
        _test.waitFor(() -> !_test.isElementPresent(Locators.extDialog(title)),
                "Ext Dialog with title '" + title + "' was still present after " + timeout + "ms", timeout);
    }

    public String getExtMsgBoxText(String title)
    {
        return getExtMsgBoxText(title, WAIT_FOR_JAVASCRIPT);
    }

    public String getExtMsgBoxText(String title, int timeout)
    {
        waitForExtDialog(title, timeout);
        Locator locator = Locator.xpath("//div[contains(@class, 'x-window-dlg')]//span[contains(@class, 'ext-mb-text')]");
        String msg = _test.getText(locator);
        return msg;
    }

    public void setExtFormElement(String text)
    {
        // Currently used only in RReportHelper.  Modify as needed to be more universal.
        _test.setFormElement(Locator.xpath("//input[(contains(@class, 'ext-mb-input') or contains(@class, 'x-form-field')) and @type='text']"), text);
    }

    public void setExtFormElementByType(String windowTitle, String inputType, String text)
    {
        _test.setFormElement(Locator.xpath(getExtDialogXPath(windowTitle) + "//input[contains(@class, 'form-field') and @type='" + inputType + "']"), text);
    }

    public void setExtFormElementByLabel(String label, String text)
    {
        setExtFormElementByLabel(null, label, text);
    }

    public void setExtFormElementByLabel(String windowTitle, String label, String text)
    {
        Locator formElement = Locator.xpath(getExtDialogXPath(windowTitle) + "//div[./label/span[text()='"+label+"']]/div/*[self::input or self::textarea]");
        if (!_test.isElementPresent(formElement))
        {
            // try the ext4 version
            formElement = Locator.xpath(getExtDialogXPath(windowTitle) + "//td[./label[text()='"+label+"']]/../td/*[self::input or self::textarea]");
        }
        _test.setFormElement(formElement, text);
    }

    public String getExtFormElementByLabel(String label)
    {
        return getExtFormElementByLabel(null, label);
    }

    public String getExtFormElementByLabel(String windowTitle, String label)
    {
        Locator formElement = Locator.xpath(getExtDialogXPath(windowTitle) + "//div[./label/span[text()='"+label+"']]/div/*[self::input or self::textarea]");
        if (!_test.isElementPresent(formElement))
        {
            // try the ext4 version
            formElement = Locator.xpath(getExtDialogXPath(windowTitle) + "//td[./label[text()='"+label+"']]/../td/*[self::input or self::textarea]");
        }
        return _test.getFormElement(formElement);
    }

    /**
     * @deprecated Use {@link org.labkey.test.components.ext4.Window} or {@link Locators#window(String)}
     */
    @Deprecated
    public String getExtDialogXPath(String windowTitle)
    {
        if (windowTitle == null) return "";
        String ext3Dialog = "//div[contains(@class, 'x-window') and " + NOT_HIDDEN + " and "+
            "./div/div/div/div/span[contains(@class, 'x-window-header-text') and contains(string(), '" + windowTitle + "')]]";
        String ext4Dialog = "//div[contains(@class, '" + Ext4Helper.getCssPrefix() + "window') and " + NOT_HIDDEN + " and "+
            "./div/div/div/div/div/span[contains(@class, '" + Ext4Helper.getCssPrefix() + "window-header-text') and contains(string(), '" + windowTitle + "')]]";
        if( _test.isElementPresent(Locator.xpath(ext3Dialog)) )
            return ext3Dialog;
        else if( _test.isElementPresent(Locator.xpath(ext4Dialog)) )
            return ext4Dialog;
        else
            fail("Unable to locate Ext dialog: '" + windowTitle + "'");
        return null; // unreachable
    }

    public void waitForLoadingMaskToDisappear(int wait)
    {
        _test.waitForElementToDisappear(Locator.xpath("//div[contains(@class, 'x-mask-loading')]"), wait);
    }

    public void waitForExt3MaskToDisappear(int wait)
    {
        _test.waitForElementToDisappear(Locator.xpath("//div[contains(@class, 'ext-el-mask') and contains(@style, 'block')]"), wait);
    }

    public void waitForExt3Mask(int wait)
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'ext-el-mask') and contains(@style, 'block')]"), wait);
    }

    public static Locator.XPathLocator locateGridRowCheckbox(String rowTextContent)
    {
        return Locator.xpath("//div[contains(@class, 'x-grid3-row')]//td/div[text()='" + rowTextContent + "']//..//..//div[@class='x-grid3-row-checker']");
    }

    public static Locator.XPathLocator locateExt3GridRow(int rowIndex, String parent)
    {
        String base = "//div[contains(@class, 'x-grid-panel')]";

        if(parent != null)
            base = parent + base;

        return Locator.xpath("(" + base + "//table[contains(@class, 'x-grid3-row-table')])[" + rowIndex + "]");
    }

    public static Locator.XPathLocator locateExt3GridCell(Locator row, int cellIndex)
    {
        return Locator.xpath("(" + ((Locator.XPathLocator) row).toXpath() + "//td[contains(@class, 'x-grid3-cell')])[" + cellIndex + "]");
    }

    public void clickXGridPanelCheckbox(int index, boolean keepExisting)
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'x-grid-panel')]"), WAIT_FOR_PAGE);
        selectExtGridItem(null, null, index, "x-grid-panel", keepExisting);
    }

    public void selectExtGridItem(String columnName, String columnVal, int idx, String markerCls, boolean keepExisting)
    {
        String script =
                "selectExtGridItem = function (columnName, columnVal, idx, markerCls, keepExisting) {\n" +
                "    // find the grid view ext element\n" +
                "    var el = Ext.DomQuery.selectNode('div.'+markerCls);\n" +
                "    if (el)\n" +
                "    {\n" +
                "        var grid = Ext.getCmp(el.id);\n" +
                "        if (grid)\n" +
                "        {\n" +
                "            if (idx == -1)\n" +
                "                idx = grid.getStore().find(columnName, columnVal);\n" +
                "            if (idx < grid.getStore().getCount())\n" +
                "            {\n" +
                "                if (idx >= 0)\n" +
                "                {\n" +
                "                    grid.getSelectionModel().selectRow(idx, keepExisting);\n" +
                "                }\n" +
                "                else\n" +
                "                {\n" +
                "                    throw 'Unable to locate ' + columnName + ': ' + columnVal;\n" +
                "                }\n" +
                "            }\n" +
                "            else\n" +
                "            {\n" +
                "                throw 'No such row: ' + idx;\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "    else\n" +
                "    {\n" +
                "        throw 'Unable to locate grid panel: ' + markerCls;\n" +
                "    }\n" +
                "};" +
                "selectExtGridItem(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]);";
        _test.executeScript(script, columnName, columnVal, idx, markerCls, keepExisting);
    }

    //Pick measure from one of multiple split panel measure pickers
    public void pickMeasure(String panelCls, String source, String measure, boolean isMultiSelect, boolean keepSelection)
    {
        _test.shortWait().until(ExpectedConditions.elementToBeClickable(By.cssSelector("." + panelCls + " .sourcepanel div.itemrow span.val"))); // if one row is ready, all should be
        _test.click(Locator.css("." + panelCls + " .sourcepanel div.itemrow span.val").withText(source));
        _test.shortWait().until(ExpectedConditions.elementToBeClickable(By.cssSelector("." + panelCls + " .sourcepanel div.itemrow span.val"))); // if one row is ready, all should be
        _test.waitAndClick(Locator.css("." + panelCls + " .sourcepanel div.itemrow span.val").withText(source));
        //select measure
        if (isMultiSelect)
        {
            _test.shortWait().until(ExpectedConditions.elementToBeClickable(By.cssSelector("." + panelCls + " .measuresgrid ." + Ext4Helper.getCssPrefix() + "grid-row"))); // if one row is ready, all should be
            selectExtGridItem("label", measure, -1, panelCls + " .measuresgrid", keepSelection);
        }
        else
        {
            _test.shortWait().until(ExpectedConditions.elementToBeClickable(By.cssSelector("." + panelCls + " .measuresgrid div.itemrow"))); // if one row is ready, all should be
            _test.click(Locator.css("." + panelCls + " .measuresgrid div.itemrow").withText(measure));
        }
    }

    public void pickMeasure(String panelCls, String source, String measure)
    {
        pickMeasure(panelCls, source, measure, false, false);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(Locator.XPathLocator parentLocator, @LoggedParam String selection)
    {
        selectComboBoxItem(parentLocator.findElement(_test.getDriver()), selection);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(WebElement comboEl, @LoggedParam String selection)
    {
        WebElement comboArrow = Locator.css(".x-form-arrow-trigger").findElement(comboEl);
        _test.click(comboArrow);
        Locator.XPathLocator comboListItemLoc = Locators.comboListItem().withText(selection);
        WebElement comboListItem = comboListItemLoc.findWhenNeeded(_test.getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
        _test.scrollIntoView(comboListItem);
        _test.waitAndClick(comboListItemLoc);
        if (_test.isElementPresent(comboListItemLoc))
        {
            _test.click(comboArrow);
            _test.waitForElementToDisappear(comboListItemLoc, WAIT_FOR_JAVASCRIPT);
        }
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(@LoggedParam String label, @LoggedParam String selection)
    {
        selectComboBoxItem(Locators.formItemWithLabel(label).notHidden(), selection);
    }

    public void selectGWTComboBoxItem(Locator.XPathLocator parentLocator, String selection)
    {
        _test.click(parentLocator.append(Locator.tagWithClass("div", "x-form-trigger-arrow")));
        _test.waitAndClick(Locator.tagWithClass("*", "x-combo-list-item").containing(selection));
    }

    public void closeExtTab(String tabName)
    {
        _test.log("Closing Ext tab " + tabName);
        _test.click(Locator.xpath("//a[contains(@class, 'x-tab-strip-close') and ..//span[contains(@class, 'x-tab-strip-text') and text()='" + tabName + "']]"));
    }

    public void clickExtTab(String tabname)
    {
        _test.log("Selecting Ext tab " + tabname);
        Locator l = Locator.xpath("//span[contains(@class, 'x-tab-strip-text') and text() = '" + tabname + "']");
        _test.waitAndClick(l);
    }

    public void clickSideTab(String tab)
    {
        if (_test.isElementPresent(Locator.xpath("//a[contains(@class, 'x-grouptabs-text') and span[contains(text(), '" + tab + "')]]")))
            // Tab hasn't rendered yet
            _test.click(Locator.xpath("//a[contains(@class, 'x-grouptabs-text') and span[contains(text(), '" + tab + "')]]"));
        else
            // Tab has rendered
            _test.click(Locator.xpath("//ul[contains(@class, 'x-grouptabs-strip')]/li[a[contains(@class, 'x-grouptabs-text') and contains(text(), '" + tab + "')]]"));
    }

    public void clickExtTabContainingText(String tabText)
    {
        _test.log("Selecting Ext tab " + tabText);
        _test.click(Locator.xpath("//span[contains(@class, 'x-tab-strip-text') and contains( text(), '" + tabText + "')]"));
    }

    public void clickExtButton(String caption)
    {
        clickExtButton(caption, WAIT_FOR_PAGE);
    }

    public void clickExtButton(String caption, int wait)
    {
        clickExtButton(null, caption, wait);
    }

    public void clickExtButton(String windowTitle, String caption)
    {
        clickExtButton(windowTitle, caption, WAIT_FOR_PAGE);
    }

     public void clickExtButton(String windowTitle, String caption, int wait)
     {
         clickExtButton(windowTitle, caption, wait, 1);
     }

    /**
     * click an ext button
     *
     * @param windowTitle title of the extWindow, or null if you would like to autodetect
     * @param caption the button text
     * @param wait time to wait for page to load
     * @param index 1-based index for multiple similar buttons
     */
    public void clickExtButton(String windowTitle, String caption, int wait, int index)
    {
        _test.log("Clicking Ext button with caption: " + caption);
        Locator loc = Locator.xpath((windowTitle!=null?getExtDialogXPath(windowTitle):"")+"//button[(contains(@class, 'x-btn-text') and text()='" + caption + "') or (@role='button' and ./span[text()='" + caption + "'])]["+index+"]");
        _test.waitForElement(loc, WAIT_FOR_JAVASCRIPT);
        _test.clickAndWait(loc, wait);
    }

    private static String jsString(String s)
    {
        if (s == null)
            return "''";

        StringBuilder js = new StringBuilder(s.length() + 10);
        js.append("'");
        int len = s.length();
        for (int i = 0 ; i<len ; i++)
        {
            char c = s.charAt(i);
            switch (c)
            {
                case '\\':
                    js.append("\\\\");
                    break;
                case '\n':
                    js.append("\\n");
                    break;
                case '\r':
                    js.append("\\r");
                    break;
                case '<':
                    js.append("\\x3C");
                    break;
                case '>':
                    js.append("\\x3E");
                    break;
                case '\'':
                    js.append("\\'");
                    break;
                case '\"':
                    js.append("\\\"");
                    break;
                default:
                    js.append(c);
                    break;
            }
        }
        js.append("'");
        return js.toString();
    }

    public static class Locators
    {
        /**
         * Locates title bar of an Ext 3 or 4 window
         *
         * @param title partial text of window title
         * @return Locator for window's title bar
         */
        public static Locator.XPathLocator extDialog(String title)
        {
            return Locator.xpath("//span[" + Locator.NOT_HIDDEN + " and contains(@class, 'window-header-text') and contains(string(), '" + title + "')]");
        }

        /**
         * Locates an Ext 3 window
         *
         * @param title Exact text of window title
         * @return Locator for Ext 3 window
         */
        public static Locator.XPathLocator window(String title)
        {
            return Locator.xpath("//div").withClass("x-window").notHidden().withDescendant(Locator.xpath("//span").withClass("x-window-header-text").withText(title));
        }

        public static Locator.XPathLocator checkerForGridRowContainingText(String text)
        {
            return Locator.xpath("//tr").withPredicate(Locator.xpath("td/div/a").withText(text)).append("//div").withClass("x-grid3-row-checker");
        }

        public static Locator.XPathLocator formItemWithLabel(String label)
        {
            return Locator.tagWithClass("div", "x-form-item").withPredicate(Locator.xpath("./label").withText(label));
        }

        public static Locator.XPathLocator comboListItem()
        {
            return Locator.tagWithClass("div", "x-combo-list-item").notHidden();
        }
    }
}
