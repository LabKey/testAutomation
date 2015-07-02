package org.labkey.test.selenium;

import org.labkey.test.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * WebElement wrapper that waits for an attempt to interact with the WebElement before actually finding it
 */
public class LazyWebElement extends WebElementWrapper
{
    protected WebElement _webElement;
    private Locator _locator;
    private SearchContext _searchContext;

    public LazyWebElement(Locator locator, SearchContext searchContext)
    {
        _locator = locator;
        _searchContext = searchContext;
    }

    protected WebElement getElement()
    {
        if (null == _webElement)
            _webElement = getLocator().findElement(getSearchContext());

        return _webElement;
    }

    protected Locator getLocator()
    {
        return _locator;
    }

    protected SearchContext getSearchContext()
    {
        return _searchContext;
    }
}

