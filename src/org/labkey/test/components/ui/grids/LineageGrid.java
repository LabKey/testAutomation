package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.ui.Pager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wraps the component described in ui-components internal\components\lineage\grid\LineageGridDisplay.tsx
 */
public class LineageGrid extends WebDriverComponent<LineageGrid.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected LineageGrid(WebElement element, WebDriver driver)
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

    public int getRecordCount()
    {
        Locator pagingCountLoc = Locator.XPathLocator.union(
                Locator.tagWithClass("span","paging-counts-with-buttons"),
                Locator.tagWithClass("span","paging-counts-without-buttons"));
        return Integer.parseInt(pagingCountLoc.findElement(this).getAttribute("data-total"));
    }

    public boolean isShowingChildrenBtnEnabled()
    {
        return !elementCache().showChildrenBtn().getAttribute("class").contains("disabled");
    }

    public LineageGrid showChildren()
    {
        if (isShowingChildrenBtnEnabled())
            elementCache().showChildrenBtn().click();
        WebDriverWrapper.waitFor(()-> !isShowingChildrenBtnEnabled(),
                "the 'show children' button did not become disabled", 1000);
        return this;
    }

    public boolean isShowingParentsBtnEnabled()
    {
        return !elementCache().showParentsBtn().getAttribute("class").contains("disabled");
    }

    public LineageGrid showParents()
    {
        if (isShowingParentsBtnEnabled())
            elementCache().showParentsBtn().click();
        WebDriverWrapper.waitFor(()-> !isShowingParentsBtnEnabled(),
                "the 'show parents' button did not become disabled", 1000);
        return this;
    }

    public String getSeedMessage()
    {
        return elementCache().lineageSeedInfoEl().getText();
    }

    public String getSeedName()
    {
        return elementCache().seedNameEl().getText();
    }

    public Pager pager()
    {
        return new Pager.PagerFinder(getDriver(), elementCache().table).waitFor(this);
    }

    public List<LineageGridRow> getRows()
    {
        return new GridRow.GridRowFinder(elementCache().table)
                .findAll().stream().map(a-> new LineageGridRow(elementCache().table, a.getComponentElement()))
                .collect(Collectors.toList());
    }

    public Map<String, String> getRowMap(String lineageName)
    {
        return elementCache().table.getRow("ID", lineageName).getRowMap();
    }

    public List<Map<String, String>> getRowMaps()
    {
        return elementCache().table.getRowMaps();
    }

    public List<String> getLineageNamesOnPage()
    {
        return elementCache().table.getColumnDataAsText("ID");
    }

    public List<LineageGridRow> getLineageRows(String lineageName)
    {
        return getRows().stream().filter(a-> a.getLineageName().equals(lineageName))
                .collect(Collectors.toList());
    }

    public List<LineageGridRow> getDuplicates()
    {
        return getRows().stream().filter(a-> a.isDuplicate()).collect(Collectors.toList());
    }

    public List<LineageGridRow> getDuplicatesByName(String lineageName)
    {
        return getRows().stream().filter(a-> a.isDuplicate() && a.getLineageName().equals(lineageName))
                .collect(Collectors.toList());
    }

    public List<LineageGridRow> getFirstParents(String lineageName)
    {
        return getRows().stream().filter(a-> a.isFirstParent() && a.getLineageName().equals(lineageName))
                .collect(Collectors.toList());
    }

    public List<LineageGridRow> getSecondParent(String lineageName)
    {
        return getRows().stream().filter(a-> a.isSecondParent() && a.getLineageName().equals(lineageName))
                .collect(Collectors.toList());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        WebElement showParentsBtn()
        {
            return Locator.tagWithClass("a", "btn-success").withText("Show Parents")
                    .waitForElement( this, 2000);
        }

        WebElement showChildrenBtn()
        {
            return Locator.tagWithClass("a", "btn-success").withText("Show Children")
                    .waitForElement(this, 2000);
        }

        // seed label
        WebElement lineageSeedInfoEl()
        {
            return Locator.tagWithClass("div", "lineage-seed-info").findElement(this);
        }

        WebElement seedNameEl()
        {
            return Locator.tagWithClass("span", "lineage-seed-name").findElement(this);
        }


        ResponsiveGrid table = new ResponsiveGrid.ResponsiveGridFinder(getDriver()).find(this);

        WebElement generationLimitMsgEl = Locator.tagWithClass("div", "lineage-grid-generation-limit-msg")
                .findWhenNeeded(this);
    }


    public static class LineageGridFinder extends WebDriverComponentFinder<LineageGrid, LineageGridFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "lineage-grid-display");

        public LineageGridFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected LineageGrid construct(WebElement el, WebDriver driver)
        {
            return new LineageGrid(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
