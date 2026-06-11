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
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.labkey.test.util.selenium.WebElementUtils.tryMapElement;

/**
 * Wraps <AppPageHeader/> component
 */
public class AppPageHeader extends WebDriverComponent<AppPageHeader.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected AppPageHeader(WebElement element, WebDriver driver)
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

    @Override
    protected void waitForReady()
    {
        getWrapper().shortWait().withMessage(getClass().getSimpleName() + " is not present.").until(ExpectedConditions.visibilityOf(getComponentElement()));
    }

    /**
     * Get the source file of the page icon. If there is no icon returns an empty string.
     *
     * @return The 'src' attribute header icon, empty string if element is not there.
     */
    public String getIconSource()
    {
        return tryMapElement(elementCache().icon, el -> el.getDomAttribute("src"));
    }

    /**
     * Gets the text of the page header. If there is no header returns an empty string.
     *
     * @return Text from the page title, empty string if element is not there.
     */
    public String getTitle()
    {
        return tryMapElement(elementCache().title, WebElement::getText);
    }

    /**
     * Get the text of the subtitle of the page. If there is no subtitle, return an empty string.
     *
     * @return Text from the page subtitle, empty string if element is not there.
     */
    public String getSubtitle()
    {
        return tryMapElement(elementCache().subtitle, WebElement::getText);
    }

    /**
     * Get the text of the description of the page. If there is no description, returns an empty string.
     *
     * @return Text from the page description, empty string if element is not there.
     */
    public String getDescription()
    {
        return tryMapElement(elementCache().description, WebElement::getText);
    }

    /**
     * @throws UnsupportedOperationException Label color is not supported by AppPageHeader.
     */
    public String getLabelColor()
    {
        throw new UnsupportedOperationException("Label color is not supported by AppPageHeader.");
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<ElementCache>.ElementCache
    {
        public final WebElement icon = getIconLocator().findWhenNeeded(this);
        public final WebElement title = getTitleLocator().findWhenNeeded(this);
        public final WebElement subtitle = getSubtitleLocator().findWhenNeeded(this);
        public final WebElement description = getDescriptionLocator().findWhenNeeded(this);

        protected Locator.XPathLocator getIconLocator()
        {
            return Locator.byClass("app-page-header__icon");
        }

        protected Locator.XPathLocator getTitleLocator()
        {
            return Locator.byClass("app-page-header__title");
        }

        protected Locator.XPathLocator getSubtitleLocator()
        {
            return Locator.byClass("app-page-header__subtitle");
        }

        protected Locator.XPathLocator getDescriptionLocator()
        {
            return Locator.byClass("app-page-header__description");
        }
    }

    public static class AppPageHeaderFinder extends WebDriverComponentFinder<AppPageHeader, AppPageHeaderFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.byClass("app-page-header");

        public AppPageHeaderFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected AppPageHeader construct(WebElement el, WebDriver driver)
        {
            return new AppPageHeader(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
