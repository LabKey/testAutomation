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
    private int _menuWaitTmeout = 500;

    protected MultiMenu(WebElement element, WebDriver driver)
    {
        super(driver, element);
    }

    public MultiMenu setMenuWait(int menuWait)
    {
        _menuWaitTmeout = menuWait;
        return this;
    }

    private WebElement getMenuList()
    {
        return Locator.tagWithClass("ul", "dropdown-menu").findElement(this);
    }

    /*
        finds the first menu item with the specified text
     */
    public WebElement getMenuItem(String text)
    {
        return Locators.menuItem().withText(text).waitForElement(getMenuList(), _menuWaitTmeout);
    }

    /**
     * Find the first item with specified text under the path of the
     * @param menuText Text of the menu to expand
     * @param itemText  Text of the item to find under its path
     * @return
     */
    public WebElement getMenuItem(String menuText, String itemText)
    {
        return getItemUnderToggle(menuText, itemText);
    }

    /**
     * Checks whether the specified menu item is disabled
     * @param text Text of the menu item
     * @return  true if class contains 'disabled'
     */
    public boolean isMenuItemDisabled(String text)
    {
        return getMenuItem(text).getAttribute("class").toLowerCase().contains("disabled");
    }

    public boolean isMenuItemDisabled(String menuText, String itemText)
    {
        return getItemUnderToggle(menuText, itemText).getAttribute("class").toLowerCase().contains("disabled");
    }

    /**
     * gets all list-items currently appearing in the menu.  This includes header and separator items,
     * which are special and don't have links
     * @return
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
        expand();
        var item = getMenuItem(menuAction);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(item));
        item.click();
    }

    /**
     *  Click a menu item under a menu
     * @param toggleText Text of the menu to expand
     * @param menuAction Text of the menu item to click
     */
    public void doMenuAction(@LoggedParam String toggleText, @LoggedParam String menuAction)
    {
        expand();
        clickItemUnderToggle(toggleText, menuAction);
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

    /*
       Finds and expands (if it is collapsed) a menu's dropdown-section toggle.
       (Expanding a collapsed dropdown-section-toggle causes items hidden while collapsed
       to be shown as dom peers after it in the list)
    */
    public WebElement expandToggle(String label)
    {
        expand();
        int listItemCount = getListItems().size();

        // find the toggle-item; it may be expanded already
        WebElement toggle = Locators.menuToggle(label).waitForElement(this, 1000);
        if (Locators.menuToggleClosed().existsIn(toggle))
        {
            toggle.click(); // expand the toggle
            WebDriverWrapper.waitFor(() ->
                            Locator.tagWithClass("span", "fa-chevron-up").existsIn(toggle),
                    "the toggle-item did not expand in time", 1000);
            WebDriverWrapper.waitFor(()-> getListItems().size() > listItemCount,
                    "the list-items did not appear in the list after expanding", 1000);
        }

        return toggle;
    }

    /**
     * gets the names of dropdown-section__menu-items between a heading and the next divider
     *
     * @param heading
     * @return
     */
    public List<String> getItemsUnderHeading(String heading, boolean collapse)
    {
        var itemTexts = getWrapper().getTexts(getItemsUnderHeading(heading));
        if (collapse)
            collapse();
        return itemTexts;
    }

    /**
     * gets the dropdown-section__menu-items between a heading and the next separator
     *
     * @param heading
     * @return
     */
    protected List<WebElement> getItemsUnderHeading(String heading)
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

    private WebElement getItemUnderHeading(String toggleLabel, String label)
    {
        var itemsUnderHeading = getItemsUnderHeading(toggleLabel);
        for (WebElement item : itemsUnderHeading)
        {
            if (item.getText().trim().equals(label))
                return item;
        }
        throw new NoSuchElementException("No item '" + label + "' under toggle '" + toggleLabel + "' found.");
    }

    /**
     * Clicks the specified item under the specified heading
     *
     * @param heading heading under which to find the item
     * @param label   text of the item
     */
    public void clickItemUnderHeading(String heading, String label)
    {
        WebElement item = getItemUnderHeading(heading, label);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(item));
        item.click();
    }

    /**
     * gets the names of dropdown-section__menu-items between a heading and the next divider
     *
     * @param heading The text of the heading
     * @param collapse Whether to collapse the menu when finished
     * @return
     */
    public List<String> getItemsUnderToggle(String heading, boolean collapse)
    {
        var itemTexts = getWrapper().getTexts(getItemsUnderToggle(heading));
        if (collapse)
            collapse();
        return itemTexts;
    }

    /**
     * Gets a list of dropdown-section__menu-items under a given toggle. For cases where the menu has duplicate
     * menu-items and you need to select one under a specific toggle, use this to find it
     *
     * @param label Text of the toggle item
     * @return A list of menu items under the specified toggle.
     */
    protected List<WebElement> getItemsUnderToggle(String label)
    {
        expand();
        expandToggle(label);
        waitForData();

        List<WebElement> itemsUnderToggle = new ArrayList<>();

        // iterate from the top and generate the list of items that follow it
        List<WebElement> listItems = getListItems();
        boolean headingFound = false;

        for (WebElement item : listItems)
        {
            String className = item.getAttribute("class");
            String text = item.getText().trim();

            if (className.contains("dropdown-section-toggle") && text.equalsIgnoreCase(label))
                headingFound = true;

            // Once we've found our toggle we know that all dropdown-section__menu-item elements belong to the heading
            // we are interested in
            if (headingFound && className.contains("dropdown-section__menu-item"))
                itemsUnderToggle.add(item);

            // Once we hit another divider or another toggle we're done looking at menu items related to the toggle, so we can stop iterating
            if (headingFound && !text.equalsIgnoreCase(label) && className.contains("dropdown-section-toggle"))
                break;
        }

        return itemsUnderToggle;
    }

    private WebElement getItemUnderToggle(String toggleLabel, String label)
    {
        waitForData();
        var itemsUnderToggle = getItemsUnderToggle(toggleLabel);
        for (WebElement item : itemsUnderToggle)
        {
            if (item.getText().trim().equals(label))
                return item;
        }
        throw new NoSuchElementException("No item '" + label + "' under toggle '" + toggleLabel + "' found.");
    }

    /**
     * clicks the dropdown-section__menu_item under the specified toggle
     *
     * @param toggleLabel text of the toggle under which to find the item
     * @param label       text of the item to click
     */
    protected void clickItemUnderToggle(String toggleLabel, String label)
    {
        var item = getItemUnderToggle(toggleLabel, label);
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
        public static Locator.XPathLocator menuToggle(String text)
        {
            return Locator.tagWithClass("li", "dropdown-section-toggle")
                    .withDescendant(Locator.tagWithClass("span", "dropdown-section-toggle__text")
                            .withText(text));
        }

        // finds the chevron-down span in a menuToggle, if present
        public static Locator.XPathLocator menuToggleClosed()
        {
            return Locator.tagWithClass("span", "fa-chevron-down");
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
