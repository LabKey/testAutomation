package org.labkey.test.components.experiment;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class LineageGraph extends WebDriverComponent<LineageGraph.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public LineageGraph(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    /**
     * finds the list of edges/nodes to the one currently focused, by name
     * @param listTitle
     * @return
     */
    public NodeDetails getDetails(String listTitle)
    {
        return elementCache().summaryList(listTitle);
    }

    /**
     * finds all details lists in the details panels
     * @return
     */
    public List<NodeDetails> getDetails()
    {
        return elementCache().summaryLists();
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
        final WebElement visGraphContainer = Locator.tagWithClass("div", "lineage-visgraph-ct")
                .findWhenNeeded(this);
        final WebElement nodeDetailContainer = Locator.tagWithClass("div", "lineage-node-detail-container")
                .findWhenNeeded(this).withTimeout(4000);

        WebElement nodeDetails = Locator.tagWithClass("div", "lineage-node-detail")
                .findWhenNeeded(nodeDetailContainer).withTimeout(3000);
        WebElement nodeDetailsName = Locator.tagWithClass("div", "lineage-name-data")
                .findWhenNeeded(nodeDetails);

        NodeDetails summaryList(String nodeLabel)
        {
            return new NodeDetails.NodeDetailsFinder(getDriver()).withTitle(nodeLabel).find(nodeDetailContainer);
        }

        List<NodeDetails> summaryLists()
        {
            return new NodeDetails.NodeDetailsFinder(getDriver()).findAll(nodeDetailContainer);
        }
    }


    public static class LineageGraphFinder extends WebDriverComponentFinder<LineageGraph, LineageGraphFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithAttributeContaining("div", "id", "run-graph-app");

        public LineageGraphFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected LineageGraph construct(WebElement el, WebDriver driver)
        {
            return new LineageGraph(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return  _baseLocator;
        }
    }
}
