/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MultiMenu extends BootstrapMenu
{
    private int _menuWaitTimeout = 2000;

    protected MultiMenu(WebElement element, WebDriver driver)
    {
        super(driver, element);
    }

    /**
     * Sets the amount of time the component will wait for menu or toggle items to appear when interacting with them
     * @param menuWait time in milliseconds to wait for menu items or toggle items to appear
     * @return the current instance
     */
    public MultiMenu setMenuWait(int menuWait)
    {
        _menuWaitTimeout = menuWait;
        return this;
    }

    private WebElement getMenuList()
    {
        return Locator.tagWithClass("ul", "dropdown-menu").findElement(this);
    }

    /**
     * finds the first menu item with the specified text.
     * If there are duplicate items in the menu by text and the one you want isn't the first in the menu,
     * consider using getMenuItemUnderToggle or getMenuItemUnderHeading
     * @param menuItem Text of the intended menuItem
     * @return
     */
    public WebElement getMenuItem(String menuItem)
    {
        expand();
        waitForData();
        return Locators.menuItem().withText(menuItem).waitForElement(getMenuList(), _menuWaitTimeout);
    }

    /**
     * Checks whether the specified menu item is disabled
     * @param menuItem Text of the menu item
     * @return  true if the item is not enabled
     */
    public boolean isMenuItemDisabled(String menuItem)
    {
        return getMenuItem(menuItem).getAttribute("class").contains("disabled");
    }

    /**
     * Checks whether the specified menu item is disabled
     * @param toggle Text of the toggle under which to find the menu item
     * @param menuItem  Text of the menu item
     * @return  true if the item is not enabled
     */
    public boolean isMenuItemUnderToggleDisabled(String toggle, String menuItem)
    {
        return getMenuItemUnderToggle(toggle, menuItem).getAttribute("class").contains("disabled");
    }

    /**
     * gets all list-items currently appearing in the menu.  This includes header and separator items,
     * which are special and don't have links and can have classes other than lk-menu-item, such as divider
     * @return all list-item elements in the menu-list container
     */
    protected List<WebElement> getListItems()
    {
        return Locators.listItem().findElements(getMenuList());
    }

    /**
     * Click a single top-level menu-item
     *
     * @param menuAction The menu item to click
     **/
    @LogMethod(quiet = true)
    public void doMenuAction(@LoggedParam String menuAction)
    {
        var item = getMenuItem(menuAction);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(item));
        item.click();
    }

    /**
     *  Click a menu item under a menu
     * @param toggleText Text of the toggle to expand
     * @param menuAction Text of the menu item to click
     */
    public void doMenuAction(@LoggedParam String toggleText, @LoggedParam String menuAction)
    {
        clickMenuItemUnderToggle(toggleText, menuAction);
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
            Optional<WebElement> expando = Locator.byClass("fa-chevron-down").findOptionalElement(menuItem);
            if (expando.isPresent())
            {
                expando.get().click();
                // Note: if we really do need to wait for the items to appear we can probably wait for elements with
                // class "dropdown-section__menu-item", we'll want to track how many we already have and check that the
                // number has increased, since nothing is nested, everything is siblings.
            }
        }
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

    /**
     * Finds and expands (if collapsed) a menu's dropdown-section-toggle
     * @param toggle Text of the toggle to expand
     * @return  The toggle element
     */
    public WebElement expandToggle(String toggle)
    {
        expand();
        waitForData();
        int listItemCount = getListItems().size();

        // find the toggle-item; it may be expanded already
        WebElement toggleElement = Locators.menuToggle(toggle).waitForElement(this, _menuWaitTimeout);
        if (Locators.menuToggleClosed().existsIn(toggleElement))
        {
            toggleElement.click(); // expand the toggle
            WebDriverWrapper.waitFor(() -> Locators.menuToggleOpened().existsIn(toggleElement),
                    "the toggle-item did not expand in time", 1000);
            WebDriverWrapper.waitFor(()-> getListItems().size() > listItemCount,
                    "the list-items did not appear in the list after expanding", 1000);
        }

        return toggleElement;
    }

    /**
     * gets the names of dropdown-section__menu-items between a heading and the next separator
     *
     * @param heading Text of the lk-dropdown-header under which to find menu items
     * @param collapse  whether to collapse the menu after
     * @return
     */
    public List<String> getMenuItemsUnderHeading(String heading, boolean collapse)
    {
        var menuItemTexts = getWrapper().getTexts(getMenuItemsUnderHeading(heading));
        if (collapse)
            collapse();
        return menuItemTexts;
    }

    /**
     * gets the dropdown-section__menu-items between a heading and the next separator
     *
     * @param heading Text of the lk-dropdown-header under which to find menu items
     * @return
     */
    protected List<WebElement> getMenuItemsUnderHeading(String heading)
    {
        expandAll();
        boolean headingFound = false;
        List<WebElement> itemsUnderHeading = new ArrayList<>();
        List<WebElement> listItems = Locator.tag("li").findElements(this);

        for (WebElement item : listItems)
        {
            String className = item.getAttribute("class");
            String role = item.getAttribute("role");
            String text = item.getText().trim();

            if (className.contains("dropdown-header") && text.equalsIgnoreCase(heading))
                headingFound = true;

            // Once we've found our header we know that all presentation elements belong to the heading
            // we are interested in
            if (headingFound && role.equals("presentation"))
                itemsUnderHeading.add(item);

            // Once we hit a divider we're done looking at menu items related to the heading, so we can stop iterating
            if (headingFound && role.equals("separator"))
                break;
        }

        return itemsUnderHeading;
    }

    public WebElement getMenuItemUnderHeading(String heading, String menuItem)
    {
        var itemsUnderHeading = getMenuItemsUnderHeading(heading);
        for (WebElement item : itemsUnderHeading)
        {
            if (item.getText().trim().equals(menuItem))
                return item;
        }
        throw new NoSuchElementException("No item '" + menuItem + "' under heading '" + heading + "' found.");
    }

    /**
     * Clicks the specified item under the specified heading
     *
     * @param heading heading under which to find the item
     * @param menuItem   text of the menuItem
     */
    public void clickMenuItemUnderHeading(String heading, String menuItem)
    {
        WebElement item = getMenuItemUnderHeading(heading, menuItem);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(item));
        item.click();
    }

    /**
     * gets the names of dropdown-section__menu-items between a toggle and the next toggle or divider
     *
     * @param toggle The text of the toggle
     * @param collapse Whether to collapse the menu when finished
     * @return texts of menu items under toggle
     */
    public List<String> getMenuItemsUnderToggle(String toggle, boolean collapse)
    {
        var itemTexts = getWrapper().getTexts(getMenuItemsUnderToggle(toggle));
        if (collapse)
            collapse();
        return itemTexts;
    }

    /**
     * Gets a list of dropdown-section__menu-items under a given toggle.
     *
     * @param toggle Text of the toggle item
     * @return A list of menu items under the specified toggle.
     */
    protected List<WebElement> getMenuItemsUnderToggle(String toggle)
    {
        expandToggle(toggle);

        List<WebElement> itemsUnderToggle = new ArrayList<>();

        // iterate from the top and generate the list of items that follow it
        List<WebElement> listItems = getListItems();
        boolean headingFound = false;

        for (WebElement item : listItems)
        {
            String className = item.getAttribute("class");
            String text = item.getText().trim();

            if (className.contains("dropdown-section-toggle") && text.equalsIgnoreCase(toggle))
                headingFound = true;

            // Once we've found our toggle we know that all dropdown-section__menu-item elements belong to the heading
            // we are interested in
            if (headingFound && className.contains("dropdown-section__menu-item"))
                itemsUnderToggle.add(item);

            // Once we hit another divider or another toggle we're done looking at menu items related to the toggle, so we can stop iterating
            if (headingFound && !text.equalsIgnoreCase(toggle) && className.contains("dropdown-section-toggle"))
                break;
        }

        return itemsUnderToggle;
    }

    /**
     * Gets the first menu item with the specified text under the specifed toggle
     * @param toggle    Text of the toggle to expand
     * @param menuItem  Text of the menu-item to get
     * @return  The menu-item element, if found
     */
    public WebElement getMenuItemUnderToggle(String toggle, String menuItem)
    {
        var itemsUnderToggle = getMenuItemsUnderToggle(toggle);
        for (WebElement item : itemsUnderToggle)
        {
            if (item.getText().trim().equals(menuItem))
                return item;
        }
        throw new NoSuchElementException("No item '" + menuItem + "' under toggle '" + toggle + "' found.");
    }

    /**
     * clicks the dropdown-section__menu_item under the specified toggle
     *
     * @param toggle text of the toggle under which to find the item
     * @param menuItem       text of the item to click
     */
    protected void clickMenuItemUnderToggle(String toggle, String menuItem)
    {
        var item = getMenuItemUnderToggle(toggle, menuItem);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(item));
        item.click();
    }

    public String getButtonText()
    {
        return getComponentElement().getText();
    }

    public static abstract class Locators
    {
        static public Locator.XPathLocator menuContainer()
        {
            return Locator.XPathLocator.union(Locator.byClass("dropdown"), Locator.byClass("dropup"));
        }

        static public Locator.XPathLocator menuContainer(String text)
        {
            return menuContainer().withChild(dropdownToggle().withText(text));
        }

        // finds the toggle to expand/collapse the root menu
        public static Locator.XPathLocator dropdownToggle()
        {
            return Locator.byClass("dropdown-toggle");
        }

        public static Locator.XPathLocator dropdownHeader()
        {
            return Locator.tagWithClass("li", "dropdown-header");
        }

        // finds a menu-item
        public static Locator.XPathLocator menuItem()
        {
            return Locator.tag("li").withChild(Locator.tagWithAttribute("a", "role", "menuitem"));
        }

        // finds any list-item (includes separators and header items)
        public static Locator.XPathLocator listItem()
        {
            return Locator.tag("li");
        }

        // finds a menu-item that is also an expand/collapse, by name
        public static Locator.XPathLocator menuToggle(String toggle)
        {
            return Locator.tagWithClass("li", "dropdown-section-toggle")
                    .withDescendant(Locator.tagWithClass("span", "dropdown-section-toggle__text")
                            .withText(toggle));
        }

        // finds the chevron-down span in a menuToggle, if present
        public static Locator.XPathLocator menuToggleClosed()
        {
            return Locator.tagWithClass("span", "fa-chevron-down");
        }

        // finds the chevron-up span in a menuToggle, if present
        public static Locator.XPathLocator menuToggleOpened()
        {
            return Locator.tagWithClass("span", "fa-chevron-up");
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

        public MultiMenuFinder withClass(String cls)
        {
            _locator = MultiMenu.Locators.menuContainer().withClass(cls);
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
