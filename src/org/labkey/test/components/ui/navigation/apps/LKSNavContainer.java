package org.labkey.test.components.ui.navigation.apps;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Optional;

public class LKSNavContainer extends BaseNavContainer
{
    protected LKSNavContainer(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    public ProductsNavContainer clickBack()
    {
        elementCache().backLink.click();
        return new ProductsNavContainer.ProductNavContainerFinder(getDriver()).withTitle("Applications").waitFor();
    }

    public void clickHome()
    {
        getWrapper().clickAndWait(elementCache().homeLink);
    }

    public void clickProject(String project)
    {
        getWrapper().clickAndWait(elementCache().projectLink(project));
    }

    public boolean hasTabs()
    {
        return tabLinks().size() > 0;
    }

    public boolean hasEmptyTabNotification()
    {
        return elementCache().noTabsNotice().isPresent();
    }

    private List<WebElement> tabLinks()
    {
        return Locator.tagWithClass("div", "clickable-item").findElements(elementCache().tabContainer);
    }

    public List<String> tabTexts()
    {
        return getWrapper().getTexts(tabLinks());
    }

    public void clickTab(String tabText)
    {
        WebElement link = Locator.tagWithClass("div", "clickable-item").withText(tabText)
                .waitForElement(elementCache().tabContainer, 2000);
        getWrapper().clickAndWait(link);
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
        final WebElement backLink = Locator.tagWithClass("span", "header-title")
                .withChild(Locator.tagWithClass("i", "back-icon"))
                .findWhenNeeded(this).withTimeout(2000);
        final WebElement homeLink = Locator.tagWithClass("a", "container-item").withText("LabKey Home")
                .findWhenNeeded(navList).withTimeout(2000);
        final WebElement tabContainer = Locator.tagWithClass("div", "container-tabs")
                .findWhenNeeded(navList).withTimeout(2000);
        Optional<WebElement> noTabsNotice()
        {
            return Locator.tagWithClass("div", "empty").findOptionalElement(tabContainer);
        }

        WebElement projectLink(String project)
        {
            return Locator.tagWithClass("a", "container-item").withText(project)
                    .waitForElement(navList, 2000);
        }
    }

    public static class LKSNavContainerFinder extends BaseNavContainerFinder<LKSNavContainer, LKSNavContainer.LKSNavContainerFinder>
    {
        public LKSNavContainerFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected LKSNavContainer construct(WebElement el, WebDriver driver)
        {
            return new LKSNavContainer(el, driver);
        }
    }
}
