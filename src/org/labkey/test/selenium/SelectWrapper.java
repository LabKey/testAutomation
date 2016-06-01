package org.labkey.test.selenium;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

public abstract class SelectWrapper extends Select
{
    public SelectWrapper()
    {
        super(new RemoteWebElement(){
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
    }

    protected abstract Select getWrappedSelect();

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
}
