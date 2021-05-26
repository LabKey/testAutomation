package org.labkey.test.components.ui.navigation.apps;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ProjectsNavContainer extends BaseNavContainer
{
    protected ProjectsNavContainer(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }


    public LeafNavContainer clickProject(String project)
    {
        elementCache().projectNavLink.withText(project).waitForElement(elementCache().navList, 2000).click();
        return new LeafNavContainer.LeafNavContainerFinder(getDriver()).withBackNavTitle(project).waitFor();
    }

    public List<String> getProjects()
    {
        return getWrapper().getTexts(elementCache().projectNavLink.findElements(elementCache().navList));
    }

    public ProductsNavContainer clickBack()
    {
        elementCache().backLink.click();
        return new ProductsNavContainer.ProductNavContainerFinder(getDriver()).withTitle("Applications").waitFor();
    }

    @Override
    protected ElementCache elementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseNavContainer.ElementCache
    {
        final Locator.XPathLocator projectNavLink = Locator.tagWithClass("div", "clickable-item")
                .withChild(Locator.tagWithClass("div", "nav-icon"));
        final WebElement backLink = Locator.tagWithClass("span", "header-title")
                .withChild(Locator.tagWithClass("i", "back-icon"))
                .findWhenNeeded(this).withTimeout(2000);
    }


    public static class ProjectsNavContainerFinder extends BaseNavContainerFinder<ProjectsNavContainer, ProjectsNavContainer.ProjectsNavContainerFinder>
    {
        public ProjectsNavContainerFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected ProjectsNavContainer construct(WebElement el, WebDriver driver)
        {
            return new ProjectsNavContainer(el, driver);
        }
    }
}
