package org.labkey.test.components.html;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.labkey.FormItemFinder;
import org.openqa.selenium.WebElement;

public class Checkbox extends Component implements FormItem<Boolean>
{
    private WebElement _el;

    public Checkbox(WebElement element)
    {
        _el = element;
    }

    public static SimpleComponentFinder<Checkbox> Checkbox(Locator loc)
    {
        return new SimpleComponentFinder<Checkbox>(loc)
        {
            @Override
            protected Checkbox construct(WebElement el)
            {
                return new Checkbox(el);
            }
        };
    }

    public static FormItemFinder<Checkbox> Checkbox()
    {
        return new FormItemFinder<Checkbox>()
        {
            @Override
            protected Checkbox construct(WebElement el)
            {
                return new Checkbox(el);
            }

            @Override
            protected String itemTag()
            {
                return "input";
            }
        };
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    public boolean isEnabled()
    {
        return getComponentElement().isEnabled();
    }

    public boolean isSelected()
    {
        return isChecked();
    }

    public boolean isDisplayed()
    {
        return getComponentElement().isDisplayed();
    }

    public boolean isChecked()
    {
        return getComponentElement().isSelected();
    }

    public void check()
    {
        set(true);
    }

    public void uncheck()
    {
        set(false);
    }

    public void set(@NotNull Boolean checked)
    {
        if (checked != isChecked())
            toggle();
    }

    public void toggle()
    {
        _el.click();
    }

    @Override
    public Boolean get()
    {
        return isSelected();
    }

    protected void assertElementType()
    {
        String type = _el.getCssValue("type");
        Assert.assertEquals("Not a checkbox: " + _el.toString(), "checkbox", type);
    }
}
