/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
import org.labkey.test.util.ext4cmp.Ext4CmpRefWD;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: klum
 * Date: Jan 3, 2012
 * Time: 3:34:16 PM
 */
public class Ext4HelperWD extends AbstractHelperWD
{
    public Ext4HelperWD(BaseWebDriverTest test)
    {
        super(test);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(Locator.XPathLocator comboBox, @LoggedParam String selection)
    {
        selectComboBoxItem(comboBox, selection, false);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(Locator.XPathLocator comboBox, @LoggedParam String selection, boolean containsText)
    {
        Locator arrowTrigger = Locator.xpath(comboBox.getPath()+"//div[contains(@class,'arrow')]");
        _test.waitAndClick(arrowTrigger);
        Locator.XPathLocator listItem;
        if (containsText)
            listItem = Locator.xpath("//*[contains(@class, 'x4-boundlist-item')]").notHidden().containing(selection);
        else
            listItem = Locator.xpath("//*[contains(@class, 'x4-boundlist-item')]").notHidden().withText(selection);

        // wait for and select the list item
        WebElement element = listItem.waitForElmement(_test.getDriver(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        _test.scrollIntoView(listItem); // Workaround: Auto-scrolling in chrome isn't working well
        element.click();

        // close combo manually if it is a checkbox combo-box
        if (_test.isElementPresent(listItem.append("/span").withClass("x4-combo-checker")))
            _test.click(arrowTrigger);

        // menu should disappear
        _test._shortWait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("li.x4-boundlist-item")));
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(@LoggedParam String label, @LoggedParam String selection)
    {
        selectComboBoxItem(Ext4HelperWD.Locators.formItemWithLabel(label), selection, false);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(@LoggedParam String label, @LoggedParam String selection, boolean containsText)
    {
        selectComboBoxItem(Ext4HelperWD.Locators.formItemWithLabel(label), selection, containsText);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItemById(@LoggedParam String labelId, @LoggedParam String selection)
    {
        Locator.XPathLocator loc = Locator.xpath("//tbody[./tr/td/label[@id='" + labelId + "-labelEl']]");
        selectComboBoxItem(loc, selection);
    }

    @LogMethod(quiet = true)
    public void selectRadioButton(@LoggedParam String label, @LoggedParam String selection)
    {
        Locator l = Locator.xpath("//div[div/label[text()='" + label + "']]//label[text()='" + selection + "']");
        if (!_test.isElementPresent(l))
        {
            // try Ext 4.1.0 version
            l = Locator.xpath("//div[./table//label[text()='" + label + "']]//label[text()='" + selection + "']");
        }
        _test.click(l);
    }

    @LogMethod(quiet = true)
    public void selectRadioButtonById(@LoggedParam String labelId)
    {
        Locator l = Locator.xpath("//label[@id='" + labelId + "']");
        _test.click(l);
    }

    @LogMethod(quiet = true)
    public void waitForComponentNotDirty(@LoggedParam final String componentId)
    {
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return (Boolean)_test.executeScript("return Ext4.getCmp('" + componentId + "').isDirty();");
            }
        }, "Page still marked as dirty", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod(quiet = true)
    public void clickExt4Tab(@LoggedParam String tabname)
    {
        Locator l = Locator.xpath("//span[contains(@class, 'x4-tab') and text() = '" + tabname + "']");
        _test.click(l);
    }

    @LogMethod(quiet = true)
    public void checkCheckbox(@LoggedParam String label)
    {
        if (!isChecked(label))
        {
            Locator l = Locators.checkbox(_test, label);
            _test.click(l);
        }
    }

    @LogMethod(quiet = true)
    public void uncheckCheckbox(@LoggedParam String label)
    {
        if (isChecked(label))
        {
            Locator l = Locators.checkbox(_test, label);
            _test.click(l);
        }
    }

    public boolean isChecked(String label)
    {
        Locator.XPathLocator checkbox = Locators.checkbox(_test, label);
        _test.assertElementPresent(checkbox);
        Locator l = checkbox.append("[./ancestor-or-self::*[contains(@class, 'checked')]]");
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
    @LogMethod(quiet = true)
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
    @LogMethod(quiet = true)
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
    @LogMethod(quiet = true)
    public void clickGridRowText(String cellText, int index)
    {
        Locator.XPathLocator rowLoc = getGridRow(cellText, index);
        _test.waitForElement(rowLoc);
        _test.click(rowLoc.append("//div[contains(@class, 'x4-grid-cell')][normalize-space() = '"+cellText+"']"));
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
        _test.waitForElement(rowLoc);
        _test.click(rowLoc.append("//span[contains(@class, 'lk-filter-panel-label')][normalize-space() = '"+cellText+"']"));
    }

    /**
     * Click the text of an Participant filter panel category grouping header
     * Currently used only for participant filter panel
     * @param categoryLabel Exact text from any category label (i.e. Cohorts, Group 1)
     */
    public void clickParticipantFilterCategory(String categoryLabel)
    {
        Locator.XPathLocator loc = Locator.xpath("//input[contains(@class, 'category-header') and contains(@category, '" + categoryLabel + "')]");
        _test.click(loc);
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

    public <Type extends Ext4CmpRefWD> List<Type> componentQuery(String componentSelector, Class<Type> clazz)
    {
        return componentQuery(componentSelector, null, clazz);
    }

    public <Type extends Ext4CmpRefWD> List<Type> componentQuery(String componentSelector, String parentId, Class<Type> clazz)
    {
        componentSelector = componentSelector.replaceAll("'", "\"");  //escape single quotes
        String script =
                "ext4ComponentQuery = function (selector, parentId) {\n" +
                "    var res = null;\n" +
                "    if (parentId)\n" +
                "        res = Ext4.getCmp(parentId).query(selector);\n" +
                "    else\n" +
                "        res = Ext4.ComponentQuery.query(selector);\n" +

                "    return null == res ? null : Ext4.Array.pluck(res, \"id\");\n" +
                "};" +
                "return ext4ComponentQuery(arguments[0], arguments[1]);";

        List<String> unfilteredIds = (List<String>)_test.executeScript(script, componentSelector, parentId);
        List<String> ids = new ArrayList<String>();
        for (String id : unfilteredIds)
        {
            if (Locator.id(id).findElements(_test._driver).size() > 0)
                ids.add(id); // ignore uninitialized ext components
        }
        return _test._ext4Helper.componentsFromIds(ids, clazz);
    }

    public <Type extends Ext4CmpRefWD> Type queryOne(String componentSelector, Class<Type> clazz)
    {
        List<Type> cmpRefs = componentQuery(componentSelector, clazz);
        if (null == cmpRefs || cmpRefs.size() == 0)
            return null;

        return cmpRefs.get(0);
    }

    public <Type extends Ext4CmpRefWD> List<Type> componentsFromIds(List<String> ids, Class<Type> clazz)
    {
        if (null == ids || ids.isEmpty())
            return null;

        try
        {
            List<Type> ret = new ArrayList<Type>(ids.size());
            for (String id : ids)
            {
                Constructor<Type> constructor = clazz.getConstructor(String.class, BaseWebDriverTest.class);
                ret.add(constructor.newInstance(id, _test));
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

    public BaseWebDriverTest.Checker getExt4SelectorChecker(final String selector)
    {
        return new BaseWebDriverTest.Checker(){
            public boolean check()
            {
                return queryOne(selector, Ext4CmpRefWD.class) != null;
            }
        };
    }

    public void clickTabContainingText(String tabText)
    {
        _test.click(Locator.xpath("//span[contains(@class, 'x4-tab-inner') and contains( text(), '" + tabText + "')]"));
    }

    public void waitForMaskToDisappear()
    {
        waitForMaskToDisappear(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForMaskToDisappear(int wait)
    {
        _test.waitForElementToDisappear(Locators.mask(), wait);
    }

    public void waitForMask()
    {
        waitForMask(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForMask(int wait)
    {
        _test.waitForElement(Locators.mask(), wait);
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
    public void clickExt4MenuButton(boolean wait, Locator menu, boolean onlyOpen, String ... subMenuLabels)
    {
        _test.waitAndClick(menu);
        for (int i = 0; i < subMenuLabels.length - 1; i++)
        {
            Locator parentLocator = ext4MenuItem(subMenuLabels[i]);
            _test.waitForElement(parentLocator, 1000);
            _test.mouseOver(parentLocator);
        }
        Locator itemLocator = ext4MenuItem(subMenuLabels[subMenuLabels.length - 1]);
        if (onlyOpen)
        {
            _test.waitForElement(itemLocator, 1000);
            return;
        }
        if (wait)
            _test.waitAndClickAndWait(itemLocator);
        else
            _test.waitAndClick(itemLocator);
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
        return Locator.xpath("//div[" + Locator.NOT_HIDDEN + " and contains(@class, 'x4-window-header')]//span[text() = '" + title + "']");
    }

    public static class Locators
    {
        public static Locator.XPathLocator checkbox(BaseWebDriverTest test, String label)
        {
            Locator.XPathLocator l = Locator.xpath("//input[contains(@class,'x4-form-checkbox')][../label[text()='" + label + "']]");
            if (!test.isElementPresent(l))
                l = Locator.xpath("//input[contains(@class,'x4-form-checkbox')][../../td/label[text()='" + label + "']]");
            return l;
        }

        public static Locator.XPathLocator window(String title)
        {
            return Locator.xpath("//div").withClass("x4-window").notHidden().withDescendant(Locator.xpath("//span").withClass("x4-window-header-text").withText(title));
        }

        public static Locator.XPathLocator formItemWithLabel(String label)
        {
            return Locator.xpath("(//table|//tbody)").withClass("x4-form-item").withPredicate("(tbody/tr|tr)/td/label[normalize-space()='" + label + "']").notHidden();
        }

        public static Locator.XPathLocator formItemWithLabelContaining(String label)
        {
            return Locator.xpath("(//table|//tbody)").withClass("x4-form-item").withPredicate("(tbody/tr|tr)/td/label[contains(normalize-space(), '" + label + "')]").notHidden();
        }

        public static Locator.XPathLocator mask()
        {
            return Locator.xpath("//div["+Locator.NOT_HIDDEN+" and contains(@class, 'x4-mask')]");
        }

        public static Locator.XPathLocator folderManagementTreeNode(String nodeText)
        {
            return Locator.xpath("//tr").withClass("x4-grid-row").append("/td/div").withText(nodeText);
        }
    }

    public static Locator.XPathLocator ext4Tab(String label)
    {
        return Locator.xpath("//span[contains(@class, 'x4-tab-inner') and text() = '" + label + "']");
    }
}
