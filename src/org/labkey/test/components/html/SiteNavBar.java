/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.test.components.html;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.pages.search.SearchResultsPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/* Wraps the new site/admin nav menus and site search */
public class SiteNavBar extends WebDriverComponent<SiteNavBar.Elements>
{
    protected final WebDriver _driver;
    protected WebElement _componentElement;

    public SiteNavBar(WebDriver driver)
    {
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return elementCache().navbarNavBlock;
    }
    protected WebDriver getDriver()
    {
        return _driver;
    }

    public void clickAdminMenuItem(boolean wait, String ... subMenuLabels)
    {
        adminMenu().clickSubMenu(wait, subMenuLabels);
    }

    public void goToModule(String moduleName)
    {
        adminMenu().goToModule(moduleName);
    }

    public void clickUserMenuItem(boolean wait, String ... subMenuLabels)
    {
        userMenu().clickSubMenu(wait, subMenuLabels);
    }

    public void enterPageAdminMode()
    {
        if (!isInPageAdminMode())
            adminMenu().clickSubMenu(true, "Page Admin Mode");
    }

    public void exitPageAdminMode()
    {
        if (isInPageAdminMode())
        {
            getWrapper().clickAndWait(Locators.exitAdminBtn.refindWhenNeeded(getDriver()));
            WebDriverWrapper.waitFor(()-> !isInPageAdminMode(), "Failed to exit page admin mode", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        }
    }

    public void stopImpersonating()
    {
        if (getWrapper().isImpersonating())
        {
            getWrapper().clickAndWait(Locators.stopImpersonatingBtn.findElement(getDriver()));
            WebDriverWrapper.waitFor(() -> !getWrapper().isImpersonating(), "Failed to stop impersonating", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        }
    }

    public boolean isInPageAdminMode()
    {
        return Locators.exitAdminBtn.findElementOrNull(getDriver()) != null;
    }

    public SearchResultsPage search(String searchTerm)
    {
        expandSearchBar();
        getWrapper().setFormElement(elementCache().searchInputElement, searchTerm);
        getWrapper().clickAndWait(elementCache().searchSubmitInput);
        return new SearchResultsPage(getDriver());
    }

    public boolean isSearchBarExpanded()
    {
        String searchBarContainerClass = elementCache().searchContainer.getAttribute("class");
        return searchBarContainerClass.contains("active");
    }

    /* toggles the search form open and returns the search input element */
    public WebElement expandSearchBar()
    {
        if (!isSearchBarExpanded())
        {
            elementCache().searchToggle.click();
            WebDriverWrapper.waitFor(this::isSearchBarExpanded, "Search bar didn't expand", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        }
        return elementCache().searchInput;
    }

    public AdminMenu adminMenu()
    {
        return elementCache().adminMenu;
    }

    public BootstrapMenu userMenu()
    {
        return elementCache().userMenu;
    }

    @Override
    protected SiteNavBar.Elements newElementCache()
    {
        return new SiteNavBar.Elements();
    }

    // TODO: Remove and verify that elements in cache won't go stale
    @Override
    protected SiteNavBar.Elements elementCache()
    {
        return newElementCache();
    }

    protected class Elements extends Component.ElementCache
    {
        public WebElement headerBlock = Locator.xpath("//div[@class='labkey-page-header']")
                .findWhenNeeded(getDriver())
                .withTimeout(WebDriverWrapper.WAIT_FOR_PAGE);

        public WebElement navbarNavBlock = Locator.xpath("//ul[@class='navbar-nav-lk']")
                .findWhenNeeded(headerBlock)
                .withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);

        public WebElement searchContainer = Locators.searchMenuToggle.parent()
                .findWhenNeeded(headerBlock).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public WebElement searchToggle = Locators.searchMenuToggle
                .findWhenNeeded(headerBlock).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public WebElement searchInput = Locators.searchInput
                .findWhenNeeded(headerBlock).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public WebElement searchInputElement = Locator.xpath("//div[@id='global-search']//input[@type='text']")
                .findWhenNeeded(searchContainer).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public WebElement searchSubmitInput = Locator.xpath("//div[@id='global-search']//a[@class='btn-search fa fa-search']")
                .findWhenNeeded(searchContainer).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public final AdminMenu adminMenu = (AdminMenu) new AdminMenuFinder(getDriver()).findWhenNeeded(navbarNavBlock).withExpandRetries(4);
        public final UserMenu userMenu = (UserMenu) new UserMenuFinder(getDriver()).findWhenNeeded(navbarNavBlock).withExpandRetries(4);
    }

    public class AdminMenu extends BootstrapMenu
    {
        protected AdminMenu(WebDriver driver, WebElement componentElement)
        {
            super(driver, componentElement);
        }

        public void goToModule(String moduleName)
        {
            WebElement moreModulesElement = openMenuTo("Go To Module", "More Modules");
        /* at this point, we want to know if the module link is visible above the 'more modules' break.
         * if it is, click it- otherwise, expand the 'More Modules' link and  */

            WebElement moduleLinkElement = findVisibleMenuItemOrNull(moduleName);
            if (moduleLinkElement != null && moduleLinkElement.isDisplayed())
            {
                getWrapper().scrollIntoView(moduleLinkElement);
            }
            else
            {
                moreModulesElement.click();
                WebDriverWrapper.waitFor(()-> findVisibleMenuItemOrNull(moduleName) != null,
                        "Did not find expected module [" + moduleName + "]", 2000);
                getWrapper().setFormElement(Locator.tagWithAttribute("input", "data-filter-item", "more-modules-item"), moduleName);
                moduleLinkElement = findVisibleMenuItem(moduleName);
            }
            getWrapper().clickAndWait(moduleLinkElement);
        }
    }

    protected class AdminMenuFinder extends SimpleComponentFinder<AdminMenu>
    {
        final WebDriver _driver;

        public AdminMenuFinder(WebDriver driver)
        {
            super(Locators.adminMenuToggle.parent());
            _driver = driver;
        }

        @Override
        protected AdminMenu construct(WebElement el)
        {
            return new AdminMenu(_driver, el);
        }
    }

    public class UserMenu extends BootstrapMenu
    {
        protected UserMenu(WebDriver driver, WebElement componentElement)
        {
            super(driver, componentElement);
        }
    }

    protected class UserMenuFinder extends SimpleComponentFinder<UserMenu>
    {
        final WebDriver _driver;

        public UserMenuFinder(WebDriver driver)
        {
            super(Locators.userMenuToggle.parent());
            _driver = driver;
        }

        @Override
        protected UserMenu construct(WebElement el)
        {
            return new UserMenu(_driver, el);
        }
    }

    private static class Locators
    {
        public static Locator.XPathLocator exitAdminBtn = Locator.xpath("//a[@class='btn btn-primary' and text()='Exit Admin Mode']");
        public static Locator.XPathLocator stopImpersonatingBtn = Locator.xpath("//a[@class='btn btn-primary' and text()='Stop impersonating']");
        public static Locator.XPathLocator searchMenuToggle = Locator.xpath("//li/a[@id='global-search-trigger']");
        public static Locator.XPathLocator searchInput = Locator.tagWithClass("input", "search-box").withAttribute("name", "q");
        public static Locator.XPathLocator userMenuToggle = Locator.xpath("//li/a[@class='dropdown-toggle' and ./i[@class='fa fa-user']]");
        public static Locator.XPathLocator adminMenuToggle = Locator.xpath("//li/a[@class='dropdown-toggle' and ./i[@class='fa fa-cog']]");
    }
}
