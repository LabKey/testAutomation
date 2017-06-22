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
package org.labkey.test.components;

import org.apache.commons.lang3.NotImplementedException;
import org.labkey.test.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public abstract class Component<EC extends Component.ElementCache> implements SearchContext
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

    private EC _elementCache;

    protected EC elementCache()
    {
        if (null == _elementCache)
            _elementCache = newElementCache();
        return _elementCache;
    }

    protected EC newElementCache()
    {
        throw new NotImplementedException("Please override newElementCache() in your component class");
    }

    protected void clearElementCache()
    {
        _elementCache = null;
    }

    public abstract class ElementCache implements SearchContext
    {
        @Override
        public List<WebElement> findElements(By by)
        {
            return getComponentElement().findElements(by);
        }

        @Override
        public WebElement findElement(By by)
        {
            return getComponentElement().findElement(by);
        }
    }

    public static abstract class ComponentFinder<S extends SearchContext, C, F extends ComponentFinder<S, C, F>>
    {
        private static final int DEFAULT_TIMEOUT = 10000;
        private int timeout = 0;
        private Integer index = null;
        private S _context;

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

        public SimpleComponentFinder<C> locatedBy(Locator loc)
        {
            return new SimpleComponentFinder<C>(loc)
            {
                @Override
                protected C construct(WebElement el)
                {
                    return ComponentFinder.this.construct(el);
                }
            };
        }

        protected abstract Locator locator();
        protected abstract C construct(WebElement el);

        protected final Locator buildLocator()
        {
            return index != null ? locator().index(index) : locator();
        }

        protected final S getContext()
        {
            return _context;
        }

        private WebElement findElement(S context)
        {
            if (timeout > 0)
                return waitForElement(context);
            else
                return buildLocator().findElement(context);
        }

        private WebElement waitForElement(S context)
        {
            return buildLocator().waitForElement(context, timeout > 0 ? timeout : DEFAULT_TIMEOUT);
        }

        private List<WebElement> findElements(S context)
        {
            if (timeout > 0)
                return waitForElements(context);
            else
                return buildLocator().findElements(context);
        }

        private List<WebElement> waitForElements(S context)
        {
            return buildLocator().waitForElements(context, timeout > 0 ? timeout : DEFAULT_TIMEOUT);
        }

        public C find(S context)
        {
            _context = context;
            return construct(findElement(context));
        }

        public List<C> findAll(S context)
        {
            _context = context;
            final List<WebElement> elements = findElements(context);
            List<C> components = new ArrayList<>();
            for (WebElement element : elements)
            {
                components.add(construct(element));
            }
            return components;
        }

        public C waitFor(S context)
        {
            _context = context;
            return construct(waitForElement(context));
        }

        public C findWhenNeeded(S context)
        {
            _context = context;
            return construct(buildLocator().findWhenNeeded(context).withTimeout(timeout));
        }

        public C findOrNull(S context)
        {
            _context = context;
            WebElement elementOrNull = buildLocator().findElementOrNull(context);
            if (elementOrNull == null)
                return null;
            else
                return construct(elementOrNull);
        }
    }

    public static abstract class SimpleComponentFinder<C> extends ComponentFinder<SearchContext, C, SimpleComponentFinder<C>>
    {
        Locator _locator;

        public SimpleComponentFinder(Locator locator)
        {
            _locator = locator;
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
