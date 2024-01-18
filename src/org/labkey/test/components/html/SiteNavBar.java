/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/* Wraps the new site/admin nav menus and site search */
public class SiteNavBar extends WebDriverComponent<SiteNavBar.Elements>
{
    protected final WebDriver _driver;
    protected final WebElement _componentElement;

    public SiteNavBar(WebDriver driver)
    {
        _driver = driver;
        _componentElement = Locator.byClass("navbar-nav-lk")
                .findWhenNeeded(driver)
                .withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _componentElement;
    }
    @Override
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

    public void doInAdminMode(Runnable runnable)
    {
        boolean initiallyInAdminMode = enterPageAdminMode();

        runnable.run();

        if (!initiallyInAdminMode)
            exitPageAdminMode();
    }

    public boolean enterPageAdminMode()
    {
        boolean initiallyInAdminMode = isInPageAdminMode();
        if (!initiallyInAdminMode)
        {
            adminMenu().clickSubMenu(true, "Page Admin Mode");
            assertTrue("Failed to enter page admin mode", isInPageAdminMode());
        }

        return initiallyInAdminMode;
    }

    public void exitPageAdminMode()
    {
        if (isInPageAdminMode())
        {
            getWrapper().clickAndWait(Locators.exitAdminBtn);
            assertFalse("Failed to exit page admin mode", isInPageAdminMode());
        }
    }

    public void stopImpersonating()
    {
        if (!getWrapper().isImpersonating())
            throw new IllegalStateException("Not currently impersonating");

        getWrapper().clickAndWait(Locator.xpath("//a[@class='btn btn-primary' and text()='Stop impersonating']").findElement(getDriver()));
        getWrapper().assertSignedInNotImpersonating();
    }

    public void stopImpersonatingWithUnloadAlert()
    {
        if (!getWrapper().isImpersonating())
            throw new IllegalStateException("Not currently impersonating");

        getWrapper().doAndAcceptUnloadAlert(()->getWrapper().clickAndWait(Locator.xpath("//a[@class='btn btn-primary' and text()='Stop impersonating']").findElement(getDriver()), 0));
        getWrapper().assertSignedInNotImpersonating();
    }

    public boolean isInPageAdminMode()
    {
        return getWrapper().executeScript("return LABKEY.pageAdminMode;", Boolean.class);
    }

    public SearchResultsPage search(String searchTerm)
    {
        expandSearchBar();
        getWrapper().setFormElement(elementCache().searchInput, searchTerm);
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

    protected class Elements extends Component<?>.ElementCache
    {
        public final WebElement searchContainer = Locator.byClass("navbar-search")
                .findWhenNeeded(this).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public final WebElement searchToggle = Locator.id("global-search-trigger")
                .findWhenNeeded(this).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public final WebElement searchInput = Locator.input("q")
                .findWhenNeeded(this).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public final WebElement searchSubmitInput = Locator.tagWithClass("a", "btn-search")
                .findWhenNeeded(searchContainer).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public final AdminMenu adminMenu = adminMenuFinder().findWhenNeeded(this).withExpandRetries(4);
        public final UserMenu userMenu = userMenuFinder().findWhenNeeded(this).withExpandRetries(4);
    }

    protected SimpleWebDriverComponentFinder<AdminMenu> adminMenuFinder()
    {
        return new SimpleWebDriverComponentFinder<>(getDriver(), Locator.id("headerAdminDropdown"), AdminMenu::new);
    }

    protected SimpleWebDriverComponentFinder<UserMenu> userMenuFinder()
    {
        return new SimpleWebDriverComponentFinder<>(getDriver(), Locators.userMenu, UserMenu::new);
    }

    public class AdminMenu extends BootstrapMenu
    {
        protected AdminMenu(WebElement componentElement, WebDriver driver)
        {
            super(driver, componentElement);
        }

        @LogMethod (quiet = true)
        public void goToModule(@LoggedParam String moduleName)
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

        public void clickDeveloperLink(String linkText, boolean triggersPageLoad)
        {
            clickSubMenu(triggersPageLoad, "Developer Links", linkText);
        }

        @Override
        public AdminMenu withExpandRetries(int retries)
        {
            return (AdminMenu) super.withExpandRetries(retries);
        }
    }

    public class UserMenu extends BootstrapMenu
    {
        protected UserMenu(WebElement componentElement, WebDriver driver)
        {
            super(driver, componentElement);
        }

        @Override
        public void expand()
        {
            getWrapper().scrollToTop();

            super.expand();
        }

        public void impersonate(String fakeUser)
        {
            ImpersonateUserWindow window;
            try
            {
                clickSubMenu(false, "Impersonate", "User");
                window = new ImpersonateUserWindow(getDriver());
                window.getComponentElement().isEnabled(); // force it to resolve
            }
            catch (NoSuchElementException notfound)
            {
                clickSubMenu(false, "Impersonate", "User");
                window = new ImpersonateUserWindow(getDriver());
            }
            window.selectUser(fakeUser);
            window.clickImpersonate();

            AbstractUserHelper.saveCurrentDisplayName(getWrapper());

            if (getDriver().getTitle().contains("403"))
            {
                // go to home
                getWrapper().clickAndWait(Locator.tagWithClass("a", "brand-logo"));
            }

        }

        public void impersonateRoles(String oneRole, String... roles)
        {
            ImpersonateRoleWindow window;
            try
            {
                clickSubMenu(false, "Impersonate", "Roles");
                window = new ImpersonateRoleWindow(getDriver());
                window.getComponentElement().isEnabled(); // force it to find/resolve
            }
            catch (NoSuchElementException notFound)
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

            try
            {
                clickSubMenu(false, "Impersonate", "Group");
                window = new ImpersonateGroupWindow(getDriver());
                window.getComponentElement().isEnabled(); // force it to resolve
            }
            catch (NoSuchElementException retry)
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

        public void signOut()
        {
            clickSubMenu(true, "Sign Out");
        }

        @Override
        public UserMenu withExpandRetries(int retries)
        {
            return (UserMenu) super.withExpandRetries(retries);
        }
    }

    public static class Locators
    {
        private static final Locator.XPathLocator exitAdminBtn = Locator.tagWithClass("a", "btn").withText("Exit Admin Mode");
        public static final Locator.XPathLocator userMenu = Locator.id("headerUserDropdown");
    }
}
