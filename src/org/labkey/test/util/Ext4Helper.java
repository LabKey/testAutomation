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
public class Ext4Helper
{

    public static void selectComboBoxItem(BaseSeleniumWebTest test, Locator.XPathLocator parentLocator, String selection)
    {
        Locator l = Locator.xpath("//tbody[" + parentLocator.getPath()+"]/tr/td/div[contains(@class,'arrow')]");
        test.waitForElement(l);
        test.clickAt(l,  "1,1");
        if(test.getBrowser().startsWith(test.IE_BROWSER))
        {
            test.sleep(500);
            test.clickAt(Locator.xpath("//div/div/div[text()='" + selection + "']"), "1,1");
            test.mouseDownAt(Locator.xpath("/html/body"), 1,1);
        }
        else
        {
            // wait for the dropdown to open
            Locator listItem =     Locator.xpath("//li[contains(@class, 'x4-boundlist-item') and contains( text(), '" + selection + "')]");
            test.waitForElement(listItem);

            // select the list item
            test.click(listItem);
            //test.mouseDown(Locator.xpath("/html/body"));
        }
    }

    public static void selectComboBoxItem(BaseSeleniumWebTest test, String label, String selection)
    {
       Ext4FieldRef.getForLabel(test, label).setValue(selection);
    }

    public static void selectComboBoxItemById(BaseSeleniumWebTest test, String labelId, String selection)
    {
        Locator.XPathLocator loc = Locator.xpath("//div[./label[@id='" + labelId + "']]");
        if (!test.isElementPresent(loc))
        {
            // try Ext 4.1.0 version
            loc = Locator.xpath("//table[./tbody/tr/td/label[@id='" + labelId + "']]");
        }

        selectComboBoxItem(test, loc, selection);
    }

    public static void selectRadioButton(BaseSeleniumWebTest test, String label, String selection)
    {
        Locator l = Locator.xpath("//div[div/label[text()='" + label + "']]//label[text()='" + selection + "']");
        if (!test.isElementPresent(l))
        {
            // try Ext 4.1.0 version
            l = Locator.xpath("//div[./table//label[text()='" + label + "']]//label[text()='" + selection + "']");
        }
        test.click(l);
    }

    public static void selectRadioButtonById(BaseSeleniumWebTest test, String labelId)
    {
        Locator l = Locator.xpath("//label[@id='" + labelId + "']");
        test.click(l);
    }

    public static <Type extends Ext4CmpRef> List<Type> componentQuery(BaseSeleniumWebTest test, String componentSelector, Class<Type> clazz)
    {
        componentSelector = componentSelector.replaceAll("'", "\"");  //escape single quotes
        String res = test.getWrapper().getEval("selenium.ext4ComponentQuery('" + componentSelector + "')");
        return componentsFromJson(test, res, clazz);
    }

    public static <Type extends Ext4CmpRef> Type queryOne(BaseSeleniumWebTest test, String componentSelector, Class<Type> clazz)
    {
        List<Type> cmpRefs = componentQuery(test, componentSelector, clazz);
        if (null == cmpRefs || cmpRefs.size() == 0)
            return null;

        return cmpRefs.get(0);
    }

    public static <Type extends Ext4CmpRef> List<Type> componentsFromJson(BaseSeleniumWebTest test, String jsonArrayStr, Class<Type> clazz)
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
                ret.add(constructor.newInstance(o.toString(), test));
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

    public static class Ext4SelectorChecker implements BaseSeleniumWebTest.Checker
    {
        private BaseSeleniumWebTest _test;
        private String _selector;

        public Ext4SelectorChecker(BaseSeleniumWebTest test, String selector)
        {
            this._selector = selector;
            this._test = test;
        }

        @Override
        public boolean check()
        {
            return queryOne(_test, _selector, Ext4CmpRef.class) != null;
        }
    }

    public static void clickTabContainingText(BaseSeleniumWebTest test, String tabText)
    {
        test.click(Locator.xpath("//span[contains(@class, 'x4-tab-inner') and contains( text(), '" + tabText + "')]"));
    }

    public static void waitForMaskToDisappear(BaseSeleniumWebTest test)
    {
        waitForMaskToDisappear(test, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public static void waitForMaskToDisappear(BaseSeleniumWebTest test, int wait)
    {
        test.waitForElementToDisappear(getExtMask(), wait);
    }

    public static void waitForMask(BaseSeleniumWebTest test)
    {
        waitForMask(test, BaseSeleniumWebTest.WAIT_FOR_JAVASCRIPT);
    }

    public static void waitForMask(BaseSeleniumWebTest test, int wait)
    {
        test.waitForElement(getExtMask(), wait);
    }

    private static Locator getExtMask()
    {
        return Locator.xpath("//div[contains(@class, 'ext4-mask')]");
    }

    public static Locator invalidField()
    {
        return Locator.xpath("//input[contains(@class, 'x4-form-field') and contains(@class, 'x4-form-invalid-field')]");
    }

    public static void clickExt4MenuItem(BaseSeleniumWebTest test, String text)
    {
        test.click(ext4MenuItem(text));
    }

    public static Locator ext4MenuItem(String text)
    {
        return Locator.xpath("//span[contains(@class, 'x4-menu-item-text') and text() = '" + text + "']");
    }

    public static Locator ext4Window(String title)
    {
        return Locator.xpath("//div[contains(@class, 'x4-window-header')]//span[text() = '" + title + "']");
    }
}
