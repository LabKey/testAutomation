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
package org.labkey.test.components.api;

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
        _el = Locators.labkeyPageNavbar.refindWhenNeeded(driver).withTimeout(WAIT_FOR_JAVASCRIPT);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    public String getCurrentProject()
    {
        return newElementCache().menuToggle.getText().replace("&nbsp;", "").trim();
    }

    public WebElement getMenuToggle()
    {
        return newElementCache().menuToggle;
    }

    public boolean isExpanded()
    {
        return newElementCache().menuContainer().getAttribute("class").contains("open");
    }

    public ProjectMenu open()
    {
        if (!isExpanded())
        {
            if (getWrapper().isElementPresent(Locator.css("li.dropdown.open > .lk-custom-dropdown-menu")))
                getWrapper().mouseOver(newElementCache().menuToggle); // Just need to hover if another menu is already open
            else
                newElementCache().menuToggle.click();
        }
        WebDriverWrapper.waitFor(this::isExpanded, 1000);
        return this;
    }

    public ProjectMenu close()
    {
        if (isExpanded())
            newElementCache().menuToggle.click();
        WebDriverWrapper.waitFor(()-> !isExpanded(), "Menu didn't close", 1000);
        return this;
    }

    /* Hovering over the project link (in the left pane) shows the folder links for that project
     * in the (right) folder pane. */
    public WebElement mouseOverProjectLink(String projectName)
    {
        open();
        WebElement menuItem = newElementCache().getMenuItem(projectName);
        getWrapper().scrollIntoView(menuItem);
        getWrapper().fireEvent(menuItem, WebDriverWrapper.SeleniumEvent.mouseover);
        return menuItem;
    }

    public void navigateToProject(String projectName)
    {
        if (!getCurrentProject().equals(projectName))   //only hover the project link if it's different... right?
            mouseOverProjectLink(projectName);
        expandFolderLinksTo(projectName);
        getWrapper().doAndWaitForPageToLoad(()-> newElementCache().getFolderLink(projectName).click());
    }

    /* Will navigate to a folder or subfolder of the specified project */
    public void navigateToFolder(String projectName, String folder)
    {
        if (!getCurrentProject().equals(projectName))   //only hover the project link if it's different... right?
            mouseOverProjectLink(projectName);
        expandFolderLinksTo(folder);
        getWrapper().doAndWaitForPageToLoad(()->newElementCache().getFolderLink(folder).click());
    }

    /* opens the expandos until the desired folder link is in view, or
    * no expandos are present in the folder list container*/
    public void expandFolderLinksTo(String folder)
    {
        open();
        Locator expandoLoc = Locator.tagWithClass("li", "clbl collapse-folder")
                .child(Locator.tagWithClass("span", "marked"));

        WebDriverWrapper.waitFor(()-> {
            if (folderLinkIsPresent(folder) || expandoLoc.findElementOrNull(newElementCache().folderListContainer())==null)
                return true;
            else    // expand any collapsed expandos until the navigation link becomes clickable
            {
                WebElement expando = expandoLoc        // expandos are 'clbl expand-folder' when opened
                        .findElementOrNull(newElementCache().folderListContainer());
                if (expando != null)
                    expando.click();
            }
            return false;
        }, WAIT_FOR_JAVASCRIPT);
    }

    public void navigateToContainer(String project, String... subfolders)
    {
        if (subfolders.length == 0)
            navigateToProject(project);
        else
            throw new IllegalArgumentException("Navigating a specific subfolder path is not yet supported"); // TODO
    }

    /* This is a way to discover via the UI if the specified project exists (or did the last time the
        * UI refreshed). */
    public boolean projectLinkExists(String projectName)
    {
        open();
        return Locator.tag("li").childTag("a").withAttribute("data-field", projectName).notHidden()
                .findElementOrNull(elementCache().menu) != null;
    }

    /* real-time check to see if the destination nav-link (folder or project) is present and visible*/
    public boolean folderLinkIsPresent(String navigationLinkText)
    {
        WebElement linkElement = Locator.tag("li").childTag("a").withText(navigationLinkText).notHidden()
                .findElementOrNull(newElementCache().folderListContainer());
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
        WebElement menuToggle = Locator.tagWithAttribute("a", "data-toggle", "dropdown").refindWhenNeeded(menuContainer());
        WebElement menu = Locator.tagWithClass("ul", "dropdown-menu").refindWhenNeeded(menuContainer());

        WebElement projectNavTrail = Locator.tagWithClass("div", "lk-project-nav-trail").refindWhenNeeded(menu);
        WebElement projectNavBtnContainer = Locator.tagWithClass("div", "beta-nav-buttons").refindWhenNeeded(menu);

        WebElement newProjectButton = Locator.xpath("//span/a[@title='New Project']").refindWhenNeeded(projectNavBtnContainer);
        WebElement newSubFolderButton = Locator.xpath("//span/a[@title='New Subfolder']").refindWhenNeeded(projectNavBtnContainer);

        WebElement menuContainer()
        {
            return Locators.menuProjectNav.waitForElement(getComponentElement(), WAIT_FOR_JAVASCRIPT);
        }
        WebElement folderListContainer()
        {
            return Locator.tagWithClass("div", "folder-list-container").waitForElement(menuContainer(), WAIT_FOR_JAVASCRIPT);
        }

        WebElement getMenuItem(String text)
        {
            return Locator.tag("li").childTag("a").withAttribute("data-field", text).notHidden().waitForElement(menu, WAIT_FOR_JAVASCRIPT);
        }
        List<WebElement> getProjectMenuLinks()
        {
            return Locator.tag("li").childTag("a").withAttribute("data-field").notHidden().findElements(menuContainer());
        }
        WebElement getFolderLink(String text)
        {
            return Locator.tag("li").childTag("a").withText(text).notHidden().waitForElement(folderListContainer(), WAIT_FOR_JAVASCRIPT);
        }
    }

    public static class Locators
    {
        public static final Locator labkeyPageNavbar = Locator.tagWithClass("nav", "labkey-page-nav")
                .withDescendant(Locator.tagWithClass("div", "navbar-header"));
        public static final Locator menuProjectNav = Locator.tagWithClassContaining("li", "dropdown")
                .withAttribute("data-name", "BetaNav");
    }
}