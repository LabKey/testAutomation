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
import org.labkey.test.pages.admin.CreateSubFolderPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

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
        getWrapper().scrollIntoView(newElementCache().getMenuItem(projectName));
        getWrapper().fireEvent(newElementCache().getMenuItem(projectName), WebDriverWrapper.SeleniumEvent.mouseover);
        getWrapper().doAndWaitForPageToLoad(()-> newElementCache().getNavigationLink(projectName).click());
    }

    /* Will navigate to a folder or subfolder of the specified project */
    public void navigateToFolder(String projectName, String folder)
    {
        open();                                     // selecting the project (menu item) will reveal the nav link in the right pane
        getWrapper().scrollIntoView(newElementCache().getMenuItem(projectName));
        getWrapper().fireEvent(newElementCache().getMenuItem(projectName), WebDriverWrapper.SeleniumEvent.mouseover);

        getWrapper().waitFor(()-> {
            if (navigationLinkIsVisible(folder))
                return true;
            else    // expand any collapsed expandos until the navigation link becomes clickable
            {
                WebElement expando = Locator.tagWithClass("li", "clbl collapse-folder")
                        .child(Locator.tagWithClass("span", "marked"))         // expandos are 'clbl expand-folder' when opened
                        .findElementOrNull(newElementCache().folderListContainer);
                if (expando != null)
                    expando.click();
            }
            return false;
        }, WAIT_FOR_JAVASCRIPT);

        getWrapper().doAndWaitForPageToLoad(()->newElementCache().getNavigationLink(folder).click());
    }

    /* This is a way to discover via the UI if the specified project exists (or did the last time the
        * UI refreshed). */
    public boolean projectLinkExists(String projectName)
    {
        open();
        return Locator.tag("li").childTag("a").withText(projectName).notHidden()
                .findElementOrNull(newElementCache().menu) != null;
    }

    /* real-time check to see if the destination nav-link (folder or project) is present and visible*/
    public boolean navigationLinkIsVisible(String navigationLinkText)
    {
        WebElement linkElement = Locator.tag("li").childTag("a").withText(navigationLinkText).notHidden()
                .findElementOrNull(newElementCache().folderListContainer);
        return linkElement != null && linkElement.isDisplayed();
    }

    public List<WebElement> projectMenuLinks()
    {
        return newElementCache().getProjectMenuLinks();
    }

    /**
     * Creates a subfolder from the context you are already in (e.g. project/folder)
     */
    public CreateSubFolderPage navigateToCreateSubFolderPage()
    {
        open();
        getWrapper().doAndWaitForPageToLoad(() -> newElementCache().newSubFolderButton.click());
        return new CreateSubFolderPage(getDriver());
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
        WebElement projectNavBtnContainer = Locator.tagWithClass("div", "beta-nav-buttons").refindWhenNeeded(menu);

        WebElement newProjectButton = Locator.xpath("//span/a[@title='New Project']").refindWhenNeeded(projectNavBtnContainer);
        WebElement newSubFolderButton = Locator.xpath("//span/a[@title='New Subfolder']").refindWhenNeeded(projectNavBtnContainer);

        WebElement getMenuItem(String text)
        {
            return Locator.tag("li").childTag("a").withAttribute("data-field", text).notHidden().waitForElement(menu, WAIT_FOR_JAVASCRIPT);
        }
        List<WebElement> getProjectMenuLinks()
        {
            return Locator.tag("li").childTag("a").withAttribute("data-field").notHidden().findElements(menuContainer);
        }
        WebElement getNavigationLink(String text)
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