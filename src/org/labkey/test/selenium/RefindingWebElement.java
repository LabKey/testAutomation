/*
 * Copyright (c) 2016-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.selenium;

import org.labkey.test.Locator;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertFalse;

public class RefindingWebElement extends LazyWebElement<RefindingWebElement>
{
    private final List<Consumer<WebElement>> _listeners = new ArrayList<>();

    public RefindingWebElement(Locator locator, SearchContext searchContext)
    {
        super(locator, searchContext);
    }

    public RefindingWebElement(WebElement element, SearchContext searchContext)
    {
        this(Locator.id(element.getAttribute("id")), searchContext);
        withRefindListener(this::assertUniqueId);
        if (element instanceof RefindingWebElement)
        {
            throw new IllegalArgumentException("Nesting RefindingWebElements is not supported");
        }
        else
        {
            _wrappedElement = element;
        }
    }

    /**
     * Refinding reliability depends on the specificity of the provided Locator
     * There is no verification that the provided WebElement matches the provided Locator and SearchContext
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
            boolean refound = false;
            try
            {
                _wrappedElement = findWrappedElement();
                refound = true;
            }
            catch (NoSuchElementException ignore) {}

            if (refound)
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
        try
        {
            for (Consumer<WebElement> listener : _listeners)
                listener.accept(newElement);
        }
        catch (WebDriverException t)
        {
            throw new RuntimeException("Error after element refind", t);
        }
    }
}
