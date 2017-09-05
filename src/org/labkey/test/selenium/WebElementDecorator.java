package org.labkey.test.selenium;

import org.openqa.selenium.WebElement;

public abstract class WebElementDecorator extends WebElementWrapper
{
    private final WebElement _decoratedElement;

    protected WebElementDecorator(WebElement decoratedElement)
    {
        _decoratedElement = decoratedElement;
    }

    @Override
    public final WebElement getWrappedElement()
    {
        return _decoratedElement;
    }

    @Override
    public String toString()
    {
        return getWrappedElement().toString();
    }
}
