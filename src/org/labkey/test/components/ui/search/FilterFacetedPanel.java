package org.labkey.test.components.ui.search;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.ui.FilterStatusValue;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Select a single facet value by clicking its label. Should replace all existing selections.
     * @param value desired value
     */
    public void selectValue(String value)
    {
        elementCache().findCheckboxLabel(value).click();
    }

    /**
     * Check single facet value by label to see if it is checked or not.
     * @param value desired value
     */
    public boolean isChecked(String value)
    {
        return elementCache().findCheckbox(value).isChecked();
    }

    /**
     * Check all the specified options. Should retain any existing selections.
     * @param values values to select
     */
    public void checkValues(String... values)
    {
        for (String value : values)
        {
            elementCache().findCheckbox(value).check();
        }
    }

    /**
     * Uncheck all the specified options. Other existing selections should be unchanged.
     * @param values values to deselect
     */
    public void uncheckValues(String... values)
    {
        for (String value : values)
        {
            elementCache().findCheckbox(value).uncheck();
        }
    }

    public List<String> getAvailableValues()
    {
        return elementCache().getAvailableValues();
    }

    public List<String> getSelectedValues()
    {
        return elementCache().getSelectedValues();
    }

    public FilterFacetedPanel filterValues(String filterStr)
    {
        elementCache().filterInput.set(filterStr);
        getWrapper().shortWait().until(ExpectedConditions.visibilityOf(elementCache().checkboxSection));
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        protected final Input filterInput =
                Input(Locator.id("filter-faceted__typeahead-input"), getDriver()).findWhenNeeded(this);
        protected final WebElement checkboxSection =
                Locator.byClass("labkey-wizard-pills").index(0).waitForElement(this, 5_000);
        protected final Locator.XPathLocator checkboxLabelLoc
                = Locator.byClass("filter-faceted__value");

        protected List<String> getAvailableValues()
        {
            return getWrapper().getTexts(checkboxLabelLoc.findElements(checkboxSection));
        }

        protected Checkbox findCheckbox(String value)
        {
            return Checkbox.Checkbox(checkboxLabelLoc.withText(value)
                    .precedingSibling("input")).find(checkboxSection);
        }

        protected WebElement findCheckboxLabel(String value)
        {
            return checkboxLabelLoc.withText(value).findElement(checkboxSection);
        }

        protected final WebElement selectedItemsSection =
                Locator.byClass("filter-faceted__tags-div").findWhenNeeded(this);

        protected List<String> getSelectedValues()
        {
            return new FilterStatusValue.FilterStatusValueFinder(getDriver()).findAll(selectedItemsSection)
                    .stream().map(FilterStatusValue::getText).collect(Collectors.toList());
        }
    }

    public static class FilterFacetedPanelFinder extends WebDriverComponentFinder<FilterFacetedPanel, FilterFacetedPanelFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.byClass("filter-faceted__panel").parent();

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
