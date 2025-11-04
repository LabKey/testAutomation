package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.labkey.test.components.ui.grids.ResponsiveGrid;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.io.File;

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

    public File clickExport(String subMenuText)
    {
        return getWrapper().doAndWaitForDownload(() -> elementCache().exportMenu.doMenuAction(subMenuText));
    }

    public String getTitle()
    {
        return elementCache().titleElement.getText();
    }

    public WebElement getSvgChart()
    {
        return Locator.byClass("svg-chart__chart").childTag("svg").waitForElement(this, WAIT_FOR_JAVASCRIPT);
    }

    public ResponsiveGrid<?> getCurveStatsGrid()
    {
        return new ResponsiveGrid.ResponsiveGridFinder(getDriver()).waitFor(elementCache().curveStatsPanel);
    }

    public boolean isCurveStatsPanelPresent()
    {
        return ElementCache.curveStatsPanelLoc.findOptionalElement(this).isPresent();
    }

    public File exportCurveStats(String type)
    {
        return getWrapper().doAndWaitForDownload(() -> elementCache().exportStatsMenu.doMenuAction(type));
    }

    public void clickClose()
    {
        var btn = elementCache().closeButton;
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(btn));
        btn.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(btn));
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
        public final WebElement headingEl = Locator.tagWithClass("div", "chart-panel__heading")
                .findWhenNeeded(this).withTimeout(2000);
        public final WebElement editButton = Locator.tagWithAttribute("button", "title", "Edit chart")
                .findWhenNeeded(headingEl);
        public final MultiMenu exportMenu = new MultiMenu.MultiMenuFinder(getDriver())
                .withButtonClass("chart-panel-export-btn")
                .findWhenNeeded(headingEl);
        public final WebElement closeButton = Locator.tagWithAttribute("button", "title", "Hide chart")
                .findWhenNeeded(headingEl);
        public final WebElement titleElement= Locator.tagWithClass("div", "chart-panel__heading-title")
                .findWhenNeeded(headingEl);
        public static final Locator curveStatsPanelLoc = Locator.byClass("curve-fit-statistics");
        public final WebElement curveStatsPanel = curveStatsPanelLoc.findWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        public final WebElement curveStatsHeader = Locator.byClass("curve-fit-statistics__header").findWhenNeeded(curveStatsPanel);
        public final MultiMenu exportStatsMenu = new MultiMenu.MultiMenuFinder(getDriver()).findWhenNeeded(curveStatsHeader);
    }


    public static class QueryChartPanelFinder extends WebDriverComponentFinder<QueryChartPanel, QueryChartPanelFinder>
    {
        private final QueryGrid _queryGrid;
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "chart-panel");
        private final String _name;

        public QueryChartPanelFinder(WebDriver driver, QueryGrid queryGrid, String name)
        {
            super(driver);
            _queryGrid = queryGrid;
            _name = name;
        }

        @Override
        protected QueryChartPanel construct(WebElement el, WebDriver driver)
        {
            return new QueryChartPanel(el, driver, _queryGrid);
        }

        @Override
        protected Locator locator()
        {
            Locator.XPathLocator headingLocator = Locator.tagWithClass("div", "chart-panel__heading").withChild(Locator.tagWithClass("div", "chart-panel__heading-title").containing(_name));
            return _baseLocator.withChild(headingLocator);
        }
    }
}
