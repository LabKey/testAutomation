package org.labkey.test.components.ui.search;

import org.labkey.remoteapi.query.Filter;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class FilterExpressionPanel extends WebDriverComponent<FilterExpressionPanel.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected FilterExpressionPanel(WebElement element, WebDriver driver)
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

    public void setFilterValue(Filter filter)
    {
        setFilterValue(filter, null, null);
    }

    public void setFilterValue(Filter filter, String value)
    {
        setFilterValue(filter, value, null);
    }

    public void setFilterValue(Filter filter, String value1, String value2)
    {

    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
    }

    public static class FilterExpressionPanelFinder extends WebDriverComponentFinder<FilterExpressionPanel, FilterExpressionPanelFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.byClass("search-filter__input-wrapper").parent();

        public FilterExpressionPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected FilterExpressionPanel construct(WebElement el, WebDriver driver)
        {
            return new FilterExpressionPanel(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
