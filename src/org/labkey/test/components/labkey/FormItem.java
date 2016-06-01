package org.labkey.test.components.labkey;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.components.ComponentElements;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.Locator.xq;

/**
 * For wrapping labkey form items
 * This works for items such as those defined in org.labkey.api.jsp.taglib
 * It should also work by convention for most labkey insert/update forms
 * <tr>
 *     <td class='labkey-form-label'/>
 *     <td>item</td>
 * </tr>
 */
public abstract class FormItem<T> extends WebDriverComponent
{
    private WebElement _rowEl;
    private WebDriverWrapper _driverWrapper;
    private Elements _elements;

    protected FormItem(WebElement rowEl, WebDriver driver)
    {
        _rowEl = rowEl;
        _driverWrapper = new WebDriverWrapperImpl(driver);
    }

    public static FormItemFinder<FormItem, ?> FormItem(WebDriver driver)
    {
        return new FormItemFinder<>(driver);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _rowEl;
    }

    protected WebDriverWrapper getDriverWrapper()
    {
        return _driverWrapper;
    }

    protected WebDriver getDriver()
    {
        return getDriverWrapper().getDriver();
    }

    public abstract T getValue();
    public abstract void setValue(T value);

    public String getLabel()
    {
        return elements().label.getText();
    }

    /**
     * @return Locator for wrapped form item (e.g. input or select)
     */
    protected Locator itemLoc()
    {
        return Locator.tag("*");
    }

    protected Elements elements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
    }

    public class Elements extends ComponentElements
    {
        @Override
        protected SearchContext getContext()
        {
            return getComponentElement();
        }

        protected WebElement label = Locator.tag("td").withClass("labkey-form-label").findWhenNeeded(this);
        protected WebElement itemTd = Locator.tag("td").withoutClass("labkey-form-label").findWhenNeeded(this);
        protected WebElement item = itemLoc().findWhenNeeded(itemTd);
    }

    public static class FormItemFinder<C extends FormItem, F extends FormItemFinder<C, F>> extends WebDriverComponentFinder<C, F>
    {
        private String name = null;
        private String labelText = "";
        private boolean partialText = true;

        protected FormItemFinder(WebDriver driver)
        {
            super(driver);
        }

        protected String getLabelText()
        {
            return labelText;
        }

        public F withName(@NotNull String name)
        {
            this.name = name;
            return (F)this;
        }

        public F withLabel(@NotNull String text)
        {
            this.labelText = text;
            partialText = false;
            return (F)this;
        }

        public F withLabelContaining(@NotNull String text)
        {
            this.labelText = text;
            partialText = true;
            return (F)this;
        }

        @Override
        protected C construct(WebElement el, WebDriver driver)
        {
            return (C)new GenericFormItem(el, driver);
        }

        @Override
        protected Locator locator()
        {
            Locator.XPathLocator loc = Locator.tag("tr").withChild(labelLoc());
            if (name != null && !name.isEmpty())
                loc = loc.withChild(Locator.tag("td").child(Locator.tagWithName("*", name)));
            return loc;
        }

        protected Locator.XPathLocator labelLoc()
        {
            Locator.XPathLocator loc = Locator.tagWithClass("td", "labkey-form-label");
            if (partialText) // Don't match nested elements (e.g. '*' for required fields)
            {
                if (labelText.isEmpty())
                    return loc;
                return loc.withPredicate("contains(text(), " + xq(labelText) + ")");
            }
            else
                return loc.withPredicate("text() = " + xq(labelText));
        }
    }
}
