package org.labkey.test.components.ui.navigation.apps;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class LeafNavContainer extends BaseNavContainer
{
    protected LeafNavContainer(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    public ProjectsNavContainer clickBack()
    {
        elementCache().backLink.click();
        return new ProjectsNavContainer.ProjectsNavContainerFinder(getDriver()).waitFor();
    }

    public List<String> getItems()
    {
        return getWrapper().getTexts(elementCache().clickableItem.findElements(elementCache().navList));
    }

    public void clickItem(String itemText)
    {
        getWrapper().clickAndWait(elementCache().clickableItem.withText(itemText).waitForElement(elementCache().navList, 2000));
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
        final Locator clickableItem = Locator.tagWithClass("div", "clickable-item");
        final WebElement backLink = Locator.tagWithClass("span", "header-title")
                .child(Locator.tagWithClass("i", "back-icon")).findWhenNeeded(header).withTimeout(2000);
    }


    public static class LeafNavContainerFinder extends BaseNavContainerFinder<LeafNavContainer, LeafNavContainer.LeafNavContainerFinder>
    {
        public LeafNavContainerFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected LeafNavContainer construct(WebElement el, WebDriver driver)
        {
            return new LeafNavContainer(el, driver);
        }
    }
}
