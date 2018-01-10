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
import org.labkey.test.components.ext4.Checkbox;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.labkey.test.components.ext4.Checkbox.Ext4Checkbox;

public class Ext4Helper
{
    private static final String DEFAULT_CSS_PREFIX = "x4-";
    private static String _cssPrefix = DEFAULT_CSS_PREFIX;

    WebDriverWrapper _test;

    public Ext4Helper(WebDriverWrapper test)
    {
        _test = test;
        resetCssPrefix();
    }

    public static void resetCssPrefix()
    {
        _cssPrefix = DEFAULT_CSS_PREFIX;
    }

    public static void setCssPrefix(String cssPrefix)
    {
        _cssPrefix = cssPrefix;
    }

    public static String getCssPrefix()
    {
        return _cssPrefix;
    }

    public enum TextMatchTechnique implements ComboBox.ComboListMatcher
    {
        EXACT
                {
                    @Override
                    public Locator.XPathLocator getLocator(Locator.XPathLocator comboListItem, String itemText)
                    {
                        return comboListItem.withText(itemText);
                    }
                },
        LEADING_NBSP
                {
                    @Override
                    public Locator.XPathLocator getLocator(Locator.XPathLocator comboListItem, String itemText)
                    {
                        return comboListItem.withText(Locator.NBSP + itemText);
                    }
                },
        CONTAINS
                {
                    @Override
                    public Locator.XPathLocator getLocator(Locator.XPathLocator comboListItem, String itemText)
                    {
                        return comboListItem.containing(itemText);
                    }
                },
        STARTS_WITH
                {
                    @Override
                    public Locator.XPathLocator getLocator(Locator.XPathLocator comboListItem, String itemText)
                    {
                        return comboListItem.startsWith(itemText);
                    }
                },
        REGEX
                {
                    @Override
                    public Locator.XPathLocator getLocator(Locator.XPathLocator comboListItem, String itemText)
                    {
                        return comboListItem.withTextMatching(itemText);
                    }
                }
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(Locator.XPathLocator comboBox, @LoggedParam String... selections)
    {
        selectComboBoxItem(comboBox, TextMatchTechnique.EXACT, selections);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(Locator.XPathLocator comboBox, TextMatchTechnique matchTechnique, @LoggedParam String... selections)
    {
        openComboList(comboBox);

        try
        {
            for (String selection : selections)
            {
                selectItemFromOpenComboList(selection, matchTechnique);
            }
        }
        catch (StaleElementReferenceException retry) // Combo-box might still be loading previous selection (no good way to detect)
        {
            for (String selection : selections)
            {
                selectItemFromOpenComboList(selection, matchTechnique);
            }
        }

        closeComboList(comboBox);
    }

    public void openComboList(Locator.XPathLocator comboBox)
    {
        Locator arrowTrigger = comboBox.append("//div[contains(@class,'arrow')]");
        _test.waitAndClick(arrowTrigger);

        if (!_test.waitForElement(comboBox.withDescendant(Locator.tag("td").withClass(_cssPrefix + "pickerfield-open")), 1000, false))
            _test.click(arrowTrigger); // try again if combo-box doesn't open

        _test.waitForElement(Locators.comboListItem());
    }

    public void selectItemFromOpenComboList(String itemText, TextMatchTechnique matchTechnique, boolean clickAt)
    {
        Locator.XPathLocator listItem = matchTechnique.getLocator(Locators.comboListItem(), itemText);

        WebElement element = listItem.waitForElement(_test.getDriver(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        boolean elementAlreadySelected = element.getAttribute("class").contains("selected");
        if (!isOpenComboBoxMultiSelect() || !elementAlreadySelected)
        {
            _test.scrollIntoView(element); // Workaround: Auto-scrolling in chrome isn't working well
            if(clickAt)
            _test.clickAt(listItem,0,0,0);
            else _test.click(listItem);
        }
    }

    public void selectItemFromOpenComboList(String itemText, TextMatchTechnique matchTechnique)
    {
        selectItemFromOpenComboList(itemText, matchTechnique,false);
    }

    public void closeComboList(Locator.XPathLocator comboBox)
    {
        closeComboList(comboBox, false);
    }

    public void closeComboList(Locator.XPathLocator comboBox, boolean forceClose)
    {
        Locator arrowTrigger = comboBox.append("//div[contains(@class,'arrow')]");

        // close combo manually if it is a multi-select combo-box
        if (forceClose || isOpenComboBoxMultiSelect())
            _test.click(arrowTrigger);

        // menu should disappear
        _test.waitForElementToDisappear(Locators.comboListItem());
    }

    private boolean isOpenComboBoxMultiSelect()
    {
        return _test.isElementPresent(Locators.comboListItem().append("/span").withClass(_cssPrefix + "combo-checker"));
    }

    @LogMethod(quiet = true)
    public void clearComboBox(@LoggedParam String label)
    {
        clearComboBox(Ext4Helper.Locators.formItemWithLabel(label));
    }

    public void clearComboBox(Locator.XPathLocator comboBox)
    {
        openComboList(comboBox);

        try
        {
            for (WebElement element : Locators.comboListItem().findElements(_test.getDriver()))
            {
                boolean elementAlreadySelected = element.getAttribute("class").contains("selected");
                if (isOpenComboBoxMultiSelect() && elementAlreadySelected)
                    element.click();
            }
        }
        catch (StaleElementReferenceException retry) // Combo-box might still be loading previous selection (no good way to detect)
        {
            for (WebElement element : Locators.comboListItem().findElements(_test.getDriver()))
            {
                boolean elementAlreadySelected = element.getAttribute("class").contains("selected");
                if (isOpenComboBoxMultiSelect() && elementAlreadySelected)
                    element.click();
            }
        }

        closeComboList(comboBox);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(@LoggedParam String label, @LoggedParam String... selections)
    {
        selectComboBoxItem(label, TextMatchTechnique.EXACT, selections);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(@LoggedParam String label, TextMatchTechnique matchTechnique, @LoggedParam String... selections)
    {
        selectComboBoxItem(Ext4Helper.Locators.formItemWithLabel(label), matchTechnique, selections);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItemById(@LoggedParam String labelId, @LoggedParam String selection)
    {
        Locator.XPathLocator loc = Locator.tagWithId("table", labelId);
        selectComboBoxItem(loc, selection);
    }

    @LogMethod(quiet = true)
    public List<String> getComboBoxOptions(@LoggedParam String label)
    {
        return getComboBoxOptions(Locators.formItemWithLabel(label));
    }

    @LogMethod(quiet=true)
    public List<String> getComboBoxOptions(Locator.XPathLocator comboBoxLocator)
    {
        openComboList(comboBoxLocator);
        return _test.getTexts(Locators.comboListItem().findElements(_test.getDriver()));
    }

    @LogMethod(quiet = true)
    public void selectRadioButton(@LoggedParam String groupLabel, @LoggedParam String selection)
    {
        WebElement radioButton;
        try
        {
            radioButton = Locator.xpath("//div[div/label[text()='" + groupLabel + "']]//label[text()='" + selection + "']").findElement(_test.getDriver());
        }
        catch (NoSuchElementException retry)
        {
            // try Ext 4.1.0 version
            radioButton = Locator.xpath("//div[./table//label[text()='" + groupLabel + "']]//label[text()='" + selection + "']").findElement(_test.getDriver());
        }
        _test.click(radioButton);
    }

    @LogMethod(quiet = true)
    public void selectRadioButtonById(@LoggedParam String labelId)
    {
        Locator l = Locator.xpath("//label[@id='" + labelId + "']");
        _test.click(l);
    }

    /**
     * @deprecated Use {@link org.labkey.test.components.ext4.RadioButton}
     */
    @Deprecated
    public void selectRadioButton(String selection)
    {
        Locator l = Locators.radiobutton(_test, selection);
        _test.click(l);
    }

    @LogMethod(quiet = true)
    public void selectRadioButtonWithLabelContaining(@LoggedParam String groupLabel, @LoggedParam String selection)
    {
        Locator l = Locator.xpath("//table/tbody/tr[td/label/span[contains(text(), '" + groupLabel + "')]]//label[text()='" + selection + "']");
        _test.click(l);
    }

    @LogMethod(quiet = true)
    public void waitForComponentNotDirty(@LoggedParam final String componentId)
    {
        WebDriverWrapper.waitFor(() -> !(Boolean)_test.executeScript("return Ext4.getCmp('" + componentId + "').isDirty();"),
                "Page still marked as dirty", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod(quiet = true)
    public void clickExt4Tab(@LoggedParam String tabname)
    {
        Locator l = Locators.tab(tabname);
        _test.click(l);
    }

    /**
     * @deprecated Use {@link Window#clickButton(String, int)}
     */
    @Deprecated
    public void clickWindowButton(String windowTitle, String buttonText, int wait, int index)
    {
        _test.log("Clicking Ext4 button with text: " + buttonText + " inside window with title: " + windowTitle);
        Locator loc = Locators.windowButton(windowTitle, buttonText).index(index);
        _test.waitAndClick(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, loc, wait);
    }

    /**
     * @deprecated Use {@link Window#clickButton(String)}
     */
    @Deprecated
    public void clickWindowButtonAndWait(String windowTitle, String buttonText)
    {
        _test.log("Clicking Ext4 button with text: " + buttonText + " inside window with title: " + windowTitle);
        Locator loc = Locators.windowButton(windowTitle, buttonText);
        _test.waitAndClickAndWait(loc);
    }

    public void clearGridSelection(WebElement gridEl)
    {
        String script =
                "selectExt4GridItem = function (selector) {\n" +
                "    var grid = Ext4.getCmp(el.id);\n" +
                "    if (grid)\n" +
                "    {\n" +
                "        grid.getSelectionModel().deselectAll();\n" +
                "    }\n" +
                "    else\n" +
                "    {\n" +
                "        throw 'Element not an Ext4 component: ' + el.id;\n" +
                "    }\n" +
                "};" +
                "selectExt4GridItem(arguments[0]);";
        _test.executeScript(script, gridEl);
    }

    @Deprecated
    public void clearGridSelection(String markerCls)
    {
        clearGridSelection(Locator.css("." + markerCls).findElement(_test.getDriver()));
    }

    public boolean isGridRowSelected(String cellText, int index) {
        WebElement gridRow = Locators.getGridRow(cellText).index(index).findElement(_test.getDriver());
        if (gridRow.getAttribute("class").contains(_cssPrefix + "grid-row-selected"))
        {
            return true;
        }
        return false;
    }

    public void selectGridItem(String columnVal, String markerCls)
    {
        WebElement gridRow = Locators.getGridRow(columnVal, markerCls).findElement(_test.getDriver());
        if (!gridRow.getAttribute("class").contains(_cssPrefix + "grid-row-selected"))
        {
            WebElement gridRowChecker = gridRow.findElement(By.cssSelector("." + _cssPrefix + "grid-cell-row-checker"));
            _test.click(gridRowChecker);
        }
    }

    public void selectGridItem(String columnName, String columnVal, int idx, WebElement gridEl, boolean keepExisting)
    {
        String script =
                "selectExt4GridItem = function (columnName, columnVal, idx, el, keepExisting) {\n" +
                "    var grid = Ext4.getCmp(el.id);\n" +
                "    if (grid)\n" +
                "    {\n" +
                "        if (idx == -1)\n" +
                "            idx = grid.getStore().find(columnName, columnVal);\n" +
                "        if (idx == -1)\n" +
                "            throw 'Unable to locate ' + columnName + ': ' + columnVal;\n" +
                "        if (idx >= grid.getStore().getCount())\n" +
                "            throw 'No such row: ' + idx;\n" +
                "        grid.getSelectionModel().select(idx, keepExisting);\n" +
                "    }\n" +
                "    else\n" +
                "    {\n" +
                "        throw 'Element not an Ext4 component: ' + el.id;\n" +
                "    }\n" +
                "};" +
                "selectExt4GridItem(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]);";
        _test.executeScript(script, columnName, columnVal, idx, gridEl, keepExisting);
    }

    public void selectGridItem(String columnName, String columnVal, int idx, String markerCls, boolean keepExisting)
    {
        WebElement grid = Locator.css("." + markerCls).findElement(_test.getDriver());
        selectGridItem(columnName, columnVal, idx, grid, keepExisting);
    }

    public void checkCheckbox(Locator.XPathLocator checkboxLocator)
    {
        new Checkbox(checkboxLocator.findElement(_test.getDriver())).check();
    }

    public void uncheckCheckbox(Locator.XPathLocator checkboxLocator)
    {
        new Checkbox(checkboxLocator.findElement(_test.getDriver())).uncheck();
    }

    /**
     * @deprecated Use {@link Checkbox}
     */
    @Deprecated
    @LogMethod(quiet = true)
    public void checkCheckbox(@LoggedParam String label)
    {
        Ext4Checkbox().withLabel(label).waitFor(_test.getDriver()).check();
    }

    /**
     * @deprecated Use {@link Checkbox}
     */
    @Deprecated
    @LogMethod(quiet = true)
    public void checkCheckbox(@LoggedParam String label, @LoggedParam int index)
    {
        Ext4Checkbox().withLabel(label).index(index).waitFor(_test.getDriver()).check();
    }

    /**
     * @deprecated Use {@link Checkbox}
     */
    @Deprecated
    @LogMethod(quiet = true)
    public void uncheckCheckbox(@LoggedParam String label)
    {
        Ext4Checkbox().withLabel(label).waitFor(_test.getDriver()).uncheck();
    }

    /**
     * @deprecated Use {@link Checkbox}
     */
    @Deprecated
    @LogMethod(quiet = true)
    public void uncheckCheckbox(@LoggedParam String label, @LoggedParam int index)
    {
        Ext4Checkbox().withLabel(label).index(index).waitFor(_test.getDriver()).uncheck();
    }

    /**
     * @deprecated Use {@link Checkbox}
     */
    @Deprecated
    public boolean isChecked(String label)
    {
        return Ext4Checkbox().withLabel(label).waitFor(_test.getDriver()).isChecked();
    }

    /**
     * @deprecated Use {@link Checkbox}
     */
    @Deprecated
    public boolean isChecked(String label, int index)
    {
        return Ext4Checkbox().withLabel(label).index(index).waitFor(_test.getDriver()).isChecked();
    }

    public boolean isChecked(Locator checkboxLoc)
    {
        return Ext4Checkbox().locatedBy(checkboxLoc).find(_test.getDriver()).isChecked();
    }

    private Checkbox findGridRowCheckbox(String cellText, int index)
    {
        Locator.XPathLocator loc = Locators.getGridRow(cellText).append(Locator.tagWithClass("div", _cssPrefix + "grid-row-checker")).index(index);
        return Ext4Checkbox().locatedBy(loc).find(_test.getDriver());
    }

    /**
     * Check the checkbox for an Ext4 grid row
     * @param cellText Exact text from any cell in the desired row
     */
    public void checkGridRowCheckbox(String cellText)
    {
        checkGridRowCheckbox(cellText, 0);
    }

    /**
     * Check the checkbox for an Ext4 grid row
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     */
    @LogMethod(quiet = true)
    public void checkGridRowCheckbox(String cellText, int index)
    {
        findGridRowCheckbox(cellText, index).check();
    }

    @LogMethod(quiet = true)
    public void checkGridCellCheckbox(String otherCellText, int index)
    {
        Locator.XPathLocator loc = Locators.getGridRow(otherCellText).append(Locator.tagWithClass("img", _cssPrefix + "grid-checkcolumn")).index(index);
        Ext4Checkbox().locatedBy(loc).find(_test.getDriver()).check();
    }

    /**
     * Uncheck the checkbox for an Ext4 grid row
     * @param cellText Exact text from any cell in the desired row
     */
    public void uncheckGridRowCheckbox(String cellText)
    {
        uncheckGridRowCheckbox(cellText, 0);
    }

    /**
     * Uncheck the checkbox for an Ext4 grid row
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     */
    @LogMethod(quiet = true)
    public void uncheckGridRowCheckbox(String cellText, int index)
    {
        findGridRowCheckbox(cellText, index).uncheck();
    }

    /**
     * Click the text of an Ext4 grid row
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     */
    @LogMethod(quiet = true)
    public void clickGridRowText(String cellText, int index)
    {
        Locator.XPathLocator rowLoc = Locators.getGridRow(cellText).index(index);
        _test.waitAndClick(rowLoc.append("//div[contains(@class, '" + _cssPrefix + "grid-cell')][normalize-space() = '" + cellText + "']"));
    }

    /**
     * Click the text of an Participant filter panel grid row
     * @param cellText Exact text from any cell in the desired row
     * @param index 0-based index of rows with matching cellText
     */
    public void clickParticipantFilterGridRowText(String cellText, int index)
    {
        _test.waitForElementToDisappear(Locator.tag("div").withClass(_cssPrefix + "tip").notHidden()); // tooltip breaks test in Chrome
        Locator.XPathLocator rowLoc = Locators.getGridRow(cellText).index(index);
        _test.waitAndClick(rowLoc.append("//div[contains(@class, 'lk-filter-panel-label') and contains(@class, 'group-label')][normalize-space() = '" + cellText + "']"));
    }

    /**
     * Click the text of an Participant filter panel category grouping header
     * @param categoryLabel Exact text from any category label (i.e. Cohorts, Group 1)
     */
    public void clickParticipantFilterCategory(String categoryLabel)
    {
        Locator.XPathLocator loc = Locator.xpath("//div[contains(@class, 'category-label') and text()='" + categoryLabel + "']/../../td/div[contains(@class, 'category-header')]");
        _test.click(loc);
    }

    /**
     * Deselect the "All" Participant filter panel category checkbox
     */
    public void deselectAllParticipantFilter()
    {
        checkGridRowCheckbox("All");
        uncheckGridRowCheckbox("All");
    }

    /**
     * Select the "All" Participant filter panel category checkbox
     */
    public void selectAllParticipantFilter()
    {
        checkGridRowCheckbox("All");
    }

    public <Type extends Ext4CmpRef> List<Type> componentQuery(String componentSelector, Class<Type> clazz)
    {
        return componentQuery(componentSelector, null, clazz);
    }

    public <Type extends Ext4CmpRef> List<Type> componentQuery(String componentSelector, String parentId, Class<Type> clazz)
    {
        for (int i = 0; i <= 4; i++)
        {
            List<Type> cmps = _componentQuery(componentSelector, parentId, clazz);
            if (cmps != null && !cmps.isEmpty())
                return cmps;

            _test.sleep(500);
        }

        return Collections.emptyList();
    }

    private <Type extends Ext4CmpRef> List<Type> _componentQuery(String componentSelector, String parentId, Class<Type> clazz)
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
        List<String> ids = new ArrayList<>();
        for (String id : unfilteredIds)
        {
            if (Locator.id(id).findElements(_test.getDriver()).size() > 0)
                ids.add(id); // ignore uninitialized ext components
        }
        return componentsFromIds(ids, clazz);
    }

    public <Type extends Ext4CmpRef> Type queryOne(String componentSelector, Class<Type> clazz)
    {
        List<Type> cmpRefs = componentQuery(componentSelector, clazz);
        if (null == cmpRefs || cmpRefs.size() == 0)
            return null;

        return cmpRefs.get(0);
    }

    public <Type extends Ext4CmpRef> List<Type> componentsFromIds(List<String> ids, Class<Type> clazz)
    {
        if (null == ids || ids.isEmpty())
            return null;

        try
        {
            List<Type> ret = new ArrayList<>(ids.size());
            for (String id : ids)
            {
                Constructor<Type> constructor = clazz.getConstructor(String.class, WebDriverWrapper.class);
                ret.add(constructor.newInstance(id, _test));
            }
            return ret;
        }
        catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Supplier<Boolean> getExt4SelectorChecker(final String selector)
    {
        return () -> queryOne(selector, Ext4CmpRef.class) != null;
    }

    public void clickTabContainingText(String tabText)
    {
        _test.click(Locators.tab(tabText));
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

    public void waitForOnReady()
    {
        _test.waitFor(() ->
        {
            try
            {
                _test.executeAsyncScript("Ext4.onReady(callback);");
                return true;
            }
            catch (WebDriverException scriptError)
            {
                _test.dismissAllAlerts();
                return false;
            }
        }, 10000);
    }

    @LogMethod(quiet = true)
    public WebElement clickExt4MenuButton(boolean wait, WebElement menu, boolean onlyOpen, @LoggedParam String ... subMenuLabels)
    {
        waitForOnReady();
        _test.click(menu);
        try
        {
            _test.waitForElement(Locators.menuItem().notHidden(), 1000);
        }
        catch (NoSuchElementException retry)
        {
            _test.click(menu); // Sometimes ext4 menus don't open on the first try
            _test.waitForElement(Locators.menuItem().notHidden(), 1000);
        }
        if (onlyOpen && subMenuLabels.length == 0)
            return null;

        for (int i = 0; i < subMenuLabels.length - 1; i++)
        {
            WebElement subMenuItem = _test.waitForElement(Locators.menuItem(subMenuLabels[i]).notHidden(), 2000);
            _test.clickAndWait(subMenuItem, 0);
        }
        WebElement item = _test.waitForElement(Locators.menuItem(subMenuLabels[subMenuLabels.length - 1]).notHidden());
        if (onlyOpen)
        {
            _test.mouseOver(item);
            return item;
        }

        if (wait)
            _test.clickAndWait(item);
        else
            _test.clickAndWait(item, 0);
        return null;
    }

    public WebElement openMenu(Locator menu, String... subMenus)
    {
        return clickExt4MenuButton(false, menu, true, subMenus);
    }

    public WebElement openMenu(WebElement menu, String... subMenus)
    {
        return clickExt4MenuButton(false, menu, true, subMenus);
    }

    @LogMethod(quiet = true)
    public WebElement clickExt4MenuButton(boolean wait, Locator menu, boolean onlyOpen, @LoggedParam String ... subMenuLabels)
    {
        return clickExt4MenuButton(wait, menu.waitForElement(_test.shortWait()), onlyOpen, subMenuLabels);
    }

    public List<WebElement> getMenuItems(WebElement menu)
    {
        openMenu(menu);
        return Locator.tagWithClass("span", _cssPrefix + "menu-item-text").findElements(_test.getDriver());
    }

    public void closeExtTab(String tabName)
    {
        _test.log("Closing Ext tab " + tabName);
        _test.click(Locator.xpath("//a[contains(@class, 'x4-tab')]//span[contains(@class, 'x4-tab-inner') and text()='" + tabName + "']/../../../span[contains(@class, 'x4-tab-close-btn')]"));
    }

    public static class Locators
    {
        public static Locator.XPathLocator comboListItem()
        {
            return Locator.tagWithClass("*", _cssPrefix + "boundlist-item").notHidden();
        }

        public static Locator.XPathLocator comboListItemSelected()
        {
            return Locator.tagWithClass("*", _cssPrefix + "boundlist-selected").notHidden();
        }

        public static Locator.XPathLocator checkbox(WebDriverWrapper test, String label)
        {
            Locator.XPathLocator l = Locator.xpath("//input[contains(@class,'" + _cssPrefix + "form-checkbox')][../label[text()='" + label + "']]");
            if (!test.isElementPresent(l))
                l = Locator.xpath("//input[contains(@class,'" + _cssPrefix + "form-checkbox')][../../td/label[text()='" + label + "']]");
            return l;
        }

        /**
         * @deprecated Use {@link org.labkey.test.components.ext4.RadioButton}
         */
        @Deprecated
        public static Locator.XPathLocator radiobutton(WebDriverWrapper test, String label)
        {
            Locator.XPathLocator l = Locator.xpath("//input[contains(@class,'" + _cssPrefix + "form-radio')][../label[contains(text(), '" + label + "')]]");
            if (!test.isElementPresent(l))
                l = Locator.xpath("//input[contains(@class,'" + _cssPrefix + "form-radio')][../../td/label[text()='" + label + "']]");
            return l;
        }

        /**
         * @deprecated Use {@link org.labkey.test.components.ext4.Window}
         */
        @Deprecated
        public static Locator.XPathLocator window()
        {
            return Window.Locators.window();
        }

        /**
         * @deprecated Use {@link org.labkey.test.components.ext4.Window}
         */
        @Deprecated
        public static Locator.XPathLocator window(String title)
        {
            return window().withDescendant(Window.Locators.title().withText(title));
        }

        /**
         * @deprecated Use {@link org.labkey.test.components.ext4.Window}
         */
        @Deprecated
        public static Locator.XPathLocator windowButton(String windowTitle, String buttonText)
        {
            return window(windowTitle).append(ext4Button(buttonText));
        }

        public static Locator.XPathLocator formItemWithLabel(String label)
        {
            return formItem().withDescendant(Locator.tag("label").withText(label));
        }

        public static Locator.XPathLocator formItemWithLabelContaining(String label)
        {
            return formItem().withDescendant(Locator.tag("label").containing(label));
        }

        public static Locator.XPathLocator formItem()
        {
            return Locator.tag("*").withClass(_cssPrefix + "form-item").notHidden();
        }

        public static Locator.XPathLocator formItemWithInputNamed(String name)
        {
            return formItem().withDescendant(Locator.tag("input").withAttribute("name", name));
        }

        public static Locator.XPathLocator menu()
        {
            return Locator.tagWithClass("div", _cssPrefix + "menu");
        }

        public static Locator.XPathLocator menuItem()
        {
            return Locator.tagWithClass("span", _cssPrefix + "menu-item-text");
        }

        public static Locator.XPathLocator menuItem(String text)
        {
            return menuItem().withText(text);
        }

        public static Locator.XPathLocator menuItemDisabled(String text)
        {
            return Locator.tagWithClass("div", _cssPrefix + "menu-item-disabled").withDescendant(menuItem(text));
        }

        public static Locator.XPathLocator mask()
        {
            return Locator.tag("div").withClass(_cssPrefix + "mask").notHidden();
        }

        public static Locator.XPathLocator folderManagementTreeSelectedNode(String nodeText)
        {
            return Locator.tag("tr").withClass(_cssPrefix + "grid-row").withClass(_cssPrefix + "grid-row-selected").append("/td/div").withText(nodeText);
        }

        public static Locator.XPathLocator tab()
        {
            return Locator.tagWithClass("a", _cssPrefix + "tab");
        }

        public static Locator.XPathLocator tab(String tabName)
        {
            return tab().containing(tabName);
        }

        public static Locator.XPathLocator ext4Button(String text)
        {
            return ext4Button().withText(text);
        }

        public static Locator.XPathLocator ext4Button()
        {
            return Locator.tag("a").notHidden().withClass(_cssPrefix + "btn");
        }

        public static Locator.XPathLocator ext4ButtonEnabled(String text)
        {
            return ext4Button(text).withoutClass(_cssPrefix + "disabled");
        }

        public static Locator.XPathLocator ext4ButtonContainingText(String text)
        {
            return Locator.tag("a").withClass(_cssPrefix + "btn").containing(text);
        }

        /**
         * @deprecated Use {@link Checkbox}
         */
        @Deprecated
        public static Locator.XPathLocator ext4Checkbox(String label)
        {
            return Locator.xpath("//input[@type = 'button' and contains(@class, 'checkbox') and following-sibling::label[text()='" + label + "']]");
        }

        /**
         * @deprecated Use {@link Checkbox}
         */
        @Deprecated
        public static Locator.XPathLocator ext4CheckboxById(String partialId)
        {
            return Locator.xpath("//input[@type = 'button' and contains(@class, 'checkbox') and contains(@id, '" + partialId + "')]");
        }

        /**
         * @deprecated Use {@link org.labkey.test.components.ext4.RadioButton}
         */
        @Deprecated
        public static Locator.XPathLocator ext4Radio(String label)
        {
            return Locator.xpath("//input[" + Locator.NOT_HIDDEN + " and @type = 'button' and contains(@class, 'radio') and following-sibling::label[contains(text(), '" + label + "')]]");
        }

        public static Locator getGridRow(String columnVal, String markerCls)
        {
            String[] markerClasses = markerCls.split(" \\.");
            Locator.XPathLocator loc = Locator.xpath("");
            for (String cls : markerClasses)
            {
                loc = loc.append(Locator.tagWithClass("*", cls));
            }
            return loc.append(getGridRow(columnVal));
        }

        public static Locator.XPathLocator getGridRow()
        {
            return Locator.tag("tr").withClass(_cssPrefix + "grid-row");
        }

        public static Locator.XPathLocator getGridDataRow()
        {
            return Locator.tag("tr").withClass(_cssPrefix + "grid-data-row");
        }

        /**
         * @param cellText Exact text from any cell in the desired row
         * @return XPathLocator for the desired row
         */
        public static Locator.XPathLocator getGridRow(String cellText)
        {
            return getGridRow().withPredicate("(td|td/table/tbody/tr/td)[string() = " + Locator.xq(cellText) + "]").notHidden();
        }

        public static Locator.XPathLocator invalidField()
        {
            return Locator.tag("input").withClass(_cssPrefix + "form-field").withClass(_cssPrefix + "form-invalid-field");
        }

        public static Locator.XPathLocator ext4Tab(String label)
        {
            return Locator.tagWithText("span", label).withClass(_cssPrefix + "tab-button").withDescendant(Locator.tagWithText("span", label).withClass(_cssPrefix + "tab-inner")).notHidden();
        }
    }
}
