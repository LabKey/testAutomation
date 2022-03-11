package org.labkey.test.components.ui.search;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

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

    public List<String> getAvailableValues()
    {
        return null;
    }

    public List<String> getSelectedValues()
    {
        return null;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        Input filterInput = Input(Locator.id("find-filter-typeahead-input"), getDriver()).findWhenNeeded(this);
        WebElement checkboxSection = Locator.byClass("labkey-wizard-pills").index(0).findWhenNeeded(this);
        WebElement selectedItemsSection = Locator.byClass("search-filter-tags__div").findWhenNeeded(this);

        Checkbox findCheckbox(String value)
        {
            return Checkbox.Checkbox(Locator.byClass("search-filter-values__value").withText(value)
                    .precedingSibling("input")).find(checkboxSection);
        }

        WebElement findCheckboxLabel(String value)
        {
            return Locator.byClass("search-filter-values__value").withText(value).findElement(checkboxSection);
        }
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
