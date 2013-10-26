/*
 * Copyright (c) 2009-2013 LabKey Corporation
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
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

/**
 * User: klum
 * Date: Apr 6, 2009
 * Time: 5:21:53 PM
 */
public class ExtHelper extends AbstractHelper
{
    public ExtHelper(BaseSeleniumWebTest test)
    {
        super(test);
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
        _test.mouseOver(menu);
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

    public void clickExtDropDownMenu(String menuId, String value)
    {
        clickExtDropDownMenu(Locator.id(menuId), value);
    }


    public void clickExtDropDownMenu(Locator menuLocator, String value)
    {
        _test.click(menuLocator);
        Locator element = Locator.xpath("//*[(self::li[contains(@class, 'x4-boundlist-item')] or self::div[contains(@class, 'x-combo-list-item')] or self::span[contains(@class, 'x-menu-item-text')]) and text()='" + value + "']");
        _test.waitForElement(element, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.click(element);
    }

    public void selectFolderManagementTreeItem(String path, boolean keepExisting)
    {
        _test.getWrapper().getEval("selenium.selectFolderManagementItem('" + path + "', " + keepExisting +");");
    }

    public void setCodeEditorValue(String id, String value)
    {
        String script = "selenium.setCodeMirrorValue(" + jsString(id) + ", " + jsString(value) + ");";
        _test.getWrapper().getEval(script);
    }

    public String getCodeEditorValue(String id)
    {
        String script = "selenium.getCodeMirrorValue(" + jsString(id) + ");";
        return _test.getWrapper().getEval(script);
    }

    /**
     * Returns a DOM Element id from an ext object id. Assumes that the ext component
     * has already been rendered.
     */
    public String getExtElementId(String extId)
    {
        for (int attempt = 0; attempt < 5; attempt++)
        {
            String id = _test.getWrapper().getEval("selenium.getExtElementId('" + extId + "');");
            _test.log("Element id for ext component id: " + extId + " is: " + id);
            if (id != null)
                return id;
            _test.sleep(500);
        }

        Assert.fail("Failed to get element id for Ext component '" + extId + "'");
        return null;
    }

    public void waitForExtDialog(String title)
    {
        waitForExtDialog(title, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForExtDialog(final String title, int timeout)
    {
        _test.waitFor(new BaseSeleniumWebTest.Checker()
        {
            public boolean check()
            {
                return _test.isElementPresent(ExtHelperWD.Locators.extDialog(title));
            }
        }, "Ext Dialog with title '" + title + "' did not appear after " + timeout + "ms", timeout);
    }

    public void waitForExtDialogToDisappear(String title)
    {
        waitForExtDialogToDisappear(title, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForExtDialogToDisappear(final String title, int timeout)
    {
        _test.waitFor(new BaseSeleniumWebTest.Checker()
        {
            public boolean check()
            {
                return !_test.isElementPresent(ExtHelperWD.Locators.extDialog(title));
            }
        }, "Ext Dialog with title '" + title + "' was still present after " + timeout + "ms", timeout);
    }

    public String getExtMsgBoxText(String title)
    {
        return getExtMsgBoxText(title, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
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
        _test.fireEvent(formElement, BaseSeleniumWebTest.SeleniumEvent.blur);
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

    @LogMethod(quiet = true)
    public void clickFileBrowserFileCheckbox(@LoggedParam String fileName)
    {
        waitForFileGridReady();
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-filecontent-grid')]"), 60000);
        _test.waitForElement(locateBrowserFileName(fileName), 60000);
        Boolean wasChecked = _test.isElementPresent(Locator.xpath("//div").withClass("x-grid3-row-selected").append("/table/tbody/tr/td/div").withText(fileName));
        _test.getWrapper().getEval("selenium.selectFileBrowserCheckbox('" + fileName + "');");
        if (wasChecked)
            _test.waitForElementToDisappear(Locator.xpath("//div").withClass("x-grid3-row-selected").append("/table/tbody/tr/td/div").withText(fileName));
        else
            _test.waitForElement(Locator.xpath("//div").withClass("x-grid3-row-selected").append("/table/tbody/tr/td/div").withText(fileName));
    }

    public void clickXGridPanelCheckbox(int index, boolean keepExisting)
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'x-grid-panel')]"), 60000);
        _test.getWrapper().getEval("selenium.selectExtGridItem(null, null, " + index + ", 'x-grid-panel', " + keepExisting + ")");
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
        _test.getWrapper().getEval("selenium.selectExt4GridItem('" + colName + "', '" + colValue + "', " + rowIndex + ", '" + markerCls + "', " + keepExisting + ")");
    }

    //Pick measure from split panel measure picker
    public void pickMeasure(String source, String measure, boolean keepSelection)
    {
        _test.waitForElement(Locator.css(".sourcepanel tr:contains('" + source + "')"));
        _test.mouseDown(Locator.css(".sourcepanel tr:contains('" + source + "')"));
        if(!keepSelection)
        {
            _test.waitForElement(Locator.css(".measurepanel tr:contains('" + measure + "')"));
            _test.mouseDown(Locator.css(".measurepanel tr:contains('" + measure + "')"));
        }
        else
        {
            _test.waitForElement(Locator.css(".measurepanel tr:contains('" + measure + "')"));
            _test.mouseDown(Locator.css(".measurepanel tr:contains('" + measure + "') div.x4-grid-row-checker"));
        }
    }

    public void pickMeasure(String source, String measure)
    {
        pickMeasure(source, measure, false);
    }

    //Pick measure from one of multiple split panel measure pickers
    public void pickMeasure(String panelCls, String source, String measure, boolean keepSelection)
    {
        _test.waitForElement(Locator.css("." + panelCls + " .sourcepanel tr:contains('" + source + "')"));
        _test.mouseDown(Locator.css("." + panelCls + " .sourcepanel tr:contains('" + source + "')"));
        if(!keepSelection)
        {
            _test.waitForElement(Locator.css("." + panelCls + " .measurepanel tr:contains('" + measure + "')"));
            _test.mouseDown(Locator.css("." + panelCls + " .measurepanel tr:contains('" + measure + "')"));
        }
        else
        {
            _test.waitForElement(Locator.css("." + panelCls + " .measurepanel tr:contains('" + measure + "')"));
            _test.mouseDown(Locator.css("." + panelCls + " .measurepanel tr:contains('" + measure + "') div.x4-grid-row-checker"));
        }
    }

    public void pickMeasure(String panelCls, String source, String measure)
    {
        pickMeasure(panelCls, source, measure, false);
    }

    @LogMethod(quiet = true)
    public void selectFileBrowserItem(@LoggedParam String path)
    {
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
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'tree-selected') and @*='/']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        for (int i = 0; i < parts.length; i++)
        {
            waitForLoadingMaskToDisappear(BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
            waitForFileGridReady();

            nodeId.append(parts[i]).append('/');

            if (i == parts.length - 1 && !path.endsWith("/")) // Trailing '/' indicates directory
            {
                //it sometimes takes time to populate the list, this should fix that problem
                _test.waitForElement(Locator.tagWithText("div", parts[i]));
                // select last item: click on tree node name

                clickFileBrowserFileCheckbox(parts[i]);
            }
            else
            {
                // expand tree node: click on expand/collapse icon
                _test.waitAndClick(Locator.xpath("//div[contains(@class, 'x-tree-node') and @*='" + nodeId + "']"));
                _test.waitForElement(Locator.xpath("//div[contains(@class, 'tree-selected') and @*='" + nodeId + "']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
            }
        }
    }

    public void selectFileBrowserRoot()
    {
        selectFileBrowserItem("/");
    }

    public void waitForImportDataEnabled()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-import-enabled')]"), 6 * BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForFileAdminEnabled()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-admin-enabled')]"), 6 * BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    /**
     * Used for directory refreshes or folder changes for the file webpart grid to be ready and initialized.
     */
    public void waitForFileGridReady()
    {
        _test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-file-grid-initialized')]"), 6 * BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod
    public void selectAllFileBrowserFiles()
    {
        Locator file = Locator.xpath("//tr[@class='x-grid3-hd-row']//div[@class='x-grid3-hd-checker']");
        _test.waitForElement(file, 60000);
        _test.sleep(1000);
        _test.mouseClick(file.toString());

        file = Locator.xpath("//tr[@class='x-grid3-hd-row']//div[@class='x-grid3-hd-inner x-grid3-hd-checker x-grid3-hd-checker-on']");
        _test.waitForElement(file, 60000);
    }

    public void selectComboBoxItem(Locator.XPathLocator parentLocator, String selection)
    {
        _test.clickAt(Locator.xpath(parentLocator.getPath() + "//img[contains(@class, 'x-form-arrow-trigger')]"), "1,1");
        _test.waitAndClick(Locator.xpath("//div["+Locator.NOT_HIDDEN+"]/div/div[text()='" + selection + "']"));
        _test.mouseDown(Locator.xpath("/html/body"));
    }

    public void selectComboBoxItem(String label, String selection)
    {
        selectComboBoxItem(Locator.xpath("//div["+Locator.NOT_HIDDEN+" and ./label/span[text()='"+label+"']]/div/div"), selection);
    }

    public void selectExt4ComboBoxItem(Locator.XPathLocator parentLocator, String selection)
    {
        _test.clickAt(Locator.xpath(parentLocator.getPath() + "//div[contains(@class, 'x4-form-arrow-trigger')]"), "1,1");
        _test.waitAndClick(Locator.xpath("//li["+Locator.NOT_HIDDEN+" and contains(@class, 'x4-boundlist-item') and normalize-space()='" + selection + "']"));
        _test.mouseDown(Locator.xpath("/html/body"));
    }

    public void selectExt4ComboBoxItem(String label, String selection)
    {
        selectExt4ComboBoxItem(Locator.xpath("//tr["+Locator.NOT_HIDDEN+" and ./td/label[text()='"+label+"']]"), selection);
    }

    public void selectGWTComboBoxItem(Locator.XPathLocator parentLocator, String selection)
    {
        _test.click(Locator.xpath(parentLocator.getPath() + "//div[contains(@class, 'x-form-trigger-arrow')]"));
        _test.waitForElement(Locator.xpath("//div[contains(@style, 'visibility: visible')]/div/div[text()='" + selection + "']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        Locator option = Locator.xpath("//div[contains(@style, 'visibility: visible')]/div/div[text()='" + selection + "']");

        _test.mouseDown(option);
        _test.mouseDown(Locator.xpath("/html/body"));
    }

    public void closeExtTab(String tabName)
    {
        _test.log("Closing Ext tab " + tabName);
        _test.mouseDown(Locator.xpath("//a[contains(@class, 'x-tab-strip-close') and ..//span[contains(@class, 'x-tab-strip-text') and text()='" + tabName + "']]"));
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
        clickExtButton(caption, BaseSeleniumWebTest.WAIT_FOR_PAGE);
    }

    public void clickExtButton(String caption, int wait)
    {
        clickExtButton(null, caption, wait);
    }

    public void clickExtButton(String windowTitle, String caption)
    {
        clickExtButton(windowTitle, caption, BaseSeleniumWebTest.WAIT_FOR_PAGE);
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
     * @param index
     */
    public void clickExtButton(String windowTitle, String caption, int wait, int index)
    {
        _test.log("Clicking Ext button with caption: " + caption);
        Locator loc = Locator.xpath((windowTitle!=null?getExtDialogXPath(windowTitle):"")+"//button[(contains(@class, 'x-btn-text') and text()='" + caption + "') or (@role='button' and ./span[text()='" + caption + "'])]["+index+"]");
        _test.waitForElement(loc, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        _test.mouseDown(loc);
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
