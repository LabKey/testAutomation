package org.labkey.test.selenium;

import org.labkey.test.Locator;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

public class LazyWaitingWebElement extends LazyWebElement
{
    public LazyWaitingWebElement(Locator locator, SearchContext searchContext)
    {
        super(locator, searchContext);
    }

    @Override
    protected WebElement getElement()
    {
        if (null == _webElement)
            _webElement = getLocator().waitForElement(new FluentWait<>(getSearchContext()));

        return _webElement;
    }
}
