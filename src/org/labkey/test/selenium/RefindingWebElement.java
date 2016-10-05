package org.labkey.test.selenium;

import org.labkey.test.Locator;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertFalse;

public class RefindingWebElement extends LazyWebElement
{
    private List<Consumer<WebElement>> _listeners = new ArrayList<>();

    public RefindingWebElement(Locator locator, SearchContext searchContext)
    {
        super(locator, searchContext);
    }

    public RefindingWebElement(WebElement element, SearchContext searchContext)
    {
        this(Locator.id(element.getAttribute("id")), searchContext);
        withRefindListener(this::assertUniqueId);
        _wrappedElement = element;
    }

    /**
     * This constructor provides no guarantee that
     */
    public RefindingWebElement(WebElement element, Locator locator, SearchContext searchContext)
    {
        this(locator, searchContext);
        _wrappedElement = element;
    }

    private void assertUniqueId(WebElement el)
    {
        String id = el.getAttribute("id");
        Locator.IdLocator webPartLocator = Locator.id(id);
        assertFalse("Unable to refind element: ambiguous ID " + id + ". Fix product code or refind manually.", webPartLocator.findElements(this).size() > 1);
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
            try
            {
                _wrappedElement = findWrappedElement();
                callListeners(super.getWrappedElement());
            }
            catch (NoSuchElementException ignore) {}
        }
        return super.getWrappedElement();
    }

    public RefindingWebElement withRefindListener(Consumer<WebElement> callback)
    {
        _listeners.add(callback);
        return this;
    }

    private void callListeners(WebElement newElement)
    {
        for (Consumer<WebElement> listener : _listeners)
            listener.accept(newElement);
    }
}
