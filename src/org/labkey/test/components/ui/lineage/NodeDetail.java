package org.labkey.test.components.ui.lineage;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Automates the labkey UI for node details (implemented in /components/lineage/LineageNodeList.tsx)
 */
public class NodeDetail extends WebDriverComponent<NodeDetail.ElementCache>
{
    private static final Locator.XPathLocator NAME_LOC = Locator.XPathLocator.union(
            Locator.tagWithClass("a", "lineage-link"),
            Locator.tag("span"));

    final WebElement _el;
    final WebDriver _driver;

    public NodeDetail(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    @Override
    protected void waitForReady()
    {
        getWrapper().shortWait().until(ExpectedConditions.visibilityOf(getComponentElement()));
    }

    public String getName()
    {
       return elementCache().nameElement.getText();
    }

    public void clickOverViewLink(boolean wait)
    {
        clickHiddenLink(elementCache().overviewLink, wait);
    }

    public void clickLineageGraphLink(boolean wait)
    {
        clickHiddenLink(elementCache().lineageGraphLink, wait);
    }

    private void clickHiddenLink(WebElement link, boolean wait)
    {
        getWrapper().scrollIntoView(getComponentElement());
        getWrapper().mouseOver(link);
        new WebDriverWait(getDriver(), Duration.ofSeconds(2)).until(ExpectedConditions.elementToBeClickable(link));
        if (wait)
            getWrapper().clickAndWait(link);
        else
            link.click();
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

        final WebElement overviewLink = Locator.tagWithClass("a", "lineage-data-link--text")
                .withText("Overview").findWhenNeeded(this).withTimeout(2000);
        final WebElement lineageGraphLink = Locator.tagWithClass("a", "lineage-data-link--text")
                .withText("Lineage").findWhenNeeded(this).withTimeout(2000);
        final WebElement nameElement = NAME_LOC.findElement(this);
    }

    public static class NodeDetailItemFinder extends WebDriverComponentFinder<NodeDetail, NodeDetailItemFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tag("li").child(Locator.tagWithClass("div", "lineage-name"));
        private Locator _locator = _baseLocator;

        public NodeDetailItemFinder(WebDriver driver)
        {
            super(driver);
        }

        public NodeDetailItemFinder withTitle(String title)
        {
            _locator = _baseLocator.withAttribute("title", title);
            return this;
        }

        public NodeDetailItemFinder withName(String name)
        {
            _locator = _baseLocator.withChild(NAME_LOC.withText(name));
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
           return _locator;
        }
    }
}
