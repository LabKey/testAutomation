/*
 * Copyright (c) 2016-2019 LabKey Corporation
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
package org.labkey.test.components.html;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;

public class Input extends WebDriverComponent<Component<?>.ElementCache> implements FormItem<String>
{
    private final WebElement _el;
    private final WebDriver _driver; // getFormElement uses javascript
    private boolean validatedType = false;

    public Input(WebElement el, WebDriver driver)
    {
        _el = el;
        _driver = driver;
    }

    public static SimpleWebDriverComponentFinder<Input> Input(Locator loc, WebDriver driver)
    {
        return new SimpleWebDriverComponentFinder<>(driver, loc, Input::new);
    }

    public static SimpleWebDriverComponentFinder<FileInput> FileInput(Locator loc, WebDriver driver)
    {
        return new SimpleWebDriverComponentFinder<>(driver, loc, FileInput::new);
    }

    @Override
    public WebElement getComponentElement()
    {
        if (!validatedType)
        {
            if (!_el.isDisplayed())
            {
                // Don't validate type if element isn't visible
                return _el;
            }
            assertElementType(_el);
            validatedType = true;
        }
        return _el;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    public String getValue()
    {
        return get();
    }

    public void setValue(String value)
    {
        set(value);
    }

    @Override
    public String get()
    {
        return getWrapper().getFormElement(getComponentElement());
    }

    @Override
    public void set(String value)
    {
        getWrapper().setFormElement(getComponentElement(), value);
    }

    public void setWithPaste(String value)
    {
        getWrapper().actionClear(getComponentElement());
        getWrapper().actionPaste(getComponentElement(), value);
        getWrapper().fireEvent(getComponentElement(), WebDriverWrapper.SeleniumEvent.change);
    }

    public void blur()
    {
        getWrapper().fireEvent(getComponentElement(), WebDriverWrapper.SeleniumEvent.blur);
    }

    protected void assertElementType(WebElement el)
    {
        String tag = el.getTagName();
        Assert.assertTrue("<" + tag + "> element. Not an input or textarea: " + el, Arrays.asList("input", "textarea").contains(tag));
    }
}
