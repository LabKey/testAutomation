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

public class EnumSelect<E extends Enum<E>> extends SelectWrapper implements FormItem<E>
{
    Class<E> _clazz;

    public EnumSelect(WebElement element, Class<E> clazz)
    {
        super(element);
        _clazz = clazz;
    }

    public static <E extends Enum<E>> Component.SimpleComponentFinder<EnumSelect<E>> EnumSelect(Locator loc, Class<E> clazz)
    {
        return new Component.SimpleComponentFinder<EnumSelect<E>>(loc)
        {
            @Override
            protected EnumSelect<E> construct(WebElement el)
            {
                return new EnumSelect<>(el, clazz);
            }
        };
    }

    @Override
    public E get()
    {
        return Enum.valueOf(_clazz, getFirstSelectedOption().getAttribute("value"));
    }

    @Override
    public void set(E value)
    {
        String valText;
        if (value instanceof OptionSelect.SelectOption)
            valText = ((OptionSelect.SelectOption)value).getValue();
        else
            valText = value.name();
        selectByValue(valText);
    }
}
