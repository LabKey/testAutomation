package org.labkey.test.selenium;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.List;

public class Select<T extends Select.SelectOption> extends org.openqa.selenium.support.ui.Select
{
    private final WebElement wrappedElement;
    private org.openqa.selenium.support.ui.Select wrappedSelect;

    private Select(WebElement element)
    {
        // Feed dummy Element to Selenium Select to prevent UnexpectedTagNameException
        super(new RemoteWebElement()
        {
            @Override
            public String getTagName()
            {
                return "select";
            }

            @Override
            public String getAttribute(String name)
            {
                return null;
            }
        });
        wrappedElement = element;
    }

    public static <O extends SelectOption> Component.SimpleComponentFinder<Select> Select(Locator loc)
    {
        return new Component.SimpleComponentFinder<Select>(loc)
        {
            @Override
            protected Select<O> construct(WebElement el)
            {
                return new Select<>(el);
            }
        };
    }

    protected org.openqa.selenium.support.ui.Select getWrappedSelect()
    {
        if (null == wrappedSelect)
            wrappedSelect = new org.openqa.selenium.support.ui.Select(wrappedElement);
        return wrappedSelect;
    }

    @Override
    public boolean isMultiple()
    {
        return getWrappedSelect().isMultiple();
    }

    @Override
    public List<WebElement> getOptions()
    {
        return getWrappedSelect().getOptions();
    }

    @Override
    public List<WebElement> getAllSelectedOptions()
    {
        return getWrappedSelect().getAllSelectedOptions();
    }

    @Override
    public WebElement getFirstSelectedOption()
    {
        return getWrappedSelect().getFirstSelectedOption();
    }

    @Override
    public void selectByVisibleText(String text)
    {
        getWrappedSelect().selectByVisibleText(text);
    }

    @Override
    public void selectByIndex(int index)
    {
        getWrappedSelect().selectByIndex(index);
    }

    @Override
    public void selectByValue(String value)
    {
        getWrappedSelect().selectByValue(value);
    }

    @Override
    public void deselectAll()
    {
        getWrappedSelect().deselectAll();
    }

    @Override
    public void deselectByValue(String value)
    {
        getWrappedSelect().deselectByValue(value);
    }

    @Override
    public void deselectByIndex(int index)
    {
        getWrappedSelect().deselectByIndex(index);
    }

    @Override
    public void deselectByVisibleText(String text)
    {
        getWrappedSelect().deselectByVisibleText(text);
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
