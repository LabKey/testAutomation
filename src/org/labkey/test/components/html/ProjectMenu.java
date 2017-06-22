package org.labkey.test.components.html;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


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
        _el = Locators.lableyPageNavbar.refindWhenNeeded(driver).withTimeout(WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
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

        WebElement projectNavTrail = Locator.tagWithClass("div", "lk-project-nav-trail").refindWhenNeeded(menu);
        WebElement projectNavBtnContainer = Locator.tagWithClass("div", "lk-project-nav-buttons").refindWhenNeeded(menu);

        WebElement newProjectButton = Locator.xpath("//div/span/a[@title='New Project']").refindWhenNeeded(projectNavBtnContainer);
        WebElement newSubFolderButton = Locator.xpath("//div/span/a[@title='New Subfolder']").refindWhenNeeded(projectNavBtnContainer);

        WebElement getMenuItem(String text)
        {
            return Locator.tag("li").childTag("a").withText(text).notHidden().findElement(menu);
        }
    }

    public static class Locators
    {
        public static final Locator lableyPageNavbar = Locator.xpath("//nav[@class='labkey-page-nav']")
                .withChild(Locator.tagWithClass("div", "container").childTag("div").withClass("navbar-header"));
        public static final Locator menuProjectNav = Locator.tagWithClassContaining("li", "dropdown")
                .withAttribute("data-name", "MenuProjectNav");
        public static final Locator containerMobile = Locator.tagWithId("li", "project-mobile");
    }
}