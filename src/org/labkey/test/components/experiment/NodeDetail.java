package org.labkey.test.components.experiment;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class NodeDetail extends WebDriverComponent<NodeDetail.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public NodeDetail(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    public String getName()
    {
       return elementCache().nameElement.getText();
    }

    private boolean isNameLinked()
    {
        return elementCache().nameSpan.existsIn(this);
    }

    public void clickOverViewLink(boolean wait)
    {
        getWrapper().mouseOver(getComponentElement());
        getWrapper().waitFor(()-> elementCache().overviewLink.isEnabled(), 1000);
        if (wait)
            getWrapper().clickAndWait(elementCache().overviewLink);
        else
            elementCache().overviewLink.click();
    }

    public void clickLineageGraphLink()
    {
        getWrapper().mouseOver(getComponentElement());
        getWrapper().waitFor(()-> elementCache().lineageGraphLink.isEnabled(), 1000);
        getWrapper().clickAndWait(elementCache().lineageGraphLink);
    }

    public WebElement getIcon()
    {
        return elementCache().icon;
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
        final WebElement icon = Locator.tagWithClass("img", "lineage-sm-icon")
                .findWhenNeeded(this);

        final WebElement overviewLink = Locator.linkWithSpan("Overview")
                .findWhenNeeded(this).withTimeout(2000);
        final WebElement lineageGraphLink = Locator.linkWithSpan("Lineage")
                .findWhenNeeded(this).withTimeout(2000);
        final Locator.XPathLocator nameLink = Locator.tagWithClass("a", "pointer");
        final Locator.XPathLocator nameSpan = Locator.tag("span");
        final WebElement nameElement = Locator.XPathLocator.union(nameLink, nameSpan).findElement(this);
    }

    public static class NodeDetailItemFinder extends WebDriverComponentFinder<NodeDetail, NodeDetailItemFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("li", "lineage-name");
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
        protected NodeDetail construct(WebElement el, WebDriver driver)
        {
            return new NodeDetail(el, driver);
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
