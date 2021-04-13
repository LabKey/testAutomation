package org.labkey.test.components.ui.navigation.apps;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public abstract class BaseNavContainer extends WebDriverComponent<BaseNavContainer.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected BaseNavContainer(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }


    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        Locator.XPathLocator headerLoc = Locator.tagWithClass("h3", "product-navigation-header");

        final WebElement header = headerLoc.findWhenNeeded(this).withTimeout(2000);
        final WebElement navList = Locator.tagWithClass("ul", "product-navigation-listing")
                .findWhenNeeded(this).withTimeout(2000);

    }

    protected abstract static class BaseNavContainerFinder<P extends BaseNavContainer, F extends BaseNavContainerFinder<P, F>> extends WebDriverComponentFinder<P, F>
    {
        private Locator _locator;
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "product-navigation-container");

        protected BaseNavContainerFinder(WebDriver driver)
        {
            super(driver);
            _locator = _baseLocator;
        }

        public F withTitle(String title)
        {
            _locator =_baseLocator.withDescendant(Locator.tagWithClass("h3", "product-navigation-header")
                    .withChild(Locator.tagWithClass("span", "header-title").withText(title)));
            return getThis();
        }

        public F withBackNavTitle(String title)
        {
            _locator =_baseLocator.withDescendant(Locator.tagWithClass("h3", "product-navigation-header")
                    .withChild(Locator.tagWithClass("span", "header-title")
                            .withChild(Locator.tagWithClass("i", "back-icon")).startsWith(title)));
            return getThis();
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
