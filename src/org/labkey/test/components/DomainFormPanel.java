package org.labkey.test.components;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DomainFormPanel extends WebDriverComponent<DomainFormPanel.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;
    private Map<String, DomainFieldRow> _fieldRows = null;

    public DomainFormPanel(WebElement element, WebDriver driver)
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

    public DomainFieldRow addField(String name)
    {
        int nextIndex = -1;
        if (fieldRows().keySet().size() > 0)
        {
            DomainFieldRow lastRow = fieldRows().values().stream().max(Comparator.comparingInt(DomainFieldRow::getIndex)).get();
            nextIndex = lastRow.getIndex()+1;
        }

        elementCache().addFieldSpan.click();
        _fieldRows = null;  // force cache refresh here

        // if there were 0 rows, use the row with empty name- else find it by nextIndex
        DomainFieldRow newFieldRow = (nextIndex == -1) ? getField("") : getField(nextIndex);

        newFieldRow.setName(name);
        return newFieldRow;
    }

    public void removeField(String name)
    {
        getField(name)
                .clickRemoveField();
        _fieldRows = null;          // invalidate the cache
    }

    public DomainFieldRow getField(String name)
    {
        if (fieldRows().containsKey(name))
            return _fieldRows.get(name);
        else return null;
    }

    public DomainFieldRow getField(int tabIndex)
    {
        return fieldRows().values().stream().filter(a-> a.getIndex()==tabIndex).findFirst().orElse(null);
    }

    public Map<String, DomainFieldRow> fieldRows()
    {
        if (_fieldRows == null)
        {
            _fieldRows = new HashMap<>();
            List<DomainFieldRow> rows = new DomainFieldRow.DomainFieldRowFinder(getDriver()).findAll(this);
            for (DomainFieldRow row : rows)
            {
                _fieldRows.put(row.getName(), row);
            }
        }
        return _fieldRows;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebDriverComponent.ElementCache
    {
        public WebElement addFieldSpan = Locator.tagWithClass("span", "domain-form-add")
                .findWhenNeeded(this);
    }

    public static class DomainFormPanelFinder extends WebDriverComponentFinder<DomainFormPanel, DomainFormPanelFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "domain-form-panel");
        private String _title = null;

        public DomainFormPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        public DomainFormPanelFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected DomainFormPanel construct(WebElement el, WebDriver driver)
        {
            return new DomainFormPanel(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withDescendant(Locator.tagWithClass("div", "panel-title").withText( _title));
            else
                return _baseLocator;
        }
    }
}
