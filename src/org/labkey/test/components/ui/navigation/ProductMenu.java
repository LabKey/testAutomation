/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.ui.navigation;

import org.labkey.test.Locator;
import org.labkey.test.components.react.BaseBootstrapMenu;
import org.labkey.test.components.react.MultiMenu;
import org.labkey.test.util.TestLogger;
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
        super(element, driver);
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
        TestLogger.debug(String.format("product menu expansion state: aria-expanded is %b, menuContentDisplayed is %b, %d of %d expected header sections are present",
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

    public List<String> getMenuSectionHeaders()
    {
        expand();
        List<WebElement> headersElements = MENU_SECTION_HEADER_LOC.findElements(this);
        return getWrapper().getTexts(headersElements).stream().map(String::trim).collect(Collectors.toList());
    }

    public Map<String, String> getMenuSectionHeaderLinks()
    {
        expand();
        List<WebElement> headerElements = MENU_SECTION_HEADER_LOC.findElements(this);
        Map<String, String> links = new HashMap<>();
        headerElements.forEach((header) -> {
            links.put(header.getText().trim(),Locator.tag("a").findElement(header).getAttribute("href"));
        });
        return links;
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

    public List<String> getMenuSectionLinks(String headerText)
    {
        expand();
        return Locator.tag("li").childTag("a").findElements(elementCache().menuSectionBody(headerText))
                .stream()
                .map(element -> element.getAttribute("href"))
                .collect(Collectors.toList());
    }

    public void clickMenuItem(String headerText, String menuText)
    {
        expand();
        elementCache().menuSectionLink(headerText, menuText).click();
    }

    public ProductMenu clickFolderItem(String folderName)
    {
        expand();
        elementCache().folderItemLink(folderName).click();
        return this;
    }

    public void goToFolder(String folderName)
    {
        clickFolderItem(folderName);
        clickMenuColumnHeader("Dashboard");
    }

    public String getButtonTitle()
    {
        WebElement buttonTitle = Locator.tagWithId("button", "product-menu")
                .child(Locator.tagWithClass("div", "title")).findElement(this);
        return buttonTitle.getText();
    }

    public String getButtonSubtitle()
    {
        WebElement buttonSubtitle = Locator.tagWithId("button", "product-menu")
                .child(Locator.tagWithClass("div", "subtitle")).findElement(this);
        return buttonSubtitle.getText();
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

        Locator.XPathLocator menuSectionHeaderLoc(String headerText)
        {
            return Locator.tagWithClass("div", "menu-section")
                    .child(Locator.tag("ul"))
                    .child(Locator.tagWithClass("li", "menu-section-header").endsWith(headerText));
        }

        WebElement menuSectionHeader(String headerText)
        {
            return menuSectionHeaderLoc(headerText).child(Locator.linkContainingText(headerText)).findElement(elementCache().menuContent);
        }

        WebElement menuSectionBody(String headerText)
        {
            return menuSectionHeaderLoc(headerText).parent("ul").findElement(elementCache().menuContent);
        }

        WebElement menuSectionLink(String headerText, String linkText)
        {
            return Locator.linkWithText(linkText).findElement(menuSectionBody(headerText));
        }

        WebElement folderItemLink(String folderName)
        {
            return Locator.tagWithClass("a", "menu-folder-item").withText(folderName).findElement(elementCache().menuContent);
        }
    }
}
