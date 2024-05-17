/*
 * Copyright (c) 2019 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
package org.labkey.test.components.ui.navigation;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.MultiMenu;
import org.labkey.test.components.ui.notifications.ServerNotificationMenu;
import org.labkey.test.components.ui.search.SampleFinder;
import org.labkey.test.util.search.HasSearchResults;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

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

    public FindByIdsDialog findBySampleIds()
    {
        elementCache().searchMenu.doMenuAction("Find Samples by ID");
        return new FindByIdsDialog(getDriver());
    }

    public FindByIdsDialog findByBarcodes()
    {
        elementCache().searchMenu.doMenuAction("Find Samples by Barcode");
        return new FindByIdsDialog(getDriver());
    }

    public SampleFinder goToSampleFinder()
    {
        elementCache().searchMenu.doMenuAction("Sample Finder");
        return new SampleFinder(getDriver());
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
        return elementCache().notificationsMenu;
    }

    public ProductMenu getProductMenu()
    {
        return elementCache().productMenu;
    }

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
        public MultiMenu searchMenu = new MultiMenu.MultiMenuFinder(getDriver()).withButtonClass("navbar__find-and-search-button").findWhenNeeded(this);
        public final ProductMenu productMenu = ProductMenu.finder(getDriver()).timeout(1000).findWhenNeeded(this);
        public final ServerNotificationMenu notificationsMenu = ServerNotificationMenu.finder(getDriver()).timeout(1000).findWhenNeeded(this);
    }
}
