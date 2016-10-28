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
package org.labkey.test.components.html;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

public class SelectWrapper extends org.openqa.selenium.support.ui.Select
{
    protected final WebElement wrappedElement;
    private org.openqa.selenium.support.ui.Select wrappedSelect;

    protected SelectWrapper(WebElement element)
    {
        // Feed dummy Element to Selenium Select to prevent UnexpectedTagNameException
        super(new RemoteWebElement()
        {
            @Override
            public String getTagName()
            {
                return "select";
            }

            @Override
            public String getAttribute(String name)
            {
                return null;
            }
        });
        wrappedElement = element;
    }

    protected org.openqa.selenium.support.ui.Select getWrappedSelect()
    {
        if (null == wrappedSelect)
            wrappedSelect = new org.openqa.selenium.support.ui.Select(wrappedElement);
        return wrappedSelect;
    }

    public static Component.SimpleComponentFinder<Select> Select(Locator loc)
    {
        return new Component.SimpleComponentFinder<Select>(loc)
        {
            @Override
            protected SelectWrapper construct(WebElement el)
            {
                return new SelectWrapper(el);
            }
        };
    }

    @Override
    public boolean isMultiple()
    {
        return getWrappedSelect().isMultiple();
    }

    @Override
    public List<WebElement> getOptions()
    {
        return getWrappedSelect().getOptions();
    }

    @Override
    public List<WebElement> getAllSelectedOptions()
    {
        return getWrappedSelect().getAllSelectedOptions();
    }

    @Override
    public WebElement getFirstSelectedOption()
    {
        return getWrappedSelect().getFirstSelectedOption();
    }

    @Override
    public void selectByVisibleText(String text)
    {
        getWrappedSelect().selectByVisibleText(text);
    }

    @Override
    public void selectByIndex(int index)
    {
        getWrappedSelect().selectByIndex(index);
    }

    @Override
    public void selectByValue(String value)
    {
        getWrappedSelect().selectByValue(value);
    }

    @Override
    public void deselectAll()
    {
        getWrappedSelect().deselectAll();
    }

    @Override
    public void deselectByValue(String value)
    {
        getWrappedSelect().deselectByValue(value);
    }

    @Override
    public void deselectByIndex(int index)
    {
        getWrappedSelect().deselectByIndex(index);
    }

    @Override
    public void deselectByVisibleText(String text)
    {
        getWrappedSelect().deselectByVisibleText(text);
    }
}
