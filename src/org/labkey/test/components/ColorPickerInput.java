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
package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ColorPickerInput extends WebDriverComponent<Component<?>.ElementCache>
{
    private final WebDriver _driver;
    final WebElement _componentElement;

    public ColorPickerInput(WebElement element, WebDriver driver)
    {
        _driver = driver;
        _componentElement = element;
    }
    @Override
    public WebElement getComponentElement()
    {
        return _componentElement;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    public void setHexValue(String hexValue)
    {
        List<WebElement> inputs = Locator.tag("input").findElements(this);
        if (inputs.isEmpty())
            throw new NoSuchElementException("Input tag not found in color picker");
        WebElement hexInput = inputs.getFirst();
        new Input(hexInput, getDriver()).setWithPaste(hexValue);
    }

    public String getHexValue()
    {
        List<WebElement> inputs = Locator.tag("input").findElements(this);
        if (inputs.isEmpty())
            throw new NoSuchElementException("Input tag not found in color picker");
        WebElement hexInput = inputs.getFirst();
        return new Input(hexInput, getDriver()).getValue();
    }

    public static class ColorPickerInputFinder extends WebDriverComponentFinder<ColorPickerInput, ColorPickerInput.ColorPickerInputFinder>
    {
        private final Locator _locator;

        public ColorPickerInputFinder(WebDriver driver)
        {
            super(driver);
            _locator = Locator.tagWithClass("div", "compact-picker");
        }

        @Override
        protected ColorPickerInput construct(WebElement element, WebDriver driver)
        {
            return new ColorPickerInput(element, driver);
        }

        @Override
        protected Locator locator() { return _locator; }
    }
}
