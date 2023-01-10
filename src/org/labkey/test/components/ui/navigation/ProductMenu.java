/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.ui.navigation;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.react.BaseBootstrapMenu;
import org.labkey.test.components.react.MultiMenu;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductMenu extends BaseBootstrapMenu
{

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
        TestLogger.debug(String.format("Product menu expansion state: aria-expanded is %b, menuContentDisplayed is %b.",
                ariaExpanded, menuContentDisplayed));

        return  ariaExpanded && menuContentDisplayed &&
                ExpectedConditions.invisibilityOfAllElements(BootstrapLocators.loadingSpinner.findElements(this)).apply(getDriver());
    }

    public List<String> getMenuSectionHeaders()
    {
        expand();
        return elementCache().menuSectionHeaderElements().stream().map(el -> el.getText().trim())
                .collect(Collectors.toList());
    }

    public Map<String, String> getMenuSectionHeaderLinks()
    {
        expand();
        List<WebElement> headerElements = elementCache().menuSectionHeaderElements();
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

    public boolean hasFolderColumn()
    {
        expand();
        return elementCache().folderColumn().isDisplayed();
    }

    public List<String> getFolderList()
    {
        expand();

        // Use .collect(Collectors.toList()) to allow the returned list to be manipulated if needed.
        return elementCache().folderMenuItems()
                .stream()
                .map(WebElement::getText).collect(Collectors.toList());
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
        private final WebElement menuContent = Locator.tagWithClass("div", "product-menu-content").refindWhenNeeded(this);
        private final WebElement sectionContent = Locator.tagWithClass("div", "sections-content").refindWhenNeeded(menuContent);

        Locator.XPathLocator menuSectionHeaderLoc(String headerText)
        {
            return Locator.tagWithClass("li", "menu-section-header").endsWith(headerText);
        }

        List<WebElement> menuSectionHeaderElements()
        {
            return Locator.tagWithClass("li", "menu-section-header").findElements(sectionContent);
        }

        WebElement menuSectionHeader(String headerText)
        {
            return menuSectionHeaderLoc(headerText).child(Locator.linkContainingText(headerText)).refindWhenNeeded(sectionContent);
        }

        WebElement menuSectionBody(String headerText)
        {
            return menuSectionHeaderLoc(headerText).parent("ul").findElement(sectionContent);
        }

        WebElement menuSectionLink(String headerText, String linkText)
        {
            return Locator.linkWithText(linkText).findElement(menuSectionBody(headerText));
        }

        WebElement folderColumn()
        {
            return Locator.tagWithClass("div", "col-folders").refindWhenNeeded(menuContent);
        }

        private final Locator folderMenuItemLocator = Locator.tagWithClass("a", "menu-folder-item");

        List<WebElement> folderMenuItems()
        {
            return folderMenuItemLocator.findElements(folderColumn());
        }

        WebElement folderItemLink(String folderName)
        {
            return folderMenuItemLocator.withText(folderName).findElement(folderColumn());
        }
    }
}
