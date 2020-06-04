package org.labkey.test.components.glassLibrary.grids;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.TestLogger.log;

public class GridRow extends WebDriverComponent<GridRow.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;
    final ResponsiveGrid _grid;
    private Map<String, String> _rowMap = null;

    public GridRow(ResponsiveGrid grid, WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
        _grid = grid;
    }

    public boolean hasSelectColumn()
    {
        return elementCache().selectColumn.isPresent();
    }

    public boolean isSelected()
    {
        return hasSelectColumn() && elementCache().selectCheckbox.isSelected();
    }

    public GridRow select(boolean checked)
    {
        assertTrue("The row does not have a select box", hasSelectColumn());
        elementCache().selectCheckbox.set(checked);
        return this;
    }

    private WebElement getCell(int colIndex)
    {
        return Locator.tag("td").index(colIndex).findElement(this);
    }

    public WebElement getCell(String columnHeader)
    {
        return getCell(_grid.getColumnIndex(columnHeader));
    }

    public boolean contains(Locator loc)
    {
        return loc.existsIn(this);
    }

    public void clickLink(String text)
    {
        log("seeking link with text [" + text + "]");
        WebElement link = Locator.linkWithText(text).findElement(getComponentElement());
        log("found element with text [" + link.getText() + "]");
        String href = link.getAttribute("href");
        link.click();
        log("waiting for url to be: [" + href + "]");
        WebDriverWrapper.waitFor(()-> getWrapper().getURL().toString().endsWith(href) &&
                getWrapper().shortWait().until(ExpectedConditions.stalenessOf(link)), 1000);
    }

    /**
     * Returns the text in the row for the specified column
     * @param columnText
     * @return
     */
    public String getText(String columnText)
    {
        return getRowMap().get(columnText);
    }

    /**
     * Returns a list of the row values as text
     * @return
     */
    public List<String> getTexts()
    {
        List<String> columnValues = getWrapper().getTexts(Locator.css("td")
                .findElements(this));
        if (hasSelectColumn())
            columnValues.remove(0);
        return columnValues;
    }

    public Map<String, String> getRowMap()
    {
        if (_rowMap == null)
        {
            _rowMap = new HashMap<>();
            List<String> columns = _grid.getColumnNames();
            List<String> rowCellTexts = getTexts();
            for (int i = 0; i < columns.size(); i++)
            {
                _rowMap.put(columns.get(i), rowCellTexts.get(i));
            }
        }
        return _rowMap;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }   // componentElement is the /tr under tbody

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
        public Optional<WebElement> selectColumn = Locator.xpath("//td/input[@type='checkbox']")
                .findOptionalElement(getComponentElement());
        public Checkbox selectCheckbox = new Checkbox(Locator.tagWithAttribute("input", "type", "checkbox")
            .findWhenNeeded(this));
        public WebElement column = Locator.tag("tr").findWhenNeeded(getComponentElement());
    }

    public static class GridRowFinder extends WebDriverComponentFinder<GridRow, GridRowFinder>
    {
        private Locator.XPathLocator _locator = Locator.tag("tbody").child("tr").withoutClass("grid-empty").withoutClass("grid-loading");
        private ResponsiveGrid _grid;

        public GridRowFinder(ResponsiveGrid grid)
        {
            super(grid.getDriver());
            _grid = grid;
        }

        protected GridRowFinder atIndex(int index)
        {
            _locator = _locator.index(index);
            return this;
        }

        /**
         * Matches rows with a descendant described by the supplied locator
         * @param descendant
         * @return
         */
        public GridRowFinder withDescendant(Locator.XPathLocator descendant)
        {
            _locator = _locator.withDescendant(descendant);
            return this;
        }

        /**
         * Matches rows with a cell matching the full text supplied
         * @param text
         * @return
         */
        public GridRowFinder withCellWithText(String text)
        {
            _locator = _locator.withChild(Locator.tagWithText("td", text));
            return this;
        }

        /**
         * Returns the first row with matching text in the specified column
         * @param value
         * @param columnIndex
         * @return
         */
        protected GridRowFinder withTextAtColumn(String value, int columnIndex)
        {
            _locator = _locator.withChild(Locator.tag("td").index(columnIndex).withText(value));
            return this;
        }

        @Override
        protected GridRowFinder getThis()
        {
            return this;
        }

        @Override
        protected GridRow construct(WebElement el, WebDriver driver)
        {
            return new GridRow(_grid, el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
