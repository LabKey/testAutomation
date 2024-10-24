package org.labkey.test.components.ui.navigation.apps;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class LeafNavContainer extends BaseNavContainer
{
    protected LeafNavContainer(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    public ProductsNavContainer clickBack()
    {
        elementCache().backLink.click();
        return new ProductsNavContainer.ProductNavContainerFinder(getDriver()).withTitle("Applications").waitFor();
    }

    public List<String> getItems()
    {
        return getWrapper().getTexts(elementCache().clickableItem.findElements(elementCache().navList));
    }

    public void clickItem(String itemText)
    {
        clickItem(itemText, WAIT_FOR_JAVASCRIPT);
    }

    public void clickItem(String itemText, int wait)
    {
        String currentUrl = getDriver().getCurrentUrl();
        var item = elementCache().clickableItem.withText(itemText).waitForElement(elementCache().navList, 2000);
        // if navigating to an item in the current application, the document itself may not reload, so a clickAndWait won't succeed.
        // Look for a change in the URL instead.
        item.click();
        WebDriverWrapper.waitFor(()->!currentUrl.equals(getDriver().getCurrentUrl()), "Failed to navigate to " + itemText + ".", 1000);
    }

    @Override
    protected ElementCache elementCache()
    {
        return (LeafNavContainer.ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseNavContainer.ElementCache
    {
        final Locator clickableItem = Locator.tagWithClass("a", "clickable-item");
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
