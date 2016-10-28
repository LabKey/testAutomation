/*
 * Copyright (c) 2016 LabKey Corporation
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

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebDriverWrapperImpl;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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

    public static abstract class WebDriverComponentFinder<C, F extends WebDriverComponentFinder<C, F>> extends ComponentFinder<SearchContext, C, F>
    {
        private final WebDriver driver;
        public WebDriverComponentFinder(WebDriver driver)
        {
            this.driver = driver;
        }

        protected WebDriver getDriver()
        {
            return driver;
        }

        public C findWhenNeeded()
        {
            return super.findWhenNeeded(driver);
        }

        public C find()
        {
            return super.find(driver);
        }

        public C waitFor()
        {
            return super.waitFor(driver);
        }

        public C findOrNull()
        {
            return super.findOrNull(driver);
        }

        @Override
        protected final C construct(WebElement el)
        {
            return construct(el, driver);
        }

        protected abstract C construct(WebElement el, WebDriver driver);
    }
}
