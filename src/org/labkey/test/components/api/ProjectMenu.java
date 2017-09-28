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

import org.apache.commons.lang3.NotImplementedException;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.pages.admin.CreateSubFolderPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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

    private boolean isOpen()
    {
        return elementCache().menuContainer().getAttribute("class").contains("open");
    }

    /**
     * expand tree should take in an arbitrary number of strings. It works it's way down the list, stoping
     * when it finds the last string. Passing in zero strings should result in the entire tree being expanded.
     */
    public void expandTree(String... menuPath)
    {
        if (menuPath.length == 0)
        {
            expandTree(null, null);
        }
        else if (menuPath.length == 1)
        {
            expandTree(null, menuPath[0]);
        }
        else if (menuPath.length == 2)
        {
            expandTree(menuPath[0], menuPath[1]);
        }
        else
        {
            throw new NotImplementedException("ExpandTree cannot yet except arbitrary paths.");
        }
    }

    private void expandTree(String project, String terminal)
    {
        WebElement context = elementCache().menuContainer();
        if (project != null)
        {
            context = elementCache().getMenuRow(project).findElementOrNull(elementCache().menuContainer());
        }

        List<WebElement> expandoButtons;
        while (!(expandoButtons = Locator.tag("li")
                .withClass("collapse-folder")
                .childTag("span")
                .findElements(context)).isEmpty())
        {
            for (WebElement expandoButton : expandoButtons)
            {
                if (terminal != null && elementCache().getMenuLink(terminal) != null)
                {
                    return;
                }
                expandoButton.click();
            }
        }
    }

    public ProjectMenu open()
    {
        if (!isOpen())
        {
            if (getWrapper().isElementPresent(Locator.css("li.dropdown.open > .lk-custom-dropdown-menu")))
                getWrapper().mouseOver(elementCache().menuToggle); // Just need to hover if another menu is already open
            else
                elementCache().menuToggle.click();
        }
        WebDriverWrapper.waitFor(this::isOpen, 1000);
        return this;
    }

    public ProjectMenu close()
    {
        if (isOpen())
            getWrapper().mouseOut(); // more in line with the user experience than clicking the toggle.
        WebDriverWrapper.waitFor(()-> !isOpen(), "Menu didn't close", 1000);
        return this;
    }

    public void navigateToMenuLink(String... menuPath)
    {
        open();
        expandTree(menuPath);
        getWrapper().doAndWaitForPageToLoad(()-> elementCache().getMenuLink(menuPath).click());
    }

    /* This is a way to discover via the UI if the specified project exists (or did the last time the
        * UI refreshed). */
    public boolean menuLinkExists(String... menuPath)
    {
        open();
        return elementCache().getMenuLink(menuPath) != null;
    }

    /* real-time check to see if the destination nav-link (folder or project) is present and visible*/
    public boolean menuLinkIsPresent(String... menuPath)
    {
        WebElement linkElement = elementCache().getMenuLink(menuPath);
        return linkElement != null && linkElement.isDisplayed();
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
        getWrapper().doAndWaitForPageToLoad(() -> elementCache().newSubFolderButton.click());
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
        WebElement anyRow = Locator.tagWithClass("ul", "dropdown-menu").childTag("li").refindWhenNeeded(menuContainer());

        WebElement projectNavTrail = Locator.tagWithClass("div", "lk-project-nav-trail").refindWhenNeeded(menu);
        WebElement projectNavBtnContainer = Locator.tagWithClass("div", "beta-nav-buttons").refindWhenNeeded(menu);

        WebElement newProjectButton = Locator.xpath("//span/a[@title='New Project']").refindWhenNeeded(projectNavBtnContainer);
        WebElement newSubFolderButton = Locator.xpath("//span/a[@title='New Subfolder']").refindWhenNeeded(projectNavBtnContainer);

        WebElement menuContainer()
        {
            return Locators.menuProjectNav.waitForElement(getComponentElement(), WAIT_FOR_JAVASCRIPT);
        }

        List<WebElement> getProjectMenuLinks()
        {
            return Locator.tag("li").childTag("a").withAttribute("data-field").notHidden().findElements(menuContainer());
        }

        Locator.XPathLocator getMenuRow(String... path)
        {
            return getMenuRowLocator(new LinkedList<>(Arrays.asList(path)));
        }

        WebElement getMenuLink(String... path)
        {
            return getMenuRow(path).childTag("a").findElementOrNull(menuContainer());
        }

        WebElement getMenuExpando(String... path)
        {
            return getMenuRow(path).childTag("span").findElementOrNull(menuContainer());
        }

        private Locator.XPathLocator getMenuRowLocator(Queue<String> path)
        {
            Locator.XPathLocator base = Locator.tag("li").childTag("a").withText(path.remove()).parent();
            if (!path.isEmpty())
                return base.descendant(getMenuRowLocator(path));
            else
                return base;
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