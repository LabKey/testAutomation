package org.labkey.test.selenium;

import org.openqa.selenium.WebElement;

import java.util.function.Supplier;

public class CachingWebElement extends BaseLazyWebElement
{
    private final Supplier<WebElement> _elementFinder;

    public CachingWebElement(Supplier<WebElement> elementFinder)
    {
        _elementFinder = elementFinder;
    }

    @Override
    protected WebElement findWrappedElement()
    {
        return _elementFinder.get();
    }
}
