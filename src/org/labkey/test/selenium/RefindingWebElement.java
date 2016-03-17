package org.labkey.test.selenium;

import org.labkey.test.Locator;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RefindingWebElement extends LazyWebElement
{
    List<Consumer<WebElement>> _listeners = new ArrayList<>();

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
            callListeners(super.getWrappedElement());
        }
        return super.getWrappedElement();
    }

    public void addRefindListener(Consumer<WebElement> listener)
    {
        _listeners.add(listener);
    }

    private void callListeners(WebElement newElement)
    {
        for (Consumer<WebElement> listener : _listeners)
            listener.accept(newElement);
    }
}
