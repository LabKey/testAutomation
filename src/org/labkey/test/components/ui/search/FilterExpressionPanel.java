package org.labkey.test.components.ui.search;

import org.labkey.remoteapi.query.Filter.Operator;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.react.ReactSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Date;
import java.util.Map;

public class FilterExpressionPanel extends WebDriverComponent<FilterExpressionPanel.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    private final Map<Operator, String> filterTypesLabelOverrides = Map.of(
            Operator.CONTAINS_ONE_OF, "Contains One Of",
            Operator.CONTAINS_NONE_OF, "Does Not Contain Any Of",
            Operator.BETWEEN, "Between",
            Operator.NOT_BETWEEN, "Not Between"
    );

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
        setFilterType(operator);
    }

    /**
     * Set a single value filter expression (e.g. 'Greater Than')
     * @param operator filter type
     * @param value filter value
     */
    public void setFilterValue(Operator operator, String value)
    {
        setFilterType(operator);
        elementCache().filter1Value1.set(value);
    }

    /**
     * Set a single value filter expression (e.g. 'Greater Than')
     * @param operator filter type
     * @param value filter value
     */
    public void setFilterValue(Operator operator, Date value)
    {
        setFilterType(operator);
        elementCache().filter1Date1.set(value.toString());
    }

    /**
     * Set a single value filter expression (e.g. 'Equals')
     * @param operator filter type
     * @param value filter value
     */
    public void setFilterValue(Operator operator, boolean value)
    {
        setFilterType(operator);
        if (value)
        {
            elementCache().filter1BoolTrue.check();
        }
        else
        {
            elementCache().filter1BoolFalse.check();
        }
    }

    /**
     * Set a two-value filter expression (e.g. 'Between')
     * @param operator filter type
     * @param value1 first value
     * @param value2 second value
     */
    public void setFilterValue(Operator operator, String value1, String value2)
    {
        setFilterType(operator);
        elementCache().filter1Value1.set(value1);
        elementCache().filter1Value2.set(value2);
    }

    /**
     * Set a two-value filter expression (e.g. 'Between')
     * @param operator filter type
     * @param value1 first value
     */
    public void setFilterValue(Operator operator, Date value1, Date value2)
    {
        setFilterType(operator);
        elementCache().filter1Date1.set(value1.toString());
        elementCache().filter1Date2.set(value2.toString());
    }

    public void clearFilter()
    {
        if (elementCache().filter2TypeSelect.getComponentElement().isDisplayed())
        {
            elementCache().filter2TypeSelect.clearSelection();
            getWrapper().shortWait().until(ExpectedConditions.invisibilityOfAllElements(
                    elementCache().filter2Value1.getComponentElement(),
                    elementCache().filter2Value2.getComponentElement()));
        }
        elementCache().filter1TypeSelect.clearSelection();
        getWrapper().shortWait().until(ExpectedConditions.invisibilityOfAllElements(
                elementCache().filter1Value1.getComponentElement(),
                elementCache().filter1Value2.getComponentElement()));
    }

    private void setFilterType(Operator operator)
    {
        if (filterTypesLabelOverrides.containsKey(operator))
        {
            elementCache().filter1TypeSelect.select(filterTypesLabelOverrides.get(operator));
        }
        else
        {
            elementCache().filter1TypeSelect.select(operator.getDisplayValue());
        }
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        protected final ReactSelect filter1TypeSelect = new ReactSelect.ReactSelectFinder(getDriver()).index(0).findWhenNeeded(this);
        protected final Input filter1Value1 = Input.Input(Locator.name("field-value-text"), getDriver()).refindWhenNeeded(this);
        protected final Input filter1Value2 = Input.Input(Locator.name("field-value-text-second"), getDriver()).refindWhenNeeded(this);
        protected final Input filter1Date1 = Input.Input(Locator.name("field-value-date-0"), getDriver()).refindWhenNeeded(this);
        protected final Input filter1Date2 = Input.Input(Locator.name("field-value-date-0-second"), getDriver()).refindWhenNeeded(this);
        protected final RadioButton filter1BoolTrue = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("field-value-bool", "true")).refindWhenNeeded(this);
        protected final RadioButton filter1BoolFalse = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("field-value-bool", "false")).refindWhenNeeded(this);

        // TODO: second filter not yet working
        protected final ReactSelect filter2TypeSelect = new ReactSelect.ReactSelectFinder(getDriver()).index(1).findWhenNeeded(this);
        protected final Input filter2Value1 = Input.Input(Locator.name("field-value-text-1"), getDriver()).refindWhenNeeded(this);
        protected final Input filter2Value2 = Input.Input(Locator.name("field-value-text-1-second"), getDriver()).refindWhenNeeded(this);
        protected final Input filter2Date1 = Input.Input(Locator.name("field-value-date-1"), getDriver()).refindWhenNeeded(this);
        protected final Input filter2Date2 = Input.Input(Locator.name("field-value-date-1-second"), getDriver()).refindWhenNeeded(this);
        protected final RadioButton filter2BoolTrue = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("field-value-bool-1", "true")).refindWhenNeeded(this);
        protected final RadioButton filter2BoolFalse = RadioButton.RadioButton(Locator.radioButtonByNameAndValue("field-value-bool-1", "false")).refindWhenNeeded(this);
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
