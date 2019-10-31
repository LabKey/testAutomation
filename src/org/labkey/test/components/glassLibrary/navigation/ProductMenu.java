/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.glassLibrary.navigation;

import org.labkey.test.Locator;
import org.labkey.test.components.glassLibrary.components.MenuFinder;
import org.labkey.test.components.html.BaseBootstrapMenu;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductMenu extends BaseBootstrapMenu
{
    private final static Locator MENU_CONTENT_LOC = Locator.tagWithClass("div", "product-menu-content");
    private final static Locator MENU_SECTION_LOC = Locator.byClass("menu-section");
    private int expectedSectionCount = 1;

    protected ProductMenu(WebElement element, WebDriver driver)
    {
        super(driver, element);
    }

    public static MenuFinder<ProductMenu> finder(WebDriver driver)
    {
        return new MenuFinder<ProductMenu>(driver)
        {
            @Override
            protected ProductMenu construct(WebElement el, WebDriver driver)
            {
                return new ProductMenu(el, driver);
            }
        };
    }

    @Override
    protected boolean isExpanded()
    {
        return super.isExpanded()
                && elementCache().menuContent.isDisplayed()
                && MENU_SECTION_LOC.findElements(this).size() >= getExpectedSectionCount();
    }

    public ProductMenu setExpectedSectionCount(int expectedSectionCount)
    {
        this.expectedSectionCount = expectedSectionCount;
        return this;
    }

    protected int getExpectedSectionCount()
    {
        return expectedSectionCount;
    }

    public List<String> getColumnHeaders()
    {
        expand();
        List<WebElement> headersElements = Locator.tagWithClass("span", "menu-section-header").findElements(this);
        return getWrapper().getTexts(headersElements);
    }

    public void clickMenuColumnHeader(String headerText)
    {
        expand();
        elementCache().menuSectionHeader(headerText).click();
    }

    public List<String> getMenuText(String headerText)
    {
        expand();
        String rawMenuText = elementCache().menuSectionBody(headerText).getText();

        return Arrays.asList(rawMenuText.split("\n"));
    }

    public void clickMenuItem(String headerText, String menuText)
    {
        expand();
        elementCache().menuSectionLink(headerText, menuText).click();
    }

    public boolean sectionHasOverflowLink(String headerText)
    {
        expand();
        try
        {
            return elementCache().overFlowLink(headerText).isDisplayed();
        }
        catch(NoSuchElementException nse)
        {
            return false;
        }
    }

    public void clickOverflowLink(String headerText)
    {
        expand();
        elementCache().overFlowLink(headerText).click();
    }

    @Override
    protected Locator getToggleLocator()
    {
        return Locator.tagWithId("button", "product-menu");
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
        private final WebElement menuContent = Locator.tagWithClass("div", "product-menu-content").findWhenNeeded(this);

        private final Map<String, WebElement> menuSections = new HashMap<>();
        WebElement menuSection(String headerText)
        {
            return Locator.tagWithClass("div", "menu-section")
                    .withChild(Locator.tagWithClass("span", "menu-section-header").withText(Locator.NBSP + headerText))
                    .findElement(menuContent);
        }

        WebElement menuSectionHeader(String headerText)
        {
            return Locator.byClass("menu-section-header").findElement(menuSection(headerText));
        }

        WebElement menuSectionBody(String headerText)
        {
            return Locator.xpath("./ul").findElement(menuSection(headerText));
        }

        WebElement menuSectionLink(String headerText, String linkText)
        {
            return Locator.linkWithText(linkText).findElement(menuSection(headerText));
        }

        WebElement overFlowLink(String headerText)
        {
            return Locator.byClass("overflow-link").findElement(menuSection(headerText));
        }
    }
}
