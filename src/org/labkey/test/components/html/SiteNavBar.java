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
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

import java.util.List;
import java.util.concurrent.TimeUnit;

/* Wraps the new site/admin nav menus and site search */
public class SiteNavBar extends WebDriverComponent<SiteNavBar.Elements>
{
    protected final WebDriver _driver;
    protected WebElement _componentElement;

    public SiteNavBar(WebDriver driver)
    {
        _driver = driver;
    }

    public SiteNavBar(WebDriverWrapper wrapper)
    {
        this(wrapper.getDriver());
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

    public SiteNavBar clickAdminMenuItem(boolean wait, boolean onlyOpen, String ... subMenuLabels)
    {
        BootstrapMenu menu = new BootstrapMenu(getDriver(), elementCache().adminMenuContainer);
        menu.clickMenuButton(wait, onlyOpen, subMenuLabels);
        return this;
    }

    public SiteNavBar goToModule(String moduleName)
    {
        BootstrapMenu menu = new BootstrapMenu(getDriver(), elementCache().adminMenuContainer);
        WebElement moreModulesElement = menu.openMenuTo("Go To Module", "More Modules");
        /* at this point, we want to know if the module link is visible above the 'more modules' break.
         * if it is, click it- otherwise, expand the 'More Modules' link and  */

        WebElement moduleLinkElement = menu.findVisibleMenuItemOrNull(moduleName);
        if (moduleLinkElement != null && moduleLinkElement.isDisplayed())
        {
            getWrapper().scrollIntoView(moduleLinkElement);
            getWrapper().doAndWaitForPageToLoad(()-> moduleLinkElement.click());
            return this;
        }
        else
        {
            moreModulesElement.click();
            getWrapper().waitFor(()-> menu.findVisibleMenuItemOrNull(moduleName) != null,
                    "Did not find expected module [" + moduleName + "]", 2000);
            WebElement moduleLink =  menu.findVisibleMenuItemOrNull(moduleName);
            getWrapper().scrollIntoView(moduleLink);        // todo: consider using filter to bring the module into view
            menu.findVisibleMenuItem(moduleName).click();
            return this;
        }
    }

    public SiteNavBar clickUserMenuItem(boolean wait, boolean onlyOpen, String ... subMenuLabels)
    {
        BootstrapMenu menu = new BootstrapMenu(getDriver(), elementCache().userMenuContainer);
        menu.clickMenuButton(wait, onlyOpen, subMenuLabels);
        return this;
    }

    public SiteNavBar enterPageAdminMode()
    {
        if (isInPageAdminMode())
            return this;
        adminMenu().clickSubMenu(true, "Page Admin Mode");
        return this;
    }

    public SiteNavBar exitPageAdminMode()
    {
        if (isInPageAdminMode())
        {
            Locators.exitAdminBtn.findElement(getDriver()).click();
            getWrapper().waitFor(()-> !isInPageAdminMode(), "Failed to exit page admin mode", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        }
        return this;
    }

    public SiteNavBar stopImpersonating()
    {
        if (getWrapper().isImpersonating())
        {
            Locators.stopImpersonatingBtn.findElement(getDriver()).click();
            getWrapper().waitFor(() -> !getWrapper().isImpersonating(), "Failed to stop impersonating", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        }
        return this;
    }

    public boolean isInPageAdminMode()
    {
        return Locators.exitAdminBtn.findElementOrNull(getDriver()) != null;
    }

    public SearchResultsPage search(String searchTerm)
    {
        expandSearchBar();
        getWrapper().setFormElement(elementCache().searchInputElement, searchTerm);
        getWrapper().doAndWaitForPageToLoad(() -> elementCache().searchSubmitInput.click(), WebDriverWrapper.WAIT_FOR_PAGE);
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
            getWrapper().waitFor(()-> isSearchBarExpanded(), "Search bar didn't expand", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        }
        return elementCache().searchInput;
    }

    public BootstrapMenu adminMenu()
    {
        return new BootstrapMenu(getDriver(), elementCache().adminMenuContainer);
    }

    public BootstrapMenu userMenu()
    {
        return new BootstrapMenu(getDriver(), elementCache().userMenuContainer);
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
                .refindWhenNeeded(headerBlock)
                .withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);

        public WebElement searchContainer = Locators.searchMenuToggle.parent()
                .refindWhenNeeded(headerBlock).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public WebElement searchToggle = Locators.searchMenuToggle
                .refindWhenNeeded(headerBlock).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public WebElement searchInput = Locators.searchInput
                .refindWhenNeeded(headerBlock).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public WebElement searchInputElement = Locator.xpath("//div[@id='global-search']//input[@type='text']")
                .refindWhenNeeded(searchContainer).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public WebElement searchSubmitInput = Locator.xpath("//div[@id='global-search']//a[@class='btn-search fa fa-search']")
                .refindWhenNeeded(searchContainer).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public WebElement userMenuContainer = Locators.userMenuToggle.parent()
                .refindWhenNeeded(headerBlock)
                .withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public WebElement adminMenuContainer = Locators.adminMenuToggle.parent()
                .refindWhenNeeded(navbarNavBlock)
                .withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
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
