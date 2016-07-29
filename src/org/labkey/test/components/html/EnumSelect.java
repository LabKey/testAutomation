package org.labkey.test.components.html;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.openqa.selenium.WebElement;

public class EnumSelect<E extends Enum<E>> extends SelectWrapper implements FormItem<E>
{
    Class<E> _clazz;

    public EnumSelect(WebElement element, Class<E> clazz)
    {
        super(element);
        _clazz = clazz;
    }

    public static <E extends Enum<E>> Component.SimpleComponentFinder<EnumSelect<E>> EnumSelect(Locator loc, Class<E> clazz)
    {
        return new Component.SimpleComponentFinder<EnumSelect<E>>(loc)
        {
            @Override
            protected EnumSelect<E> construct(WebElement el)
            {
                return new EnumSelect<>(el, clazz);
            }
        };
    }

    @Override
    public E get()
    {
        return Enum.valueOf(_clazz, getFirstSelectedOption().getAttribute("value"));
    }

    @Override
    public void set(E value)
    {
        String valText;
        if (value instanceof OptionSelect.SelectOption)
            valText = ((OptionSelect.SelectOption)value).getValue();
        else
            valText = value.name();
        selectByValue(valText);
    }
}
