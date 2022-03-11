package org.labkey.test.components.ui.search;

import org.labkey.remoteapi.query.Filter.Operator;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.ReactSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

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

    /**
     * Set a valueless filter expression (e.g. 'Not Blank')
     * @param operator filter type
     */
    public void setFilterValue(Operator operator)
    {
        elementCache().filterTypeSelect.select(operator.getDisplayValue());
    }

    /**
     * Set a single value filter expression (e.g. 'Greater Than')
     * @param operator filter type
     * @param value filter value
     */
    public void setFilterValue(Operator operator, String value)
    {
        elementCache().filterTypeSelect.select(operator.getDisplayValue());
        elementCache().filterValue1.set(value);
    }

    /**
     * Set a two-value filter expression (e.g. 'Between')
     * @param operator filter type
     * @param value1 first value
     * @param value2 second value
     */
    public void setFilterValue(Operator operator, String value1, String value2)
    {
        elementCache().filterTypeSelect.select(operator.getDisplayValue());
        elementCache().filterValue1.set(value1);
        elementCache().filterValue2.set(value2);
    }

    public void clearFilter()
    {
        elementCache().filterTypeSelect.clearSelection();
        getWrapper().shortWait().until(ExpectedConditions.invisibilityOfAllElements(
                elementCache().filterValue1.getComponentElement(),
                elementCache().filterValue2.getComponentElement()));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        protected final ReactSelect filterTypeSelect = new ReactSelect.ReactSelectFinder(getDriver()).findWhenNeeded(this);
        protected final Input filterValue1 = Input.Input(Locator.name("field-value-text"), getDriver()).refindWhenNeeded(this);
        protected final Input filterValue2 = Input.Input(Locator.name("field-value-text-second"), getDriver()).refindWhenNeeded(this);;
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
