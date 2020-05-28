package org.labkey.test.components.glassLibrary.grids;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        elementCache().selectCheckbox.set(checked);
        return this;
    }

    public WebElement getCell(int colIndex)
    {
        return Locator.tag("td").index(colIndex).findElement(this);
    }

    public boolean contains(Locator loc)
    {
        return loc.existsIn(this);
    }

    public String getValue(String columnText)
    {
        return getRowMap().get(columnText);
    }

    public List<String> getValues()
    {
        List<String> columnValues = getWrapper().getTexts(Locator.css("td")
                .findElements(getComponentElement()));
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
            List<String> rowCellTexts = getValues();
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
        private Locator _locator;
        private Locator.CssLocator _cssLocator = Locator.css("tbody").child("tr")
                .withoutClass("grid-empty").withoutClass("grid-loading");
        private Locator.XPathLocator _xPathLocator = Locator.tag("tbody").child("tr")
                .withoutClass("grid-empty").withoutClass("grid-loading");
        private ResponsiveGrid _grid;

        public GridRowFinder(ResponsiveGrid grid)
        {
            super(grid.getDriver());
            _grid = grid;
        }

        public GridRowFinder atIndex(int index)
        {
            _locator = _xPathLocator.index(index);
            return this;
        }

        public GridRowFinder withCheckedBox()
        {
            _locator = _cssLocator.append(Locator.css(" input:checked[type=checkbox]"));
            return this;
        }

        public GridRowFinder withDescendant(Locator.XPathLocator descendant)
        {
            _locator = _xPathLocator.withDescendant(descendant);
            return this;
        }

        public GridRowFinder withValue(String value)
        {
            _locator = _xPathLocator.withChild(Locator.tagWithText("td", value));
            return this;
        }

        public GridRowFinder withValueAtColumnIndex(String value, int columnIndex)
        {
            _locator = _xPathLocator.withChild(Locator.tag("td").index(columnIndex).withText(value));
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
