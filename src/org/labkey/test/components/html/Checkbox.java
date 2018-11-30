/*
 * Copyright (c) 2016-2017 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.labkey.FormItemFinder;
import org.openqa.selenium.WebElement;

public class Checkbox extends Component implements FormItem<Boolean>
{
    private WebElement _el;

    public Checkbox(WebElement element)
    {
        _el = element;
    }

    public static SimpleComponentFinder<Checkbox> Checkbox(Locator loc)
    {
        return new SimpleComponentFinder<Checkbox>(loc)
        {
            @Override
            protected Checkbox construct(WebElement el)
            {
                return new Checkbox(el);
            }
        };
    }

    public static FormItemFinder<Checkbox> Checkbox()
    {
        return new FormItemFinder<Checkbox>()
        {
            @Override
            protected Checkbox construct(WebElement el)
            {
                return new Checkbox(el);
            }

            @Override
            protected String itemTag()
            {
                return "input";
            }
        };
    }

    @Override
    public WebElement getComponentElement()
    {
        // assertElementType(); //TODO: Enable once we verify that it doesn't break tests
        return _el;
    }

    public boolean isEnabled()
    {
        return getComponentElement().isEnabled();
    }

    public boolean isSelected()
    {
        return isChecked();
    }

    public boolean isDisplayed()
    {
        return getComponentElement().isDisplayed();
    }

    public boolean isChecked()
    {
        return getComponentElement().isSelected();
    }

    public void check()
    {
        set(true);
    }

    public void uncheck()
    {
        set(false);
    }

    public void set(@NotNull Boolean checked)
    {
        if (checked != isChecked())
            toggle();
    }

    public void toggle()
    {
        _el.click();
    }

    @Override
    public Boolean get()
    {
        return isSelected();
    }

    protected void assertElementType()
    {
        String type = _el.getAttribute("type");
        Assert.assertEquals("Not a checkbox: " + _el.toString(), "checkbox", type);
    }
}
