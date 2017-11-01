/*
 * Copyright (c) 2015-2017 LabKey Corporation
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
package org.labkey.test;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.List;

public class CachingLocator extends Locator
{
    private Locator _locator;

    private List<WebElement> cachedWebElements;
    private SearchContext cachedContext;

    public CachingLocator(Locator locator)
    {
        super(locator.toString());
        _locator = locator;
    }

    private void clearCache()
    {
        cachedWebElements = null;
    }

    @Override
    public Locator containing(String contains)
    {
        clearCache();
        _locator = _locator.containing(contains);
        return this;
    }

    @Override
    public Locator withText(String text)
    {
        clearCache();
        _locator = _locator.withText(text);
        return this;
    }

    @Override
    public Locator index(Integer index)
    {
        clearCache();
        _locator = _locator.index(index);
        return this;
    }

    @Override
    public String toString()
    {
        return _locator.toString();
    }

    @Override
    protected By getBy()
    {
        return _locator.getBy();
    }

    @Override
    public List<WebElement> findElements(SearchContext context)
    {
        if (cachedWebElements == null || cachedWebElements.isEmpty() || context != cachedContext)
        {
            cachedWebElements = _locator.findElements(context);
            cachedContext = context;
        }

        return cachedWebElements;
    }

    @Override
    public WebElement findElement(SearchContext context)
    {
        return findElements(context).get(0);
    }
}
