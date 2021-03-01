/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.ui.navigation;

import org.labkey.test.Locator;
import org.labkey.test.components.html.BaseBootstrapMenu;
import org.labkey.test.components.react.MultiMenu;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductMenu extends BaseBootstrapMenu
{
    private final static Locator MENU_SECTION_HEADER_LOC = Locator.byClass("menu-section-header");
    private int expectedSectionCount = 1;

    protected ProductMenu(WebElement element, WebDriver driver)
    {
        super(driver, element);
    }

    public static SimpleWebDriverComponentFinder<ProductMenu> finder(WebDriver driver)
    {
        return new MultiMenu.MultiMenuFinder(driver).withButtonId("product-menu").wrap(ProductMenu::new);
    }

    @Override
    protected boolean isExpanded()
    {
        boolean ariaExpanded = super.isExpanded();
        boolean menuContentDisplayed = elementCache().menuContent.isDisplayed();
        int headerSectionCount =  MENU_SECTION_HEADER_LOC.findElements(this).size();
        int expectedHeaderSecionCount = getExpectedSectionCount();
        getWrapper().log(String.format("product menu expansion state: aria-expanded is %b, menuContentDisplayed is %b, %d of %d expected header sections are present",
                ariaExpanded, menuContentDisplayed, headerSectionCount, expectedHeaderSecionCount));

        return  ariaExpanded && menuContentDisplayed && headerSectionCount >= expectedHeaderSecionCount;
    }

    /**
     * the number of menu sections is how many sections (not just columns) this menu will wait for before reporting
     * that it is fully open
     * @param expectedSectionCount the number of sections to expect
     * @return the current instance
     */
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
        return getWrapper().getTexts(headersElements).stream().map(String::trim).collect(Collectors.toList());
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
        Locator.XPathLocator menuSectionHeaderLoc(String headerText)
        {
            return Locator.tagWithClass("div", "menu-section")
                    .child(Locator.tagWithClass("span", "menu-section-header").withText(Locator.NBSP + headerText));
        }

        WebElement menuSectionHeader(String headerText)
        {
            return menuSectionHeaderLoc(headerText).child(Locator.linkWithText(Locator.NBSP + headerText)).findElement(elementCache().menuContent);
        }

        WebElement menuSectionBody(String headerText)
        {
            return menuSectionHeaderLoc(headerText).followingSibling("ul").findElement(elementCache().menuContent);
        }

        WebElement menuSectionLink(String headerText, String linkText)
        {
            return Locator.linkWithText(linkText).findElement(menuSectionBody(headerText));
        }

        WebElement overFlowLink(String headerText)
        {
            return menuSectionHeaderLoc(headerText)
                    .followingSibling("span").withClass("overflow-link").findElement(elementCache().menuContent);
        }
    }
}
