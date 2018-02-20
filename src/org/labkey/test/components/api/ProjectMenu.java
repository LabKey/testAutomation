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
import org.labkey.test.pages.admin.CreateProjectPage;
import org.labkey.test.pages.admin.CreateSubFolderPage;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/**
 * Wraps the project/folder menu nav in labkey pages
 * Defined in org/labkey/core/project/folderNav.jsp and {@link org.labkey.api.view.menu.FolderMenu}
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
        return elementCache().menuToggle.getText().replace("&nbsp;", "").trim();
    }

    private boolean isOpen()
    {
        return elementCache().menuContainer.getAttribute("class").contains("open");
    }

    public ProjectMenu open()
    {
        if (!isOpen())
        {
            getWrapper().executeScript("window.scrollTo(0,0);");
            if (getWrapper().isElementPresent(Locator.css("li.dropdown.open > .lk-custom-dropdown-menu")))
                getWrapper().mouseOver(elementCache().menuToggle); // Just need to hover if another menu is already open
            else
                elementCache().menuToggle.click();
            WebDriverWrapper.waitFor(this::isOpen, "Project menu didn't open", 2000);
            getWrapper().waitForElement(Locator.tagWithClass("div", "folder-nav"));
        }
        return this;
    }

    public ProjectMenu close()
    {
        if (isOpen())
            elementCache().menuToggle.click();
        WebDriverWrapper.waitFor(()-> !isOpen(), "Menu didn't close", 1000);
        clearElementCache();
        return this;
    }

    public void navigateToProject(String projectName)
    {
        open();
        getWrapper().clickAndWait(elementCache().findProjectLink(projectName, false));
    }

    /* Will navigate to a folder or subfolder of the specified project */
    public void navigateToFolder(String projectName, String folder)
    {
        if (projectName.equals(folder) | projectName.equals("/"))
        {
            TestLogger.log("WARNING: Don't use folder navigation helper to navigate to projects");
            navigateToProject(folder);
            return;
        }
        if (projectName.equals("home"))
            projectName = "Home";
        open();
        getWrapper().clickAndWait(expandToFolder(projectName, folder));
    }

    /* opens the expandos until the desired folder link is in view */
    private WebElement expandToFolder(String project, String subfolder)
    {
        open();

        WebElement projectNode = elementCache().findProjectNode(project);
        WebElement folderLink = Locators.folderTreeLink.withText(subfolder).findElement(projectNode);
        Locator collapsedFolderLoc = Locators.collapsedFolderTreeNode
                .withDescendant(Locators.folderTreeLink.withText(subfolder));

        List<WebElement> collapsedNodes = collapsedFolderLoc.findElements(projectNode);

        for (WebElement folderTreeNode : collapsedNodes)
        {
            expandFolderTreeNode(folderTreeNode);
        }

        return getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(folderLink));
    }

    private WebElement expandAllUnder(WebElement parent)
    {
        for (WebElement folderTreeNode : Locators.collapsedFolderTreeNode.findElements(parent))
        {
            expandFolderTreeNode(folderTreeNode);
        }

        return parent;
    }

    private void expandFolderTreeNode(WebElement folderTreeNode)
    {
        Locators.childExpando.findElement(folderTreeNode).click();
        getWrapper().shortWait().until(ExpectedConditions.attributeContains(folderTreeNode, "class", "expand-folder"));
    }

    /**
     * Expand all subfolders under the specified project
     */
    public WebElement expandProjectFully(String project)
    {
        open();
        return expandAllUnder(elementCache().findProjectNode(project));
    }

    /**
     * Expand all subfolders under the specified folder
     */
    public WebElement expandFolderFully(String project, String folder)
    {
        expandToFolder(project, folder);
        return expandAllUnder(elementCache().findProjectNode(project));
    }

    public WebElement expandAll()
    {
        open();
        return expandAllUnder(elementCache().folderTree);
    }

    /* This is a way to discover via the UI if the specified project exists (or did the last time the
        * UI refreshed). */
    public boolean projectLinkExists(String projectName)
    {
        open();
        return elementCache().findProjectLink(projectName, true) != null;
    }

    public List<WebElement> projectMenuLinks()
    {
        return elementCache().getProjectMenuLinks();
    }

    /**
     * Creates a subfolder from the context you are already in (e.g. project/folder)
     */
    public CreateSubFolderPage navigateToCreateSubFolderPage()
    {
        open();
        getWrapper().clickAndWait(elementCache().newSubFolderButton);
        return new CreateSubFolderPage(getDriver());
    }

    public CreateProjectPage navigateToCreateProjectPage()
    {
        open();
        getWrapper().clickAndWait(elementCache().newProjectButton);
        return new CreateProjectPage(getDriver());
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
        final WebElement menuContainer = Locators.menuProjectNav.refindWhenNeeded(getComponentElement());
        final WebElement menuToggle = Locator.tagWithAttribute("a", "data-toggle", "dropdown").refindWhenNeeded(menuContainer);
        final WebElement menu = Locator.tagWithClass("ul", "dropdown-menu").refindWhenNeeded(menuContainer);
        final WebElement folderTree = Locator.tagWithClass("div", "folder-nav").refindWhenNeeded(menuContainer);

        List<WebElement> getProjectMenuLinks()
        {
            return Locators.childFolderTreeLink.findElements(folderTree);
        }

        WebElement findProjectLink(String projectName, boolean acceptNull)
        {
            Locator.XPathLocator linkLoc = Locators.childFolderTreeLink.withText(projectName);

            if (acceptNull)
                return linkLoc.findElementOrNull(folderTree);
            return linkLoc.findElement(folderTree);
        }

        WebElement findProjectNode(String projectName)
        {
            Locator.XPathLocator nodeLoc = Locators.childFolderTreeNode(projectName);
            return nodeLoc.findElement(folderTree);
        }

        final WebElement projectNavBtnContainer = Locator.tagWithClass("div", "folder-menu-buttons").refindWhenNeeded(menu);
        final WebElement newProjectButton = Locator.xpath("//span/a[@title='New Project']").refindWhenNeeded(projectNavBtnContainer);
        final WebElement newSubFolderButton = Locator.xpath("//span/a[@title='New Subfolder']").refindWhenNeeded(projectNavBtnContainer);
    }

    public static class Locators
    {
        public static final Locator labkeyPageNavbar = Locator.tagWithClass("nav", "labkey-page-nav")
                .withDescendant(Locator.tagWithClass("div", "navbar-header"));
        public static final Locator menuProjectNav = Locator.tagWithClassContaining("li", "dropdown")
                .withAttribute("data-name", "FolderNav");

        private static final Locator.XPathLocator folderTreeNode = Locator.tag("li").withClass("folder-tree-node");
        private static final Locator.XPathLocator folderTreeLink = folderTreeNode.childTag("a");
        private static final Locator.XPathLocator folderTreeFolder = folderTreeNode.childTag("*").position(2); // Finds readable and unreadable folders
        private static final Locator.XPathLocator childFolderTreeNode = Locator.xpath("./ul").child(folderTreeNode);
        private static final Locator.XPathLocator childFolderTreeLink = childFolderTreeNode.childTag("a");
        private static final Locator.XPathLocator childFolderTreeFolder = childFolderTreeNode.childTag("*").position(2); // Finds readable and unreadable folders
        private static final Locator.XPathLocator childExpando = Locator.xpath("./span").withClass("marked");
        private static final Locator.XPathLocator collapsedFolderTreeNode = Locator.xpath("descendant-or-self::li").withClass("collapse-folder");

        private static Locator.XPathLocator folderTreeNode(String folder)
        {
            return folderTreeNode.withChild(Locator.tag("*").position(2).withText(folder));
        }

        private static Locator.XPathLocator childFolderTreeNode(String folder)
        {
            return childFolderTreeNode.withChild(Locator.tag("*").position(2).withText(folder));
        }
    }
}