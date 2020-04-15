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
        if (isNameLinked())
            return elementCache().nameLink.findElement(this).getText();
        else
            return elementCache().nameSpan.findElement(this).getText();
    }

    private boolean isNameLinked()
    {
        return elementCache().nameSpan.existsIn(this);
    }

    public void clickOverViewLink()
    {
        getWrapper().mouseOver(getComponentElement());
        getWrapper().waitFor(()-> elementCache().overviewLink.isEnabled(), 1000);
        getWrapper().clickAndWait(elementCache().overviewLink);
    }

    public void getLineageGraphLink()
    {
        getWrapper().mouseOver(getComponentElement());
        getWrapper().waitFor(()-> elementCache().lineageGraphLink.isEnabled(), 1000);
        getWrapper().clickAndWait(elementCache().lineageGraphLink);
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

        final WebElement overviewLink = Locator.linkWithSpan("Overview")
                .findWhenNeeded(this).withTimeout(2000);
        final WebElement lineageGraphLink = Locator.linkWithSpan("Lineage")
                .findWhenNeeded(this).withTimeout(2000);
        final Locator nameLink = Locator.tagWithClass("a", "pointer");
        final Locator nameSpan = Locator.tag("span");
    }

    public static class NodeDetailItemFinder extends WebDriverComponentFinder<NodeDetailItem, NodeDetailItemFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("li", "lineage-name");
        private String _name = null;
        private String _linkedName = null;
        private String _title = null;

        public NodeDetailItemFinder(WebDriver driver)
        {
            super(driver);
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
           if (_title != null)
                return _baseLocator.withAttribute("title", _title);
            else
                return _baseLocator;
        }
    }
}
