package org.labkey.test.components.ui.lineage;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.ui.grids.DetailTable;
import org.labkey.test.components.ui.grids.ResponsiveGrid;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.labkey.test.WebDriverWrapper.sleep;

/**
 * Automates the Labkey UI component implemented in /components/lineage/LineageGraph.tsx
 */
public class LineageGraph extends WebDriverComponent<LineageGraph.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    protected LineageGraph(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    /**
     * Switch from old 'graphviz' run graph to the "Beta" run graph
     * "Beta" run graph might be the default if 'graphviz' isn't installed
     * Assumes the caller has already navigated to 'experiment-showRunGraph.view' somehow
     */
    public static LineageGraph showLineageGraph(WebDriver driver)
    {
        LineageGraph lineageGraph = new LineageGraph.LineageGraphFinder(driver).findWhenNeeded();
        if (!lineageGraph.getComponentElement().isDisplayed())
        {
            Locator.linkWithSpan("Toggle Beta Graph (new!)").findElement(driver).click();
        }
        new WebDriverWait(driver, Duration.ofSeconds(2)).until(ExpectedConditions.visibilityOf(lineageGraph.getComponentElement()));
        return lineageGraph;
    }

    public Map<String, String> getCurrentNodeData()
    {
       return elementCache().detailTable().getTableData();
    }

    public WebElement getFocusedNodeImage()
    {
        return elementCache().componentDetailImage;
    }

    public String getFocusedNodeText()
    {
        return elementCache().nodeDetailName.getText();
    }

    /**
     * finds the list of edges/nodes to the one currently focused, by name
     * @param listTitle The intended node's title
     * @return a NodeDetailGroup for the specified node
     */
    public NodeDetailGroup getDetailGroup(String listTitle)
    {
        return elementCache().summaryList(listTitle);
    }

    public NodeDetail getNodeDetail(String title)
    {
        return new NodeDetail.NodeDetailItemFinder(getDriver()).withTitle(title)
                .waitFor(elementCache().nodeDetailContainer);
    }

    /**
     * finds all details lists in the details panels
     * @return a List of NodeDetailGroup, for all shown nodes
     */
    public List<NodeDetailGroup> getDetailGroups()
    {
        return elementCache().summaryLists();
    }

    /**
     * clicks the overview Link of the currently-selected node/element and optionally waits for a page load
     * @param wait  Whether or not to expect a page load, false if not
     */
    public void clickOverviewLink(boolean wait)
    {
        if (wait)
            getWrapper().clickAndWait(elementCache().nodeOverviewLink);
        else
            elementCache().nodeOverviewLink.click();
    }

    public boolean hasLineageLink()
    {
        return elementCache().lineageLinkLoc.existsIn(elementCache().nodeDetailLinksContainer);
    }

    public void clickLineageLink(boolean wait)
    {
        if (wait)
            getWrapper().clickAndWait(elementCache().nodeLineageLink);
        else
            elementCache().nodeLineageLink.click();
    }

    public void clickDetailsTab()
    {
        elementCache().detailsTab.click();
        sleep(1000); // bootstrap tab animation
    }

    public void clickRunPropertiesTab()
    {
        elementCache().runPropertiesTab.click();
        sleep(1000); // bootstrap tab animation
    }

    public void clickRunStepDetailsTab()
    {
        elementCache().runStepDetailsTab.click();
        sleep(1000); // bootstrap tab animation
    }

    public void clickProvenanceMapTab()
    {
        elementCache().provenanceMapTab.click();
        sleep(1000); // bootstrap tab animation
    }

    public ResponsiveGrid getProvenanceMapGrid()
    {
        return new ResponsiveGrid.ResponsiveGridFinder(getDriver()).inParentWithId("lineage-run-step-tabs-pane-2").find();
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
        // container for the graph
        final WebElement visGraphContainer = Locator.tagWithClass("div", "lineage-visgraph-ct")
                .findWhenNeeded(this);
        // container for the details of the currently-selected node
        final WebElement nodeDetailContainer = Locator.tagWithClass("div", "lineage-node-detail-container")
                .findWhenNeeded(this).withTimeout(4000);
        WebElement componentDetailImage = Locator.tagWithClass("i", "component-detail--child--img")
                .child(Locator.tag("img")).findWhenNeeded(nodeDetailContainer);
        WebElement nodeDetailName = Locator.tagWithClass("h4", "lineage-name-data")
                .findWhenNeeded(nodeDetailContainer);
        WebElement nodeDetailLinksContainer = Locator.tagWithClass("div", "lineage-node-detail")
                .findWhenNeeded(nodeDetailContainer);
        WebElement nodeOverviewLink = Locator.linkWithSpan("Overview").withClass("lineage-data-link--text")
                .findWhenNeeded(nodeDetailLinksContainer);
        Locator lineageLinkLoc = Locator.linkWithSpan("Lineage").withClass("lineage-data-link--text");
        WebElement nodeLineageLink = lineageLinkLoc.findWhenNeeded(nodeDetailLinksContainer);

        DetailTable detailTable()
        {
            return new DetailTable.DetailTableFinder(getDriver()).waitFor(nodeDetailContainer);
        }

        WebElement nodeDetails = Locator.tagWithClass("div", "lineage-node-detail")
                .findWhenNeeded(nodeDetailContainer).withTimeout(3000);
        WebElement nodeDetailsName = Locator.tagWithClass("div", "lineage-name-data")
                .findWhenNeeded(nodeDetails);


        WebElement detailsTab = Locator.css("a[role=\"tab\"]").withText("Details").findWhenNeeded(this);
        WebElement runPropertiesTab = Locator.css("a[role=\"tab\"]").withText("Run Properties").findWhenNeeded(this);
        WebElement runStepDetailsTab = Locator.css("a[role=\"tab\"]").withText("Step Details").findWhenNeeded(this);
        WebElement provenanceMapTab = Locator.css("a[role=\"tab\"]").withText("Provenance Map").findWhenNeeded(this);

        NodeDetailGroup summaryList(String nodeLabel)
        {
            return new NodeDetailGroup.NodeDetailsFinder(getDriver()).withTitle(nodeLabel).waitFor(nodeDetailContainer);
        }

        List<NodeDetailGroup> summaryLists()
        {
            return new NodeDetailGroup.NodeDetailsFinder(getDriver()).findAll(nodeDetailContainer);
        }
    }


    public static class LineageGraphFinder extends WebDriverComponentFinder<LineageGraph, LineageGraphFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "row")
                .withChild(Locator.tagWithClass("div", "col-md-8")
                .withChild(Locator.tagWithClass("div", "lineage-visgraph-ct")));

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
