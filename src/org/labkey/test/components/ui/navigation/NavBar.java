/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.ui.navigation;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.ui.navigation.apps.ServerNotificationMenu;
import org.labkey.test.util.search.HasSearchResults;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.waitFor;

public abstract class NavBar extends WebDriverComponent<NavBar.ElementCache>
{
    private final WebDriver _driver;
    private final WebElement _navBarElement;

    protected NavBar(WebDriver driver)
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

    public HasSearchResults searchFor(String searchString)
    {
        elementCache().searchBox.set(searchString);
        elementCache().searchBox.getComponentElement().sendKeys(Keys.ENTER);
        return null;
    }

    public FindByIdsDialog findByIds()
    {
        elementCache().findAndSearchMenuButton.click();
        waitFor(()->elementCache().findSamplesOption.isDisplayed(), "Find samples menu did not show up.", 500);
        elementCache().findSamplesOption.click();
        return new FindByIdsDialog(getDriver());
    }

    public String getDisplayedProjectName()
    {
        return elementCache().projectNameDisplay.getText();
    }

    public String getUserAvatarSource()
    {
        return elementCache().userIcon.getAttribute("src");
    }

    /**
     * Get the {@link ServerNotificationMenu} on the menu bar.
     *
     * @return A {@link ServerNotificationMenu}
     */
    public ServerNotificationMenu getNotificationMenu()
    {
        return ServerNotificationMenu.finder(getDriver()).find(this);
    }

    public abstract ProductMenu getProductMenu();

    public abstract UserMenu getUserMenu();

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
    protected abstract ElementCache newElementCache();

    protected abstract class ElementCache extends Component<ElementCache>.ElementCache
    {
        public WebElement headerLogo = Locator.tagWithClass("a", "header-logo__link").findWhenNeeded(this);
        public WebElement headerLogoImage = Locator.tagWithClass("img", "header-logo__image").findWhenNeeded(this);
        public WebElement userMenuButton = Locator.tagWithId("a", "user-menu-dropdown").findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        public WebElement userIcon = Locator.tagWithAttribute("img", "alt", "User Avatar").findWhenNeeded(this);
        public WebElement projectNameDisplay = Locator.tagWithClass("span", "project-name").findWhenNeeded(this);
        public Input searchBox = Input.Input(Locator.tagWithClass("input", "navbar__search-input"), getDriver()).findWhenNeeded(this);
        public WebElement searchForm = Locator.tagWithClass("form", "navbar__search-form").findWhenNeeded(this);
        public WebElement findAndSearchMenuButton = Locator.tagWithId("button", "find-and-search-menu").findWhenNeeded(searchForm);
        public WebElement findSamplesOption = Locator.linkContainingText("Find Samples").findWhenNeeded(searchForm);
    }
}
