/*
 * Copyright (c) 2015-2019 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;

/**
 * WebElement wrapper that waits for an attempt to interact with the WebElement before actually finding it
 */
public class LazyWebElement<T extends LazyWebElement<T>> extends BaseLazyWebElement
{
    private final Locator _locator;
    private final SearchContext _searchContext;
    private Long _waitMs;

    public LazyWebElement(@NotNull Locator locator, @NotNull SearchContext searchContext)
    {
        _locator = locator;
        _searchContext = searchContext;
    }

    @Override
    public boolean isDisplayed()
    {
        try
        {
            return super.isDisplayed();
        }
        catch (StaleElementReferenceException | NoSuchElementException ex)
        {
            return false;
        }
    }

    public final T withTimeout(long ms)
    {
        _waitMs = ms;
        return (T)this;
    }

    @Override
    protected WebElement findWrappedElement()
    {
        if (_waitMs != null && _waitMs > 0)
        {
            FluentWait<SearchContext> wait = new FluentWait<>(getSearchContext());
            wait.withTimeout(Duration.ofMillis(_waitMs));
            return getLocator().waitForElement(wait);
        }
        else
            return getLocator().findElement(getSearchContext());
    }

    protected Locator getLocator()
    {
        return _locator;
    }

    protected SearchContext getSearchContext()
    {
        return _searchContext;
    }

    @Override
    protected String getUnfoundToString()
    {
        return getSearchContext().toString() + " -> " + getClass().getSimpleName() + "{" + getLocator().toString() + "}";
    }
}

