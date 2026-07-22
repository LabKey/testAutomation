/*
 * Copyright (c) 2019-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

public abstract class NavBar extends WebDriverComponent<NavBar.ElementCache>
{
    private final WebDriver _driver;
    private final WebElement _navBarElement;

    protected NavBar(WebDriver driver)
    {
        this(Locator.tagWithClass("nav", "navbar").findElement(driver), driver);
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
        return elementCache().headerLogoImage.getAttribute("src");
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
        return elementCache().notificationsMenu();
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
        public WebElement headerLogo = Locator.tagWithClass("a", "header-logo__link").refindWhenNeeded(this);
        public WebElement headerLogoImage = Locator.tagWithClass("img", "header-logo__image").refindWhenNeeded(this);
        public WebElement userMenuButton = Locator.tagWithId("a", "user-menu-dropdown").refindWhenNeeded(this);
        public WebElement userIcon = Locator.tagWithAttribute("img", "alt", "User Avatar").refindWhenNeeded(this);
        public WebElement projectNameDisplay = Locator.tagWithClass("span", "project-name").refindWhenNeeded(this);
        public Input searchBox = Input.Input(Locator.tagWithClass("input", "navbar__search-input"), getDriver()).refindWhenNeeded(this);
        public MultiMenu searchMenu = new MultiMenu.MultiMenuFinder(getDriver()).withButtonClass("navbar__find-and-search-button").refindWhenNeeded(this);
        public final ProductMenu productMenu = ProductMenu.finder(getDriver()).timeout(1000).refindWhenNeeded(this);
        public final ServerNotificationMenu notificationsMenu()
        {
            return ServerNotificationMenu.finder(getDriver()).timeout(1000).refindWhenNeeded(this);
        }
    }
}
