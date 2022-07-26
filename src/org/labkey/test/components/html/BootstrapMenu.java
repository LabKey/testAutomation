/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.test.components.html;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.react.BaseBootstrapMenu;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class BootstrapMenu extends BaseBootstrapMenu
{
    /* componentElement should contain the toggle anchor *and* the UL containing list items */
    public BootstrapMenu(WebDriver driver, WebElement componentElement)
    {
        super(componentElement, driver);
    }

    static public BootstrapMenuFinder finder(WebDriver driver)
    {
        return new BootstrapMenuFinder(driver);
    }

    /**
     * @deprecated Use {@link BootstrapMenuFinder} directly
     */
    @Deprecated
    static public BootstrapMenu find(WebDriver driver, String menuToggleText)
    {
        return new BootstrapMenuFinder(driver).withButtonTextContaining(menuToggleText).find();
    }

    @Override
    public BootstrapMenu withExpandRetries(int retries)
    {
        return (BootstrapMenu) super.withExpandRetries(retries);
    }

    public List<WebElement> findVisibleMenuItems()
    {
        return elementCache().findVisibleMenuItems();
    }

    public List<WebElement> findVisibleMenuItemsWithCls(String cls)
    {
        return elementCache().findVisibleMenuItemsWithClass(cls);
    }

    protected WebElement findVisibleMenuItemOrNull(String text)
    {
        return elementCache().findVisibleMenuItemOrNull(text);
    }

    protected WebElement findDisabledMenuItemOrNull(String text)
    {
        return elementCache().findDisabledMenuItemOrNull(text);
    }

    protected WebElement findVisibleMenuItem(String text)
    {
        return elementCache().findVisibleMenuItem(text);
    }

    @LogMethod(quiet = true)
    public WebElement openMenuTo(@LoggedParam String ... subMenuLabels)
    {
        expand();

        if (subMenuLabels.length == 0)
            return null;

        for (int i = 0; i < subMenuLabels.length - 1; i++)
        {
            WebElement subMenuItem = Locators.menuItem(subMenuLabels[i])
                    .waitForElement(elementCache().findOpenMenu(), 2000);
            subMenuItem.click();
        }
        WebElement item = Locators.menuItem(subMenuLabels[subMenuLabels.length - 1])
                .waitForElement(elementCache().findOpenMenu(), WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        getWrapper().fireEvent(item, WebDriverWrapper.SeleniumEvent.mouseover); /* mouseOver causes selenium to attempt
                                to scroll the item into view, and if that can't be done we'll fail here.  See if firing
                                the event is less flaky */
        return item;
    }

    @LogMethod(quiet = true)
    public void clickSubMenu(int timeout, @LoggedParam String ... subMenuLabels)
    {
        if (subMenuLabels.length < 1)
            throw new IllegalArgumentException("Specify menu item(s)");

        WebElement item = openMenuTo(subMenuLabels);
        getWrapper().scrollIntoView(item);

        getWrapper().clickAndWait(item, timeout);
    }

    @LogMethod(quiet = true)
    public void clickSubMenu(boolean wait, @LoggedParam String ... subMenuLabels)
    {
        clickSubMenu(wait ? getWrapper().getDefaultWaitForPage() : 0, subMenuLabels);
    }

    @Override
    protected Locator getToggleLocator()
    {
        return Locators.dropdownToggle();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseBootstrapMenu.ElementCache
    {
        public WebElement findVisibleMenuPanel()
        {
            WebElement menuList = findOpenMenu();
            List<WebElement> submenus = Locator.css("ul.dropdown-layer-menu.open").findElements(menuList);
            if (!submenus.isEmpty())
                return submenus.get(submenus.size() - 1);   /* if one or more submenus are open, use the last open one */
            return menuList;
        }

        public List<WebElement> findVisibleMenuItems()
        {
            return Locator.xpath("./li/a").findElements(findVisibleMenuPanel()); /* direct children of the currently-open menu or submenu */
        }

        public List<WebElement> findVisibleMenuItemsWithClass(String cls)
        {
            return Locator.xpath("./li[contains(@class, '" + cls + "')]/a").findElements(findVisibleMenuPanel()); /* direct children of the currently-open menu or submenu */
        }

        protected WebElement findVisibleMenuItem(String text)
        {
            return Locator.xpath("./li/a").withText(text).findElement(findVisibleMenuPanel());
        }

        protected WebElement findVisibleMenuItemOrNull(String text)
        {
            return Locator.xpath("./li/a").withText(text).findElementOrNull(findVisibleMenuPanel());
        }

        protected WebElement findDisabledMenuItemOrNull(String text)
        {
            Locator.XPathLocator loc=  Locator.xpath("./li/a").withText(text).parent().withClass("disabled");
            return loc.findElementOrNull(findVisibleMenuPanel());
        }
    }

    static public class Locators
    {
        public static Locator.XPathLocator dropdownToggle()
        {
            return Locator.byClass("dropdown-toggle");
        }

        public static Locator.XPathLocator menuItem(String text)
        {
            return Locator.tag("li").childTag("a").withText(text).notHidden();
        }

        public static Locator.XPathLocator menuItemDisabled(String text)
        {
            return Locator.tagWithClass("li", "disabled").childTag("a").withText(text).notHidden();
        }

        public static Locator.XPathLocator dropdownMenu()
        {
            return dropdownMenu(dropdownToggle());
        }

        private static Locator.XPathLocator dropdownMenu(Locator.XPathLocator toggleLoc)
        {
            return Locator.byClass("dropdown")
                    .withChild(toggleLoc)
                    .withChild(Locator.tag("ul"));
        }
    }

    public static class BootstrapMenuFinder extends WebDriverComponentFinder<BootstrapMenu, BootstrapMenu.BootstrapMenuFinder>
    {
        private Locator _locator = Locators.dropdownMenu();

        public BootstrapMenuFinder(WebDriver driver)
        {
            super(driver);
        }

        public BootstrapMenuFinder withToggleId(String id)
        {
            _locator = Locators.dropdownMenu(Locators.dropdownToggle().withAttribute("id", id));
            return this;
        }

        /**
         * Some menu controls have IDs prepended with dynamic numbers, like 123-special-menu
         * @param substring the part of the ID that isn't dynamic
         * @return
         */
        public BootstrapMenuFinder withToggleIdContaining(String substring)
        {
            _locator = Locators.dropdownMenu(Locators.dropdownToggle().withAttributeContaining("id", substring));
            return this;
        }

        public BootstrapMenuFinder withTitle(String title)
        {
            _locator = Locators.dropdownMenu().withAttribute("title", title);
            return this;
        }

        public BootstrapMenuFinder withButtonText(String text)
        {
            _locator = Locators.dropdownMenu(Locators.dropdownToggle().withText(text));
            return this;
        }

        public BootstrapMenuFinder withButtonTextContaining(String text)
        {
            _locator = Locators.dropdownMenu(Locators.dropdownToggle().containing(text));
            return this;
        }

        @Override
        protected BootstrapMenuFinder getThis()
        {
            return this;
        }

        @Override
        protected BootstrapMenu construct(WebElement el, WebDriver driver)
        {
            return new BootstrapMenu(driver, el);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
