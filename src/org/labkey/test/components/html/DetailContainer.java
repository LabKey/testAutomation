package org.labkey.test.components.html;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


public class DetailContainer extends WebDriverComponent<DetailContainer.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected DetailContainer(WebElement element, WebDriver driver)
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

    public WebElement body()
    {
        return elementCache().body;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        WebElement header = Locators.headerLoc.findWhenNeeded(this);
        WebElement body = Locators.bodyLoc.findWhenNeeded(this);
    }

    static public class Locators
    {
        static public Locator.XPathLocator panelLoc = Locator.tagWithClass("div", "panel-default");
        static public Locator.XPathLocator headerLoc = Locator.tagWithClass("div", "panel-heading");
        static public Locator.XPathLocator bodyLoc = Locator.tagWithClass("div", "panel-body");
        static public Locator panelWithTitle(String title)
        {
            return panelLoc.withChild(headerLoc.withText(title));
        }
    }

    public static class DetailContainerFinder extends WebDriverComponentFinder<DetailContainer, DetailContainerFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locators.panelLoc;
        private String _title = null;

        public DetailContainerFinder(WebDriver driver)
        {
            super(driver);
        }

        public DetailContainerFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected DetailContainer construct(WebElement el, WebDriver driver)
        {
            return new DetailContainer(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return Locators.panelWithTitle(_title);
            else
                return _baseLocator;
        }
    }
}
