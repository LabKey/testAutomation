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
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.components.Component;
import org.labkey.test.pages.search.SearchResultsPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/* Wraps the new site/admin nav menus and site search */
public class SiteNavBar extends Component
{
    protected WebDriverWrapper _driver;
    protected WebElement _componentElement;

    public SiteNavBar(WebDriver driver)
    {
        this(new WebDriverWrapperImpl(driver));
    }

    public SiteNavBar(WebDriverWrapper driver)
    {
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        if (null == _componentElement)
            _componentElement= elements().navbarNavBlock;
        return _componentElement;
    }
    protected WebDriver getDriver()
    {
        return _driver.getDriver();
    }

    public SiteNavBar clickAdminMenuItem(boolean wait, boolean onlyOpen, String ... subMenuLabels)
    {
        BootstrapMenu menu = new BootstrapMenu(getDriver(), elements().adminMenuContainer);
        menu.clickMenuButton(wait, onlyOpen, subMenuLabels);
        return this;
    }

    public SiteNavBar clickUserMenuItem(boolean wait, boolean onlyOpen, String ... subMenuLabels)
    {
        BootstrapMenu menu = new BootstrapMenu(getDriver(), elements().userMenuContainer);
        menu.clickMenuButton(wait, onlyOpen, subMenuLabels);
        return this;
    }

    public SiteNavBar enterPageAdminMode()
    {
        if (isInPageAdminMode())
            return this;
        clickAdminMenuItem(true, false, "Page Admin Mode");
        return this;
    }

    public SiteNavBar exitPageAdminMode()
    {
        if (isInPageAdminMode())
        {
            Locators.exitAdminBtn.findElement(getDriver()).click();
            _driver.waitFor(()-> !isInPageAdminMode(), WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        }
        return this;
    }

    public SiteNavBar stopImpersonating()
    {
        if (isImpersonating())
        {
            Locators.stopImpersonatingBtn.findElement(getDriver()).click();
            _driver.waitFor(()-> !isImpersonating(), WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        }
        return this;
    }

    public boolean isImpersonating()
    {
        return _driver.isImpersonating();
    }

    public boolean isInPageAdminMode()
    {
        return Locators.exitAdminBtn.findElementOrNull(getDriver()) != null;
    }

    public SearchResultsPage search(String searchTerm)
    {
        if (!isSearchBarExpanded())
        {
            elements().searchToggle.click();
            _driver.waitFor(()-> isSearchBarExpanded(), WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        }
        _driver.setFormElement(elements().searchInputElement, searchTerm);
        _driver.doAndWaitForPageToLoad(()-> elements().searchSubmitInput.click(), WebDriverWrapper.WAIT_FOR_PAGE);
        return new SearchResultsPage(getDriver());
    }

    public boolean isSearchBarExpanded()
    {
        String searchBarContainerClass = elements().searchContainer.getAttribute("class");
        return searchBarContainerClass.contains("active");
    }

    protected Elements elements()
    {
        return new Elements();
    }

    protected class Elements extends ElementCache
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
        public WebElement searchInputElement = Locator.xpath("//div[@id='global-search']//input[@type='text']")
                .refindWhenNeeded(searchContainer).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public WebElement searchSubmitInput = Locator.xpath("//div[@id='global-search']//a[@class='btn-search fa fa-search']")
                .refindWhenNeeded(searchContainer).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public WebElement userMenuContainer = Locators.userMenuToggle.parent()
                .refindWhenNeeded(headerBlock)
                .withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public WebElement userMenuToggle = Locators.userMenuToggle
                .refindWhenNeeded(headerBlock)
                .withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public WebElement adminMenuContainer = Locators.adminMenuToggle.parent()
                .refindWhenNeeded(navbarNavBlock)
                .withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        public WebElement adminMenuToggle = Locators.adminMenuToggle
                .refindWhenNeeded(navbarNavBlock)
                .withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
    }

    public static class Locators
    {
        public static Locator.XPathLocator exitAdminBtn = Locator.xpath("//a[@class='btn btn-primary' and text()='Exit Admin Mode']");
        public static Locator.XPathLocator stopImpersonatingBtn = Locator.xpath("//a[@class='btn btn-primary' and text()='Stop impersonating']");
        public static Locator.XPathLocator searchMenuToggle = Locator.xpath("//li/a[@id='global-search-trigger']");
        public static Locator.XPathLocator userMenuToggle = Locator.xpath("//li/a[@class='dropdown-toggle' and ./i[@class='fa fa-user']]");
        public static Locator.XPathLocator adminMenuToggle = Locator.xpath("//li/a[@class='dropdown-toggle' and ./i[@class='fa fa-cog']]");
    }
}
