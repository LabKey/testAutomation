/*
 * Copyright (c) 2021-2026 LabKey Corporation
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
package org.labkey.test.components.ui.navigation.apps;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ProductListItem extends WebDriverComponent<ProductListItem.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected ProductListItem(WebElement element, WebDriver driver)
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

    public String getTitle()
    {
        return elementCache().title.getText();
    }

    public String getSubTitle()
    {
        return elementCache().subTitle.getText();
    }

    public void clickNavIcon()
    {
        elementCache().navIcon.click();
    }

    public boolean isEnabled()
    {
        return !_el.getAttribute("class").contains("labkey-page-nav-disabled");
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement productIcon = Locator.tagWithClass("div", "product-icon").findWhenNeeded(this);
        final WebElement navIcon = Locator.tagWithClass("div", "nav-icon")
                .findWhenNeeded(this).withTimeout(2000);
        final WebElement title = Locator.tagWithClass("div", "product-title")
                .findWhenNeeded(this).withTimeout(2000);
        final WebElement subTitle = Locator.tagWithClass("div", "product-subtitle")
                .findWhenNeeded(this).withTimeout(2000);
    }


    public static class ProductListItemFinder extends WebDriverComponentFinder<ProductListItem, ProductListItemFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tag("li").withChild(Locator.tagWithClass("div", "product-icon"));
        private String _title = null;

        public ProductListItemFinder(WebDriver driver)
        {
            super(driver);
        }

        public ProductListItemFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected ProductListItem construct(WebElement el, WebDriver driver)
        {
            return new ProductListItem(el, driver);
        }


        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withChild(Locator.tagWithClass("div", "product-title").withText(_title));
            else
                return _baseLocator;
        }
    }
}
