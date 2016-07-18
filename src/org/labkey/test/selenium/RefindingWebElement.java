package org.labkey.test.selenium;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        String id = element.getAttribute("id");
        if (id == null || id.isEmpty())
            throw new IllegalArgumentException("Unable to refind element. Id is empty/null.");
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
            _wrappedElement = null;
            callListeners(super.getWrappedElement());
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
