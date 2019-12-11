/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebDriverWrapperImpl;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Wrapper for components that need a WebDriver for full functionality (e.g. page navigation or JavaScript execution)
 */
public abstract class WebDriverComponent<EC extends Component.ElementCache> extends Component<EC>
{
    private WebDriverWrapper _dWrapper;
    protected WebDriverWrapper getWrapper()
    {
        if (_dWrapper == null)
            _dWrapper = new WebDriverWrapperImpl(getDriver());
        return _dWrapper;
    }

    protected abstract WebDriver getDriver();

    public WebElement doAndWaitForElementToRefresh(Runnable func, Locator loc, int timeout)
    {
        return getWrapper().doAndWaitForElementToRefresh(func, loc, this, new WebDriverWait(getDriver(), timeout));
    }

    public static abstract class WebDriverComponentFinder<C, F extends WebDriverComponentFinder<C, F>> extends ComponentFinder<SearchContext, C, F>
    {
        private final WebDriver driver;
        public WebDriverComponentFinder(WebDriver driver)
        {
            this.driver = driver;
        }

        public WebDriver getDriver()
        {
            return driver;
        }

        public C findWhenNeeded()
        {
            return super.findWhenNeeded(getDriver());
        }

        public C find()
        {
            return super.find(getDriver());
        }

        public List<C> findAll()
        {
            return super.findAll(getDriver());
        }

        public C waitFor()
        {
            return super.waitFor(getDriver());
        }

        /**
         * @deprecated Use {@link #findOptional()}
         */
        @Deprecated
        public C findOrNull()
        {
            return super.findOrNull(getDriver());
        }

        public Optional<C> findOptional()
        {
            return super.findOptional(getDriver());
        }

        @Override
        protected final C construct(WebElement el)
        {
            return construct(el, getDriver());
        }

        protected abstract C construct(WebElement el, WebDriver driver);

        /**
         * Use the element that would be found by this class to construct an alternate component.
         * @param factory Usually the constructor for the alternate component
         * @param <R> An alternate component type that is found by the same locator as the wrapped finder's
         * @return A component finder that will find the alternate component type
         */
        public <R extends WebDriverComponent<?>> SimpleWebDriverComponentFinder<R> wrap(BiFunction<WebElement, WebDriver, R> factory)
        {
            return new SimpleWebDriverComponentFinder<>(getDriver(), locator(), factory)
                    .index(getIndex())
                    .timeout(getTimeout());
        }
    }

    public static final class SimpleWebDriverComponentFinder<C extends WebDriverComponent<?>> extends WebDriverComponentFinder<C, SimpleWebDriverComponentFinder<C>>
    {
        private final Locator _locator;
        private final BiFunction<WebElement, WebDriver, C> _factory;

        public SimpleWebDriverComponentFinder(WebDriver driver, Locator locator, BiFunction<WebElement, WebDriver, C> factory)
        {
            super(driver);
            _locator = locator;
            _factory = factory;
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }

        @Override
        protected C construct(WebElement el, WebDriver driver)
        {
            return _factory.apply(el, driver);
        }
    }
}
