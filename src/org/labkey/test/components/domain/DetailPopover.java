/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.ui.grids.DetailTable;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

public class DetailPopover extends WebDriverComponent<DetailPopover.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected DetailPopover(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    public DetailPopover showPopover()
    {
        getWrapper().mouseOver(getComponentElement());
        return this;
    }

    public DetailPopover collapse()
    {
        // mouseOver the app header icon, instead of calling geWrapper.mouseOut() as that does not always work
        getWrapper().mouseOver(Locator.byClass("navbar-left-icon").findElement(getDriver()));
        clearElementCache();
        return this;
    }

    public DetailTable getDetailTable()
    {
        showPopover();
        getWrapper().mouseOver(elementCache().detailTable.getComponentElement());
        return elementCache().detailTable;
    }

    public Map<String, String> getTableData()
    {
        return getDetailTable().getTableDataByLabel();
    }

    public void clickTableLink(String fieldKey)
    {
        getDetailTable().clickField(fieldKey);
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

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public WebElement popover = Locator.tagWithClass("div", "header-details-hover")
                .findWhenNeeded(getDriver()).withTimeout(1500);
        public DetailTable detailTable = new DetailTable.DetailTableFinder(getDriver())
                .findWhenNeeded(popover);
    }


    public static class DetailPopoverFinder extends WebDriverComponentFinder<DetailPopover, DetailPopoverFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("span", "header-details-link");
        private String _title = null;

        public DetailPopoverFinder(WebDriver driver)
        {
            super(driver);
        }

        public DetailPopoverFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected DetailPopover construct(WebElement el, WebDriver driver)
        {
            return new DetailPopover(el, driver);
        }


        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withText(_title);
            else
                return _baseLocator;
        }
    }
}
