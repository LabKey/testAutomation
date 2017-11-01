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
package org.labkey.test.components.labkey;

import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.WAIT_FOR_PAGE;

public class PortalTab extends WebDriverComponent<PortalTab.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public PortalTab(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    static public PortalTabFinder finder(WebDriver driver)
    {
        return new PortalTabFinder(driver);
    }

    public static PortalTab find(String tabText, WebDriver driver)
    {
        return new PortalTab(Locators.container.append(Locators.tabItem.withChild(Locators.tabIdLoc(tabText)))
                                .refindWhenNeeded(driver).withTimeout(WAIT_FOR_JAVASCRIPT),
                            driver);
    }

    public static List<PortalTab> findTabs(WebDriver driver)
    {
        List<PortalTab> tabs = new ArrayList<>();
        Locators.tabList.findElements(driver)
                .stream()
                .forEachOrdered((e)-> tabs.add(new PortalTab(e,  driver)));
        return tabs;
    }

    public static PortalTab findActiveTab(WebDriver driver)
    {
        return new PortalTab(Locators.container.append(
                Locators.tabItem.withAttribute("class", "active"))
                .waitForElement(driver, LabKeySiteWrapper.WAIT_FOR_JAVASCRIPT), driver);
    }

    public String getText()
    {
        return Locator.xpath("./a").findElement(getComponentElement()).getText();
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el; // should be //li[contains(@role,'presentation')]
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    public int getIndex()
    {
        return getWrapper().getElementIndex(getComponentElement()); // zero-based
    }

    public BootstrapMenu getMenu()
    {
        return new BootstrapMenu(getDriver(), getComponentElement()); // componentElement should be the li containing the
    }

    public String getName()
    {
        return elementCache().anchor.getText();
    }

    public PortalTab moveLeft()
    {
        getWrapper().log("Attempting to move tab [" + getName() + "] to the left");
        String text = getText();
        getMenu().clickSubMenu(false, "Move", "Left");
        return PortalTab.find(text, getDriver());
    }

    public PortalTab moveRight()
    {
        getWrapper().log("Attempting to move tab [" + getName() + "] to the right");
        String text = getText();
        getMenu().clickSubMenu(false, "Move", "Right");
        return PortalTab.find(text, getDriver());
    }

    /* assumes you're in admin mode.  When not in admin mode, it will not be in the page.
     * 'true' here means the eye glyph is shown in the tab anchor */
    public boolean isHidden() //
    {
        return Locator.xpath("//a/i[contains(@class,'fa-eye-slash')]").findElementOrNull(getComponentElement()) != null;
    }

    public boolean isActive()
    {
        String componentElementClass = getComponentElement().getAttribute("class");
        return componentElementClass != null && componentElementClass.equalsIgnoreCase("active");
    }

    public PortalTab activate()
    {
        if (isActive())
            return this;
        else
        {
            TestLogger.log("Activating tab [" + getName() + "]");
            String text = getText();
            getWrapper().clickAndWait(elementCache().anchor);
            return PortalTab.find(text, getDriver());
        }
    }

    public PortalTab show()
    {
        getWrapper().log("Attempting to show tab [" + getName() + "]");
        String text = getText();
        getMenu().clickSubMenu(false,"Show");
        return PortalTab.find(text, getDriver());
    }

    public PortalTab hide()
    {
        getWrapper().log("Attempting to hide tab [" + getName() + "]");
        String text = getText();
        getMenu().clickSubMenu(false,"Hide");
        return PortalTab.find(text, getDriver());
    }

    public void delete()
    {
        getWrapper().log("Attempting to delete tab [" + getName() + "]");
        String text = getText();
        getMenu().clickSubMenu(false,"Delete");
    }

    /* clicking 'rename' will pop a form to take the name */
    public void clickRename()
    {
        getMenu().clickSubMenu(false,"Rename");
    }

    public PortalTab rename(String newName)
    {
        String text = getText();
        clickRename();

        getWrapper().waitForText("Rename Tab");
        getWrapper().setFormElement(Locator.input("tabName"),newName);
        getWrapper().clickButton("Ok", 0);

        /* if the rename is bogus (there's already a tab by that name)
         * calling code will have to handle that */
        return this;
    }

    /* Some tabs are also containers; these tabs have a select in them to support navigation among sub-tabs
    * */
    public PortalTab goToTabContainer(String subContainer)
    {
        String currentTabText = getText(); // use to find the current tab after the page refreshes
        getWrapper().doAndWaitForPageToLoad(()->
                getWrapper().selectOptionByText(Locators.subContainerTabSelect, subContainer),
                WAIT_FOR_PAGE);
        return PortalTab.find(currentTabText, getDriver());
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebDriverComponent.ElementCache
    {
        WebElement anchor = Locator.xpath("./a").findElement(getComponentElement());
    }


    /* TODO: tighten up locators to only find tabs that are appropriate to the form factor.
    *  Right now, there are two lists of tabs; the ones shown in small and large form factors.
    * */
    public static class Locators
    {

        static public Locator.XPathLocator tabIdLoc(String tabText)
        {
            String tabId = tabText.replace(" ", "") + "Tab";
            return Locator.xpath("//a[@id=" + Locator.xq(tabId) + "]");
        }

        static public Locator.XPathLocator container = Locator.tagWithClass("ul", "lk-nav-tabs");
        static public Locator.XPathLocator tabItem = Locator.xpath("//li[@role='presentation']");
        static public Locator.XPathLocator subContainerTabSelect = Locator.tagWithAttribute("select", "title", "subContainerTabs");

        static public Locator.XPathLocator tabList = container.append(tabItem);
    }

    public static class PortalTabFinder extends WebDriverComponentFinder<PortalTab, PortalTab.PortalTabFinder>
    {
        private Locator _locator = Locators.container.append(Locators.tabItem);

        public PortalTabFinder(WebDriver driver)
        {
            super(driver);
        }

        public PortalTabFinder withTabText(String text)
        {
            _locator = Locators.container.append(Locators.tabItem.withChild(Locators.tabIdLoc(text)));
            return this;
        }

        public PortalTabFinder isActiveTab()
        {
            _locator = Locators.container.append(Locators.tabItem.withAttribute("class", "active"));
            return this;
        }

        @Override
        protected PortalTab construct(WebElement el, WebDriver driver)
        {
            return new PortalTab(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}