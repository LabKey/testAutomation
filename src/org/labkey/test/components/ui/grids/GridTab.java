/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class GridTab extends WebDriverComponent<GridTab.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public GridTab(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    public GridTab select()
    {
        if (!isActive())
            elementCache().anchor.click();
        WebDriverWrapper.waitFor(()-> isActive(), "the tab did not become enabled in time", 1000);
        return this;
    }

    public boolean isActive()
    {
        return getComponentElement().getAttribute("class").equals("active");
    }

    public String getText()
    {
        return elementCache().anchor.getText();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement anchor = Locator.css("a").findWhenNeeded(this);
    }

    public static class GridTabFinder extends WebDriverComponentFinder<GridTab, GridTabFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tag("li").withChild(Locator.tag("a"));
        private String _startsWith = null;
        private String _fullText = null;

        public GridTabFinder(WebDriver driver)
        {
            super(driver);
        }

        public GridTabFinder withText(String fullText)
        {
            _fullText = fullText;
            return this;
        }

        public GridTabFinder startsWith(String startsWith)
        {
            _startsWith = startsWith;
            return this;
        }

        @Override
        protected GridTab construct(WebElement el, WebDriver driver)
        {
            return new GridTab(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_startsWith != null)
                return _baseLocator.withChild(Locator.tag("a").startsWith(_startsWith));
            else if (_fullText != null)
                return _baseLocator.withChild(Locator.tag("a").withText(_fullText));
            else
                return _baseLocator;
        }
    }
}
