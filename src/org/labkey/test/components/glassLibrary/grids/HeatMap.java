package org.labkey.test.components.glassLibrary.grids;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.util.TestLogger.log;

public class HeatMap extends WebDriverComponent<HeatMap.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;
    private final Locator.XPathLocator _rowsLoc = Locator.tag("tbody").childTag("tr").withoutClass("grid-empty").withoutClass("grid-loading");
    private final Locator _loadingRowLoc = Locator.css("tbody tr.grid-loading");
    private final Locator _spinnerLoc = Locator.css("span i.fa-spinner");
    private final Locator _emptyRowLoc = Locator.css("tbody tr.grid-empty");

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

    public String currentMonth()
    {
        return getColumnNames().get(12);
    }

    public String getColumnByIndex(int index)
    {
        return getColumnNames().get(index);
    }

    public List<String> getColumnNames()
    {
        List<WebElement> headerCells = elementCache().headerCells();
        return getWrapper().getTexts(headerCells);
    }

    public List<String> getEntityNames()
    {
        List<String> entityNames = new ArrayList<>();
        for (WebElement row : getRows())
        {
            WebElement entityLink = Locator.xpath("//td/div/a").findElement(row);
            entityNames.add(entityLink.getText());
        }
        return entityNames;
    }

    public Optional<WebElement> getOptionalRow(String linkText)
    {
        List<WebElement> rows = getRows();
        if(rows.size() == 0)  // try again; we expect there to be rows if we're here
        {
            new WebDriverWait(getDriver(), 10).until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("tbody tr"), 0));
            rows=getRows();
        }
        for (WebElement row : rows)
        {
            WebElement firstCell = Locator.tag("td").findElement(row);
            if (Locator.linkWithText(linkText).existsIn(firstCell))
            {
                return Optional.of(row);
            }
        }
        return Optional.empty();
    }

    public WebElement getEntityLink(String linkText)
    {
        WebElement row = getOptionalRow(linkText).get();
        return Locator.linkWithText(linkText).waitForElement(row, WAIT_FOR_JAVASCRIPT);
    }

    public WebElement getSummaryLink(String rowLinkText)
    {
        WebElement row = getOptionalRow(rowLinkText).get();
        return Locator.tagWithAttributeContaining("span", "title", " last 12 months")
                .child("a").findElement(row);
    }

    public WebElement getCell(String rowLinkText, String headerText)
    {
        WebElement row = getOptionalRow(rowLinkText).get();
        return Locator.css("td[headers=\"" + headerText + "\"]").findElement(row);
    }

    public String getCellColor(String rowLinkText, String headerText)
    {
        WebElement cell = getCell(rowLinkText, headerText);
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
                Locator.tag("td").existsIn(this);
    }

    public Boolean hasData()
    {
        if (!isLoaded())
            return false;
        else
        {
            try {
                List<WebElement> elements = _emptyRowLoc.findElements(getComponentElement());
                return elements.size() == 0;
            }
            catch (StaleElementReferenceException e)
            {
                // the emptyRow element is gone, which should mean there's now data.
                return true;
            }
        }
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
