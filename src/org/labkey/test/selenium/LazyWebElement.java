/*
 * Copyright (c) 2015 LabKey Corporation
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
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

import java.util.concurrent.TimeUnit;

/**
 * WebElement wrapper that waits for an attempt to interact with the WebElement before actually finding it
 */
public class LazyWebElement extends WebElementWrapper
{
    protected WebElement _wrappedElement;
    private Locator _locator;
    private SearchContext _searchContext;
    private Long _waitMs;

    public LazyWebElement(Locator locator, SearchContext searchContext)
    {
        _locator = locator;
        _searchContext = searchContext;
    }

    public LazyWebElement withTimeout(long ms)
    {
        _waitMs = ms;
        return this;
    }

    private WebElement findWrappedElement()
    {
        if (_waitMs != null && _waitMs > 0)
        {
            FluentWait<SearchContext> wait = new FluentWait<>(getSearchContext());
            wait.withTimeout(_waitMs, TimeUnit.MILLISECONDS);
            return getLocator().waitForElement(wait);
        }
        else
            return getLocator().findElement(getSearchContext());
    }

    @Override
    public WebElement getWrappedElement()
    {
        if (null == _wrappedElement)
            _wrappedElement = findWrappedElement();

        return _wrappedElement;
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

