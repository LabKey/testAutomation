/*
 * Copyright (c) 2012 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/**
 * User: klum
 * Date: Apr 6, 2009
 * Time: 5:21:53 PM
 */
public class ExtHelperWD extends AbstractHelperWD
{
    public ExtHelperWD(BaseWebDriverTest test)
    {
        super(test);
    }
    /**
     * Clicks the Ext or labkey menu item from the submenu specified by the menu object's text
     */
    public void clickMenuButton(boolean wait, String menusLabel, String... subMenuLabels)
    {
        Locator menu = Locator.extButton(menusLabel);
        if (!_test.isElementPresent(menu))
            menu = Locator.navButton(menusLabel);
        if (!_test.isElementPresent(menu))
            Assert.fail("No Ext or LabKey menu for label '" + menusLabel + "' found");
        clickExtMenuButton(wait, menu, subMenuLabels);
    }

    /**
     * Clicks the ext menu item from the submenu specified by the ext object's text
     */
    public void clickExtMenuButton(boolean wait, Locator menu, String... subMenuLabels)
    {
        _test.click(menu);
        for (int i = 0; i < subMenuLabels.length - 1; i++)
        {
            Locator parentLocator = Locator.menuItem(subMenuLabels[i]);
            _test.waitForElement(parentLocator, 1000);
            _test.mouseOver(parentLocator);
        }
        Locator itemLocator = Locator.menuItem(subMenuLabels[subMenuLabels.length - 1]);
        _test.waitForElement(itemLocator, 1000);
        if (wait)
            _test.clickAndWait(itemLocator);
        else
            _test.click(itemLocator);
    }

    // Tests for the presence of the last specified submenu. Main menu item plus intervening submenus must exist.
    public boolean isExtMenuPresent(String menuLabel, String... subMenuLabels)
    {
        Locator menu = Locator.extButton(menuLabel);
        if (!_test.isElementPresent(menu))
            menu = Locator.navButton(menuLabel);
        if (!_test.isElementPresent(menu))
            Assert.fail("No Ext or LabKey menu for label '" + menuLabel + "' found");
        _test.click(menu);

        for (int i = 0; i < subMenuLabels.length - 1; i++)
        {
            Locator parentLocator = Locator.menuItem(subMenuLabels[i]);
            _test.waitForElement(parentLocator, 1000);
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

    public void clickExtDropDownMenu(String menuId, String value)
    {
        clickExtDropDownMenu(Locator.id(menuId), value);
    }

    public int getExtElementHeight(String className, int index)
    {
        List<WebElement> elements = _test._driver.findElements(By.className(className));
        return elements.get(index).getSize().height;        
    }

    public void clickExtDropDownMenu(Locator menuLocator, String value)
    {
        _test.click(menuLocator);
        Locator element = Locator.xpath("//*[(self::li[contains(@class, 'x4-boundlist-item')] or self::div[contains(@class, 'x-combo-list-item')] or self::span[contains(@class, 'x-menu-item-text')]) and text()='" + value + "']");
        _test.waitForElement(element, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.click(element);
    }

    public void selectFolderManagementTreeItem(String path, boolean keepExisting)
    {
        selectExtFolderTreeNode(path, "folder-management-tree", keepExisting);
    }

    private void selectExtFolderTreeNode(String path, String markerCls, boolean keepExisting)
    {
        String script =
                "selectExtFolderTreeNode = function(containerPath, markerCls, keepExisting) {\n" +
                "    // Get Path Array\n" +
                "    var pathArray = containerPath.split(\"/\");\n" +
                "    if (pathArray.length == 0)\n" +
                "        throw 'Unable to parse path: ' + containerPath;\n" +
                "    // Remove invalid paths due to parsing\n" +
                "    if (pathArray[0] == \"\")\n" +
                "        pathArray = pathArray.slice(1);\n" +
                "    if (pathArray[pathArray.length-1] == \"\")\n" +
                "        pathArray = pathArray.slice(0, pathArray.length-1);\n" +
                "    var el = Ext.DomQuery.selectNode(\"div[class*='\"+markerCls+\"']\");\n" +
                "    if (el) {\n" +
                "        var tree = Ext.getCmp(el.id);\n" +
                "        if (tree) {\n" +
                "            var root = tree.getRootNode();\n" +
                "            if (!root) {\n" +
                "                throw 'Unable to find root node.';\n" +
                "            }\n" +
                "            var _path = \"\";\n" +
                "            var node;\n" +
                "            for (var i=0; i < pathArray.length; i++) {\n" +
                "                _path += '/' + pathArray[i];\n" +
                "                node = root.findChild('containerPath', _path, true);\n" +
                "                if (node) {\n" +
                "                    if (i==(pathArray.length-1)) {\n" +
                "                        var e = {};\n" +
                "                        if (keepExisting) {\n" +
                "                            e.ctrlKey = true;\n" +
                "                        }\n" +
                "                        tree.getSelectionModel().select(node, e, keepExisting);\n" +
                "                    }\n" +
                "                }\n" +
                "                else {\n" +
                "                    throw 'Unable to find node: ' + _path;\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "        else {\n" +
                "            throw el.id + ' does not appear to be a valid Ext Component.';\n" +
                "        }\n" +
                "    }\n" +
                "    else {\n" +
                "        throw 'Unable to locate tree panel: ' + markerCls;\n" +
                "    }\n" +
                "};" +
                "selectExtFolderTreeNode(arguments[0], arguments[1], arguments[2]);";
        _test.executeScript(script, path, markerCls, keepExisting);
    }

    public void setQueryEditorValue(String id, String value)
    {
        String script =
                "setEditAreaValue = function(id, value) {\n" +
                "    try {\n" +
                "        if (window.editAreaLoader) {\n" +
                "            var eal = window.editAreaLoader;\n" +
                "            eal.setValue(id, value);\n" +
                "        }\n" +
                "        else {\n" +
                "            throw 'Unable to find editAreaLoader.';\n" +
                "        }\n" +
                "    } catch (e) {\n" +
                "        throw 'setEditAreaValue() threw an exception: ' + e.message;\n" +
                "    }\n" +
                "};\n" +
                "setEditAreaValue(arguments[0], arguments[1]);";
        _test.executeScript(script, id, value);
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

        Assert.fail("Failed to get element id for Ext component '" + extId + "'");
        return null;
    }

    public void waitForExtDialog(final String title)
    {
        waitForExtDialog(title, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForExtDialog(final String title, int timeout)
    {
        final Locator locator = Locator.xpath("//span["+Locator.NOT_HIDDEN + " and contains(@class, 'window-header-text') and contains(string(), '" + title + "')]");

        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            public boolean check()
            {
                return _test.isElementPresent(locator);
            }
        }, "Ext Dialog with title '" + title + "' did not appear after " + timeout + "ms", timeout);
    }

    public void waitForExtDialogToDisappear(String title)
    {
        waitForExtDialogToDisappear(title, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForExtDialogToDisappear(String title, int timeout)
    {
        final Locator locator = Locator.xpath("//span["+Locator.NOT_HIDDEN + " and contains(@class, 'window-header-text') and contains(string(), '" + title + "')]");

        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            public boolean check()
            {
                return !_test.isElementPresent(locator);
            }
        }, "Ext Dialog with title '" + title + "' was still present after " + timeout + "ms", timeout);
    }

    public String getExtMsgBoxText(String title)
    {
        return getExtMsgBoxText(title, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
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

    public String getExtDialogXPath(String windowTitle)
    {
        if (windowTitle == null) return "";
        String ext3Dialog = "//div[contains(@class, 'x-window') and " + Locator.NOT_HIDDEN + " and "+
            "./div/div/div/div/span[contains(@class, 'x-window-header-text') and contains(string(), '" + windowTitle + "')]]";
        String ext4Dialog = "//div[contains(@class, 'x4-window') and " + Locator.NOT_HIDDEN + " and "+
            "./div/div/div/div/div/span[contains(@class, 'x4-window-header-text') and contains(string(), '" + windowTitle + "')]]";
        if( _test.isElementPresent(Locator.xpath(ext3Dialog)) )
            return ext3Dialog;
        else if( _test.isElementPresent(Locator.xpath(ext4Dialog)) )
            return ext4Dialog;
        else
            Assert.fail("Unable to locate Ext dialog: '" + windowTitle + "'");
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

    public static Locator locateGridRowCheckbox(String rowTextContent)
    {
        return Locator.xpath("//div[contains(@class, 'x-grid3-row')]//td/div[text()='" + rowTextContent + "']//..//..//div[@class='x-grid3-row-checker']");
    }

    public static Locator locateBrowserFileName(String fileName)
    {
        return Locator.xpath("//div[contains(@class, 'x-grid3-row')]//td/div[text()='" + fileName + "']");
    }

    public static Locator locateExt3GridRow(int rowIndex, String parent)
    {
        String base = "//div[contains(@class, 'x-grid-panel')]";

        if(parent != null)
            base = parent + base;

        return Locator.xpath("(" + base + "//table[contains(@class, 'x-grid3-row-table')])[" + rowIndex + "]");
    }

    public static Locator locateExt3GridCell(Locator row, int cellIndex)
    {
        return Locator.xpath("(" + ((Locator.XPathLocator)row).getPath() + "//td[contains(@class, 'x-grid3-cell')])[" + cellIndex + "]");
    }

    public void clickFileBrowserFileCheckbox(String fileName)
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-filecontent-grid')]"), 60000);
        _test.waitForElement(locateBrowserFileName(fileName), 60000);
        _test.sleep(100); // Avoid race condition for file selection.
        selectExtGridItem("name", fileName, -1, "labkey-filecontent-grid", true);
    }

    public void clickXGridPanelCheckbox(int index, boolean keepExisting)
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'x-grid-panel')]"), 60000);
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

    public void selectExt4GridItem(String columnName, String columnVal, int idx, String markerCls, boolean keepExisting)
    {
        String script =
                "selectExt4GridItem = function (columnName, columnVal, idx, markerCls, keepExisting) {\n" +
                "    var el = Ext4.DomQuery.selectNode(\".\"+markerCls);\n" +
                "    if (el)\n" +
                "    {\n" +
                "        var grid = Ext4.getCmp(el.id);\n" +
                "        if (grid)\n" +
                "        {\n" +
                "            if (idx == -1)\n" +
                "                idx = grid.getStore().find(columnName, columnVal);\n" +

                "            if (idx == -1)\n" +
                "                throw 'Unable to locate ' + columnName + ': ' + columnVal;\n" +

                "            if (idx >= grid.getStore().getCount())\n" +
                "                throw 'No such row: ' + idx;\n" +

                "            grid.getSelectionModel().select(idx, keepExisting);\n" +
                "        }\n" +
                "    }\n" +
                "    else\n" +
                "    {\n" +
                "        throw 'Unable to locate grid: ' + markerCls;\n" +
                "    }\n" +
                "};" +
                "selectExt4GridItem(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]);";
        _test.executeScript(script, columnName, columnVal, idx, markerCls, keepExisting);
    }

    //TODO:  comment this
    public void clickX4GridPanelCheckbox(int index, String markerCls, boolean keepExisting)
    {
        clickX4GridPanelCheckbox(null, null, index, markerCls, keepExisting);
    }

    public void clickX4GridPanelCheckbox(String colName, String colValue, String markerCls, boolean keepExisting)
    {
        clickX4GridPanelCheckbox(colName, colValue, -1, markerCls, keepExisting);
    }

    protected void clickX4GridPanelCheckbox(String colName, String colValue, int rowIndex, String markerCls, boolean keepExisting)
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'x4-grid')]"), 60000);
        selectExt4GridItem(colName, colValue, rowIndex, markerCls, keepExisting);
    }

    //Pick measure from split panel measure picker
    public void pickMeasure(final String source, final String measure, boolean keepSelection)
    {
        _test._shortWait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".sourcepanel .x4-grid-row"))); // if one row is ready, all should be
        selectExt4GridItem("queryName", source, -1, "sourcegrid", keepSelection);
        //select measure
        _test._shortWait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".measuresgrid .x4-grid-row"))); // if one row is ready, all should be
        selectExt4GridItem("label", measure, -1, "measuresgrid", keepSelection);
    }

    public void pickMeasure(String source, String measure)
    {
        pickMeasure(source, measure, false);
    }

    //Pick measure from one of multiple split panel measure pickers
    public void pickMeasure(String panelCls, String source, String measure, boolean keepSelection)
    {
        _test._shortWait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("."+panelCls+" .sourcepanel .x4-grid-row"))); // if one row is ready, all should be
        selectExt4GridItem("queryName", source, -1, panelCls + " .sourcegrid", keepSelection);
        //select measure
        _test._shortWait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("."+panelCls+" .measuresgrid .x4-grid-row"))); // if one row is ready, all should be
        selectExt4GridItem("label", measure, -1, panelCls + " .measuresgrid", keepSelection);
    }

    public void pickMeasure(String panelCls, String source, String measure)
    {
        pickMeasure(panelCls, source, measure, false);
    }

    @Deprecated
    public void prevClickFileBrowserFileCheckbox(String fileName)
    {
        Locator file = locateGridRowCheckbox(fileName);

        _test.waitForElement(file, 60000);
        _test.mouseDown(file);
    }

    @LogMethod
    public void selectFileBrowserItem(String path)
    {
        _test.log("selectFileBrowserItem path: " + path);

        String[] parts = {};
        StringBuilder nodeId = new StringBuilder();
        if (path.startsWith("/"))
            path = path.substring(1);
        if (!path.equals(""))
        {
            parts = path.split("/");
            nodeId.append('/');
        }
        waitForFileGridReady();

        // expand root tree node
        _test.waitAndClick(Locator.xpath("//div[contains(@class, 'x-tree-node') and @*='/']"));
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'tree-selected') and @*='/']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        for (int i = 0; i < parts.length; i++)
        {
            nodeId.append(parts[i]).append('/');

            if (i == parts.length - 1 && !path.endsWith("/")) // Trailing '/' indicates directory
            {
                // select last item: click on tree node name
                clickFileBrowserFileCheckbox(parts[i]);
            }
            else
            {
                // expand tree node: click on expand/collapse icon
                _test.waitAndClick(Locator.xpath("//div[contains(@class, 'x-tree-node') and @*='" + nodeId + "']"));
                _test.waitForElement(Locator.xpath("//div[contains(@class, 'tree-selected') and @*='" + nodeId + "']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
            }
        }
    }

    public void selectFileBrowserRoot()
    {
        selectFileBrowserItem("/");
    }

    public void waitForImportDataEnabled()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-import-enabled')]"), 6 * BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForFileAdminEnabled()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-admin-enabled')]"), 6 * BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    /**
     * Used for directory refreshes or folder changes for the file webpart grid to be ready and initialized.
     */
    public void waitForFileGridReady()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-file-grid-initialized')]"), 6 * BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    public void selectAllFileBrowserFiles()
    {
        Locator file = Locator.xpath("//tr[@class='x-grid3-hd-row']//div[@class='x-grid3-hd-checker']");
        _test.waitForElement(file, 60000);
        _test.sleep(1000);
        _test.click(file);

        file = Locator.xpath("//tr[@class='x-grid3-hd-row']//div[@class='x-grid3-hd-inner x-grid3-hd-checker x-grid3-hd-checker-on']");
        _test.waitForElement(file, 60000);
    }

    public void selectComboBoxItem(Locator.XPathLocator parentLocator, String selection)
    {
        _test.click(Locator.xpath(parentLocator.getPath() + "//*[contains(@class, 'x-form-arrow-trigger')]"));
        _test.waitAndClick(Locator.xpath("//div["+Locator.NOT_HIDDEN+"]/div/div[text()='" + selection + "']"));
        _test.waitForElementToDisappear(Locator.xpath("//div["+Locator.NOT_HIDDEN+"]/div/div[text()='" + selection + "']"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void selectComboBoxItem(String label, String selection)
    {
        selectComboBoxItem(Locator.xpath("//div["+Locator.NOT_HIDDEN+" and ./label/span[text()='"+label+"']]/div/div"), selection);
    }

    public void selectExt4ComboBoxItem(Locator.XPathLocator parentLocator, String selection)
    {
        _test.click(Locator.xpath(parentLocator.getPath() + "//div[contains(@class, 'x4-form-arrow-trigger')]"));
        _test.waitAndClick(Locator.xpath("//li["+Locator.NOT_HIDDEN+" and contains(@class, 'x4-boundlist-item') and text()='" + selection + "']"));
        _test.mouseDown(Locator.xpath("/html/body"));
    }

    public void selectExt4ComboBoxItem(String label, String selection)
    {
        selectExt4ComboBoxItem(Locator.xpath("//tr["+Locator.NOT_HIDDEN+" and ./td/label[text()='"+label+"']]"), selection);
    }

    public void selectGWTComboBoxItem(Locator.XPathLocator parentLocator, String selection)
    {
        _test.click(Locator.xpath(parentLocator.getPath() + "//div[contains(@class, 'x-form-trigger-arrow')]"));

        _test.waitAndClick(Locator.css(".x-combo-list-item").containing(selection));
    }

    public void closeExtTab(String tabName)
    {
        _test.log("Closing Ext tab " + tabName);
        _test.mouseDownAt(Locator.xpath("//a[contains(@class, 'x-tab-strip-close') and ..//span[contains(@class, 'x-tab-strip-text') and text()='" + tabName + "']]"), 1, 1);
    }

    public void clickExtTab(String tabname)
    {
        _test.log("Selecting Ext tab " + tabname);
        Locator l = Locator.xpath("//span[contains(@class, 'x-tab-strip-text') and text() = '" + tabname + "']");
        _test.waitForElement(l, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        if(_test.getBrowser().startsWith(BaseWebDriverTest.IE_BROWSER))
        {
            _test.mouseDownAt(l,  1,1);
            _test.clickAt(l, "1,1");
        }
        else
        {
            _test.click(l);
        }
    }

    public void clickSideTab(String tab)
    {
        if (_test.isElementPresent(Locator.xpath("//a[contains(@class, 'x-grouptabs-text') and span[contains(text(), '" + tab + "')]]")))
            // Tab hasn't rendered yet
            _test.mouseDown(Locator.xpath("//a[contains(@class, 'x-grouptabs-text') and span[contains(text(), '" + tab + "')]]"));
        else
            // Tab has rendered
            _test.mouseDown(Locator.xpath("//ul[contains(@class, 'x-grouptabs-strip')]/li[a[contains(@class, 'x-grouptabs-text') and contains(text(), '" + tab + "')]]"));
    }

    public void clickExtTabContainingText(String tabText)
    {
        _test.log("Selecting Ext tab " + tabText);
        _test.click(Locator.xpath("//span[contains(@class, 'x-tab-strip-text') and contains( text(), '" + tabText + "')]"));
    }

    public void clickExtButton(String caption)
    {
        clickExtButton(caption, BaseWebDriverTest.WAIT_FOR_PAGE);
    }

    public void clickExtButton(String caption, int wait)
    {
        clickExtButton(null, caption, wait);
    }

    public void clickExtButton(String windowTitle, String caption)
    {
        clickExtButton(windowTitle, caption, BaseWebDriverTest.WAIT_FOR_PAGE);
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
        _test.waitForElement(loc, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.clickAndWait(loc, wait);
    }

    public void checkCheckbox(String label)
    {
        Locator checkbox = Locator.ext4Checkbox(label);
        if(!isChecked(label))
            _test.click(checkbox);
        if(!isChecked(label))
            Assert.fail("Failed to check checkbox '" + label + "'.");
    }

    public void uncheckCheckbox(String label)
    {
        Locator checkbox = Locator.ext4Checkbox(label);
        if(isChecked(label))
            _test.click(checkbox);
        if(isChecked(label))
            Assert.fail("Failed to uncheck checkbox '" + label + "'.");
    }

    public boolean isChecked(String label)
    {
        Locator checked = Locator.xpath("//table[contains(@class, 'x4-form-cb-checked')]//input[@type = 'button' and contains(@class, 'checkbox') and following-sibling::label[text()='" + label + "']]");
        return _test.isElementPresent(checked);
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
}
