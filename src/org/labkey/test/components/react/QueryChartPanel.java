package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/*
    Wraps the chart-panel of a query grid when it is showing a chart
 */
public class QueryChartPanel extends WebDriverComponent<QueryChartPanel.ElementCache>
{
    private final QueryGrid _queryGrid;
    private final WebElement _el;
    private final WebDriver _driver;

    protected QueryChartPanel(WebElement element, WebDriver driver, QueryGrid queryGrid)
    {
        _el = element;
        _driver = driver;
        _queryGrid = queryGrid;
    }

    public QueryChartDialog clickEdit()
    {
        var editButton = elementCache().editButton;
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(editButton));
        editButton.click();
        return new QueryChartDialog("Edit Chart", getDriver(), _queryGrid);
    }

    public String getTitle()
    {
        return elementCache().titleElement.getText();
    }

    public WebElement getSvgChart()
    {
        return Locator.byClass("svg-chart").waitForElement(this, WAIT_FOR_JAVASCRIPT);
    }

    public QueryGrid clickClose()
    {
        var btn = elementCache().closeButton;
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(btn));
        btn.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(btn));
        return _queryGrid;
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

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public final WebElement headingEl = Locator.tagWithClass("div", "chart-panel__heading")
                .findWhenNeeded(this).withTimeout(2000);
        public final WebElement editButton = Locator.tagWithAttribute("button", "title", "Edit chart")
                .findWhenNeeded(headingEl);
        public final WebElement closeButton = Locator.tagWithAttribute("button", "title", "Hide chart")
                .findWhenNeeded(headingEl);
        public final WebElement titleElement= Locator.tagWithClass("div", "chart-panel__heading-title")
                .findWhenNeeded(headingEl);
    }


    public static class QueryChartPanelFinder extends WebDriverComponentFinder<QueryChartPanel, QueryChartPanelFinder>
    {
        private final QueryGrid _queryGrid;
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "chart-panel");

        public QueryChartPanelFinder(WebDriver driver, QueryGrid queryGrid)
        {
            super(driver);
            _queryGrid = queryGrid;
        }

        @Override
        protected QueryChartPanel construct(WebElement el, WebDriver driver)
        {
            return new QueryChartPanel(el, driver, _queryGrid);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
