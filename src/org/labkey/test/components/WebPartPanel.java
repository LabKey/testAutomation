package org.labkey.test.components;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class WebPartPanel extends WebDriverComponent
{
    private WebElement _componentElement;
    private WebDriver _driver;

    protected WebPartPanel(WebElement componentElement, WebDriver driver)
    {
        _componentElement = componentElement;
        _driver = driver;
    }

    public static WebPartFinder WebPart(WebDriver driver)
    {
        return new WebPartFinder(driver)
        {
            @Override
            protected WebDriverComponent construct(WebElement el, WebDriver driver)
            {
                return new WebPartPanel(el, driver);
            }
        };
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _componentElement;
    }

    public static abstract class WebPartFinder<C extends WebPartPanel, F extends WebPartFinder<C, F>> extends WebDriverComponentFinder<C, F>
    {
        private String _title;
        private boolean _partialTitle = true;

        public WebPartFinder(WebDriver driver)
        {
            super(driver);
        }

        public F withTitle(String title)
        {
            _title = title;
            _partialTitle = false;
            return (F)this;
        }

        public F withTitleContaining(String partialTitle)
        {
            _title = partialTitle;
            _partialTitle = true;
            return (F)this;
        }

        @Override
        protected Locator locator()
        {
            Locator.XPathLocator webPartTitle = Locator.xpath("tbody/tr/th/span").withClass("labkey-wp-title-text");
            webPartTitle = _partialTitle ? webPartTitle.containing(_title) : webPartTitle.withText(_title);
            return Locator.tagWithClass("table", "labkey-wp").withDescendant(webPartTitle);
        }
    }
}
