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
import org.labkey.test.components.internal.ImpersonateGroupWindow;
import org.labkey.test.components.internal.ImpersonateRoleWindow;
import org.labkey.test.components.internal.ImpersonateUserWindow;
import org.labkey.test.pages.admin.CreateProjectPage;
import org.labkey.test.pages.admin.FolderManagementPage;
import org.labkey.test.pages.admin.PermissionsPage;
import org.labkey.test.pages.files.FileContentPage;
import org.labkey.test.pages.search.SearchResultsPage;
import org.labkey.test.pages.user.ShowUsersPage;
import org.labkey.test.util.AbstractUserHelper;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    public PermissionsPage goToPermissionsPage()
    {
        adminMenu().clickSubMenu(true,"Folder", "Permissions");
        return new PermissionsPage(getDriver());
    }

    public FolderManagementPage goToFolderManagement()
    {
        adminMenu().clickSubMenu(true, "Folder", "Management");
        return new FolderManagementPage(getDriver());
    }

    public CreateProjectPage goToCreateProjectPage()
    {
        adminMenu().clickSubMenu(true, "Site", "Create Project");
        return new CreateProjectPage(getDriver());
    }

    public ShowUsersPage goToSiteUsersPage()
    {
        adminMenu().clickSubMenu(true, "Site", "Site Users");
        return new ShowUsersPage(getDriver());
    }

    public FileContentPage goToFileContentPage()
    {
        goToModule("FileContent");
        return new FileContentPage(getDriver());
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

        assertTrue("Failed to enter page admin mode", isInPageAdminMode());
    }

    public void exitPageAdminMode()
    {
        if (isInPageAdminMode())
        {
            getWrapper().clickAndWait(Locators.exitAdminBtn.refindWhenNeeded(getDriver()));
            assertFalse("Failed to exit page admin mode", isInPageAdminMode());
        }
    }

    public void stopImpersonating()
    {
        if (!getWrapper().isImpersonating())
            throw new IllegalStateException("Not currently impersonating");

        getWrapper().clickAndWait(Locators.stopImpersonatingBtn.findElement(getDriver()));
        getWrapper().assertSignedInNotImpersonating();
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

    public UserMenu userMenu()
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
        public final AdminMenu adminMenu = new AdminMenuFinder(getDriver()).findWhenNeeded(navbarNavBlock).withExpandRetries(4);
        public final UserMenu userMenu = new UserMenuFinder(getDriver()).findWhenNeeded(navbarNavBlock).withExpandRetries(4);
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
                WebElement input = Locator.tagWithAttribute("input", "data-filter-item", "more-modules-item").findElementOrNull(getDriver());
                if (input != null)
                    getWrapper().setFormElement(input, moduleName);
                moduleLinkElement = findVisibleMenuItem(moduleName);
            }
            getWrapper().clickAndWait(moduleLinkElement);
        }

        @Override
        public AdminMenu withExpandRetries(int retries)
        {
            return (AdminMenu) super.withExpandRetries(retries);
        }
    }

    protected class AdminMenuFinder extends SimpleComponentFinder<AdminMenu>
    {
        final WebDriver _driver;

        public AdminMenuFinder(WebDriver driver)
        {
            super(Locators.adminMenu);
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

        public void impersonate(String fakeUser)
        {
            ImpersonateUserWindow window;
            try {
                clickSubMenu(false, "Impersonate", "User");
                window = new ImpersonateUserWindow(getDriver());
                window.getComponentElement().isDisplayed(); // force it to resolve
            }catch (NoSuchElementException notfound)
            {
                clickSubMenu(false, "Impersonate", "User");
                window = new ImpersonateUserWindow(getDriver());
            }
            window.selectUser(fakeUser);
            window.clickImpersonate();

            AbstractUserHelper.saveCurrentDisplayName(getWrapper());

            if (getWrapper().isElementPresent(Locator.lkButton("Home")))
            {
                getWrapper().clickAndWait(Locator.lkButton("Home"));
            }
        }

        public void impersonateRoles(String oneRole, String... roles)
        {
            ImpersonateRoleWindow window;
            try {
                clickSubMenu(false, "Impersonate", "Roles");
                window = new ImpersonateRoleWindow(getDriver());
                window.getComponentElement().isDisplayed(); // force it to find/resolve
            } catch (NoSuchElementException notFound)
            {
                clickSubMenu(false, "Impersonate", "Roles");
                window = new ImpersonateRoleWindow(getDriver());
            }

            window.selectRoles(oneRole);
            window.selectRoles(roles);
            window.clickImpersonate();
        }

        public void impersonateGroup(String group, boolean isSiteGroup)
        {
            ImpersonateGroupWindow window;

            try{
                clickSubMenu(false, "Impersonate", "Group");
                window = new ImpersonateGroupWindow(getDriver());
                window.getComponentElement().isDisplayed(); // force it to resolve
            }catch (NoSuchElementException retry)
            {
                clickSubMenu(false, "Impersonate", "Group");
                window = new ImpersonateGroupWindow(getDriver());
            }

            if (isSiteGroup)
                window.selectSiteGroup(group);
            else
                window.selectGroup(group);

            window.clickImpersonate();
        }

        @Override
        public UserMenu withExpandRetries(int retries)
        {
            return (UserMenu) super.withExpandRetries(retries);
        }
    }

    protected class UserMenuFinder extends SimpleComponentFinder<UserMenu>
    {
        final WebDriver _driver;

        public UserMenuFinder(WebDriver driver)
        {
            super(Locators.userMenu);
            _driver = driver;
        }

        @Override
        protected UserMenu construct(WebElement el)
        {
            return new UserMenu(_driver, el);
        }
    }

    public static class Locators
    {
        private static Locator.XPathLocator exitAdminBtn = Locator.xpath("//a[@class='btn btn-primary' and text()='Exit Admin Mode']");
        private static Locator.XPathLocator stopImpersonatingBtn = Locator.xpath("//a[@class='btn btn-primary' and text()='Stop impersonating']");
        private static Locator.XPathLocator searchMenuToggle = Locator.xpath("//li/a[@id='global-search-trigger']");
        private static Locator.XPathLocator searchInput = Locator.tagWithClass("input", "search-box").withAttribute("name", "q");
        public static Locator.XPathLocator userMenu = Locator.id("headerUserDropdown");
        public static Locator.XPathLocator adminMenu = Locator.id("headerAdminDropdown");
    }
}
