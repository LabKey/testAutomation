/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.glassLibrary.components;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiMenu extends BootstrapMenu
{
    protected MultiMenu(WebElement element, WebDriver driver)
    {
        super(driver, element);
    }

    /**
     * Send in a list of menu text to click, they will be clicked in the order given.
     *
     * @param pathToAction List of the menus to click
     * @return void
     **/
    public void doMenuAction(List<String> pathToAction)
    {
        expand();

        WebElement menuList = Locator.tagWithClass("ul", "dropdown-menu").findElement(this);

        for (int i = 0; i < pathToAction.size(); i++)
        {

            WebElement menuItem = Locator.tag("li").withChild(Locator.tagWithAttribute("a", "role", "menuitem").withText(pathToAction.get(i))).findElement(menuList);

            Assert.assertFalse("Menu item not enabled.", menuItem.getAttribute("class").toLowerCase().contains("disabled"));

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

    private List<String> waitForDataThenGetMenuText()
    {
        List<WebElement> topMenuList;
        List<String> menuText = new ArrayList<>();

        boolean stale;
        boolean loading;
        int tries = 1;

        do {

            stale = false;
            loading = false;

            try
            {

                topMenuList = Locator.tagWithAttribute("a", "role", "menuitem").findElements(this);
                menuText = new ArrayList<>();

                for (WebElement menuItem : topMenuList) {

                    String menuItemText = menuItem.getText();

                    // Check to see if we are still loading data.
                    if(menuItemText.toLowerCase().contains("loading"))
                        loading = true;

                    menuText.add(menuItem.getText());

                }

            }
            catch(StaleElementReferenceException staleExc)
            {
                // This happens in the time between the change of the menu content from containing "loading"
                // to having data (like "sample sets").
                stale = true;
            }

            // Just a small pause, no need to keep hitting the DOM if it looks like the server is doing something
            if(stale || loading )
                WebDriverWrapper.sleep(500);

            tries++;

            // Keep trying until we get valid menu text.
        } while((stale || loading) && (tries <= 10));

        if(tries > 10)
            throw new RuntimeException("Menu text never returned.");

        return menuText;
    }

    private void expandAll()
    {
        expand();

        List<String> menuText = waitForDataThenGetMenuText();

        WebElement menuList = Locator.tagWithClass("ul", "dropdown-menu").findElement(this);

        for (int i = 0; i < menuText.size(); i++)
        {

            WebElement menuItem = Locator.tag("li").withChild(Locator.tagWithAttribute("a", "role", "menuitem").withText(menuText.get(i))).findElement(menuList);

            try
            {
                if (!Locator.xpath("./i").findElement(menuItem).getAttribute("class").contains("fa-chevron-up"))
                {
                    // This is a sub-menu item, but click it only if the sub-menu is not expanded.
                    menuItem.click();
                }
            }
            catch (NoSuchElementException nse)
            {
                // There is no chevron so don't do anything.
            }

        }
    }

    @LogMethod(quiet = true)
    public void doMenuAction(@LoggedParam String ... subMenuLabels)
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

    public String getButtonText()
    {
        return getComponentElement().getText();
    }

    @Override
    protected Locator getToggleLocator()
    {
        return Locators.dropdownToggle();
    }

    protected static abstract class Locators
    {
        static public Locator.XPathLocator menuContainer()
        {
            return Locator.tagWithClass("div", "dropdown");
        }

        static public Locator.XPathLocator menuContainer(String text)
        {
            return menuContainer().withChild(dropdownToggle().withText(text));
        }

        private static Locator.XPathLocator dropdownToggle()
        {
            return Locator.tagWithClass("button", "dropdown-toggle");
        }
    }

    public static class MultiMenuFinder extends MenuFinder<MultiMenu>
    {
        public MultiMenuFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected MultiMenu construct(WebElement el, WebDriver driver)
        {
            return new MultiMenu(el, driver);
        }
    }
}
