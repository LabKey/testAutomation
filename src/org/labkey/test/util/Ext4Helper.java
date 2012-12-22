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

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: Jan 3, 2012
 * Time: 3:34:16 PM
 */
public class Ext4Helper extends AbstractHelper
{
    public Ext4Helper(BaseSeleniumWebTest test)
    {
        super(test);
    }

    @LogMethod
    public void selectComboBoxItem(Locator.XPathLocator parentLocator, String selection)
    {
        Locator l = Locator.xpath(parentLocator.getPath()+"//div[contains(@class,'arrow')]");
        _test.waitForElement(l);
        _test.clickAt(l, "1,1");
        if(_test.getBrowser().startsWith(BaseSeleniumWebTest.IE_BROWSER))
        {
            _test.sleep(500);
            _test.clickAt(Locator.xpath("//div/div/div[text()='" + selection + "']"), "1,1");
            _test.mouseDownAt(Locator.xpath("/html/body"), 1,1);
        }
        else
        {
            // wait for the dropdown to open
            Locator listItem =     Locator.xpath("//li[contains(@class, 'x4-boundlist-item') and contains( text(), '" + selection + "')]");
            _test.waitForElement(listItem);

            // select the list item
            _test.click(listItem);
            //test.mouseDown(Locator.xpath("/html/body"));
        }
    }

    public void selectComboBoxItem(String label, String selection)
    {
       Ext4FieldRef.getForLabel(_test, label).setValue(selection);
    }

    @LogMethod
    public void selectComboBoxItemById(String labelId, String selection)
    {
        Locator.XPathLocator loc = Locator.xpath("//tbody[./tr/td/label[@id='" + labelId + "-labelEl']]");
        selectComboBoxItem(loc, selection);
    }

    @LogMethod
    public void selectRadioButton(String label, String selection)
    {
        Locator l = Locator.xpath("//div[div/label[text()='" + label + "']]//label[text()='" + selection + "']");
        if (!_test.isElementPresent(l))
        {
            // try Ext 4.1.0 version
            l = Locator.xpath("//div[./table//label[text()='" + label + "']]//label[text()='" + selection + "']");
        }
        _test.click(l);
    }

    public void waitForComponentNotDirty(final String componentId)
    {

        _test.waitFor(new BaseSeleniumWebTest.Checker()
        {
            @Override
            public boolean check()
            {
                return !Boolean.getBoolean(_test.getWrapper().getEval("selenium.browserbot.getCurrentWindow().Ext4.getCmp('" + componentId + "').isDirty();"));
            }
        }, "Page still marked as dirty", BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public void selectRadioButtonById(String labelId)
    {
        Locator l = Locator.xpath("//label[@id='" + labelId + "']");
        _test.click(l);
    }

    public void clickExt4Tab(String tabname)
    {

        _test.log("Selecting Ext tab " + tabname);
        Locator l = Locator.xpath("//span[contains(@class, 'x4-tab') and text() = '" + tabname + "']");
        _test.waitForElement(l, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
        if(_test.getBrowser().startsWith(BaseSeleniumWebTest.IE_BROWSER))
        {
            _test.mouseDownAt(l,  1,1);
            _test.clickAt(l, "1,1");
        }
        else
        {
            _test.click(l);
        }
    }

    public void checkCheckbox(String label)
    {
        if (!isChecked(label))
        {
            Locator l = Locator.xpath("//table[contains(@class, 'x4-form-item')][.//label[text()='" + label + "']]//input[contains(@class,'x4-form-checkbox')]");
            _test.click(l);
        }
    }

    public void uncheckCheckbox(String label)
    {
        if (isChecked(label))
        {
            Locator l = Locator.xpath("//table[contains(@class, 'x4-form-cb-checked')][.//label[text()='" + label + "']]//input[contains(@class,'x4-form-checkbox')]");
            _test.click(l);
        }
    }

    public boolean isChecked(String label)
    {
        _test.assertElementPresent(Locator.xpath("//table[contains(@class, 'x4-form-item')][.//label[text()='" + label + "']]//input"));
        Locator l = Locator.xpath("//table[contains(@class, 'x4-form-cb-checked')][.//label[text()='" + label + "']]//input");
        return _test.isElementPresent(l);
    }

    /**
     * Check the checkbox for an Ext4 grid row
     * Currently used only for participant filter panel.
     * @param cellText Exact text from any cell in the desired row
     */
    public void checkGridRowCheckbox(String cellText)
    {
        checkGridRowCheckbox(cellText, 0);
    }

    /**
     * Check the checkbox for an Ext4 grid row
     * Currently used only for participant filter panel
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     */
    @LogMethod
    public void checkGridRowCheckbox(String cellText, int index)
    {
        Locator.XPathLocator rowLoc = getGridRow(cellText, index);
        if (!isChecked(rowLoc))
            _test.mouseDown(rowLoc.append("//div[contains(@class, 'x4-grid-row-checker')]"));
    }

    /**
     * Uncheck the checkbox for an Ext4 grid row
     * Currently used only for participant filter panel.
     * @param cellText Exact text from any cell in the desired row
     */
    public void uncheckGridRowCheckbox(String cellText)
    {
        uncheckGridRowCheckbox(cellText, 0);
    }

    /**
     * Uncheck the checkbox for an Ext4 grid row
     * Currently used only for participant filter panel
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     */
    @LogMethod
    public void uncheckGridRowCheckbox(String cellText, int index)
    {
        Locator.XPathLocator rowLoc = getGridRow(cellText, index);
        if (isChecked(rowLoc))
            _test.mouseDown(rowLoc.append("//div[contains(@class, 'x4-grid-row-checker')]"));
    }

    /**
     * Click the text of an Ext4 grid row
     * Currently used only for time chart measure picker
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     */
    @LogMethod
    public void clickGridRowText(String cellText, int index)
    {
        Locator.XPathLocator rowLoc = getGridRow(cellText, index);
        _test.mouseDown(rowLoc.append("//div[contains(@class, 'x4-grid-cell')][string() = '"+cellText+"']"));
    }

    /**
     * Click the text of an Participant filter panel grid row
     * Currently used only for participant filter panel
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     */
    public void clickParticipantFilterGridRowText(String cellText, int index)
    {
        Locator.XPathLocator rowLoc = getGridRow(cellText, index);
        _test.click(rowLoc.append("//span[contains(@class, 'lk-filter-panel-label')][string() = '"+cellText+"']"));
    }

    /**
     * Determines if the specified row has a checked checkbox
     * @param rowLoc Locator provided by {@link #getGridRow(String, int)}
     * @return true if the specified row has a checked checkbox
     */
    private boolean isChecked(Locator.XPathLocator rowLoc)
    {
        _test.assertElementPresent(rowLoc);
        return _test.isElementPresent(rowLoc.append("[contains(@class, 'x4-grid-row-selected')]"));
    }

    /**
     * Determines if the specified row has a checked checkbox
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     * @return true if the specified row has a checked checkbox
     */
    public boolean isChecked(String cellText, int index)
    {
        Locator.XPathLocator rowLoc = getGridRow(cellText, index);
        _test.assertElementPresent(rowLoc);
        return _test.isElementPresent(rowLoc.append("[contains(@class, 'x4-grid-row-selected')]"));
    }

    public <Type extends Ext4CmpRef> List<Type> componentQuery(String componentSelector, Class<Type> clazz)
    {
        componentSelector = componentSelector.replaceAll("'", "\"");  //escape single quotes
        String res = _test.getWrapper().getEval("selenium.ext4ComponentQuery('" + componentSelector + "')");
        return componentsFromJson(res, clazz);
    }

    public <Type extends Ext4CmpRef> Type queryOne(String componentSelector, Class<Type> clazz)
    {
        List<Type> cmpRefs = componentQuery(componentSelector, clazz);
        if (null == cmpRefs || cmpRefs.size() == 0)
            return null;

        return cmpRefs.get(0);
    }

    public <Type extends Ext4CmpRef> List<Type> componentsFromJson(String jsonArrayStr, Class<Type> clazz)
    {
        if (null == jsonArrayStr || "null".equals(jsonArrayStr))
            return null;

        try
        {
            JSONArray array = (JSONArray) JSONValue.parse(jsonArrayStr);
            List<Type> ret = new ArrayList<Type>(array.size());
            for (Object o : array)
            {
                Constructor<Type> constructor = clazz.getConstructor(String.class, BaseSeleniumWebTest.class);
                ret.add(constructor.newInstance(o.toString(), _test));
            }
            return ret;
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
        catch (InstantiationException e)
        {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    public BaseSeleniumWebTest.Checker getExt4SelectorChecker(final String selector)
    {
        return new BaseSeleniumWebTest.Checker()
        {
            @Override
            public boolean check()
            {
                return queryOne(selector, Ext4CmpRef.class) != null;
            }
        };
    }

    public void clickTabContainingText(String tabText)
    {
        _test.click(Locator.xpath("//span[contains(@class, 'x4-tab-inner') and contains( text(), '" + tabText + "')]"));
    }

    public void waitForMaskToDisappear()
    {
        waitForMaskToDisappear(BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForMaskToDisappear(int wait)
    {
        _test.waitForElementToDisappear(getExtMask(), wait);
    }

    public void waitForMask()
    {
        waitForMask(BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForMask(int wait)
    {
        _test.waitForElement(getExtMask(), wait);
    }

    private Locator getExtMask()
    {
        return Locator.xpath("//div["+Locator.NOT_HIDDEN+" and contains(@class, 'x4-mask')]");
    }

    /**
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     * @return XPathLocator for the desired row
     */
    private Locator.XPathLocator getGridRow(String cellText, int index)
    {
        return Locator.xpath("(//tr[contains(@class, 'x4-grid-row')][td[string() = '" + cellText + "']]["+Locator.NOT_HIDDEN+"])[" + (index + 1) + "]");
    }

    public static Locator.XPathLocator invalidField()
    {
        return Locator.xpath("//input[contains(@class, 'x4-form-field') and contains(@class, 'x4-form-invalid-field')]");
    }

    @LogMethod
    public static void clickExt4MenuButton(BaseSeleniumWebTest test, boolean wait, Locator menu, boolean onlyOpen, String ... subMenuLabels)
    {
        test.click(menu);
        for (int i = 0; i < subMenuLabels.length - 1; i++)
        {
            Locator parentLocator = ext4MenuItem(subMenuLabels[i]);
            test.waitForElement(parentLocator, 1000);
            test.mouseOver(parentLocator);
        }
        Locator itemLocator = ext4MenuItem(subMenuLabels[subMenuLabels.length - 1]);
        test.waitForElement(itemLocator, 1000);
        if (onlyOpen)
            return;
        if (wait)
            test.clickAndWait(itemLocator);
        else
            test.click(itemLocator);
    }

    public void clickExt4MenuItem(String text)
    {
        _test.click(ext4MenuItem(text));
    }

    public static Locator.XPathLocator ext4MenuItem(String text)
    {
        return Locator.xpath("//span[contains(@class, 'x4-menu-item-text') and text() = '" + text + "']");
    }

    public static Locator.XPathLocator ext4Window(String title)
    {
        return Locator.xpath("//div[contains(@class, 'x4-window-header')]//span[text() = '" + title + "']");
    }
}
