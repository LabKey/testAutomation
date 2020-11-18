package org.labkey.test.components.glassLibrary.heatmap;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.util.TestLogger.log;

/**
 * Manipulates the HeatMap component from labkey-ui-components (components/src/internal/components/heatmap/HeatMap.tsx)
 */
public class HeatMap extends WebDriverComponent<HeatMap.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;
    private final Locator.XPathLocator _rowsLoc = Locator.tag("tbody").childTag("tr").withoutClass("grid-empty").withoutClass("grid-loading");
    private final Locator _loadingRowLoc = Locator.css("tbody tr.grid-loading");
    private final Locator _spinnerLoc = Locator.css("span i.fa-spinner");
    private final Locator _emptyRowLoc = Locator.css("tbody tr.grid-empty");
    private final Locator _rowLink = Locator.xpath("//td/div/a");

    protected HeatMap(WebElement element, WebDriver driver)
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

    /**
     * Gets the column label for the column at that index; for heatmaps with 12 months, the current month index is
     * 12, index 0 is empty string, 1 will be 'Jan', and 13 will be whatever the summary link column is
     * @param index 0-based; month values start at 1 because the name column is at 0
     * @return The string value of the header cell at that index
     */
    public String getColumnByIndex(int index)
    {
        return getColumnNames().get(index);
    }

    public List<String> getColumnNames()
    {
        List<WebElement> headerCells = elementCache().headerCells();
        return getWrapper().getTexts(headerCells);
    }

    /**
     * Each row has an anchor tag in its first column; usually it's a link to a sampletype or an assay.
     * We will refer to that link as the row's name
     * @return The text value of the name link (in column 0)
     */
    public List<String> getRowNames()
    {
        List<String> rowNames = new ArrayList<>();
        for (WebElement row : getRows())
        {
            WebElement rowLink = _rowLink.findElement(row);
            rowNames.add(rowLink.getText());
        }
        return rowNames;
    }

    public WebElement getRow(String linkText)
    {
        Locator.XPathLocator matchingLink = Locator.tag("td").append(Locator.linkWithText(linkText));
        return _rowsLoc.withChild(matchingLink).findElement(this);
    }

    public WebElement getRowLink(String linkText)
    {
        WebElement row = getRow(linkText);
        return Locator.linkWithText(linkText).waitForElement(row, WAIT_FOR_JAVASCRIPT);
    }

    public WebElement getSummaryLink(String rowName)
    {
        WebElement row = getRow(rowName);
        return Locator.tag("td").child("span").withPredicate("@title").child("a").findElement(row);
    }

    public WebElement getCell(String rowName, String headerText)
    {
        WebElement row = getRow(rowName);
        return Locator.css("td[headers=\"" + headerText + "\"]").findElement(row);
    }

    public String getCellColor(String rowName, String headerText)
    {
        WebElement cell = getCell(rowName, headerText);
        WebElement backgroundCell = Locator.tag("div").findElement(cell);
        return backgroundCell.getCssValue("background-color");
    }

    public String getToolTipTextForCell(String rowLinkText, String headerText)
    {
        WebElement cell = getCell(rowLinkText, headerText);
        getWrapper().mouseOver(cell);
        WebDriverWrapper.waitFor(()-> null != cell.getAttribute("aria-describedby"),
                "MouseOver did not cause cell to show tooltip", 2000);
        String toolTipId = cell.getAttribute("aria-describedby");
        WebElement toolTip = Locator.id(toolTipId).findElement(getDriver());
        return toolTip.getText();
    }

    public Boolean isLoaded()
    {
        return !_loadingRowLoc.existsIn(this) &&
                !_spinnerLoc.existsIn(this) &&
                _rowsLoc.existsIn(this);
    }

    public Boolean hasData()
    {
        if (isLoaded())
            return !_emptyRowLoc.existsIn(getComponentElement()) && _rowsLoc.existsIn(getComponentElement());
        else
            return false;
    }

    public HeatMap waitForRefresh()
    {
        WebDriverWrapper.waitFor(()-> {
            try{
                return hasData();
            }catch (IllegalStateException | StaleElementReferenceException comErr)
            {
                log("Error occurred in heatmap while waiting for refresh");
                return false;
            }
        }, "Took too long to refresh heatmap",  WAIT_FOR_JAVASCRIPT);
        return this;
    }

    public List<WebElement> getRows()
    {
        waitForRefresh();
        return _rowsLoc.findElements(getComponentElement());
    }

    protected void waitForLoaded()
    {
        WebDriverWrapper.waitFor(this::isLoaded, "Grid still loading", 30000);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public ElementCache()
        {
            waitForLoaded();
        }

        public List<WebElement> headerCells()
        {
            return Locator.tag("tr").child("th").findElements(this);
        }
    }


    public static class HeatMapFinder extends WebDriverComponentFinder<HeatMap, HeatMapFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "table-responsive")
                .withChild(Locator.tagWithClass("table", "heatmap-container"));

        public HeatMapFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected HeatMap construct(WebElement el, WebDriver driver)
        {
            return new HeatMap(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
