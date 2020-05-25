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

    public boolean containing(Locator loc)
    {
        return loc.existsIn(this);
    }

    public boolean isRowSelected()
    {
        return hasSelectColumn() && elementCache().selectCheckbox.isSelected();
    }

    public WebElement getCell(int colIndex)
    {
        return Locator.tag("td").index(colIndex).findElement(this);
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
}
