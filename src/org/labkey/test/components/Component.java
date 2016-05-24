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
package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.List;

public abstract class Component implements SearchContext
{
    public abstract WebElement getComponentElement();

    public WebElement findElement(Locator locator)
    {
        return locator.findElement(this);
    }

    public List<WebElement> findElements(Locator locator)
    {
        return locator.findElements(this);
    }

    @Override
    public WebElement findElement(By by)
    {
        return getComponentElement().findElement(by);
    }

    @Override
    public List<WebElement> findElements(By by)
    {
        return getComponentElement().findElements(by);
    }

    protected static abstract class ComponentFinder<S extends SearchContext, C extends Component, F extends ComponentFinder<S, C, F>>
    {
        private static final int DEFAULT_TIMEOUT = 10000;
        private int timeout = 0;
        private int index = 0;

        public F timeout(int timeout)
        {
            this.timeout = timeout;
            return (F)this;
        }

        public F index(int index)
        {
            this.index = index;
            return (F)this;
        }

        protected abstract Locator locator();
        protected abstract C construct(WebElement el);

        protected final Locator buildLocator()
        {
            return locator().index(index);
        }

        private final WebElement findElement(S context)
        {
            return buildLocator().findElement(context);
        }

        private final WebElement waitForElement(S context)
        {
            return buildLocator().waitForElement(context, timeout > 0 ? timeout : DEFAULT_TIMEOUT);
        }

        public C find(S context)
        {
            return construct(findElement(context));
        }

        public C waitFor(S context)
        {
            return construct(waitForElement(context));
        }

        public C findWhenNeeded(S context)
        {
            return construct(new LazyWebElement(buildLocator(), context).withTimeout(timeout));
        }
    }
}
