package org.labkey.test.components.html;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.openqa.selenium.WebElement;

public class OptionSelect<T extends OptionSelect.SelectOption> extends SelectWrapper implements FormItem<String>
{
    public OptionSelect(WebElement element)
    {
        super(element);
    }

    public static <O extends SelectOption> Component.SimpleComponentFinder<OptionSelect> OptionSelect(Locator loc)
    {
        return new Component.SimpleComponentFinder<OptionSelect>(loc)
        {
            @Override
            protected OptionSelect<O> construct(WebElement el)
            {
                return new OptionSelect<>(el);
            }
        };
    }

    public SelectOption getSelection()
    {
        return new SelectOption()
        {
            @Override
            public String getText()
            {
                return getFirstSelectedOption().getText();
            }

            @Override
            public String getValue()
            {
                return getFirstSelectedOption().getAttribute("value");
            }
        };
    }

    public void selectOption(T option)
    {
        if (null != option.getValue())
            selectByValue(option.getValue());
        else
            selectByVisibleText(option.getText());
    }

    @Override
    public String get()
    {
        return getSelection().getText();
    }

    @Override
    public void set(String text)
    {
        selectByVisibleText(text);
    }

    public interface SelectOption
    {
        String getValue();
        String getText();

        static SelectOption option(String value, String text)
        {
            return new SelectOption()
            {
                @Override
                public String getValue()
                {
                    return value;
                }

                @Override
                public String getText()
                {
                    return text;
                }
            };
        }

        static SelectOption textOption(String text)
        {
            return option(null, text);
        }

        static SelectOption valueOption(String value)
        {
            return option(value, null);
        }
    }
}
