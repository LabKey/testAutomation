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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Wraps the "Applications" panel in the AppsMenu
 */
public class ProductsNavContainer extends BaseNavContainer
{
    protected ProductsNavContainer(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    public boolean productEnabled(Product product)
    {
        return getProduct(product).isEnabled();
    }

    public LeafNavContainer clickProduct(Product product)
    {
        getProduct(product).clickNavIcon();
        return new LeafNavContainer.LeafNavContainerFinder(getDriver()).withTitle(product.getName()).waitFor();
    }

    public LKSNavContainer clickLabkey()
    {
        getProduct(Product.LabKey).clickNavIcon();
        return new LKSNavContainer.LKSNavContainerFinder(getDriver()).withBackNavTitle(Product.LabKey.getName()).waitFor();
    }

    public List<ProductListItem> getProducts()
    {
        return new ProductListItem.ProductListItemFinder(getDriver()).findAll(elementCache().navList);
    }

    public List<String> getProductNames()
    {
        return getProducts().stream().map(a-> a.getTitle()).collect(Collectors.toList());
    }

    public ProductListItem getProduct(Product product)
    {
        return new ProductListItem.ProductListItemFinder(getDriver()).withTitle(product.getName())
                .waitFor(elementCache().navList);
    }

    public boolean hasMenuSettingsLink()
    {
        return Locator.linkWithText("Menu Settings").existsIn(elementCache().footer);
    }

    public WebElement getMenuSettingsLink()
    {
        return Locator.linkWithText("Menu Settings").waitForElement(elementCache().footer, 2000);
    }

    public WebElement getDocumentationFooterLink()
    {
        return Locator.linkWithText("More LabKey Solutions").waitForElement(elementCache().footer, 2000);
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends BaseNavContainer.ElementCache
    {
        final WebElement footer = Locator.tagWithClass("div", "product-navigation-footer")
                .findWhenNeeded(this).withTimeout(2000);
    }

    public static class ProductNavContainerFinder extends BaseNavContainerFinder<ProductsNavContainer, ProductsNavContainer.ProductNavContainerFinder>
    {
        public ProductNavContainerFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected ProductsNavContainer construct(WebElement el, WebDriver driver)
        {
            return new ProductsNavContainer(el, driver);
        }

    }

    public enum Product
    {
        Biologics("Biologics"),
        SampleManager("Sample Manager"),
        LabKey("LabKey Server");

        Product(String name)
        {
            _name = name;
        }
        private final String _name;

        public String getName()
        {
            return _name;
        }
    }
}
