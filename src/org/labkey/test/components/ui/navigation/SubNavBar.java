/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.ui.navigation;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class SubNavBar extends WebDriverComponent<SubNavBar.ElementCache>
{
    final WebElement _subNavBarElement;
    final WebDriver _driver;

    public SubNavBar(WebDriver driver)
    {
        this(Locators.component.findElement(driver), driver);
    }

    private SubNavBar(WebElement element, WebDriver driver)
    {
        _subNavBarElement = element;
        _driver = driver;
    }

    static public SubNavBarFinder finder(WebDriver driver)
    {
        return new SubNavBarFinder(driver);
    }

    public boolean hasTab(String tabText)
    {
        return getWrapper().isElementPresent(elementCache().getTabLocator(tabText));
    }

    public void clickTab(String tabText)
    {
        elementCache().getTab(tabText).click();
    }

    public String getActiveTab()
    {
        return elementCache().activeTab.getText();
    }

    public String getParentTabText()
    {
        return elementCache().parentNav.getText();
    }

    public void clickParentTab()
    {
        elementCache().parentNav.click();
    }

    public boolean isScrollVisible()
    {
        if(getWrapper().isElementPresent(Locators.scrollButtonGroup))
            return getWrapper().isElementVisible(Locators.scrollButtonGroup);
        else
            return false;
    }

    public List<String> getMenuText()
    {
        List<String> menuText = new ArrayList<>();

        List<WebElement> menuElements = elementCache().tabScrollContainer.findElements(By.tagName("li"));
        menuElements.forEach(e -> menuText.add(e.getText()));
        return menuText;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _subNavBarElement;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        WebElement parentNav = Locator.css("div.parent-nav > ul.navbar-nav > li > a").findWhenNeeded(this);
        WebElement tabScrollContainer = Locator.tagWithClass("div", "tab-scroll-ct").findWhenNeeded(this);
        WebElement activeTab = Locator.css("ul.navbar-nav > li.active").findWhenNeeded(this);

        Locator.XPathLocator getTabLocator(String text)
        {
            return Locator.tagWithClass("ul", "nav navbar-nav")
                    .child(Locator.tag("li")).withText(text);
        }

        WebElement getTab(String text)
        {
            return getTabLocator(text).findElement(this);
        }

    }

    static protected class Locators
    {
        static Locator.XPathLocator component = Locator.tagWithClass("nav", "sub-nav")
                .child(Locator.tagWithClass("div", "sub-nav-container"));
        static Locator scrollButtonGroup = Locator.xpath("//nav[contains(@class,'sub-nav')]//div[contains(@class,'scroll-btn-group')]");
    }

    // Extend simpleComponentFinder
    public static class SubNavBarFinder extends WebDriverComponentFinder<SubNavBar, SubNavBarFinder>
    {
        private Locator.XPathLocator _locator;

        private SubNavBarFinder(WebDriver driver)
        {
            super(driver);
            _locator = Locators.component;
        }

        @Override
        protected SubNavBar construct(WebElement el, WebDriver driver)
        {
            return new SubNavBar(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }

}
