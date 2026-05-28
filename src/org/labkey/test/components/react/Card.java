/*
 * Copyright (c) 2022-2026 LabKey Corporation
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
package org.labkey.test.components.react;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * automates shared component implemented by /internal/components/base/Cards.tsx
 */
public class Card extends WebDriverComponent<Card.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected Card(WebElement element, WebDriver driver)
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

    /**
     * returns the text of the 'title' tag in the content block
     */
    public String getTitle()
    {
        return elementCache().titleElement.getText();
    }

    /**
     * returns the URL to which the user will go if the component is clicked
     */
    public String getLinkHref()
    {
        return getComponentElement().getAttribute("href");
    }

    /**
     * gets the contents of the 'content' element, minus the title (which is a child of the content element) because
     * title is separately available here
     * @return the text of the 'content' element, minus the title
     */
    public String getContent()
    {
        String content = elementCache().contentElement.getText();
        return content.replace(getTitle(), "").trim();
    }

    /**
     * gets the contents of the 'alt' attribute of the center Icon element
     * @return The text of the 'alt' attribute
     */
    public String getIconAltString()
    {
        return elementCache().cardBlockCenterContentImg.getAttribute("alt");
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement titleElement = Locator.tagWithClass("div", "cards__card-title")
                .findWhenNeeded(this);
        final WebElement contentElement = Locator.tagWithClass("div", "cards__card-content")
                .findWhenNeeded(this);
        final WebElement cardBlockCenterElement = Locator.tagWithClass("div", "cards__block-center")
                .findWhenNeeded(this);
        final WebElement cardBlockCenterContentImg = Locator.tagWithClass("div", "cards__block-center-content")
                .child("img").findWhenNeeded(this);
    }

    public static class CardFinder extends WebDriverComponentFinder<Card, CardFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("a", "cards__card");
        private String _title = null;

        public CardFinder(WebDriver driver)
        {
            super(driver);
        }

        public CardFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected Card construct(WebElement el, WebDriver driver)
        {
            return new Card(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withDescendant(Locator.tagWithClass("div", "cards__card-title").withText(_title));
            else
                return _baseLocator;
        }
    }
}
