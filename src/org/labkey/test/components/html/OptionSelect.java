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

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.openqa.selenium.WebElement;

public class OptionSelect<T extends OptionSelect.SelectOption> extends SelectWrapper implements FormItem<String>
{
    public OptionSelect(WebElement element)
    {
        super(element);
    }

    public static Component.SimpleComponentFinder<OptionSelect<SelectOption>> OptionSelect(Locator loc)
    {
        return finder(loc, SelectOption.class);
    }

    public static <O extends SelectOption> Component.SimpleComponentFinder<OptionSelect<O>> finder(Locator loc, Class<O> optionClass)
    {
        return new Component.SimpleComponentFinder<OptionSelect<O>>(loc)
        {
            @Override
            protected OptionSelect<O> construct(WebElement el)
            {
                return new OptionSelect<>(el);
            }
        };
    }

    public SelectOption getSelection()
    {
        return new SelectOption()
        {
            @Override
            public String getText()
            {
                return getFirstSelectedOption().getText();
            }

            @Override
            public String getValue()
            {
                return getFirstSelectedOption().getAttribute("value");
            }
        };
    }

    public void selectOption(T option)
    {
        if (null != option.getValue())
            selectByValue(option.getValue());
        else
            selectByVisibleText(option.getText());
    }

    @Override
    public String get()
    {
        return getSelection().getText();
    }

    @Override
    public void set(String text)
    {
        selectByVisibleText(text);
    }

    public interface SelectOption
    {
        String getValue();
        default String getText()
        {
            return null;
        }

        static SelectOption option(String value, String text)
        {
            return new SelectOption()
            {
                @Override
                public String getValue()
                {
                    return value;
                }

                @Override
                public String getText()
                {
                    return text;
                }
            };
        }

        static SelectOption textOption(String text)
        {
            return option(null, text);
        }

        static SelectOption valueOption(String value)
        {
            return option(value, null);
        }
    }
}
