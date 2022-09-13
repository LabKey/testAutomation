/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.react;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MultiMenu extends BootstrapMenu
{
    protected MultiMenu(WebElement element, WebDriver driver)
    {
        super(driver, element);
    }

    private WebElement getMenuList()
    {
        return Locator.tagWithClass("ul", "dropdown-menu").findElement(this);
    }

    public WebElement getMenuItem(String text)
    {
        return Locator.tag("li").withChild(Locator.tagWithAttribute("a", "role", "menuitem")
                .withText(text)).findElement(getMenuList());
    }

    public boolean isMenuItemDisabled(String text)
    {
        return getMenuItem(text).getAttribute("class").toLowerCase().contains("disabled");
    }

    /**
     * Send in a list of menu text to click, they will be clicked in the order given.
     *
     * @param pathToAction List of the menus to click
     **/
    @LogMethod(quiet = true)
    public void doMenuAction(@LoggedParam List<String> pathToAction)
    {
        expand();

        WebElement menuList = getMenuList();

        for (int i = 0; i < pathToAction.size(); i++)
        {
            Assert.assertFalse("Menu item not enabled.", isMenuItemDisabled(pathToAction.get(i)));

            WebElement menuItem = getMenuItem(pathToAction.get(i));

            if (i < pathToAction.size() - 1)
            {
                // Everything in the pathToAction should contain a sub menu except possibly the last item.
                Assert.assertTrue("Item in menu path '" + pathToAction.get(i) + "' does not contain a sub-menu.", menuItem.getAttribute("class").contains("dropdown-submenu"));
                if(!Locator.xpath("./i").findElement(menuItem).getAttribute("class").contains("fa-chevron-up"))
                {
                    // This is a sub-menu item, but click it only if the sub-menu is not expanded.
                    menuItem.click();
                }
            }
            else // Last item+
            {
                menuItem.click();
            }
        }
    }

    private List<WebElement> waitForData()
    {
        List<WebElement> topMenuList;
        List<String> menuText = new ArrayList<>();
        RuntimeException lastException;

        int tries = 1;

        do {

            try
            {
                lastException = null;
                boolean loading = false;

                topMenuList = Locator.tagWithAttribute("a", "role", "menuitem").findElements(this);
                menuText = new ArrayList<>();

                for (WebElement menuItem : topMenuList) {

                    String menuItemText = menuItem.getText();

                    // Check to see if we are still loading data.
                    if(menuItemText.toLowerCase().contains("loading"))
                        loading = true;

                    menuText.add(menuItem.getText());

                }

                if (!loading)
                {
                    return topMenuList;
                }
            }
            catch (StaleElementReferenceException ex)
            {
                lastException = ex;
                // This happens in the time between the change of the menu content from containing "loading"
                // to having data (like "sample types").
                getComponentElement().isEnabled(); // will throw an uncaught 'StaleReferenceException' if the entire menu went stale
            }

            // Just a small pause, no need to keep hitting the DOM if it looks like the server is doing something
            WebDriverWrapper.sleep(500);

            tries++;

            // Keep trying until we get valid menu text.
        } while(tries <= 10);

        if (lastException != null)
        {
            throw new RuntimeException("Menu items kept going stale.", lastException);
        }
        else
        {
            throw new RuntimeException("Menu items still loading: " + menuText);
        }
    }

    private void expandAll()
    {
        expand();

        List<WebElement> menuItems = waitForData();

        for (WebElement menuItem : menuItems)
        {
            menuItem = Locator.xpath("..").findElement(menuItem); // Up a level
            Optional<WebElement> expando = Locator.xpath("./i").withClass("fa-chevron-down").findOptionalElement(menuItem);
            if (expando.isPresent())
            {
                expando.get().click();
                Locator.tagWithClass("ul", "well").waitForElement(menuItem, 1_000);
            }
        }
    }

    public void doMenuAction(String ... subMenuLabels)
    {
        doMenuAction(Arrays.asList(subMenuLabels));
    }

    public List<String> getMenuText()
    {
        expandAll();

        List<WebElement> menuList = Locator.tagWithAttribute("a", "role", "menuitem").findElements(this);
        List<String> menuText = new ArrayList<>();
        for(WebElement menuItem : menuList)
        {
            menuText.add(menuItem.getText());
        }

        collapse();

        return menuText;
    }

    public List<String> getItemsUnderHeading(String heading)
    {
        expandAll();

        WebElement submenu = Locator.byClass("dropdown-header").withText(heading)
                .followingSibling("li").withClass("dropdown-submenu").findElement(this);
        List<WebElement> menuList = Locator.tagWithAttribute("a", "role", "menuitem").findElements(submenu);
        List<String> menuText = getWrapper().getTexts(menuList);

        collapse();

        return menuText;
    }

    public String getButtonText()
    {
        return getComponentElement().getText();
    }

    public static abstract class Locators
    {
        static public Locator.XPathLocator menuContainer()
        {
            return Locator.tagWithClass("*", "dropdown");
        }

        static public Locator.XPathLocator menuContainer(String text)
        {
            return menuContainer().withChild(BootstrapMenu.Locators.dropdownToggle().withText(text));
        }
    }

    public static class MultiMenuFinder extends WebDriverComponent.WebDriverComponentFinder<MultiMenu, MultiMenuFinder>
    {
        private Locator _locator;

        public MultiMenuFinder(WebDriver driver)
        {
            super(driver);
            _locator = MultiMenu.Locators.menuContainer();
        }

        public MultiMenuFinder withText(String text)
        {
            _locator = MultiMenu.Locators.menuContainer(text);
            return this;
        }

        public MultiMenuFinder withButtonId(String id)
        {
            _locator = Locators.menuContainer().withChild(Locator.id(id));
            return this;
        }

        public MultiMenuFinder withButtonClass(String cls)
        {
            _locator = Locators.menuContainer().withChild(Locator.tagWithClass("button", cls));
            return this;
        }

        /**
         * Looks for a menu with an 'fa-*' icon instead of text
         */
        public MultiMenuFinder withButtonIcon(String iconClass)
        {
            _locator = Locators.menuContainer().withChild(BootstrapMenu.Locators.dropdownToggle().withChild(Locator.byClass(iconClass)));
            return this;
        }

        @Override
        protected MultiMenuFinder getThis()
        {
            return this;
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }

        @Override
        protected MultiMenu construct(WebElement el, WebDriver driver)
        {
            return new MultiMenu(el, driver);
        }
    }
}
