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
package org.labkey.test.components;

import org.apache.commons.lang3.NotImplementedException;
import org.labkey.test.Locator;
import org.labkey.test.selenium.RefindingWebElement;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class Component<EC extends Component.ElementCache> implements SearchContext
{
    private boolean _cacheCreated = false;

    public abstract WebElement getComponentElement();

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + " " + getComponentElement();
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
        {
            try
            {
                getComponentElement().isEnabled(); // Trigger refind
            }
            catch (NoSuchElementException | StaleElementReferenceException ignore)
            {
                // Pass if element doesn't exist. Might be checking if component is visible.
            }

            _elementCache = newElementCache();
            waitForReady();
        }
        return _elementCache;
    }

    protected void waitForReady() { }

    protected EC newElementCache()
    {
        throw new NotImplementedException("Please override newElementCache() in your component class");
    }

    protected void clearElementCache()
    {
        _elementCache = null;
        _cacheCreated = false;
    }

    public abstract class ElementCache implements SearchContext
    {
        protected ElementCache()
        {
            if (_cacheCreated)
            {
                TestLogger.warn("Misused element cache in " + Component.this.getClass().getName());
            }
            else
            {
                _cacheCreated = true;
            }
        }

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

        @Override
        public String toString()
        {
            return Component.this.toString();
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
            return getThis();
        }

        protected final int getTimeout()
        {
            return timeout;
        }

        public F index(Integer index)
        {
            this.index = index;
            return getThis();
        }

        protected final Integer getIndex()
        {
            return index;
        }

        public SimpleComponentFinder<C> locatedBy(Locator loc)
        {
            return new SimpleComponentFinder<>(loc, ComponentFinder.this::construct);
        }

        protected abstract Locator locator();
        protected abstract C construct(WebElement el);

        // TODO: Override in all sublasses and make abstract
        protected F getThis()
        {
            return (F) this;
        }

        public final Locator buildLocator()
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

        public C refindWhenNeeded(S context)
        {
            _context = context;
            RefindingWebElement componentElement = buildLocator().refindWhenNeeded(context).withTimeout(timeout);
            C component = construct(componentElement);
            if (component instanceof Component)
            {
                componentElement.withRefindListener(el -> ((Component<?>) component).clearElementCache());
            }
            return component;
        }

        public C findOrNull(S context)
        {
            return findOptional(context).orElse(null);
        }

        public Optional<C> findOptional(S context)
        {
            _context = context;
            Optional<WebElement> optionalElement = buildLocator().findOptionalElement(context);
            return optionalElement.map(this::construct);
        }

        /**
         * Use the element that would be found by this class to construct an alternate component.
         * @param factory Usually the constructor for the alternate component
         * @param <R> An alternate component type that is found by the same locator as the wrapped finder's
         * @return A component finder that will find the alternate component type
         */
        public <R extends Component<?>> SimpleComponentFinder<R> wrap(Function<WebElement, R> factory)
        {
            return new SimpleComponentFinder<>(locator(), factory)
                    .index(getIndex())
                    .timeout(getTimeout());
        }
    }

    public static final class SimpleComponentFinder<C> extends ComponentFinder<SearchContext, C, SimpleComponentFinder<C>>
    {
        private final Locator _locator;
        private final Function<WebElement, C> _factory;

        public SimpleComponentFinder(Locator locator, Function<WebElement, C> factory)
        {
            _locator = locator;
            _factory = factory;
        }

        @Override
        protected C construct(WebElement el)
        {
            return _factory.apply(el);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
