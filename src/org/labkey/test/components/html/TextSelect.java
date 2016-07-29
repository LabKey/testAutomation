package org.labkey.test.components.html;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.openqa.selenium.WebElement;

public class TextSelect extends SelectWrapper implements FormItem<String>
{
    public TextSelect(WebElement element)
    {
        super(element);
    }

    public static Component.SimpleComponentFinder<TextSelect> TextSelect(Locator loc)
    {
        return new Component.SimpleComponentFinder<TextSelect>(loc)
        {
            @Override
            protected TextSelect construct(WebElement el)
            {
                return new TextSelect(el);
            }
        };
    }

    @Override
    public String get()
    {
        return getFirstSelectedOption().getText();
    }

    @Override
    public void set(String value)
    {
        selectByVisibleText(value);
    }
}
