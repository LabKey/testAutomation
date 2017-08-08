/*
 * Copyright (c) 2017 LabKey Corporation
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
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;


public class BootstrapMenu extends WebDriverComponent<BootstrapMenu.Elements>
{
    protected final WebDriver _driver;
    protected final WebElement _componentElement;

    /* componentElement should contain the toggle anchor *and* the UL containing list items */
    public BootstrapMenu(WebDriver driver, WebElement componentElement)
    {
        this(new WebDriverWrapperImpl(driver), componentElement);
    }

    public BootstrapMenu(WebDriverWrapper wrapper, WebElement componentElement)
    {
        _componentElement = componentElement;
        _driver = wrapper.getDriver();
    }

    static public BootstrapMenu find(WebDriver driver, String menuToggleText)
    {
        WebElement container = Locators.bootstrapMenuContainer()
                .withChild(Locators.toggleAnchor().containing(menuToggleText)).findElement(driver);
        return new BootstrapMenu(driver, container);
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _componentElement;
    }

    public boolean isExpanded()
    {
        String expandedAttribute = elementCache().toggleAnchor.getAttribute("aria-expanded");
        return expandedAttribute != null && expandedAttribute.equals("true");
    }

    public void expand()
    {
        if (!isExpanded())
            elementCache().toggleAnchor.click();
        getWrapper().waitFor(()-> isExpanded(), "Menu did not expand as expected", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    public void collapse()
    {
        if (isExpanded())
            elementCache().toggleAnchor.click();
        getWrapper().waitFor(()-> !isExpanded(), "Menu did not collapse as expected", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    public List<WebElement> findVisibleMenuItems()
    {
        return elementCache().findVisibleMenuItems();
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
            TestLogger.log("attempting to click menu item with text [" + subMenuItem.getText() + "]");
            getWrapper().click(subMenuItem);
        }
        WebElement item = Locators.menuItem(subMenuLabels[subMenuLabels.length - 1])
                .notHidden()
                .waitForElement(elementCache().findOpenMenu(), WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        getWrapper().mouseOver(item);
        return item;
    }

    @LogMethod(quiet = true)
    public void clickSubMenu(boolean wait, @LoggedParam String ... subMenuLabels)
    {
        if (subMenuLabels.length < 1)
            throw new IllegalArgumentException("Specify menu item(s)");

        WebElement item = openMenuTo(subMenuLabels);
        TestLogger.log("attempting to click menu item with text [" + item.getText() + "]");

        if (wait)
            getWrapper().clickAndWait(item);
        else
            getWrapper().clickAndWait(item, 0);
    }

    /**
     * @deprecated Use {@link #clickSubMenu(boolean, String...)} or {@link #openMenuTo(String...)}
     */
    @Deprecated
    public WebElement clickMenuButton(boolean wait, boolean onlyOpen, @LoggedParam String ... subMenuLabels)
    {
        if (onlyOpen)
            return openMenuTo(subMenuLabels);
        else
            clickSubMenu(wait, subMenuLabels);
        return null;
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    // TODO: Remove and verify that elements in cache won't go stale
    @Override
    protected Elements elementCache()
    {
        return newElementCache();
    }

    protected class Elements extends Component.ElementCache
    {
        public WebElement toggleAnchor = Locators.toggleAnchor().findWhenNeeded(getComponentElement());

        public WebElement findOpenMenu()
        {
            WebElement insideContainerList = Locator.tagWithClassContaining("ul", "dropdown-menu")
                    .findElementOrNull(getComponentElement());
            if (insideContainerList != null)
                return insideContainerList;

            // outside the container, require it to be block-display,
            // as is the case with dataRegion header menus.
            return Locator.tagWithClassContaining("ul", "dropdown-menu")
                    .notHidden()
                    .withAttributeContaining("style", "display: block")
                    .findElement(getDriver());
        }

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
    }

    static public class Locators
    {
        public static Locator.XPathLocator toggleAnchor()
        {
            return Locator.tagWithAttribute("*", "data-toggle", "dropdown");
        }

        public static Locator.XPathLocator menuItem(String text)
        {
            return Locator.tag("li").childTag("a").withText(text).notHidden();
        }

        public static Locator.XPathLocator menuItemDisabled(String text)
        {
            return Locator.tagWithClass("li", "disabled").childTag("a").withText(text).notHidden();
        }

        public static Locator.XPathLocator bootstrapMenuContainer()
        {
            return Locator.tag("*")
                    .withPredicate(toggleAnchor()
                    .followingSibling("ul"));
        }
    }
}
