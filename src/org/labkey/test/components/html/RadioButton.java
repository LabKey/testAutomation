package org.labkey.test.components.html;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.openqa.selenium.WebElement;

public class RadioButton extends Checkbox
{
    public RadioButton(WebElement element)
    {
        super(element);
    }

    public static SimpleComponentFinder<RadioButton> RadioButton(Locator loc)
    {
        return new SimpleComponentFinder<RadioButton>(loc)
        {
            @Override
            protected RadioButton construct(WebElement el)
            {
                return new RadioButton(el);
            }
        };
    }

    protected void assertElementType()
    {
        String type = getComponentElement().getCssValue("type");
        Assert.assertEquals("Not a checkbox: " + getComponentElement().toString(), "checkbox", type);
    }
}
