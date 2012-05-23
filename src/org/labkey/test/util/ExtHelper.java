/*
 * Copyright (c) 2009-2012 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

/**
 * User: klum
 * Date: Apr 6, 2009
 * Time: 5:21:53 PM
 */
public class ExtHelper
{
    /**
     * Clicks the Ext or labkey menu item from the submenu specified by the menu object's text
     */
    public static void clickMenuButton(BaseSeleniumWebTest test, boolean wait, String menusLabel, String ... subMenuLabels)
    {
        Locator menu = Locator.extButton(menusLabel);
        if (!test.isElementPresent(menu))
            menu = Locator.navButton(menusLabel);
        if (!test.isElementPresent(menu))
            BaseSeleniumWebTest.fail("No Ext or LabKey menu for label '" + menusLabel + "' found");
        clickExtMenuButton(test, wait, menu, subMenuLabels);
    }

    /**
     * Clicks the ext menu item from the submenu specified by the ext object's text
     */
    public static void clickExtMenuButton(BaseSeleniumWebTest test, boolean wait, Locator menu, String ... subMenuLabels)
    {
        test.click(menu);
        for (int i = 0; i < subMenuLabels.length - 1; i++)
        {
            Locator parentLocator = Locator.menuItem(subMenuLabels[i]);
            test.waitForElement(parentLocator, 1000);
            test.mouseOver(parentLocator);
        }
        Locator itemLocator = Locator.menuItem(subMenuLabels[subMenuLabels.length - 1]);
        test.waitForElement(itemLocator, 1000);
        if (wait)
            test.clickAndWait(itemLocator);
        else
            test.click(itemLocator);
    }

    public static void clickExtDropDownMenu(BaseSeleniumWebTest test, String menuId, String value)
    {
        clickExtDropDownMenu(test, Locator.id(menuId), value);
    }


    public static void clickExtDropDownMenu(BaseSeleniumWebTest test, Locator menuLocator, String value)
    {
        test.click(menuLocator);
        Locator element = Locator.xpath("//*[(self::li[contains(@class, 'x4-boundlist-item')] or self::div[contains(@class, 'x-combo-list-item')] or self::span[contains(@class, 'x-menu-item-text')]) and text()='" + value + "']");
        test.waitForElement(element, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        test.click(element);
    }

    public static void selectFolderManagementTreeItem(BaseSeleniumWebTest test, String path, boolean keepExisting)
    {
        test.getWrapper().getEval("selenium.selectFolderManagementItem('" + path + "', " + keepExisting +");");
    }
    
    public static void setQueryEditorValue(BaseSeleniumWebTest test, String id, String value)
    {
        String script = "selenium.setEditAreaValue(" + jsString(id) + ", " + jsString(value) + ");";
        test.getWrapper().getEval(script);
    }

    /**
     * Returns a DOM Element id from an ext object id. Assumes that the ext component
     * has already been rendered.
     */
    public static String getExtElementId(BaseSeleniumWebTest test, String extId)
    {
        for (int attempt = 0; attempt < 5; attempt++)
        {
            String id = test.getWrapper().getEval("selenium.getExtElementId('" + extId + "');");
            test.log("Element id for ext component id: " + extId + " is: " + id);
            if (id != null)
                return id;
            test.sleep(500);
        }

        test.fail("Failed to get element id for Ext component '" + extId + "'");
        return null;
    }

    public static void waitForExtDialog(final BaseSeleniumWebTest test, String title)
    {
        waitForExtDialog(test, title, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public static void waitForExtDialog(final BaseSeleniumWebTest test, String title, int timeout)
    {
        final Locator locator = Locator.xpath("//span["+Locator.NOT_HIDDEN + " and contains(@class, 'window-header-text') and contains(string(), '" + title + "')]");

        test.waitFor(new BaseSeleniumWebTest.Checker()
        {
            public boolean check()
            {
                return test.isElementPresent(locator);
            }
        }, "Ext Dialog with title '" + title + "' did not appear after " + timeout + "ms", timeout);
    }

    public static String getExtMsgBoxText(BaseSeleniumWebTest test, String title)
    {
        return getExtMsgBoxText(test, title, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public static String getExtMsgBoxText(BaseSeleniumWebTest test, String title, int timeout)
    {
        waitForExtDialog(test, title, timeout);
        Locator locator = Locator.xpath("//div[contains(@class, 'x-window-dlg')]//span[contains(@class, 'ext-mb-text')]");
        String msg = test.getText(locator);
        return msg;
    }

    public static void setExtFormElement(BaseSeleniumWebTest test, String text)
    {
        // Currently used only in RReportHelper.  Modify as needed to be more universal.
        test.setFormElement(Locator.xpath("//input[(contains(@class, 'ext-mb-input') or contains(@class, 'x-form-field')) and @type='text']"), text);
    }

    public static void setExtFormElementByType(BaseSeleniumWebTest test, String windowTitle, String inputType, String text)
    {
        test.setFormElement(Locator.xpath(getExtDialogXPath(test, windowTitle) + "//input[contains(@class, 'form-field') and @type='"+inputType+"']"), text);
    }

    public static void setExtFormElementByLabel(BaseSeleniumWebTest test, String label, String text)
    {
        setExtFormElementByLabel(test, null, label, text);
    }

    public static void setExtFormElementByLabel(BaseSeleniumWebTest test, String windowTitle, String label, String text)
    {
        Locator formElement = Locator.xpath(getExtDialogXPath(test, windowTitle) + "//div[./label/span[text()='"+label+"']]/div/*[self::input or self::textarea]");
        if (!test.isElementPresent(formElement))
        {
            // try the ext4 version
            formElement = Locator.xpath(getExtDialogXPath(test, windowTitle) + "//div[./label[text()='"+label+"']]/div/*[self::input or self::textarea]");
        }
        test.setFormElement(formElement, text);
        test.fireEvent(formElement, BaseSeleniumWebTest.SeleniumEvent.blur);
    }

    public static String getExtFormElementByLabel(BaseSeleniumWebTest test, String label)
    {
        return getExtFormElementByLabel(test, null, label);
    }

    public static String getExtFormElementByLabel(BaseSeleniumWebTest test, String windowTitle, String label)
    {
        Locator formElement = Locator.xpath(getExtDialogXPath(test, windowTitle) + "//div[./label/span[text()='"+label+"']]/div/*[self::input or self::textarea]");
        if (!test.isElementPresent(formElement))
        {
            // try the ext4 version
            formElement = Locator.xpath(getExtDialogXPath(test, windowTitle) + "//div[./label[text()='"+label+"']]/div/*[self::input or self::textarea]");
        }
        return test.getFormElement(formElement);
    }

    @Deprecated
    public static String getExtDialogXPath(String windowTitle)
    {
        return "//div[contains(@class, 'x-window') and " + Locator.NOT_HIDDEN + " and "+
            "./div/div/div/div/span[contains(@class, 'x-window-header-text') and contains(string(), '" + windowTitle + "')]]";
    }

    public static String getExtDialogXPath(BaseSeleniumWebTest test, String windowTitle)
    {
        if (windowTitle == null) return "";
        String ext3Dialog = "//div[contains(@class, 'x-window') and " + Locator.NOT_HIDDEN + " and "+
            "./div/div/div/div/span[contains(@class, 'x-window-header-text') and contains(string(), '" + windowTitle + "')]]";
        String ext4Dialog = "//div[contains(@class, 'x4-window') and @role='dialog' and " + Locator.NOT_HIDDEN + " and "+
            "./div/div/div/div/span[contains(@class, 'x4-window-header-text') and contains(string(), '" + windowTitle + "')]]";
        if( test.isElementPresent(Locator.xpath(ext3Dialog)) )
            return ext3Dialog;
        else if( test.isElementPresent(Locator.xpath(ext4Dialog)) )
            return ext4Dialog;
        else
            BaseSeleniumWebTest.fail("Unable to locate Ext dialog: '" + windowTitle + "'");
        return null; // unreachable
    }

    public static void waitForLoadingMaskToDisappear(BaseSeleniumWebTest test, int wait)
    {
        test.waitForElementToDisappear(Locator.xpath("//div[contains(@class, 'x-mask-loading')]"), wait);
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

    public static void clickFileBrowserFileCheckbox(BaseSeleniumWebTest test, String fileName)
    {
        test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-filecontent-grid')]"), 60000);
        test.waitForElement(locateBrowserFileName(fileName), 60000);
        test.getWrapper().getEval("selenium.selectFileBrowserCheckbox('" + fileName + "');");
    }

    public static void clickXGridPanelCheckbox(BaseSeleniumWebTest test, int index, boolean keepExisting)
    {
        test.waitForElement(Locator.xpath("//div[contains(@class, 'x-grid-panel')]"), 60000);
        test.getWrapper().getEval("selenium.selectExtGridItem(null, null, " + index + ", 'x-grid-panel', " + keepExisting + ")");
    }

    public static void clickX4GridPanelCheckbox(BaseSeleniumWebTest test, int index, String markerCls, boolean keepExisting)
    {
        clickX4GridPanelCheckbox(test, null, null, index, markerCls, keepExisting);
    }

    public static void clickX4GridPanelCheckbox(BaseSeleniumWebTest test, String colName, String colValue, String markerCls, boolean keepExisting)
    {
        clickX4GridPanelCheckbox(test, colName, colValue, -1, markerCls, keepExisting);
    }

    protected static void clickX4GridPanelCheckbox(BaseSeleniumWebTest test, String colName, String colValue, int rowIndex, String markerCls, boolean keepExisting)
    {
        test.waitForElement(Locator.xpath("//div[contains(@class, 'x4-grid')]"), 60000);
        test.getWrapper().getEval("selenium.selectExt4GridItem('" + colName + "', '" + colValue + "', " + rowIndex + ", '" + markerCls + "', " + keepExisting + ")");
    }

    @Deprecated
    public static void prevClickFileBrowserFileCheckbox(BaseSeleniumWebTest test, String fileName)
    {
        Locator file = locateGridRowCheckbox(fileName);

        test.waitForElement(file, 60000);
        test.mouseDown(file);
    }

    /**
     * Select a <b>single</b> row in the file browser by clicking on the file name.
     * Use {@link ExtHelper#clickFileBrowserFileCheckbox(BaseSeleniumWebTest, String)} to click the checkbox for multi-select.
     */
    @Deprecated
    public static void selectFileBrowserFile(BaseSeleniumWebTest test, String fileName)
    {
        Locator file = locateBrowserFileName(fileName);

        test.waitForElement(file, 60000);
        test.mouseDown(file);
    }

    public static void selectFileBrowserItem(BaseSeleniumWebTest test, String path)
    {
        test.log("selectFileBrowserItem path: " + path);
        if (path.startsWith("/"))
            path = path.substring(1);
        String[] parts = path.split("/");
        StringBuilder nodeId = new StringBuilder();
        nodeId.append('/');
        
        waitForFileGridReady(test);
        for (int i = 0; i < parts.length; i++)
        {
            nodeId.append(parts[i]).append('/');

            if (i == parts.length - 1 && !path.endsWith("/")) // Trailing '/' indicates directory 
            {
                // select last item: click on tree node name
                clickFileBrowserFileCheckbox(test, parts[i]);
            }
            else
            {
                // expand tree node: click on expand/collapse icon
                test.waitAndClick(Locator.xpath("//div[contains(@class, 'x-tree-node') and @*='" + nodeId + "']"));
                test.waitForElement(Locator.xpath("//div[contains(@class, 'tree-selected') and @*='" + nodeId + "']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
            }
        }
    }

    public static void selectFileBrowserRoot(BaseSeleniumWebTest test)
    {
        test.waitAndClick(Locator.xpath("//div[contains(@class, 'x-tree-node') and @*='/']"));
        test.waitForElement(Locator.xpath("//div[contains(@class, 'tree-selected') and @*='/']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public static void waitForImportDataEnabled(BaseSeleniumWebTest test)
    {
        test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-import-enabled')]"), 6 * BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    /**
     * Used for directory refreshes or folder changes for the file webpart grid to be ready and initialized.
     * @param test
     */
    public static void waitForFileGridReady(BaseSeleniumWebTest test)
    {
        test.waitForElement(Locator.xpath("//div[contains(@class, 'labkey-file-grid-initialized')]"), 6 * test.WAIT_FOR_JAVASCRIPT);
    }

    public static void selectAllFileBrowserFiles(BaseSeleniumWebTest test)
    {
        Locator file = Locator.xpath("//tr[@class='x-grid3-hd-row']//div[@class='x-grid3-hd-checker']");
        test.waitForElement(file, 60000);
        test.sleep(1000);
        test.mouseClick(file.toString());

        file = Locator.xpath("//tr[@class='x-grid3-hd-row']//div[@class='x-grid3-hd-inner x-grid3-hd-checker x-grid3-hd-checker-on']");
        test.waitForElement(file, 60000);
    }

    public static void selectComboBoxItemExt4(BaseSeleniumWebTest test, String label, String selection)
    {

        selectComboBoxItemExt4(test, Locator.xpath("//div[./label[text()='" + label + "']]/div/div"), selection);
    }

    private static void selectComboBoxItemExt4(BaseSeleniumWebTest test, Locator.XPathLocator parentLocator, String selection)
    {
        test.clickAt(Locator.xpath(parentLocator.getPath() + "//div[contains(@class, 'arrow')]"), "1,1");
        if(test.getBrowser().startsWith(test.IE_BROWSER))      //TODO:  haven't tried this out yet
        {
            test.sleep(500);
            test.clickAt(Locator.xpath("//div/div/div[text()='" + selection + "']"), "1,1");
            test.mouseDownAt(Locator.xpath("/html/body"), 1,1);
        }
        else
        {
            Locator selector = Locator.xpath("//div[./div[@role='presentation']]/div/div/ul/li[text()='" + selection + "']");
            test.waitForElement(selector, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
            test.click(selector);
        }
    }

    public static void selectComboBoxItem(BaseSeleniumWebTest test,  Locator.XPathLocator parentLocator, String selection)
    {
        test.clickAt(Locator.xpath(parentLocator.getPath() + "//img[contains(@class, 'x-form-arrow-trigger')]"), "1,1");
        if(test.getBrowser().startsWith(test.IE_BROWSER))
        {
            test.sleep(500);
            test.clickAt(Locator.xpath("//div/div/div[text()='" + selection + "']"), "1,1");
            test.mouseDownAt(Locator.xpath("/html/body"), 1,1);
        }
        else
        {
            test.waitAndClick(Locator.xpath("//div["+Locator.NOT_HIDDEN+"]/div/div[text()='" + selection + "']"));
            test.mouseDown(Locator.xpath("/html/body"));
        }
    }

    public static void selectComboBoxItem(BaseSeleniumWebTest test, String label, String selection)
    {
        selectComboBoxItem(test, Locator.xpath("//div["+Locator.NOT_HIDDEN+" and ./label/span[text()='"+label+":']]/div/div"), selection);
    }

    public static void selectGWTComboBoxItem(BaseSeleniumWebTest test, Locator.XPathLocator parentLocator, String selection)
    {
        test.click(Locator.xpath(parentLocator.getPath() + "//div[contains(@class, 'x-form-trigger-arrow')]"));
        test.waitForElement(Locator.xpath("//div[contains(@style, 'visibility: visible')]/div/div[text()='" + selection + "']"), BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);

        Locator option = Locator.xpath("//div[contains(@style, 'visibility: visible')]/div/div[text()='" + selection + "']");

        test.mouseDown(option);
        test.mouseDown(Locator.xpath("/html/body"));
    }

    public static void closeExtTab(BaseSeleniumWebTest test, String tabName)
    {
        test.log("Closing Ext tab " + tabName);
        test.mouseDownAt(Locator.xpath("//a[contains(@class, 'x-tab-strip-close') and ..//span[contains(@class, 'x-tab-strip-text') and text()='" + tabName + "']]"), 1, 1);
    }

    public static void clickExtTab(BaseSeleniumWebTest test, String tabname)
    {
        test.log("Selecting Ext tab " + tabname);
        Locator l = Locator.xpath("//span[contains(@class, 'x-tab-strip-text') and text() = '" + tabname + "']");
        if(test.getBrowser().startsWith(BaseSeleniumWebTest.IE_BROWSER))
        {
            test.mouseDownAt(l,  1,1);
            test.clickAt(l, "1,1");
        }
        else
        {
            test.click(l);
        }
    }

    public static void clickSideTab(BaseSeleniumWebTest test, String tab)
    {
        if (test.isElementPresent(Locator.xpath("//a[contains(@class, 'x-grouptabs-text') and span[contains(text(), '" + tab + "')]]")))
            // Tab hasn't rendered yet
            test.mouseDown(Locator.xpath("//a[contains(@class, 'x-grouptabs-text') and span[contains(text(), '" + tab + "')]]"));
        else
            // Tab has rendered
            test.mouseDown(Locator.xpath("//ul[contains(@class, 'x-grouptabs-strip')]/li[a[contains(@class, 'x-grouptabs-text') and contains(text(), '" + tab + "')]]"));
    }

    public static void clickExtTabContainingText(BaseSeleniumWebTest test, String tabText)
    {
        test.log("Selecting Ext tab " + tabText);
        test.click(Locator.xpath("//span[contains(@class, 'x-tab-strip-text') and contains( text(), '" + tabText + "')]"));
    }

    public static void clickExtButton(BaseSeleniumWebTest test, String caption)
    {
        clickExtButton(test, caption, BaseSeleniumWebTest.WAIT_FOR_PAGE);
    }

    public static void clickExtButton(BaseSeleniumWebTest test, String caption, int wait)
    {
        clickExtButton(test, null, caption, wait);
    }

    public static void clickExtButton(BaseSeleniumWebTest test, String windowTitle, String caption)
    {
        clickExtButton(test, windowTitle, caption, BaseSeleniumWebTest.WAIT_FOR_PAGE);
    }

     public static void clickExtButton(BaseSeleniumWebTest test, String windowTitle, String caption, int wait)
     {
         clickExtButton(test, windowTitle, caption, wait, 1);
     }

    /**
     * click an ext button
     *
     * @param test  the calling test
     * @param windowTitle title of the extWindow, or null if you would like to autodetect
     * @param caption the button text
     * @param wait time to wait for page to load
     * @param index
     */
    public static void clickExtButton(BaseSeleniumWebTest test, String windowTitle, String caption, int wait, int index)
    {
        test.log("Clicking Ext button with caption: " + caption);
        Locator loc = Locator.xpath((windowTitle!=null?getExtDialogXPath(test, windowTitle):"")+"//button[(contains(@class, 'x-btn-text') and text()='" + caption + "') or (@role='button' and ./span[text()='" + caption + "'])]["+index+"]");
        test.waitForElement(loc, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        if (wait > 0)
            test.clickAndWait(loc, wait);
        else
            test.click(loc);
    }

    public static void checkCheckbox(BaseSeleniumWebTest test, String label)
    {
        Locator checkbox = Locator.ext4Checkbox(label);
        if(!isChecked(test, label))
            test.click(checkbox);
        if(!isChecked(test, label))
            BaseSeleniumWebTest.fail("Failed to check checkbox '" + label + "'.");
    }

    public static void uncheckCheckbox(BaseSeleniumWebTest test, String label)
    {
        Locator checkbox = Locator.ext4Checkbox(label);
        if(isChecked(test, label))
            test.click(checkbox);
        if(isChecked(test, label))
            BaseSeleniumWebTest.fail("Failed to uncheck checkbox '" + label + "'.");
    }

    private static boolean isChecked(BaseSeleniumWebTest test, String label)
    {
        String checked = test.getAttribute(Locator.ext4Checkbox(label), "aria-checked");
        return checked.equalsIgnoreCase("true");
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
