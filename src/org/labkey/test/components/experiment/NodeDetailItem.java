package org.labkey.test.components.experiment;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class NodeDetailItem extends WebDriverComponent<NodeDetailItem.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public NodeDetailItem(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    public String getName()
    {
        return elementCache().name.getText();
    }

    public WebElement getOverViewLink()
    {
        return elementCache().overviewLink;
    }

    public WebElement getLineageGraphLink()
    {
        return elementCache().lineageGraphLink;
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
        final WebElement image = Locator.tagWithClass("img", "lineage-sm-icon")
                .findWhenNeeded(this);
        final WebElement name = Locator.tag("span")
                .findWhenNeeded(this);
        final WebElement overviewLink = Locator.linkWithSpan("Overview")
                .findWhenNeeded(this);
        final WebElement lineageGraphLink = Locator.linkWithSpan("Lineage")
                .findWhenNeeded(this);
    }

    public static class NodeDetailItemFinder extends WebDriverComponentFinder<NodeDetailItem, NodeDetailItemFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("li", "lineage-name");
        private String _name = null;
        private String _title = null;

        public NodeDetailItemFinder(WebDriver driver)
        {
            super(driver);
        }

        public NodeDetailItemFinder withName(String name)
        {
            _name = name;
            return this;
        }

        public NodeDetailItemFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected NodeDetailItem construct(WebElement el, WebDriver driver)
        {
            return new NodeDetailItem(el, driver);
        }


        @Override
        protected Locator locator()
        {
            if (_name != null)
                return _baseLocator.withChild(Locator.tagWithText("span", _name));
            else if (_title != null)
                return _baseLocator.withAttribute("title", _title);
            else
                return _baseLocator;
        }
    }
}
