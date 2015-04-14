package org.labkey.test;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.List;

public class CachingLocator extends Locator
{
    private Locator _locator;

    private WebElement cachedWebElement;
    private List<WebElement> cachedWebElements;

    public CachingLocator(Locator locator)
    {
        super(locator.toString());
        _locator = locator;
    }

    private void clearCache()
    {
        cachedWebElements = null;
        cachedWebElement = null;
    }

    @Override
    public Locator containing(String contains)
    {
        clearCache();
        _locator = _locator.containing(contains);
        return this;
    }

    @Override
    public Locator withText(String text)
    {
        clearCache();
        _locator = _locator.withText(text);
        return this;
    }

    @Override
    public Locator index(Integer index)
    {
        clearCache();
        _locator = _locator.index(index);
        return this;
    }

    @Override
    public String toString()
    {
        return _locator.toString();
    }

    @Override
    public By toBy()
    {
        return _locator.toBy();
    }

    @Override
    public List<WebElement> findElements(SearchContext context)
    {
        if (cachedWebElements == null || cachedWebElements.isEmpty())
            cachedWebElements = _locator.findElements(context);

        return cachedWebElements;
    }

    @Override
    public WebElement findElement(SearchContext context)
    {
        if (cachedWebElement == null)
            cachedWebElement = _locator.findElement(context);

        return cachedWebElement;
    }
}
