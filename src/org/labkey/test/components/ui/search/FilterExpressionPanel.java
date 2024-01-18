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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class FilterExpressionPanel extends WebDriverComponent<FilterExpressionPanel.ElementCache>
{
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd H:m");

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
     * Set the first filter expression
     */
    public void setFilter(Expression expression)
    {
        setFilter(0, expression._operator, expression._value1, expression._value2);
    }

    /**
     * Set both filter expressions
     */
    public void setFilters(Expression expression0, Expression expression1)
    {
        setFilter(0, expression0._operator, expression0._value1, expression0._value2);
        setFilter(1, expression1._operator, expression1._value1, expression1._value2);
    }

    public void clearFilter()
    {
        if (elementCache().filterTypeSelects.get(1).getComponentElement().isDisplayed())
        {
            elementCache().filterTypeSelects.get(1).clearSelection();
        }
        elementCache().filterTypeSelects.get(0).clearSelection();
    }

    public void setFilter(int index, Object operator, Object value1, Object value2)
    {
        setFilterType(index, operator);
        if (value1 != null)
        {
            if (value1 instanceof Boolean boolVal)
            {
                if (boolVal)
                {
                    elementCache().boolTrueRadios.get(index).check();
                }
                else
                {
                    elementCache().boolFalseRadios.get(index).check();
                }
            }
            else if (value1 instanceof Date dateVal1)
            {
                final String dateStr1 = DATE_FORMAT.format(dateVal1);
                elementCache().dateValues.get(index).set(dateStr1);
                if (value2 instanceof Date dateVal2)
                {
                    final String dateStr2 = DATE_FORMAT.format(dateVal2);
                    elementCache().dateValuesSecond.get(index).set(dateStr2);
                }
                else if (value2 != null)
                {
                    throw new IllegalArgumentException("Mismatched expression value types: " +
                            value1.getClass().getSimpleName() + "=/=" +
                            value2.getClass().getSimpleName());
                }
            }
            else
            {
                elementCache().textValuesFirst.get(index).set(value1.toString());
                if (value2 != null)
                {
                    elementCache().textValuesSecond.get(index).set(value2.toString());
                }
            }
        }
    }

    private void setFilterType(int index, Object op)
    {
        if (op instanceof Operator operator)
        {
            if (filterTypesLabelOverrides.containsKey(operator))
            {
                elementCache().filterTypeSelects.get(index).select(filterTypesLabelOverrides.get(operator));
            }
            else
            {
                elementCache().filterTypeSelects.get(index).select(operator.getDisplayValue());
            }
        }
        else
            elementCache().filterTypeSelects.get(index).select(op.toString());

    }

    public boolean hasFilterType(int index, String filterCaption)
    {
        return elementCache().filterTypeSelects.get(index).getOptions().contains(filterCaption);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        protected final List<ReactSelect> filterTypeSelects = List.of(
                new ReactSelect.ReactSelectFinder(getDriver()).index(0).findWhenNeeded(this),
                new ReactSelect.ReactSelectFinder(getDriver()).index(1).refindWhenNeeded(this));
        protected final List<Input> textValuesFirst = List.of(
                Input.Input(Locator.name("field-value-text-0"), getDriver()).refindWhenNeeded(this),
                Input.Input(Locator.name("field-value-text-1"), getDriver()).refindWhenNeeded(this));
        protected final List<Input> textValuesSecond = List.of(
                Input.Input(Locator.name("field-value-text-0-second"), getDriver()).refindWhenNeeded(this),
                Input.Input(Locator.name("field-value-text-1-second"), getDriver()).refindWhenNeeded(this));
        protected final List<Input> dateValues = List.of(
                Input.Input(Locator.name("field-value-date-0"), getDriver()).refindWhenNeeded(this),
                Input.Input(Locator.name("field-value-date-1"), getDriver()).refindWhenNeeded(this));
        protected final List<Input> dateValuesSecond = List.of(
                Input.Input(Locator.name("field-value-date-0-second"), getDriver()).refindWhenNeeded(this),
                Input.Input(Locator.name("field-value-date-1-second"), getDriver()).refindWhenNeeded(this));
        protected final List<RadioButton> boolTrueRadios = List.of(
                RadioButton.RadioButton(Locator.radioButtonByNameAndValue("field-value-bool-0", "true")).refindWhenNeeded(this),
                RadioButton.RadioButton(Locator.radioButtonByNameAndValue("field-value-bool-1", "true")).refindWhenNeeded(this));
        protected final List<RadioButton> boolFalseRadios = List.of(
                RadioButton.RadioButton(Locator.radioButtonByNameAndValue("field-value-bool-0", "false")).refindWhenNeeded(this),
                RadioButton.RadioButton(Locator.radioButtonByNameAndValue("field-value-bool-1", "false")).refindWhenNeeded(this));
    }

    public static class FilterExpressionPanelFinder extends WebDriverComponentFinder<FilterExpressionPanel, FilterExpressionPanelFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.byClass("filter-expression__input-wrapper").parent();

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

    public static class Expression
    {
        private final Operator _operator;
        private final Object _value1;
        private final Object _value2;

        /**
         * Set a two-value filter expression (e.g. 'Between')
         * @param operator filter type
         * @param value1 first value
         * @param value2 second value
         */
        public Expression(Operator operator, Object value1, Object value2)
        {
            _operator = operator;
            _value1 = value1;
            _value2 = value2;
        }

        /**
         * Set a single value filter expression (e.g. 'Greater Than')
         * @param operator filter type
         * @param value filter value
         */
        public Expression(Operator operator, Object value)
        {
            this(operator, value, null);
        }

        /**
         * Set a valueless filter expression (e.g. 'Not Blank')
         * @param operator filter type
         */
        public Expression(Operator operator)
        {
            this(operator, null, null);
        }
    }
}
