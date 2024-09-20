/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.ui.navigation;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductMenu extends WebDriverComponent<ProductMenu.ElementCache>
{
    private final WebElement _componentElement;
    private final WebDriver _driver;

    protected ProductMenu(WebElement element, WebDriver driver)
    {
        _componentElement = element;
        _driver = driver;
    }

    public static SimpleWebDriverComponentFinder<ProductMenu> finder(WebDriver driver)
    {
        return new SimpleWebDriverComponentFinder<>(driver, rootLocator, ProductMenu::new);
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

    protected boolean isExpanded()
    {
        boolean ariaExpanded = "true".equals(elementCache().toggle.getAttribute("aria-expanded"));
        boolean menuContentDisplayed = elementCache().menuContent.isDisplayed();
        TestLogger.debug(String.format("Product menu expansion state: aria-expanded is %b, menuContentDisplayed is %b.",
                ariaExpanded, menuContentDisplayed));

        return  ariaExpanded && menuContentDisplayed &&
                ExpectedConditions.invisibilityOfAllElements(BootstrapLocators.loadingSpinner.findElements(this)).apply(getDriver());
    }

    public void expand()
    {
        if (!isExpanded())
        {
            clearElementCache();
            elementCache().toggle.click();
            WebDriverWrapper.waitFor(this::isExpanded, "AppsMenu did not expand as expected", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        }
    }

    public void collapse()
    {
        if (isExpanded())
        {
            elementCache().toggle.click();
        }
        clearElementCache();
    }

    public List<String> getMenuSectionHeaders()
    {
        expand();
        return elementCache().menuSectionHeaderElements().stream().map(el -> el.getText().trim()).toList();
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
        clickNavLink(elementCache().menuSectionHeader(headerText));
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
                .toList();
    }

    public void clickMenuItem(String headerText, String menuText)
    {
        expand();
        clickNavLink(elementCache().menuSectionLink(headerText, menuText));
    }

    public boolean hasFolderColumn()
    {
        expand();
        return elementCache().folderColumn.isDisplayed();
    }

    public List<String> getFolderList()
    {
        expand();

        // Use .collect(Collectors.toList()) to allow the returned list to be manipulated if needed.
        return elementCache().folderMenuItems()
                .stream()
                .map(WebElement::getText)
                .toList();
    }

    public ProductMenu clickFolderItem(String folderName)
    {
        expand();

        if (!elementCache().activeFolderMenuItemLocator.withText(folderName)
                .existsIn(elementCache().folderColumn))
        {
            Locator.CssLocator menuSectionLoc = Locator.css(".sections-content .menu-section");
            WebElement menuSectionEl = menuSectionLoc.findElement(elementCache().menuContent);

            // clicking the folder item link should replace its containing li with an active one
            elementCache().folderItemLink(folderName).click();

            // await it becoming active
            WebDriverWrapper.waitFor(() -> elementCache().activeFolderMenuItemLocator.withText(folderName)
                            .existsIn(elementCache().folderColumn),
                    "the folder item did not become active in time", 2000);

            // setting the folder item active (if it wasn't) may update contents if the user's permissions differ there
            clearElementCache();
            getWrapper().shortWait().until(ExpectedConditions.stalenessOf(menuSectionEl));
            getWrapper().shortWait().until(ExpectedConditions.visibilityOfNestedElementsLocatedBy(elementCache().menuContent, menuSectionLoc));
        }

        return this;
    }

    public void goToFolderDashboard(String folderName)
    {
        clickFolderItem(folderName);
        clickNavLink(elementCache().activeDashboardIcon);
    }

    public int getDashboardIconCount()
    {
        return elementCache().dashboardIconLoc.findElements(elementCache().menuContent).size();
    }

    public void goToFolderAdministration(String folderName)
    {
        clickFolderItem(folderName);
        clickNavLink(elementCache().activeAdministrationIcon);
    }

    public int getAdministrationIconCount()
    {
        return elementCache().administrationIconLoc.findElements(elementCache().menuContent).size();
    }

    public String getButtonTitle()
    {
        WebElement buttonTitle = elementCache().toggle.findElement(Locator.byClass("title"));
        return buttonTitle.getText();
    }

    public String getButtonSubtitle()
    {
        WebElement buttonSubtitle = elementCache().toggle.findElement(Locator.byClass("subtitle"));
        return buttonSubtitle.getText();
    }

    private void clickNavLink(WebElement link)
    {
        if (isCurrentFolderSelected())
        {
            link.click();
            getWrapper().shortWait().until(ExpectedConditions.stalenessOf(link));
        }
        else
        {
            getWrapper().clickAndWait(link);
        }
        clearElementCache();
    }

    private boolean isCurrentFolderSelected()
    {
        if (!elementCache().folderColumn.isDisplayed())
            return true;

        String folderName = elementCache().activeFolderMenuItemLocator.findElement(elementCache().folderColumn).getText();
        return getWrapper().getCurrentContainerPath().endsWith("/" + folderName);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    static Locator rootLocator = Locator.byClass("product-menu");

    protected class ElementCache extends Component<?>.ElementCache
    {
        private final WebElement rootElement = rootLocator.findElement(getDriver());
        private final WebElement toggle = Locator.byClass("product-menu-button").findElement(rootElement);
        private final WebElement menuContent = Locator.tagWithClass("div", "product-menu-content").findWhenNeeded(this);
        private final WebElement folderColumn = Locator.tagWithClass("div", "col-folders").findWhenNeeded(menuContent);
        private final WebElement sectionContent = Locator.tagWithClass("div", "sections-content").findWhenNeeded(menuContent);

        public Locator.XPathLocator dashboardIconLoc = Locator.tagWithClass("i", "fa-home");
        public WebElement activeDashboardIcon = Locator.tagWithClass("div", "col-folders")
                .descendant(Locator.tagWithClass("li", "active"))
                .descendant(dashboardIconLoc)
                .findWhenNeeded(menuContent);
        public Locator.XPathLocator administrationIconLoc = Locator.tagWithClass("i", "fa-gear");
        public WebElement activeAdministrationIcon = Locator.tagWithClass("div", "col-folders")
                .descendant(Locator.tagWithClass("li", "active"))
                .descendant(administrationIconLoc)
                .findWhenNeeded(menuContent);


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
            return menuSectionHeaderLoc(headerText).child(Locator.linkContainingText(headerText)).findWhenNeeded(sectionContent);
        }

        WebElement menuSectionBody(String headerText)
        {
            return menuSectionHeaderLoc(headerText)
                    .parent("ul")
                    .parent("div") // .product-menu-section-header
                    .followingSibling("div") // .product-menu-section-body
                    .childTag("ul")
                    .findElement(sectionContent);
        }

        WebElement menuSectionLink(String headerText, String linkText)
        {
            return Locator.linkWithText(linkText).findElement(menuSectionBody(headerText));
        }

        private final Locator.XPathLocator folderMenuItemLocator = Locator.tagWithClass("a", "menu-folder-item");
        private final Locator activeFolderMenuItemLocator = Locator.tagWithClass("li", "active").descendant(folderMenuItemLocator);

        List<WebElement> folderMenuItems()
        {
            return folderMenuItemLocator.findElements(folderColumn);
        }

        WebElement folderItemLink(String folderName)
        {
            return folderMenuItemLocator.withText(folderName).findElement(folderColumn);
        }
    }
}
