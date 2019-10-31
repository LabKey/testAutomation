/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.glassLibrary.navigation;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class NavBar extends WebDriverComponent<NavBar.ElementCache>
{
    private final WebDriver _driver;
    private final WebElement _navBarElement;

    public NavBar(WebDriver driver)
    {
        this(Locator.tagWithClass("nav", "navbar-container").findElement(driver), driver);
    }

    protected NavBar(WebElement element, WebDriver driver)
    {
        _navBarElement = element;
        _driver = driver;
    }

    public void clickHeaderLogo()
    {
        elementCache().headerLogo.click();
    }

    public String getHeaderLogoImgSrc()
    {
        return elementCache().headerLogo.getAttribute("src");
    }

    public NavBar setSearchTerm(String searchString)
    {
        getWrapper().setFormElement(elementCache().searchBox, searchString);
        return this;
    }

    public String getDisplayedProjectName()
    {
        return elementCache().projectNameDisplay.getText();
    }

    public ProductMenu getProductMenu()
    {
        return elementCache().productMenu;
    }

    public String getUserAvatarSource()
    {
        return elementCache().userIcon.getAttribute("src");
    }

    public UserMenuContent clickUserMenu()
    {
        elementCache().userMenuButton.click();
        return new UserMenuContent(_driver);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _navBarElement;
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

    protected class ElementCache extends Component.ElementCache
    {
        public WebElement headerLogo = Locator.tagWithClass("a", "header-logo__link").findWhenNeeded(this);
        public WebElement headerLogoImage = Locator.tagWithClass("img", "header-logo__image").findWhenNeeded(this);
        public WebElement userMenuButton = Locator.tagWithId("a", "user-menu-dropdown").findWhenNeeded(this);
        public WebElement userIcon = Locator.tagWithAttribute("img", "alt", "User Avatar").findWhenNeeded(this);
        ProductMenu productMenu = ProductMenu.finder(getDriver()).findWhenNeeded(this);
        public WebElement projectNameDisplay = Locator.tagWithClass("span", "project-name").findWhenNeeded(this);
        public WebElement searchBox = Locator.tagWithAttribute("input", "placeholder","Enter search terms").findWhenNeeded(this);
    }

}