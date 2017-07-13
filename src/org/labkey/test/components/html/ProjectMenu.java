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
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;


/**
 * Wraps the project/folder menu nav in labkey pages
 */
public class ProjectMenu extends WebDriverComponent<ProjectMenu.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public ProjectMenu(WebDriver driver)
    {
        _driver = driver;
        _el = Locators.lableyPageNavbar.refindWhenNeeded(driver).withTimeout(WAIT_FOR_JAVASCRIPT);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    public WebElement getMenuToggle()
    {
        return newElementCache().menuToggle;
    }

    public boolean isExpanded()
    {
        return newElementCache().menuContainer.getAttribute("class").contains("open");
    }

    public ProjectMenu open()
    {
        if (!isExpanded())
            newElementCache().menuToggle.click();
        getWrapper().waitFor(()-> isExpanded(), 1000);
        return this;
    }

    public ProjectMenu close()
    {
        if (isExpanded())
            newElementCache().menuToggle.click();
        getWrapper().waitFor(()-> !isExpanded(), 1000);
        return this;
    }

    public void navigateToProject(String projectName)
    {
        open();
        getWrapper().doAndWaitForPageToLoad(()-> newElementCache().getMenuItem(projectName).click());
    }

    public void navigateToSubFolder(String projectName, String subFolder)
    {
        open();
        getWrapper().fireEvent(newElementCache().getMenuItem(projectName), WebDriverWrapper.SeleniumEvent.mouseover);
        getWrapper().doAndWaitForPageToLoad(()->newElementCache().getFolderPageLink(subFolder).click());
    }

    public boolean projectLinkExists(String projectName)
    {
        open();
        return Locator.tag("li").childTag("a").withText(projectName).notHidden()
                .findElementOrNull(newElementCache().menu) != null;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebDriverComponent.ElementCache
    {
        WebElement menuContainer = Locators.menuProjectNav.refindWhenNeeded(getComponentElement());
        WebElement menuToggle = Locator.tagWithAttribute("a", "data-toggle", "dropdown").refindWhenNeeded(menuContainer);
        WebElement menu = Locator.tagWithClass("ul", "dropdown-menu").refindWhenNeeded(menuContainer);
        WebElement folderListContainer = Locator.tagWithClass("div", "folder-list-container").refindWhenNeeded(menuContainer);

        WebElement projectNavTrail = Locator.tagWithClass("div", "lk-project-nav-trail").refindWhenNeeded(menu);
        WebElement projectNavBtnContainer = Locator.tagWithClass("div", "lk-project-nav-buttons").refindWhenNeeded(menu);

        WebElement newProjectButton = Locator.xpath("//div/span/a[@title='New Project']").refindWhenNeeded(projectNavBtnContainer);
        WebElement newSubFolderButton = Locator.xpath("//div/span/a[@title='New Subfolder']").refindWhenNeeded(projectNavBtnContainer);

        WebElement getMenuItem(String text)
        {
            return Locator.tag("li").childTag("a").withText(text).notHidden().findElement(menu);
        }
        WebElement getFolderPageLink(String text)
        {
            return Locator.tag("li").childTag("a").withText(text).notHidden().waitForElement(folderListContainer, WAIT_FOR_JAVASCRIPT);
        }
    }

    public static class Locators
    {
        public static final Locator lableyPageNavbar = Locator.xpath("//nav[@class='labkey-page-nav']")
                .withChild(Locator.tagWithClass("div", "container").childTag("div").withClass("navbar-header"));
        public static final Locator menuProjectNav = Locator.tagWithClassContaining("li", "dropdown")
                .withAttribute("data-name", "BetaNav");
        public static final Locator containerMobile = Locator.tagWithId("li", "project-mobile");
    }
}