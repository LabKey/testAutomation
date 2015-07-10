package org.labkey.test.selenium;

import org.labkey.test.Locator;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

/**
 * Re-find WebElement with every interaction
 */
public class EphemeralWebElement extends LazyWebElement
{
    public EphemeralWebElement(Locator locator, SearchContext searchContext)
    {
        super(locator, searchContext);
    }

    @Override
    public WebElement getWrappedElement()
    {
        return super.getWrappedElement();
    }
}
