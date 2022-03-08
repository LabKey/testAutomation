package org.labkey.test.components.ui.search;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.html.Input.Input;

public class FilterFacetedPanel extends WebDriverComponent<FilterFacetedPanel.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected FilterFacetedPanel(WebElement element, WebDriver driver)
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

    public void selectValue(String value)
    {

    }

    public void checkValues(String... values)
    {

    }

    public void uncheckValues(String... values)
    {

    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        Input filterInput = Input(Locator.id("find-filter-typeahead-input"), getDriver()).findWhenNeeded(this);
        WebElement selectedItemsSection = Locator.byClass("search-filter-tags__div").refindWhenNeeded(this);
    }

    public static class FilterFacetedPanelFinder extends WebDriverComponentFinder<FilterFacetedPanel, FilterFacetedPanelFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.byClass("search-filter-values__panel").parent();

        public FilterFacetedPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected FilterFacetedPanel construct(WebElement el, WebDriver driver)
        {
            return new FilterFacetedPanel(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
