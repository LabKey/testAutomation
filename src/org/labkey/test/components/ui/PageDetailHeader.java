/*
 * Copyright (c) 2018-2026 LabKey Corporation
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
package org.labkey.test.components.ui;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.util.selenium.WebElementUtils.tryMapElement;

/**
 * Wraps <PageDetailHeader/> component
 */
public class PageDetailHeader extends AppPageHeader
{
    protected PageDetailHeader(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    /**
     * Get the rgb style value for the label color in the header
     *
     * @return A string such as "rgb(104, 204, 202)" as used in the "color-icon__circle-small" "i" element in the detail header or the empty string if element is not there.
     */
    @Override
    public String getLabelColor()
    {
        return tryMapElement(elementCache().colorIcon, el -> el.getCssValue("background-color"));
    }

    /**
     * Get the rgb style value for the sample color swatch that precedes the title. A sample with its own color shows
     * this swatch instead of the sample type's label color in the subtitle.
     *
     * @return A string such as "rgb(104, 204, 202)", or an empty string if there is no sample color swatch.
     */
    @Override
    public String getSampleColor()
    {
        return tryMapElement(elementCache().sampleColorIcon, el -> el.getCssValue("background-color"));
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

    protected class ElementCache extends AppPageHeader.ElementCache
    {
        @Override
        protected Locator.XPathLocator getTitleLocator()
        {
            return Locator.byClass("detail__header--name");
        }

        @Override
        protected Locator.XPathLocator getDescriptionLocator()
        {
            return Locator.byClass("detail__header--desc");
        }

        @Override
        protected Locator.XPathLocator getSubtitleLocator()
        {
            return Locator.byClass("detail-subtitle");
        }

        @Override
        protected Locator.XPathLocator getIconLocator()
        {
            return Locator.byClass("detail__header-icon");
        }

        final WebElement colorIcon = Locator.byClass("color-icon__circle-small").findWhenNeeded(subtitle);
        final WebElement sampleColorIcon = Locator.byClass("sample-color-header").findWhenNeeded(title);
    }

    public static class PageDetailHeaderFinder extends WebDriverComponentFinder<PageDetailHeader, PageDetailHeaderFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.byClass("page-header");

        public PageDetailHeaderFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected PageDetailHeader construct(WebElement el, WebDriver driver)
        {
            return new PageDetailHeader(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
