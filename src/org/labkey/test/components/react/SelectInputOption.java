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
package org.labkey.test.components.react;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;

/*
    This component is meant to wrap the verbose options in filteringReactSelect, ReactSelect
 */
public class SelectInputOption extends WebDriverComponent<SelectInputOption.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected SelectInputOption(WebElement element, WebDriver driver)
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

    public boolean isFocused()
    {
        return getComponentElement().getAttribute("class").contains("select-input__option--is-focused");
    }

    public Map<String, String> getData()
    {
        List<WebElement> fieldRows = elementCache().identifyingFieldLoc.findElements(this);
        Map<String, String> data = new CaseInsensitiveHashMap<>();
        for(WebElement fieldRow : fieldRows)
        {
            String fieldLabel = elementCache().fieldLabelLoc.findElement(fieldRow).getText();
            String fieldValue = elementCache().fieldValueLoc.findElement(fieldRow).getText();
            data.put(StringUtils.stripEnd(fieldLabel, ":"), fieldValue);
        }
        return data;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public Locator.XPathLocator fieldLabelLoc = Locator.tagWithClass("span", "identifying_field_label");
        public Locator.XPathLocator identifyingFieldLoc = Locator.tag("div")
                .withChild(fieldLabelLoc);
        public Locator fieldValueLoc = fieldLabelLoc.followingSibling("span");
    }


    public static class SelectInputOptionFinder extends WebDriverComponentFinder<SelectInputOption, SelectInputOptionFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "select-input__option");
        private String _key = null;
        private String _value = null;

        public SelectInputOptionFinder(WebDriver driver)
        {
            super(driver);
        }

        public SelectInputOptionFinder withValue(String key, String value)
        {
            _key = key;
            _value = value;
            return this;
        }

        @Override
        protected SelectInputOption construct(WebElement el, WebDriver driver)
        {
            return new SelectInputOption(el, driver);
        }


        @Override
        protected Locator locator()
        {
            if (_key != null)
                return _baseLocator.withChild(
                        Locator.tag("div").withChild(
                                Locator.tagWithClass("span", "identifying_field_label").withText(_key + ":")
                        .parent())   // children are siblings
                        .withChild(Locator.tagWithText("span", _value)));
            else
                return _baseLocator;
        }
    }
}
