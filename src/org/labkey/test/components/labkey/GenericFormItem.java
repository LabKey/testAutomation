/*
 * Copyright (c) 2016 LabKey Corporation
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
package org.labkey.test.components.labkey;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.FormItem;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.html.OptionSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class GenericFormItem extends WebDriverComponent implements FormItem
{
    private final WebElement _el;
    private final WebDriver _driver;
    private FormItem wrappedItem;

    public GenericFormItem(WebElement el, WebDriver driver)
    {
        _el = el;
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public Object get()
    {
        if (wrappedItem == null)
            wrappedItem = getSpecificFormItem();
        return wrappedItem.get();
    }

    @Override
    public void set(Object value)
    {
        if (wrappedItem == null)
            wrappedItem = getSpecificFormItem();
        wrappedItem.set(value);
    }

    private FormItem getSpecificFormItem()
    {
        WebElement element = getComponentElement();
        String tagName = element.getTagName();

        if (tagName.equals("td"))
        {
            List<WebElement> children = Locator.css("*").findElements(this);
            if (children.isEmpty())
                return new ReadOnlyFormItem(getComponentElement());
            element = children.get(0);
            tagName = element.getTagName();
        }

        switch (tagName)
        {
            case "select":
                return new OptionSelect(element);
            case "input":
            {
                String type = element.getAttribute("type");
                if ("checkbox".equals(type))
                    return new Checkbox(element);
                if ("radio".equals(type))
                    return new RadioButton(element);
                else
                    return new Input(element, getDriver());
            }
            case "textarea":
                return new Input(element, getDriver());
            default:
                return new ReadOnlyFormItem(getComponentElement());
        }
    }
}
