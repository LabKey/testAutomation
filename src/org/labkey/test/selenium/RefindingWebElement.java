package org.labkey.test.selenium;

import org.labkey.test.Locator;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

public class RefindingWebElement extends LazyWebElement
{
    public RefindingWebElement(Locator locator, SearchContext searchContext)
    {
        super(locator, searchContext);
    }

    @Override
    public WebElement getWrappedElement()
    {
        try
        {
            super.getWrappedElement().isEnabled(); // Check for staleness
        }
        catch (StaleElementReferenceException refind)
        {
            _wrappedElement = null;
        }
        return super.getWrappedElement();
    }
}
