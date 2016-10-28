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

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Wrapper for components that should be found at global scope (e.g. an Ext dialog)
 */
public abstract class FloatingComponent<EC extends Component.ElementCache> extends Component<EC>
{
    public static abstract class WebDriverComponentFinder<C extends FloatingComponent, F extends WebDriverComponentFinder<C, F>> extends ComponentFinder<WebDriver, C, F>
    {
        WebDriver driver;

        @Override
        public C find(WebDriver context)
        {
            driver = context;
            return super.find(context);
        }

        @Override
        public C waitFor(WebDriver context)
        {
            driver = context;
            return super.waitFor(context);
        }

        @Override
        public C findWhenNeeded(WebDriver context)
        {
            driver = context;
            return super.findWhenNeeded(context);
        }

        @Override
        protected C construct(WebElement el)
        {
            return construct(el, driver);
        }

        protected abstract C construct(WebElement el, WebDriver driver);
    }
}
