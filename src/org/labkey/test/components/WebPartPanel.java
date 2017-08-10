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
package org.labkey.test.components;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class WebPartPanel extends WebDriverComponent
{
    private WebElement _componentElement;
    private WebDriver _driver;

    protected WebPartPanel(WebElement componentElement, WebDriver driver)
    {
        _componentElement = componentElement;
        _driver = driver;
    }

    public static WebPartFinder WebPart(WebDriver driver)
    {
        return new WebPartFinder(driver)
        {
            @Override
            protected WebDriverComponent construct(WebElement el, WebDriver driver)
            {
                return new WebPartPanel(el, driver);
            }
        };
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _componentElement;
    }

    public static abstract class WebPartFinder<C extends WebPartPanel, F extends WebPartFinder<C, F>> extends WebDriverComponentFinder<C, F>
    {
        private String _title;
        private boolean _partialTitle = true;

        public WebPartFinder(WebDriver driver)
        {
            super(driver);
        }

        public String getTitle()
        {
            return _title;
        }

        public boolean isPartialTitle()
        {
            return _partialTitle;
        }

        public F withTitle(String title)
        {
            _title = title;
            _partialTitle = false;
            return (F)this;
        }

        public F withTitleContaining(String partialTitle)
        {
            _title = partialTitle;
            _partialTitle = true;
            return (F)this;
        }

        @Override
        protected Locator.XPathLocator locator()
        {
            Locator.XPathLocator webPartTitle = titleLocator();
            webPartTitle = _partialTitle ? webPartTitle.containing(_title) : webPartTitle.withText(_title);

            return Locator.tagWithClass("table", "labkey-wp").withDescendant(webPartTitle);
        }

        protected Locator.XPathLocator titleLocator()
        {
            return Locator.tag("tbody/tr/*"/*td or th*/).withClass("labkey-wp-title-left");
        }
    }
}
