package org.labkey.test.components.core;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps an oft-used table that uses data-fieldKey attributes to display key/column pairs
 */
public class DetailComponentTable extends WebDriverComponent<DetailComponentTable.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public DetailComponentTable(WebElement element, WebDriver driver)
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

    public Map<String, String> getData()
    {
        Map data = new HashMap();
        List<String> keys = elementCache().fieldKeys();
        for (String key : keys)
            data.put(key, elementCache().getTableValue(key));
        return data;
    }

    public String getTableValue(String fieldKey)
    {
        return elementCache().getTableValue(fieldKey);
    }

    public List<String> fieldKeys()
    {
        return elementCache().fieldKeys();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        public String getTableValue(String fieldKey)
        {
            return Locator.tagWithAttribute("td", "data-fieldkey", fieldKey)
                    .waitForElement(this, 4000).getText();
        }

        public List<String> fieldKeys()
        {
            List<String> fieldKeys = new ArrayList<>();
            List<WebElement> fields = Locator.tag("td").withAttribute("data-fieldkey")
                    .findElements(this);
            fields.forEach(a->fieldKeys.add(a.getAttribute("data-fieldKey")));
            return fieldKeys;
        }
    }


    public static class DetailComponentTableFinder extends WebDriverComponentFinder<DetailComponentTable, DetailComponentTableFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("table", "detail-component--table__fixed");

        public DetailComponentTableFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected DetailComponentTable construct(WebElement el, WebDriver driver)
        {
            return new DetailComponentTable(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
