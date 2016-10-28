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

import org.labkey.test.components.Component;
import org.labkey.test.components.html.FormItem;
import org.openqa.selenium.WebElement;

public class ReadOnlyFormItem extends Component implements FormItem<String>
{
    private WebElement _el;

    protected ReadOnlyFormItem(WebElement el)
    {
        _el = el;
    }

    public static FormItemFinder<ReadOnlyFormItem> ReadOnlyFormItem()
    {
        return new FormItemFinder<ReadOnlyFormItem>()
        {
            @Override
            protected ReadOnlyFormItem construct(WebElement el)
            {
                return new ReadOnlyFormItem(el);
            }

            @Override
            protected String itemTag()
            {
                return ".";
            }
        };
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    public String get()
    {
        return getComponentElement().getText();
    }

    public void set(String value)
    {
        throw new UnsupportedOperationException("Field is read-only or needs special automation");
    }
}
