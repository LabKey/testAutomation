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
package org.labkey.test.components.ui.ontology;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class TreeSearchResult extends WebDriverComponent<TreeSearchResult.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected TreeSearchResult(WebElement element, WebDriver driver)
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

    public String getLabel()
    {
        return elementCache().label.getText();
    }

    public String getCode()
    {
        return elementCache().code.getText();
    }

    public void select()
    {
        WebElement component = getComponentElement();
        getWrapper().scrollIntoView(component);
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(component));
        component.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(component));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement label = Locator.tagWithClass("div", "bold").findWhenNeeded(this);
        final WebElement code = Locator.tagWithClass("div", "col-xs-2").findWhenNeeded(this);
    }


    public static class TreeSearchResultFinder extends WebDriverComponentFinder<TreeSearchResult, TreeSearchResultFinder>
    {
        private Locator.XPathLocator _locator= Locator.tagWithClass("li", "selectable-item")
                .withChild(Locator.tagWithClass("div", "row"));

        public TreeSearchResultFinder(WebDriver driver)
        {
            super(driver);
        }

        public TreeSearchResultFinder withLabel(String label)
        {
            _locator = Locator.tagWithClass("li", "selectable-item")
                    .withChild(Locator.tagWithClass("div", "row")
                            .withChild(Locator.tagWithClass("div", "bold").withText(label)));
            return this;
        }

        public TreeSearchResultFinder withCode(String code)
        {
            _locator = Locator.tagWithClass("li", "selectable-item")
                    .withChild(Locator.tagWithClass("div", "row")
                    .withChild(Locator.tagWithClass("div", "col-xs-2").withText(code)));
            return this;
        }

        @Override
        protected TreeSearchResult construct(WebElement el, WebDriver driver)
        {
            return new TreeSearchResult(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
