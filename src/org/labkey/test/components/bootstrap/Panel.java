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
package org.labkey.test.components.bootstrap;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.bootstrap.Panel.ElementCache;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Wraps a bootstrap styled panel.
 * It doesn't wrap a particular UI component but many components follow this pattern for styling.
 * <pre>{@code
 *
 * <div class="panel panel-default">
 *   <h2 class="panel-heading">Panel Title</h2>
 *   <div class="panel-body">
 *       <SomeComponent>
 *   </div>
 * </div>
 *
 * }</pre>
 */
public abstract class Panel<EC extends ElementCache> extends WebDriverComponent<EC>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected Panel(WebElement element, WebDriver driver)
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
     * Get the text of the panel's header
     * @return Panel title or 'null' if panel has no 'panel-heading' element
     */
    public String getTitle()
    {
        return elementCache().panelHeading.isDisplayed() ? elementCache().panelHeading.getText() : null;
    }

    /**
     * Get the text of the panel's header
     * @return Panel title or 'null' if panel has no 'panel-heading' element
     */
    public WebElement getPanelBody()
    {
        return elementCache().panelBody;
    }

    @Override
    protected abstract EC newElementCache();

    public class ElementCache extends Component<?>.ElementCache
    {
        protected final WebElement panelHeading = Locator.byClass("panel-heading").findWhenNeeded(this);
        protected final WebElement panelBody = Locator.byClass("panel-body").findWhenNeeded(this);
    }

    protected static abstract class AbstractPanelFinder<C extends Panel<?>, F extends AbstractPanelFinder<C, F>> extends WebDriverComponentFinder<C, F>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "panel");
        private String _title = null;

        public AbstractPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        public F withTitle(String title)
        {
            _title = title;
            return getThis();
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                // Using .containing because panels that have a grid that has been updated/modified will have additional text in the header.
                return _baseLocator.withChild(Locator.byClass("panel-heading").containing(_title));
            else
                return _baseLocator;
        }
    }

    public static class PanelFinder extends AbstractPanelFinder<Panel<?>, PanelFinder>
    {
        public PanelFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected PanelFinder getThis()
        {
            return this;
        }

        @Override
        protected Panel<?> construct(WebElement el, WebDriver driver)
        {
            return new PanelImpl(el, driver);
        }
    }
}

class PanelImpl extends Panel<ElementCache>
{
    PanelImpl(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }
}
